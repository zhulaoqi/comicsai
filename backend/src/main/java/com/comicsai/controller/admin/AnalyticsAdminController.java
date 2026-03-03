package com.comicsai.controller.admin;

import com.comicsai.common.ApiResponse;
import com.comicsai.model.dto.AnalyticsQueryDTO;
import com.comicsai.model.dto.RechargeQueryDTO;
import com.comicsai.model.dto.TokenCostQueryDTO;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.RechargeAnalyticsVO;
import com.comicsai.model.vo.TokenCostAnalyticsVO;
import com.comicsai.model.vo.UsageAnalyticsVO;
import com.comicsai.service.AnalyticsService;
import com.comicsai.service.FileStorageService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsAdminController {

    private final AnalyticsService analyticsService;
    private final FileStorageService fileStorageService;

    public AnalyticsAdminController(AnalyticsService analyticsService, FileStorageService fileStorageService) {
        this.analyticsService = analyticsService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/usage")
    public ApiResponse<UsageAnalyticsVO> getUsageAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) Boolean isPaid) {
        AnalyticsQueryDTO query = new AnalyticsQueryDTO();
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setContentType(contentType);
        query.setIsPaid(isPaid);
        UsageAnalyticsVO result = analyticsService.getUsageAnalytics(query);
        return ApiResponse.success(result);
    }


    @GetMapping("/token-cost")
    public ApiResponse<TokenCostAnalyticsVO> getTokenCostAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String providerName,
            @RequestParam(required = false) Long storylineId) {
        TokenCostQueryDTO query = new TokenCostQueryDTO();
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setProviderName(providerName);
        query.setStorylineId(storylineId);
        TokenCostAnalyticsVO result = analyticsService.getTokenCostAnalytics(query);
        return ApiResponse.success(result);
    }

    @GetMapping("/recharge")
    public ApiResponse<RechargeAnalyticsVO> getRechargeAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        RechargeQueryDTO query = new RechargeQueryDTO();
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        RechargeAnalyticsVO result = analyticsService.getRechargeAnalytics(query);
        return ApiResponse.success(result);
    }

    /** GET /api/admin/analytics/storage-health — checks file storage directories are accessible */
    @GetMapping("/storage-health")
    public ApiResponse<Map<String, Object>> storageHealth() {
        boolean healthy = fileStorageService.isStorageHealthy();
        return ApiResponse.success(Map.of("healthy", healthy, "status", healthy ? "OK" : "DEGRADED"));
    }

}
