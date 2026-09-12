package com.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitErrorResponse {
    private int status;
    private String error;
    private String message;
    private String tier;
    private int limit;
    private long retryAfterSeconds;
    private Instant timestamp;
}
