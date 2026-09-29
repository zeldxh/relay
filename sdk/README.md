# @zeldxh/relay-sdk

Node.js client for the relay webhook delivery API: register endpoints and topics,
publish events, manage subscriptions, and verify inbound webhooks.

Zero runtime dependencies, built on Node 18+'s global `fetch` and `node:crypto`.

## Setup

```ts
import { RelayClient } from "@zeldxh/relay-sdk";

const relay = new RelayClient({
  apiKey: process.env.RELAY_API_KEY!,
  // baseUrl defaults to http://localhost:8080; point it at your deployment in prod
  baseUrl: process.env.RELAY_BASE_URL,
});
```

## Publishing events

An event published to a topic fans out to every endpoint subscribed to it. An endpoint
must be verified (see below) before it can be subscribed.

```ts
const topic = await relay.createTopic("order.created");

const endpoint = await relay.createEndpoint("orders-webhook", "https://example.com/hooks/orders");
await relay.verifyEndpoint(endpoint.id); // pings the URL with a signed test payload

await relay.subscribe(topic.id, endpoint.id);

const { eventId, deliveryIds } = await relay.publish(topic.id, { orderId: 42, total: 19.99 });
```

## Verifying inbound webhooks

Every delivery is signed with the endpoint's own secret (returned once, from
`createEndpoint`). Verify it before trusting the request body:

```ts
import { verifyWebhookSignature } from "@zeldxh/relay-sdk";

app.post("/hooks/orders", express.text({ type: "*/*" }), (req, res) => {
  const signature = req.header("x-relay-signature");
  if (!signature || !verifyWebhookSignature(req.body, signature, process.env.ORDERS_ENDPOINT_SECRET!)) {
    return res.sendStatus(401);
  }

  const payload = JSON.parse(req.body);
  // ... handle payload
  res.sendStatus(200);
});
```

`req.body` must be the exact raw bytes Relay signed, decoded as UTF-8 **text**.
This is why the route above uses `express.text()` instead of the usual
`express.json()`, which would hand you an already-parsed object that no longer
matches the signed bytes.

## Inspecting and replaying deliveries

```ts
const failed = await relay.listDeliveries({ status: "FAILED", page: 0, size: 20 });
for (const delivery of failed.content) {
  await relay.replayDelivery(delivery.id);
}
```

## Error handling

Failed requests throw `RelayApiError`, which carries the HTTP `status` and the
response body as `message`:

```ts
import { RelayApiError } from "@zeldxh/relay-sdk";

try {
  await relay.verifyEndpoint(endpoint.id);
} catch (err) {
  if (err instanceof RelayApiError && err.status === 422) {
    console.error("endpoint didn't respond to the verification ping");
  } else {
    throw err;
  }
}
```

## Development

```
pnpm install
pnpm test
pnpm run build
```
