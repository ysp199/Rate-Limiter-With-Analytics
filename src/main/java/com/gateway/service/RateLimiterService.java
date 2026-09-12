package com.gateway.service;

import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RateLimiterService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final long defaultWindowSeconds;

    public RateLimiterService(
            ReactiveRedisTemplate<String, String> redisTemplate,
            @Value("${rate-limiter.window-seconds:60}") long defaultWindowSeconds) {
        this.redisTemplate = redisTemplate;
        this.defaultWindowSeconds = defaultWindowSeconds;
    }

    @Getter
    @Builder
    public static class RateLimitResult {
        private final boolean allowed;
        private final long currentCount;
        private final int limit;
        private final long remaining;
        private final long retryAfterSeconds;
    }

    public Mono<RateLimitResult> isAllowed(String clientIdentifier, int maxRequests) {
        String key = "rate_limit:" + clientIdentifier;
        long nowMillis = Instant.now().toEpochMilli();
        long windowMillis = defaultWindowSeconds * 1000L;
        long windowStart = nowMillis - windowMillis;
        String member = nowMillis + ":" + UUID.randomUUID();

        return redisTemplate.opsForZSet()
                // 1. Remove elements outside the sliding window
                .removeRangeByScore(key, Range.closed(0.0, (double) windowStart))
                // 2. Add current request to the sliding window
                .then(redisTemplate.opsForZSet().add(key, member, (double) nowMillis))
                // 3. Set TTL on the key so it auto-expires
                .then(redisTemplate.expire(key, Duration.ofSeconds(defaultWindowSeconds + 5)))
                // 4. Count elements currently in the window
                .then(redisTemplate.opsForZSet().size(key))
                .map(count -> {
                    boolean allowed = count <= maxRequests;
                    long remaining = Math.max(0, maxRequests - count);
                    long retryAfter = allowed ? 0 : defaultWindowSeconds;
                    return RateLimitResult.builder()
                            .allowed(allowed)
                            .currentCount(count)
                            .limit(maxRequests)
                            .remaining(remaining)
                            .retryAfterSeconds(retryAfter)
                            .build();
                });
    }
}
