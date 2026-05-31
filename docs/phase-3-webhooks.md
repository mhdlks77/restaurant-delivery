# Phase 3 — Webhook Handler

## Overview
Receives and processes Uber Direct webhook events. Updates delivery status
in the database as the delivery progresses through its lifecycle.

## Webhook Endpoint

Public — must be reachable by Uber's servers.
Registered in the Uber Developer Dashboard.

## Security — Signature Verification
Every incoming request is verified using HMAC-SHA256 before processing.
Uber signs the raw request body with the webhook secret configured in the dashboard.

Verification flow:
1. Extract `X-Postmates-Signature` header
2. Compute HMAC-SHA256 of raw body using `UBER_WEBHOOK_SECRET`
3. Compare using constant-time comparison (prevents timing attacks)
4. Reject if mismatch — log warning, return 200 anyway (no information leakage)

## Why Always Return 200
Uber retries webhook delivery if it receives anything other than 2xx.
Returning 4xx or 5xx on a processing failure would cause Uber to retry,
potentially leading to duplicate status updates. Always return 200 and
handle failures internally via logging.

## Event Types Handled

| Uber Event | Internal Status |
|---|---|
| `delivery.created` | `CREATED` |
| `delivery.pickup` | `PICKUP` |
| `delivery.dropoff` | `DROPOFF` |
| `delivery.delivered` | `DELIVERED` |
| `delivery.failed` | `FAILED` |
| `delivery.returned` | `RETURNED` |
| `delivery.cancelled` | `CANCELLED` |

## Key Classes

| Class | Location | Responsibility |
|---|---|---|
| `UberWebhookController` | `controller/` | Receives POST, verifies signature, delegates |
| `UberWebhookService` | `service/` | Routes events, updates DB status |
| `UberWebhookEvent` | `dto/` | Maps incoming webhook JSON |
| `UberWebhookMeta` | `dto/` | Nested meta block — contains delivery ID |
| `WebhookSignatureVerifier` | `util/` | HMAC-SHA256 signature verification |

## Environment Variables Required

| Variable | Description |
|---|---|
| `UBER_WEBHOOK_SECRET` | Copied from Uber Developer Dashboard after registering webhook URL |

## Registering the Webhook
1. Deploy app to Railway and confirm `/api/health` returns UP
2. Go to developer.uber.com → your app → Webhooks
3. Add URL: `https://your-app.up.railway.app/webhooks/uber`
4. Select all delivery event types
5. Copy the secret and set `UBER_WEBHOOK_SECRET` in Railway environment variables

## Local Testing
Use the `.http` files in `requests/phase-3-webhooks.http` with a temporary
signature bypass. Replace `del_your_actual_delivery_id_here` with a real
delivery ID from a Phase 2 sandbox test.

Verify status updates via SQL:
```sql
SELECT woocommerce_order_id, uber_delivery_id, status, updated_at
FROM delivery_orders ORDER BY updated_at DESC;
```

## Deployment
Hosted on Railway. All environment variables set via Railway dashboard.
Local DB (Docker) is replaced by Railway-provisioned PostgreSQL in production.