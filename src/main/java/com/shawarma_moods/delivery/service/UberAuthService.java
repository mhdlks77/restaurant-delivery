package com.shawarma_moods.delivery.service;

import com.shawarma_moods.delivery.config.UberProperties;
import com.shawarma_moods.delivery.dto.UberTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class UberAuthService {

    private static final Logger log = LoggerFactory.getLogger(UberAuthService.class);
    private static final String TOKEN_ENDPOINT = "https://auth.uber.com/oauth/v2/token";
    // Refresh token 60 seconds before actual expire to avoid edge cases
    private static final long EXPIRY_BUFFER_SECONDS = 60;

    private final UberProperties uberProperties;
    private final RestTemplate restTemplate;

    private String cachedToken;
    private Instant tokenExpiresAt;

    public UberAuthService(UberProperties uberProperties, RestTemplate restTemplate){
        this.uberProperties = uberProperties;
        this.restTemplate = restTemplate;
    }

    // Return a valid Uber access token
    // Fetch a new one if none exists or current one is near expiry

    public String getAccessToken() {
        if (isTokenValid()) {
            log.debug("Returning cached Uber access token");
            return cachedToken;
        }
        log.info("Fetching new Uber access token");
        return fetchNewToken();
    }

    private boolean isTokenValid() {
        return cachedToken != null
                && tokenExpiresAt != null
                && Instant.now().isBefore(tokenExpiresAt.minusSeconds(EXPIRY_BUFFER_SECONDS));
    }

    private String fetchNewToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", uberProperties.getClientId());
        body.add("client_secret", uberProperties.getClientSecret());
        body.add("grant_type", "client_credentials");
        body.add("scope", "eats.deliveries");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<UberTokenResponse> response = restTemplate.postForEntity(
                    TOKEN_ENDPOINT,
                    request,
                    UberTokenResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                UberTokenResponse tokenResponse = response.getBody();
                cachedToken = tokenResponse.getAccessToken();
                tokenExpiresAt = Instant.now().plusSeconds(tokenResponse.getExpiresIn());
                log.info("Uber access token fetched successfully, expires at {}", tokenExpiresAt);
                return cachedToken;
            } else {
                throw new RuntimeException("Unexpected response from Uber auth: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to fetch Uber access token: {}", e.getMessage());
            throw new RuntimeException("Could not authenticate with Uber Direct", e);
        }
    }
}
