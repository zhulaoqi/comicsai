package com.comicsai.scheduler;

import com.comicsai.service.PublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers content publishing daily at 6:00 AM.
 * Delegates to PublishingService.publishApprovedContent().
 */
@Component
public class ContentPublishingJob {

    private static final Logger log = LoggerFactory.getLogger(ContentPublishingJob.class);

    private final PublishingService publishingService;

    public ContentPublishingJob(PublishingService publishingService) {
        this.publishingService = publishingService;
    }

    @Scheduled(cron = "0 0 6 * * ?")
    public void executeContentPublishing() {
        log.info("Content publishing job started.");
        try {
            int count = publishingService.publishApprovedContent();
            log.info("Content publishing job completed. Published {} items.", count);
        } catch (Exception e) {
            log.error("Content publishing job failed: {}", e.getMessage(), e);
        }
    }
}
