package com.comicsai.ai.openai;

import com.comicsai.ai.ImageAiProvider;
import com.comicsai.ai.ImageGenerationRequest;
import com.comicsai.ai.ImageGenerationResult;
import com.comicsai.common.exception.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DALL-E image generation adapter.
 * Adapts the OpenAI Images API to the ImageAiProvider interface.
 */
public class DallEImageProvider implements ImageAiProvider {

    private static final Logger log = LoggerFactory.getLogger(DallEImageProvider.class);
    private static final String PROVIDER_NAME = "dall-e";

    private final String apiKey;
    private final String baseUrl;

    public DallEImageProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public ImageGenerationResult generateImage(ImageGenerationRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new AiProviderException(PROVIDER_NAME, "Prompt must not be null or blank");
        }

        String model = request.getModel() != null ? request.getModel() : "dall-e-3";
        String size = request.getSize() != null ? request.getSize() : "1024x1024";

        log.info("Calling DALL-E image generation: model={}, size={}", model, size);

        try {
            // Placeholder: In production, this would make an HTTP call to the OpenAI Images API.
            throw new AiProviderException(PROVIDER_NAME,
                    "DALL-E API call not implemented - configure a real HTTP client for production use");
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to call DALL-E API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
