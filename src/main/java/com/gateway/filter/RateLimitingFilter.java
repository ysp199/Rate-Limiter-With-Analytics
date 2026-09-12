package com.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.dto.RateLimitErrorResponse;
import com.gateway.model.UserTier;
import com.gateway.service.RateLimiterService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Instant;

@Component
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimiterService rateLimiterService, ObjectMapper objectMapper) {
        this.rateLimiterService = rateLimiterService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Bypass rate limiting for actuator health/metrics endpoints
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String userId = request.getHeaders().getFirst("X-User-Id");
        String tierHeader = request.getHeaders().getFirst("X-User-Tier");
        UserTier userTier = UserTier.fromString(tierHeader);

        String clientIdentifier;
        if (userId != null && !userId.isBlank()) {
            clientIdentifier = "user:" + userId;
        } else {
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            String clientIp = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
            clientIdentifier = "ip:" + clientIp;
        }

        int limit = userTier.getRequestsPerMinute();

        return rateLimiterService.isAllowed(clientIdentifier, limit)
                .flatMap(result -> {
                    ServerHttpResponse response = exchange.getResponse();

                    // Set standard rate limit headers
                    response.getHeaders().add("X-RateLimit-Limit", String.valueOf(result.getLimit()));
                    response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));
                    response.getHeaders().add("X-RateLimit-Reset", String.valueOf(result.getRetryAfterSeconds()));

                    if (result.isAllowed()) {
                        return chain.filter(exchange);
                    } else {
                        return rateLimitExceededResponse(exchange, userTier, result.getLimit(), result.getRetryAfterSeconds());
                    }
                });
    }

    private Mono<Void> rateLimitExceededResponse(ServerWebExchange exchange, UserTier tier, int limit, long retryAfterSeconds) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().add("Retry-After", String.valueOf(retryAfterSeconds));

        RateLimitErrorResponse errorResponse = RateLimitErrorResponse.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message("API rate limit exceeded. Please upgrade your tier or wait before retrying.")
                .tier(tier.name())
                .limit(limit)
                .retryAfterSeconds(retryAfterSeconds)
                .timestamp(Instant.now())
                .build();

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            byte[] fallback = "{\"status\":429,\"error\":\"Too Many Requests\"}".getBytes();
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }

    @Override
    public int getOrder() {
        return -50; // Runs after JwtAuthenticationFilter (-100)
    }
}
