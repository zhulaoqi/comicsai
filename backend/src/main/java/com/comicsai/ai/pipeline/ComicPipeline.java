package com.comicsai.ai.pipeline;

import com.comicsai.ai.agent.CoverArtAgent;
import com.comicsai.ai.agent.StoryboardAgent;
import com.comicsai.ai.agent.StoryboardAgent.StoryboardPanel;
import com.comicsai.ai.agent.SummaryAgent;
import com.comicsai.ai.message.Msg;
import com.comicsai.mapper.ComicPageMapper;
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
import java.util.List;

/**
 * Pipeline that orchestrates comic generation:
 * StoryboardAgent -> CoverArtAgent -> panel images -> SummaryAgent -> persist.
 */
@Component
public class ComicPipeline implements GenerationPipeline {

    private static final Logger log = LoggerFactory.getLogger(ComicPipeline.class);

    private final StoryboardAgent storyboardAgent;
    private final CoverArtAgent coverArtAgent;
    private final SummaryAgent summaryAgent;
    private final ContentService contentService;
    private final FileStorageService fileStorageService;
    private final ComicPageMapper comicPageMapper;
    private final TokenUsageMapper tokenUsageMapper;

    public ComicPipeline(StoryboardAgent storyboardAgent,
                         CoverArtAgent coverArtAgent,
                         SummaryAgent summaryAgent,
                         ContentService contentService,
                         FileStorageService fileStorageService,
                         ComicPageMapper comicPageMapper,
                         TokenUsageMapper tokenUsageMapper) {
        this.storyboardAgent = storyboardAgent;
        this.coverArtAgent = coverArtAgent;
        this.summaryAgent = summaryAgent;
        this.contentService = contentService;
        this.fileStorageService = fileStorageService;
        this.comicPageMapper = comicPageMapper;
        this.tokenUsageMapper = tokenUsageMapper;
    }

    @Override
    @Transactional
    public Content execute(Storyline storyline, GenerationConfig config) throws IOException {
        String systemPrompt = buildSystemPrompt(storyline);
        int chapterNum = (storyline.getGeneratedCount() != null ? storyline.getGeneratedCount() : 0) + 1;

        // Step 1: Generate storyboard script via StoryboardAgent
        Msg storyboardInput = Msg.builder()
                .name("pipeline")
                .role(Msg.ROLE_USER)
                .content("生成第" + chapterNum + "话漫画分镜")
                .meta("storylineContext", systemPrompt)
                .meta("chapterNum", chapterNum)
                .meta("chatModelName", config.getTextProvider())
                .meta("textModel", config.getTextModel())
                .meta("temperature", config.getTemperature())
                .meta("maxTokens", config.getMaxTokens())
                .build();

        Msg storyboardResult = storyboardAgent.call(storyboardInput);
        String title = storyboardResult.getMeta("title");
        String description = storyboardResult.getMeta("description");
        List<StoryboardPanel> panels = storyboardResult.getMeta("panels");

        if (panels == null || panels.isEmpty()) {
            panels = List.of(new StoryboardPanel("默认场景：" + storyline.getTitle(), "..."));
        }

        // Step 2: Generate cover image via CoverArtAgent
        String coverPrompt = "漫画封面：" + title + "。" + description + " 风格：" +
                (config.getImageStyle() != null ? config.getImageStyle() : "日式漫画风格");

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
                "cover_" + chapterNum + "." + getImageExtension(coverResult.getImageFormat()));

        // Step 3: Create content record
        ContentCreateDTO dto = new ContentCreateDTO();
        dto.setStorylineId(storyline.getId());
        dto.setTitle(title);
        dto.setContentType(ContentType.COMIC);
        dto.setCoverUrl(coverUrl);
        dto.setDescription(description);
        Content content = contentService.createContent(dto);

        recordTokenUsage(content.getId(), storyline.getId(),
                config.getTextProvider(), storyboardResult.getModel(),
                storyboardResult.getInputTokens(), storyboardResult.getOutputTokens());

        recordTokenUsage(content.getId(), storyline.getId(),
                config.getImageProvider(), coverResult.getModel(),
                coverResult.getInputTokens(), 0);

        // Step 4: Generate images for each panel
        for (int i = 0; i < panels.size(); i++) {
            StoryboardPanel panel = panels.get(i);

            Msg panelInput = Msg.builder()
                    .name("pipeline")
                    .role(Msg.ROLE_USER)
                    .content(panel.sceneDescription())
                    .meta("imageModelName", config.getImageProvider())
                    .meta("imageModel", config.getImageModel())
                    .meta("imageSize", config.getImageSize())
                    .meta("imageStyle", config.getImageStyle())
                    .build();

            Msg panelResult = coverArtAgent.call(panelInput);
            String imageUrl = fileStorageService.storeComicImage(
                    panelResult.getImageData(),
                    "page_" + (i + 1) + "." + getImageExtension(panelResult.getImageFormat()));

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

        // Step 5: Generate summary via SummaryAgent
        generateAndStoreSummary(storyline, config, storyboardResult.getContent());

        log.info("ComicPipeline completed: content={} for storyline={}", content.getId(), storyline.getId());
        return content;
    }

    private void generateAndStoreSummary(Storyline storyline, GenerationConfig config, String contentText) {
        try {
            Msg summaryInput = Msg.builder()
                    .name("pipeline")
                    .role(Msg.ROLE_USER)
                    .content(contentText)
                    .meta("chatModelName", config.getTextProvider())
                    .meta("textModel", config.getTextModel())
                    .build();

            Msg summaryResult = summaryAgent.call(summaryInput);

            storyline.setLatestChapterSummary(summaryResult.getContent());
            storyline.setUpdatedAt(LocalDateTime.now());

            recordTokenUsage(null, storyline.getId(),
                    config.getTextProvider(), summaryResult.getModel(),
                    summaryResult.getInputTokens(), summaryResult.getOutputTokens());
        } catch (Exception e) {
            log.warn("Failed to generate summary for storyline {}: {}", storyline.getId(), e.getMessage());
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
