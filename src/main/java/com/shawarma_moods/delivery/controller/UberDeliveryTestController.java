package com.shawarma_moods.delivery.controller;

import com.shawarma_moods.delivery.dto.UberDeliveryResponse;
import com.shawarma_moods.delivery.dto.UberQuoteResponse;
import com.shawarma_moods.delivery.service.UberDeliveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class UberDeliveryTestController {

    private final UberDeliveryService uberDeliveryService;

    public UberDeliveryTestController(UberDeliveryService uberDeliveryService){
        this.uberDeliveryService = uberDeliveryService;
    }

    // Test quote only !!REMOVE BEFORE PRODUCTION!!
    @GetMapping("/quote")
    public ResponseEntity<UberQuoteResponse> testQuote(
            @RequestParam String dropoffAddress
    ) {
        UberQuoteResponse quote = uberDeliveryService.getQuote(dropoffAddress);
        return ResponseEntity.ok(quote);
    }

    // Test full delivery creation
    @PostMapping("/delivery")
    public ResponseEntity<Map<String, String>> testDelivery(@RequestBody Map<String, String> body) {
        UberDeliveryResponse response = uberDeliveryService.createDelivery(
                body.get("woocommerceOrderId"),
                body.get("customerName"),
                body.get("customerAddress"),
                body.get("customerPhone"),
                Integer.parseInt(body.get("orderValueCents")),
                "Test food order"
        );
        return ResponseEntity.ok(Map.of(
                "uberDeliveryId", response.getId(),
                "status", response.getStatus(),
                "trackingUrl", response.getTrackingUrl() != null ? response.getTrackingUrl() : "n/a"
        ));
    }
}
