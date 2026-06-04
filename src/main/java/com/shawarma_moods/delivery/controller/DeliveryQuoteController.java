package com.shawarma_moods.delivery.controller;

import com.shawarma_moods.delivery.service.UberDeliveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/delivery")
public class DeliveryQuoteController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryQuoteController.class);

    private final UberDeliveryService uberDeliveryService;

    public DeliveryQuoteController(UberDeliveryService uberDeliveryService) {
        this.uberDeliveryService = uberDeliveryService;
    }

    /**
     * Validates a delivery address against Uber Direct before the customer pays.
     * Called by the checkout page when the customer finishes entering their address.
     *
     * Returns: { deliverable: true,  feeCents: 599  }
     *       or { deliverable: false, reason: "..."  }
     */
    @GetMapping("/quote-check")
    public ResponseEntity<Map<String, Object>> checkDeliveryAddress(
            @RequestParam String address
    ) {
        try {
            var quote = uberDeliveryService.getQuote(address);
            int feeCents = quote.getFee() != null ? quote.getFee() : 0;
            log.info("Quote check for '{}': deliverable, fee={} cents", address, feeCents);
            return ResponseEntity.ok(Map.of(
                    "deliverable", true,
                    "feeCents",    feeCents
            ));
        } catch (Exception e) {
            log.warn("Quote check failed for '{}': {}", address, e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "deliverable", false,
                    "reason", "We can't deliver to this address. Please check it or choose pickup."
            ));
        }
    }
}
