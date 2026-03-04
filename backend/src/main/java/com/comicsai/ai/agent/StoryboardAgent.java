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
 * Agent responsible for generating comic storyboard scripts.
 * Encapsulates prompt engineering and script parsing logic.
 *
 * Input Msg metadata:
 *   - "storylineContext" (String): system prompt built from storyline settings
 *   - "chapterNum" (Integer): chapter number
 *   - "chatModelName" (String): preferred chat model
 *   - "textModel" (String): model name
 *   - "temperature" (Double)
 *   - "maxTokens" (Integer)
 *
 * Output Msg metadata:
 *   - "title" (String)
 *   - "description" (String)
 *   - "panels" (List of StoryboardPanel records)
 *   + token usage fields from the underlying model
 */
@Component
public class StoryboardAgent implements Agent {

    private static final Logger log = LoggerFactory.getLogger(StoryboardAgent.class);
    private static final String NAME = "StoryboardAgent";

    private final ModelRegistry modelRegistry;

    public StoryboardAgent(ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    @Override
    public Msg call(Msg input) {
        String systemPrompt = input.getMeta("storylineContext");
        int chapterNum = input.<Integer>getMeta("chapterNum");
        String chatModelName = input.getMeta("chatModelName");
        String textModel = input.getMeta("textModel");

        String prompt = String.format(
                "请为漫画创作第%d话的分镜脚本。要求：\n" +
                "1. 包含一个标题\n" +
                "2. 包含一段简短描述\n" +
                "3. 包含3-5个分镜，每个分镜包含：场景描述（用于生成图片）和对话文本\n" +
                "4. 请用以下格式输出：\n" +
                "标题：[标题]\n" +
                "描述：[描述]\n" +
                "---\n" +
                "分镜1：\n场景：[详细的场景描述，用于AI绘图]\n对话：[角色对话文本]\n" +
                "---\n" +
                "分镜2：\n场景：[详细的场景描述]\n对话：[角色对话文本]\n" +
                "---\n" +
                "(以此类推)", chapterNum);

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
        String script = response.getContent();

        String title = parseTitle(script, "第" + chapterNum + "话");
        String description = parseDescription(script);
        List<StoryboardPanel> panels = parseStoryboardPanels(script);

        log.info("{} generated storyboard: title='{}', panels={}", NAME, title, panels.size());

        return Msg.builder()
                .name(NAME)
                .role(Msg.ROLE_ASSISTANT)
                .content(script)
                .meta("title", title)
                .meta("description", description)
                .meta("panels", panels)
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

    static String parseTitle(String script, String defaultTitle) {
        for (String line : script.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("标题：") || trimmed.startsWith("标题:")) {
                String title = trimmed.substring(3).trim();
                return title.isEmpty() ? defaultTitle : title;
            }
        }
        return defaultTitle;
    }

    static String parseDescription(String script) {
        for (String line : script.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("描述：") || trimmed.startsWith("描述:")) {
                return trimmed.substring(3).trim();
            }
        }
        return "";
    }

    static List<StoryboardPanel> parseStoryboardPanels(String script) {
        List<StoryboardPanel> panels = new ArrayList<>();
        String[] sections = script.split("---");

        for (String section : sections) {
            String trimmed = section.trim();
            if (trimmed.isEmpty()) continue;

            String scene = null;
            String dialogue = null;

            for (String line : trimmed.split("\n")) {
                String l = line.trim();
                if (l.startsWith("场景：") || l.startsWith("场景:")) {
                    scene = l.substring(3).trim();
                } else if (l.startsWith("对话：") || l.startsWith("对话:")) {
                    dialogue = l.substring(3).trim();
                }
            }

            if (scene != null) {
                panels.add(new StoryboardPanel(scene, dialogue != null ? dialogue : ""));
            }
        }
        return panels;
    }

    /** A single storyboard panel for comic generation. */
    public record StoryboardPanel(String sceneDescription, String dialogueText) {}
}
