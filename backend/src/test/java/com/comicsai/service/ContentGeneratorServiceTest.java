package com.comicsai.service;

import com.comicsai.ai.pipeline.ComicPipeline;
import com.comicsai.ai.pipeline.NovelPipeline;
import com.comicsai.common.exception.AiProviderException;
import com.comicsai.mapper.StorylineMapper;
import com.comicsai.model.entity.Content;
import com.comicsai.model.entity.GenerationConfig;
import com.comicsai.model.entity.Storyline;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentGeneratorServiceTest {

    @Mock private StorylineService storylineService;
    @Mock private StorylineMapper storylineMapper;
    @Mock private ComicPipeline comicPipeline;
    @Mock private NovelPipeline novelPipeline;

    private ContentGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        generatorService = new ContentGeneratorService(
                storylineService, storylineMapper, comicPipeline, novelPipeline);
    }

    private Storyline createStoryline(ContentType type) {
        Storyline s = new Storyline();
        s.setId(1L);
        s.setTitle("测试故事线");
        s.setGenre("奇幻");
        s.setContentType(type);
        s.setCharacterSettings("主角：小明");
        s.setWorldview("魔法世界");
        s.setPlotOutline("冒险之旅");
        s.setStatus(StorylineStatus.ENABLED);
        s.setGeneratedCount(0);
        return s;
    }

    private GenerationConfig createConfig() {
        GenerationConfig c = new GenerationConfig();
        c.setTextProvider("qwen");
        c.setTextModel("qwen-max");
        c.setImageProvider("wanxiang");
        c.setImageModel("wanx-v1");
        c.setTemperature(0.7);
        c.setMaxTokens(2000);
        c.setImageStyle("anime");
        c.setImageSize("1024*1024");
        return c;
    }

    // ==================== Pipeline Delegation ====================

    @Test
    void generateContentForStoryline_comic_delegatesToComicPipeline() throws IOException {
        Storyline storyline = createStoryline(ContentType.COMIC);
        GenerationConfig config = createConfig();
        when(storylineService.getGenerationConfig(1L)).thenReturn(config);

        Content mockContent = new Content();
        mockContent.setId(10L);
        when(comicPipeline.execute(storyline, config)).thenReturn(mockContent);

        Content result = generatorService.generateContentForStoryline(storyline);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        verify(comicPipeline).execute(storyline, config);
        verify(novelPipeline, never()).execute(any(), any());
    }

    @Test
    void generateContentForStoryline_novel_delegatesToNovelPipeline() throws IOException {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        GenerationConfig config = createConfig();
        when(storylineService.getGenerationConfig(1L)).thenReturn(config);

        Content mockContent = new Content();
        mockContent.setId(20L);
        when(novelPipeline.execute(storyline, config)).thenReturn(mockContent);

        Content result = generatorService.generateContentForStoryline(storyline);

        assertNotNull(result);
        assertEquals(20L, result.getId());
        verify(novelPipeline).execute(storyline, config);
        verify(comicPipeline, never()).execute(any(), any());
    }

    @Test
    void generateContentForStoryline_updatesGeneratedCount() throws IOException {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        storyline.setGeneratedCount(5);
        when(storylineService.getGenerationConfig(1L)).thenReturn(createConfig());
        when(novelPipeline.execute(any(), any())).thenReturn(new Content());

        generatorService.generateContentForStoryline(storyline);

        assertEquals(6, storyline.getGeneratedCount());
        verify(storylineMapper).updateById(storyline);
    }

    // ==================== Retry Mechanism ====================

    @Test
    void generateContentForStorylineWithRetry_retriesOnAiFailure() throws IOException {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        when(storylineService.getGenerationConfig(1L)).thenReturn(createConfig());
        when(novelPipeline.execute(any(), any()))
                .thenThrow(new AiProviderException("qwen", "API error"));

        Content result = generatorService.generateContentForStorylineWithRetry(storyline, 1);
        assertNull(result);
    }

    @Test
    void generateContentForStorylineWithRetry_throwsAfterMaxAttempts() throws IOException {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        when(storylineService.getGenerationConfig(1L)).thenReturn(createConfig());
        when(novelPipeline.execute(any(), any()))
                .thenThrow(new AiProviderException("qwen", "API error"));

        assertThrows(AiProviderException.class, () ->
                generatorService.generateContentForStorylineWithRetry(storyline, 3));
    }

    @Test
    void generateContentForStorylineWithRetry_usesDefaultConfigWhenNone() throws IOException {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        when(storylineService.getGenerationConfig(1L)).thenReturn(null);
        when(novelPipeline.execute(any(), any()))
                .thenThrow(new AiProviderException("qwen", "API error"));

        Content result = generatorService.generateContentForStorylineWithRetry(storyline, 1);
        assertNull(result);
    }

    // ==================== Generate All Content ====================

    @Test
    void generateAllContent_processesEnabledStorylines() throws IOException {
        Storyline enabled = createStoryline(ContentType.NOVEL);
        when(storylineService.getEnabledStorylines()).thenReturn(List.of(enabled));
        when(storylineService.getGenerationConfig(1L)).thenReturn(createConfig());
        when(novelPipeline.execute(any(), any()))
                .thenThrow(new AiProviderException("qwen", "error"));

        generatorService.generateAllContent();

        verify(storylineService).getEnabledStorylines();
    }

    // ==================== Default Config ====================

    @Test
    void getDefaultGenerationConfig_returnsValidConfig() {
        GenerationConfig config = generatorService.getDefaultGenerationConfig();
        assertNotNull(config.getTextProvider());
        assertNotNull(config.getTextModel());
        assertNotNull(config.getImageProvider());
        assertNotNull(config.getImageModel());
        assertNotNull(config.getTemperature());
        assertNotNull(config.getMaxTokens());
        assertEquals("1024*1024", config.getImageSize());
    }
}
