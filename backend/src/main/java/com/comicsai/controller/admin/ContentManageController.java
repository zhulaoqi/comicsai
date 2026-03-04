package com.comicsai.controller.admin;

import com.comicsai.common.ApiResponse;
import com.comicsai.model.dto.*;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.ContentDetailVO;
import com.comicsai.model.vo.ContentManageVO;
import com.comicsai.model.vo.PageVO;
import com.comicsai.service.ContentCacheService;
import com.comicsai.service.ContentGeneratorService;
import com.comicsai.service.ContentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/contents")
public class ContentManageController {

    private final ContentService contentService;
    private final ContentCacheService contentCacheService;
    private final ContentGeneratorService contentGeneratorService;

    public ContentManageController(ContentService contentService,
                                   ContentCacheService contentCacheService,
                                   ContentGeneratorService contentGeneratorService) {
        this.contentService = contentService;
        this.contentCacheService = contentCacheService;
        this.contentGeneratorService = contentGeneratorService;
    }

    @GetMapping
    public ApiResponse<PageVO<ContentManageVO>> listContents(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) Long storylineId,
            @RequestParam(required = false) Boolean isPaid,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        ContentQueryDTO query = new ContentQueryDTO();
        query.setStatus(status);
        query.setContentType(contentType);
        query.setStorylineId(storylineId);
        query.setIsPaid(isPaid);
        query.setPage(page);
        query.setSize(size);
        PageVO<ContentManageVO> result = contentService.getContents(query);
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<ContentDetailVO> getContentDetail(@PathVariable Long id) {
        ContentDetailVO detail = contentService.getContentDetail(id);
        return ApiResponse.success(detail);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> updateContent(@PathVariable Long id, @Valid @RequestBody ContentUpdateDTO dto) {
        contentService.updateContent(id, dto);
        contentCacheService.evictAll(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/review")
    public ApiResponse<Void> reviewContent(@PathVariable Long id, @Valid @RequestBody ReviewDTO dto) {
        contentService.reviewContent(id, dto.getAction());
        contentCacheService.evictAll(id);
        return ApiResponse.success();
    }

    @PostMapping("/batch-review")
    public ApiResponse<Void> batchReviewContents(@Valid @RequestBody BatchReviewDTO dto) {
        contentService.batchReviewContents(dto.getContentIds(), dto.getAction());
        contentCacheService.evictListCaches();
        for (Long contentId : dto.getContentIds()) {
            contentCacheService.evictContentDetail(contentId);
        }
        return ApiResponse.success();
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> toggleContentStatus(@PathVariable Long id, @Valid @RequestBody StatusDTO dto) {
        contentService.toggleContentStatus(id, dto.getAction());
        contentCacheService.evictAll(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/paid")
    public ApiResponse<Void> setContentPaid(@PathVariable Long id, @Valid @RequestBody PaidDTO dto) {
        contentService.setContentPaidExtended(id, dto.getIsPaid(), dto.getPrice(),
                dto.getFreeChapterCount(), dto.getDefaultChapterPrice());
        contentCacheService.evictAll(id);
        return ApiResponse.success();
    }

    @PutMapping("/chapters/{chapterId}/price")
    public ApiResponse<Void> setChapterPrice(@PathVariable Long chapterId,
                                             @RequestBody ChapterPriceDTO dto) {
        contentService.setChapterPrice(chapterId, dto.getPrice());
        return ApiResponse.success();
    }

    @PostMapping("/chapters/{chapterId}/regenerate")
    public ApiResponse<Void> regenerateChapter(@PathVariable Long chapterId) {
        contentGeneratorService.regenerateChapterAsync(chapterId);
        return ApiResponse.success("重新生成任务已提交，请稍后刷新查看结果", null);
    }

    @PutMapping("/chapters/{chapterId}/publish")
    public ApiResponse<Void> publishChapter(@PathVariable Long chapterId) {
        contentService.publishChapter(chapterId);
        return ApiResponse.success();
    }

    @PutMapping("/chapters/{chapterId}/unpublish")
    public ApiResponse<Void> unpublishChapter(@PathVariable Long chapterId) {
        contentService.unpublishChapter(chapterId);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/chapters/publish-all")
    public ApiResponse<Void> publishAllChapters(@PathVariable Long id) {
        contentService.publishAllChapters(id);
        return ApiResponse.success();
    }

    @PostMapping("/batch-paid")
    public ApiResponse<Void> batchSetContentPaid(@Valid @RequestBody BatchPaidDTO dto) {
        contentService.batchSetContentPaid(dto.getContentIds(), dto.getIsPaid(), dto.getPrice());
        contentCacheService.evictListCaches();
        for (Long contentId : dto.getContentIds()) {
            contentCacheService.evictContentDetail(contentId);
        }
        return ApiResponse.success();
    }
}
