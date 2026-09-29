"use client";

import { Fragment, useState } from "react";
import useSWR from "swr";
import { api, ApiError, fetcher } from "@/lib/apiClient";
import type { Delivery, DeliveryStatus, Page } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";

const STATUS_FILTERS: (DeliveryStatus | "ALL")[] = ["ALL", "PENDING", "SUCCESS", "FAILED"];
const PAGE_SIZE = 20;

const BADGE_CLASSES: Record<DeliveryStatus, string> = {
  SUCCESS: "border-green-600 text-green-700 dark:text-green-400",
  FAILED: "border-red-600 text-red-700 dark:text-red-400",
  PENDING: "border-yellow-600 text-yellow-700 dark:text-yellow-400",
};

export default function DeliveriesPage() {
  const [status, setStatus] = useState<DeliveryStatus | "ALL">("ALL");
  const [pageNumber, setPageNumber] = useState(0);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);

  const query =
    status === "ALL" ? `?page=${pageNumber}&size=${PAGE_SIZE}` : `?status=${status}&page=${pageNumber}&size=${PAGE_SIZE}`;
  const { data: page, isLoading, mutate } = useSWR<Page<Delivery>>(`/deliveries${query}`, fetcher);

  async function replay(id: number) {
    setBusyId(id);
    setError(null);
    try {
      await api.post(`/deliveries/${id}/replay`);
      await mutate();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to replay delivery.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-6 px-6 py-10">
      <div>
        <h1 className="text-xl font-semibold">Deliveries</h1>
        <p className="text-sm text-muted-foreground">
          Every delivery attempt across your endpoints. Failed deliveries can be replayed.
        </p>
      </div>

      <div className="flex gap-2">
        {STATUS_FILTERS.map((s) => (
          <Button
            key={s}
            size="sm"
            variant={status === s ? "default" : "outline"}
            onClick={() => {
              setStatus(s);
              setPageNumber(0);
            }}
          >
            {s.toLowerCase()}
          </Button>
        ))}
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : !page || page.content.length === 0 ? (
        <p className="text-sm text-muted-foreground">No deliveries.</p>
      ) : (
        <>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Topic</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Attempts</TableHead>
                <TableHead>Last response</TableHead>
                <TableHead>Next attempt</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {page.content.map((delivery) => (
                <Fragment key={delivery.id}>
                  <TableRow>
                    <TableCell>{delivery.topic}</TableCell>
                    <TableCell>
                      <Badge variant="outline" className={BADGE_CLASSES[delivery.status]}>
                        {delivery.status.toLowerCase()}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {delivery.attemptCount}/{delivery.maxAttempts}
                    </TableCell>
                    <TableCell>{delivery.lastResponseStatus ?? "—"}</TableCell>
                    <TableCell className="text-xs">{new Date(delivery.nextAttemptAt).toLocaleString()}</TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button
                          variant="ghost"
                          size="xs"
                          onClick={() => setExpandedId(expandedId === delivery.id ? null : delivery.id)}
                        >
                          {expandedId === delivery.id ? "hide" : "payload"}
                        </Button>
                        {delivery.status === "FAILED" && (
                          <Button variant="outline" size="xs" onClick={() => replay(delivery.id)} disabled={busyId === delivery.id}>
                            {busyId === delivery.id ? "Replaying…" : "Replay"}
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                  {expandedId === delivery.id && (
                    <TableRow>
                      <TableCell colSpan={6} className="bg-muted/50">
                        <pre className="overflow-x-auto whitespace-pre-wrap font-mono text-xs">{delivery.payload}</pre>
                      </TableCell>
                    </TableRow>
                  )}
                </Fragment>
              ))}
            </TableBody>
          </Table>

          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">
              Page {page.number + 1} of {Math.max(page.totalPages, 1)} — {page.totalElements} total
            </span>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPageNumber((n) => Math.max(0, n - 1))}
                disabled={page.number === 0}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPageNumber((n) => n + 1)}
                disabled={page.number + 1 >= page.totalPages}
              >
                Next
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
