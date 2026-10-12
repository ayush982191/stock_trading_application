# Stock Trading Application - Production Debugging & Multi-Microservice Observability

This file defines the rules and patterns for diagnosing failures, tracking telemetry, and ensuring resilience across multiple microservices.

---

## 1. Multi-Microservice Observability & Production Debugging

When multiple microservices are involved, diagnose and isolate issues using unified telemetry and resilience patterns:

### 1.1 Distributed Tracing & Correlation IDs
1. **Trace Propagation:**
   - Every incoming request at `api-gateway` must be tagged with a unique `traceId` and `spanId` (Micrometer Tracing / OpenTelemetry / W3C Trace Context).
   - Propagate `traceId` across all synchronous HTTP calls (via `traceparent`, `X-Correlation-ID`) and asynchronous Kafka records (via Kafka record headers `traceparent`).
2. **MDC Injection:**
   - Automatically inject `traceId` and `spanId` into Logback's **MDC (Mapped Diagnostic Context)** so all log lines across every service share the identical trace identifier.
   - Use centralized log aggregators (Grafana Loki, ELK) to search by a single `traceId` to view the end-to-end user journey across all microservices.

### 1.2 Structured & Contextual Logging
1. **Domain Context:**
   - Always include key business identifiers in log entries: `userId`, `orderId`, `portfolioId`.
2. **Zero Sensitive Data (PII / Security):**
   - Never log raw passwords, tokens, full credit card/PAN numbers, or personal identifying information.
3. **Parameterized Logging:**
   - Use parameterized placeholders (`log.info("Order placed: id={}, user={}", orderId, userId)`), never string concatenation in logger calls.

### 1.3 Kafka Observability & Poison-Pill Handling
1. **Dead-Letter Topics (DLT):**
   - If a consumer fails to process a message after maximum retries, route it to a **Dead-Letter Topic (DLT)** (e.g. `user.registered.DLT`) with error details and stack trace preserved in the Kafka headers. Never drop messages silently.
2. **Consumer Idempotency:**
   - Consumers must enforce **idempotency** using the event's `eventId` (via a Redis TTL key or database deduplication table) to safely withstand Kafka message re-deliveries.

### 1.4 Health Checks & Microservice Metrics
1. **Spring Boot Actuator:**
   - Expose `/actuator/health` (with database and Kafka liveness/readiness indicators) and `/actuator/prometheus`.
2. **The 4 Golden Signals:**
   - Continuously monitor: Latency (p95/p99), Traffic (RPS), Errors (5xx error rates), and Saturation (HikariCP pool utilization, Kafka consumer group lag).

### 1.5 Cross-Service Resilience & Fault Isolation
1. **Circuit Breakers & Timeouts:**
   - Protect synchronous cross-service calls with **Circuit Breakers, Timeouts, and Bulkheads** (Resilience4j).
2. **Graceful Degradation:**
   - If an auxiliary service (like `notification-service`) degrades or goes offline, primary business transactions (`registration`, `order placement`) must not fail—degrade gracefully or queue asynchronously.
3. **Distributed Transaction Management (Saga Pattern):**
   - For multi-service transactional workflows, implement the **Saga Pattern** (orchestration or choreography with compensating events) rather than distributed locks.

---

## 2. Related References

- Core Architecture & Scaling Rules: [AGENTS.md](file:///c:/Users/Lumbini_User/Desktop/stock_trading_application/AGENTS.md)
- Registration API Scaling Spec: [REGISTRATION_API_SCALING_GUIDE.md](file:///c:/Users/Lumbini_User/Desktop/stock_trading_application/REGISTRATION_API_SCALING_GUIDE.md)
