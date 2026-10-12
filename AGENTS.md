# Stock Trading Application - Core Architecture & Engineering Standards

This file defines the foundational architecture, security, and scaling standards across all services.

---

## 1. Project Context & Scale Target

- **Domain:** Stock Trading & Investment Platform (Microservices Architecture).
- **Target Scale:** **1,000,000+ Concurrent/Active Users**.
- **Microservices:**
  - `user-service`: Authentication, User Management, KYC & Onboarding.
  - `order-service`: Order matching, placement, and trade execution.
  - `portfolio-service`: Holdings, asset tracking, and valuation.
  - `market-data-service`: Real-time stock prices and market feeds.
  - `notification-service`: Email/SMS alerts and push notifications.
  - `api-gateway`: Routing, SSL termination, and rate limiting.

---

## 2. Core Engineering Standards

### 2.1 Financial Integrity & Security
1. **No Client-Controlled Balances:**
   - Never allow incoming request DTOs (`RegisterRequest`, etc.) to set wallet balance, initial deposit, or funds directly.
   - All balance changes must originate from an audited ledger transaction or verified payment gateway event.
2. **Password Security:**
   - Always hash passwords using `PasswordEncoder` (`BCryptPasswordEncoder` with strength $\ge 12$).
   - Never log, expose, or return raw or hashed passwords in DTOs or logs.
3. **Data Sanitization:**
   - Always normalize emails (`trim().toLowerCase(Locale.ROOT)`).
   - Enforce strict validation via Jakarta Bean Validation (`@Valid`, `@Email`, `@NotBlank`, regex pattern matching).

### 2.2 Concurrency & Database Design (1M+ Scale)
1. **TOCTOU Race Condition Prevention:**
   - Do not rely solely on Java-level checks like `if (repository.existsByEmail(...))`.
   - Always back uniqueness checks with database-level `UNIQUE` constraints and catch `DataIntegrityViolationException`.
2. **Primary Key Index Performance:**
   - Avoid standard random UUID v4 for high-insert tables due to B-Tree fragmentation. Use time-ordered **UUID v7** or **TSID**.
3. **Connection Pool Safety:**
   - Keep DB transactions short. Never perform network calls (HTTP, Kafka, email) inside active DB transactions without careful outbox isolation.

### 2.3 Distributed Systems & Kafka Messaging
1. **Dual-Write Protection (Transactional Outbox Pattern):**
   - Avoid direct `userRepository.save()` followed by `kafkaTemplate.send()` in the same method. If Kafka fails or DB rolls back, state becomes inconsistent.
   - Use the **Transactional Outbox Pattern** to guarantee atomic persistence and at-least-once message delivery.
2. **Strongly-Typed Events:**
   - Never use `Map<String, Object>` for Kafka payloads. Use versioned event records/DTOs (e.g., `UserRegisteredEvent`).
   - Include `eventId`, `timestamp`, and idempotency keys so downstream microservices can safely de-duplicate messages.

### 2.4 Error Handling & API Contracts
1. **No Generic Exceptions:**
   - Never throw raw `RuntimeException` or `Exception`.
   - Use specific domain exceptions (e.g., `UserAlreadyExistsException`, `InsufficientBalanceException`).
2. **RFC 7807 `ProblemDetail`:**
   - Handle all exceptions in centralized `@RestControllerAdvice` classes returning standard Spring 6 `ProblemDetail` responses.

### 2.5 Rate Limiting & User Lifecycle
1. **Gateway Protection:**
   - Public endpoints (`/register`, `/login`) must be rate-limited using Redis token bucket or Bucket4j.
2. **User Lifecycle:**
   - New accounts must start in `PENDING_VERIFICATION` status until OTP/Email verification and KYC checks pass.

---

## 3. Related References

- Production Debugging & Observability: [GEMINI.md](file:///c:/Users/Lumbini_User/Desktop/stock_trading_application/GEMINI.md)
- Registration API Scaling Spec: [REGISTRATION_API_SCALING_GUIDE.md](file:///c:/Users/Lumbini_User/Desktop/stock_trading_application/REGISTRATION_API_SCALING_GUIDE.md)
