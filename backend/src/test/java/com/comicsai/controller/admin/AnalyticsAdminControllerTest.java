package com.comicsai.controller.admin;

import com.comicsai.common.JwtUtil;
import com.comicsai.config.JwtInterceptor;
import com.comicsai.model.dto.AnalyticsQueryDTO;
import com.comicsai.model.dto.RechargeQueryDTO;
import com.comicsai.model.dto.TokenCostQueryDTO;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.RechargeAnalyticsVO;
import com.comicsai.model.vo.TokenCostAnalyticsVO;
import com.comicsai.model.vo.UsageAnalyticsVO;
import com.comicsai.service.AnalyticsService;
import com.comicsai.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsAdminController.class)
class AnalyticsAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void getUsageAnalytics_shouldReturnAnalyticsData() throws Exception {
        UsageAnalyticsVO vo = new UsageAnalyticsVO();
        vo.setTotalViews(100L);
        vo.setUniqueViewers(50L);
        vo.setAverageDurationSeconds(45.5);
        vo.setContentUsageList(Collections.emptyList());
        when(analyticsService.getUsageAnalytics(any(AnalyticsQueryDTO.class))).thenReturn(vo);

        mockMvc.perform(get("/api/admin/analytics/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalViews").value(100))
                .andExpect(jsonPath("$.data.uniqueViewers").value(50))
                .andExpect(jsonPath("$.data.averageDurationSeconds").value(45.5));
    }

    @Test
    void getUsageAnalytics_shouldPassDateRangeFilters() throws Exception {
        UsageAnalyticsVO vo = new UsageAnalyticsVO();
        vo.setTotalViews(0L);
        vo.setUniqueViewers(0L);
        vo.setAverageDurationSeconds(0.0);
        vo.setContentUsageList(Collections.emptyList());
        when(analyticsService.getUsageAnalytics(any(AnalyticsQueryDTO.class))).thenReturn(vo);

        mockMvc.perform(get("/api/admin/analytics/usage")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk());

        ArgumentCaptor<AnalyticsQueryDTO> captor = ArgumentCaptor.forClass(AnalyticsQueryDTO.class);
        verify(analyticsService).getUsageAnalytics(captor.capture());
        AnalyticsQueryDTO query = captor.getValue();
        assertEquals(LocalDate.of(2024, 1, 1), query.getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), query.getEndDate());
    }

    @Test
    void getUsageAnalytics_shouldPassContentTypeFilter() throws Exception {
        UsageAnalyticsVO vo = new UsageAnalyticsVO();
        vo.setTotalViews(0L);
        vo.setUniqueViewers(0L);
        vo.setAverageDurationSeconds(0.0);
        vo.setContentUsageList(Collections.emptyList());
        when(analyticsService.getUsageAnalytics(any(AnalyticsQueryDTO.class))).thenReturn(vo);

        mockMvc.perform(get("/api/admin/analytics/usage")
                        .param("contentType", "COMIC"))
                .andExpect(status().isOk());

        ArgumentCaptor<AnalyticsQueryDTO> captor = ArgumentCaptor.forClass(AnalyticsQueryDTO.class);
        verify(analyticsService).getUsageAnalytics(captor.capture());
        assertEquals(ContentType.COMIC, captor.getValue().getContentType());
    }

    @Test
    void getUsageAnalytics_shouldPassPaidStatusFilter() throws Exception {
        UsageAnalyticsVO vo = new UsageAnalyticsVO();
        vo.setTotalViews(0L);
        vo.setUniqueViewers(0L);
        vo.setAverageDurationSeconds(0.0);
        vo.setContentUsageList(Collections.emptyList());
        when(analyticsService.getUsageAnalytics(any(AnalyticsQueryDTO.class))).thenReturn(vo);

        mockMvc.perform(get("/api/admin/analytics/usage")
                        .param("isPaid", "true"))
                .andExpect(status().isOk());

        ArgumentCaptor<AnalyticsQueryDTO> captor = ArgumentCaptor.forClass(AnalyticsQueryDTO.class);
        verify(analyticsService).getUsageAnalytics(captor.capture());
        assertTrue(captor.getValue().getIsPaid());
    }

    // ==================== Token Cost Analytics Tests ====================

    private TokenCostAnalyticsVO buildEmptyTokenCostVO() {
        TokenCostAnalyticsVO vo = new TokenCostAnalyticsVO();
        vo.setTotalInputTokens(0);
        vo.setTotalOutputTokens(0);
        vo.setTotalEstimatedCost(BigDecimal.ZERO);
        vo.setProviderModelCosts(Collections.emptyList());
        vo.setStorylineCosts(Collections.emptyList());
        vo.setDailyTrend(Collections.emptyList());
        return vo;
    }

    @Test
    void getTokenCostAnalytics_shouldReturnData() throws Exception {
        TokenCostAnalyticsVO vo = buildEmptyTokenCostVO();
        vo.setTotalInputTokens(500);
        vo.setTotalOutputTokens(200);
        vo.setTotalEstimatedCost(new BigDecimal("1.50"));
        when(analyticsService.getTokenCostAnalytics(any(TokenCostQueryDTO.class))).thenReturn(vo);

        mockMvc.perform(get("/api/admin/analytics/token-cost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalInputTokens").value(500))
                .andExpect(jsonPath("$.data.totalOutputTokens").value(200))
                .andExpect(jsonPath("$.data.totalEstimatedCost").value(1.50));
    }

    @Test
    void getTokenCostAnalytics_shouldPassDateRangeFilters() throws Exception {
        when(analyticsService.getTokenCostAnalytics(any(TokenCostQueryDTO.class))).thenReturn(buildEmptyTokenCostVO());

        mockMvc.perform(get("/api/admin/analytics/token-cost")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk());

        ArgumentCaptor<TokenCostQueryDTO> captor = ArgumentCaptor.forClass(TokenCostQueryDTO.class);
        verify(analyticsService).getTokenCostAnalytics(captor.capture());
        assertEquals(LocalDate.of(2024, 1, 1), captor.getValue().getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), captor.getValue().getEndDate());
    }

    @Test
    void getTokenCostAnalytics_shouldPassProviderNameFilter() throws Exception {
        when(analyticsService.getTokenCostAnalytics(any(TokenCostQueryDTO.class))).thenReturn(buildEmptyTokenCostVO());

        mockMvc.perform(get("/api/admin/analytics/token-cost")
                        .param("providerName", "OpenAI"))
                .andExpect(status().isOk());

        ArgumentCaptor<TokenCostQueryDTO> captor = ArgumentCaptor.forClass(TokenCostQueryDTO.class);
        verify(analyticsService).getTokenCostAnalytics(captor.capture());
        assertEquals("OpenAI", captor.getValue().getProviderName());
    }

    @Test
    void getTokenCostAnalytics_shouldPassStorylineIdFilter() throws Exception {
        when(analyticsService.getTokenCostAnalytics(any(TokenCostQueryDTO.class))).thenReturn(buildEmptyTokenCostVO());

        mockMvc.perform(get("/api/admin/analytics/token-cost")
                        .param("storylineId", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<TokenCostQueryDTO> captor = ArgumentCaptor.forClass(TokenCostQueryDTO.class);
        verify(analyticsService).getTokenCostAnalytics(captor.capture());
        assertEquals(5L, captor.getValue().getStorylineId());
    }

    // ==================== Recharge Analytics Tests ====================

    private RechargeAnalyticsVO buildEmptyRechargeVO() {
        RechargeAnalyticsVO vo = new RechargeAnalyticsVO();
        vo.setTotalRechargeCount(0L);
        vo.setTotalRechargeAmount(BigDecimal.ZERO);
        vo.setAverageRechargeAmount(BigDecimal.ZERO);
        vo.setRechargeUsers(Collections.emptyList());
        return vo;
    }

    @Test
    void getRechargeAnalytics_shouldReturnData() throws Exception {
        RechargeAnalyticsVO vo = buildEmptyRechargeVO();
        vo.setTotalRechargeCount(10L);
        vo.setTotalRechargeAmount(new BigDecimal("500.00"));
        vo.setAverageRechargeAmount(new BigDecimal("50.00"));
        when(analyticsService.getRechargeAnalytics(any(RechargeQueryDTO.class))).thenReturn(vo);

        mockMvc.perform(get("/api/admin/analytics/recharge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalRechargeCount").value(10))
                .andExpect(jsonPath("$.data.totalRechargeAmount").value(500.00))
                .andExpect(jsonPath("$.data.averageRechargeAmount").value(50.00));
    }

    @Test
    void getRechargeAnalytics_shouldPassDateRangeFilters() throws Exception {
        when(analyticsService.getRechargeAnalytics(any(RechargeQueryDTO.class))).thenReturn(buildEmptyRechargeVO());

        mockMvc.perform(get("/api/admin/analytics/recharge")
                        .param("startDate", "2024-01-01")
                        .param("endDate", "2024-01-31"))
                .andExpect(status().isOk());

        ArgumentCaptor<RechargeQueryDTO> captor = ArgumentCaptor.forClass(RechargeQueryDTO.class);
        verify(analyticsService).getRechargeAnalytics(captor.capture());
        assertEquals(LocalDate.of(2024, 1, 1), captor.getValue().getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), captor.getValue().getEndDate());
    }

    @Test
    void getRechargeAnalytics_shouldWorkWithoutFilters() throws Exception {
        when(analyticsService.getRechargeAnalytics(any(RechargeQueryDTO.class))).thenReturn(buildEmptyRechargeVO());

        mockMvc.perform(get("/api/admin/analytics/recharge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<RechargeQueryDTO> captor = ArgumentCaptor.forClass(RechargeQueryDTO.class);
        verify(analyticsService).getRechargeAnalytics(captor.capture());
        assertNull(captor.getValue().getStartDate());
        assertNull(captor.getValue().getEndDate());
    }
}
