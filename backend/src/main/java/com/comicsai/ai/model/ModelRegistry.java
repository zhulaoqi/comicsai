package com.comicsai.ai.model;

import com.comicsai.ai.message.Msg;
import com.comicsai.common.exception.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central registry for ChatModel and ImageModel instances.
 * Provides lookup by name and fallback execution (try primary, then alternatives).
 * Replaces the old AiProviderFactory.
 */
@Component
public class ModelRegistry {

    private static final Logger log = LoggerFactory.getLogger(ModelRegistry.class);

    private final Map<String, ChatModel> chatModels = new LinkedHashMap<>();
    private final Map<String, ImageModel> imageModels = new LinkedHashMap<>();

    /**
     * Spring auto-injects all ChatModel and ImageModel beans.
     */
    @Autowired
    public ModelRegistry(List<ChatModel> chatModelList, List<ImageModel> imageModelList) {
        chatModelList.forEach(m -> chatModels.put(m.getName(), m));
        imageModelList.forEach(m -> imageModels.put(m.getName(), m));
        log.info("ModelRegistry initialized — chat: {}, image: {}", chatModels.keySet(), imageModels.keySet());
    }

    /** Package-private constructor for unit testing. */
    ModelRegistry(Map<String, ChatModel> chatModels, Map<String, ImageModel> imageModels) {
        this.chatModels.putAll(chatModels);
        this.imageModels.putAll(imageModels);
    }

    public ChatModel getChatModel(String name) {
        ChatModel model = chatModels.get(name);
        if (model == null) {
            throw new AiProviderException(name, "Unknown chat model: " + name);
        }
        return model;
    }

    public ImageModel getImageModel(String name) {
        ImageModel model = imageModels.get(name);
        if (model == null) {
            throw new AiProviderException(name, "Unknown image model: " + name);
        }
        return model;
    }

    /**
     * Try primary chat model; on failure fall back to every other registered model.
     */
    public Msg chatWithFallback(String primaryName, List<Msg> messages, ModelConfig config) {
        AiProviderException lastException = null;

        ChatModel primary = chatModels.get(primaryName);
        if (primary != null) {
            try {
                return primary.chat(messages, config);
            } catch (AiProviderException e) {
                log.warn("Primary chat model '{}' failed: {}", primaryName, e.getMessage());
                lastException = e;
            }
        } else {
            log.warn("Primary chat model '{}' not found, trying fallbacks", primaryName);
        }

        for (Map.Entry<String, ChatModel> entry : chatModels.entrySet()) {
            if (entry.getKey().equals(primaryName)) continue;
            try {
                log.info("Falling back to chat model '{}'", entry.getKey());
                return entry.getValue().chat(messages, config);
            } catch (AiProviderException e) {
                log.warn("Fallback chat model '{}' also failed: {}", entry.getKey(), e.getMessage());
                lastException = e;
            }
        }

        throw new AiProviderException(primaryName,
                "All chat models failed. Last error: " + (lastException != null ? lastException.getMessage() : "unknown"),
                lastException);
    }

    /**
     * Try primary image model; on failure fall back to every other registered model.
     */
    public Msg imageWithFallback(String primaryName, String prompt, ModelConfig config) {
        AiProviderException lastException = null;

        ImageModel primary = imageModels.get(primaryName);
        if (primary != null) {
            try {
                return primary.generateImage(prompt, config);
            } catch (AiProviderException e) {
                log.warn("Primary image model '{}' failed: {}", primaryName, e.getMessage());
                lastException = e;
            }
        } else {
            log.warn("Primary image model '{}' not found, trying fallbacks", primaryName);
        }

        for (Map.Entry<String, ImageModel> entry : imageModels.entrySet()) {
            if (entry.getKey().equals(primaryName)) continue;
            try {
                log.info("Falling back to image model '{}'", entry.getKey());
                return entry.getValue().generateImage(prompt, config);
            } catch (AiProviderException e) {
                log.warn("Fallback image model '{}' also failed: {}", entry.getKey(), e.getMessage());
                lastException = e;
            }
        }

        throw new AiProviderException(primaryName,
                "All image models failed. Last error: " + (lastException != null ? lastException.getMessage() : "unknown"),
                lastException);
    }
}
