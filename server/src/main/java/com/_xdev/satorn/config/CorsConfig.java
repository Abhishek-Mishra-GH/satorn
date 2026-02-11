package com._xdev.satorn.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig {

    @Value("${security.allowed-origins:${spring.security.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:5173}}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toList());

        // Use origin patterns so "*" works in dev (especially with credentials-based requests).
        configuration.setAllowedOriginPatterns(origins.isEmpty() ? List.of("*") : origins);

        // Allow all common methods and headers for frontend API calls.
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));

        // Expose headers to client
        configuration.setExposedHeaders(List.of("Authorization"));

        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);

        // Max age for preflight requests (1 hour)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
