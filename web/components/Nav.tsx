"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { api } from "@/lib/apiClient";
import { clearStoredApiKey } from "@/lib/apiKey";
import { clearStoredSession, getStoredSession } from "@/lib/session";
import { useIsConnected } from "@/lib/useConnection";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme-toggle";

const LINKS = [
  { href: "/endpoints", label: "Endpoints" },
  { href: "/topics", label: "Topics" },
  { href: "/deliveries", label: "Deliveries" },
];

export function Nav() {
  const pathname = usePathname();
  const router = useRouter();
  const connected = useIsConnected();

  if (!connected) return null;

  const session = getStoredSession();

  async function disconnect() {
    if (getStoredSession()) {
      try {
        await api.post("/auth/logout");
      } catch {
        // best-effort — clearing local state below still logs the user out of this browser
      }
      clearStoredSession();
    }
    clearStoredApiKey();
    router.push("/");
  }

  return (
    <nav className="sticky top-0 z-10 border-b bg-background/80 backdrop-blur-sm">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-6">
        <div className="flex items-center gap-8">
          <Link href="/endpoints" className="text-lg font-semibold tracking-tight">
            relay
          </Link>
          <div className="flex items-center gap-1">
            {LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className={
                  pathname?.startsWith(link.href)
                    ? "rounded-md bg-muted px-3 py-1.5 text-sm font-medium"
                    : "rounded-md px-3 py-1.5 text-sm text-muted-foreground hover:bg-muted/50 hover:text-foreground"
                }
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>
        <div className="flex items-center gap-3">
          {session?.organizationName && (
            <span className="hidden text-sm text-muted-foreground sm:inline">{session.organizationName}</span>
          )}
          <ThemeToggle />
          <Button variant="ghost" size="sm" onClick={disconnect}>
            Disconnect
          </Button>
        </div>
      </div>
    </nav>
  );
}
