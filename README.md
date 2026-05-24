# Notification Orchestration Engine

A centralized, **asynchronous**, multi-tenant notification delivery engine that runs on EC2, authenticates the users using JWT, and delivers notification via email.

It also handles idempotency of notifications, and retries failed notifications at regular intervals.

## Live Demo

**Swagger UI:** http://3.111.159.59:8080/swagger-ui/index.html

**Postman Collection:** [Run in Postman](https://mayyaannkk-101309.postman.co/workspace/Mayank-Pant's-Workspace~bcb113d5-1682-4009-8cf2-71caba4a08a8/collection/47658753-3aef6fc5-4f20-4b2e-9594-c1769e7d5af0?action=share&source=copy-link&creator=47658753)

> Import the collection, run Login first to get the token automatically saved, then run any other request.

> Note: The API returns `QUEUED` status — the notification is accepted and published to Kafka. Delivery happens asynchronously via the EmailWorker consumer.

Login:
```bash
curl -X POST http://3.111.159.59:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "testpass123"}'
```

Send notification:
```bash
curl -X POST http://3.111.159.59:8080/api/v1/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{                
    "channel": "EMAIL",                         
    "recipient": "email@example.com",
    "subject": "Live from AWS EC2",                                                
    "body": "This notification was sent from a Spring Boot app running on AWS EC2."
  }'
```

***
## Why I Built It This Way

### Phase 1 — Synchronous Foundation
The first version was intentionally synchronous. Before introducing any async complexity,
I wanted to establish the complete delivery pipeline end to end — controller to orchestrator
to database to email provider — and understand the failure modes at each step.

This gave clarity on three things: what the notification lifecycle looks like
(PENDING → SENT/FAILED), how retries should work when delivery fails, and where
the natural boundaries between modules should be. A `RetryScheduler` polls the
database every 60 seconds for FAILED notifications and retries up to 3 times
before marking them DEAD.

The application also supports multi-tenant idempotent delivery from the start —
each notification carries an optional idempotency key. If the same key is seen
twice, the duplicate is detected before any processing happens, preventing
double sends during client retries or network failures.

### Phase 2 — Async Delivery with Kafka
Phase 1 had one clear problem: the HTTP request blocked until the email was
delivered. If Gmail was slow or temporarily down, the caller waited. At scale,
this ties up threads and degrades the entire system.

Phase 2 replaced the synchronous send with a Kafka topic. The orchestrator
now publishes the notification to `notifications.email` and returns `QUEUED`
immediately — the HTTP request completes in milliseconds regardless of email
provider performance. A separate `EmailWorker` consumer picks up the message
and handles delivery asynchronously.

This also gave a better retry mechanism. Instead of a polling scheduler,
Kafka redelivers unconsumed messages automatically. Failed messages after
3 redeliveries go to a Dead Letter Queue (`notifications.dlq`) for manual
inspection.

The before/after is demonstrable — in Phase 1 the HTTP thread and the email
send thread were the same. In Phase 2 they are different threads entirely,
visible in the application logs.

### Phase 3 — Rate Limiting with Redis
Phase 2 introduced a new problem: a single tenant could now fire thousands of
requests per second — each one accepted, saved to the database, and published
to Kafka — overwhelming the system and degrading service for everyone else.

A database counter was not the right solution. Every rate limit check would require
a database query with locking, adding latency to every single request and creating
a new bottleneck. The rate limiter needed to be faster than the thing it was protecting.

Phase 3 adds a sliding window rate limiter using Redis sorted sets. Redis operates
in microseconds with atomic operations — no locking, no disk I/O. Each tenant gets
an independent counter per channel. The rate limit check happens as the very first
operation in the orchestrator — before idempotency check, before database write,
before Kafka publish. If the limit is exceeded, the request is rejected immediately
with 429 Too Many Requests without touching any other system.

The sliding window avoids the burst problem of fixed windows — there is no boundary
moment where a tenant can double their rate by timing requests at a window reset.

***
## Architecture

### Request Flow
1. Client authenticates via `POST /api/v1/auth/login` and receives a JWT token
2. Client sends notification request to `POST /api/v1/notifications` with Bearer token
3. Spring Security filter validates the JWT and sets the tenant identity
4. `NotificationOrchestrator` checks rate limit via Redis — rejects with 429 if exceeded
5. Idempotency key checked — rejects with SKIPPED if duplicate detected
6. Notification saved to PostgreSQL as `PENDING`
7. Orchestrator publishes to Kafka topic `notifications.email` and returns `QUEUED` immediately
8. `EmailWorker` consumer picks up the message asynchronously and calls Gmail SMTP
9. Status updated to `SENT` or `FAILED` in PostgreSQL
10. `RetryScheduler` polls every 60 seconds for `FAILED` notifications and retries up to 3 times before marking `DEAD`

***
## Notification Status Lifecycle

The notification status works like a finite state machine and moves from PENDING to SENT/DEAD in a predictable manner:
- PENDING → SENT        (successful delivery)
- PENDING → FAILED      (delivery failed, eligible for retry)
- FAILED  → SENT        (retry succeeded)
- FAILED  → FAILED      (retry failed, retryCount incremented)
- FAILED  → DEAD        (retryCount reached maxAttempts, no more retries)
- PENDING → SKIPPED     (duplicate idempotency key detected)

***
## Tech Stack

| Technology | Why |
|-----------|-----|
| Spring Boot 3.2 | Industry standard for Java REST APIs, production-grade auto-configuration |
| Spring Security + JWT | Stateless authentication — no server-side sessions, scales horizontally |
| PostgreSQL | Reliable relational DB with JSONB support for flexible payloads |
| Flyway | Version-controlled database migrations — schema changes tracked in Git |
| Docker | Consistent environment across development and production |
| AWS EC2 | Cloud deployment with auto-restart on reboot |
| Testcontainers | Integration tests against real PostgreSQL — no in-memory substitutes |
| Apache Kafka | Async message queue — decouples HTTP request from email delivery |
| Redis | Per-tenant sliding window rate limiter — microsecond latency, atomic ZSET operations |


***
## Modules

- auth - handles generating JWT token, validating it, and setting the security context
- api - handles user requests, validates the request body, and sends the request to the orchestrator
- core - handles the core business logic and orchestrates the application by calling the different modules methods
- persistence - handles the database interaction and provides methods to call from anywhere from your code 

***
## Running Locally

Steps:
1. Clone the repository
2. Make sure Docker Desktop is running
3. Set environment variables (see `.env.example`)
4. Start infrastructure: `docker-compose up -d`
5. Run the app: `mvn spring-boot:run -pl api --also-make`
6. API available at `http://localhost:8080`
7. Swagger UI at `http://localhost:8080/swagger-ui/index.html`

***
## API Reference

### POST /api/v1/auth/login

Get a JWT token:

**Request:**
```json
{
  "username": "testuser",
  "password": "testpass123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

### POST /api/v1/notifications

Send a notification. Requires Bearer token:

**Request:**
```json
{
  "channel": "EMAIL",
  "recipient": "user@example.com",
  "subject": "Optional subject",
  "body": "Notification body",
  "idempotencyKey": "optional-unique-key"
}
```

**Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "status": "QUEUED",
  "message": "Notification accepted and queued for delivery",
  "createdAt": "2026-05-22T16:58:40Z"
}
```

***
## Roadmap

### ✅ Phase 2 — Asynchronous Delivery with Kafka (complete)
Phase 1 processes notifications synchronously — the HTTP request waits while the
email sends. This works but has a problem: if the email provider is slow or down,
the caller is blocked.

Phase 2 replaces the synchronous send with a Kafka topic. The orchestrator publishes
the notification to `notifications.email` and returns immediately. A separate consumer
picks it up and attempts delivery. The caller gets a response in milliseconds regardless
of email provider performance.

Changes:
- `kafka` module: KafkaProducer, KafkaConsumer, topic configuration
- Orchestrator: publishes to Kafka instead of calling EmailSender directly
- DLQ (Dead Letter Queue): failed messages after 3 retries go to `notifications.dlq`
- RetryScheduler: replaced by Kafka retry with exponential backoff

### ✅ Phase 3 — Rate Limiting with Redis (complete)
Without rate limiting, a single tenant can flood the system with thousands of
notifications per second, degrading service for everyone else.

Phase 3 adds a sliding window rate limiter using Redis sorted sets. Each tenant
gets a configurable limit per minute per channel (default: 10). Requests exceeding
the limit receive a 429 Too Many Requests response immediately — before any
database or Kafka interaction happens.

The sliding window algorithm uses Redis ZSET operations:
- Remove entries older than 60 seconds
- Count remaining entries
- If count >= limit → reject with 429
- Otherwise → add entry and accept

Changes:
- `cache` module: RedisRateLimiter using ZSET sliding window algorithm
- Orchestrator: rate limit check as first operation before any processing
- Per-tenant, per-channel configurable limits via app.rate-limit.max-per-minute