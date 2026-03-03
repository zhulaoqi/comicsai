package com.comicsai.controller.admin;

import com.comicsai.common.JwtUtil;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.common.exception.IllegalStateTransitionException;
import com.comicsai.model.dto.*;
import com.comicsai.model.entity.ComicPage;
import com.comicsai.model.entity.NovelChapter;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.ContentDetailVO;
import com.comicsai.model.vo.ContentManageVO;
import com.comicsai.model.vo.PageVO;
import com.comicsai.service.ContentCacheService;
import com.comicsai.service.ContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentManageController.class)
class ContentManageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContentService contentService;

    @MockBean
    private ContentCacheService contentCacheService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== List Contents ====================

    @Test
    void listContents_noFilters_shouldReturnAllContents() throws Exception {
        ContentManageVO vo = buildManageVO(1L, "Test Content", ContentType.COMIC, ContentStatus.PENDING_REVIEW);
        PageVO<ContentManageVO> page = new PageVO<>(List.of(vo), 1, 1, 10);

        when(contentService.getContents(any(ContentQueryDTO.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].title").value("Test Content"))
                .andExpect(jsonPath("$.data.records[0].status").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void listContents_withStatusFilter_shouldPassToService() throws Exception {
        PageVO<ContentManageVO> page = new PageVO<>(Collections.emptyList(), 0, 1, 10);
        when(contentService.getContents(any(ContentQueryDTO.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/contents").param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).getContents(argThat(q -> q.getStatus() == ContentStatus.PUBLISHED));
    }

    @Test
    void listContents_withTypeFilter_shouldPassToService() throws Exception {
        PageVO<ContentManageVO> page = new PageVO<>(Collections.emptyList(), 0, 1, 10);
        when(contentService.getContents(any(ContentQueryDTO.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/contents").param("contentType", "NOVEL"))
                .andExpect(status().isOk());

        verify(contentService).getContents(argThat(q -> q.getContentType() == ContentType.NOVEL));
    }

    @Test
    void listContents_withStorylineFilter_shouldPassToService() throws Exception {
        PageVO<ContentManageVO> page = new PageVO<>(Collections.emptyList(), 0, 1, 10);
        when(contentService.getContents(any(ContentQueryDTO.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/contents").param("storylineId", "5"))
                .andExpect(status().isOk());

        verify(contentService).getContents(argThat(q -> q.getStorylineId() != null && q.getStorylineId() == 5L));
    }

    @Test
    void listContents_withPaidFilter_shouldPassToService() throws Exception {
        PageVO<ContentManageVO> page = new PageVO<>(Collections.emptyList(), 0, 1, 10);
        when(contentService.getContents(any(ContentQueryDTO.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/contents").param("isPaid", "true"))
                .andExpect(status().isOk());

        verify(contentService).getContents(argThat(q -> Boolean.TRUE.equals(q.getIsPaid())));
    }

    @Test
    void listContents_withPagination_shouldPassParams() throws Exception {
        PageVO<ContentManageVO> page = new PageVO<>(Collections.emptyList(), 0, 2, 5);
        when(contentService.getContents(any(ContentQueryDTO.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/contents")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    @Test
    void listContents_withAllFilters_shouldPassAllToService() throws Exception {
        PageVO<ContentManageVO> page = new PageVO<>(Collections.emptyList(), 0, 1, 10);
        when(contentService.getContents(any(ContentQueryDTO.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/contents")
                        .param("status", "PENDING_REVIEW")
                        .param("contentType", "COMIC")
                        .param("storylineId", "3")
                        .param("isPaid", "false"))
                .andExpect(status().isOk());

        verify(contentService).getContents(argThat(q ->
                q.getStatus() == ContentStatus.PENDING_REVIEW
                        && q.getContentType() == ContentType.COMIC
                        && q.getStorylineId() == 3L
                        && Boolean.FALSE.equals(q.getIsPaid())
        ));
    }

    // ==================== Content Detail ====================

    @Test
    void getContentDetail_shouldReturnDetail() throws Exception {
        ContentDetailVO detail = buildComicDetailVO(1L, "Comic Detail");
        when(contentService.getContentDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/contents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Comic Detail"))
                .andExpect(jsonPath("$.data.comicPages").isArray());
    }

    @Test
    void getContentDetail_notFound_shouldReturn404() throws Exception {
        when(contentService.getContentDetail(999L))
                .thenThrow(new EntityNotFoundException("内容", 999L));

        mockMvc.perform(get("/api/admin/contents/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== Update Content ====================

    @Test
    void updateContent_shouldSucceedAndEvictCache() throws Exception {
        ContentUpdateDTO dto = new ContentUpdateDTO();
        dto.setTitle("Updated Title");
        dto.setCoverUrl("/files/covers/new.jpg");

        mockMvc.perform(put("/api/admin/contents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).updateContent(eq(1L), any(ContentUpdateDTO.class));
        verify(contentCacheService).evictAll(1L);
    }

    @Test
    void updateContent_blankTitle_shouldReturn400() throws Exception {
        ContentUpdateDTO dto = new ContentUpdateDTO();
        dto.setTitle("");

        mockMvc.perform(put("/api/admin/contents/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void updateContent_notFound_shouldReturn404() throws Exception {
        ContentUpdateDTO dto = new ContentUpdateDTO();
        dto.setTitle("Title");

        doThrow(new EntityNotFoundException("内容", 999L))
                .when(contentService).updateContent(eq(999L), any(ContentUpdateDTO.class));

        mockMvc.perform(put("/api/admin/contents/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== Review Content ====================

    @Test
    void reviewContent_approve_shouldSucceedAndEvictCache() throws Exception {
        ReviewDTO dto = new ReviewDTO();
        dto.setAction("approve");

        mockMvc.perform(put("/api/admin/contents/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).reviewContent(1L, "approve");
        verify(contentCacheService).evictAll(1L);
    }

    @Test
    void reviewContent_reject_shouldSucceedAndEvictCache() throws Exception {
        ReviewDTO dto = new ReviewDTO();
        dto.setAction("reject");

        mockMvc.perform(put("/api/admin/contents/2/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).reviewContent(2L, "reject");
        verify(contentCacheService).evictAll(2L);
    }

    @Test
    void reviewContent_invalidAction_shouldReturn400() throws Exception {
        ReviewDTO dto = new ReviewDTO();
        dto.setAction("invalid");

        mockMvc.perform(put("/api/admin/contents/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void reviewContent_illegalTransition_shouldReturn409() throws Exception {
        ReviewDTO dto = new ReviewDTO();
        dto.setAction("approve");

        doThrow(new IllegalStateTransitionException("PUBLISHED", "PENDING_PUBLISH"))
                .when(contentService).reviewContent(1L, "approve");

        mockMvc.perform(put("/api/admin/contents/1/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    // ==================== Batch Review ====================

    @Test
    void batchReview_approve_shouldSucceedAndEvictCaches() throws Exception {
        BatchReviewDTO dto = new BatchReviewDTO();
        dto.setContentIds(List.of(1L, 2L, 3L));
        dto.setAction("approve");

        mockMvc.perform(post("/api/admin/contents/batch-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).batchReviewContents(List.of(1L, 2L, 3L), "approve");
        verify(contentCacheService).evictListCaches();
        verify(contentCacheService).evictContentDetail(1L);
        verify(contentCacheService).evictContentDetail(2L);
        verify(contentCacheService).evictContentDetail(3L);
    }

    @Test
    void batchReview_emptyIds_shouldReturn400() throws Exception {
        BatchReviewDTO dto = new BatchReviewDTO();
        dto.setContentIds(Collections.emptyList());
        dto.setAction("approve");

        mockMvc.perform(post("/api/admin/contents/batch-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void batchReview_blankAction_shouldReturn400() throws Exception {
        BatchReviewDTO dto = new BatchReviewDTO();
        dto.setContentIds(List.of(1L));
        dto.setAction("");

        mockMvc.perform(post("/api/admin/contents/batch-review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== Toggle Status ====================

    @Test
    void toggleStatus_offline_shouldSucceedAndEvictCache() throws Exception {
        StatusDTO dto = new StatusDTO();
        dto.setAction("offline");

        mockMvc.perform(put("/api/admin/contents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).toggleContentStatus(1L, "offline");
        verify(contentCacheService).evictAll(1L);
    }

    @Test
    void toggleStatus_online_shouldSucceedAndEvictCache() throws Exception {
        StatusDTO dto = new StatusDTO();
        dto.setAction("online");

        mockMvc.perform(put("/api/admin/contents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).toggleContentStatus(1L, "online");
        verify(contentCacheService).evictAll(1L);
    }

    @Test
    void toggleStatus_invalidAction_shouldReturn400() throws Exception {
        StatusDTO dto = new StatusDTO();
        dto.setAction("invalid");

        mockMvc.perform(put("/api/admin/contents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void toggleStatus_illegalTransition_shouldReturn409() throws Exception {
        StatusDTO dto = new StatusDTO();
        dto.setAction("offline");

        doThrow(new IllegalStateTransitionException("PENDING_REVIEW", "OFFLINE"))
                .when(contentService).toggleContentStatus(1L, "offline");

        mockMvc.perform(put("/api/admin/contents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    // ==================== Set Paid ====================

    @Test
    void setContentPaid_shouldSucceedAndEvictCache() throws Exception {
        PaidDTO dto = new PaidDTO();
        dto.setIsPaid(true);
        dto.setPrice(new BigDecimal("9.99"));

        mockMvc.perform(put("/api/admin/contents/1/paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).setContentPaid(1L, true, new BigDecimal("9.99"));
        verify(contentCacheService).evictAll(1L);
    }

    @Test
    void setContentPaid_setFree_shouldSucceedAndEvictCache() throws Exception {
        PaidDTO dto = new PaidDTO();
        dto.setIsPaid(false);

        mockMvc.perform(put("/api/admin/contents/1/paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).setContentPaid(1L, false, null);
        verify(contentCacheService).evictAll(1L);
    }

    @Test
    void setContentPaid_nullIsPaid_shouldReturn400() throws Exception {
        PaidDTO dto = new PaidDTO();
        // isPaid is null

        mockMvc.perform(put("/api/admin/contents/1/paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void setContentPaid_paidWithoutPrice_shouldReturn400() throws Exception {
        PaidDTO dto = new PaidDTO();
        dto.setIsPaid(true);
        // price is null — service should throw

        doThrow(new BusinessException(400, "付费内容价格必须大于0"))
                .when(contentService).setContentPaid(1L, true, null);

        mockMvc.perform(put("/api/admin/contents/1/paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== Batch Set Paid ====================

    @Test
    void batchSetPaid_shouldSucceedAndEvictCaches() throws Exception {
        BatchPaidDTO dto = new BatchPaidDTO();
        dto.setContentIds(List.of(1L, 2L));
        dto.setIsPaid(true);
        dto.setPrice(new BigDecimal("5.00"));

        mockMvc.perform(post("/api/admin/contents/batch-paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(contentService).batchSetContentPaid(List.of(1L, 2L), true, new BigDecimal("5.00"));
        verify(contentCacheService).evictListCaches();
        verify(contentCacheService).evictContentDetail(1L);
        verify(contentCacheService).evictContentDetail(2L);
    }

    @Test
    void batchSetPaid_emptyIds_shouldReturn400() throws Exception {
        BatchPaidDTO dto = new BatchPaidDTO();
        dto.setContentIds(Collections.emptyList());
        dto.setIsPaid(true);
        dto.setPrice(new BigDecimal("5.00"));

        mockMvc.perform(post("/api/admin/contents/batch-paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void batchSetPaid_nullIsPaid_shouldReturn400() throws Exception {
        BatchPaidDTO dto = new BatchPaidDTO();
        dto.setContentIds(List.of(1L));
        // isPaid is null

        mockMvc.perform(post("/api/admin/contents/batch-paid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== Helpers ====================

    private ContentManageVO buildManageVO(Long id, String title, ContentType type, ContentStatus status) {
        ContentManageVO vo = new ContentManageVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setContentType(type);
        vo.setStatus(status);
        vo.setStorylineId(1L);
        vo.setCoverUrl("/files/covers/test.jpg");
        vo.setIsPaid(false);
        vo.setGeneratedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        vo.setCreatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        vo.setUpdatedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        return vo;
    }

    private ContentDetailVO buildComicDetailVO(Long id, String title) {
        ContentDetailVO detail = new ContentDetailVO();
        detail.setId(id);
        detail.setTitle(title);
        detail.setContentType(ContentType.COMIC);
        detail.setStatus(ContentStatus.PENDING_REVIEW);
        detail.setCoverUrl("/files/covers/comic.jpg");
        detail.setIsPaid(false);

        ComicPage page1 = new ComicPage();
        page1.setId(1L);
        page1.setContentId(id);
        page1.setPageNumber(1);
        page1.setImageUrl("/files/comic-images/p1.jpg");
        page1.setDialogueText("Hello!");

        detail.setComicPages(List.of(page1));
        detail.setNovelChapters(Collections.emptyList());
        return detail;
    }
}
