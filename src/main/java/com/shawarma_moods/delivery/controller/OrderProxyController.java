package com.shawarma_moods.delivery.controller;

import tools.jackson.databind.JsonNode;
import com.shawarma_moods.delivery.service.OrderRequestValidator;
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
    private final OrderRequestValidator orderRequestValidator;

    public OrderProxyController(WooCommerceService wooCommerceService,
                                OrderRequestValidator orderRequestValidator) {
        this.wooCommerceService = wooCommerceService;
        this.orderRequestValidator = orderRequestValidator;
    }

    /**
     * Proxies order creation to WooCommerce.
     * Keeps WC credentials server-side — never exposed to the browser.
     *
     * The incoming payload is validated and rebuilt server-side (whitelist) so a
     * caller cannot create junk orders, send malformed data, manipulate prices,
     * or force the payment status. Invalid input → 400 via GlobalExceptionHandler.
     *
     * POST /api/orders/create
     * Returns: WC order object including id and payment_url
     */
    @PostMapping("/create")
    public ResponseEntity<JsonNode> createOrder(@RequestBody Map<String, Object> orderPayload) {
        log.info("Order creation requested");
        Map<String, Object> sanitized = orderRequestValidator.validateAndSanitize(orderPayload);
        JsonNode order = wooCommerceService.createOrder(sanitized);
        return ResponseEntity.ok(order);
    }
}
