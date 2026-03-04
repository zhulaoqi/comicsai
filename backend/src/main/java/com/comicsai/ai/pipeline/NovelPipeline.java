package com.comicsai.ai.pipeline;

import com.comicsai.ai.agent.CoverArtAgent;
import com.comicsai.ai.agent.NovelWriterAgent;
import com.comicsai.ai.agent.SummaryAgent;
import com.comicsai.ai.message.Msg;
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
    private final NovelChapterMapper novelChapterMapper;
    private final TokenUsageMapper tokenUsageMapper;

    public NovelPipeline(NovelWriterAgent novelWriterAgent,
                         CoverArtAgent coverArtAgent,
                         SummaryAgent summaryAgent,
                         ContentService contentService,
                         FileStorageService fileStorageService,
                         NovelChapterMapper novelChapterMapper,
                         TokenUsageMapper tokenUsageMapper) {
        this.novelWriterAgent = novelWriterAgent;
        this.coverArtAgent = coverArtAgent;
        this.summaryAgent = summaryAgent;
        this.contentService = contentService;
        this.fileStorageService = fileStorageService;
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

        // Step 2: Generate cover image only for the first chapter (novels don't need per-chapter covers)
        String coverUrl;
        boolean shouldGenerateCover = chapterNum == 1
                && config.getImageProvider() != null
                && !config.getImageProvider().isBlank();

        if (shouldGenerateCover) {
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
                coverUrl = fileStorageService.storeCoverImage(
                        coverResult.getImageData(),
                        "novel_cover_" + chapterNum + "." + getImageExtension(coverResult.getImageFormat()));

                recordTokenUsage(null, storyline.getId(),
                        config.getImageProvider(), coverResult.getModel(),
                        coverResult.getInputTokens(), 0);
            } catch (Exception e) {
                log.warn("Cover generation failed for storyline {}, using default: {}", storyline.getId(), e.getMessage());
                coverUrl = "/files/covers/default_novel_cover.png";
            }
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

        recordTokenUsage(content.getId(), storyline.getId(),
                config.getTextProvider(), writerResult.getModel(),
                writerResult.getInputTokens(), writerResult.getOutputTokens());

        // Step 4: Save novel chapter
        NovelChapter chapter = new NovelChapter();
        chapter.setContentId(content.getId());
        chapter.setChapterNumber(chapterNum);
        chapter.setChapterTitle(chapterTitle);
        chapter.setChapterText(chapterText);
        novelChapterMapper.insert(chapter);

        // Step 5: Generate summary
        String summary = generateAndStoreSummary(storyline, config, chapterText);
        if (summary != null) {
            chapter.setChapterSummary(summary);
            novelChapterMapper.updateById(chapter);
        }

        log.info("NovelPipeline completed: content={} for storyline={}", content.getId(), storyline.getId());
        return content;
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
