package com.zombiedetector.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Exposes CORS as a CorsConfigurationSource bean (rather than only via WebMvcConfigurer)
 * so Spring Security's filter chain -- which runs in front of the DispatcherServlet and
 * would otherwise reject preflight OPTIONS requests before MVC ever sees them -- can use
 * the same rules. Previously this only allowed localhost:5173; *.vercel.app was documented
 * as configured but wasn't actually present here, and Authorization wasn't in the allowed
 * headers list, which would have silently broken the JWT-based calls once auth landed.
 */
@Configuration
public class CorsConfig {

    // Optional comma-separated extras (e.g. a custom domain): CORS_EXTRA_ORIGINS=https://app.example.com
    @Value("${app.cors.extra-origins:}")
    private String extraOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:5173",
                "https://*.vercel.app"
        ));
        for (String origin : extraOrigins.split(",")) {
            if (!origin.isBlank()) origins.add(origin.trim());
        }

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(false); // JWT is a bearer header, not a cookie -- no credentials needed

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}