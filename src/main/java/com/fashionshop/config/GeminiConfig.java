package com.fashionshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${app.gemini.api-key}")
    private String apiKey;

    @Value("${app.gemini.model:gemini-1.5-flash}")
    private String model;

    public String getApiKey() { return apiKey; }
    public String getModel() { return model; }

    public String buildApiUrl() {
        return "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;
    }
}
