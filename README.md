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
- ✅ Global Exception Handling
- 🔜 CORS Policy configuration
- 🔜 Logging & Metrics integration

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
| Build Tool | Maven | 3.8+ |

---

## 🔐 Security & Filters

### Authentication Filter
The Gateway intercepts every request and:
1.  Checks if the path is secured (via `RouterValidator`).
2.  Extracts the `Authorization` header (Bearer token).
3.  Calls `user-service/api/v1/auth/validate` to verify the token.
4.  Injects `loggedInUser` (ID) and `role` headers for downstream services.

### Rate Limiting
Uses the **Token Bucket** algorithm via Redis:
-   **User Service**: 10 req/s (Replenish Rate), 20 (Burst Capacity).
-   **Candidate Service**: 5 req/s (Replenish Rate), 10 (Burst Capacity).

---

## 📦 Project Structure

```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/voting/gateway/
│   │   │   ├── ApiGatewayApplication.java
│   │   │   ├── config/              ← Security & Rate Limit Config
│   │   │   ├── dto/                 ← API Response DTOs
│   │   │   └── docs/                ← Implementation Plan
│   │   └── resources/
│   │       └── application.yml      ← Route & Redis Config
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

---

## 📋 Implementation Progress

> Detailed checklist: [IMPLEMENTATION_PLAN.md](src/main/java/com/voting/gateway/docs/IMPLEMENTATION_PLAN.md)

| Task | Description | Status |
|------|-------------|--------|
| 2.3 | API Gateway Setup | ✅ Done |
| 2.4 | JWT Validation Filter | ✅ Done |
| 2.5 | Rate Limiting | ✅ Done |

---

## 📝 License

This project is licensed under the MIT License.

---

> **Maintainer:** Vaibhav Jain  
> **Last Updated:** May 11, 2026
