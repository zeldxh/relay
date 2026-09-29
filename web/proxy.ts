import { NextRequest, NextResponse } from "next/server";

// Read at request time (not next.config's rewrites(), which bakes its destination into a
// build-time manifest - a runtime `docker run -e RELAY_API_URL=...` would silently have no
// effect, since the same built image needs to work across environments with different
// backend URLs).
export default function proxy(request: NextRequest) {
  if (!request.nextUrl.pathname.startsWith("/api/relay/")) {
    return NextResponse.next();
  }

  const relayApiUrl = process.env.RELAY_API_URL ?? "http://localhost:8080";
  const target = new URL(request.nextUrl.pathname.replace(/^\/api\/relay/, ""), relayApiUrl);
  target.search = request.nextUrl.search;
  return NextResponse.rewrite(target);
}
