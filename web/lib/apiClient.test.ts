import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api, ApiError } from "./apiClient";
import { clearStoredApiKey, setStoredApiKey } from "./apiKey";

describe("apiClient", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    clearStoredApiKey();
    vi.unstubAllGlobals();
  });

  it("prefixes requests with /api/relay and omits Authorization when no key is stored", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(
      new Response(JSON.stringify({ ok: true }), { status: 200, headers: { "Content-Type": "application/json" } })
    );

    await api.get("/endpoints");

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toBe("/api/relay/endpoints");
    expect((init?.headers as Headers).has("Authorization")).toBe(false);
  });

  it("attaches the stored API key as a Bearer token", async () => {
    setStoredApiKey("relay_test123");
    vi.mocked(fetch).mockResolvedValueOnce(new Response("{}", { status: 200 }));

    await api.get("/endpoints");

    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect((init?.headers as Headers).get("Authorization")).toBe("Bearer relay_test123");
  });

  it("throws an ApiError with the response status and body on failure", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response("endpoint not found", { status: 404 }));

    await expect(api.get("/endpoints/999")).rejects.toMatchObject({
      status: 404,
      message: "endpoint not found",
    });
  });

  it("throws instances of ApiError specifically", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response("nope", { status: 401 }));

    await expect(api.get("/endpoints")).rejects.toBeInstanceOf(ApiError);
  });

  it("serializes the request body for post()", async () => {
    vi.mocked(fetch).mockResolvedValueOnce(new Response("{}", { status: 201 }));

    await api.post("/endpoints", { name: "orders", url: "https://example.com" });

    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect(init?.method).toBe("POST");
    expect(init?.body).toBe(JSON.stringify({ name: "orders", url: "https://example.com" }));
  });
});
