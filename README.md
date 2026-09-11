# Rate-Limited API Gateway with Analytics

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring%20Cloud-Gateway-blue.svg)](https://spring.io/projects/spring-cloud-gateway)
[![Redis](https://img.shields.io/badge/Redis-Rate%20Limiting-red.svg)](https://redis.io/)
[![MySQL](https://img.shields.io/badge/MySQL-Analytics%20Storage-orange.svg)](https://www.mysql.com/)
[![Prometheus](https://img.shields.io/badge/Prometheus-Monitoring-E6522C.svg)](https://prometheus.io/)
[![Grafana](https://img.shields.io/badge/Grafana-Dashboards-F46800.svg)](https://grafana.com/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A high-performance, non-blocking **API Gateway** built with **Spring Boot 3** and **Spring Cloud Gateway (WebFlux)**. It provides centralized routing, dynamic rate-limiting powered by **Redis**, stateless **JWT authentication**, an asynchronous **MySQL analytics pipeline**, and full observability using **Prometheus** and **Grafana**.

---

## Key Highlights

- **Centralized Routing & Filtering**: Serves as the unified entry point for backend microservices, handling path-based routing, request/response decoration, and cross-cutting security.
- **Distributed Redis Rate Limiting**: Implements sliding window / token bucket rate limiting via atomic Redis operations to prevent API abuse, mitigate DDoS attacks, and enforce fair usage quotas.
- **Tier-Based Quotas**: Dynamically enforces rate limits based on client identity and subscription tier (`Anonymous`, `Free`, `Premium`).
- **Stateless JWT Authentication**: Validates Bearer tokens at the edge, extracts user claims, and injects identity headers (`X-User-Id`, `X-User-Role`, `X-User-Tier`) downstream.
- **Asynchronous Analytics Pipeline**: Captures comprehensive request metadata (path, latency, status code, client IP, rate-limit outcomes) and persists logs to MySQL non-blockingly without impacting gateway throughput.
- **Full Observability & Dashboards**: Emits custom Micrometer metrics scraped by Prometheus and visualizes traffic, latency percentiles, and rate-limit triggers in real-time Grafana dashboards.

---

## Architecture Flow

```
                      +-----------------------------+
                      |   Client (Web/Mobile/API)   |
                      +--------------+--------------+
                                     |
                                     | HTTP Requests (Bearer JWT / API Key)
                                     v
+-----------------------------------------------------------------------------------+
|                           Spring Cloud API Gateway                                 |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  | Request Pipeline (Reactive Gateway Filters)                                 |  |
|  |  1. Global Tracing Filter (Generates TraceId & Latency Timer)               |  |
|  |  2. JWT Authentication Filter (Validates token, extracts User & Tier)       |  |
|  |  3. Dynamic Rate Limiter (Redis Sliding Window / Token Bucket)              |  |
|  |     - Anonymous: 5 req/min  |  Free: 20 req/min  |  Premium: 100 req/min     |  |
|  |     - Returns HTTP 429 + X-RateLimit-* headers when exceeded                |  |
|  |  4. Downstream Routing (Proxies request to target microservices)            |  |
|  |  5. Post-Filter: Async Analytics Dispatch (MySQL) & Prometheus Metrics      |  |
|  +-----------------------------------------------------------------------------+  |
+---------+--------------------------+-----------------------+----------------------+
          |                          |                       |
          v                          v                       v
    +-----------+              +-----------+          +---------------+
    |   Redis   |              |   MySQL   |          |  Prometheus   |
    | Rate Limit|              | Analytics |          | & Grafana     |
    | State/TTL |              |  Storage  |          | Dashboards    |
    +-----------+              +-----------+          +---------------+
                                     |
                                     v
                       +---------------------------+
                       | Downstream Backend        |
                       | Microservices (Users/etc) |
                       +---------------------------+
```

---

## Tech Stack

| Layer | Technology | Purpose |
| :--- | :--- | :--- |
| **Runtime & Core** | Java 21, Spring Boot 3.3.4 | Core application framework |
| **API Gateway** | Spring Cloud Gateway (WebFlux / Project Reactor) | Non-blocking, reactive HTTP routing & filtering |
| **Rate Limiter** | Redis (Reactive Redis Template) | Distributed counter, sliding window & token bucket algorithms |
| **Security** | JJWT (io.jsonwebtoken 0.12.6) | HMAC-SHA256 stateless token signing & verification |
| **Analytics Store** | MySQL 8.0 / Spring Data R2DBC | Asynchronous, reactive request log & metrics storage |
| **Observability** | Spring Boot Actuator, Micrometer, Prometheus | Metrics instrumentation and scraping |
| **Visualization** | Grafana | Real-time traffic, latency, and rate-limiting dashboards |
| **Deployment** | Docker & Docker Compose | Containerized multi-service orchestration |

---

## Project Structure

```
.
├── pom.xml                                   # Project dependencies & build lifecycle
├── README.md                                 # Project documentation
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── gateway/
        │           ├── ApiGatewayApplication.java      # Application entry point
        │           ├── analytics/                      # Asynchronous request logging & metrics
        │           ├── filter/                         # Gateway filters (JWT, RateLimit, Trace)
        │           ├── ratelimit/                      # Redis sliding window rate limiter
        │           └── security/                       # JWT utility & token verification
        └── resources/
            ├── application.yml               # Route definitions, Redis, MySQL & tiers config
            ├── schema.sql                    # MySQL analytics logging table definitions
            └── scripts/
                └── sliding_window.lua        # Atomic Redis sliding window Lua script
```

---

## Rate Limiting Policy

Requests include RFC-standard rate-limiting headers in every response:

```http
HTTP/1.1 200 OK
X-RateLimit-Limit: 20
X-RateLimit-Remaining: 14
X-RateLimit-Reset: 35
```

When quota is exceeded, the Gateway rejects requests with `HTTP 429 Too Many Requests`:

```json
{
  "timestamp": "2026-09-11T16:30:00Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit quota exceeded for tier [FREE]. Please try again later.",
  "retryAfterSeconds": 28
}
```

### Rate Limit Tiers

| Tier | Window | Max Requests | Identified By |
| :--- | :--- | :--- | :--- |
| **Anonymous** | 60 seconds | 5 requests | Client IP address (`X-Forwarded-For`) |
| **Free** | 60 seconds | 20 requests | JWT Subject (`sub`) with `ROLE_FREE` |
| **Premium** | 60 seconds | 100 requests | JWT Subject (`sub`) with `ROLE_PREMIUM` |

---

## Getting Started

### Prerequisites
- **Java 21 JDK**
- **Docker & Docker Compose** (or local installations of Redis 7+ and MySQL 8+)
- **Maven 3.8+**

### Configuration
Environment variables can be overridden via `application.yml` or your `.env` file:

```yaml
# application.yml snippet
rate-limiter:
  window-seconds: 60
  tiers:
    anonymous: 5
    free: 20
    premium: 100

spring:
  data:
    redis:
      host: localhost
      port: 6379
  r2dbc:
    url: r2dbc:mysql://localhost:3306/gateway_analytics
```

### Running with Docker Compose
```bash
# Start Redis, MySQL, Prometheus, Grafana, and Downstream Services
docker compose up -d
```

### Accessing Endpoints & Monitoring
- **API Gateway**: `http://localhost:8080`
- **Actuator Health**: `http://localhost:8080/actuator/health`
- **Prometheus Metrics**: `http://localhost:8080/actuator/prometheus`
- **Prometheus Server**: `http://localhost:9090`
- **Grafana Dashboard**: `http://localhost:3000` *(Default: admin / admin)*

---

## License
Distributed under the MIT License. See `LICENSE` for more information.