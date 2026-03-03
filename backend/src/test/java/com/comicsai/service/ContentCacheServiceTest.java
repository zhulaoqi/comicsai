package com.comicsai.service;

import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.ContentDetailVO;
import com.comicsai.model.vo.ContentVO;
import com.comicsai.model.vo.PageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentCacheServiceTest {

    @Mock
    private ContentService contentService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ContentCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new ContentCacheService(contentService, redisTemplate);
    }

    // ==================== getPublishedContents ====================

    @Test
    void getPublishedContents_cacheHit_shouldReturnCachedData() {
        PageVO<ContentVO> cached = new PageVO<>(List.of(buildContentVO(1L)), 1, 1, 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("content:home:page:1")).thenReturn(cached);

        PageVO<ContentVO> result = cacheService.getPublishedContents(1, 10, null);

        assertEquals(1, result.getRecords().size());
        verify(contentService, never()).getPublishedContents(any(), any(), any());
    }

    @Test
    void getPublishedContents_cacheMiss_shouldQueryDbAndCache() {
        PageVO<ContentVO> dbResult = new PageVO<>(List.of(buildContentVO(1L)), 1, 1, 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("content:home:page:1")).thenReturn(null);
        when(contentService.getPublishedContents(1, 10, null)).thenReturn(dbResult);

        PageVO<ContentVO> result = cacheService.getPublishedContents(1, 10, null);

        assertEquals(1, result.getRecords().size());
        verify(contentService).getPublishedContents(1, 10, null);
        verify(valueOperations).set(eq("content:home:page:1"), eq(dbResult), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getPublishedContents_withType_shouldUseDifferentCacheKey() {
        PageVO<ContentVO> dbResult = new PageVO<>(List.of(buildContentVO(1L)), 1, 1, 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("content:list:COMIC:page:1")).thenReturn(null);
        when(contentService.getPublishedContents(1, 10, ContentType.COMIC)).thenReturn(dbResult);

        cacheService.getPublishedContents(1, 10, ContentType.COMIC);

        verify(valueOperations).get("content:list:COMIC:page:1");
        verify(valueOperations).set(eq("content:list:COMIC:page:1"), eq(dbResult), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getPublishedContents_emptyResult_shouldCacheWithShortTTL() {
        PageVO<ContentVO> emptyResult = new PageVO<>(Collections.emptyList(), 0, 1, 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("content:home:page:1")).thenReturn(null);
        when(contentService.getPublishedContents(1, 10, null)).thenReturn(emptyResult);

        cacheService.getPublishedContents(1, 10, null);

        verify(valueOperations).set(eq("content:home:page:1"), eq(emptyResult), eq(1L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getPublishedContents_redisFailure_shouldFallbackToDb() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));
        PageVO<ContentVO> dbResult = new PageVO<>(List.of(buildContentVO(1L)), 1, 1, 10);
        when(contentService.getPublishedContents(1, 10, null)).thenReturn(dbResult);

        PageVO<ContentVO> result = cacheService.getPublishedContents(1, 10, null);

        assertEquals(1, result.getRecords().size());
        verify(contentService).getPublishedContents(1, 10, null);
    }

    // ==================== getContentDetail ====================

    @Test
    void getContentDetail_cacheHit_shouldReturnCachedData() {
        ContentDetailVO cached = buildDetailVO(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("content:detail:1")).thenReturn(cached);

        ContentDetailVO result = cacheService.getContentDetail(1L);

        assertEquals(1L, result.getId());
        verify(contentService, never()).getContentDetail(any());
    }

    @Test
    void getContentDetail_cacheMiss_shouldQueryDbAndCache() {
        ContentDetailVO dbResult = buildDetailVO(1L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("content:detail:1")).thenReturn(null);
        when(contentService.getContentDetail(1L)).thenReturn(dbResult);

        ContentDetailVO result = cacheService.getContentDetail(1L);

        assertEquals(1L, result.getId());
        verify(contentService).getContentDetail(1L);
        verify(valueOperations).set(eq("content:detail:1"), eq(dbResult), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void getContentDetail_redisFailure_shouldFallbackToDb() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));
        ContentDetailVO dbResult = buildDetailVO(1L);
        when(contentService.getContentDetail(1L)).thenReturn(dbResult);

        ContentDetailVO result = cacheService.getContentDetail(1L);

        assertEquals(1L, result.getId());
    }

    // ==================== searchContents ====================

    @Test
    void searchContents_shouldDelegateDirectlyToService() {
        PageVO<ContentVO> dbResult = new PageVO<>(List.of(buildContentVO(1L)), 1, 1, 10);
        when(contentService.searchContents("test", 1, 10)).thenReturn(dbResult);

        PageVO<ContentVO> result = cacheService.searchContents("test", 1, 10);

        assertEquals(1, result.getRecords().size());
        verify(contentService).searchContents("test", 1, 10);
        // No Redis interaction for search
        verifyNoInteractions(redisTemplate);
    }

    // ==================== Cache Eviction ====================

    @Test
    void evictContentDetail_shouldDeleteCacheKey() {
        when(redisTemplate.delete("content:detail:1")).thenReturn(true);

        cacheService.evictContentDetail(1L);

        verify(redisTemplate).delete("content:detail:1");
    }

    @Test
    void evictListCaches_shouldDeleteAllListKeys() {
        Set<String> homeKeys = Set.of("content:home:page:1", "content:home:page:2");
        Set<String> typeKeys = Set.of("content:list:COMIC:page:1");
        when(redisTemplate.keys("content:home:page:*")).thenReturn(homeKeys);
        when(redisTemplate.keys("content:list:*")).thenReturn(typeKeys);

        cacheService.evictListCaches();

        verify(redisTemplate).delete(homeKeys);
        verify(redisTemplate).delete(typeKeys);
    }

    @Test
    void evictAll_shouldEvictBothDetailAndList() {
        when(redisTemplate.delete("content:detail:1")).thenReturn(true);
        when(redisTemplate.keys("content:home:page:*")).thenReturn(Collections.emptySet());
        when(redisTemplate.keys("content:list:*")).thenReturn(Collections.emptySet());

        cacheService.evictAll(1L);

        verify(redisTemplate).delete("content:detail:1");
        verify(redisTemplate).keys("content:home:page:*");
        verify(redisTemplate).keys("content:list:*");
    }

    // ==================== Helpers ====================

    private ContentVO buildContentVO(Long id) {
        ContentVO vo = new ContentVO();
        vo.setId(id);
        vo.setTitle("Test Content " + id);
        vo.setContentType(ContentType.COMIC);
        vo.setCoverUrl("/files/covers/test.jpg");
        vo.setIsPaid(false);
        vo.setPublishedAt(LocalDateTime.of(2024, 1, 15, 10, 0));
        return vo;
    }

    private ContentDetailVO buildDetailVO(Long id) {
        ContentDetailVO vo = new ContentDetailVO();
        vo.setId(id);
        vo.setTitle("Detail " + id);
        vo.setContentType(ContentType.COMIC);
        vo.setStatus(ContentStatus.PUBLISHED);
        vo.setCoverUrl("/files/covers/test.jpg");
        vo.setIsPaid(false);
        vo.setComicPages(Collections.emptyList());
        vo.setNovelChapters(Collections.emptyList());
        return vo;
    }
}
