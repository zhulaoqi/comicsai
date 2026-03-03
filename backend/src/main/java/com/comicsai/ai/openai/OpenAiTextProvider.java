package com.comicsai.ai.openai;

import com.comicsai.ai.TextAiProvider;
import com.comicsai.ai.TextGenerationRequest;
import com.comicsai.ai.TextGenerationResult;
import com.comicsai.common.exception.AiProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenAI GPT text generation adapter.
 * Adapts the OpenAI Chat Completions API to the TextAiProvider interface.
 */
public class OpenAiTextProvider implements TextAiProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiTextProvider.class);
    private static final String PROVIDER_NAME = "openai";

    private final String apiKey;
    private final String baseUrl;

    public OpenAiTextProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    @Override
    public TextGenerationResult generateText(TextGenerationRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new AiProviderException(PROVIDER_NAME, "Prompt must not be null or blank");
        }

        String model = request.getModel() != null ? request.getModel() : "gpt-4";

        log.info("Calling OpenAI text generation: model={}, maxTokens={}", model, request.getMaxTokens());

        try {
            // Placeholder: In production, this would make an HTTP call to the OpenAI API.
            // The actual HTTP client integration is deferred until API keys are available.
            throw new AiProviderException(PROVIDER_NAME,
                    "OpenAI API call not implemented - configure a real HTTP client for production use");
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to call OpenAI API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
