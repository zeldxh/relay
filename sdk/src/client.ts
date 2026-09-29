import { RelayApiError } from "./errors.js";
import type {
  Delivery,
  Endpoint,
  IngestEventResult,
  ListDeliveriesOptions,
  Page,
  Subscription,
  Topic,
} from "./types.js";

export interface RelayClientOptions {
  apiKey: string;
  /** Defaults to http://localhost:8080 for local development. */
  baseUrl?: string;
}

export class RelayClient {
  private readonly apiKey: string;
  private readonly baseUrl: string;

  constructor(options: RelayClientOptions) {
    if (!options.apiKey) {
      throw new Error("RelayClient requires an apiKey");
    }
    this.apiKey = options.apiKey;
    this.baseUrl = (options.baseUrl ?? "http://localhost:8080").replace(/\/+$/, "");
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const headers = new Headers(init.headers);
    headers.set("Authorization", `Bearer ${this.apiKey}`);
    if (init.body !== undefined) {
      headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`${this.baseUrl}${path}`, { ...init, headers });

    if (!response.ok) {
      const body = await response.text();
      throw new RelayApiError(response.status, body || response.statusText);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }

  createTopic(name: string): Promise<Topic> {
    return this.request<Topic>("/topics", { method: "POST", body: JSON.stringify({ name }) });
  }

  listTopics(): Promise<Topic[]> {
    return this.request<Topic[]>("/topics");
  }

  listSubscriptions(topicId: number): Promise<Subscription[]> {
    return this.request<Subscription[]>(`/topics/${topicId}/subscriptions`);
  }

  subscribe(topicId: number, endpointId: number): Promise<Subscription> {
    return this.request<Subscription>(`/topics/${topicId}/subscriptions`, {
      method: "POST",
      body: JSON.stringify({ endpointId }),
    });
  }

  /** Publishes an event to a topic; relay fans it out to every subscribed, verified endpoint. */
  publish(topicId: number, payload: unknown): Promise<IngestEventResult> {
    return this.request<IngestEventResult>(`/topics/${topicId}/events`, {
      method: "POST",
      body: JSON.stringify({ payload }),
    });
  }

  createEndpoint(name: string, url: string, rateLimitPerSecond?: number): Promise<Endpoint> {
    return this.request<Endpoint>("/endpoints", {
      method: "POST",
      body: JSON.stringify({ name, url, rateLimitPerSecond }),
    });
  }

  listEndpoints(): Promise<Endpoint[]> {
    return this.request<Endpoint[]>("/endpoints");
  }

  getEndpoint(id: number): Promise<Endpoint> {
    return this.request<Endpoint>(`/endpoints/${id}`);
  }

  /** Pings the endpoint's URL with a signed test payload; relay only delivers to verified endpoints. */
  verifyEndpoint(id: number): Promise<Endpoint> {
    return this.request<Endpoint>(`/endpoints/${id}/verify`, { method: "POST" });
  }

  listDeliveries(options: ListDeliveriesOptions = {}): Promise<Page<Delivery>> {
    const params = new URLSearchParams();
    if (options.status) params.set("status", options.status);
    if (options.page !== undefined) params.set("page", String(options.page));
    if (options.size !== undefined) params.set("size", String(options.size));
    const query = params.toString();
    return this.request<Page<Delivery>>(`/deliveries${query ? `?${query}` : ""}`);
  }

  /** Resets a FAILED delivery back to PENDING with a fresh attempt budget. */
  replayDelivery(id: number): Promise<Delivery> {
    return this.request<Delivery>(`/deliveries/${id}/replay`, { method: "POST" });
  }
}
