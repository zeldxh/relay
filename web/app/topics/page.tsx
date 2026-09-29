"use client";

import { useState } from "react";
import useSWR from "swr";
import { api, ApiError } from "@/lib/apiClient";
import type { Endpoint, Subscription, Topic } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

interface TopicsData {
  topics: Topic[];
  endpoints: Endpoint[];
  subscriptionsByTopic: Record<number, Subscription[]>;
}

async function loadTopicsData(): Promise<TopicsData> {
  const [topics, endpoints] = await Promise.all([
    api.get<Topic[]>("/topics"),
    api.get<Endpoint[]>("/endpoints"),
  ]);
  const entries = await Promise.all(
    topics.map(async (topic) => [topic.id, await api.get<Subscription[]>(`/topics/${topic.id}/subscriptions`)] as const)
  );
  return { topics, endpoints, subscriptionsByTopic: Object.fromEntries(entries) };
}

export default function TopicsPage() {
  const { data, isLoading, mutate } = useSWR<TopicsData>("topics-overview", loadTopicsData);
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [subscribingTopicId, setSubscribingTopicId] = useState<number | null>(null);
  const [selectedEndpointId, setSelectedEndpointId] = useState<string>("");

  async function createTopic(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await api.post<Topic>("/topics", { name });
      setName("");
      await mutate();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create topic.");
    }
  }

  async function subscribe(topicId: number) {
    if (!selectedEndpointId) return;
    setError(null);
    try {
      await api.post(`/topics/${topicId}/subscriptions`, { endpointId: Number(selectedEndpointId) });
      setSubscribingTopicId(null);
      setSelectedEndpointId("");
      await mutate();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to subscribe endpoint.");
    }
  }

  const topics = data?.topics ?? [];
  const endpoints = data?.endpoints ?? [];
  const subscriptionsByTopic = data?.subscriptionsByTopic ?? {};
  const verifiedEndpoints = endpoints.filter((e) => e.verified);

  function endpointName(id: number) {
    return endpoints.find((e) => e.id === id)?.name ?? `#${id}`;
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-col gap-8 px-6 py-10">
      <div>
        <h1 className="text-xl font-semibold">Topics</h1>
        <p className="text-sm text-muted-foreground">
          Events are published to a topic and fan out to every endpoint subscribed to it.
        </p>
      </div>

      <form onSubmit={createTopic} className="flex items-end gap-3">
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="topicName">Name</Label>
          <Input id="topicName" placeholder="order.created" value={name} onChange={(e) => setName(e.target.value)} required />
        </div>
        <Button type="submit">Add topic</Button>
      </form>

      {error && <p className="text-sm text-destructive">{error}</p>}

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : topics.length === 0 ? (
        <p className="text-sm text-muted-foreground">No topics yet.</p>
      ) : (
        <div className="flex flex-col gap-4">
          {topics.map((topic) => (
            <Card key={topic.id}>
              <CardHeader className="flex flex-row items-center justify-between">
                <CardTitle>{topic.name}</CardTitle>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setSubscribingTopicId(subscribingTopicId === topic.id ? null : topic.id)}
                >
                  {subscribingTopicId === topic.id ? "Cancel" : "Subscribe endpoint"}
                </Button>
              </CardHeader>
              <CardContent className="flex flex-col gap-3">
                <ul className="flex flex-col gap-1 text-sm text-muted-foreground">
                  {(subscriptionsByTopic[topic.id] ?? []).map((sub) => (
                    <li key={sub.id}>→ {endpointName(sub.endpointId)}</li>
                  ))}
                  {(subscriptionsByTopic[topic.id] ?? []).length === 0 && <li>No subscribers yet.</li>}
                </ul>

                {subscribingTopicId === topic.id && (
                  <div className="flex items-center gap-2">
                    <Select value={selectedEndpointId} onValueChange={(value) => setSelectedEndpointId(value ?? "")}>
                      <SelectTrigger size="sm">
                        <SelectValue placeholder="Select a verified endpoint…" />
                      </SelectTrigger>
                      <SelectContent>
                        {verifiedEndpoints.map((endpoint) => (
                          <SelectItem key={endpoint.id} value={String(endpoint.id)}>
                            {endpoint.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                    <Button size="sm" onClick={() => subscribe(topic.id)} disabled={!selectedEndpointId}>
                      Subscribe
                    </Button>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
