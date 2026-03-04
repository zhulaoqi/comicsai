package com.comicsai.ai.agent;

import com.comicsai.ai.message.Msg;
import com.comicsai.ai.model.ModelConfig;
import com.comicsai.ai.model.ModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Agent responsible for generating images (covers and comic panels).
 * Builds optimised image prompts and delegates to the ImageModel.
 *
 * Input Msg:
 *   - content: base description for the image
 *   - metadata: "imageModelName", "imageModel", "imageSize", "imageStyle"
 *
 * Output Msg:
 *   - imageData / imageFormat populated
 *   + token usage metadata
 */
@Component
public class CoverArtAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(CoverArtAgent.class);
    private static final String NAME = "CoverArtAgent";

    private final ModelRegistry modelRegistry;

    public CoverArtAgent(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @Override
    public Msg call(Msg input) {
        String description = input.getContent();
        String imageModelName = input.getMeta("imageModelName");
        String imageModel = input.getMeta("imageModel");
        String imageSize = input.getMeta("imageSize");
        String imageStyle = input.getMeta("imageStyle");

        ModelConfig config = ModelConfig.builder()
                .modelName(imageModel)
                .imageSize(imageSize)
                .imageStyle(imageStyle)
                .build();

        log.info("{} generating image: model={}, size={}", NAME, imageModelName, imageSize);

        Msg result = modelRegistry.imageWithFallback(imageModelName, description, config);

        return Msg.builder()
                .name(NAME)
                .role(Msg.ROLE_ASSISTANT)
                .content(description)
                .imageData(result.getImageData())
                .imageFormat(result.getImageFormat())
                .meta("inputTokens", result.getInputTokens())
                .meta("model", result.getModel())
                .build();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
