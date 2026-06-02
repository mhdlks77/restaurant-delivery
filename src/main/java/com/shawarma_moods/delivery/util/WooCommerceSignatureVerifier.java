package com.shawarma_moods.delivery.util;

import com.shawarma_moods.delivery.config.WooCommerceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

@Component
public class WooCommerceSignatureVerifier {

    private static final Logger log = LoggerFactory.getLogger(WooCommerceProperties.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Verifies the woocommerce webhook signature
     * Woocommerce sends X-WC-Webhook-Signature as a base64-encoded
     * HMAC-SHA256 hash of the raw request body
     * @param rawBody unmodified request body bytes
     * @param signature value of -WC-Webhook header
     * @param secret your WooCommerce webhook secret
     * @return true if signature is valid
     */

    public boolean verify(byte[] rawBody, String signature, String secret) {
        if (signature == null || secret == null) {
            log.warn("Missing WooCommerce signature or secret - rejecting webhook");
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
            String expectedSignature = Base64.getEncoder().encodeToString(expectedHash);

            boolean valid = MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );

            if (!valid) {
                log.warn("WooCommerce webhook signature mismatch — possible spoofed request");
            }

            return valid;

        } catch (Exception e) {
            log.error("Failed to verify WooCommerce webhook signature: {}", e.getMessage());
            return false;
        }
    }
}
