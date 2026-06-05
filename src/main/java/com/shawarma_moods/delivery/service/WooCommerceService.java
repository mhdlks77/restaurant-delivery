package com.shawarma_moods.delivery.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shawarma_moods.delivery.config.WooCommerceProperties;
// ObjectMapper instantiated directly — not injected via Spring context
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Base64;
import java.util.Map;

@Service
public class WooCommerceService {

    private static final Logger log = LoggerFactory.getLogger(WooCommerceService.class);

    private final WooCommerceProperties wcProps;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WooCommerceService(WooCommerceProperties wcProps,
                              RestTemplate restTemplate) {
        this.wcProps      = wcProps;
        this.restTemplate = restTemplate;
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    /**
     * Fetches all published products and categories from WooCommerce,
     * filters by location, and returns a categorised menu.
     */
    public JsonNode getMenu(Integer locationId) {
        String wcUrl = wcProps.getUrl();
        if (wcUrl == null || wcUrl.isBlank()) {
            throw new IllegalStateException("WOOCOMMERCE_URL env var is not set on Railway");
        }
        // Normalise — strip trailing slash, then strip /wp-json/wc/v3 if already present
        wcUrl = wcUrl.stripTrailing().replaceAll("/$", "");
        if (wcUrl.endsWith("/wp-json/wc/v3")) {
            wcUrl = wcUrl.substring(0, wcUrl.length() - "/wp-json/wc/v3".length());
        }
        String base = wcUrl + "/wp-json/wc/v3";

        // Build product URL — attempt server-side taxonomy filter
        UriComponentsBuilder productsUrlBuilder = UriComponentsBuilder
                .fromUriString(base + "/products")
                .queryParam("per_page", 100)
                .queryParam("status", "publish");
        if (locationId != null) {
            productsUrlBuilder.queryParam("restaurant_location", locationId);
        }
        String productsUrl  = productsUrlBuilder.toUriString();

        String categoriesUrl = UriComponentsBuilder
                .fromUriString(base + "/products/categories")
                .queryParam("per_page", 100)
                .queryParam("hide_empty", true)
                .toUriString();

        log.info("Fetching menu from WooCommerce — products: {}", productsUrl);

        HttpHeaders headers = buildAuthHeaders();

        try {
            ResponseEntity<JsonNode> productsRes = restTemplate.exchange(
                    productsUrl, HttpMethod.GET,
                    new HttpEntity<>(headers), JsonNode.class);

            ResponseEntity<JsonNode> categoriesRes = restTemplate.exchange(
                    categoriesUrl, HttpMethod.GET,
                    new HttpEntity<>(headers), JsonNode.class);

            JsonNode products   = productsRes.getBody();
            JsonNode categories = categoriesRes.getBody();

            log.info("Menu fetched — {} products, {} categories",
                    products != null ? products.size() : 0,
                    categories != null ? categories.size() : 0);

            return buildMenu(products, categories, locationId);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("WooCommerce API rejected menu request: {} {} — body: {}",
                    e.getStatusCode(), e.getStatusText(), e.getResponseBodyAsString());
            throw new RuntimeException("WooCommerce API error: " + e.getStatusCode(), e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Could not reach WooCommerce at {}: {}", wcUrl, e.getMessage());
            throw new RuntimeException("Could not reach WooCommerce store", e);
        }
    }

    // ── Order proxy ──────────────────────────────────────────────────────────

    /**
     * Creates a WooCommerce order on behalf of the frontend.
     * Returns the full WC order object (including payment_url and id).
     */
    public JsonNode createOrder(Map<String, Object> orderPayload) {
        String url = wcProps.getUrl() + "/wp-json/wc/v3/orders";

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderPayload, buildAuthHeaders());

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                url, HttpMethod.POST, request, JsonNode.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("WooCommerce order creation failed: " + response.getStatusCode());
        }

        log.info("WooCommerce order created: id={}", response.getBody().path("id").asText());
        return response.getBody();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private HttpHeaders buildAuthHeaders() {
        String credentials = Base64.getEncoder()
                .encodeToString((wcProps.getKey() + ":" + wcProps.getSecret()).getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + credentials);
        return headers;
    }

    /**
     * Builds the categorised menu structure the frontend expects:
     * [ { name, items: [ { id, name, description, price, image } ] } ]
     *
     * Applies client-side meta filter on _yoast_wpseo_primary_restaurant_location
     * as a fallback in case the taxonomy param isn't picked up server-side.
     */
    private JsonNode buildMenu(JsonNode products, JsonNode categories, Integer locationId) {
        // Build category map: id → { name, items[] }
        var categoryMap = new java.util.LinkedHashMap<Integer, java.util.Map<String, Object>>();
        if (categories != null) {
            for (JsonNode cat : categories) {
                int id = cat.path("id").asInt();
                categoryMap.put(id, new java.util.LinkedHashMap<>(Map.of(
                        "name",  cat.path("name").asText(),
                        "items", new java.util.ArrayList<>()
                )));
            }
        }

        if (products != null) {
            for (JsonNode product : products) {
                // Client-side location filter fallback
                if (locationId != null && !matchesLocation(product, locationId)) {
                    continue;
                }

                var item = new java.util.LinkedHashMap<String, Object>();
                item.put("id",          product.path("id").asInt());
                item.put("name",        product.path("name").asText());
                item.put("description", product.path("short_description").asText("").replaceAll("<[^>]+>", ""));

                String price = product.path("price").asText("");
                if (price.isBlank()) price = product.path("regular_price").asText("");
                item.put("price", price.isBlank() ? null : Double.parseDouble(price));

                JsonNode images = product.path("images");
                item.put("image", images.isEmpty() ? null : images.get(0).path("src").asText(null));

                // Assign to each of its categories
                for (JsonNode cat : product.path("categories")) {
                    int catId = cat.path("id").asInt();
                    if (categoryMap.containsKey(catId)) {
                        @SuppressWarnings("unchecked")
                        var items = (java.util.List<Object>) categoryMap.get(catId).get("items");
                        items.add(item);
                    }
                }
            }
        }

        // Filter out empty categories and return as JsonNode
        var result = categoryMap.values().stream()
                .filter(cat -> {
                    @SuppressWarnings("unchecked")
                    var items = (java.util.List<?>) cat.get("items");
                    return !items.isEmpty();
                })
                .toList();

        return objectMapper.valueToTree(result);
    }

    private boolean matchesLocation(JsonNode product, int locationId) {
        JsonNode meta = product.path("meta_data");
        for (JsonNode m : meta) {
            if ("_yoast_wpseo_primary_restaurant_location".equals(m.path("key").asText())) {
                return String.valueOf(locationId).equals(m.path("value").asText());
            }
        }
        return false;
    }
}
