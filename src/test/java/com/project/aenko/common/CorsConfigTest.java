package com.project.aenko.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.aenko.common.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;

class CorsConfigTest {

    private CorsConfiguration singleConfig(String origins) {
        return ((org.springframework.web.cors.UrlBasedCorsConfigurationSource) new AppConfig()
                        .corsConfigurationSource(origins))
                .getCorsConfigurations().values().iterator().next();
    }

    @Test
    void multipleOriginsAreConfigured() {
        CorsConfiguration config = singleConfig("https://aenko.vercel.app, http://localhost:5173");

        assertThat(config.getAllowedOrigins())
                .containsExactly("https://aenko.vercel.app", "http://localhost:5173");
    }

    @Test
    void blankEntriesAreIgnored() {
        CorsConfiguration config = singleConfig("https://aenko.vercel.app,,  ");

        assertThat(config.getAllowedOrigins()).containsExactly("https://aenko.vercel.app");
    }

    @Test
    void defaultOriginRemainsLocalDevelopment() {
        CorsConfiguration config = singleConfig("http://localhost:5173");

        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:5173");
    }
}
