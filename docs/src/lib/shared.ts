export const appName = 'relay';
export const docsRoute = '/docs';
export const docsImageRoute = '/og/docs';
export const docsContentRoute = '/llms.mdx/docs';

export const gitConfig = {
  user: 'zeldxh',
  repo: 'relay',
  branch: 'main',
};

// Single source of truth for site-wide SEO metadata (root/home <head>, sitemap, robots,
// JSON-LD). Falls back to localhost so `pnpm dev`/self-hosted builds without
// NEXT_PUBLIC_SITE_URL set still produce valid (if not publicly resolvable) metadata.
export const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? 'http://localhost:3001';
export const orgName = 'zeldxh';
export const tagline = 'Webhooks that deliver themselves.';
export const siteDescription =
  'Relay is an open-source, self-hosted webhook delivery service: publish an event to a topic, ' +
  'Relay signs it, fans it out to every subscribed endpoint, and retries automatically until it lands.';
export const keywords = [
  'webhooks',
  'webhook delivery',
  'event delivery',
  'pub/sub',
  'webhook retries',
  'HMAC signed webhooks',
  'self-hosted webhooks',
  'open source webhooks',
];

// Next.js does NOT deep-merge a page's `openGraph` object with its parent layout's - a page
// that defines its own `openGraph` replaces the parent's entirely, dropping any field (like
// siteName/locale/type) it didn't repeat. So every page that sets `openGraph` must include
// these explicitly rather than relying on the root layout to supply them.
export const ogSiteName = `${appName} docs`;
export const ogLocale = 'en_US';
