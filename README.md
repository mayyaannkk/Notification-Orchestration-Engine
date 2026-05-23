# Notification Orchestration Engine

A centralized, synchronous, multi-tenant notification delivery engine that runs on EC2, authenticates the users using JWT, and delivers notification via email.

It also handles idempotency of notifications, and retries failed notifications at regular intervals.

## Live Demo

**Swagger UI:** http://3.111.159.59:8080/swagger-ui/index.html


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
Phase 1 is intentionally synchronous — the goal was to establish the complete delivery pipeline, understand the failure modes, and build a solid retry mechanism before introducing asynchronous complexity. 

Later on we will move towards Kafka to decouple the request from the backend, so the user can fire-and-forget and the system will process it at its own pace.

The application also supports multi-tenant idempotent notification delivery, meaning that multiple users can access it, but each notification will only be sent once. The application handles duplicate requests via idempotencyKey.

The application, at regular intervals, also tries to retry failed delivery by fetching them from the database. The retry logic happens for each FAILED notification until the max retry count, and after that the notification goes to a DEAD state.

***
## Architecture
The application takes in the request, the controller takes it, validates the request and diverts it to the orchestrator which then first saves the notification in the DB with a state of PENDING.

It then tries to send the notification via the specified channel, if successful, it will update the DB notification status to SENT. Else it will update to FAILED.

This ensures that the notification is saved in the DB first, so even if the delivery fails, our scheduled retry will pick the FAILED ones and try to run them again.

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
4. Start the database: `docker-compose up -d postgres`
5. Run the app: `mvn spring-boot:run -pl api --also-make`
6. API available at `http://localhost:8080`
7. Swagger UI at `http://localhost:8080/swagger-ui/index.html`
8. Swagger UI (live): `http://3.111.159.59:8080/swagger-ui/index.html`

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
  "status": "SENT",
  "message": "Notification sent successfully",
  "createdAt": "2026-05-22T16:58:40Z"
}
```

***
## Roadmap

### Phase 2 — Asynchronous Delivery with Kafka
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

### Phase 3 — Rate Limiting with Redis
Without rate limiting, a single tenant can flood the system with thousands of
notifications per second, degrading service for everyone else.

Phase 3 adds a sliding window rate limiter using Redis sorted sets. Each tenant gets
a configurable limit per minute per channel. Requests exceeding the limit receive
a 429 Too Many Requests response immediately.

Changes:
- `cache` module: RedisRateLimiter using ZSET sliding window algorithm
- Orchestrator: rate limit check before processing
- Per-tenant, per-channel configurable limits