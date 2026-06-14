# Inventory Management Platform

Full-stack, production-grade multi-tenant inventory management platform for small/mid-sized
retailers. Users register, create stores, manage a product catalog, track stock, record
sales/purchases/returns, and get automated low-stock and expiry alerts.

**Backend:** Java 21 · Spring Boot 3 &nbsp;|&nbsp; **Frontend:** React 19 · TypeScript

[![CI](https://github.com/Aryaman9/Intelligent-Inventory-Management/actions/workflows/ci.yml/badge.svg)](https://github.com/Aryaman9/Intelligent-Inventory-Management/actions/workflows/ci.yml) ![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-green) ![React](https://img.shields.io/badge/React-19-blue) ![TypeScript](https://img.shields.io/badge/TypeScript-5-blue) ![Postgres](https://img.shields.io/badge/PostgreSQL-14-blue) ![MongoDB](https://img.shields.io/badge/MongoDB-6-green) ![Redis](https://img.shields.io/badge/Redis-7-red)

---

## Architecture

```
React (Vite :5173 dev  /  nginx :80 prod)
    │  HTTP + JWT  (/api/v1 proxied to backend)
    ▼
Spring Boot (:3000)
  ├─ CorrelationIdFilter      →  MDC trace ID on every request
  ├─ JwtAuthenticationFilter  →  validates JWT, checks blacklist, sets MDC userId
  ├─ RateLimitFilter          →  per-tier Redis counter (FREE/PRO/ENTERPRISE)
  └─ IdempotencyFilter        →  requires Idempotency-Key on transaction writes

Services (@Transactional)
  └─ ApplicationEventPublisher  (after-commit, async)
       ├─ SaleCompletedEvent      →  LowStockAlertListener / ExpiryAlertListener
       ├─ InventoryUpdatedEvent   →  CacheInvalidationListener
       └─ UserActionAuditEvent    →  AuditLogListener

PostgreSQL  — users, stores, inventory (@Version), transactions, audit log
MongoDB     — product catalog (flexible schema: variants, attributes)
Redis       — JWT blacklist, idempotency store, inventory cache, alert TTLs, rate-limit counters

Observability
  ├─ Prometheus metrics via Micrometer  →  /actuator/prometheus
  ├─ Structured JSON logs (traceId + userId in every line)
  └─ Swagger UI                          →  /swagger-ui.html
```

The split between PostgreSQL and MongoDB is intentional: relational data (stores, stock levels,
transactions) lives in Postgres with foreign-key constraints; product definitions go to MongoDB
because attributes vary significantly by category (pharmaceuticals have prescription flags,
perishables have shelf life, etc.).

---

## Key SDE-2 Features

| Feature | What It Does | Why It Matters |
|---|---|---|
| **Optimistic Locking** | `@Version` on inventory + `@Retryable` (3 attempts, backoff) | Prevents negative stock under concurrent sales |
| **Idempotency** | Redis-backed dedup on all transaction mutations, persisted in `afterCommit()` | Prevents duplicate transactions on network retry |
| **Event-Driven Alerts** | `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` | Decoupled alert pipeline (low stock, expiry) |
| **Circuit Breaker** | Resilience4j around MongoDB product lookups | Sale continues with a fallback product name when Mongo is down |
| **Rate Limiting** | Per-user, per-tier Redis counters (FREE/PRO/ENTERPRISE) | Protects the backend from abuse; returns 429 + `Retry-After` |
| **Cache Stampede Guard** | Redis lock on cache miss in `CacheService.getOrLoad` | Prevents a thundering herd on hot keys |
| **Structured Observability** | JSON logs + Prometheus metrics + trace IDs | Production monitoring ready |
| **JWT with Blacklist** | Logout/refresh-rotation blacklist token JTIs in Redis | Logout is effective immediately, not at token expiry |

---

## Tech Stack

**Backend**: Java 21, Spring Boot 3.3, PostgreSQL 14, MongoDB 6, Redis 7, Flyway, MapStruct,
Resilience4j, JJWT 0.12, Micrometer/Prometheus, Logstash JSON logging, Testcontainers, RestAssured.

**Frontend**: React 19, TypeScript, Vite, Tailwind CSS v4, Radix UI, TanStack Query v5,
React Hook Form + Zod, Recharts.

---

## Quick Start

### Prerequisites
- Java 21
- Node.js 20+
- Docker & Docker Compose
- Maven (or use the included `./mvnw` wrapper)

### Option 1 — Local Development (recommended)

```bash
# 1. Start databases (Postgres + MongoDB + Redis)
docker-compose up -d

# 2. Copy env defaults (sets DB password + JWT secrets for local use)
cp .env.example .env

# 3. Run the backend (port 3000)
./mvnw spring-boot:run

# 4. In another terminal — run the frontend (port 5173)
cd frontend
npm install
npm run dev
```

Open <http://localhost:5173> — the Vite dev server proxies `/api` calls to the backend.

### Option 2 — Full Docker

```bash
docker-compose --profile full up -d
```

This builds and runs everything: frontend on **port 80**, backend on **3000**, plus all three
databases. (The plain `docker-compose up -d` above starts only the databases, for local dev.)

### Run Tests

```bash
# Backend unit + integration tests (Docker must be running for Testcontainers)
./mvnw verify

# Frontend type-check + build
cd frontend && npm run build
```

### Endpoints
- **Frontend**: <http://localhost:5173> (dev) or <http://localhost:80> (Docker)
- **API**: <http://localhost:3000/api/v1/>
- **Swagger UI**: <http://localhost:3000/swagger-ui.html>
- **Health**: <http://localhost:3000/actuator/health>
- **Metrics**: <http://localhost:3000/actuator/prometheus>

---

## Screenshots

> _Add screenshots of the Dashboard, Inventory page, Sale form with invoice, and Alerts page here._

---

## API Overview

All endpoints are under `/api/v1`. Every response uses the same envelope:

```json
{ "success": true,  "message": "...", "data": { ... }, "correlationId": "...", "timestamp": "..." }
{ "success": false, "error": "...", "errorCode": "INV_003", "correlationId": "...", "timestamp": "..." }
```

Paginated responses include a `pagination` object with `total`, `page`, `limit`, and `pages`.

| Module | Endpoints |
|---|---|
| Auth | `POST /auth/register` · `POST /auth/login` · `POST /auth/refresh` · `GET /auth/me` · `POST /auth/logout` |
| Stores | `GET\|POST /stores` · `GET\|PATCH\|DELETE /stores/{id}` · `GET /stores/stats` |
| Products | `GET\|POST /products` · `GET\|PATCH\|DELETE /products/{id}` · `GET /products/categories` |
| Inventory | `POST /inventory` · `GET\|PATCH\|DELETE /inventory/{id}` · `GET /inventory/store/{storeId}` · `GET /inventory/alerts` · `GET /inventory/stats/{storeId}` |
| Transactions | `POST /transactions/sale` · `/purchase` · `/return` · `GET /transactions/store/{storeId}` · `GET /transactions/stats/{storeId}` |

All transaction writes require an `Idempotency-Key` header. Every authenticated response carries
`X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset`; 429 responses also carry
`Retry-After`. The OpenAPI document is served live at `/v3/api-docs`.

---

## Testing

Five named integration tests (Testcontainers + RestAssured) prove the SDE-2 patterns end-to-end:

1. **`SaleTransactionConcurrencyTest`** — 20 concurrent threads selling from 100 units; optimistic
   locking yields exactly 10 successful sales and a final quantity of exactly 0, never negative.
2. **`IdempotencyIntegrationTest`** — the same sale sent 5× with one key produces exactly 1
   transaction and an identical business payload each time.
3. **`RateLimitIntegrationTest`** — a FREE-tier user exceeding the write limit receives 429 with a
   `Retry-After` header.
4. **`AlertPipelineIntegrationTest`** — a sale that drops stock below threshold surfaces a low-stock
   alert (Awaitility for the async pipeline).
5. **`AuthFlowIntegrationTest`** — register → login → `/me` → logout → `/me` returns 401 (blacklisted),
   plus account lockout and refresh-token rotation.

Service-layer unit tests (`AuthServiceTest`, `TransactionServiceTest`, `InventoryServiceTest`) cover
registration/login/lockout, sale happy-path/insufficient-stock/ownership/idempotency, and inventory
creation/ownership/stats.

```bash
./mvnw verify   # runs all unit + integration tests; Docker required for Testcontainers
```

---

## Frontend

The React frontend provides:

- **Auth** — login/register with JWT management and automatic refresh-token rotation
- **Store Management** — create, edit, and manage multiple stores
- **Product Catalog** — full product CRUD with search and category filtering
- **Inventory** — per-store stock management with visual low-stock indicators
- **Sales & Purchases** — forms that generate an `Idempotency-Key` so retries are safe
- **Alerts Dashboard** — real-time low-stock and expiry alerts
- **Analytics** — revenue charts, payment breakdowns, and profit margins

---

## Architecture Decision Records

See [`docs/adr/`](docs/adr/):

- [ADR-001 — Optimistic Locking over Pessimistic](docs/adr/001-optimistic-locking.md)
- [ADR-002 — Idempotency Strategy](docs/adr/002-idempotency-strategy.md)
- [ADR-003 — Event-Driven Alerts vs Polling](docs/adr/003-event-driven-alerts.md)

---

## Environment Variables

| Variable | Purpose |
|---|---|
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `SPRING_DATASOURCE_URL` | JDBC URL (override for Docker networking) |
| `MONGO_URI` | Full MongoDB connection URI |
| `REDIS_HOST` / `REDIS_PORT` | Redis host and port |
| `JWT_ACCESS_SECRET` | HMAC-SHA256 signing key for access tokens (≥32 chars) |
| `JWT_REFRESH_SECRET` | HMAC-SHA256 signing key for refresh tokens (≥32 chars) |

See [`.env.example`](.env.example) for local defaults.
