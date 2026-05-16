# Frontend

React 19 + TypeScript frontend for the Inventory Management Platform.

## Stack

- **Vite** — dev server and build tool
- **TanStack Query v5** — server state, caching, background refetches
- **React Hook Form + Zod** — form handling and schema validation
- **Axios** — HTTP client with a token-refresh interceptor (auto-retries on 401)
- **Radix UI + Tailwind CSS v4** — accessible UI primitives
- **Recharts** — analytics charts

## Running

```bash
npm install
npm run dev     # http://localhost:5173
npm run build   # production build → dist/
```

The dev server proxies `/api` → `http://localhost:3000` so no CORS configuration is needed locally. See `vite.config.ts` for the proxy setup.

## Project Structure

```
src/
  components/     feature-scoped UI components
  hooks/          data hooks wrapping TanStack Query (useInventory, useProducts, ...)
  lib/            axios instance, auth context, shared types, idempotency key util
  pages/          top-level page components (one per route)
```

Auth state lives in a React context (`lib/auth.tsx`). The axios interceptor in `lib/axios.ts` handles access token injection and transparent refresh on 401 — individual components don't deal with token logic.
