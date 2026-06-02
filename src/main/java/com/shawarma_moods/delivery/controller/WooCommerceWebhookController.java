package com.shawarma_moods.delivery.controller;

import com.shawarma_moods.delivery.config.WooCommerceProperties;
import com.shawarma_moods.delivery.dto.WooCommerceOrder;
import com.shawarma_moods.delivery.service.OrderRoutingService;
import com.shawarma_moods.delivery.util.WooCommerceSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhooks")
public class WooCommerceWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WooCommerceWebhookController.class);

    private final OrderRoutingService orderRoutingService;
    private final WooCommerceSignatureVerifier signatureVerifier;
    private final WooCommerceProperties wooCommerceProperties;
    private final ObjectMapper objectMapper;

    public WooCommerceWebhookController(
            OrderRoutingService orderRoutingService,
            WooCommerceSignatureVerifier signatureVerifier,
            WooCommerceProperties wooCommerceProperties,
            ObjectMapper objectMapper
    ) {
        this.orderRoutingService = orderRoutingService;
        this.signatureVerifier = signatureVerifier;
        this.wooCommerceProperties = wooCommerceProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Receives WooCommerce order webhooks.
     * Returns 200 immediately — WooCommerce retries on anything else.
     * Verifies signature, parses order, routes to delivery or pickup handler.
     */
    @PostMapping("/woocommerce")
    public ResponseEntity<Void> handleWooCommerceWebhook(
            @RequestHeader(value = "X-WC-Webhook-Signature", required = false) String signature,
            @RequestBody byte[] rawBody
    ) {
        if (!signatureVerifier.verify(rawBody, signature, wooCommerceProperties.getWebhookSecret())) {
            log.warn("Rejected WooCommerce webhook — invalid signature");
            return ResponseEntity.ok().build();
        }

        try {
            WooCommerceOrder order = objectMapper.readValue(rawBody, WooCommerceOrder.class);
            log.info("Received WooCommerce webhook for order {}", order.getId());
            orderRoutingService.routeOrder(order);
        } catch (Exception e) {
            log.error("Failed to process WooCommerce webhook: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
