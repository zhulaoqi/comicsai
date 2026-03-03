package com.comicsai.controller.reader;

import com.comicsai.common.ApiResponse;
import com.comicsai.common.annotation.RequireAuth;
import com.comicsai.config.JwtInterceptor;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.ContentDetailVO;
import com.comicsai.model.vo.ContentVO;
import com.comicsai.model.vo.PageVO;
import com.comicsai.service.ContentCacheService;
import com.comicsai.service.ContentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reader/contents")
public class ContentController {

    private final ContentCacheService contentCacheService;
    private final ContentService contentService;

    public ContentController(ContentCacheService contentCacheService,
                             ContentService contentService) {
        this.contentCacheService = contentCacheService;
        this.contentService = contentService;
    }

    @GetMapping
    public ApiResponse<PageVO<ContentVO>> listContents(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) ContentType type) {
        PageVO<ContentVO> result = contentCacheService.getPublishedContents(page, size, type);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentDetailVO> getContentDetail(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        contentService.checkContentAccess(id, userId);
        ContentDetailVO detail = contentCacheService.getContentDetail(id);
        return ApiResponse.success(detail);
    }

    @GetMapping("/search")
    public ApiResponse<PageVO<ContentVO>> searchContents(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        PageVO<ContentVO> result = contentCacheService.searchContents(keyword, page, size);
        return ApiResponse.success(result);
    }

    @PostMapping("/{id}/unlock")
    @RequireAuth
    public ApiResponse<Void> unlockContent(
            @PathVariable Long id,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        contentService.unlockContent(id, userId);
        return ApiResponse.success();
    }
}
