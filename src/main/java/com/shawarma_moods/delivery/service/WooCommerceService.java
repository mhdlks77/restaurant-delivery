package com.shawarma_moods.delivery.service;

import com.shawarma_moods.delivery.config.WooCommerceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

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
        String base = resolveBase();

        UriComponentsBuilder productsUrlBuilder = UriComponentsBuilder
                .fromUriString(base + "/products")
                .queryParam("per_page", 100)
                .queryParam("status", "publish");
        if (locationId != null) {
            productsUrlBuilder.queryParam("restaurant_location", locationId);
        }
        String productsUrl   = productsUrlBuilder.toUriString();
        String categoriesUrl = UriComponentsBuilder
                .fromUriString(base + "/products/categories")
                .queryParam("per_page", 100)
                .queryParam("hide_empty", true)
                .toUriString();

        log.info("Fetching menu from WooCommerce — products: {}", productsUrl);

        HttpHeaders headers = buildAuthHeaders();

        try {
            // Fetch as String — avoids Jackson 2.x vs 3.x type mismatch on RestTemplate
            String productsJson    = restTemplate.exchange(productsUrl,    HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
            String categoriesJson  = restTemplate.exchange(categoriesUrl,  HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();

            JsonNode products   = objectMapper.readTree(productsJson);
            JsonNode categories = objectMapper.readTree(categoriesJson);

            log.info("WooCommerce returned {} products and {} categories before filtering",
                    products.size(), categories.size());

            return buildMenu(products, categories, locationId);

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("WooCommerce API rejected menu request: {} — body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("WooCommerce API error: " + e.getStatusCode(), e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Could not reach WooCommerce: {}", e.getMessage());
            throw new RuntimeException("Could not reach WooCommerce store", e);
        } catch (Exception e) {
            log.error("Unexpected error fetching menu: {}", e.getMessage(), e);
            throw new RuntimeException("Menu fetch failed", e);
        }
    }

    // ── Order proxy ──────────────────────────────────────────────────────────

    /**
     * Creates a WooCommerce order on behalf of the frontend.
     * Returns the full WC order object (includes id and payment_url).
     */
    public JsonNode createOrder(Map<String, Object> orderPayload) {
        String url = resolveBase() + "/orders";

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(orderPayload, buildAuthHeaders());

        try {
            String responseBody = restTemplate.exchange(url, HttpMethod.POST, request, String.class).getBody();
            JsonNode order = objectMapper.readTree(responseBody);
            log.info("WooCommerce order created: id={}", order.path("id").asText());
            return order;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("WooCommerce order creation failed: {} — body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("WooCommerce order creation failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Unexpected error creating order: {}", e.getMessage(), e);
            throw new RuntimeException("Order creation failed", e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns the WooCommerce REST API base URL (always ends with /wp-json/wc/v3).
     * Handles WOOCOMMERCE_URL being set as either:
     *   https://shawarma-moods.com
     *   https://shawarma-moods.com/wp-json/wc/v3
     */
    private String resolveBase() {
        String url = wcProps.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("WOOCOMMERCE_URL env var is not set on Railway");
        }
        url = url.strip().replaceAll("/$", "");
        if (url.endsWith("/wp-json/wc/v3")) {
            url = url.substring(0, url.length() - "/wp-json/wc/v3".length());
        }
        return url + "/wp-json/wc/v3";
    }

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
     */
    private JsonNode buildMenu(JsonNode products, JsonNode categories, Integer locationId) {
        var categoryMap = new java.util.LinkedHashMap<Integer, ObjectNode>();

        for (JsonNode cat : categories) {
            int id = cat.path("id").asInt();
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", cat.path("name").asText());
            node.set("items", objectMapper.createArrayNode());
            categoryMap.put(id, node);
        }

        for (JsonNode product : products) {
            // Location filter intentionally disabled — all products are available at all
            // active locations. Re-enable matchesLocation() when per-location menus are needed.

            ObjectNode item = objectMapper.createObjectNode();
            item.put("id",   product.path("id").asInt());
            item.put("name", product.path("name").asText());
            item.put("description", product.path("short_description").asText("").replaceAll("<[^>]+>", ""));

            String price = product.path("price").asText("");
            if (price.isBlank()) price = product.path("regular_price").asText("");
            if (price.isBlank()) item.putNull("price");
            else item.put("price", Double.parseDouble(price));

            JsonNode images = product.path("images");
            if (images.isEmpty()) item.putNull("image");
            else item.put("image", images.get(0).path("src").asText());

            for (JsonNode cat : product.path("categories")) {
                int catId = cat.path("id").asInt();
                if (categoryMap.containsKey(catId)) {
                    ((ArrayNode) categoryMap.get(catId).get("items")).add(item);
                }
            }
        }

        ArrayNode result = objectMapper.createArrayNode();
        categoryMap.values().stream()
                .filter(cat -> !cat.get("items").isEmpty())
                .forEach(result::add);

        return result;
    }

    private boolean matchesLocation(JsonNode product, int locationId) {
        for (JsonNode m : product.path("meta_data")) {
            if ("_yoast_wpseo_primary_restaurant_location".equals(m.path("key").asText())) {
                return String.valueOf(locationId).equals(m.path("value").asText());
            }
        }
        return false;
    }
}
