-- Schema for API Gateway Analytics & Auditing Pipeline
CREATE TABLE IF NOT EXISTS api_request_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL,
    client_ip VARCHAR(45) NOT NULL,
    user_id VARCHAR(64) DEFAULT 'anonymous',
    user_tier VARCHAR(32) DEFAULT 'ANONYMOUS',
    http_method VARCHAR(10) NOT NULL,
    request_uri VARCHAR(512) NOT NULL,
    route_id VARCHAR(64),
    response_status INT NOT NULL,
    latency_ms BIGINT NOT NULL,
    user_agent VARCHAR(256),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_created_at (created_at),
    INDEX idx_user_id (user_id),
    INDEX idx_route_id (route_id),
    INDEX idx_response_status (response_status),
    INDEX idx_client_ip (client_ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
