package com.comicsai.ai;

/**
 * Unified interface for text generation AI providers.
 * Implementations adapt specific AI services (OpenAI GPT, Qwen, etc.)
 * to a common contract used by ContentGeneratorService.
 */
public interface TextAiProvider {

    /**
     * Generate text content based on the given request.
     *
     * @param request the text generation parameters
     * @return the generation result including content and token usage
     */
    TextGenerationResult generateText(TextGenerationRequest request);

    /**
     * @return the unique provider name identifier (e.g. "openai", "qwen")
     */
    String getProviderName();
}
