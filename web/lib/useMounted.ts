"use client";

import { useSyncExternalStore } from "react";

function noopSubscribe() {
  return () => {};
}

/** True once hydrated on the client. Avoids a server/client mismatch for anything that only
 * knows its real value in the browser (theme, localStorage-backed state). */
export function useMounted(): boolean {
  return useSyncExternalStore(noopSubscribe, () => true, () => false);
}
