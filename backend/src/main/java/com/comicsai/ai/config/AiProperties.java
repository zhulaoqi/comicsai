package com.comicsai.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe AI configuration properties bound from application.yml {@code ai.*}.
 */
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private ProviderConfig qwen = new ProviderConfig();
    private ProviderConfig gemini = new ProviderConfig();

    public ProviderConfig getQwen() { return qwen; }
    public void setQwen(ProviderConfig qwen) { this.qwen = qwen; }

    public ProviderConfig getGemini() { return gemini; }
    public void setGemini(ProviderConfig gemini) { this.gemini = gemini; }

    public static class ProviderConfig {
        private String apiKey = "";
        private String baseUrl = "";

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
