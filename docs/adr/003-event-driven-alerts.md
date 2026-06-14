# ADR-003: Event-Driven Alerts via Spring Application Events

## Status: Accepted

## Context
When inventory drops below a threshold, users need alerts. Options: poll the
database on a schedule, or react to inventory changes in real time.

## Decision
Use Spring's `ApplicationEventPublisher` with `@TransactionalEventListener`
(AFTER_COMMIT) and `@Async` processing. Events are published after the sale
transaction commits, and listeners record alert state and metrics. The
`/inventory/alerts` endpoint computes the authoritative alert list directly from
PostgreSQL, so an alert is correct the instant the transaction commits, while the
async listeners handle the side effects (metrics, cache warming).

## Consequences
- **Pro**: Near-real-time alerts (milliseconds after a sale).
- **Pro**: Decoupled — `TransactionService` doesn't know about alerts.
- **Pro**: Async — alert side effects don't slow down the sale response.
- **Pro**: Proven by `AlertPipelineIntegrationTest` — a sale that crosses the
  threshold makes the low-stock alert appear.
- **Con**: In-process events are not durable. If the app crashes between commit
  and listener execution, the side effect is lost. Acceptable here — the
  authoritative alert is recomputed from Postgres on read.

## Alternatives Considered
- **Scheduled polling**: Simpler, but introduces latency (up to the poll interval)
  and wastes resources scanning unchanged rows.
- **Message broker (Kafka/RabbitMQ)**: Durable and scalable, but overkill for a
  single-instance retail app. Would be the right choice at scale.
