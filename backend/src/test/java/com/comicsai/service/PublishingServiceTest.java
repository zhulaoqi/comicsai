package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.comicsai.mapper.ContentMapper;
import com.comicsai.model.entity.Content;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublishingServiceTest {

    @Mock
    private ContentMapper contentMapper;
    @Mock
    private ContentCacheService contentCacheService;

    private PublishingService publishingService;

    @BeforeEach
    void setUp() {
        publishingService = new PublishingService(contentMapper, contentCacheService);
    }

    @Test
    void publishApprovedContent_withPendingContent_shouldPublishAll() {
        Content c1 = buildContent(1L, ContentStatus.PENDING_PUBLISH);
        Content c2 = buildContent(2L, ContentStatus.PENDING_PUBLISH);
        when(contentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(c1, c2));
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        int count = publishingService.publishApprovedContent();

        assertEquals(2, count);
        verify(contentMapper, times(2)).updateById(any(Content.class));
        verify(contentCacheService).evictListCaches();
    }

    @Test
    void publishApprovedContent_shouldSetStatusToPublished() {
        Content content = buildContent(1L, ContentStatus.PENDING_PUBLISH);
        when(contentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(content));
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        publishingService.publishApprovedContent();

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        Content updated = captor.getValue();
        assertEquals(ContentStatus.PUBLISHED, updated.getStatus());
        assertNotNull(updated.getPublishedAt());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void publishApprovedContent_noPendingContent_shouldReturnZeroAndSkip() {
        when(contentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        int count = publishingService.publishApprovedContent();

        assertEquals(0, count);
        verify(contentMapper, never()).updateById(any());
        verify(contentCacheService, never()).evictListCaches();
    }

    @Test
    void publishApprovedContent_shouldClearListCachesAfterPublishing() {
        Content content = buildContent(1L, ContentStatus.PENDING_PUBLISH);
        when(contentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(content));
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        publishingService.publishApprovedContent();

        verify(contentCacheService).evictListCaches();
    }

    @Test
    void publishApprovedContent_shouldSetPublishedAtTimestamp() {
        Content content = buildContent(1L, ContentStatus.PENDING_PUBLISH);
        assertNull(content.getPublishedAt());
        when(contentMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(content));
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        LocalDateTime before = LocalDateTime.now();
        publishingService.publishApprovedContent();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        Content updated = captor.getValue();
        assertNotNull(updated.getPublishedAt());
        assertFalse(updated.getPublishedAt().isBefore(before));
        assertFalse(updated.getPublishedAt().isAfter(after));
    }

    private Content buildContent(Long id, ContentStatus status) {
        Content content = new Content();
        content.setId(id);
        content.setStorylineId(100L);
        content.setTitle("Test Content " + id);
        content.setContentType(ContentType.COMIC);
        content.setStatus(status);
        content.setCoverUrl("/covers/test.jpg");
        content.setIsPaid(false);
        content.setGeneratedAt(LocalDateTime.now().minusDays(1));
        content.setCreatedAt(LocalDateTime.now().minusDays(1));
        content.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return content;
    }
}
