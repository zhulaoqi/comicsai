package com.comicsai.ai.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comicsai.ai.agent.CoverArtAgent;
import com.comicsai.ai.agent.NovelWriterAgent;
import com.comicsai.ai.agent.SummaryAgent;
import com.comicsai.ai.message.Msg;
import com.comicsai.mapper.ContentMapper;
import com.comicsai.mapper.NovelChapterMapper;
import com.comicsai.mapper.TokenUsageMapper;
import com.comicsai.model.dto.ContentCreateDTO;
import com.comicsai.model.entity.*;
import com.comicsai.model.enums.ContentType;
import com.comicsai.service.ContentService;
import com.comicsai.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Pipeline that orchestrates novel generation:
 * NovelWriterAgent -> CoverArtAgent -> SummaryAgent -> persist.
 */
@Component
public class NovelPipeline implements GenerationPipeline {

    private static final Logger log = LoggerFactory.getLogger(NovelPipeline.class);

    private final NovelWriterAgent novelWriterAgent;
    private final CoverArtAgent coverArtAgent;
    private final SummaryAgent summaryAgent;
    private final ContentService contentService;
    private final FileStorageService fileStorageService;
    private final ContentMapper contentMapper;
    private final NovelChapterMapper novelChapterMapper;
    private final TokenUsageMapper tokenUsageMapper;

    public NovelPipeline(NovelWriterAgent novelWriterAgent,
                         CoverArtAgent coverArtAgent,
                         SummaryAgent summaryAgent,
                         ContentService contentService,
                         FileStorageService fileStorageService,
                         ContentMapper contentMapper,
                         NovelChapterMapper novelChapterMapper,
                         TokenUsageMapper tokenUsageMapper) {
        this.novelWriterAgent = novelWriterAgent;
        this.coverArtAgent = coverArtAgent;
        this.summaryAgent = summaryAgent;
        this.contentService = contentService;
        this.fileStorageService = fileStorageService;
        this.contentMapper = contentMapper;
        this.novelChapterMapper = novelChapterMapper;
        this.tokenUsageMapper = tokenUsageMapper;
    }

    @Override
    @Transactional
    public Content execute(Storyline storyline, GenerationConfig config) throws IOException {
        String systemPrompt = buildSystemPrompt(storyline);
        int chapterNum = (storyline.getGeneratedCount() != null ? storyline.getGeneratedCount() : 0) + 1;

        // Step 1: Generate chapter content via NovelWriterAgent
        Msg writerInput = Msg.builder()
                .name("pipeline")
                .role(Msg.ROLE_USER)
                .content("创作第" + chapterNum + "章")
                .meta("storylineContext", systemPrompt)
                .meta("chapterNum", chapterNum)
                .meta("chatModelName", config.getTextProvider())
                .meta("textModel", config.getTextModel())
                .meta("temperature", config.getTemperature())
                .meta("maxTokens", config.getMaxTokens())
                .build();

        Msg writerResult = novelWriterAgent.call(writerInput);
        String chapterTitle = writerResult.getMeta("chapterTitle");
        String chapterText = writerResult.getMeta("chapterText");

        // Step 2: Reuse existing Content or create a new one
        Content content = findExistingContent(storyline.getId());

        if (content == null) {
            // First chapter: create Content + generate cover
            String coverUrl = generateCover(storyline, config, chapterTitle);

            ContentCreateDTO dto = new ContentCreateDTO();
            dto.setStorylineId(storyline.getId());
            dto.setTitle(storyline.getTitle());
            dto.setContentType(ContentType.NOVEL);
            dto.setCoverUrl(coverUrl);
            dto.setDescription(chapterText.length() > 200 ? chapterText.substring(0, 200) + "..." : chapterText);
            content = contentService.createContent(dto);

            log.info("Created new Content {} for storyline {}", content.getId(), storyline.getId());
        } else {
            log.info("Appending chapter {} to existing Content {} for storyline {}",
                    chapterNum, content.getId(), storyline.getId());
        }

        recordTokenUsage(content.getId(), storyline.getId(),
                config.getTextProvider(), writerResult.getModel(),
                writerResult.getInputTokens(), writerResult.getOutputTokens());

        // Step 3: Save novel chapter
        NovelChapter chapter = new NovelChapter();
        chapter.setContentId(content.getId());
        chapter.setChapterNumber(chapterNum);
        chapter.setChapterTitle(chapterTitle);
        chapter.setChapterText(chapterText);
        novelChapterMapper.insert(chapter);

        // Step 4: Generate summary for continuity
        String summary = generateAndStoreSummary(storyline, config, chapterText);
        if (summary != null) {
            chapter.setChapterSummary(summary);
            novelChapterMapper.updateById(chapter);
        }

        log.info("NovelPipeline completed: content={}, chapter={} for storyline={}",
                content.getId(), chapterNum, storyline.getId());
        return content;
    }

