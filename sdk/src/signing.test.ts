import { describe, expect, it } from "vitest";
import { signPayload, verifyWebhookSignature } from "./signing.js";

describe("signPayload", () => {
  it("matches the known vector the backend's HmacSignerTest is tested against", () => {
    // Fixed input/output pair shared with HmacSignerTest.java - if either side changes the
    // signing scheme, this fails loudly instead of the SDK silently drifting from the server.
    const signature = signPayload('{"type":"ping"}', "test-secret");

    expect(signature).toBe("5a325db300c4be4c44b2d95c065fdce8b91830a6e6ce2622d63c301205b83cc3");
  });

  it("produces a 64-character lowercase hex digest", () => {
    const signature = signPayload('{"hello":"world"}', "s3cr3t");

    expect(signature).toMatch(/^[0-9a-f]{64}$/);
  });

  it("is deterministic for the same payload and secret", () => {
    expect(signPayload("a", "s")).toBe(signPayload("a", "s"));
  });

  it("differs for different payloads or secrets", () => {
    expect(signPayload("a", "s")).not.toBe(signPayload("b", "s"));
    expect(signPayload("a", "s1")).not.toBe(signPayload("a", "s2"));
  });
});

describe("verifyWebhookSignature", () => {
  const secret = "endpoint-secret";
  const body = '{"orderId":42}';

  it("accepts a correctly signed body", () => {
    const signature = signPayload(body, secret);

    expect(verifyWebhookSignature(body, signature, secret)).toBe(true);
  });

  it("rejects a tampered body", () => {
    const signature = signPayload(body, secret);

    expect(verifyWebhookSignature('{"orderId":43}', signature, secret)).toBe(false);
  });

  it("rejects the wrong secret", () => {
    const signature = signPayload(body, secret);

    expect(verifyWebhookSignature(body, signature, "wrong-secret")).toBe(false);
  });

  it("rejects a malformed signature without throwing", () => {
    expect(verifyWebhookSignature(body, "not-hex-and-wrong-length", secret)).toBe(false);
  });
});
