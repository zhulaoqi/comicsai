package com.comicsai.ai.model;

import com.comicsai.ai.message.Msg;

/**
 * Image generation model service interface (AgentScope Model layer).
 * Wraps a specific image-generation API.
 */
public interface ImageModel {

    /**
     * Generate an image from a text prompt.
     *
     * @param prompt text description of the desired image
     * @param config model-specific parameters (size, style, model name, etc.)
     * @return Msg carrying imageData, imageFormat, and token-usage metadata
     */
    Msg generateImage(String prompt, ModelConfig config);

    /** Unique identifier for this model service (e.g. "wanxiang", "dall-e"). */
    String getName();
}
