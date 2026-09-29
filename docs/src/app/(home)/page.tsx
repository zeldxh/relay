import Link from 'next/link';
import {
  ArrowRight,
  Building2,
  Gauge,
  History,
  KeyRound,
  Network,
  Radio,
  RefreshCw,
  ScrollText,
  Users,
  Workflow,
} from 'lucide-react';
import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import { ogLocale, ogSiteName, siteDescription, siteUrl, tagline } from '@/lib/shared';

export const metadata: Metadata = {
  // No `title` here on purpose: the root layout's default title *is* this page's title,
  // and a string here would run through the "%s | relay docs" template instead of
  // replacing it, producing "relay — ... | relay docs".
  description: siteDescription,
  alternates: {
    canonical: '/',
  },
  openGraph: {
    // A page's `openGraph` replaces the parent layout's rather than merging with it, so
    // type/siteName/locale have to be repeated here too - see the note in lib/shared.ts.
    type: 'website',
    url: siteUrl,
    siteName: ogSiteName,
    locale: ogLocale,
    title: `relay — ${tagline}`,
    description: siteDescription,
  },
};

const USE_CASES: { icon: ReactNode; title: string; description: string }[] = [
  {
    icon: <Users className="size-5" />,
    title: 'Ship a webhooks API to your own customers',
    description:
      'Give each of your customers their own endpoint and signing secret, and let Relay handle delivery, retries, and verification — so your product can offer webhooks without your team building the infrastructure.',
  },
  {
    icon: <Network className="size-5" />,
    title: 'Fan out one event to many systems',
    description:
      'Publish a single event to a topic and every subscribed endpoint — analytics, billing, a partner integration — gets its own independently tracked, independently retried delivery.',
  },
  {
    icon: <Workflow className="size-5" />,
    title: 'Decouple internal services reliably',
    description:
      'Let one service publish what happened and move on. Relay queues the delivery, retries with backoff on failure, and keeps services from needing to know about each other’s uptime.',
  },
  {
    icon: <History className="size-5" />,
    title: 'Keep an audit trail of every integration event',
    description:
      'Every publish, delivery attempt, and outcome is recorded against the organization that triggered it — so when a customer asks “did you send this?”, the answer is a lookup, not a guess.',
  },
];

const FEATURES: { icon: ReactNode; title: string; description: string }[] = [
  {
    icon: <Building2 className="size-5" />,
    title: 'Multi-tenant by default',
    description:
      'Every organization gets its own API key and only ever sees its own endpoints, topics, and deliveries.',
  },
  {
    icon: <KeyRound className="size-5" />,
    title: 'Signed payloads',
    description:
      'Every delivery carries an HMAC-SHA256 X-Relay-Signature, computed with a secret unique to each endpoint.',
  },
  {
    icon: <RefreshCw className="size-5" />,
    title: 'Automatic retries',
    description:
      'Failed deliveries back off exponentially — 30s, 1m, 2m... up to 8 attempts — with no code on your side.',
  },
  {
    icon: <Gauge className="size-5" />,
    title: 'Per-endpoint rate limits',
    description:
      'A Redis-backed limiter keeps a slow or misbehaving endpoint from getting flooded, across every instance.',
  },
  {
    icon: <Radio className="size-5" />,
    title: 'Topics & fan-out',
    description:
      'Publish once to a topic; every subscribed, verified endpoint gets its own independently-tracked delivery.',
  },
  {
    icon: <ScrollText className="size-5" />,
    title: 'Full audit trail',
    description:
      'Every create, verify, subscribe, and replay is recorded against the organization that did it.',
  },
];

const STEPS = [
  {
    n: '01',
    title: 'Publish',
    description: 'POST an event to a topic. It fans out to every endpoint subscribed to it.',
  },
  {
    n: '02',
    title: 'Sign',
    description: 'Each delivery is signed with the receiving endpoint’s own secret.',
  },
  {
    n: '03',
    title: 'Deliver',
    description: 'Sent over a Redis stream for near-immediate dispatch, with a backup poller as a safety net.',
  },
  {
    n: '04',
    title: 'Retry',
    description: 'Anything but a 2xx response schedules a backed-off retry, automatically.',
  },
];

