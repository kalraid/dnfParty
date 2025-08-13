package com.dfparty.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class DfApiConfig {

    @Value("${df.api.base-url}")
    private String baseUrl;

    @Value("${df.api.key}")
    private String apiKey;

    @Value("${df.api.rate-limit}")
    private int rateLimit;

    @Value("${df.api.rate-limit-window}")
    private long rateLimitWindow;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    public long getRateLimitWindow() {
        return rateLimitWindow;
    }
} 