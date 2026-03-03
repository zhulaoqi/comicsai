package com.comicsai.ai.qwen;

import com.comicsai.ai.ImageAiProvider;
import com.comicsai.ai.ImageGenerationRequest;
import com.comicsai.ai.ImageGenerationResult;
import com.comicsai.common.exception.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alibaba Wanxiang (通义万相) image generation adapter.
 * Adapts the DashScope Image API to the ImageAiProvider interface.
 */
public class WanxiangImageProvider implements ImageAiProvider {

    private static final Logger log = LoggerFactory.getLogger(WanxiangImageProvider.class);
    private static final String PROVIDER_NAME = "wanxiang";

    private final String apiKey;
    private final String baseUrl;

    public WanxiangImageProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public ImageGenerationResult generateImage(ImageGenerationRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new AiProviderException(PROVIDER_NAME, "Prompt must not be null or blank");
        }

        String model = request.getModel() != null ? request.getModel() : "wanx-v1";
        String size = request.getSize() != null ? request.getSize() : "1024x1024";

        log.info("Calling Wanxiang image generation: model={}, size={}", model, size);

        try {
            // Placeholder: In production, this would make an HTTP call to the DashScope Image API.
            throw new AiProviderException(PROVIDER_NAME,
                    "Wanxiang API call not implemented - configure a real HTTP client for production use");
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to call Wanxiang API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
