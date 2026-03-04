package com.comicsai.ai.model;

/**
 * Immutable configuration passed to ChatModel / ImageModel calls.
 * Centralises the parameters previously scattered across Request DTOs.
 */
public class ModelConfig {

    private final String modelName;
    private final Double temperature;
    private final Integer maxTokens;
    private final String imageSize;
    private final String imageStyle;

    private ModelConfig(Builder builder) {
        this.modelName = builder.modelName;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.imageSize = builder.imageSize;
        this.imageStyle = builder.imageStyle;
    }

    public String getModelName() { return modelName; }
    public Double getTemperature() { return temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public String getImageSize() { return imageSize; }
    public String getImageStyle() { return imageStyle; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String modelName;
        private Double temperature;
        private Integer maxTokens;
        private String imageSize;
        private String imageStyle;

        public Builder modelName(String modelName) { this.modelName = modelName; return this; }
        public Builder temperature(Double temperature) { this.temperature = temperature; return this; }
        public Builder maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder imageSize(String imageSize) { this.imageSize = imageSize; return this; }
        public Builder imageStyle(String imageStyle) { this.imageStyle = imageStyle; return this; }

        public ModelConfig build() { return new ModelConfig(this); }
    }
}
