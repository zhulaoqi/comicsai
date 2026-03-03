package com.comicsai.ai.qwen;

import com.comicsai.ai.TextAiProvider;
import com.comicsai.ai.TextGenerationRequest;
import com.comicsai.ai.TextGenerationResult;
import com.comicsai.common.exception.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alibaba Qwen (通义千问) text generation adapter.
 * Adapts the DashScope API to the TextAiProvider interface.
 */
public class QwenTextProvider implements TextAiProvider {

    private static final Logger log = LoggerFactory.getLogger(QwenTextProvider.class);
    private static final String PROVIDER_NAME = "qwen";

    private final String apiKey;
    private final String baseUrl;

    public QwenTextProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public TextGenerationResult generateText(TextGenerationRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new AiProviderException(PROVIDER_NAME, "Prompt must not be null or blank");
        }

        String model = request.getModel() != null ? request.getModel() : "qwen-turbo";

        log.info("Calling Qwen text generation: model={}, maxTokens={}", model, request.getMaxTokens());

        try {
            // Placeholder: In production, this would make an HTTP call to the DashScope API.
            throw new AiProviderException(PROVIDER_NAME,
                    "Qwen API call not implemented - configure a real HTTP client for production use");
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to call Qwen API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
