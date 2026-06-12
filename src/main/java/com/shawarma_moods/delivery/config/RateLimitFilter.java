package com.shawarma_moods.delivery.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory, per-IP rate limiter for abuse-prone public endpoints.
 *
 * Protects:
 *   POST /api/orders/create        — stop order spam / WooCommerce flooding
 *   GET  /api/delivery/quote-check — stop Uber Direct API quota exhaustion
 *
 * Uses a token-bucket per (client-IP + endpoint). No external dependency and no
 * shared state across instances — appropriate for the current single-instance
 * Railway deployment. If the backend is ever horizontally scaled, move this to a
 * shared store (e.g. Redis) so limits are enforced cluster-wide.
 *
 * Client IP is taken from the left-most X-Forwarded-For entry (Railway sits
 * behind a proxy, so request.getRemoteAddr() would be the proxy for everyone).
 */
@Component
@Order(2) // runs after DashboardAuthFilter (@Order 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Per-endpoint limits: capacity (burst) and refill window in milliseconds. */
    private static final Map<String, Limit> LIMITS = Map.of(
            "/api/orders/create",        new Limit(5, 60_000),   // 5 orders / minute / IP
            "/api/delivery/quote-check", new Limit(20, 60_000)   // 20 quotes / minute / IP
    );

    private static final int MAX_TRACKED_BUCKETS = 50_000; // memory safety cap

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        Limit limit = LIMITS.get(request.getRequestURI());

        // Not a rate-limited endpoint — pass straight through
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String key = clientIp + "|" + request.getRequestURI();

        // Opportunistic cleanup if the map grows too large (many distinct IPs)
        if (buckets.size() > MAX_TRACKED_BUCKETS) {
            long cutoff = System.currentTimeMillis() - limit.windowMs() * 2L;
            buckets.entrySet().removeIf(e -> e.getValue().lastAccess() < cutoff);
        }

        TokenBucket bucket = buckets.computeIfAbsent(key,
                k -> new TokenBucket(limit.capacity(), limit.windowMs()));

        if (bucket.tryConsume()) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for {} on {}", clientIp, request.getRequestURI());
            response.setStatus(429); // 429 Too Many Requests
            response.setHeader("Retry-After", String.valueOf(limit.windowMs() / 1000));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many requests. Please slow down and try again shortly.\"}");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    /** Endpoint limit config. */
    private record Limit(int capacity, long windowMs) {}

    /**
     * Thread-safe token bucket. {@code capacity} tokens refill linearly over
     * {@code windowMs}; each request consumes one token.
     */
    private static final class TokenBucket {
        private final int capacity;
        private final double refillPerMs;
        private double tokens;
        private long lastRefillMs;
        private long lastAccessMs;

        TokenBucket(int capacity, long windowMs) {
            this.capacity = capacity;
            this.refillPerMs = (double) capacity / windowMs;
            this.tokens = capacity;
            this.lastRefillMs = System.currentTimeMillis();
            this.lastAccessMs = this.lastRefillMs;
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            lastAccessMs = now;

            long elapsed = now - lastRefillMs;
            if (elapsed > 0) {
                tokens = Math.min(capacity, tokens + elapsed * refillPerMs);
                lastRefillMs = now;
            }

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        long lastAccess() {
            return lastAccessMs;
        }
    }
}
