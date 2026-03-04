package com.comicsai.ai.model;

import com.comicsai.ai.message.Msg;

import java.util.List;

/**
 * Text/chat model service interface (AgentScope Model layer).
 * Wraps a specific LLM provider API for text generation.
 */
public interface ChatModel {

    /**
     * Send a conversation to the model and get a response.
     *
     * @param messages ordered list of messages (system, user, assistant turns)
     * @param config   model-specific parameters (temperature, maxTokens, model name, etc.)
     * @return assistant response Msg with content and token-usage metadata
     */
    Msg chat(List<Msg> messages, ModelConfig config);

    /** Unique identifier for this model service (e.g. "qwen", "gemini"). */
    String getName();
}
