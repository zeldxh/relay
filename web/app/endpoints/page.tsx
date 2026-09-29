"use client";

import { useState } from "react";
import useSWR from "swr";
import { api, ApiError, fetcher } from "@/lib/apiClient";
import type { Endpoint } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

export default function EndpointsPage() {
  const { data: endpoints, isLoading, mutate } = useSWR<Endpoint[]>("/endpoints", fetcher);
  const [name, setName] = useState("");
  const [url, setUrl] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  async function createEndpoint(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api.post<Endpoint>("/endpoints", { name, url });
      setName("");
      setUrl("");
      await mutate();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create endpoint.");
    }
  }

  async function verify(id: number) {
    setBusyId(id);
    setError(null);
    try {
      await api.post<Endpoint>(`/endpoints/${id}/verify`);
      await mutate();
    } catch (err) {
      setError(err instanceof ApiError ? `Verification failed: ${err.message}` : "Verification failed.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-6 py-10">
      <div>
        <h1 className="text-xl font-semibold">Endpoints</h1>
        <p className="text-sm text-muted-foreground">
          Register a URL, verify it, then subscribe it to topics to start receiving events.
        </p>
      </div>

      <form onSubmit={createEndpoint} className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="endpointName">Name</Label>
          <Input id="endpointName" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="endpointUrl">URL</Label>
          <Input
            id="endpointUrl"
            className="w-80"
            placeholder="https://example.com/webhook"
            value={url}
            onChange={(e) => setUrl(e.target.value)}
            required
          />
        </div>
        <Button type="submit">Add endpoint</Button>
      </form>

      {error && <p className="text-sm text-destructive">{error}</p>}

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : !endpoints || endpoints.length === 0 ? (
        <p className="text-sm text-muted-foreground">No endpoints yet.</p>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>URL</TableHead>
              <TableHead>Rate limit</TableHead>
              <TableHead>Status</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {endpoints.map((endpoint) => (
              <TableRow key={endpoint.id}>
                <TableCell>{endpoint.name}</TableCell>
                <TableCell className="font-mono text-xs">{endpoint.url}</TableCell>
                <TableCell>{endpoint.rateLimitPerSecond}/s</TableCell>
                <TableCell>
                  {endpoint.verified ? (
                    <Badge variant="outline" className="border-green-600 text-green-700 dark:text-green-400">
                      verified
                    </Badge>
                  ) : (
                    <Badge variant="outline" className="border-yellow-600 text-yellow-700 dark:text-yellow-400">
                      unverified
                    </Badge>
                  )}
                </TableCell>
                <TableCell>
                  {!endpoint.verified && (
                    <Button
                      variant="outline"
                      size="xs"
                      onClick={() => verify(endpoint.id)}
                      disabled={busyId === endpoint.id}
                    >
                      {busyId === endpoint.id ? "Verifying…" : "Verify"}
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
