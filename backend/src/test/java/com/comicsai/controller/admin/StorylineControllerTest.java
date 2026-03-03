package com.comicsai.controller.admin;

import com.comicsai.common.JwtUtil;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.model.dto.GenerationConfigDTO;
import com.comicsai.model.dto.StorylineCreateDTO;
import com.comicsai.model.dto.StorylineQueryDTO;
import com.comicsai.model.dto.StorylineUpdateDTO;
import com.comicsai.model.entity.GenerationConfig;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;
import com.comicsai.model.vo.GenerationConfigVO;
import com.comicsai.model.vo.PageVO;
import com.comicsai.model.vo.StorylineDetailVO;
import com.comicsai.model.vo.StorylineVO;
import com.comicsai.service.StorylineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StorylineController.class)
class StorylineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StorylineService storylineService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== List Storylines ====================

    @Test
    void listStorylines_shouldReturnPagedResults() throws Exception {
        StorylineVO vo = buildStorylineVO(1L, "奇幻冒险", ContentType.NOVEL, StorylineStatus.ENABLED);
        PageVO<StorylineVO> page = new PageVO<>(List.of(vo), 1, 1, 10);

        when(storylineService.getStorylines(any(StorylineQueryDTO.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/storylines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].title").value("奇幻冒险"))
                .andExpect(jsonPath("$.data.records[0].status").value("ENABLED"));
    }

    @Test
    void listStorylines_withFilters_shouldPassQueryParams() throws Exception {
        PageVO<StorylineVO> page = new PageVO<>(Collections.emptyList(), 0, 1, 10);
        when(storylineService.getStorylines(any(StorylineQueryDTO.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/storylines")
                        .param("contentType", "COMIC")
                        .param("status", "ENABLED")
                        .param("genre", "奇幻")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== Create Storyline ====================

    @Test
    void createStoryline_shouldReturnCreatedId() throws Exception {
        when(storylineService.createStoryline(any(StorylineCreateDTO.class))).thenReturn(1L);

        StorylineCreateDTO dto = new StorylineCreateDTO();
        dto.setTitle("奇幻冒险");
        dto.setGenre("奇幻");
        dto.setContentType(ContentType.NOVEL);
        dto.setCharacterSettings("勇者与魔王");
        dto.setWorldview("剑与魔法的世界");
        dto.setPlotOutline("勇者踏上征途");

        mockMvc.perform(post("/api/admin/storylines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void createStoryline_shouldReturn400WhenTitleBlank() throws Exception {
        StorylineCreateDTO dto = new StorylineCreateDTO();
        dto.setTitle("");
        dto.setGenre("奇幻");
        dto.setContentType(ContentType.NOVEL);
        dto.setCharacterSettings("勇者与魔王");
        dto.setWorldview("剑与魔法的世界");
        dto.setPlotOutline("勇者踏上征途");

        mockMvc.perform(post("/api/admin/storylines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ==================== Get Storyline Detail ====================

    @Test
    void getStorylineDetail_shouldReturnDetail() throws Exception {
        StorylineDetailVO detail = buildStorylineDetailVO(1L, "奇幻冒险");
        when(storylineService.getStorylineDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/storylines/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("奇幻冒险"))
                .andExpect(jsonPath("$.data.characterSettings").value("勇者与魔王"));
    }

    @Test
    void getStorylineDetail_shouldReturn404WhenNotFound() throws Exception {
        when(storylineService.getStorylineDetail(999L))
                .thenThrow(new EntityNotFoundException("故事线", 999L));

        mockMvc.perform(get("/api/admin/storylines/999"))
                .andExpect(status().isNotFound());
    }

    // ==================== Update Storyline ====================

    @Test
    void updateStoryline_shouldReturn200() throws Exception {
        doNothing().when(storylineService).updateStoryline(eq(1L), any(StorylineUpdateDTO.class));

        StorylineUpdateDTO dto = new StorylineUpdateDTO();
        dto.setTitle("新标题");
        dto.setGenre("科幻");
        dto.setContentType(ContentType.COMIC);
        dto.setCharacterSettings("太空探险家");
        dto.setWorldview("银河系");
        dto.setPlotOutline("探索未知星球");

        mockMvc.perform(put("/api/admin/storylines/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== Toggle Status ====================

    @Test
    void toggleStorylineStatus_shouldReturn200() throws Exception {
        doNothing().when(storylineService).toggleStorylineStatus(eq(1L), eq("ENABLED"));

        mockMvc.perform(put("/api/admin/storylines/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void toggleStorylineStatus_shouldReturn400ForInvalidStatus() throws Exception {
        doThrow(new BusinessException(400, "无效的状态值"))
                .when(storylineService).toggleStorylineStatus(eq(1L), eq("INVALID"));

        mockMvc.perform(put("/api/admin/storylines/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    // ==================== Configure Generation ====================

    @Test
    void configureGeneration_shouldReturn200() throws Exception {
        doNothing().when(storylineService).configureGeneration(eq(1L), any(GenerationConfigDTO.class));

        GenerationConfigDTO dto = new GenerationConfigDTO();
        dto.setTextProvider("openai");
        dto.setTextModel("gpt-4");
        dto.setImageProvider("dall-e");
        dto.setImageModel("dall-e-3");
        dto.setTemperature(0.8);
        dto.setMaxTokens(4000);

        mockMvc.perform(put("/api/admin/storylines/1/generation-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void configureGeneration_shouldReturn400WhenTextProviderBlank() throws Exception {
        GenerationConfigDTO dto = new GenerationConfigDTO();
        dto.setTextProvider("");
        dto.setTextModel("gpt-4");

        mockMvc.perform(put("/api/admin/storylines/1/generation-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void configureGeneration_shouldReturn404WhenStorylineNotFound() throws Exception {
        doThrow(new EntityNotFoundException("故事线", 999L))
                .when(storylineService).configureGeneration(eq(999L), any(GenerationConfigDTO.class));

        GenerationConfigDTO dto = new GenerationConfigDTO();
        dto.setTextProvider("openai");
        dto.setTextModel("gpt-4");

        mockMvc.perform(put("/api/admin/storylines/999/generation-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // ==================== Get Generation Config ====================

    @Test
    void getGenerationConfig_shouldReturnConfig() throws Exception {
        GenerationConfig config = new GenerationConfig();
        config.setId(1L);
        config.setStorylineId(1L);
        config.setTextProvider("openai");
        config.setTextModel("gpt-4");
        config.setImageProvider("dall-e");
        config.setImageModel("dall-e-3");
        config.setTemperature(0.7);
        config.setMaxTokens(2000);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());

        when(storylineService.getGenerationConfig(1L)).thenReturn(config);

        mockMvc.perform(get("/api/admin/storylines/1/generation-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.textProvider").value("openai"))
                .andExpect(jsonPath("$.data.textModel").value("gpt-4"))
                .andExpect(jsonPath("$.data.imageProvider").value("dall-e"));
    }

    @Test
    void getGenerationConfig_shouldReturnNullDataWhenNotConfigured() throws Exception {
        when(storylineService.getGenerationConfig(1L)).thenReturn(null);

        mockMvc.perform(get("/api/admin/storylines/1/generation-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== Helpers ====================

    private StorylineVO buildStorylineVO(Long id, String title, ContentType contentType, StorylineStatus status) {
        StorylineVO vo = new StorylineVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setGenre("奇幻");
        vo.setContentType(contentType);
        vo.setStatus(status);
        vo.setGeneratedCount(5);
        vo.setCreatedAt(LocalDateTime.now());
        vo.setUpdatedAt(LocalDateTime.now());
        return vo;
    }

    private StorylineDetailVO buildStorylineDetailVO(Long id, String title) {
        StorylineDetailVO vo = new StorylineDetailVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setGenre("奇幻");
        vo.setContentType(ContentType.NOVEL);
        vo.setCharacterSettings("勇者与魔王");
        vo.setWorldview("剑与魔法的世界");
        vo.setPlotOutline("勇者踏上征途");
        vo.setStatus(StorylineStatus.ENABLED);
        vo.setGeneratedCount(0);
        vo.setCreatedAt(LocalDateTime.now());
        vo.setUpdatedAt(LocalDateTime.now());
        return vo;
    }
}
