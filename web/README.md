# relay dashboard

Next.js dashboard for the relay webhook delivery API. See the [root README](../README.md)
for what Relay does and how to run the whole stack.

## Development

```bash
pnpm install
pnpm dev
```

Open [http://localhost:3000](http://localhost:3000). The backend API (`../`) must be
running on `localhost:8080`. Requests are proxied through Next.js rewrites in
`next.config.ts` to avoid CORS.

```bash
pnpm test    # unit tests (vitest)
pnpm lint    # eslint
pnpm build   # production build
```
