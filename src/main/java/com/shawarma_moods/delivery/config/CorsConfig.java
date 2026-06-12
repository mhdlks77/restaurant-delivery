package com.shawarma_moods.delivery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Public customer-facing API — called directly from the browser.
        // /api/menu        → menu fetch
        // /api/orders/**   → order creation + status polling
        // /api/delivery/** → address quote-check
        //
        // /api/dashboard/** is intentionally excluded — it is only ever called
        // server-to-server (Vercel proxy → Railway) and must never be reachable
        // from a browser, even with the correct token.
        registry.addMapping("/api/menu")
                .allowedOriginPatterns(
                        "https://order.shawarma-moods.com",
                        "https://*.vercel.app",
                        "http://localhost:5173"
                )
                .allowedMethods("GET")
                .allowedHeaders("*");

        registry.addMapping("/api/orders/**")
                .allowedOriginPatterns(
                        "https://order.shawarma-moods.com",
                        "https://*.vercel.app",
                        "http://localhost:5173"
                )
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");

        registry.addMapping("/api/delivery/**")
                .allowedOriginPatterns(
                        "https://order.shawarma-moods.com",
                        "https://*.vercel.app",
                        "http://localhost:5173"
                )
                .allowedMethods("GET")
                .allowedHeaders("*");
    }
}
