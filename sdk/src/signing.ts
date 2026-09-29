import { createHmac, timingSafeEqual } from "node:crypto";

/**
 * Computes the same HMAC-SHA256-over-UTF8-hex signature relay puts in the
 * X-Relay-Signature header on every outbound delivery (see HmacSigner.java).
 */
export function signPayload(payload: string, secret: string): string {
  return createHmac("sha256", secret).update(payload, "utf8").digest("hex");
}

/**
 * Verifies an inbound webhook. Use this in your endpoint handler to confirm a request
 * actually came from relay before trusting its body - compares in constant time so
 * signature checks can't be timed to leak information about the expected value.
 */
export function verifyWebhookSignature(rawBody: string, signatureHeader: string, secret: string): boolean {
  const expected = signPayload(rawBody, secret);
  const expectedBytes = Buffer.from(expected, "utf8");
  const actualBytes = Buffer.from(signatureHeader, "utf8");

  if (expectedBytes.length !== actualBytes.length) {
    return false;
  }
  return timingSafeEqual(expectedBytes, actualBytes);
}
