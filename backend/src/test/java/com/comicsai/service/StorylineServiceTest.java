package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.mapper.GenerationConfigMapper;
import com.comicsai.mapper.StorylineMapper;
import com.comicsai.mapper.StorylineVersionMapper;
import com.comicsai.model.dto.GenerationConfigDTO;
import com.comicsai.model.dto.StorylineCreateDTO;
import com.comicsai.model.dto.StorylineQueryDTO;
import com.comicsai.model.dto.StorylineUpdateDTO;
import com.comicsai.model.entity.GenerationConfig;
import com.comicsai.model.entity.Storyline;
import com.comicsai.model.entity.StorylineVersion;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;
import com.comicsai.model.vo.PageVO;
import com.comicsai.model.vo.StorylineDetailVO;
import com.comicsai.model.vo.StorylineVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorylineServiceTest {

    @Mock
    private StorylineMapper storylineMapper;
    @Mock
    private StorylineVersionMapper storylineVersionMapper;
    @Mock
    private GenerationConfigMapper generationConfigMapper;

    private StorylineService storylineService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        storylineService = new StorylineService(storylineMapper, storylineVersionMapper, generationConfigMapper, objectMapper);
    }

    // ==================== Create Storyline ====================

    @Test
    void createStoryline_shouldCreateWithDisabledStatusAndZeroCount() {
        StorylineCreateDTO dto = buildCreateDTO();
        when(storylineMapper.insert(any(Storyline.class))).thenReturn(1);

        storylineService.createStoryline(dto);

        ArgumentCaptor<Storyline> captor = ArgumentCaptor.forClass(Storyline.class);
        verify(storylineMapper).insert(captor.capture());

        Storyline saved = captor.getValue();
        assertEquals("奇幻冒险", saved.getTitle());
        assertEquals("奇幻", saved.getGenre());
        assertEquals(ContentType.NOVEL, saved.getContentType());
        assertEquals("勇者与魔王", saved.getCharacterSettings());
        assertEquals("剑与魔法的世界", saved.getWorldview());
        assertEquals("勇者踏上征途", saved.getPlotOutline());
        assertEquals(StorylineStatus.DISABLED, saved.getStatus());
        assertEquals(0, saved.getGeneratedCount());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void createStoryline_shouldRejectWhenGenreIsBlank() {
        StorylineCreateDTO dto = buildCreateDTO();
        dto.setGenre("  ");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storylineService.createStoryline(dto));
        assertEquals("题材类型不能为空", ex.getMessage());
    }

    @Test
    void createStoryline_shouldRejectWhenCharacterSettingsIsNull() {
        StorylineCreateDTO dto = buildCreateDTO();
        dto.setCharacterSettings(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storylineService.createStoryline(dto));
        assertEquals("角色设定不能为空", ex.getMessage());
    }

    @Test
    void createStoryline_shouldRejectWhenWorldviewIsBlank() {
        StorylineCreateDTO dto = buildCreateDTO();
        dto.setWorldview("");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storylineService.createStoryline(dto));
        assertEquals("世界观描述不能为空", ex.getMessage());
    }

    @Test
    void createStoryline_shouldRejectWhenPlotOutlineIsNull() {
        StorylineCreateDTO dto = buildCreateDTO();
        dto.setPlotOutline(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storylineService.createStoryline(dto));
        assertEquals("剧情大纲不能为空", ex.getMessage());
    }

    // ==================== Update Storyline ====================

    @Test
    void updateStoryline_shouldSaveVersionSnapshotBeforeUpdate() {
        Storyline existing = buildStoryline(1L, "旧标题", StorylineStatus.DISABLED);
        when(storylineMapper.selectById(1L)).thenReturn(existing);
        when(storylineVersionMapper.selectOne(any())).thenReturn(null); // no previous versions
        when(storylineVersionMapper.insert(any(StorylineVersion.class))).thenReturn(1);
        when(storylineMapper.updateById(any(Storyline.class))).thenReturn(1);

        StorylineUpdateDTO dto = buildUpdateDTO();
        storylineService.updateStoryline(1L, dto);

        // Verify version snapshot was saved
        ArgumentCaptor<StorylineVersion> versionCaptor = ArgumentCaptor.forClass(StorylineVersion.class);
        verify(storylineVersionMapper).insert(versionCaptor.capture());

        StorylineVersion savedVersion = versionCaptor.getValue();
        assertEquals(1L, savedVersion.getStorylineId());
        assertEquals(1, savedVersion.getVersionNumber());
        assertNotNull(savedVersion.getSnapshotJson());
        assertTrue(savedVersion.getSnapshotJson().contains("旧标题"));
        assertNotNull(savedVersion.getCreatedAt());

        // Verify storyline was updated
        ArgumentCaptor<Storyline> storylineCaptor = ArgumentCaptor.forClass(Storyline.class);
        verify(storylineMapper).updateById(storylineCaptor.capture());

        Storyline updated = storylineCaptor.getValue();
        assertEquals("新标题", updated.getTitle());
        assertEquals("科幻", updated.getGenre());
    }

    @Test
    void updateStoryline_shouldIncrementVersionNumber() {
        Storyline existing = buildStoryline(1L, "标题", StorylineStatus.DISABLED);
        when(storylineMapper.selectById(1L)).thenReturn(existing);

        StorylineVersion previousVersion = new StorylineVersion();
        previousVersion.setVersionNumber(3);
        when(storylineVersionMapper.selectOne(any())).thenReturn(previousVersion);
        when(storylineVersionMapper.insert(any(StorylineVersion.class))).thenReturn(1);
        when(storylineMapper.updateById(any(Storyline.class))).thenReturn(1);

        StorylineUpdateDTO dto = buildUpdateDTO();
        storylineService.updateStoryline(1L, dto);

        ArgumentCaptor<StorylineVersion> captor = ArgumentCaptor.forClass(StorylineVersion.class);
        verify(storylineVersionMapper).insert(captor.capture());
        assertEquals(4, captor.getValue().getVersionNumber());
    }

    @Test
    void updateStoryline_shouldRejectWhenStorylineNotFound() {
        when(storylineMapper.selectById(999L)).thenReturn(null);

        StorylineUpdateDTO dto = buildUpdateDTO();
        assertThrows(EntityNotFoundException.class,
                () -> storylineService.updateStoryline(999L, dto));
    }

    @Test
    void updateStoryline_shouldRejectWhenTemplateFieldsInvalid() {
        StorylineUpdateDTO dto = buildUpdateDTO();
        dto.setGenre("");

        // Should fail validation before even looking up the storyline
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storylineService.updateStoryline(1L, dto));
        assertEquals("题材类型不能为空", ex.getMessage());
    }

    // ==================== Toggle Status ====================

    @Test
    void toggleStorylineStatus_shouldEnableStoryline() {
        Storyline storyline = buildStoryline(1L, "测试", StorylineStatus.DISABLED);
        when(storylineMapper.selectById(1L)).thenReturn(storyline);
        when(storylineMapper.updateById(any(Storyline.class))).thenReturn(1);

        storylineService.toggleStorylineStatus(1L, "ENABLED");

        ArgumentCaptor<Storyline> captor = ArgumentCaptor.forClass(Storyline.class);
        verify(storylineMapper).updateById(captor.capture());
        assertEquals(StorylineStatus.ENABLED, captor.getValue().getStatus());
    }

    @Test
    void toggleStorylineStatus_shouldDisableStoryline() {
        Storyline storyline = buildStoryline(1L, "测试", StorylineStatus.ENABLED);
        when(storylineMapper.selectById(1L)).thenReturn(storyline);
        when(storylineMapper.updateById(any(Storyline.class))).thenReturn(1);

        storylineService.toggleStorylineStatus(1L, "DISABLED");

        ArgumentCaptor<Storyline> captor = ArgumentCaptor.forClass(Storyline.class);
        verify(storylineMapper).updateById(captor.capture());
        assertEquals(StorylineStatus.DISABLED, captor.getValue().getStatus());
    }

    @Test
    void toggleStorylineStatus_shouldBeCaseInsensitive() {
        Storyline storyline = buildStoryline(1L, "测试", StorylineStatus.DISABLED);
        when(storylineMapper.selectById(1L)).thenReturn(storyline);
        when(storylineMapper.updateById(any(Storyline.class))).thenReturn(1);

        storylineService.toggleStorylineStatus(1L, "enabled");

        ArgumentCaptor<Storyline> captor = ArgumentCaptor.forClass(Storyline.class);
        verify(storylineMapper).updateById(captor.capture());
        assertEquals(StorylineStatus.ENABLED, captor.getValue().getStatus());
    }

    @Test
    void toggleStorylineStatus_shouldRejectInvalidStatus() {
        Storyline storyline = buildStoryline(1L, "测试", StorylineStatus.DISABLED);
        when(storylineMapper.selectById(1L)).thenReturn(storyline);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storylineService.toggleStorylineStatus(1L, "INVALID"));
        assertTrue(ex.getMessage().contains("无效的状态值"));
    }

    @Test
    void toggleStorylineStatus_shouldRejectWhenStorylineNotFound() {
        when(storylineMapper.selectById(999L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> storylineService.toggleStorylineStatus(999L, "ENABLED"));
    }

    // ==================== Get Detail ====================

    @Test
    void getStorylineDetail_shouldReturnFullDetail() {
        Storyline storyline = buildStoryline(1L, "奇幻冒险", StorylineStatus.ENABLED);
        when(storylineMapper.selectById(1L)).thenReturn(storyline);
        when(generationConfigMapper.selectOne(any())).thenReturn(null);

        StorylineDetailVO detail = storylineService.getStorylineDetail(1L);

        assertEquals(1L, detail.getId());
        assertEquals("奇幻冒险", detail.getTitle());
        assertEquals("奇幻", detail.getGenre());
        assertEquals(ContentType.NOVEL, detail.getContentType());
        assertEquals("勇者与魔王", detail.getCharacterSettings());
        assertEquals("剑与魔法的世界", detail.getWorldview());
        assertEquals("勇者踏上征途", detail.getPlotOutline());
        assertEquals(StorylineStatus.ENABLED, detail.getStatus());
        assertNull(detail.getGenerationConfig());
    }

    @Test
    void getStorylineDetail_shouldIncludeGenerationConfig() {
        Storyline storyline = buildStoryline(1L, "奇幻冒险", StorylineStatus.ENABLED);
        when(storylineMapper.selectById(1L)).thenReturn(storyline);

        GenerationConfig config = buildGenerationConfig(1L, 1L);
        when(generationConfigMapper.selectOne(any())).thenReturn(config);

        StorylineDetailVO detail = storylineService.getStorylineDetail(1L);

        assertNotNull(detail.getGenerationConfig());
        assertEquals("openai", detail.getGenerationConfig().getTextProvider());
        assertEquals("gpt-4", detail.getGenerationConfig().getTextModel());
    }

    @Test
    void getStorylineDetail_shouldThrowWhenNotFound() {
        when(storylineMapper.selectById(999L)).thenReturn(null);

        assertThrows(EntityNotFoundException.class,
                () -> storylineService.getStorylineDetail(999L));
    }

    // ==================== List Storylines ====================

    @Test
    @SuppressWarnings("unchecked")
    void getStorylines_shouldReturnPagedResults() {
        Storyline s1 = buildStoryline(1L, "故事线1", StorylineStatus.ENABLED);
        Storyline s2 = buildStoryline(2L, "故事线2", StorylineStatus.DISABLED);

        Page<Storyline> mockPage = new Page<>(1, 10, 2);
        mockPage.setRecords(Arrays.asList(s1, s2));

        when(storylineMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        StorylineQueryDTO query = new StorylineQueryDTO();
        PageVO<StorylineVO> result = storylineService.getStorylines(query);

        assertEquals(2, result.getRecords().size());
        assertEquals(2, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
        assertFalse(result.isHasNext());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getStorylines_shouldFilterByContentType() {
        Page<Storyline> mockPage = new Page<>(1, 10, 0);
        mockPage.setRecords(Collections.emptyList());

        when(storylineMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        StorylineQueryDTO query = new StorylineQueryDTO();
        query.setContentType(ContentType.COMIC);
        storylineService.getStorylines(query);

        verify(storylineMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getStorylines_shouldDefaultToPage1Size10() {
        Page<Storyline> mockPage = new Page<>(1, 10, 0);
        mockPage.setRecords(Collections.emptyList());

        when(storylineMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        StorylineQueryDTO query = new StorylineQueryDTO();
        query.setPage(null);
        query.setSize(null);
        PageVO<StorylineVO> result = storylineService.getStorylines(query);

        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
    }

    // ==================== Version Snapshot JSON Content ====================

    @Test
    void updateStoryline_snapshotShouldContainAllFields() throws Exception {
        Storyline existing = buildStoryline(1L, "原始标题", StorylineStatus.ENABLED);
        existing.setLatestChapterSummary("上一章摘要");
        existing.setGeneratedCount(5);

        when(storylineMapper.selectById(1L)).thenReturn(existing);
        when(storylineVersionMapper.selectOne(any())).thenReturn(null);
        when(storylineVersionMapper.insert(any(StorylineVersion.class))).thenReturn(1);
        when(storylineMapper.updateById(any(Storyline.class))).thenReturn(1);

        StorylineUpdateDTO dto = buildUpdateDTO();
        storylineService.updateStoryline(1L, dto);

        ArgumentCaptor<StorylineVersion> captor = ArgumentCaptor.forClass(StorylineVersion.class);
        verify(storylineVersionMapper).insert(captor.capture());

        String json = captor.getValue().getSnapshotJson();
        // Snapshot should contain the OLD data (before update)
        assertTrue(json.contains("原始标题"));
        assertTrue(json.contains("奇幻"));
        assertTrue(json.contains("勇者与魔王"));
        assertTrue(json.contains("剑与魔法的世界"));
        assertTrue(json.contains("勇者踏上征途"));
    }

    // ==================== Generation Config ====================

    @Test
    void configureGeneration_shouldCreateNewConfig() {
        Storyline storyline = buildStoryline(1L, "测试", StorylineStatus.DISABLED);
        when(storylineMapper.selectById(1L)).thenReturn(storyline);
        when(generationConfigMapper.selectOne(any())).thenReturn(null);
        when(generationConfigMapper.insert(any(GenerationConfig.class))).thenReturn(1);

        GenerationConfigDTO dto = buildGenerationConfigDTO();
        storylineService.configureGeneration(1L, dto);

        ArgumentCaptor<GenerationConfig> captor = ArgumentCaptor.forClass(GenerationConfig.class);
        verify(generationConfigMapper).insert(captor.capture());

        GenerationConfig saved = captor.getValue();
        assertEquals(1L, saved.getStorylineId());
        assertEquals("openai", saved.getTextProvider());
        assertEquals("gpt-4", saved.getTextModel());
        assertEquals("dall-e", saved.getImageProvider());
        assertEquals("dall-e-3", saved.getImageModel());
        assertEquals(0.8, saved.getTemperature());
        assertEquals(4000, saved.getMaxTokens());
        assertEquals("anime", saved.getImageStyle());
        assertEquals("1024x1024", saved.getImageSize());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void configureGeneration_shouldUpdateExistingConfig() {
        Storyline storyline = buildStoryline(1L, "测试", StorylineStatus.DISABLED);
        when(storylineMapper.selectById(1L)).thenReturn(storyline);

        GenerationConfig existing = buildGenerationConfig(10L, 1L);
        when(generationConfigMapper.selectOne(any())).thenReturn(existing);
        when(generationConfigMapper.updateById(any(GenerationConfig.class))).thenReturn(1);

        GenerationConfigDTO dto = buildGenerationConfigDTO();
        dto.setTextProvider("qwen");
        dto.setTextModel("qwen-max");
        storylineService.configureGeneration(1L, dto);

        ArgumentCaptor<GenerationConfig> captor = ArgumentCaptor.forClass(GenerationConfig.class);
        verify(generationConfigMapper).updateById(captor.capture());
        verify(generationConfigMapper, never()).insert(any());

        GenerationConfig updated = captor.getValue();
        assertEquals(10L, updated.getId());
        assertEquals("qwen", updated.getTextProvider());
        assertEquals("qwen-max", updated.getTextModel());
    }

    @Test
    void configureGeneration_shouldUseDefaultTemperatureAndMaxTokens() {
        Storyline storyline = buildStoryline(1L, "测试", StorylineStatus.DISABLED);
        when(storylineMapper.selectById(1L)).thenReturn(storyline);
        when(generationConfigMapper.selectOne(any())).thenReturn(null);
        when(generationConfigMapper.insert(any(GenerationConfig.class))).thenReturn(1);

        GenerationConfigDTO dto = buildGenerationConfigDTO();
        dto.setTemperature(null);
        dto.setMaxTokens(null);
        storylineService.configureGeneration(1L, dto);

        ArgumentCaptor<GenerationConfig> captor = ArgumentCaptor.forClass(GenerationConfig.class);
        verify(generationConfigMapper).insert(captor.capture());

        assertEquals(0.7, captor.getValue().getTemperature());
        assertEquals(2000, captor.getValue().getMaxTokens());
    }

    @Test
    void configureGeneration_shouldThrowWhenStorylineNotFound() {
        when(storylineMapper.selectById(999L)).thenReturn(null);

        GenerationConfigDTO dto = buildGenerationConfigDTO();
        assertThrows(EntityNotFoundException.class,
                () -> storylineService.configureGeneration(999L, dto));
    }

    @Test
    void getGenerationConfig_shouldReturnConfigForStoryline() {
        GenerationConfig config = buildGenerationConfig(1L, 1L);
        when(generationConfigMapper.selectOne(any())).thenReturn(config);

        GenerationConfig result = storylineService.getGenerationConfig(1L);

        assertNotNull(result);
        assertEquals("openai", result.getTextProvider());
        assertEquals("gpt-4", result.getTextModel());
    }

    @Test
    void getGenerationConfig_shouldReturnNullWhenNotConfigured() {
        when(generationConfigMapper.selectOne(any())).thenReturn(null);

        GenerationConfig result = storylineService.getGenerationConfig(1L);

        assertNull(result);
    }

    // ==================== Helpers ====================

    private StorylineCreateDTO buildCreateDTO() {
        StorylineCreateDTO dto = new StorylineCreateDTO();
        dto.setTitle("奇幻冒险");
        dto.setGenre("奇幻");
        dto.setContentType(ContentType.NOVEL);
        dto.setCharacterSettings("勇者与魔王");
        dto.setWorldview("剑与魔法的世界");
        dto.setPlotOutline("勇者踏上征途");
        return dto;
    }

    private StorylineUpdateDTO buildUpdateDTO() {
        StorylineUpdateDTO dto = new StorylineUpdateDTO();
        dto.setTitle("新标题");
        dto.setGenre("科幻");
        dto.setContentType(ContentType.COMIC);
        dto.setCharacterSettings("太空探险家");
        dto.setWorldview("银河系");
        dto.setPlotOutline("探索未知星球");
        return dto;
    }

    private Storyline buildStoryline(Long id, String title, StorylineStatus status) {
        Storyline storyline = new Storyline();
        storyline.setId(id);
        storyline.setTitle(title);
        storyline.setGenre("奇幻");
        storyline.setContentType(ContentType.NOVEL);
        storyline.setCharacterSettings("勇者与魔王");
        storyline.setWorldview("剑与魔法的世界");
        storyline.setPlotOutline("勇者踏上征途");
        storyline.setStatus(status);
        storyline.setGeneratedCount(0);
        storyline.setCreatedAt(LocalDateTime.now());
        storyline.setUpdatedAt(LocalDateTime.now());
        return storyline;
    }

    private GenerationConfigDTO buildGenerationConfigDTO() {
        GenerationConfigDTO dto = new GenerationConfigDTO();
        dto.setTextProvider("openai");
        dto.setTextModel("gpt-4");
        dto.setImageProvider("dall-e");
        dto.setImageModel("dall-e-3");
        dto.setTemperature(0.8);
        dto.setMaxTokens(4000);
        dto.setImageStyle("anime");
        dto.setImageSize("1024x1024");
        return dto;
    }

    private GenerationConfig buildGenerationConfig(Long id, Long storylineId) {
        GenerationConfig config = new GenerationConfig();
        config.setId(id);
        config.setStorylineId(storylineId);
        config.setTextProvider("openai");
        config.setTextModel("gpt-4");
        config.setImageProvider("dall-e");
        config.setImageModel("dall-e-3");
        config.setTemperature(0.7);
        config.setMaxTokens(2000);
        config.setImageStyle("anime");
        config.setImageSize("1024x1024");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }
}
