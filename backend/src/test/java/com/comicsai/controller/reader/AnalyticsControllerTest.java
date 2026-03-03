package com.comicsai.controller.reader;

import com.comicsai.common.JwtUtil;
import com.comicsai.config.JwtInterceptor;
import com.comicsai.service.AnalyticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void recordView_shouldCallServiceAndReturn200() throws Exception {
        Map<String, Object> body = Map.of("contentId", 1);

        mockMvc.perform(post("/api/reader/analytics/view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(analyticsService).recordViewEvent(1L, null);
    }

    @Test
    void recordView_shouldPassUserIdFromRequest() throws Exception {
        Map<String, Object> body = Map.of("contentId", 5);

        mockMvc.perform(post("/api/reader/analytics/view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .requestAttr(JwtInterceptor.USER_ID_ATTR, 100L)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(analyticsService).recordViewEvent(5L, 100L);
    }

    @Test
    void recordView_shouldReturn400WhenContentIdMissing() throws Exception {
        mockMvc.perform(post("/api/reader/analytics/view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordDuration_shouldCallServiceAndReturn200() throws Exception {
        Map<String, Object> body = Map.of("contentId", 1, "durationSeconds", 120);

        mockMvc.perform(post("/api/reader/analytics/duration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(analyticsService).recordReadDuration(1L, null, 120);
    }

    @Test
    void recordDuration_shouldReturn400WhenDurationMissing() throws Exception {
        Map<String, Object> body = Map.of("contentId", 1);

        mockMvc.perform(post("/api/reader/analytics/duration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordDuration_shouldReturn400WhenDurationIsNegative() throws Exception {
        Map<String, Object> body = Map.of("contentId", 1, "durationSeconds", -5);

        mockMvc.perform(post("/api/reader/analytics/duration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
