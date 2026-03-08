package com.mud.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Value("${claude.api.key}")
    private String claudeApiKey;

    @Value("${claude.api.base-url}")
    private String claudeBaseUrl;

    @Bean
    public WebClient claudeWebClient() {
        return WebClient.builder()
            .baseUrl(claudeBaseUrl)
            .defaultHeader("x-api-key", claudeApiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .defaultHeader("content-type", "application/json")
            .codecs(config -> config.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
            .build();
    }
}
