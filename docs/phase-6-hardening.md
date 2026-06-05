# Phase 6 — Hardening

**Date:** 2026-06-05
**Scope:** Security, reliability, and cleanup pass before go-live.

---

## 1. WooCommerce API Keys — Moved to Backend

**Problem:** `VITE_WC_CONSUMER_KEY` and `VITE_WC_CONSUMER_SECRET` were Vite environment variables, meaning they were bundled into the browser JavaScript and visible to anyone who opened DevTools. An attacker could use them to read all WooCommerce orders or modify products.

**Fix:** All WooCommerce API calls are now proxied through the Spring Boot backend. The frontend never touches WooCommerce directly.

### Changes
| File | Change |
|---|---|
| `src/api/woocommerce.js` | Rewritten — calls `/api/menu` and `/api/orders/create` on the backend instead of WooCommerce directly |
| `vercel.json` | Removed `/wp-json/*` rewrite (no longer needed) |
| `vite.config.js` | Removed `/wp-json` dev proxy (no longer needed) |
| `WooCommerceService.java` | New service — calls WC API with Basic auth using server-side credentials |
| `MenuController.java` | New — `GET /api/menu?locationId=98` |
| `OrderProxyController.java` | New — `POST /api/orders/create` |

### Environment variables
- **Remove from Vercel:** `VITE_WC_CONSUMER_KEY`, `VITE_WC_CONSUMER_SECRET`, `VITE_WC_BASE_URL`
- **Keep on Railway:** `WOOCOMMERCE_URL`, `WOOCOMMERCE_KEY`, `WOOCOMMERCE_SECRET`

---

## 2. Deleted Public Test Controllers

**Problem:** `UberAuthTestController` and `UberDeliveryTestController` were publicly accessible REST endpoints with no authentication. Anyone who discovered them could trigger Uber API calls against the account.

**Fix:** Both controllers deleted entirely.

| Deleted file | Exposed endpoints |
|---|---|
| `UberAuthTestController.java` | `GET /api/test/auth` |
| `UberDeliveryTestController.java` | `GET /api/test/quote`, `POST /api/test/delivery` |

---

## 3. Uber Dispatch Failure Recovery

**Problem:** If the Uber Direct API rejected a delivery (bad address, outage, etc.) after the customer had already paid, the order was silently lost — no record in the database, no alert to staff, customer paid for nothing.

**Fix:** The Uber API call is now wrapped in a try-catch in `OrderRoutingService`. On failure:

1. The order is saved to `delivery_orders` with status `FAILED` and all customer details preserved
2. An `ERROR` log is written with customer name, phone, and WC order ID
3. On the dashboard, dispatch-failed orders surface in **New Orders** (not Past Orders) with a red border and a warning banner: *"Delivery dispatch failed — please call the customer"*
4. Customer phone number is visible in the expanded card

### Idempotency
A check was added before the Uber call in `handleDelivery` to detect duplicate WooCommerce webhook deliveries. Previously, a retry on a successfully processed order would log a false `UBER DISPATCH FAILED` error. Now it exits silently with a `WARN`.

### Changes
| File | Change |
|---|---|
| `OrderRoutingService.java` | Try-catch around Uber call; `persistFailedDelivery()` method added; idempotency pre-check added |
| `OrderSummaryResponse.java` | Added `uberDeliveryId` field (null = dispatch failed before Uber accepted) |
| `DashboardPage.jsx` | Dispatch-failed orders routed to New Orders section |
| `OrderCard.jsx` | `isDispatchFailed` flag; red border + warning banner |
| `dashboard.css` | `.order-card--dispatch-failed`, `.dispatch-failed-banner` styles |

---

## 4. Global Error Handler

**Problem:** Unhandled exceptions returned Spring Boot's default HTML whitepage error — not useful for a JSON API, and potentially leaking stack trace details to the client.

**Fix:** Added `GlobalExceptionHandler.java` (`@RestControllerAdvice`) that intercepts all unhandled exceptions and returns clean JSON.

### Response shape
```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again.",
  "timestamp": "2026-06-05T04:30:00.000Z"
}
```

### Handled cases
| Exception | HTTP status | Scenario |
|---|---|---|
| `NoResourceFoundException` | 404 | Unknown route |
| `MethodArgumentTypeMismatchException` | 400 | Bad path/query param type |
| `IllegalArgumentException` | 400 | App code rejects bad input |
| `Exception` (catch-all) | 500 | Anything else — full stack logged to Railway |

Full stack traces are logged server-side but never sent to the client.

---

## 5. Webhook Idempotency Audit

Both WooCommerce and Uber Direct can retry webhook delivery on timeout or failure. The audit confirmed existing checks and added two missing protections.

### WooCommerce webhook
| Scenario | Before | After |
|---|---|---|
| Retry on pickup order | Skipped (idempotency check) ✅ | Unchanged ✅ |
| Retry on successful delivery | False `UBER DISPATCH FAILED` ERROR log ❌ | Silent `WARN`, skipped ✅ |
| Retry on failed delivery | Skipped (idempotency check) ✅ | Unchanged ✅ |

### Uber webhook
| Scenario | Before | After |
|---|---|---|
| Same event delivered twice | Status set twice (harmless) ✅ | Unchanged ✅ |
| Late/out-of-order event (e.g. `PICKUP` after `DELIVERED`) | Status regresses ❌ | Ignored with `WARN` log ✅ |
| Terminal event (`FAILED`, `CANCELLED`, `RETURNED`) | Always applied ✅ | Unchanged ✅ |

**Status regression protection** added to `UberWebhookService`. A `STATUS_PROGRESSION` list defines the forward order. Any update that would move an order backwards in the progression is rejected.

---

## 6. Cleanup

| Item | File | Change |
|---|---|---|
| `System.out.println` | `AppConfig.java` | Removed leftover startup print statement |
| Unused imports | `AppConfig.java` | Removed `@Value`, `CommandLineRunner` imports |
| `@Component` on `@ConfigurationProperties` | `WooCommerceProperties`, `UberProperties`, `RestaurantProperties` | Removed — caused duplicate bean registration in Spring Boot 4.x. Registration is handled by `@EnableConfigurationProperties` in `DeliveryApplication.java` |

---

## Spring Boot 4.x Compatibility Notes

Issues encountered and resolved during this phase due to the Spring Boot 4.x / Spring Framework 7.x stack:

| Issue | Root cause | Fix |
|---|---|---|
| `UriComponentsBuilder.fromHttpUrl()` compile error | Method removed in Spring Framework 7.x | Replaced with `fromUriString()` |
| Duplicate `@ConfigurationProperties` beans | `@Component` + `@EnableConfigurationProperties` both register the bean | Removed `@Component` from all properties classes |
| `ObjectMapper` bean not found | Jackson auto-configuration not triggered | Instantiated `new ObjectMapper()` directly in `WooCommerceService` |
| `jackson-databind` not on classpath | Not transitively included in Spring Boot 4.x `spring-boot-starter-webmvc` | Added explicit dependency to `pom.xml` |
