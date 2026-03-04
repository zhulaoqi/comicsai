package com.comicsai.ai.message;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified message type inspired by AgentScope's Msg.
 * Carries text, image data, and metadata between Agents, Models, and Pipelines.
 */
public class Msg {

    public static final String ROLE_SYSTEM = "system";
    public static final String ROLE_USER = "user";
    public static final String ROLE_ASSISTANT = "assistant";

    private final String name;
    private final String role;
    private final String content;
    private final byte[] imageData;
    private final String imageFormat;
    private final Map<String, Object> metadata;

    private Msg(Builder builder) {
        this.name = builder.name;
        this.role = builder.role;
        this.content = builder.content;
        this.imageData = builder.imageData;
        this.imageFormat = builder.imageFormat;
        this.metadata = builder.metadata;
    }

    public String getName() { return name; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public byte[] getImageData() { return imageData; }
    public String getImageFormat() { return imageFormat; }
    public Map<String, Object> getMetadata() { return metadata; }

    public boolean hasImage() {
        return imageData != null && imageData.length > 0;
    }

    @SuppressWarnings("unchecked")
    public <T> T getMeta(String key) {
        return metadata == null ? null : (T) metadata.get(key);
    }

    public Integer getInputTokens() {
        return getMeta("inputTokens");
    }

    public Integer getOutputTokens() {
        return getMeta("outputTokens");
    }

    public String getModel() {
        return getMeta("model");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private String role = ROLE_USER;
        private String content;
        private byte[] imageData;
        private String imageFormat;
        private Map<String, Object> metadata;

        public Builder name(String name) { this.name = name; return this; }
        public Builder role(String role) { this.role = role; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder imageData(byte[] imageData) { this.imageData = imageData; return this; }
        public Builder imageFormat(String imageFormat) { this.imageFormat = imageFormat; return this; }

        public Builder meta(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Msg build() {
            return new Msg(this);
        }
    }

    @Override
    public String toString() {
        return "Msg{name='" + name + "', role='" + role + "', content='"
                + (content != null && content.length() > 50 ? content.substring(0, 50) + "..." : content)
                + "', hasImage=" + hasImage() + "}";
    }
}
