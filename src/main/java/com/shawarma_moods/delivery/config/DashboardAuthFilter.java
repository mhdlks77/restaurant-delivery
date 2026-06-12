package com.shawarma_moods.delivery.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protects all /api/dashboard/** endpoints with a shared secret token.
 *
 * Every request to /api/dashboard/** must include:
 *   X-Dashboard-Token: <value of DASHBOARD_API_TOKEN env var>
 *
 * This token is appended server-side by the Vercel proxy function
 * (api/dashboard/[...path].js) after verifying the staff auth cookie.
 * Direct calls to the Railway backend without this header are rejected.
 *
 * Uses constant-time comparison (MessageDigest.isEqual) to prevent
 * timing-based token enumeration attacks.
 */
@Component
@Order(1)
public class DashboardAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DashboardAuthFilter.class);
    private static final String TOKEN_HEADER = "X-Dashboard-Token";
    private static final String DASHBOARD_PATH_PREFIX = "/api/dashboard";

    @Value("${dashboard.api.token:}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith(DASHBOARD_PATH_PREFIX)) {
            // Not a dashboard route — pass through unchanged
            chain.doFilter(request, response);
            return;
        }

        // Fail closed: if the token is not configured, the endpoint is unreachable
        if (expectedToken == null || expectedToken.isBlank()) {
            log.error("DASHBOARD_API_TOKEN is not configured — blocking all dashboard requests");
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"error\":\"Dashboard token not configured\"}");
            return;
        }

        String providedToken = request.getHeader(TOKEN_HEADER);

        if (providedToken == null) {
            log.warn("Dashboard request to {} rejected — missing {} header", path, TOKEN_HEADER);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing dashboard token\"}");
            return;
        }

        // Constant-time comparison — prevents timing attacks
        boolean valid = MessageDigest.isEqual(
                providedToken.getBytes(StandardCharsets.UTF_8),
                expectedToken.getBytes(StandardCharsets.UTF_8)
        );

        if (!valid) {
            log.warn("Dashboard request to {} rejected — invalid token", path);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid dashboard token\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
