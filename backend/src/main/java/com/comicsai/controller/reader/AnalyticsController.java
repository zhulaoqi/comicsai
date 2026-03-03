package com.comicsai.controller.reader;

import com.comicsai.common.ApiResponse;
import com.comicsai.config.JwtInterceptor;
import com.comicsai.model.dto.DurationDTO;
import com.comicsai.model.dto.ViewEventDTO;
import com.comicsai.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reader/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @PostMapping("/view")
    public ApiResponse<Void> recordView(@Valid @RequestBody ViewEventDTO dto,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        analyticsService.recordViewEvent(dto.getContentId(), userId);
        return ApiResponse.success();
    }

    @PostMapping("/duration")
    public ApiResponse<Void> recordDuration(@Valid @RequestBody DurationDTO dto,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        analyticsService.recordReadDuration(dto.getContentId(), userId, dto.getDurationSeconds());
        return ApiResponse.success();
    }
}
