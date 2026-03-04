package com.comicsai.ai.agent;

import com.comicsai.ai.message.Msg;
import com.comicsai.ai.model.ModelConfig;
import com.comicsai.ai.model.ModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent responsible for generating chapter summaries used as context for the next chapter.
 *
 * Input Msg:
 *   - content: the chapter text to summarize
 *   - metadata: "chatModelName", "textModel"
 *
 * Output Msg:
 *   - content: the summary text
 *   + token usage metadata
 */
@Component
public class SummaryAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(SummaryAgent.class);
    private static final String NAME = "SummaryAgent";

    private final ModelRegistry modelRegistry;

    public SummaryAgent(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @Override
    public Msg call(Msg input) {
        String contentText = input.getContent();
        String chatModelName = input.getMeta("chatModelName");
        String textModel = input.getMeta("textModel");

        String prompt = "请用100-200字概括以下内容的主要剧情发展，用于作为下一章创作的上下文参考：\n\n" + contentText;

        List<Msg> messages = List.of(
                Msg.builder().role(Msg.ROLE_SYSTEM).content("你是一位专业的文学编辑，擅长提炼故事摘要。").build(),
                Msg.builder().role(Msg.ROLE_USER).content(prompt).build()
        );

        ModelConfig config = ModelConfig.builder()
                .modelName(textModel)
                .temperature(0.3)
                .maxTokens(500)
                .build();

        Msg response = modelRegistry.chatWithFallback(chatModelName, messages, config);

        log.info("{} generated summary ({} chars)", NAME,
                response.getContent() != null ? response.getContent().length() : 0);

        return Msg.builder()
                .name(NAME)
                .role(Msg.ROLE_ASSISTANT)
                .content(response.getContent())
                .meta("inputTokens", response.getInputTokens())
                .meta("outputTokens", response.getOutputTokens())
                .meta("model", response.getModel())
                .build();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