export default function HomePage() {
  return (
    <>
      <Hero />
      <UseCases />
      <HowItWorks />
      <Features />
      <FinalCta />
      <Footer />
    </>
  );
}

function Hero() {
  return (
    <section>
      <div className="mx-auto grid max-w-6xl gap-12 px-6 py-20 md:grid-cols-2 md:items-center md:py-28">
        <div className="flex flex-col gap-6">
          <span className="inline-flex w-fit items-center gap-1.5 rounded-full border border-fd-border bg-fd-card px-3 py-1 text-xs font-medium text-fd-muted-foreground">
            <span className="size-1.5 rounded-full bg-fd-primary" />
            Open source &middot; self-hosted
          </span>
          <h1 className="text-4xl font-bold tracking-tight text-balance sm:text-5xl">
            Webhooks that deliver themselves.
          </h1>
          <p className="text-lg text-pretty text-fd-muted-foreground">
            Publish an event to a topic. Relay signs it, fans it out to every subscribed
            endpoint, and retries automatically until it lands &mdash; so you don&rsquo;t have
            to build any of that yourself.
          </p>
          <div className="flex flex-wrap items-center gap-3 pt-2">
            <Link
              href="/docs/quickstart"
              className="inline-flex items-center gap-2 rounded-lg bg-fd-primary px-5 py-2.5 text-sm font-semibold text-fd-primary-foreground transition-opacity hover:opacity-90"
            >
              Get started
              <ArrowRight className="size-4" />
            </Link>
            <Link
              href="/docs"
              className="inline-flex items-center gap-2 rounded-lg border border-fd-border bg-fd-card px-5 py-2.5 text-sm font-semibold transition-colors hover:bg-fd-accent"
            >
              Read the docs
            </Link>
          </div>
        </div>
        <CodePanel />
      </div>
    </section>
  );
}

function CodePanel() {
  return (
    <div className="overflow-hidden rounded-xl border border-fd-border bg-fd-card shadow-xl shadow-black/[0.03] dark:shadow-black/20">
      <div className="flex items-center gap-1.5 border-b border-fd-border px-4 py-3">
        <span className="size-2.5 rounded-full bg-red-400/70" />
        <span className="size-2.5 rounded-full bg-yellow-400/70" />
        <span className="size-2.5 rounded-full bg-green-400/70" />
        <span className="ml-2 text-xs text-fd-muted-foreground">publish-order.ts</span>
      </div>
      <pre className="overflow-x-auto p-5 text-[13px] leading-relaxed">
        <code className="font-mono">
          <span className="text-fd-muted-foreground">{'// npm i @zeldxh/relay-sdk'}</span>{'\n'}
          <Kw>import</Kw> {'{ RelayClient }'} <Kw>from</Kw> <Str>&quot;@zeldxh/relay-sdk&quot;</Str>;{'\n\n'}
          <Kw>const</Kw> relay = <Kw>new</Kw> RelayClient({'{'}{'\n'}
          {'  '}apiKey: process.env.RELAY_API_KEY!,{'\n'}
          {'}'});{'\n\n'}
          <Kw>const</Kw> topic = <Kw>await</Kw> relay.createTopic(<Str>&quot;order.created&quot;</Str>);{'\n\n'}
          <Kw>await</Kw> relay.publish(topic.id, {'{'}{'\n'}
          {'  '}orderId: <Num>42</Num>,{'\n'}
          {'  '}total: <Num>19.99</Num>,{'\n'}
          {'}'});{'\n\n'}
          <span className="text-fd-muted-foreground">{'// -> every subscribed, verified endpoint'}</span>{'\n'}
          <span className="text-fd-muted-foreground">{'//    gets its own signed, retried delivery'}</span>
        </code>
      </pre>
    </div>
  );
}

function Kw({ children }: { children: ReactNode }) {
  return <span className="text-fd-primary">{children}</span>;
}
function Str({ children }: { children: ReactNode }) {
  return <span className="text-emerald-600 dark:text-emerald-400">{children}</span>;
}
function Num({ children }: { children: ReactNode }) {
  return <span className="text-orange-600 dark:text-orange-400">{children}</span>;
}

