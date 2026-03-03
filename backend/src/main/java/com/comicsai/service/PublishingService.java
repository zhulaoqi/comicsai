package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comicsai.mapper.ContentMapper;
import com.comicsai.model.entity.Content;
import com.comicsai.model.enums.ContentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PublishingService {

    private static final Logger log = LoggerFactory.getLogger(PublishingService.class);

    private final ContentMapper contentMapper;
    private final ContentCacheService contentCacheService;

    public PublishingService(ContentMapper contentMapper, ContentCacheService contentCacheService) {
        this.contentMapper = contentMapper;
        this.contentCacheService = contentCacheService;
    }

    /**
     * Publish all approved content: update APPROVED → PUBLISHED, set publishedAt, clear caches.
     * Called by ContentPublishingJob at 6:00 AM daily.
     *
     * @return the number of contents published
     */
    @Transactional
    public int publishApprovedContent() {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Content::getStatus, ContentStatus.PENDING_PUBLISH);
        List<Content> pendingContents = contentMapper.selectList(wrapper);

        if (pendingContents.isEmpty()) {
            log.info("No content pending publication. Skipping this publishing cycle.");
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        int publishedCount = 0;

        for (Content content : pendingContents) {
            content.setStatus(ContentStatus.PUBLISHED);
            content.setPublishedAt(now);
            content.setUpdatedAt(now);
            contentMapper.updateById(content);
            publishedCount++;
        }

        // Clear related caches after publishing
        contentCacheService.evictListCaches();

        log.info("Successfully published {} content items.", publishedCount);
        return publishedCount;
    }
}
