package com.comicsai.ai.agent;

import com.comicsai.ai.message.Msg;
import com.comicsai.ai.model.ModelConfig;
import com.comicsai.ai.model.ModelRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
        Integer wordCountMeta = input.getMeta("chapterWordCount");
        int targetWordCount = wordCountMeta != null ? wordCountMeta : 2000;
        String chatModelName = input.getMeta("chatModelName");
        String textModel = input.getMeta("textModel");

        String prompt = String.format(
                "请创作第%d章的小说内容。\n\n" +
                "【字数要求】\n" +
                "正文必须不少于%d字，目标%d字左右。这是硬性要求，字数不足将被退回重写。\n\n" +
                "【内容要求】\n" +
                "1. 包含一个吸引读者的章节标题\n" +
                "2. 正文要有完整的场景描写、人物对话和情节推进\n" +
                "3. 每个场景至少包含：环境/氛围描写 + 角色动作/表情 + 对话/心理活动\n" +
                "4. 章节中至少包含2-3个场景转换或情节节点\n" +
                "5. 结尾处设置悬念或伏笔\n\n" +
                "【输出格式】\n" +
                "章节标题：[标题]\n" +
                "---\n" +
                "[正文内容]\n\n" +
                "【重要】请输出纯文本，不要使用任何 Markdown 格式（如 **加粗**、*斜体*、# 标题、``` 代码块等）。",
                chapterNum, targetWordCount, targetWordCount);

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

        String chapterTitle = stripMarkdown(parseChapterTitle(content, "第" + chapterNum + "章"));
        String chapterText = stripMarkdown(parseChapterText(content));

        log.info("{} generated chapter: title='{}', length={}", NAME, chapterTitle, chapterText.length());

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

    private static final Pattern MD_BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern MD_ITALIC = Pattern.compile("\\*(.+?)\\*");
    private static final Pattern MD_HEADING = Pattern.compile("(?m)^#{1,6}\\s+");
    private static final Pattern MD_CODE_BLOCK = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern MD_INLINE_CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern MD_LINK = Pattern.compile("\\[([^\\]]+)]\\([^)]+\\)");

    static String stripMarkdown(String text) {
        if (text == null || text.isEmpty()) return text;
        String result = text;
        result = MD_CODE_BLOCK.matcher(result).replaceAll("");
        result = MD_BOLD.matcher(result).replaceAll("$1");
        result = MD_ITALIC.matcher(result).replaceAll("$1");
        result = MD_HEADING.matcher(result).replaceAll("");
        result = MD_INLINE_CODE.matcher(result).replaceAll("$1");
        result = MD_LINK.matcher(result).replaceAll("$1");
        return result.trim();
    }
}