    /**
     * Find the existing NOVEL Content for this storyline (all chapters share one Content).
     */
    private Content findExistingContent(Long storylineId) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Content::getStorylineId, storylineId)
               .eq(Content::getContentType, ContentType.NOVEL)
               .orderByDesc(Content::getCreatedAt)
               .last("LIMIT 1");
        return contentMapper.selectOne(wrapper);
    }

    private String generateCover(Storyline storyline, GenerationConfig config, String chapterTitle) {
        if (config.getImageProvider() == null || config.getImageProvider().isBlank()) {
            return "/files/covers/default_novel_cover.png";
        }
        try {
            String coverPrompt = "小说封面：" + storyline.getTitle() + " " + chapterTitle +
                    " 风格：" + (config.getImageStyle() != null ? config.getImageStyle() : "写实插画风格");

            Msg coverInput = Msg.builder()
                    .name("pipeline")
                    .role(Msg.ROLE_USER)
                    .content(coverPrompt)
                    .meta("imageModelName", config.getImageProvider())
                    .meta("imageModel", config.getImageModel())
                    .meta("imageSize", config.getImageSize())
                    .meta("imageStyle", config.getImageStyle())
                    .build();

            Msg coverResult = coverArtAgent.call(coverInput);
            String coverUrl = fileStorageService.storeCoverImage(
                    coverResult.getImageData(),
                    "novel_cover_" + storyline.getId() + "." + getImageExtension(coverResult.getImageFormat()));

            recordTokenUsage(null, storyline.getId(),
                    config.getImageProvider(), coverResult.getModel(),
                    coverResult.getInputTokens(), 0);

            return coverUrl;
        } catch (Exception e) {
            log.warn("Cover generation failed for storyline {}, using default: {}", storyline.getId(), e.getMessage());
            return "/files/covers/default_novel_cover.png";
        }
    }

    private String generateAndStoreSummary(Storyline storyline, GenerationConfig config, String contentText) {
        try {
            Msg summaryInput = Msg.builder()
                    .name("pipeline")
                    .role(Msg.ROLE_USER)
                    .content(contentText)
                    .meta("chatModelName", config.getTextProvider())
                    .meta("textModel", config.getTextModel())
                    .build();

            Msg summaryResult = summaryAgent.call(summaryInput);
            String summary = summaryResult.getContent();

            storyline.setLatestChapterSummary(summary);
            storyline.setUpdatedAt(LocalDateTime.now());

            recordTokenUsage(null, storyline.getId(),
                    config.getTextProvider(), summaryResult.getModel(),
                    summaryResult.getInputTokens(), summaryResult.getOutputTokens());

            return summary;
        } catch (Exception e) {
            log.warn("Failed to generate summary for storyline {}: {}", storyline.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * Regenerate a specific chapter: call WriterAgent with the same chapter number,
     * overwrite old content, and re-generate summary.
     */
    @Transactional
    public NovelChapter regenerateChapter(Storyline storyline, GenerationConfig config,
                                          Content content, NovelChapter oldChapter) throws IOException {
        int chapterNum = oldChapter.getChapterNumber();

        String systemPrompt = buildRegeneratePrompt(storyline, content.getId(), chapterNum);

        Msg writerInput = Msg.builder()
                .name("pipeline")
                .role(Msg.ROLE_USER)
                .content("重新创作第" + chapterNum + "章，请生成与之前不同的全新内容")
                .meta("storylineContext", systemPrompt)
                .meta("chapterNum", chapterNum)
                .meta("chatModelName", config.getTextProvider())
                .meta("textModel", config.getTextModel())
                .meta("temperature", config.getTemperature())
                .meta("maxTokens", config.getMaxTokens())
                .build();

        Msg writerResult = novelWriterAgent.call(writerInput);
        String chapterTitle = writerResult.getMeta("chapterTitle");
        String chapterText = writerResult.getMeta("chapterText");

        oldChapter.setChapterTitle(chapterTitle);
        oldChapter.setChapterText(chapterText);
        novelChapterMapper.updateById(oldChapter);

        recordTokenUsage(content.getId(), storyline.getId(),
                config.getTextProvider(), writerResult.getModel(),
                writerResult.getInputTokens(), writerResult.getOutputTokens());

        String summary = generateAndStoreSummary(storyline, config, chapterText);
        if (summary != null) {
            oldChapter.setChapterSummary(summary);
            novelChapterMapper.updateById(oldChapter);
        }

        log.info("Chapter {} regenerated for content={}, storyline={}",
                chapterNum, content.getId(), storyline.getId());
        return oldChapter;
    }

    /**
     * Build prompt for regeneration — uses the previous chapter's summary
     * instead of storyline.latestChapterSummary for positional accuracy.
     */
    private String buildRegeneratePrompt(Storyline storyline, Long contentId, int chapterNum) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位专业的创意写作AI助手。请基于以下故事线设定进行创作。\n\n");
        sb.append("【角色设定】\n").append(storyline.getCharacterSettings()).append("\n\n");
        sb.append("【世界观】\n").append(storyline.getWorldview()).append("\n\n");
        sb.append("【剧情大纲】\n").append(storyline.getPlotOutline()).append("\n\n");

        if (chapterNum > 1) {
            LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NovelChapter::getContentId, contentId)
                   .eq(NovelChapter::getChapterNumber, chapterNum - 1);
            NovelChapter prevChapter = novelChapterMapper.selectOne(wrapper);
            if (prevChapter != null && prevChapter.getChapterSummary() != null) {
                sb.append("【前章摘要】\n").append(prevChapter.getChapterSummary()).append("\n\n");
                sb.append("请基于前章摘要继续创作，保持剧情连贯性。\n");
            }
        }
        return sb.toString();
    }

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

    private void recordTokenUsage(Long contentId, Long storylineId,
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

    private BigDecimal estimateCost(Integer inputTokens, Integer outputTokens) {
        int inTk = inputTokens != null ? inputTokens : 0;
        int outTk = outputTokens != null ? outputTokens : 0;
        double cost = (inTk / 1000.0) * 0.002 + (outTk / 1000.0) * 0.006;
        return BigDecimal.valueOf(cost).setScale(4, RoundingMode.HALF_UP);
    }

    private String getImageExtension(String format) {
        if (format == null) return "png";
        return switch (format.toLowerCase()) {
            case "jpeg", "jpg" -> "jpg";
            case "webp" -> "webp";
            default -> "png";
        };
    }
}
