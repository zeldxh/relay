"use client";

import { useSyncExternalStore } from "react";
import { getStoredApiKey } from "./apiKey";
import { getStoredSession } from "./session";

function noopSubscribe() {
  return () => {};
}

function isConnected() {
  return Boolean(getStoredApiKey()) || Boolean(getStoredSession());
}

/**
 * Whether the browser holds either an API key or a dashboard session. Only reflects our own
 * connect/disconnect actions, which already trigger a re-render via navigation, so no
 * cross-tab subscription is needed here.
 */
export function useIsConnected(): boolean {
  return useSyncExternalStore(noopSubscribe, isConnected, () => false);
}
