# ADR-002: Client-Supplied Idempotency Keys

## Status: Accepted

## Context
Network failures between client and server can cause duplicate sale submissions.
In a payment-adjacent system, duplicate transactions are the #1 bug source.

## Decision
Require an `Idempotency-Key` header (client-generated UUID) on all mutating
transaction endpoints. The key is checked before any business logic runs; the
response is stored in Redis with a 24h TTL inside `afterCommit()` so the record
can never exist without a committed transaction. On a duplicate key, the cached
response is replayed.

## Consequences
- **Pro**: Client retries are safe — the same key always returns the same result.
- **Pro**: Simple to implement with Redis — no complex distributed locking.
- **Pro**: Proven by `IdempotencyIntegrationTest` — 5 identical requests produce
  exactly 1 database transaction and an unchanged-after-first inventory level.
- **Con**: Requires client cooperation (must send the header). Documented in the API
  and enforced by `IdempotencyFilter` (400 if the header is missing).
- **Con**: 24h TTL means keys expire — extremely delayed retries won't be caught.

## Notes
The idempotency record is keyed by scope + key (`idem:sale:<key>`) and caches the
business payload (`SaleResponse`). The HTTP envelope (`timestamp`, `correlationId`)
is regenerated per request by design, so replays carry fresh envelope metadata while
the business result is identical.

## Alternatives Considered
- **Server-generated idempotency**: Harder to implement; the client doesn't know the
  key until after the first response, defeating the purpose.
- **Database unique constraint only**: Catches duplicates but still executes the
  business logic. A Redis check-first avoids wasted work.
