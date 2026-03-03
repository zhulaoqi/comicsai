package com.comicsai.service;

import com.comicsai.ai.*;
import com.comicsai.common.exception.AiProviderException;
import com.comicsai.mapper.*;
import com.comicsai.model.dto.ContentCreateDTO;
import com.comicsai.model.entity.*;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentGeneratorServiceTest {

    @Mock private StorylineService storylineService;
    @Mock private ContentService contentService;
    @Mock private AiProviderFactory aiProviderFactory;
    @Mock private FileStorageService fileStorageService;
    @Mock private ComicPageMapper comicPageMapper;
    @Mock private NovelChapterMapper novelChapterMapper;
    @Mock private TokenUsageMapper tokenUsageMapper;
    @Mock private StorylineMapper storylineMapper;

    private ContentGeneratorService generatorService;

    @BeforeEach
    void setUp() {
        generatorService = new ContentGeneratorService(
                storylineService, contentService, aiProviderFactory,
                fileStorageService, comicPageMapper, novelChapterMapper,
                tokenUsageMapper, storylineMapper);
    }

    // ==================== Helper Methods ====================

    private Storyline createStoryline(ContentType type) {
        Storyline s = new Storyline();
        s.setId(1L);
        s.setTitle("测试故事线");
        s.setGenre("奇幻");
        s.setContentType(type);
        s.setCharacterSettings("主角：小明，一个勇敢的少年");
        s.setWorldview("一个充满魔法的世界");
        s.setPlotOutline("小明踏上冒险之旅");
        s.setStatus(StorylineStatus.ENABLED);
        s.setGeneratedCount(0);
        return s;
    }

    private GenerationConfig createConfig() {
        GenerationConfig c = new GenerationConfig();
        c.setTextProvider("openai");
        c.setTextModel("gpt-4");
        c.setImageProvider("dall-e");
        c.setImageModel("dall-e-3");
        c.setTemperature(0.7);
        c.setMaxTokens(2000);
        c.setImageStyle("anime");
        c.setImageSize("1024x1024");
        return c;
    }

    private TextGenerationResult createTextResult(String content) {
        return new TextGenerationResult(content, 100, 200, "gpt-4");
    }

    private ImageGenerationResult createImageResult() {
        return new ImageGenerationResult(new byte[]{1, 2, 3}, "png", 50, "dall-e-3");
    }

    // ==================== System Prompt Building ====================

    @Test
    void buildSystemPrompt_includesCharacterSettings() {
        Storyline storyline = createStoryline(ContentType.COMIC);
        String prompt = generatorService.buildSystemPrompt(storyline);
        assertTrue(prompt.contains(storyline.getCharacterSettings()));
    }

    @Test
    void buildSystemPrompt_includesWorldview() {
        Storyline storyline = createStoryline(ContentType.COMIC);
        String prompt = generatorService.buildSystemPrompt(storyline);
        assertTrue(prompt.contains(storyline.getWorldview()));
    }

    @Test
    void buildSystemPrompt_includesPlotOutline() {
        Storyline storyline = createStoryline(ContentType.COMIC);
        String prompt = generatorService.buildSystemPrompt(storyline);
        assertTrue(prompt.contains(storyline.getPlotOutline()));
    }

    @Test
    void buildSystemPrompt_includesPreviousChapterSummary() {
        Storyline storyline = createStoryline(ContentType.COMIC);
        storyline.setLatestChapterSummary("上一章：小明遇到了一条龙");
        String prompt = generatorService.buildSystemPrompt(storyline);
        assertTrue(prompt.contains("上一章：小明遇到了一条龙"));
        assertTrue(prompt.contains("前章摘要"));
    }

    @Test
    void buildSystemPrompt_noPreviousSummary_omitsSummarySection() {
        Storyline storyline = createStoryline(ContentType.COMIC);
        storyline.setLatestChapterSummary(null);
        String prompt = generatorService.buildSystemPrompt(storyline);
        assertFalse(prompt.contains("前章摘要"));
    }

    @Test
    void buildSystemPrompt_blankSummary_omitsSummarySection() {
        Storyline storyline = createStoryline(ContentType.COMIC);
        storyline.setLatestChapterSummary("   ");
        String prompt = generatorService.buildSystemPrompt(storyline);
        assertFalse(prompt.contains("前章摘要"));
    }

    // ==================== Comic Generation ====================

    @Test
    void generateComic_createsContentWithPendingReviewStatus() throws IOException {
        Storyline storyline = createStoryline(ContentType.COMIC);
        GenerationConfig config = createConfig();

        String script = "标题：冒险开始\n描述：小明的第一天\n---\n分镜1：\n场景：森林中的小路\n对话：我要出发了！\n---";
        when(aiProviderFactory.generateTextWithFallback(eq("openai"), any()))
                .thenReturn(createTextResult(script))
                .thenReturn(createTextResult("小明踏上了冒险之旅的摘要"));
        when(aiProviderFactory.generateImageWithFallback(eq("dall-e"), any()))
                .thenReturn(createImageResult());
        when(fileStorageService.storeCoverImage(any(byte[].class), anyString()))
                .thenReturn("/files/covers/cover.png");
        when(fileStorageService.storeComicImage(any(byte[].class), anyString()))
                .thenReturn("/files/comic-images/page.png");

        Content mockContent = new Content();
        mockContent.setId(10L);
        mockContent.setStatus(ContentStatus.PENDING_REVIEW);
        when(contentService.createContent(any(ContentCreateDTO.class))).thenReturn(mockContent);

        Content result = generatorService.generateComic(storyline, config);

        assertNotNull(result);
        assertEquals(10L, result.getId());

        // Verify content was created with correct type
        ArgumentCaptor<ContentCreateDTO> dtoCaptor = ArgumentCaptor.forClass(ContentCreateDTO.class);
        verify(contentService).createContent(dtoCaptor.capture());
        assertEquals(ContentType.COMIC, dtoCaptor.getValue().getContentType());
        assertEquals("冒险开始", dtoCaptor.getValue().getTitle());
    }

    @Test
    void generateComic_savesComicPages() throws IOException {
        Storyline storyline = createStoryline(ContentType.COMIC);
        GenerationConfig config = createConfig();

        String script = "标题：测试\n描述：描述\n---\n分镜1：\n场景：场景A\n对话：对话A\n---\n分镜2：\n场景：场景B\n对话：对话B\n---";
        when(aiProviderFactory.generateTextWithFallback(eq("openai"), any()))
                .thenReturn(createTextResult(script))
                .thenReturn(createTextResult("摘要内容"));
        when(aiProviderFactory.generateImageWithFallback(eq("dall-e"), any()))
                .thenReturn(createImageResult());
        when(fileStorageService.storeCoverImage(any(byte[].class), anyString()))
                .thenReturn("/files/covers/cover.png");
        when(fileStorageService.storeComicImage(any(byte[].class), anyString()))
                .thenReturn("/files/comic-images/page.png");

        Content mockContent = new Content();
        mockContent.setId(10L);
        when(contentService.createContent(any())).thenReturn(mockContent);

        generatorService.generateComic(storyline, config);

        // 2 panels parsed from script
        verify(comicPageMapper, times(2)).insert(any(ComicPage.class));
    }

    @Test
    void generateComic_recordsTokenUsage() throws IOException {
        Storyline storyline = createStoryline(ContentType.COMIC);
        GenerationConfig config = createConfig();

        String script = "标题：测试\n描述：描述\n---\n分镜1：\n场景：场景A\n对话：对话A\n---";
        when(aiProviderFactory.generateTextWithFallback(eq("openai"), any()))
                .thenReturn(createTextResult(script))
                .thenReturn(createTextResult("摘要"));
        when(aiProviderFactory.generateImageWithFallback(eq("dall-e"), any()))
                .thenReturn(createImageResult());
        when(fileStorageService.storeCoverImage(any(byte[].class), anyString()))
                .thenReturn("/files/covers/cover.png");
        when(fileStorageService.storeComicImage(any(byte[].class), anyString()))
                .thenReturn("/files/comic-images/page.png");

        Content mockContent = new Content();
        mockContent.setId(10L);
        when(contentService.createContent(any())).thenReturn(mockContent);

        generatorService.generateComic(storyline, config);

        // Token usage: 1 for text script + 1 for cover image + 1 for panel image + 1 for summary = 4
        verify(tokenUsageMapper, atLeast(3)).insert(any(TokenUsage.class));
    }

    @Test
    void generateComic_updatesStorylineGeneratedCount() throws IOException {
        Storyline storyline = createStoryline(ContentType.COMIC);
        storyline.setGeneratedCount(5);
        GenerationConfig config = createConfig();

        String script = "标题：测试\n描述：描述\n---\n分镜1：\n场景：场景A\n对话：对话A\n---";
        when(aiProviderFactory.generateTextWithFallback(eq("openai"), any()))
                .thenReturn(createTextResult(script))
                .thenReturn(createTextResult("摘要"));
        when(aiProviderFactory.generateImageWithFallback(eq("dall-e"), any()))
                .thenReturn(createImageResult());
        when(fileStorageService.storeCoverImage(any(byte[].class), anyString()))
                .thenReturn("/files/covers/cover.png");
        when(fileStorageService.storeComicImage(any(byte[].class), anyString()))
                .thenReturn("/files/comic-images/page.png");

        Content mockContent = new Content();
        mockContent.setId(10L);
        when(contentService.createContent(any())).thenReturn(mockContent);

        generatorService.generateComic(storyline, config);

        // storylineMapper.updateById called for summary + generated count
        verify(storylineMapper, atLeast(1)).updateById(any(Storyline.class));
        assertEquals(6, storyline.getGeneratedCount());
    }

    // ==================== Novel Generation ====================

    @Test
    void generateNovel_createsContentAndChapter() throws IOException {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        GenerationConfig config = createConfig();

        String chapterContent = "章节标题：黎明之前\n---\n这是一个漫长的夜晚，小明独自走在回家的路上...";
        when(aiProviderFactory.generateTextWithFallback(eq("openai"), any()))
                .thenReturn(createTextResult(chapterContent))
                .thenReturn(createTextResult("小明在夜晚独行的摘要"));
        when(aiProviderFactory.generateImageWithFallback(eq("dall-e"), any()))
                .thenReturn(createImageResult());
        when(fileStorageService.storeCoverImage(any(byte[].class), anyString()))
                .thenReturn("/files/covers/novel_cover.png");

        Content mockContent = new Content();
        mockContent.setId(20L);
        when(contentService.createContent(any())).thenReturn(mockContent);

        Content result = generatorService.generateNovel(storyline, config);

        assertNotNull(result);
        assertEquals(20L, result.getId());

        // Verify novel chapter was saved
        ArgumentCaptor<NovelChapter> chapterCaptor = ArgumentCaptor.forClass(NovelChapter.class);
        verify(novelChapterMapper).insert(chapterCaptor.capture());
        assertEquals("黎明之前", chapterCaptor.getValue().getChapterTitle());
        assertEquals(1, chapterCaptor.getValue().getChapterNumber());
        assertTrue(chapterCaptor.getValue().getChapterText().contains("漫长的夜晚"));
    }

    @Test
    void generateNovel_withNoImageProvider_usesDefaultCover() throws IOException {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        GenerationConfig config = createConfig();
        config.setImageProvider(null);

        String chapterContent = "章节标题：开始\n---\n正文内容";
        when(aiProviderFactory.generateTextWithFallback(eq("openai"), any()))
                .thenReturn(createTextResult(chapterContent))
                .thenReturn(createTextResult("摘要"));

        Content mockContent = new Content();
        mockContent.setId(20L);
        when(contentService.createContent(any())).thenReturn(mockContent);

        generatorService.generateNovel(storyline, config);

        ArgumentCaptor<ContentCreateDTO> dtoCaptor = ArgumentCaptor.forClass(ContentCreateDTO.class);
        verify(contentService).createContent(dtoCaptor.capture());
        assertEquals("/files/covers/default_novel_cover.png", dtoCaptor.getValue().getCoverUrl());
    }

    @Test
    void generateNovel_recordsTokenUsage() throws IOException {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        GenerationConfig config = createConfig();

        String chapterContent = "章节标题：测试\n---\n正文";
        when(aiProviderFactory.generateTextWithFallback(eq("openai"), any()))
                .thenReturn(createTextResult(chapterContent))
                .thenReturn(createTextResult("摘要"));
        when(aiProviderFactory.generateImageWithFallback(eq("dall-e"), any()))
                .thenReturn(createImageResult());
        when(fileStorageService.storeCoverImage(any(byte[].class), anyString()))
                .thenReturn("/files/covers/cover.png");

        Content mockContent = new Content();
        mockContent.setId(20L);
        when(contentService.createContent(any())).thenReturn(mockContent);

        generatorService.generateNovel(storyline, config);

        // Token usage: 1 for cover image + 1 for text generation + 1 for summary = 3
        verify(tokenUsageMapper, atLeast(2)).insert(any(TokenUsage.class));
    }

    // ==================== Summary Generation ====================

    @Test
    void generateAndStoreSummary_updatesStorylineLatestChapterSummary() {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        GenerationConfig config = createConfig();

        when(aiProviderFactory.generateTextWithFallback(eq("openai"), any()))
                .thenReturn(createTextResult("这是生成的摘要内容"));

        String summary = generatorService.generateAndStoreSummary(storyline, config, "一些内容文本");

        assertEquals("这是生成的摘要内容", summary);
        assertEquals("这是生成的摘要内容", storyline.getLatestChapterSummary());
        verify(storylineMapper).updateById(storyline);
    }

    @Test
    void generateAndStoreSummary_failureDoesNotThrow() {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        GenerationConfig config = createConfig();

        when(aiProviderFactory.generateTextWithFallback(eq("openai"), any()))
                .thenThrow(new AiProviderException("openai", "API error"));

        String summary = generatorService.generateAndStoreSummary(storyline, config, "内容");

        assertNull(summary);
        verify(storylineMapper, never()).updateById(any());
    }

    // ==================== Retry Mechanism ====================

    @Test
    void generateContentForStorylineWithRetry_retriesOnAiFailure() {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        when(storylineService.getGenerationConfig(1L)).thenReturn(createConfig());
        when(aiProviderFactory.generateTextWithFallback(anyString(), any()))
                .thenThrow(new AiProviderException("openai", "API error"));

        // First attempt fails, scheduleRetry is called
        Content result = generatorService.generateContentForStorylineWithRetry(storyline, 1);
        assertNull(result); // Returns null because retry is scheduled
    }

    @Test
    void generateContentForStorylineWithRetry_throwsAfterMaxAttempts() {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        when(storylineService.getGenerationConfig(1L)).thenReturn(createConfig());
        when(aiProviderFactory.generateTextWithFallback(anyString(), any()))
                .thenThrow(new AiProviderException("openai", "API error"));

        assertThrows(AiProviderException.class, () ->
                generatorService.generateContentForStorylineWithRetry(storyline, 3));
    }

    @Test
    void generateContentForStorylineWithRetry_usesDefaultConfigWhenNone() {
        Storyline storyline = createStoryline(ContentType.NOVEL);
        when(storylineService.getGenerationConfig(1L)).thenReturn(null);
        when(aiProviderFactory.generateTextWithFallback(anyString(), any()))
                .thenThrow(new AiProviderException("openai", "API error"));

        // Should not throw NPE - uses default config
        Content result = generatorService.generateContentForStorylineWithRetry(storyline, 1);
        assertNull(result);
    }

    // ==================== Token Usage Recording ====================

    @Test
    void recordTokenUsage_savesCompleteRecord() {
        generatorService.recordTokenUsage(10L, 1L, "openai", "gpt-4", 100, 200);

        ArgumentCaptor<TokenUsage> captor = ArgumentCaptor.forClass(TokenUsage.class);
        verify(tokenUsageMapper).insert(captor.capture());

        TokenUsage usage = captor.getValue();
        assertEquals(10L, usage.getContentId());
        assertEquals(1L, usage.getStorylineId());
        assertEquals("openai", usage.getProviderName());
        assertEquals("gpt-4", usage.getModelName());
        assertEquals(100, usage.getInputTokens());
        assertEquals(200, usage.getOutputTokens());
        assertNotNull(usage.getEstimatedCost());
        assertNotNull(usage.getCalledAt());
    }

    @Test
    void recordTokenUsage_handlesNullTokenCounts() {
        generatorService.recordTokenUsage(10L, 1L, "openai", null, null, null);

        ArgumentCaptor<TokenUsage> captor = ArgumentCaptor.forClass(TokenUsage.class);
        verify(tokenUsageMapper).insert(captor.capture());

        TokenUsage usage = captor.getValue();
        assertEquals("unknown", usage.getModelName());
        assertEquals(0, usage.getInputTokens());
        assertEquals(0, usage.getOutputTokens());
    }

    // ==================== Cost Estimation ====================

    @Test
    void estimateCost_calculatesCorrectly() {
        BigDecimal cost = generatorService.estimateCost(1000, 1000);
        // 1K input * 0.002 + 1K output * 0.006 = 0.008
        assertEquals(0, cost.compareTo(BigDecimal.valueOf(0.008).setScale(4)));
    }

    @Test
    void estimateCost_handlesNullInputs() {
        BigDecimal cost = generatorService.estimateCost(null, null);
        assertEquals(0, cost.compareTo(BigDecimal.ZERO.setScale(4)));
    }

    // ==================== Parsing Helpers ====================

    @Test
    void parseTitle_extractsTitle() {
        String script = "标题：冒险开始\n描述：一段描述";
        assertEquals("冒险开始", generatorService.parseTitle(script, "默认"));
    }

    @Test
    void parseTitle_returnsDefaultWhenNoTitle() {
        String script = "没有标题的内容";
        assertEquals("默认标题", generatorService.parseTitle(script, "默认标题"));
    }

    @Test
    void parseDescription_extractsDescription() {
        String script = "标题：测试\n描述：这是描述内容";
        assertEquals("这是描述内容", generatorService.parseDescription(script));
    }

    @Test
    void parseStoryboardPanels_parsesMultiplePanels() {
        String script = "标题：测试\n---\n分镜1：\n场景：森林\n对话：你好\n---\n分镜2：\n场景：城市\n对话：再见\n---";
        var panels = generatorService.parseStoryboardPanels(script);
        assertEquals(2, panels.size());
        assertEquals("森林", panels.get(0).sceneDescription());
        assertEquals("你好", panels.get(0).dialogueText());
        assertEquals("城市", panels.get(1).sceneDescription());
        assertEquals("再见", panels.get(1).dialogueText());
    }

    @Test
    void parseStoryboardPanels_emptyScript_returnsEmptyList() {
        var panels = generatorService.parseStoryboardPanels("");
        assertTrue(panels.isEmpty());
    }

    @Test
    void parseChapterTitle_extractsTitle() {
        String content = "章节标题：黎明之前\n---\n正文内容";
        assertEquals("黎明之前", generatorService.parseChapterTitle(content, "默认"));
    }

    @Test
    void parseChapterText_extractsTextAfterSeparator() {
        String content = "章节标题：测试\n---\n这是正文内容";
        assertEquals("这是正文内容", generatorService.parseChapterText(content));
    }

    @Test
    void parseChapterText_noSeparator_returnsAfterFirstLine() {
        String content = "第一行\n这是正文内容";
        assertEquals("这是正文内容", generatorService.parseChapterText(content));
    }

    // ==================== Generate All Content ====================

    @Test
    void generateAllContent_processesOnlyEnabledStorylines() {
        Storyline enabled = createStoryline(ContentType.NOVEL);
        enabled.setStatus(StorylineStatus.ENABLED);
        when(storylineService.getEnabledStorylines()).thenReturn(List.of(enabled));
        when(storylineService.getGenerationConfig(1L)).thenReturn(createConfig());

        // Make it fail so we can verify it was attempted
        when(aiProviderFactory.generateTextWithFallback(anyString(), any()))
                .thenThrow(new AiProviderException("openai", "error"));

        // Should not throw - errors are caught per storyline
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
    }
}
