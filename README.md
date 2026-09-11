# Rate-Limited API Gateway with Analytics

A centralized, high-performance API Gateway built with **Spring Boot 3** and **Spring Cloud Gateway (WebFlux)** for routing, filtering, and securing backend microservices.

- **Redis Rate Limiting**: Enforces fair usage and mitigates abuse using distributed token bucket and sliding window algorithms.
- **JWT Authentication**: Provides stateless token validation at the edge and propagates user context downstream.
- **MySQL Analytics Pipeline**: Asynchronously logs API request metadata, latency, and status codes without blocking traffic.
- **Real-Time Observability**: Integrates Prometheus metrics scraping and Grafana dashboards for monitoring throughput and errors.