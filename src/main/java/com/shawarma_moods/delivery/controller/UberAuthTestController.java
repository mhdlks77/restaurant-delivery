package com.shawarma_moods.delivery.controller;

import com.shawarma_moods.delivery.service.UberAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class UberAuthTestController {

    private final UberAuthService uberAuthService;

    public UberAuthTestController(UberAuthService uberAuthService){
        this.uberAuthService = uberAuthService;
    }

    // !!THIS IS A TEMPORARY TEST ENDPOINT - REMOVE BEFORE PRODUCTION!!

    @GetMapping("/uber-token")
    ResponseEntity<Map<String, String>> testToken(){
        String token = uberAuthService.getAccessToken();
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "tokenLength", String.valueOf(token.length()),
                "tokenPrefix", token.substring(0, 10) + "..."
        ));
    }
}
