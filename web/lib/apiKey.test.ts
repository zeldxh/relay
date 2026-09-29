import { describe, expect, it, afterEach } from "vitest";
import { clearStoredApiKey, getStoredApiKey, setStoredApiKey } from "./apiKey";

describe("apiKey storage", () => {
  afterEach(() => {
    clearStoredApiKey();
  });

  it("returns null when nothing is stored", () => {
    expect(getStoredApiKey()).toBeNull();
  });

  it("round-trips a stored key", () => {
    setStoredApiKey("relay_abc123");
    expect(getStoredApiKey()).toBe("relay_abc123");
  });

  it("clears the stored key", () => {
    setStoredApiKey("relay_abc123");
    clearStoredApiKey();
    expect(getStoredApiKey()).toBeNull();
  });
});
