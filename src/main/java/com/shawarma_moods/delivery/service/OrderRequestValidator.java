package com.shawarma_moods.delivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes the order payload coming from the public
 * /api/orders/create endpoint before it is forwarded to WooCommerce.
 *
 * Security model: WHITELIST. The returned payload is rebuilt from scratch —
 * only fields we explicitly accept are forwarded. This prevents:
 *   - junk/spam orders (structure + bounds enforced)
 *   - malformed payloads reaching WooCommerce
 *   - price manipulation (client-supplied prices/totals are dropped; WooCommerce
 *     computes prices from product_id server-side)
 *   - the client forcing set_paid / payment status
 *
 * Throws IllegalArgumentException on any violation — GlobalExceptionHandler
 * maps that to HTTP 400 with a clean message.
 */
@Component
public class OrderRequestValidator {

    private static final Logger log = LoggerFactory.getLogger(OrderRequestValidator.class);

    private static final int MAX_LINE_ITEMS    = 50;
    private static final int MAX_QTY_PER_ITEM   = 50;
    private static final int MAX_NOTE_LENGTH    = 500;
    private static final int MAX_FIELD_LENGTH   = 200;

    private static final Set<String> VALID_ORDER_TYPES = Set.of("delivery", "pickup");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    // Line-item meta keys we allow through (trio configuration only)
    private static final Set<String> ALLOWED_LINE_ITEM_META =
            Set.of("_trio", "_trio_side", "_trio_drink");

