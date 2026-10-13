# 🚪 API Gateway

> Part of the **Blockchain-Inspired Online Voting System** — a production-grade, scalable microservices platform.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud%20Gateway-4.1.5-blue.svg)](https://spring.io/projects/spring-cloud-gateway)
[![Redis](https://img.shields.io/badge/Redis-Rate%20Limiting-red.svg)](https://redis.io/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📌 Overview

The **API Gateway** is the single entry point for all external requests. It handles routing to appropriate microservices, provides centralized authentication via JWT validation, and protects the system using rate limiting.

### Feature Status
- ✅ Centralized Routing
- ✅ Service Discovery Integration (Eureka)
- ✅ JWT Authentication Filter
- ✅ Redis-based Rate Limiting
- ✅ JSON errors from the gateway itself (401 / 403 / 503)
- ✅ Spoofing protection: client-sent identity headers are always stripped
- ✅ Exact method + path rules for open endpoints; coarse role checks at the edge
- ✅ Per-route circuit breakers with JSON fallbacks (Resilience4j)
- ✅ CORS policy (allowed origins from `CORS_ALLOWED_ORIGINS`)
- ✅ Tests (30)
- ✅ Observability: Prometheus metrics (`/actuator/prometheus`), Zipkin tracing, JSON logs (`json-logs` / `logstash` profiles)

---

## 🏗️ Architecture

```
                               ┌─────────────────────────────────┐
                               │          CLIENTS                │
                               │   (Web App / Mobile / Admin)    │
                               └──────────────┬──────────────────┘
                                              │ HTTPS
       ┌────────────────┐      ┌──────────────▼──────────────────┐
       │ EUREKA SERVER  │◄─────┤        ★API GATEWAY★           │
       │ (Registry)     │      │           (8080)                │
       └────────────────┘      │  • Route matching               │
                               │  • Rate limiting (Redis)        │
                               │  • JWT validation (WebClient)   │
                               └──────────────┬──────────────────┘
                                              │
           ┌────────────────┬─────────────────┼────────────────┬────────────────┐
           │                │                 │                │                │
    ┌──────▼──────┐  ┌──────▼──────┐   ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
    │    USER     │  │  CANDIDATE  │   │    VOTING   │  │    RESULT   │  │    EVENT    │
    │   SERVICE   │  │   SERVICE   │   │   SERVICE   │  │   SERVICE   │  │    BUS      │
    └─────────────┘  └─────────────┘   └─────────────┘  └─────────────┘  └─────────────┘
```

---

## 🛠️ Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 17 |
| Framework | Spring Boot | 3.3.6 |
| Gateway | Spring Cloud Gateway | 4.1.5 |
| Discovery | Netflix Eureka Client | — |
| Rate Limiter | Redis (Reactive) | — |
| HTTP Client | WebClient (WebFlux) | — |
| Resilience | Resilience4j (reactive circuit breaker) | via Spring Cloud 2023.0.3 |
| Build Tool | Maven | 3.8+ |

---

## 🔐 Security & Filters

### Authentication Filter
`AuthenticationFilter` runs first on **every** route and:
1.  Removes any `loggedInUser` / `role` headers sent by the client (nobody can claim an identity or another user's rate-limit bucket).
2.  Lets open endpoints through (`RouterValidator`, exact method + path):
    `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/candidates/**`,
    `GET /api/v1/votes/election/*/count`, `GET /api/v1/results/**`.
3.  Otherwise reads the Bearer token and asks `user-service /api/v1/auth/validate` who it belongs to (`TokenValidator`).
4.  Applies coarse role rules (`RoleAuthorizer`): `POST /api/v1/votes` needs `ROLE_VOTER`; non-GET `/api/v1/candidates/**` needs `ROLE_ADMIN`.
    Finer rules (vote owner, admin-only reads) stay in the services, which also verify the JWT themselves.
5.  Adds the verified `loggedInUser` (user id) and `role` headers and forwards the request.

| Situation | Response |
|-----------|----------|
| No / malformed `Authorization` header on a secured route | 401 JSON |
| Token rejected by user-service | 401 JSON |
| Wrong role for the route | 403 JSON |
| user-service down or failing | 503 JSON |

### Circuit Breakers
Each route has a Resilience4j circuit breaker (`userService`, `candidateService`, `votingService`, `resultService`):
50 % failures over 20 calls opens it for 10 s; calls time out after 5 s. When open, timed out or when a service has no
registered instance, `FallbackController` answers `503 {"success":false,"message":"<service> is temporarily unavailable…"}`.
A vote that timed out may still have been stored; retrying with the same idempotency key returns the original receipt.

### CORS
Allowed origins come from `CORS_ALLOWED_ORIGINS` (comma-separated, default `http://localhost:3000`); methods
GET/POST/PUT/PATCH/DELETE/OPTIONS; headers `Authorization`, `Content-Type`.

### Rate Limiting
Uses the **Token Bucket** algorithm via Redis, one bucket per verified user (`user:<id>`) or, for anonymous calls, per client IP (`ip:<address>`):
-   **User Service**: 10 req/s (Replenish Rate), 20 (Burst Capacity).
-   **Candidate Service**: 5 req/s (Replenish Rate), 10 (Burst Capacity).
-   **Voting Service**: 5 req/s (Replenish Rate), 10 (Burst Capacity).
-   **Result Service**: 20 req/s (Replenish Rate), 40 (Burst Capacity). *(Higher limit — public, read-heavy)*

---

## 📦 Project Structure

```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/voting/gateway/
│   │   │   ├── ApiGatewayApplication.java
│   │   │   ├── config/              ← AuthenticationFilter, RouterValidator, RoleAuthorizer,
│   │   │   │                          TokenValidator, RateLimiterConfig, WebClientConfig
│   │   │   ├── controller/          ← FallbackController (503 JSON)
│   │   │   └── dto/                 ← API Response DTOs
│   │   └── resources/
│   │       └── application.yml      ← Routes, CORS, circuit breakers, Redis
│   └── test/java/com/voting/gateway/ ← 30 tests (filter, rules, token validation, CORS, fallback)
├── README.md
├── pom.xml
└── mvnw
```

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Redis (Running at `localhost:6379`)
- Eureka Server (Running at `localhost:8761`)

### Setup

1. **Build the project**
   ```bash
   ./mvnw clean install
   ```

2. **Run the service**
   ```bash
   ./mvnw spring-boot:run
   ```
   Optional env vars: `REDIS_HOST`, `REDIS_PORT`, `EUREKA_URL`, `CORS_ALLOWED_ORIGINS`.

3. **Run the tests** (no Redis, Eureka or other services needed)
   ```bash
   ./mvnw test
   ```

---

## 🐳 Docker

```bash
docker build -t voting/api-gateway:1.0.0 .
```

The multi-stage `Dockerfile` builds the jar with Maven and runs it on a Java 17 JRE (Alpine) as a non-root user, with a
health check on `/actuator/health/readiness` (port 8080). To run the whole system, use `docker compose up -d --build` in
the root `Voting_System` repo (see its README).

---

## 📋 Implementation Progress

| Task | Description | Status |
|------|-------------|--------|
| 2.3 | API Gateway Setup | ✅ Done |
| 2.4 | JWT Validation Filter | ✅ Done |
| 2.5 | Rate Limiting | ✅ Done |
| 5.0 | Voting Service Route | ✅ Done |
| 5.1 | Result Service Route (public reads; identity headers still stripped) | ✅ Done |
| 6.x | Circuit breakers with JSON fallbacks | ✅ Done |
| 7.4 | CORS policy | ✅ Done |
| 7.6 | Per-user rate limiting that cannot be spoofed | ✅ Done |

---

## 📝 License

This project is licensed under the MIT License.

---

> **Maintainer:** Vaibhav Jain  
> **Last Updated:** May 29, 2026
