package com.comicsai.scheduler;

import com.comicsai.service.ContentGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that triggers AI content generation daily at 2:00 AM.
 * Delegates to ContentGeneratorService.generateAllContent().
 */
@Component
public class ContentGenerationJob {

    private static final Logger log = LoggerFactory.getLogger(ContentGenerationJob.class);

    private final ContentGeneratorService contentGeneratorService;

    public ContentGenerationJob(ContentGeneratorService contentGeneratorService) {
        this.contentGeneratorService = contentGeneratorService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void executeContentGeneration() {
        log.info("Content generation job started.");
        try {
            contentGeneratorService.generateAllContent();
            log.info("Content generation job completed successfully.");
        } catch (Exception e) {
            log.error("Content generation job failed: {}", e.getMessage(), e);
        }
    }
}