    /**
     * Validates the incoming payload and returns a clean, server-controlled
     * payload safe to forward to WooCommerce.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> validateAndSanitize(Map<String, Object> raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Order payload is missing.");
        }

        // ── order_type (from meta_data) ──────────────────────────────────────
        String orderType = extractOrderType(raw);
        if (!VALID_ORDER_TYPES.contains(orderType)) {
            throw new IllegalArgumentException("Invalid or missing order_type.");
        }
        boolean isDelivery = "delivery".equals(orderType);

        // ── billing ──────────────────────────────────────────────────────────
        Object billingObj = raw.get("billing");
        if (!(billingObj instanceof Map<?, ?> billingRaw)) {
            throw new IllegalArgumentException("Billing details are required.");
        }
        Map<String, Object> billing = sanitizeBilling((Map<String, Object>) billingRaw, isDelivery);

        // ── line_items ───────────────────────────────────────────────────────
        Object lineItemsObj = raw.get("line_items");
        if (!(lineItemsObj instanceof List<?> lineItemsRaw) || lineItemsRaw.isEmpty()) {
            throw new IllegalArgumentException("Your order must contain at least one item.");
        }
        if (lineItemsRaw.size() > MAX_LINE_ITEMS) {
            throw new IllegalArgumentException("Too many distinct items in one order.");
        }
        List<Map<String, Object>> lineItems = sanitizeLineItems(lineItemsRaw);

        // ── shipping (delivery only) ─────────────────────────────────────────
        Map<String, Object> shipping = new LinkedHashMap<>();
        if (isDelivery && raw.get("shipping") instanceof Map<?, ?> shippingRaw) {
            shipping = sanitizeShipping((Map<String, Object>) shippingRaw);
        }

        // ── customer_note ────────────────────────────────────────────────────
        String customerNote = null;
        if (raw.get("customer_note") instanceof String note && !note.isBlank()) {
            if (note.length() > MAX_NOTE_LENGTH) {
                throw new IllegalArgumentException("Order note is too long.");
            }
            customerNote = note.trim();
        }

        // ── location_id (passed through as order meta) ───────────────────────
        String locationId = extractMetaValue(raw, "location_id");

        // ── Rebuild a clean, server-controlled payload ───────────────────────
        Map<String, Object> clean = new LinkedHashMap<>();
        // Server controls payment fields — never trust the client here
        clean.put("payment_method", "stripe");
        clean.put("payment_method_title", "Credit Card");
        clean.put("set_paid", false);
        clean.put("billing", billing);
        clean.put("shipping", shipping);
        clean.put("line_items", lineItems);
        if (customerNote != null) {
            clean.put("customer_note", customerNote);
        }

        List<Map<String, Object>> metaData = new ArrayList<>();
        metaData.add(Map.of("key", "order_type", "value", orderType));
        metaData.add(Map.of("key", "location_id", "value", locationId != null ? locationId : ""));
        clean.put("meta_data", metaData);

        log.info("Order payload validated — type: {}, items: {}", orderType, lineItems.size());
        return clean;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String extractOrderType(Map<String, Object> raw) {
        return extractMetaValue(raw, "order_type");
    }

    @SuppressWarnings("unchecked")
    private String extractMetaValue(Map<String, Object> raw, String key) {
        if (raw.get("meta_data") instanceof List<?> meta) {
            for (Object entry : meta) {
                if (entry instanceof Map<?, ?> m
                        && key.equals(String.valueOf(m.get("key")))) {
                    Object v = m.get("value");
                    return v != null ? String.valueOf(v) : null;
                }
            }
        }
        return null;
    }

    private Map<String, Object> sanitizeBilling(Map<String, Object> billing, boolean isDelivery) {
        Map<String, Object> clean = new LinkedHashMap<>();
        clean.put("first_name", requireField(billing, "first_name", "First name"));
        clean.put("last_name",  requireField(billing, "last_name",  "Last name"));

        String email = requireField(billing, "email", "Email");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email address.");
        }
        clean.put("email", email);

        String phone = requireField(billing, "phone", "Phone");
        if (phone.replaceAll("\\D", "").length() < 10) {
            throw new IllegalArgumentException("Invalid phone number.");
        }
        clean.put("phone", phone);

        // Address — required for delivery, optional for pickup
        clean.put("address_1", optionalField(billing, "address_1", isDelivery, "Street address"));
        clean.put("address_2", optionalField(billing, "address_2", false, null));
        clean.put("city",      optionalField(billing, "city",      isDelivery, "City"));
        clean.put("postcode",  optionalField(billing, "postcode",  isDelivery, "Postal code"));
        clean.put("country", "CA");
        return clean;
    }

    private Map<String, Object> sanitizeShipping(Map<String, Object> shipping) {
        Map<String, Object> clean = new LinkedHashMap<>();
        clean.put("first_name", str(shipping.get("first_name")));
        clean.put("last_name",  str(shipping.get("last_name")));
        clean.put("address_1",  str(shipping.get("address_1")));
        clean.put("address_2",  str(shipping.get("address_2")));
        clean.put("city",       str(shipping.get("city")));
        clean.put("postcode",   str(shipping.get("postcode")));
        clean.put("country", "CA");
        return clean;
    }

    private List<Map<String, Object>> sanitizeLineItems(List<?> rawItems) {
        List<Map<String, Object>> clean = new ArrayList<>();
        for (Object o : rawItems) {
            if (!(o instanceof Map<?, ?> itemRaw)) {
                throw new IllegalArgumentException("Malformed line item.");
            }

            int productId = positiveInt(itemRaw.get("product_id"), "product_id");
            int quantity  = positiveInt(itemRaw.get("quantity"), "quantity");
            if (quantity > MAX_QTY_PER_ITEM) {
                throw new IllegalArgumentException("Quantity exceeds the per-item limit.");
            }

            // Rebuild — NOTE: no price/total/subtotal fields are copied, so the
            // client cannot influence pricing. WooCommerce prices by product_id.
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("product_id", productId);
            item.put("quantity", quantity);

            Object variationId = itemRaw.get("variation_id");
            if (variationId != null) {
                item.put("variation_id", positiveInt(variationId, "variation_id"));
            }

            // Whitelist trio meta only
            if (itemRaw.get("meta_data") instanceof List<?> metaRaw) {
                List<Map<String, Object>> meta = new ArrayList<>();
                for (Object m : metaRaw) {
                    if (m instanceof Map<?, ?> entry) {
                        String key = str(entry.get("key"));
                        if (ALLOWED_LINE_ITEM_META.contains(key)) {
                            String value = str(entry.get("value"));
                            if (value.length() <= MAX_FIELD_LENGTH) {
                                meta.add(Map.of("key", key, "value", value));
                            }
                        }
                    }
                }
                if (!meta.isEmpty()) item.put("meta_data", meta);
            }

            clean.add(item);
        }
        return clean;
    }

    // ── Low-level field helpers ──────────────────────────────────────────────

    private String requireField(Map<String, Object> map, String key, String label) {
        String v = str(map.get(key)).trim();
        if (v.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        if (v.length() > MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException(label + " is too long.");
        }
        return v;
    }

    private String optionalField(Map<String, Object> map, String key, boolean required, String label) {
        String v = str(map.get(key)).trim();
        if (v.isEmpty() && required) {
            throw new IllegalArgumentException(label + " is required for delivery.");
        }
        if (v.length() > MAX_FIELD_LENGTH) {
            throw new IllegalArgumentException((label != null ? label : key) + " is too long.");
        }
        return v;
    }

    private int positiveInt(Object value, String label) {
        if (value instanceof Number n) {
            int i = n.intValue();
            if (i <= 0) throw new IllegalArgumentException("Invalid " + label + ".");
            return i;
        }
        if (value instanceof String s) {
            try {
                int i = Integer.parseInt(s.trim());
                if (i <= 0) throw new IllegalArgumentException("Invalid " + label + ".");
                return i;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid " + label + ".");
            }
        }
        throw new IllegalArgumentException("Missing or invalid " + label + ".");
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
