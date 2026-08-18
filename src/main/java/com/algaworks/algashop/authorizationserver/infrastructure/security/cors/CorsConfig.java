package com.algaworks.algashop.authorizationserver.infrastructure.security.cors;

import com.algaworks.algashop.authorizationserver.infrastructure.security.AlgaShopSecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(AlgaShopSecurityProperties properties) {
        CorsConfiguration cors = new CorsConfiguration();

        cors.setAllowedOrigins(properties.getCors().getAllowedOrigins());
        cors.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        cors.setAllowCredentials(true); // envia o iframe do client publico

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
