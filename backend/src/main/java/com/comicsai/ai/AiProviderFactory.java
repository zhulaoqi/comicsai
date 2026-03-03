package com.comicsai.ai;

import com.comicsai.ai.gemini.GeminiTextProvider;
import com.comicsai.ai.openai.DallEImageProvider;
import com.comicsai.ai.qwen.QwenTextProvider;
import com.comicsai.ai.qwen.WanxiangImageProvider;
import com.comicsai.common.exception.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory that creates AI provider instances based on configuration.
 * Supports fallback: if the primary provider fails, it tries the next available provider.
 *
 * Provider names used in GenerationConfig map to concrete implementations:
 *   Text:  "openai" → OpenAiTextProvider,  "qwen" → QwenTextProvider
 *   Image: "dall-e" → DallEImageProvider,   "wanxiang" → WanxiangImageProvider
 */
@Component
public class AiProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(AiProviderFactory.class);

    private final Map<String, TextAiProvider> textProviders = new LinkedHashMap<>();
    private final Map<String, ImageAiProvider> imageProviders = new LinkedHashMap<>();

    @Autowired
    public AiProviderFactory(
            @Value("${ai.gemini.api-key:}") String geminiApiKey,
            @Value("${ai.qwen.api-key:}") String qwenApiKey,
            @Value("${ai.qwen.base-url:https://dashscope.aliyuncs.com}") String qwenBaseUrl) {

        // Register text providers (insertion order = fallback order)
        textProviders.put("gemini", new GeminiTextProvider(geminiApiKey));
        textProviders.put("qwen", new QwenTextProvider(qwenApiKey, qwenBaseUrl));

        // Register image providers
        imageProviders.put("wanxiang", new WanxiangImageProvider(qwenApiKey, qwenBaseUrl));

        log.info("AI Provider Factory initialized with text providers: {}, image providers: {}",
                textProviders.keySet(), imageProviders.keySet());
    }

    /**
     * Package-private constructor for testing — allows injecting custom provider maps.
     */
    AiProviderFactory(Map<String, TextAiProvider> textProviders, Map<String, ImageAiProvider> imageProviders) {
        this.textProviders.putAll(textProviders);
        this.imageProviders.putAll(imageProviders);
    }

    /**
     * Get a text provider by name.
     *
     * @param providerName the provider identifier (e.g. "openai", "qwen")
     * @return the matching TextAiProvider
     * @throws AiProviderException if no provider is registered with that name
     */
    public TextAiProvider getTextProvider(String providerName) {
        TextAiProvider provider = textProviders.get(providerName);
        if (provider == null) {
            throw new AiProviderException(providerName, "Unknown text AI provider: " + providerName);
        }
        return provider;
    }

    /**
     * Get an image provider by name.
     *
     * @param providerName the provider identifier (e.g. "dall-e", "wanxiang")
     * @return the matching ImageAiProvider
     * @throws AiProviderException if no provider is registered with that name
     */
    public ImageAiProvider getImageProvider(String providerName) {
        ImageAiProvider provider = imageProviders.get(providerName);
        if (provider == null) {
            throw new AiProviderException(providerName, "Unknown image AI provider: " + providerName);
        }
        return provider;
    }

    /**
     * Generate text with automatic fallback.
     * Tries the primary provider first; on failure, iterates through all other
     * registered text providers until one succeeds.
     *
     * @param primaryProviderName the preferred provider name
     * @param request             the generation request
     * @return the generation result from whichever provider succeeded
     * @throws AiProviderException if ALL providers fail
     */
    public TextGenerationResult generateTextWithFallback(String primaryProviderName, TextGenerationRequest request) {
        AiProviderException lastException = null;

        // Try primary provider first
        TextAiProvider primary = textProviders.get(primaryProviderName);
        if (primary != null) {
            try {
                return primary.generateText(request);
            } catch (AiProviderException e) {
                log.warn("Primary text provider '{}' failed: {}", primaryProviderName, e.getMessage());
                lastException = e;
            }
        } else {
            log.warn("Primary text provider '{}' not found, trying fallbacks", primaryProviderName);
        }

        // Try fallback providers (all others in registration order)
        for (Map.Entry<String, TextAiProvider> entry : textProviders.entrySet()) {
            if (entry.getKey().equals(primaryProviderName)) {
                continue;
            }
            try {
                log.info("Falling back to text provider '{}'", entry.getKey());
                return entry.getValue().generateText(request);
            } catch (AiProviderException e) {
                log.warn("Fallback text provider '{}' also failed: {}", entry.getKey(), e.getMessage());
                lastException = e;
            }
        }

        throw new AiProviderException(
                primaryProviderName,
                "All text AI providers failed. Last error: " + (lastException != null ? lastException.getMessage() : "unknown"),
                lastException);
    }

    /**
     * Generate an image with automatic fallback.
     * Tries the primary provider first; on failure, iterates through all other
     * registered image providers until one succeeds.
     *
     * @param primaryProviderName the preferred provider name
     * @param request             the generation request
     * @return the generation result from whichever provider succeeded
     * @throws AiProviderException if ALL providers fail
     */
    public ImageGenerationResult generateImageWithFallback(String primaryProviderName, ImageGenerationRequest request) {
        AiProviderException lastException = null;

        // Try primary provider first
        ImageAiProvider primary = imageProviders.get(primaryProviderName);
        if (primary != null) {
            try {
                return primary.generateImage(request);
            } catch (AiProviderException e) {
                log.warn("Primary image provider '{}' failed: {}", primaryProviderName, e.getMessage());
                lastException = e;
            }
        } else {
            log.warn("Primary image provider '{}' not found, trying fallbacks", primaryProviderName);
        }

        // Try fallback providers
        for (Map.Entry<String, ImageAiProvider> entry : imageProviders.entrySet()) {
            if (entry.getKey().equals(primaryProviderName)) {
                continue;
            }
            try {
                log.info("Falling back to image provider '{}'", entry.getKey());
                return entry.getValue().generateImage(request);
            } catch (AiProviderException e) {
                log.warn("Fallback image provider '{}' also failed: {}", entry.getKey(), e.getMessage());
                lastException = e;
            }
        }

        throw new AiProviderException(
                primaryProviderName,
                "All image AI providers failed. Last error: " + (lastException != null ? lastException.getMessage() : "unknown"),
                lastException);
    }

}
