package com.gateway.filter;

import com.gateway.model.ApiLog;
import com.gateway.repository.ApiLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class AnalyticsFilter implements GlobalFilter, Ordered {

    private final ApiLogRepository apiLogRepository;
    private final MeterRegistry meterRegistry;

    public AnalyticsFilter(ApiLogRepository apiLogRepository, MeterRegistry meterRegistry) {
        this.apiLogRepository = apiLogRepository;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // Skip logging internal actuator health checks to reduce noise
        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String requestId = UUID.randomUUID().toString();

        return chain.filter(exchange).doFinally(signalType -> {
            long latencyMs = System.currentTimeMillis() - startTime;
            ServerHttpResponse response = exchange.getResponse();
            Integer statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 500;

            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            String routeId = route != null ? route.getId() : "unknown-route";

            String userId = request.getHeaders().getFirst("X-User-Id");
            String userTier = request.getHeaders().getFirst("X-User-Tier");
            String userAgent = request.getHeaders().getFirst("User-Agent");

            InetSocketAddress remoteAddress = request.getRemoteAddress();
            String clientIp = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";

            // Record Prometheus / Micrometer observability metrics
            meterRegistry.counter("gateway.requests.total",
                    "route", routeId,
                    "method", request.getMethod().name(),
                    "status", String.valueOf(statusCode)
            ).increment();

            meterRegistry.timer("gateway.requests.latency",
                    "route", routeId,
                    "status", String.valueOf(statusCode)
            ).record(latencyMs, TimeUnit.MILLISECONDS);

            // Asynchronously persist API request log into MySQL via R2DBC
            ApiLog log = ApiLog.builder()
                    .requestId(requestId)
                    .clientIp(clientIp)
                    .userId(userId != null ? userId : "anonymous")
                    .userTier(userTier != null ? userTier : "ANONYMOUS")
                    .httpMethod(request.getMethod().name())
                    .requestUri(path)
                    .routeId(routeId)
                    .responseStatus(statusCode)
                    .latencyMs(latencyMs)
                    .userAgent(userAgent != null ? userAgent : "unknown")
                    .createdAt(LocalDateTime.now())
                    .build();

            apiLogRepository.save(log)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
