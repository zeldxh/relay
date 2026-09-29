"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { api, ApiError } from "@/lib/apiClient";
import { setStoredApiKey } from "@/lib/apiKey";
import { setStoredSession } from "@/lib/session";
import { useIsConnected } from "@/lib/useConnection";
import type { AuthResponse } from "@/lib/types";
import { AuthLayout } from "@/components/AuthLayout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export default function LoginPage() {
  const router = useRouter();
  const connected = useIsConnected();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [existingKey, setExistingKey] = useState("");
  const [showKeyForm, setShowKeyForm] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (connected) {
      router.push("/endpoints");
    }
  }, [connected, router]);

  if (connected) {
    return null;
  }

  async function login(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const auth = await api.post<AuthResponse>("/auth/login", { email, password });
      setStoredSession({ organizationId: auth.organizationId, organizationName: auth.organizationName, email: auth.email });
      router.push("/endpoints");
    } catch (err) {
      setError(err instanceof ApiError ? "Invalid email or password." : "Something went wrong.");
    } finally {
      setBusy(false);
    }
  }

  function connectWithExistingKey(e: React.FormEvent) {
    e.preventDefault();
    setStoredApiKey(existingKey.trim());
    router.push("/endpoints");
  }

  return (
    <AuthLayout>
      {showKeyForm ? (
        <div className="flex flex-col gap-6">
          <div className="flex flex-col gap-2">
            <h2 className="text-2xl font-semibold tracking-tight">Connect with an API key</h2>
            <p className="text-sm text-muted-foreground">For organizations provisioned directly through the API.</p>
          </div>
          <form onSubmit={connectWithExistingKey} className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="apiKey">API key</Label>
              <Input
                id="apiKey"
                className="h-10 font-mono text-sm"
                placeholder="relay_..."
                value={existingKey}
                onChange={(e) => setExistingKey(e.target.value)}
                required
              />
            </div>
            <Button type="submit" size="lg" className="h-10" disabled={!existingKey}>
              Connect
            </Button>
          </form>
          <button
            type="button"
            onClick={() => setShowKeyForm(false)}
            className="text-sm text-muted-foreground underline underline-offset-4 hover:text-foreground"
          >
            Back to login
          </button>
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          <div className="flex flex-col gap-2">
            <h2 className="text-2xl font-semibold tracking-tight">Log in</h2>
            <p className="text-sm text-muted-foreground">Access your organization&apos;s dashboard.</p>
          </div>

          <form onSubmit={login} className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                className="h-10"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                className="h-10"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </div>
            <Button type="submit" size="lg" className="h-10 mt-2" disabled={busy || !email || !password}>
              {busy ? "Logging in…" : "Log in"}
            </Button>
            {error && <p className="text-sm text-destructive">{error}</p>}
          </form>

          <div className="flex flex-col items-center gap-3 text-sm">
            <p className="text-muted-foreground">
              New to relay?{" "}
              <Link href="/signup" className="font-medium text-foreground underline underline-offset-4">
                Create an account
              </Link>
            </p>
            <button
              type="button"
              onClick={() => setShowKeyForm(true)}
              className="text-muted-foreground underline underline-offset-4 hover:text-foreground"
            >
              Or connect with an API key
            </button>
          </div>
        </div>
      )}
    </AuthLayout>
  );
}
