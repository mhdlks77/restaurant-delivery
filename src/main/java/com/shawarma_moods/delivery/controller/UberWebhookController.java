package com.shawarma_moods.delivery.controller;

import com.shawarma_moods.delivery.config.UberProperties;
import com.shawarma_moods.delivery.dto.UberWebhookEvent;
import com.shawarma_moods.delivery.service.UberWebhookService;
import com.shawarma_moods.delivery.util.WebhookSignatureVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhooks")
public class UberWebhookController {

    private static final Logger log = LoggerFactory.getLogger(UberWebhookController.class);

    private final UberWebhookService uberWebhookService;
    private final WebhookSignatureVerifier signatureVerifier;
    private final UberProperties uberProperties;
    private final ObjectMapper objectMapper;

    public UberWebhookController(
            UberWebhookService uberWebhookService,
            WebhookSignatureVerifier signatureVerifier,
            UberProperties uberProperties,
            ObjectMapper objectMapper
    ) {
        this.uberWebhookService = uberWebhookService;
        this.signatureVerifier = signatureVerifier;
        this.uberProperties = uberProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Receives Uber Direct webhook events.
     * Returns 200 OK immediately before any processing — Uber retries
     * on slow responses, which could cause duplicate event handling.
     * Signature is verified before any processing begins.
     */
    @PostMapping("/uber")
    public ResponseEntity<Void> handleUberWebhook(
            @RequestHeader(value = "X-Postmates-Signature", required = false) String signature,
            @RequestBody byte[] rawBody
    ) {
        // Always return 200 immediately — process asynchronously after
        // Verify signature first — reject unsigned requests
        if (!signatureVerifier.verify(rawBody, signature, uberProperties.getWebhookSecret())) {
            log.warn("Rejected Uber webhook — invalid signature");
            // Still return 200 to avoid leaking information to potential attackers
            return ResponseEntity.ok().build();
        }

        try {
            UberWebhookEvent event = objectMapper.readValue(rawBody, UberWebhookEvent.class);
            uberWebhookService.handleEvent(event);
        } catch (Exception e) {
            // Log but never return 5xx — Uber would retry and we'd process duplicates
            log.error("Failed to process Uber webhook: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }
}
