package com.comicsai.service;

import com.comicsai.ai.pipeline.ComicPipeline;
import com.comicsai.ai.pipeline.GenerationPipeline;
import com.comicsai.ai.pipeline.NovelPipeline;
import com.comicsai.common.exception.AiProviderException;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.mapper.ContentMapper;
import com.comicsai.mapper.NovelChapterMapper;
import com.comicsai.mapper.StorylineMapper;
import com.comicsai.model.entity.Content;
import com.comicsai.model.entity.GenerationConfig;
import com.comicsai.model.entity.NovelChapter;
import com.comicsai.model.entity.Storyline;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Simplified entry point for AI content generation.
 * Delegates the actual generation workflow to {@link ComicPipeline} / {@link NovelPipeline},
 * and handles retry logic, storyline lifecycle, and error reporting.
 */
@Service
public class ContentGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(ContentGeneratorService.class);

    static final int MAX_RETRY_ATTEMPTS = 3;
    static final long RETRY_DELAY_MINUTES = 30;

    private final StorylineService storylineService;
    private final StorylineMapper storylineMapper;
    private final ContentMapper contentMapper;
    private final NovelChapterMapper novelChapterMapper;
    private final ComicPipeline comicPipeline;
    private final NovelPipeline novelPipeline;

    public ContentGeneratorService(StorylineService storylineService,
                                   StorylineMapper storylineMapper,
                                   ContentMapper contentMapper,
                                   NovelChapterMapper novelChapterMapper,
                                   ComicPipeline comicPipeline,
                                   NovelPipeline novelPipeline) {
        this.storylineService = storylineService;
        this.storylineMapper = storylineMapper;
        this.contentMapper = contentMapper;
        this.novelChapterMapper = novelChapterMapper;
        this.comicPipeline = comicPipeline;
        this.novelPipeline = novelPipeline;
    }

    @org.springframework.scheduling.annotation.Async
    public void generateContentAsync(Storyline storyline) {
        try {
            generateContentForStoryline(storyline);
        } catch (Exception e) {
            log.error("Async content generation failed for storyline {}: {}", storyline.getId(), e.getMessage(), e);
        }
    }

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

    public Content generateContentForStoryline(Storyline storyline) {
        return generateContentForStorylineWithRetry(storyline, 1);
    }

    Content generateContentForStorylineWithRetry(Storyline storyline, int attempt) {
        try {
            GenerationConfig config = storylineService.getGenerationConfig(storyline.getId());
            if (config == null) {
                config = getDefaultGenerationConfig();
            }

            GenerationPipeline pipeline = (storyline.getContentType() == ContentType.COMIC)
                    ? comicPipeline
                    : novelPipeline;

            Content content = pipeline.execute(storyline, config);

            updateStorylineGeneratedCount(storyline);
            return content;

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

    GenerationConfig getDefaultGenerationConfig() {
        GenerationConfig config = new GenerationConfig();
        config.setTextProvider("qwen");
        config.setTextModel("qwen-max");
        config.setImageProvider("wanxiang");
        config.setImageModel("wanx-v1");
        config.setTemperature(0.7);
        config.setMaxTokens(8192);
        config.setChapterWordCount(2000);
        config.setImageStyle("anime");
        config.setImageSize("1024*1024");
        return config;
    }

    private void updateStorylineGeneratedCount(Storyline storyline) {
        int currentCount = storyline.getGeneratedCount() != null ? storyline.getGeneratedCount() : 0;
        storyline.setGeneratedCount(currentCount + 1);
        storyline.setUpdatedAt(LocalDateTime.now());
        storylineMapper.updateById(storyline);
    }

    void scheduleRetry(Storyline storyline, int nextAttempt) {
        log.info("Scheduling retry attempt {} for storyline {} in {} minutes",
                nextAttempt, storyline.getId(), RETRY_DELAY_MINUTES);
    }

    void sendAlertNotification(Storyline storyline, Exception e) {
        log.error("ALERT: Content generation failed for storyline '{}' (ID: {}) after {} attempts. Error: {}",
                storyline.getTitle(), storyline.getId(), MAX_RETRY_ATTEMPTS, e.getMessage());
    }

    /**
     * Async regeneration of a specific novel chapter.
     * Sets content to REGENERATING, re-generates via NovelPipeline, then resets to PENDING_REVIEW.
     */
    @org.springframework.scheduling.annotation.Async
    public void regenerateChapterAsync(Long chapterId) {
        NovelChapter chapter = novelChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new EntityNotFoundException("章节", chapterId);
        }

        Content content = contentMapper.selectById(chapter.getContentId());
        if (content == null) {
            throw new EntityNotFoundException("内容", chapter.getContentId());
        }

        if (content.getStorylineId() == null) {
            throw new BusinessException(400, "该内容没有关联的故事线，无法重新生成");
        }

        Storyline storyline = storylineService.getStorylineById(content.getStorylineId());
        GenerationConfig config = storylineService.getGenerationConfig(storyline.getId());
        if (config == null) {
            config = getDefaultGenerationConfig();
        }

        transitionContentStatus(content, ContentStatus.REGENERATING);

        try {
            novelPipeline.regenerateChapter(storyline, config, content, chapter);
            transitionContentStatus(content, ContentStatus.PENDING_REVIEW);
            log.info("Chapter {} regenerated successfully, content {} back to PENDING_REVIEW",
                    chapterId, content.getId());
        } catch (Exception e) {
            log.error("Chapter regeneration failed for chapter {}: {}", chapterId, e.getMessage(), e);
            transitionContentStatus(content, ContentStatus.PENDING_REVIEW);
        }
    }

    private void transitionContentStatus(Content content, ContentStatus targetStatus) {
        content.setStatus(targetStatus);
        content.setUpdatedAt(LocalDateTime.now());
        contentMapper.updateById(content);
    }
}
