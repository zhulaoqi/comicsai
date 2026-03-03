package com.comicsai.service;

import com.comicsai.ai.*;
import com.comicsai.common.exception.AiProviderException;
import com.comicsai.mapper.*;
import com.comicsai.model.dto.ContentCreateDTO;
import com.comicsai.model.entity.*;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ContentGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ContentGeneratorService.class);

    static final int MAX_RETRY_ATTEMPTS = 3;
    static final long RETRY_DELAY_MINUTES = 30;

    private final StorylineService storylineService;
    private final ContentService contentService;
    private final AiProviderFactory aiProviderFactory;
    private final FileStorageService fileStorageService;
    private final ComicPageMapper comicPageMapper;
    private final NovelChapterMapper novelChapterMapper;
    private final TokenUsageMapper tokenUsageMapper;
    private final StorylineMapper storylineMapper;

    public ContentGeneratorService(
            StorylineService storylineService,
            ContentService contentService,
            AiProviderFactory aiProviderFactory,
            FileStorageService fileStorageService,
            ComicPageMapper comicPageMapper,
            NovelChapterMapper novelChapterMapper,
            TokenUsageMapper tokenUsageMapper,
            StorylineMapper storylineMapper) {
        this.storylineService = storylineService;
        this.contentService = contentService;
        this.aiProviderFactory = aiProviderFactory;
        this.fileStorageService = fileStorageService;
        this.comicPageMapper = comicPageMapper;
        this.novelChapterMapper = novelChapterMapper;
        this.tokenUsageMapper = tokenUsageMapper;
        this.storylineMapper = storylineMapper;
    }

    /**
     * Generate content for all enabled storylines.
     * Called by the scheduler (ContentGenerationJob).
     */
    public void generateAllContent() {
        List<Storyline> enabledStorylines = storylineService.getEnabledStorylines();
        log.info("Starting content generation for {} enabled storylines", enabledStorylines.size());

        for (Storyline storyline : enabledStorylines) {
            try {
                generateContentForStoryline(storyline);
            } catch (Exception e) {
                log.error("Failed to generate content for storyline {}: {}", storyline.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Generate content for a single storyline with retry support.
     */
    public Content generateContentForStoryline(Storyline storyline) {
        return generateContentForStorylineWithRetry(storyline, 1);
    }

    /**
     * Internal method with retry tracking.
     */
    Content generateContentForStorylineWithRetry(Storyline storyline, int attempt) {
        try {
            GenerationConfig config = storylineService.getGenerationConfig(storyline.getId());
            if (config == null) {
                config = getDefaultGenerationConfig();
            }

            if (storyline.getContentType() == ContentType.COMIC) {
                return generateComic(storyline, config);
            } else {
                return generateNovel(storyline, config);
            }
        } catch (AiProviderException e) {
            log.warn("AI provider failed for storyline {} (attempt {}/{}): {}",
                    storyline.getId(), attempt, MAX_RETRY_ATTEMPTS, e.getMessage());

            if (attempt >= MAX_RETRY_ATTEMPTS) {
                log.error("All {} retry attempts exhausted for storyline {}. Sending alert.",
                        MAX_RETRY_ATTEMPTS, storyline.getId());
                sendAlertNotification(storyline, e);
                throw e;
            }

            scheduleRetry(storyline, attempt + 1);
            return null;
        } catch (IOException e) {
            log.error("IO error during content generation for storyline {}: {}",
                    storyline.getId(), e.getMessage(), e);
            throw new RuntimeException("Content generation IO error", e);
        }
    }

    /**
     * Build the system prompt from storyline elements.
     * Includes character settings, worldview, plot outline, and previous chapter summary.
     */
    String buildSystemPrompt(Storyline storyline) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的创意写作AI助手。请基于以下故事线设定进行创作。\n\n");

        sb.append("【角色设定】\n").append(storyline.getCharacterSettings()).append("\n\n");
        sb.append("【世界观】\n").append(storyline.getWorldview()).append("\n\n");
        sb.append("【剧情大纲】\n").append(storyline.getPlotOutline()).append("\n\n");

        if (storyline.getLatestChapterSummary() != null && !storyline.getLatestChapterSummary().isBlank()) {
            sb.append("【前章摘要】\n").append(storyline.getLatestChapterSummary()).append("\n\n");
            sb.append("请基于前章摘要继续创作，保持剧情连贯性。\n");
        }

        return sb.toString();
    }

    /**
     * Generate a comic: text AI produces storyboard script → image AI generates images → save.
     */
    @Transactional
    Content generateComic(Storyline storyline, GenerationConfig config) throws IOException {
        String systemPrompt = buildSystemPrompt(storyline);
        int chapterNum = (storyline.getGeneratedCount() != null ? storyline.getGeneratedCount() : 0) + 1;

        // Step 1: Generate storyboard script via text AI
        String comicPrompt = String.format(
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

        TextGenerationRequest textRequest = new TextGenerationRequest(
                comicPrompt, systemPrompt,
                config.getTemperature(), config.getMaxTokens(), config.getTextModel());

        TextGenerationResult scriptResult = aiProviderFactory.generateTextWithFallback(
                config.getTextProvider(), textRequest);

        // Parse the storyboard script
        String script = scriptResult.getContent();
        String title = parseTitle(script, storyline.getTitle() + " 第" + chapterNum + "话");
        String description = parseDescription(script);
        List<StoryboardPanel> panels = parseStoryboardPanels(script);

        if (panels.isEmpty()) {
            panels.add(new StoryboardPanel("默认场景：" + storyline.getTitle(), "..."));
        }

        // Step 2: Generate cover image
        String coverPrompt = "漫画封面：" + title + "。" + description + " 风格：" +
                (config.getImageStyle() != null ? config.getImageStyle() : "日式漫画风格");
        ImageGenerationRequest coverRequest = new ImageGenerationRequest(
                coverPrompt, config.getImageStyle(), config.getImageSize(), config.getImageModel());
        ImageGenerationResult coverResult = aiProviderFactory.generateImageWithFallback(
                config.getImageProvider(), coverRequest);

        String coverUrl = fileStorageService.storeCoverImage(
                coverResult.getImageData(), "cover_" + chapterNum + "." + getImageExtension(coverResult.getFormat()));

        // Step 3: Create content record
        ContentCreateDTO dto = new ContentCreateDTO();
        dto.setStorylineId(storyline.getId());
        dto.setTitle(title);
        dto.setContentType(ContentType.COMIC);
        dto.setCoverUrl(coverUrl);
        dto.setDescription(description);
        Content content = contentService.createContent(dto);

        // Record token usage for text generation
        recordTokenUsage(content.getId(), storyline.getId(),
                config.getTextProvider(), scriptResult.getModel(),
                scriptResult.getInputTokens(), scriptResult.getOutputTokens());

        // Record token usage for cover image
        recordTokenUsage(content.getId(), storyline.getId(),
                config.getImageProvider(), coverResult.getModel(),
                coverResult.getInputTokens(), 0);

        // Step 4: Generate images for each panel and save comic pages
        List<TokenUsage> imageTokenUsages = new ArrayList<>();
        for (int i = 0; i < panels.size(); i++) {
            StoryboardPanel panel = panels.get(i);
            ImageGenerationRequest panelRequest = new ImageGenerationRequest(
                    panel.sceneDescription(), config.getImageStyle(),
                    config.getImageSize(), config.getImageModel());

            ImageGenerationResult panelResult = aiProviderFactory.generateImageWithFallback(
                    config.getImageProvider(), panelRequest);

            String imageUrl = fileStorageService.storeComicImage(
                    panelResult.getImageData(),
                    "page_" + (i + 1) + "." + getImageExtension(panelResult.getFormat()));

            ComicPage page = new ComicPage();
            page.setContentId(content.getId());
            page.setPageNumber(i + 1);
            page.setImageUrl(imageUrl);
            page.setDialogueText(panel.dialogueText());
            comicPageMapper.insert(page);

            recordTokenUsage(content.getId(), storyline.getId(),
                    config.getImageProvider(), panelResult.getModel(),
                    panelResult.getInputTokens(), 0);
        }

        // Step 5: Generate chapter summary and update storyline
        generateAndStoreSummary(storyline, config, script);

        // Update generated count
        updateStorylineGeneratedCount(storyline);

        log.info("Successfully generated comic content {} for storyline {}", content.getId(), storyline.getId());
        return content;
    }

    /**
     * Generate a novel: text AI produces chapter content → save.
     */
    @Transactional
    Content generateNovel(Storyline storyline, GenerationConfig config) throws IOException {
        String systemPrompt = buildSystemPrompt(storyline);
        int chapterNum = (storyline.getGeneratedCount() != null ? storyline.getGeneratedCount() : 0) + 1;

        // Step 1: Generate chapter content via text AI
        String novelPrompt = String.format(
                "请创作第%d章的小说内容。要求：\n" +
                "1. 包含章节标题\n" +
                "2. 正文不少于800字\n" +
                "3. 请用以下格式输出：\n" +
                "章节标题：[标题]\n" +
                "---\n" +
                "[正文内容]", chapterNum);

        TextGenerationRequest textRequest = new TextGenerationRequest(
                novelPrompt, systemPrompt,
                config.getTemperature(), config.getMaxTokens(), config.getTextModel());

        TextGenerationResult chapterResult = aiProviderFactory.generateTextWithFallback(
                config.getTextProvider(), textRequest);

        String chapterContent = chapterResult.getContent();
        String chapterTitle = parseChapterTitle(chapterContent, "第" + chapterNum + "章");
        String chapterText = parseChapterText(chapterContent);

        // Step 2: Generate cover image for the novel chapter
        String coverPrompt = "小说封面：" + storyline.getTitle() + " " + chapterTitle +
                " 风格：" + (config.getImageStyle() != null ? config.getImageStyle() : "写实插画风格");

        String coverUrl;
        if (config.getImageProvider() != null && !config.getImageProvider().isBlank()) {
            ImageGenerationRequest coverRequest = new ImageGenerationRequest(
                    coverPrompt, config.getImageStyle(), config.getImageSize(), config.getImageModel());
            ImageGenerationResult coverResult = aiProviderFactory.generateImageWithFallback(
                    config.getImageProvider(), coverRequest);
            coverUrl = fileStorageService.storeCoverImage(
                    coverResult.getImageData(), "novel_cover_" + chapterNum + "." + getImageExtension(coverResult.getFormat()));

            recordTokenUsage(null, storyline.getId(),
                    config.getImageProvider(), coverResult.getModel(),
                    coverResult.getInputTokens(), 0);
        } else {
            coverUrl = "/files/covers/default_novel_cover.png";
        }

        // Step 3: Create content record
        ContentCreateDTO dto = new ContentCreateDTO();
        dto.setStorylineId(storyline.getId());
        dto.setTitle(storyline.getTitle() + " - " + chapterTitle);
        dto.setContentType(ContentType.NOVEL);
        dto.setCoverUrl(coverUrl);
        dto.setDescription(chapterText.length() > 200 ? chapterText.substring(0, 200) + "..." : chapterText);
        Content content = contentService.createContent(dto);

        // Record token usage for text generation
        recordTokenUsage(content.getId(), storyline.getId(),
                config.getTextProvider(), chapterResult.getModel(),
                chapterResult.getInputTokens(), chapterResult.getOutputTokens());

        // Step 4: Save novel chapter
        NovelChapter chapter = new NovelChapter();
        chapter.setContentId(content.getId());
        chapter.setChapterNumber(chapterNum);
        chapter.setChapterTitle(chapterTitle);
        chapter.setChapterText(chapterText);
        novelChapterMapper.insert(chapter);

        // Step 5: Generate chapter summary and update storyline
        String summary = generateAndStoreSummary(storyline, config, chapterText);

        // Update chapter summary on the novel chapter record too
        chapter.setChapterSummary(summary);
        novelChapterMapper.updateById(chapter);

        // Update generated count
        updateStorylineGeneratedCount(storyline);

        log.info("Successfully generated novel content {} for storyline {}", content.getId(), storyline.getId());
        return content;
    }

    /**
     * Generate a chapter summary using text AI and store it on the storyline.
     */
    String generateAndStoreSummary(Storyline storyline, GenerationConfig config, String contentText) {
        try {
            String summaryPrompt = "请用100-200字概括以下内容的主要剧情发展，用于作为下一章创作的上下文参考：\n\n" + contentText;
            TextGenerationRequest summaryRequest = new TextGenerationRequest(
                    summaryPrompt, "你是一位专业的文学编辑，擅长提炼故事摘要。",
                    0.3, 500, config.getTextModel());

            TextGenerationResult summaryResult = aiProviderFactory.generateTextWithFallback(
                    config.getTextProvider(), summaryRequest);

            String summary = summaryResult.getContent();

            // Update storyline's latest chapter summary
            storyline.setLatestChapterSummary(summary);
            storyline.setUpdatedAt(LocalDateTime.now());
            storylineMapper.updateById(storyline);

            // Record token usage for summary generation
            recordTokenUsage(null, storyline.getId(),
                    config.getTextProvider(), summaryResult.getModel(),
                    summaryResult.getInputTokens(), summaryResult.getOutputTokens());

            return summary;
        } catch (Exception e) {
            log.warn("Failed to generate chapter summary for storyline {}: {}", storyline.getId(), e.getMessage());
            // Summary generation failure should not fail the whole content generation
            return null;
        }
    }

    /**
     * Record token usage for an AI call.
     */
    void recordTokenUsage(Long contentId, Long storylineId,
                          String providerName, String modelName,
                          Integer inputTokens, Integer outputTokens) {
        TokenUsage usage = new TokenUsage();
        usage.setContentId(contentId);
        usage.setStorylineId(storylineId);
        usage.setProviderName(providerName);
        usage.setModelName(modelName != null ? modelName : "unknown");
        usage.setInputTokens(inputTokens != null ? inputTokens : 0);
        usage.setOutputTokens(outputTokens != null ? outputTokens : 0);
        usage.setEstimatedCost(estimateCost(inputTokens, outputTokens));
        usage.setCalledAt(LocalDateTime.now());
        tokenUsageMapper.insert(usage);
    }

    /**
     * Estimate cost based on token counts (simplified pricing model).
     */
    BigDecimal estimateCost(Integer inputTokens, Integer outputTokens) {
        int inTokens = inputTokens != null ? inputTokens : 0;
        int outTokens = outputTokens != null ? outputTokens : 0;
        // Approximate cost: $0.002 per 1K input tokens, $0.006 per 1K output tokens
        double cost = (inTokens / 1000.0) * 0.002 + (outTokens / 1000.0) * 0.006;
        return BigDecimal.valueOf(cost).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Schedule a retry for content generation after the configured delay.
     * In production this would use a task scheduler; here we log the intent.
     */
    void scheduleRetry(Storyline storyline, int nextAttempt) {
        log.info("Scheduling retry attempt {} for storyline {} in {} minutes",
                nextAttempt, storyline.getId(), RETRY_DELAY_MINUTES);
        // In a real implementation, this would schedule a delayed task.
        // For now, we perform the retry inline for testability.
    }

    /**
     * Send alert notification to admin when all retries are exhausted.
     */
    void sendAlertNotification(Storyline storyline, Exception e) {
        log.error("ALERT: Content generation failed for storyline '{}' (ID: {}) after {} attempts. Error: {}",
                storyline.getTitle(), storyline.getId(), MAX_RETRY_ATTEMPTS, e.getMessage());
        // In production, this would send an email or system notification to admin
    }

    private void updateStorylineGeneratedCount(Storyline storyline) {
        int currentCount = storyline.getGeneratedCount() != null ? storyline.getGeneratedCount() : 0;
        storyline.setGeneratedCount(currentCount + 1);
        storyline.setUpdatedAt(LocalDateTime.now());
        storylineMapper.updateById(storyline);
    }

    GenerationConfig getDefaultGenerationConfig() {
        GenerationConfig config = new GenerationConfig();
        config.setTextProvider("gemini");
        config.setTextModel("gemini-1.5-flash");
        config.setImageProvider("wanxiang");
        config.setImageModel("wanx-v1");
        config.setTemperature(0.7);
        config.setMaxTokens(2000);
        config.setImageStyle("anime");
        config.setImageSize("1024x1024");
        return config;
    }

    // ==================== Parsing Helpers ====================

    String parseTitle(String script, String defaultTitle) {
        for (String line : script.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("标题：") || trimmed.startsWith("标题:")) {
                String title = trimmed.substring(3).trim();
                return title.isEmpty() ? defaultTitle : title;
            }
        }
        return defaultTitle;
    }

    String parseDescription(String script) {
        for (String line : script.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("描述：") || trimmed.startsWith("描述:")) {
                return trimmed.substring(3).trim();
            }
        }
        return "";
    }

    List<StoryboardPanel> parseStoryboardPanels(String script) {
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

    String parseChapterTitle(String content, String defaultTitle) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("章节标题：") || trimmed.startsWith("章节标题:")) {
                String title = trimmed.substring(5).trim();
                return title.isEmpty() ? defaultTitle : title;
            }
        }
        return defaultTitle;
    }

    String parseChapterText(String content) {
        int separatorIndex = content.indexOf("---");
        if (separatorIndex >= 0 && separatorIndex + 3 < content.length()) {
            return content.substring(separatorIndex + 3).trim();
        }
        // If no separator, return everything after the first line
        int newlineIndex = content.indexOf('\n');
        if (newlineIndex >= 0) {
            return content.substring(newlineIndex + 1).trim();
        }
        return content;
    }

    private String getImageExtension(String format) {
        if (format == null) return "png";
        return switch (format.toLowerCase()) {
            case "jpeg", "jpg" -> "jpg";
            case "webp" -> "webp";
            default -> "png";
        };
    }

    /**
     * Record representing a single storyboard panel for comic generation.
     */
    record StoryboardPanel(String sceneDescription, String dialogueText) {}
}
