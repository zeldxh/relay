import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { RelayClient } from "./client.js";
import { RelayApiError } from "./errors.js";

describe("RelayClient", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("requires an apiKey", () => {
    expect(() => new RelayClient({ apiKey: "" })).toThrow(/apiKey/);
  });

  it("defaults the base URL to localhost:8080", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response("[]", { status: 200 }));
    const client = new RelayClient({ apiKey: "relay_test" });

    await client.listEndpoints();

    const [url] = vi.mocked(fetch).mock.calls[0];
    expect(url).toBe("http://localhost:8080/endpoints");
  });

  it("strips a trailing slash from a custom base URL", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response("[]", { status: 200 }));
    const client = new RelayClient({ apiKey: "relay_test", baseUrl: "https://relay.example.com/" });

    await client.listEndpoints();

    const [url] = vi.mocked(fetch).mock.calls[0];
    expect(url).toBe("https://relay.example.com/endpoints");
  });

  it("sends the api key as a bearer token", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response("[]", { status: 200 }));
    const client = new RelayClient({ apiKey: "relay_abc123" });

    await client.listEndpoints();

    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect((init?.headers as Headers).get("Authorization")).toBe("Bearer relay_abc123");
  });

  it("publishes an event with the payload wrapped for the ingest endpoint", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ eventId: 1, deliveryIds: [10, 11] }), { status: 201 })
    );
    const client = new RelayClient({ apiKey: "relay_test" });

    const result = await client.publish(5, { orderId: 42 });

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toBe("http://localhost:8080/topics/5/events");
    expect(init?.body).toBe(JSON.stringify({ payload: { orderId: 42 } }));
    expect(result).toEqual({ eventId: 1, deliveryIds: [10, 11] });
  });

  it("builds the query string for listDeliveries filters", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 }), {
        status: 200,
      })
    );
    const client = new RelayClient({ apiKey: "relay_test" });

    await client.listDeliveries({ status: "FAILED", page: 2, size: 50 });

    const [url] = vi.mocked(fetch).mock.calls[0];
    expect(url).toBe("http://localhost:8080/deliveries?status=FAILED&page=2&size=50");
  });

  it("throws a RelayApiError with the response status and body on failure", async () => {
    vi.mocked(fetch).mockImplementation(async () => new Response("endpoint not found", { status: 404 }));
    const client = new RelayClient({ apiKey: "relay_test" });

    await expect(client.getEndpoint(999)).rejects.toBeInstanceOf(RelayApiError);
    await expect(client.getEndpoint(999)).rejects.toMatchObject({ status: 404, message: "endpoint not found" });
  });
});
