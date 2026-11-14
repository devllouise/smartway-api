package com.smartway.api.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient weatherWebClient(WebClient.Builder builder) {
        return builder.baseUrl("https://api.openweathermap.org/data/2.5").build();
    }

    @Bean
    public WebClient openAiWebClient(WebClient.Builder builder) {
        return builder.baseUrl("https://api.openai.com/v1").build();
    }
}
