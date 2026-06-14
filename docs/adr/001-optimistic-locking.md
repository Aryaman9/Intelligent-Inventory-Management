# ADR-001: Optimistic Locking for Inventory Concurrency

## Status: Accepted

## Context
Multiple users can sell from the same inventory simultaneously. We need to prevent
negative stock without severely limiting throughput.

## Decision
Use JPA `@Version` (optimistic locking) with Spring Retry (`@Retryable`) instead
of pessimistic locking (`SELECT FOR UPDATE`). `recordSale`, `recordPurchase`, and
`recordReturn` retry up to 3 times with exponential backoff on
`ObjectOptimisticLockingFailureException`, and fall back to a `@Recover` method that
surfaces a 409 Conflict if retries are exhausted.

## Consequences
- **Pro**: No lock contention — reads are never blocked. Under typical retail
  workloads (low collision rate), this is faster than pessimistic locking.
- **Pro**: Proven by `SaleTransactionConcurrencyTest` — 20 concurrent threads,
  exactly 10 successful sales from 100 units, final quantity exactly 0, zero
  negative stock.
- **Con**: Under extreme contention, retries add latency. Acceptable for this
  domain (retail POS, not a stock exchange).
- **Con**: Requires `@Retryable` with a `@Recover` fallback, adding complexity.

## Alternatives Considered
- **Pessimistic locking**: Simpler, but blocks all concurrent reads/writes on
  the same row. Unacceptable for a multi-store system.
- **Database-level advisory locks**: PostgreSQL-specific, harder to test.
