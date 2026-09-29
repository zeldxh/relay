import { Building2, KeyRound, RefreshCw } from "lucide-react";
import type { ReactNode } from "react";

const PITCH = [
  { icon: Building2, text: "Multi-tenant by default — your data, isolated by API key." },
  { icon: KeyRound, text: "Every delivery signed with HMAC-SHA256, per endpoint." },
  { icon: RefreshCw, text: "Automatic retries with exponential backoff — no code required." },
];

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      {/* Fixed dark treatment on purpose, not theme-relative — this panel should read the
          same whether the app is in light or dark mode, like a brand mark would. */}
      <div className="relative hidden flex-col justify-between overflow-hidden bg-neutral-950 p-12 text-neutral-50 lg:flex">
        <span className="text-lg font-semibold tracking-tight">relay</span>

        <div className="flex flex-col gap-8">
          <h1 className="text-4xl leading-tight font-bold text-balance">
            Webhooks that deliver themselves.
          </h1>
          <ul className="flex flex-col gap-5">
            {PITCH.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-start gap-3">
                <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-white/10">
                  <Icon className="size-4" />
                </span>
                <span className="text-sm text-neutral-400">{text}</span>
              </li>
            ))}
          </ul>
        </div>

        <p className="text-sm text-neutral-500">Open source &middot; self-hosted</p>
      </div>

      <div className="flex flex-col items-center justify-center p-6 sm:p-12">
        <div className="flex w-full max-w-sm flex-col gap-8">{children}</div>
      </div>
    </div>
  );
}
