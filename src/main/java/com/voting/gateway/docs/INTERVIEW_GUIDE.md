# Interview Guide: API Gateway

### Q1: What are the primary functions of our API Gateway?
**Answer:** It acts as the "Front Door" for the voting system. Key responsibilities include:
- **Routing**: Forwarding requests to `user-service` or `candidate-service` based on path predicates.
- **Authentication**: Centralized JWT validation using the `AuthenticationFilter`.
- **Rate Limiting**: Protecting services from traffic spikes using Redis.

### Q2: Explain the `AuthenticationFilter` implementation.
**Answer:** It is a custom `AbstractGatewayFilterFactory` that:
1.  Uses `RouterValidator` to identify if a path is secured.
2.  Extracts the JWT from the `Authorization` header.
3.  Calls the `user-service`'s `/validate` endpoint using **WebClient**.
4.  **Header Propagation**: If valid, it injects the user's ID (`loggedInUser`) and `role` into the headers of the request before forwarding it downstream.

### Q3: How is Rate Limiting handled in our gateway?
**Answer:** We use the `RequestRateLimiter` filter with a **Token Bucket algorithm** backed by **Redis**.
- **Replenish Rate**: The steady rate at which tokens are added back (e.g., 10 req/s).
- **Burst Capacity**: The maximum number of tokens the bucket can hold for short traffic spikes.

### Q4: What is the `KeyResolver` and why is it important?
**Answer:** The `KeyResolver` determines the "identity" of the bucket. Our `userKeyResolver` identifies users by:
1.  The `loggedInUser` header (if authenticated).
2.  The **Client IP Address** (as a fallback for anonymous requests).
This ensures that rate limits are applied per user rather than globally across all traffic.

### Q5: Why did we choose Redis for Rate Limiting?
**Answer:** Redis provides a **Distributed State**. If we scale to multiple instances of the API Gateway, they all share the same rate-limit counters in Redis. Without Redis, a user could bypass the limit by hitting different gateway instances.

### Q6: How do you handle CORS in Spring Cloud Gateway?
**Answer:** We configure CORS globally in the `application.yml` or via a `CorsWebFilter` bean. This allows us to define which origins (e.g., our frontend) can access our APIs, handling the "Pre-flight" OPTIONS requests centrally.

### Q7: What happens if a downstream service is slow? (Circuit Breakers)
**Answer:** We use **Resilience4j** to implement Circuit Breakers. If a service like `candidate-service` fails or times out repeatedly, the circuit "opens." The Gateway then stops calling the service and returns a fallback response, saving system resources and preventing a total crash.

### Q8: How do you handle SSL/TLS termination at the Gateway?
**Answer:** The Gateway is typically the only component exposed to the public internet. We terminate SSL/TLS at the Gateway (decryption) and then communicate with internal microservices over HTTP (within the private VPC) to reduce the overhead of encryption/decryption for every internal hop.

### Q9: Can the API Gateway handle Log Aggregation?
**Answer:** While the Gateway shouldn't *store* logs, it is the perfect place to inject a **Correlation ID** (Trace ID) into every request. This ID is passed to all downstream services, allowing us to trace a single request's journey through multiple services in tools like **ELK Stack** or **Zipkin/Sleuth**.

### Q10: What is Dynamic Routing in Spring Cloud Gateway?
**Answer:** Dynamic routing allows us to update routing rules without restarting the Gateway. This can be achieved by using a `RouteDefinitionLocator` that pulls rules from a database, Redis, or a configuration server (like Spring Cloud Config).

---

## 📊 Observability & System Health

### Q11: How do you monitor the health of the Gateway?
**Answer:** We use **Spring Boot Actuator** which provides `/health`, `/metrics`, and `/info` endpoints. These are scraped by **Prometheus** and visualized in **Grafana** to track request throughput, latency, and error rates (4xx/5xx) in real-time.

### Q12: Why use `WebClient` instead of `RestTemplate` in the Gateway?
**Answer:** `RestTemplate` is blocking and synchronous (one thread per request). `WebClient` is **non-blocking and asynchronous**, which is essential for a Gateway to handle thousands of concurrent connections efficiently without being limited by the size of the thread pool.
