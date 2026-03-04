package com.comicsai.ai.pipeline;

import com.comicsai.model.entity.Content;
import com.comicsai.model.entity.GenerationConfig;
import com.comicsai.model.entity.Storyline;

import java.io.IOException;

/**
 * Pipeline interface for content generation workflows (AgentScope Pipeline layer).
 * Each implementation orchestrates agents, file storage, and persistence
 * for a specific content type.
 */
public interface GenerationPipeline {

    /**
     * Execute the full generation pipeline for a storyline.
     *
     * @param storyline the storyline to generate content for
     * @param config    AI generation configuration
     * @return the created Content entity
     */
    Content execute(Storyline storyline, GenerationConfig config) throws IOException;
}
