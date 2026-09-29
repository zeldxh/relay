# Contributing

## Project layout

This is a monorepo with four parts:

| Path | What it is | Stack |
|---|---|---|
| `/` | The API and delivery worker | Spring Boot 3, Java 21, Maven |
| `web/` | The dashboard (endpoints, topics, deliveries) | Next.js, pnpm |
| `docs/` | The docs/landing site | Next.js + Fumadocs, pnpm |
| `sdk/` | The Node client (`@zeldxh/relay-sdk`) | TypeScript, pnpm |

## Running it locally

```bash
docker compose up -d          # postgres + redis (add --build for the full stack incl. web/docs)
./mvnw spring-boot:run         # API on :8080
cd web && pnpm install && pnpm dev    # dashboard on :3000
cd docs && pnpm install && pnpm dev   # docs on :3001
```

Use **pnpm**, not npm, in `web/`, `docs/`, and `sdk/`: the lockfiles and CI are pnpm-only.

## Before opening a PR

Each part has its own checks; run whichever you touched:

```bash
./mvnw verify                          # backend: tests + build
cd web && pnpm lint && pnpm test && pnpm build
cd docs && pnpm lint && pnpm test && pnpm build
cd sdk && pnpm lint && pnpm test
```

CI runs the same four jobs (`build`, `web`, `docs`, `sdk`) on every PR. A job only
runs if its directory changed. All of them need to pass before merging.

## Commits and PRs

- Write commit messages the way a person would: what changed and why, no
  appended attribution of any kind (no `Co-authored-by:` trailers, session
  links, or similar) regardless of what a tool's default template suggests.
- Prefer a small number of commits that each explain themselves over one
  commit per file save.
- Squash-merge is the norm for this repo.

## Reviews

A PR needs a review from someone other than its author before merging - don't
approve and merge your own work.