function UseCases() {
  return (
    <section className="px-6 py-20">
      <div className="mx-auto max-w-6xl">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">Built for real integrations</h2>
          <p className="mt-3 text-fd-muted-foreground">
            A few of the ways teams use Relay to move events between systems.
          </p>
        </div>
        <div className="mt-14 grid gap-4 sm:grid-cols-2">
          {USE_CASES.map((useCase) => (
            <div
              key={useCase.title}
              className="flex gap-4 rounded-xl border border-fd-border bg-fd-card p-6"
            >
              <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-fd-primary/10 text-fd-primary">
                {useCase.icon}
              </div>
              <div>
                <h3 className="font-semibold">{useCase.title}</h3>
                <p className="mt-1.5 text-sm text-fd-muted-foreground">{useCase.description}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function HowItWorks() {
  return (
    <section className="px-6 py-20">
      <div className="mx-auto max-w-6xl">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">How it works</h2>
          <p className="mt-3 text-fd-muted-foreground">
            One publish, tracked all the way to a successful delivery.
          </p>
        </div>
        <div className="mt-14 grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
          {STEPS.map((step, i) => (
            <div key={step.n} className="relative">
              <div className="flex items-center gap-3">
                <span className="font-mono text-sm font-semibold text-fd-primary">{step.n}</span>
                <span className="h-px flex-1 bg-fd-border" />
              </div>
              <h3 className="mt-4 font-semibold">{step.title}</h3>
              <p className="mt-1.5 text-sm text-fd-muted-foreground">{step.description}</p>
              {i < STEPS.length - 1 && (
                <ArrowRight className="absolute top-1 -right-6 hidden size-4 text-fd-muted-foreground lg:block" />
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function Features() {
  return (
    <section className="bg-fd-card/40 px-6 py-20">
      <div className="mx-auto max-w-6xl">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
            Everything a production delivery pipeline needs
          </h2>
          <p className="mt-3 text-fd-muted-foreground">
            Built in from the start, not bolted on later.
          </p>
        </div>
        <div className="mt-14 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {FEATURES.map((feature) => (
            <div
              key={feature.title}
              className="rounded-xl border border-fd-border bg-fd-card p-6 transition-colors hover:bg-fd-accent/50"
            >
              <div className="inline-flex size-10 items-center justify-center rounded-lg bg-fd-primary/10 text-fd-primary">
                {feature.icon}
              </div>
              <h3 className="mt-4 font-semibold">{feature.title}</h3>
              <p className="mt-1.5 text-sm text-fd-muted-foreground">{feature.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function FinalCta() {
  return (
    <section className="px-6 py-20">
      <div className="mx-auto flex max-w-3xl flex-col items-center gap-6 text-center">
        <h2 className="text-2xl font-bold tracking-tight sm:text-3xl">
          Stop building webhook infrastructure.
        </h2>
        <p className="max-w-xl text-fd-muted-foreground">
          Get an API key, register an endpoint, and deliver your first event in a few minutes.
        </p>
        <div className="flex flex-wrap items-center justify-center gap-3">
          <Link
            href="/docs/quickstart"
            className="inline-flex items-center gap-2 rounded-lg bg-fd-primary px-5 py-2.5 text-sm font-semibold text-fd-primary-foreground transition-opacity hover:opacity-90"
          >
            Get started
            <ArrowRight className="size-4" />
          </Link>
          <Link
            href="/docs/api-reference"
            className="inline-flex items-center gap-2 rounded-lg border border-fd-border bg-fd-card px-5 py-2.5 text-sm font-semibold transition-colors hover:bg-fd-accent"
          >
            API reference
          </Link>
        </div>
      </div>
    </section>
  );
}

function Footer() {
  return (
    <footer className="px-6 py-10">
      <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-4 text-sm text-fd-muted-foreground sm:flex-row">
        <p>relay</p>
        <div className="flex items-center gap-6">
          <Link href="/docs" className="hover:text-fd-foreground">
            Documentation
          </Link>
          <Link href="/docs/sdk" className="hover:text-fd-foreground">
            SDK
          </Link>
          <a
            href="https://github.com/zeldxh/relay"
            target="_blank"
            rel="noreferrer"
            className="hover:text-fd-foreground"
          >
            GitHub
          </a>
        </div>
      </div>
    </footer>
  );
}
