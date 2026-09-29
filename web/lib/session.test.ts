import { afterEach, describe, expect, it } from "vitest";
import { clearStoredSession, getStoredSession, setStoredSession } from "./session";

describe("session storage", () => {
  afterEach(() => {
    clearStoredSession();
  });

  it("returns null when nothing is stored", () => {
    expect(getStoredSession()).toBeNull();
  });

  it("round-trips a stored session", () => {
    setStoredSession({ organizationId: 1, organizationName: "Acme", email: "owner@example.com" });
    expect(getStoredSession()).toEqual({ organizationId: 1, organizationName: "Acme", email: "owner@example.com" });
  });

  it("clears the stored session", () => {
    setStoredSession({ organizationId: 1, organizationName: "Acme", email: null });
    clearStoredSession();
    expect(getStoredSession()).toBeNull();
  });

  it("returns null for malformed stored JSON instead of throwing", () => {
    window.localStorage.setItem("relay.session", "not-json");
    expect(getStoredSession()).toBeNull();
  });
});
