package com.shawarma_moods.delivery.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class WebhookSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureVerifier.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Verifies the Uber webhook signature.
     * Uber sends an X-Postmates-Signature header containing an HMAC-SHA256
     * hash of the raw request body, signed with your webhook secret.
     *
     * @param rawBody   raw request body bytes (must be unmodified)
     * @param signature value of the X-Postmates-Signature header
     * @param secret    your configured webhook secret
     * @return true if signature is valid
     */
    public boolean verify(byte[] rawBody, String signature, String secret) {

        if (signature == null || secret == null) {
            log.warn("Missing signature or secret — rejecting webhook");
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(keySpec);
            byte[] expectedHash = mac.doFinal(rawBody);
            String expectedSignature = HexFormat.of().formatHex(expectedHash);

            // Constant-time comparison to prevent timing attacks
            boolean valid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("Webhook signature mismatch — possible spoofed request");
            }

            return valid;

        } catch (Exception e) {
            log.error("Failed to verify webhook signature: {}", e.getMessage());
            return false;
        }
    }

;}
