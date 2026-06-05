package com.shawarma_moods.delivery.controller;

import tools.jackson.databind.JsonNode;
import com.shawarma_moods.delivery.service.WooCommerceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/menu")
public class MenuController {

    private static final Logger log = LoggerFactory.getLogger(MenuController.class);

    private final WooCommerceService wooCommerceService;

    public MenuController(WooCommerceService wooCommerceService) {
        this.wooCommerceService = wooCommerceService;
    }

    /**
     * Returns the menu categorised by WooCommerce product category,
     * filtered by restaurant location ID.
     *
     * GET /api/menu?locationId=98
     */
    @GetMapping
    public ResponseEntity<JsonNode> getMenu(
            @RequestParam(required = false) Integer locationId
    ) {
        log.info("Menu requested for locationId={}", locationId);
        JsonNode menu = wooCommerceService.getMenu(locationId);
        return ResponseEntity.ok(menu);
    }
}
