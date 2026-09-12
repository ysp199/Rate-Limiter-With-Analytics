package com.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("api_request_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiLog {

    @Id
    private Long id;

    @Column("request_id")
    private String requestId;

    @Column("client_ip")
    private String clientIp;

    @Column("user_id")
    private String userId;

    @Column("user_tier")
    private String userTier;

    @Column("http_method")
    private String httpMethod;

    @Column("request_uri")
    private String requestUri;

    @Column("route_id")
    private String routeId;

    @Column("response_status")
    private Integer responseStatus;

    @Column("latency_ms")
    private Long latencyMs;

    @Column("user_agent")
    private String userAgent;

    @Column("created_at")
    private LocalDateTime createdAt;
}
