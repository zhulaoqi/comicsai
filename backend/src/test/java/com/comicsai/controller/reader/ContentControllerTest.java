package com.comicsai.controller.reader;

import com.comicsai.common.JwtUtil;
import com.comicsai.common.exception.AccessDeniedException;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.common.exception.InsufficientBalanceException;
import com.comicsai.model.entity.ComicPage;
import com.comicsai.model.entity.NovelChapter;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.ContentDetailVO;
import com.comicsai.model.vo.ContentVO;
import com.comicsai.model.vo.PageVO;
import com.comicsai.service.ContentCacheService;
import com.comicsai.service.ContentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContentController.class)
class ContentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ContentCacheService contentCacheService;

    @MockBean
    private ContentService contentService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    // ==================== List Contents ====================

    @Test
    void listContents_shouldReturnPagedResults() throws Exception {
        ContentVO vo = buildContentVO(1L, "Test Comic", ContentType.COMIC);
        PageVO<ContentVO> page = new PageVO<>(List.of(vo), 1, 1, 10);

        when(contentCacheService.getPublishedContents(1, 10, null)).thenReturn(page);

        mockMvc.perform(get("/api/reader/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].title").value("Test Comic"))
                .andExpect(jsonPath("$.data.records[0].contentType").value("COMIC"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void listContents_withTypeFilter_shouldPassTypeToService() throws Exception {
        ContentVO vo = buildContentVO(1L, "My Novel", ContentType.NOVEL);
        PageVO<ContentVO> page = new PageVO<>(List.of(vo), 1, 1, 10);

        when(contentCacheService.getPublishedContents(1, 10, ContentType.NOVEL)).thenReturn(page);

        mockMvc.perform(get("/api/reader/contents").param("type", "NOVEL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].contentType").value("NOVEL"));
    }

    @Test
    void listContents_withPagination_shouldPassParams() throws Exception {
        PageVO<ContentVO> page = new PageVO<>(Collections.emptyList(), 0, 2, 5);

        when(contentCacheService.getPublishedContents(2, 5, null)).thenReturn(page);

        mockMvc.perform(get("/api/reader/contents")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    @Test
    void listContents_emptyResult_shouldReturnEmptyList() throws Exception {
        PageVO<ContentVO> page = new PageVO<>(Collections.emptyList(), 0, 1, 10);

        when(contentCacheService.getPublishedContents(1, 10, null)).thenReturn(page);

        mockMvc.perform(get("/api/reader/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    // ==================== Content Detail ====================

    @Test
    void getContentDetail_comic_shouldReturnWithPages() throws Exception {
        ContentDetailVO detail = buildComicDetailVO(1L, "Comic Title");

        when(contentCacheService.getContentDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/reader/contents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Comic Title"))
                .andExpect(jsonPath("$.data.contentType").value("COMIC"))
                .andExpect(jsonPath("$.data.comicPages").isArray())
                .andExpect(jsonPath("$.data.comicPages[0].pageNumber").value(1));
    }

    @Test
    void getContentDetail_novel_shouldReturnWithChapters() throws Exception {
        ContentDetailVO detail = buildNovelDetailVO(2L, "Novel Title");

        when(contentCacheService.getContentDetail(2L)).thenReturn(detail);

        mockMvc.perform(get("/api/reader/contents/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentType").value("NOVEL"))
                .andExpect(jsonPath("$.data.novelChapters").isArray())
                .andExpect(jsonPath("$.data.novelChapters[0].chapterNumber").value(1));
    }

    @Test
    void getContentDetail_notFound_shouldReturn404() throws Exception {
        when(contentCacheService.getContentDetail(999L))
                .thenThrow(new EntityNotFoundException("Content", 999L));

        mockMvc.perform(get("/api/reader/contents/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== Search ====================

    @Test
    void searchContents_shouldReturnMatchingResults() throws Exception {
        ContentVO vo = buildContentVO(1L, "Dragon Adventure", ContentType.NOVEL);
        PageVO<ContentVO> page = new PageVO<>(List.of(vo), 1, 1, 10);

        when(contentCacheService.searchContents("Dragon", 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/reader/contents/search").param("keyword", "Dragon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].title").value("Dragon Adventure"));
    }

    @Test
    void searchContents_blankKeyword_shouldReturn400() throws Exception {
        when(contentCacheService.searchContents("   ", 1, 10))
                .thenThrow(new BusinessException(400, "搜索关键词不能为空"));

        mockMvc.perform(get("/api/reader/contents/search").param("keyword", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void searchContents_emptyResult_shouldReturnEmptyList() throws Exception {
        PageVO<ContentVO> page = new PageVO<>(Collections.emptyList(), 0, 1, 10);

        when(contentCacheService.searchContents("nonexistent", 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/reader/contents/search").param("keyword", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void searchContents_withPagination_shouldPassParams() throws Exception {
        PageVO<ContentVO> page = new PageVO<>(Collections.emptyList(), 0, 3, 5);

        when(contentCacheService.searchContents("test", 3, 5)).thenReturn(page);

        mockMvc.perform(get("/api/reader/contents/search")
                        .param("keyword", "test")
                        .param("page", "3")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(3))
                .andExpect(jsonPath("$.data.size").value(5));
    }

    // ==================== Guest Access ====================

    @Test
    void listContents_guestAccess_shouldSucceedWithoutAuth() throws Exception {
        PageVO<ContentVO> page = new PageVO<>(Collections.emptyList(), 0, 1, 10);
        when(contentCacheService.getPublishedContents(1, 10, null)).thenReturn(page);

        // No Authorization header — guest access
        mockMvc.perform(get("/api/reader/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getContentDetail_guestAccess_shouldSucceedWithoutAuth() throws Exception {
        ContentDetailVO detail = buildComicDetailVO(1L, "Free Comic");
        when(contentCacheService.getContentDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/reader/contents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== Content VO Completeness ====================

    @Test
    void listContents_shouldReturnCompleteVOFields() throws Exception {
        ContentVO vo = buildContentVO(1L, "Paid Comic", ContentType.COMIC);
        vo.setIsPaid(true);
        vo.setPrice(new BigDecimal("9.99"));
        vo.setDescription("A great comic");
        PageVO<ContentVO> page = new PageVO<>(List.of(vo), 1, 1, 10);

        when(contentCacheService.getPublishedContents(1, 10, null)).thenReturn(page);

        mockMvc.perform(get("/api/reader/contents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].coverUrl").value("/files/covers/test.jpg"))
                .andExpect(jsonPath("$.data.records[0].title").value("Paid Comic"))
                .andExpect(jsonPath("$.data.records[0].contentType").value("COMIC"))
                .andExpect(jsonPath("$.data.records[0].publishedAt").exists())
                .andExpect(jsonPath("$.data.records[0].isPaid").value(true))
                .andExpect(jsonPath("$.data.records[0].price").value(9.99));
    }

    // ==================== Access Control ====================

    @Test
    void getContentDetail_paidContent_guestShouldReturn403() throws Exception {
        doThrow(new AccessDeniedException("请登录后访问付费内容"))
                .when(contentService).checkContentAccess(eq(1L), isNull());

        mockMvc.perform(get("/api/reader/contents/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("请登录后访问付费内容"));
    }

    @Test
    void getContentDetail_paidContent_userNotUnlocked_shouldReturn403() throws Exception {
        doThrow(new AccessDeniedException("该内容为付费内容，请先解锁"))
                .when(contentService).checkContentAccess(eq(1L), isNull());

        mockMvc.perform(get("/api/reader/contents/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void getContentDetail_freeContent_shouldPassAccessCheck() throws Exception {
        doNothing().when(contentService).checkContentAccess(eq(1L), isNull());
        ContentDetailVO detail = buildComicDetailVO(1L, "Free Comic");
        when(contentCacheService.getContentDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/reader/contents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Free Comic"));
    }

    // ==================== Unlock Endpoint ====================

    @Test
    void unlockContent_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/reader/contents/1/unlock"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== Helpers ====================

    private ContentVO buildContentVO(Long id, String title, ContentType type) {
        ContentVO vo = new ContentVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setContentType(type);
        vo.setCoverUrl("/files/covers/test.jpg");
        vo.setIsPaid(false);
        vo.setPublishedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        return vo;
    }

    private ContentDetailVO buildComicDetailVO(Long id, String title) {
        ContentDetailVO detail = new ContentDetailVO();
        detail.setId(id);
        detail.setTitle(title);
        detail.setContentType(ContentType.COMIC);
        detail.setStatus(ContentStatus.PUBLISHED);
        detail.setCoverUrl("/files/covers/comic.jpg");
        detail.setIsPaid(false);
        detail.setPublishedAt(LocalDateTime.of(2024, 1, 15, 10, 0));

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

    private ContentDetailVO buildNovelDetailVO(Long id, String title) {
        ContentDetailVO detail = new ContentDetailVO();
        detail.setId(id);
        detail.setTitle(title);
        detail.setContentType(ContentType.NOVEL);
        detail.setStatus(ContentStatus.PUBLISHED);
        detail.setCoverUrl("/files/covers/novel.jpg");
        detail.setIsPaid(false);
        detail.setPublishedAt(LocalDateTime.of(2024, 1, 15, 10, 0));

        NovelChapter ch1 = new NovelChapter();
        ch1.setId(1L);
        ch1.setContentId(id);
        ch1.setChapterNumber(1);
        ch1.setChapterTitle("Chapter 1");
        ch1.setChapterText("Once upon a time...");

        detail.setComicPages(Collections.emptyList());
        detail.setNovelChapters(List.of(ch1));
        return detail;
    }
}
