package com.shawarma_moods.delivery.controller;

import tools.jackson.databind.JsonNode;
import com.shawarma_moods.delivery.service.WooCommerceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderProxyController {

    private static final Logger log = LoggerFactory.getLogger(OrderProxyController.class);

    private final WooCommerceService wooCommerceService;

    public OrderProxyController(WooCommerceService wooCommerceService) {
        this.wooCommerceService = wooCommerceService;
    }

    /**
     * Proxies order creation to WooCommerce.
     * Keeps WC credentials server-side — never exposed to the browser.
     *
     * POST /api/orders/create
     * Body: WooCommerce order payload (same shape the frontend used to send directly)
     * Returns: WC order object including id and payment_url
     */
    @PostMapping("/create")
    public ResponseEntity<JsonNode> createOrder(@RequestBody Map<String, Object> orderPayload) {
        log.info("Order creation requested");
        JsonNode order = wooCommerceService.createOrder(orderPayload);
        return ResponseEntity.ok(order);
    }
}
