# relay docs

Documentation site for [relay](https://github.com/zeldxh/relay), built with
[Fumadocs](https://fumadocs.dev).

## Development

```bash
pnpm install
pnpm dev
```

Open [http://localhost:3001](http://localhost:3001). It uses port `3001`, not the default
`3000`, since the dashboard (`../web`) already uses that port and both often run
side by side locally.

```bash
pnpm lint          # eslint
pnpm types:check   # generated route types + tsc --noEmit
pnpm build         # production build
```

## Content

Docs pages live in `content/docs/**.mdx`, grouped by folder with a `meta.json`
controlling sidebar title and page order in each. See
[`src/lib/source.ts`](./src/lib/source.ts) for how the content is loaded, and
[Fumadocs' own docs](https://fumadocs.dev/docs/mdx) for the MDX/Markdown API.
