# Inventory Management Platform

A full-stack inventory management system for small/mid-sized retailers. Users register, create stores, manage a product catalog, track stock levels, record sales/purchases/returns, and get automated alerts for low stock and expiring items.

**Backend:** Spring Boot 3 · Java 21 · PostgreSQL · MongoDB · Redis  
**Frontend:** React 19 · TypeScript · Vite · TanStack Query · Tailwind CSS

---

## Running Locally

**Prerequisites:** Docker, Java 21, Node 20+

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Start backend (port 3000)
./mvnw spring-boot:run

# 3. Start frontend (port 5173)
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`, register an account, and start from there.

---

## Architecture

```
React (Vite :5173)
    │  HTTP + JWT
    ▼
Spring Boot (:3000)
  ├─ CorrelationIdFilter  →  MDC trace ID on every request
  ├─ JwtAuthenticationFilter
  └─ IdempotencyFilter    →  deduplicates POST/PUT/PATCH via Redis

Services (@Transactional)
  └─ ApplicationEventPublisher
       ├─ InventoryUpdatedEvent  →  AlertListener (async)
       ├─ SaleCompletedEvent     →  CacheInvalidationListener (async)
       └─ UserActionAuditEvent   →  AuditLogListener (async)

PostgreSQL  — users, stores, inventory, transactions, audit log
MongoDB     — product catalog (flexible schema: variants, attributes)
Redis       — JWT blacklist, idempotency store, inventory cache, alert TTLs
```

The split between PostgreSQL and MongoDB is intentional: relational data (stores, stock levels, transactions) lives in Postgres with foreign key constraints; product definitions go to MongoDB because attributes vary significantly by category (pharmaceuticals have prescription flags, perishables have shelf life, etc.).

---

## Notable Implementation Details

**Optimistic locking with retry** — Inventory rows carry a `@Version` column. `recordSale` and `recordPurchase` are annotated with `@Retryable(ObjectOptimisticLockingFailureException, maxAttempts=3)` so concurrent writes on the same SKU back off and retry rather than failing immediately.

**Idempotency for financial operations** — Every sale, purchase, and return requires an `Idempotency-Key` header. The key is checked before any DB write; on a replay the cached response is returned as-is. The key is persisted to Redis inside `afterCommit()` so a mid-flight crash can't produce a committed transaction with no idempotency record.

**Cache stampede protection** — `CacheService.getOrLoad` acquires a Redis lock on cache miss so only one thread fetches from the source; other threads poll until the value is warm rather than all hitting Postgres simultaneously.

**Circuit breaker** — MongoDB product lookups go through a Resilience4j circuit breaker (`mongoProducts`). When the circuit is open, transactions fall back to "Unknown Product" for the name field and continue rather than failing the sale.

**JWT with blacklist** — Logout adds the access token to a Redis set (TTL matching remaining token validity). The JWT filter checks this set before trusting any token, so logout is effective immediately without waiting for token expiry.

**Event-driven audit log** — Every user action publishes a `UserActionAuditEvent` consumed asynchronously by `AuditLogListener`. Async consumption means a slow audit write can never block a sale.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Databases | PostgreSQL 14, MongoDB 6, Redis 7 |
| ORM | Spring Data JPA + Hibernate 6 |
| Security | Spring Security 6, JJWT 0.12 |
| Resilience | Resilience4j (circuit breaker + retry) |
| Migrations | Flyway |
| Mapping | MapStruct 1.5 |
| Frontend | React 19, TypeScript, Vite, TanStack Query v5 |
| Forms | React Hook Form + Zod |
| Styling | Tailwind CSS v4, Radix UI |
| Containers | Docker Compose (Postgres + MongoDB + Redis) |

---

## API

All endpoints are under `/api/v1`. The server runs on port `3000`.

Every response follows the same envelope:

```json
{ "success": true, "message": "...", "data": { ... } }
{ "success": false, "error": "...", "errorCode": "INV_001", "correlationId": "...", "timestamp": "..." }
```

Paginated responses include a `pagination` object with `total`, `page`, `limit`, and `pages`.

Key endpoint groups:
- `POST /api/v1/auth/register` — `POST /api/v1/auth/login` — `POST /api/v1/auth/logout`
- `GET|POST|PUT|DELETE /api/v1/stores`
- `GET|POST|PUT|DELETE /api/v1/products`
- `GET|POST|PUT|DELETE /api/v1/inventory`
- `POST /api/v1/transactions/sale` — `/purchase` — `/return`
- `GET /api/v1/transactions` — `/stats`
- `GET /api/v1/alerts`

---

## Environment Variables

The application reads configuration from environment variables with fallback defaults for local development. For any non-local environment set these explicitly:

| Variable | Purpose |
|---|---|
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `MONGO_URI` | Full MongoDB connection URI |
| `REDIS_HOST` | Redis hostname |
| `REDIS_PORT` | Redis port |
| `JWT_ACCESS_SECRET` | HMAC-SHA256 signing key for access tokens (≥32 chars) |
| `JWT_REFRESH_SECRET` | HMAC-SHA256 signing key for refresh tokens (≥32 chars) |
