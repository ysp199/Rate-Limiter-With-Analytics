package com.gateway.repository;

import com.gateway.model.ApiLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ApiLogRepository extends ReactiveCrudRepository<ApiLog, Long> {

    Flux<ApiLog> findByUserId(String userId);

    Flux<ApiLog> findByResponseStatus(Integer responseStatus);

    @Query("SELECT * FROM api_request_logs ORDER BY created_at DESC LIMIT :limit")
    Flux<ApiLog> findRecentLogs(int limit);
}
