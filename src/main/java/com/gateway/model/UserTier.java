package com.gateway.model;

import lombok.Getter;

@Getter
public enum UserTier {
    ANONYMOUS(5, 10),
    FREE(20, 30),
    PREMIUM(100, 150),
    ENTERPRISE(1000, 1500);

    private final int requestsPerMinute;
    private final int burstCapacity;

    UserTier(int requestsPerMinute, int burstCapacity) {
        this.requestsPerMinute = requestsPerMinute;
        this.burstCapacity = burstCapacity;
    }

    public static UserTier fromString(String tier) {
        if (tier == null || tier.trim().isEmpty()) {
            return ANONYMOUS;
        }
        try {
            return UserTier.valueOf(tier.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ANONYMOUS;
        }
    }
}
