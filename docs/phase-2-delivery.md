# Phase 2 — Delivery Quote & Order Creation

## Overview
Implements delivery quote requests and delivery creation via the Uber Direct API.
All deliveries are persisted to PostgreSQL with idempotency protection.

---

## Flow

```
WooCommerce order received
        ↓
UberDeliveryService.createDelivery()
        ↓
Idempotency check — delivery already exists for this WC order ID?
        ↓ (if not)
getQuote(dropoffAddress) → returns Uber quote ID
        ↓
Build delivery request (pickup, dropoff, manifest items)
        ↓
POST /v1/customers/{customerId}/deliveries
        ↓
Persist DeliveryOrder to DB (status: CREATED)
        ↓
Return delivery ID + tracking URL
```

---

## Key Classes

| Class | Location | Responsibility |
|---|---|---|
| `DeliveryOrder` | `model/` | JPA entity — persisted delivery record |
| `DeliveryStatus` | `model/` | Enum of all possible delivery states |
| `OrderType` | `model/` | DELIVERY or PICKUP |
| `UberManifestItem` | `dto/` | Represents one item in the delivery manifest |
| `UberDeliveryRequest` | `dto/` | Full payload sent to Uber on delivery creation |
| `UberDeliveryResponse` | `dto/` | Uber's response — delivery ID, status, tracking URL |
| `UberQuoteRequest` | `dto/` | Pickup + dropoff addresses for quote request |
| `UberQuoteResponse` | `dto/` | Uber's response — fee, currency, estimated times |
| `UberDeliveryService` | `service/` | Quote and delivery creation logic |
| `RestaurantProperties` | `config/` | Binds `restaurant.*` properties from config |
| `DeliveryOrderRepository` | `repository/` | DB access for delivery orders |
| `UberDeliveryTestController` | `controller/` | Temporary test endpoints — remove before prod |

---

## What is a Manifest?

Uber Direct requires a manifest with every delivery request. The manifest
describes what the driver is picking up — used by the driver at the restaurant
and by Uber for insurance purposes if something goes wrong in transit.

At minimum it requires:
- At least one item with a `name`, `quantity`, `size`, and `price`
- `size` accepts `"small"`, `"medium"`, or `"large"` — use `"medium"` for food orders

In Phase 2 a single placeholder manifest item is used:
```json
{
  "name": "Food order",
  "quantity": 1,
  "size": "medium",
  "price": <orderValueCents>
}
```

**Phase 4 will replace this placeholder** with real line items extracted from
the WooCommerce webhook payload (e.g. `"2x Burger, 1x Fries"`).

---

## Idempotency

Before creating a delivery, the service checks whether a `DeliveryOrder`
already exists for the given `woocommerceOrderId`. If one exists, it throws
rather than creating a duplicate delivery.

This protects against WooCommerce webhook retries triggering multiple Uber
deliveries for the same order — a real risk in production.

If you need to re-test the same order ID locally, clear the record first:
```sql
DELETE FROM delivery_orders WHERE woocommerce_order_id = 'TEST-001';
```

---

## Address Requirements

Uber Direct validates both addresses before accepting a quote or delivery request.

**Rules:**
- Both pickup and dropoff must be real, geocodable street addresses
- Dropoff must be within Uber's serviceable delivery zone relative to pickup
- Partial or placeholder addresses will be rejected with a `400 Bad Request`

**Common failure:** using a test address in a different city or region from
the restaurant. Always use a real nearby address when testing.

**Pickup address** is set via environment variable and maps to the restaurant location:
```properties
RESTAURANT_ADDRESS=123 Rue Saint-Denis, Montreal, QC H2X 3K3
```

---

## Database Table: `delivery_orders`

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT | Auto-generated primary key |
| `woocommerce_order_id` | VARCHAR | Unique — WooCommerce order reference |
| `uber_delivery_id` | VARCHAR | Returned by Uber on creation |
| `status` | VARCHAR | Enum — see delivery status lifecycle below |
| `customer_name` | VARCHAR | Dropoff contact name |
| `customer_address` | VARCHAR | Dropoff street address |
| `customer_phone` | VARCHAR | Dropoff contact phone |
| `order_type` | VARCHAR | DELIVERY or PICKUP |
| `created_at` | TIMESTAMP | Set automatically on insert |
| `updated_at` | TIMESTAMP | Updated automatically on every change |

### Delivery Status Lifecycle

```
PENDING → CREATED → PICKUP → DROPOFF → DELIVERED
                           ↘ FAILED → RETURNED
                           ↘ CANCELLED
```

Status is updated by Uber webhook events in Phase 3.

---

## Environment Variables Required

| Variable | Description |
|---|---|
| `UBER_CLIENT_ID` | From Uber Developer Dashboard |
| `UBER_CLIENT_SECRET` | From Uber Developer Dashboard |
| `UBER_CUSTOMER_ID` | From Uber Developer Dashboard |
| `RESTAURANT_NAME` | Display name sent to Uber as pickup contact |
| `RESTAURANT_ADDRESS` | Full street address of the restaurant location |
| `RESTAURANT_PHONE` | Restaurant phone number sent to Uber as pickup contact |

---

## Test Endpoints

> **Remove `UberDeliveryTestController` before deploying to production.**

### Get a delivery quote (safe — creates nothing)
```
GET /api/test/quote?dropoffAddress=1234 Rue Sainte-Catherine O, Montreal, QC H3G 1P1
```

Expected response:
```json
{
  "id": "quote_xxxxxxxxxxxxxxxx",
  "fee": 599,
  "currency": "CAD",
  "estimatedPickupTime": 1234567890,
  "estimatedDropoffTime": 1234567890
}
```

### Create a sandbox delivery
```
POST /api/test/delivery
Content-Type: application/json

{
  "woocommerceOrderId": "TEST-001",
  "customerName": "John Doe",
  "customerAddress": "1234 Rue Sainte-Catherine O, Montreal, QC H3G 1P1",
  "customerPhone": "+15141234567",
  "orderValueCents": "2500"
}
```

Expected response:
```json
{
  "uberDeliveryId": "del_xxxxxxxxxxxxxxxx",
  "status": "pending",
  "trackingUrl": "https://track.uber.com/..."
}
```

### Verify DB record
```sql
SELECT * FROM delivery_orders WHERE woocommerce_order_id = 'TEST-001';
```

---

## Known Issues & Lessons Learned

| Issue | Cause | Resolution |
|---|---|---|
| `400 Bad Request` on delivery creation | Missing `manifest_items` array | Added `UberManifestItem` DTO and populated `manifest_items` in request |
| `400 Bad Request` on quote | Dropoff address outside serviceable zone | Use a real nearby address within the same city as the restaurant |
| Duplicate delivery on retry | Missing idempotency check | Added `findByWoocommerceOrderId` check before creation |

---

## Git

Branch: `phase-2/delivery-quote-and-creation`
Commit: `phase-2: delivery quote, order creation, manifest items, db persistence, idempotency`