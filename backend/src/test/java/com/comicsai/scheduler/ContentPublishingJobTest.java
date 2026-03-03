package com.comicsai.scheduler;

import com.comicsai.service.PublishingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentPublishingJobTest {

    @Mock
    private PublishingService publishingService;

    private ContentPublishingJob contentPublishingJob;

    @BeforeEach
    void setUp() {
        contentPublishingJob = new ContentPublishingJob(publishingService);
    }

    @Test
    void executeContentPublishing_shouldCallPublishApprovedContent() {
        when(publishingService.publishApprovedContent()).thenReturn(5);

        contentPublishingJob.executeContentPublishing();

        verify(publishingService).publishApprovedContent();
    }

    @Test
    void executeContentPublishing_whenNoPendingContent_shouldStillSucceed() {
        when(publishingService.publishApprovedContent()).thenReturn(0);

        contentPublishingJob.executeContentPublishing();

        verify(publishingService).publishApprovedContent();
    }

    @Test
    void executeContentPublishing_whenExceptionThrown_shouldNotPropagate() {
        when(publishingService.publishApprovedContent()).thenThrow(new RuntimeException("DB error"));

        // Should not throw — the job catches exceptions internally
        contentPublishingJob.executeContentPublishing();

        verify(publishingService).publishApprovedContent();
    }
}
