package com.comicsai.scheduler;

import com.comicsai.service.ContentGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentGenerationJobTest {

    @Mock
    private ContentGeneratorService contentGeneratorService;

    private ContentGenerationJob contentGenerationJob;

    @BeforeEach
    void setUp() {
        contentGenerationJob = new ContentGenerationJob(contentGeneratorService);
    }

    @Test
    void executeContentGeneration_shouldCallGenerateAllContent() {
        contentGenerationJob.executeContentGeneration();

        verify(contentGeneratorService).generateAllContent();
    }

    @Test
    void executeContentGeneration_whenExceptionThrown_shouldNotPropagate() {
        doThrow(new RuntimeException("AI service down")).when(contentGeneratorService).generateAllContent();

        // Should not throw — the job catches exceptions internally
        contentGenerationJob.executeContentGeneration();

        verify(contentGeneratorService).generateAllContent();
    }
}
