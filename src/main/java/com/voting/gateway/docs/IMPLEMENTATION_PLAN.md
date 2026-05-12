# 📋 API Gateway — Implementation Plan

> **Project:** Blockchain-Inspired Online Voting System  
> **Service:** `api-gateway`  
> **Tech:** Java 17 · Spring Cloud Gateway · Redis · Eureka Client  
> **Status:** ✅ COMPLETED

---

## 📌 Overview

The API Gateway acts as the entry point, providing a unified URL for all microservices. It offloads cross-cutting concerns like Authentication and Rate Limiting from the individual services.

---

## 🏗️ Implementation Checklist

### Step 1: Project Bootstrapping
- [x] 1.1 — Create directory `api-gateway`
- [x] 1.2 — Configure `pom.xml` with `spring-cloud-starter-gateway`, `spring-cloud-starter-netflix-eureka-client`, and `spring-boot-starter-data-redis-reactive`
- [x] 1.3 — Create `ApiGatewayApplication.java`

### Step 2: Routing Configuration
- [x] 2.1 — Configure `application.yml` with routes for `user-service` and `candidate-service`
- [x] 2.2 — Use `lb://` URI format for dynamic load balancing via Eureka
- [x] 2.3 — Map paths: `/api/v1/auth/**`, `/api/v1/users/**`, `/api/v1/candidates/**`

### Step 3: Security (JWT Filter)
- [x] 3.1 — Create `RouterValidator.java` to define public vs private endpoints
- [x] 3.2 — Create `AuthenticationFilter.java` (Global Gateway Filter)
- [x] 3.3 — Implement JWT validation by calling `user-service/validate` using `WebClient`
- [x] 3.4 — Inject `loggedInUser` and `role` into request headers after validation

### Step 4: Rate Limiting
- [x] 4.1 — Configure `RateLimiterConfig.java`
- [x] 4.2 — Implement `KeyResolver` to identify users (via `loggedInUser` header or IP)
- [x] 4.3 — Apply `RequestRateLimiter` filter to routes in `application.yml`

### Step 5: Verification
- [x] 5.1 — Build and verify all filters work as expected
- [x] 5.2 — Verify Redis connectivity for rate limiting

---

## 📝 Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Framework | Spring Cloud Gateway | Modern, reactive, and non-blocking, superior to Netflix Zuul for high concurrency |
| Auth Strategy | Gateway-side Validation | Prevents unauthenticated traffic from ever reaching internal business services |
| Rate Limiter | Redis Token Bucket | Distributed and scalable across multiple gateway instances |

---

> **Last Updated:** May 11, 2026  
