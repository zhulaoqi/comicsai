package com.comicsai.ai;

/**
 * Unified interface for image generation AI providers.
 * Implementations adapt specific AI services (DALL-E, Wanxiang, etc.)
 * to a common contract used by ContentGeneratorService.
 */
public interface ImageAiProvider {

    /**
     * Generate an image based on the given request.
     *
     * @param request the image generation parameters
     * @return the generation result including image data and token usage
     */
    ImageGenerationResult generateImage(ImageGenerationRequest request);

    /**
     * @return the unique provider name identifier (e.g. "dall-e", "wanxiang")
     */
    String getProviderName();
}
