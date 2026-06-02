# Phase 4c — Backend Order Routing

## Overview
Receives WooCommerce order webhooks and routes them based on order_type
metadata set by the React frontend. Delivery orders go to Uber Direct,
pickup orders are saved for the restaurant dashboard.

## Webhook Endpoint
POST /webhooks/woocommerce

## Routing Logic
WooCommerce webhook received
↓
Verify X-WC-Webhook-Signature (HMAC-SHA256, base64)
↓
Parse WooCommerceOrder JSON
↓
Read meta_data.order_type
↓
"delivery" → UberDeliveryService.createDelivery()
"pickup"   → PickupOrderRepository.save()
unknown    → log warning, no action

## Order Metadata (set by React frontend)
| Key | Values | Purpose |
|---|---|---|
| `order_type` | `delivery` / `pickup` | Routing decision |
| `location_id` | `1` (expandable) | Restaurant location |

## Key Classes
| Class | Location | Responsibility |
|---|---|---|
| `WooCommerceWebhookController` | `controller/` | Receives webhook, verifies signature |
| `OrderRoutingService` | `service/` | Routes by order type |
| `WooCommerceOrder` | `dto/` | Maps WC webhook payload |
| `WooCommerceSignatureVerifier` | `util/` | HMAC-SHA256 base64 verification |
| `PickupOrder` | `model/` | JPA entity for pickup orders |
| `PickupOrderRepository` | `repository/` | DB access for pickup orders |

## Database Tables
- `delivery_orders` — populated for delivery orders (Phase 2)
- `pickup_orders` — populated for pickup orders (new in this phase)

## Signature Verification
WooCommerce uses HMAC-SHA256 with base64 encoding (unlike Uber which uses hex).
Secret is configured in WooCommerce dashboard and stored as WOOCOMMERCE_WEBHOOK_SECRET.