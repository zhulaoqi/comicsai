package com.comicsai.ai.agent;

import com.comicsai.ai.message.Msg;
import com.comicsai.ai.model.ModelConfig;
import com.comicsai.ai.model.ModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent responsible for generating novel chapter content.
 * Encapsulates prompt engineering and chapter-text parsing.
 *
 * Input Msg metadata:
 *   - "storylineContext" (String): system prompt
 *   - "chapterNum" (Integer)
 *   - "chatModelName" / "textModel" / "temperature" / "maxTokens"
 *
 * Output Msg metadata:
 *   - "chapterTitle" (String)
 *   - "chapterText" (String)
 *   + token usage fields
 */
@Component
public class NovelWriterAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(NovelWriterAgent.class);
    private static final String NAME = "NovelWriterAgent";

    private final ModelRegistry modelRegistry;

    public NovelWriterAgent(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @Override
    public Msg call(Msg input) {
        String systemPrompt = input.getMeta("storylineContext");
        int chapterNum = input.<Integer>getMeta("chapterNum");
        String chatModelName = input.getMeta("chatModelName");
        String textModel = input.getMeta("textModel");

        String prompt = String.format(
                "请创作第%d章的小说内容。要求：\n" +
                "1. 包含章节标题\n" +
                "2. 正文不少于800字\n" +
                "3. 请用以下格式输出：\n" +
                "章节标题：[标题]\n" +
                "---\n" +
                "[正文内容]", chapterNum);

        List<Msg> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Msg.builder().role(Msg.ROLE_SYSTEM).content(systemPrompt).build());
        }
        messages.add(Msg.builder().role(Msg.ROLE_USER).content(prompt).build());

        ModelConfig config = ModelConfig.builder()
                .modelName(textModel)
                .temperature(input.getMeta("temperature"))
                .maxTokens(input.getMeta("maxTokens"))
                .build();

        Msg response = modelRegistry.chatWithFallback(chatModelName, messages, config);
        String content = response.getContent();

        String chapterTitle = parseChapterTitle(content, "第" + chapterNum + "章");
        String chapterText = parseChapterText(content);

        log.info("{} generated chapter: title='{}'", NAME, chapterTitle);

        return Msg.builder()
                .name(NAME)
                .role(Msg.ROLE_ASSISTANT)
                .content(content)
                .meta("chapterTitle", chapterTitle)
                .meta("chapterText", chapterText)
                .meta("inputTokens", response.getInputTokens())
                .meta("outputTokens", response.getOutputTokens())
                .meta("model", response.getModel())
                .build();
    }

    @Override
    public String getName() {
        return NAME;
    }

    // ==================== Parsing Helpers ====================

    static String parseChapterTitle(String content, String defaultTitle) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("章节标题：") || trimmed.startsWith("章节标题:")) {
                String title = trimmed.substring(5).trim();
                return title.isEmpty() ? defaultTitle : title;
            }
        }
        return defaultTitle;
    }

    static String parseChapterText(String content) {
        int separatorIndex = content.indexOf("---");
        if (separatorIndex >= 0 && separatorIndex + 3 < content.length()) {
            return content.substring(separatorIndex + 3).trim();
        }
        int newlineIndex = content.indexOf('\n');
        if (newlineIndex >= 0) {
            return content.substring(newlineIndex + 1).trim();
        }
        return content;
    }
}
