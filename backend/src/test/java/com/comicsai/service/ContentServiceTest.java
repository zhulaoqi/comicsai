package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comicsai.common.exception.AccessDeniedException;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.common.exception.IllegalStateTransitionException;
import com.comicsai.common.exception.InsufficientBalanceException;
import com.comicsai.mapper.ComicPageMapper;
import com.comicsai.mapper.ContentMapper;
import com.comicsai.mapper.ContentUnlockMapper;
import com.comicsai.mapper.NovelChapterMapper;
import com.comicsai.mapper.UserMapper;
import com.comicsai.model.dto.ContentCreateDTO;
import com.comicsai.model.dto.ContentQueryDTO;
import com.comicsai.model.dto.ContentUpdateDTO;
import com.comicsai.model.entity.ComicPage;
import com.comicsai.model.entity.Content;
import com.comicsai.model.entity.ContentUnlock;
import com.comicsai.model.entity.NovelChapter;
import com.comicsai.model.entity.User;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.ContentDetailVO;
import com.comicsai.model.vo.ContentManageVO;
import com.comicsai.model.vo.ContentVO;
import com.comicsai.model.vo.PageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private ContentMapper contentMapper;
    @Mock
    private ComicPageMapper comicPageMapper;
    @Mock
    private NovelChapterMapper novelChapterMapper;
    @Mock
    private ContentUnlockMapper contentUnlockMapper;
    @Mock
    private UserMapper userMapper;

    private ContentService contentService;

    @BeforeEach
    void setUp() {
        contentService = new ContentService(contentMapper, comicPageMapper, novelChapterMapper, contentUnlockMapper, userMapper);
    }

    // ==================== Create Content ====================

    @Test
    void createContent_shouldSetInitialStatusToPendingReview() {
        ContentCreateDTO dto = new ContentCreateDTO();
        dto.setStorylineId(1L);
        dto.setTitle("Test Comic");
        dto.setContentType(ContentType.COMIC);
        dto.setCoverUrl("/files/covers/test.jpg");

        when(contentMapper.insert(any(Content.class))).thenReturn(1);

        Content result = contentService.createContent(dto);

        assertEquals(ContentStatus.PENDING_REVIEW, result.getStatus());
        assertEquals(false, result.getIsPaid());
        assertNotNull(result.getCreatedAt());

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).insert(captor.capture());
        assertEquals("Test Comic", captor.getValue().getTitle());
    }

    // ==================== Get Content ====================

    @Test
    void getContentById_shouldReturnContent() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PUBLISHED);
        when(contentMapper.selectById(1L)).thenReturn(content);

        Content result = contentService.getContentById(1L);
        assertEquals(1L, result.getId());
        assertEquals("Test", result.getTitle());
    }

    @Test
    void getContentById_shouldThrowWhenNotFound() {
        when(contentMapper.selectById(999L)).thenReturn(null);
        assertThrows(EntityNotFoundException.class, () -> contentService.getContentById(999L));
    }

    // ==================== Update Content ====================

    @Test
    void updateContent_shouldUpdateFields() {
        Content content = buildContent(1L, "Old Title", ContentType.COMIC, ContentStatus.PENDING_REVIEW);
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        ContentUpdateDTO dto = new ContentUpdateDTO();
        dto.setTitle("New Title");
        dto.setCoverUrl("/files/covers/new.jpg");
        dto.setDescription("New description");

        contentService.updateContent(1L, dto);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        assertEquals("New Title", captor.getValue().getTitle());
        assertEquals("/files/covers/new.jpg", captor.getValue().getCoverUrl());
        assertEquals("New description", captor.getValue().getDescription());
    }

    // ==================== Status Transitions ====================

    @Test
    void reviewContent_approve_shouldTransitionToPendingPublish() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PENDING_REVIEW);
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        contentService.reviewContent(1L, "approve");

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        assertEquals(ContentStatus.PENDING_PUBLISH, captor.getValue().getStatus());
    }

    @Test
    void reviewContent_reject_shouldTransitionToRejected() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PENDING_REVIEW);
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        contentService.reviewContent(1L, "reject");

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        assertEquals(ContentStatus.REJECTED, captor.getValue().getStatus());
    }

    @Test
    void transitionStatus_shouldThrowOnIllegalTransition_PendingReviewToPublished() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PENDING_REVIEW);
        when(contentMapper.selectById(1L)).thenReturn(content);

        assertThrows(IllegalStateTransitionException.class,
                () -> contentService.transitionStatus(1L, ContentStatus.PUBLISHED));
    }

    @Test
    void transitionStatus_shouldThrowOnIllegalTransition_RejectedToPendingPublish() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.REJECTED);
        when(contentMapper.selectById(1L)).thenReturn(content);

        assertThrows(IllegalStateTransitionException.class,
                () -> contentService.transitionStatus(1L, ContentStatus.PENDING_PUBLISH));
    }

    @Test
    void transitionStatus_shouldThrowOnIllegalTransition_PublishedToPendingReview() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PUBLISHED);
        when(contentMapper.selectById(1L)).thenReturn(content);

        assertThrows(IllegalStateTransitionException.class,
                () -> contentService.transitionStatus(1L, ContentStatus.PENDING_REVIEW));
    }

    @Test
    void transitionStatus_publishedToOffline_shouldSucceed() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PUBLISHED);
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        contentService.transitionStatus(1L, ContentStatus.OFFLINE);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        assertEquals(ContentStatus.OFFLINE, captor.getValue().getStatus());
    }

    @Test
    void transitionStatus_offlineToPublished_shouldSucceed() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.OFFLINE);
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        contentService.transitionStatus(1L, ContentStatus.PUBLISHED);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        assertEquals(ContentStatus.PUBLISHED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getPublishedAt());
    }

    @Test
    void transitionStatus_pendingPublishToPublished_shouldSetPublishedAt() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PENDING_PUBLISH);
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        contentService.transitionStatus(1L, ContentStatus.PUBLISHED);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        assertEquals(ContentStatus.PUBLISHED, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getPublishedAt());
    }

    @Test
    void reviewContent_invalidAction_shouldThrow() {
        assertThrows(BusinessException.class,
                () -> contentService.reviewContent(1L, "invalid"));
    }

    @Test
    void batchReviewContents_shouldReviewAll() {
        Content c1 = buildContent(1L, "C1", ContentType.COMIC, ContentStatus.PENDING_REVIEW);
        Content c2 = buildContent(2L, "C2", ContentType.NOVEL, ContentStatus.PENDING_REVIEW);
        when(contentMapper.selectById(1L)).thenReturn(c1);
        when(contentMapper.selectById(2L)).thenReturn(c2);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        contentService.batchReviewContents(List.of(1L, 2L), "approve");

        verify(contentMapper, times(2)).updateById(any(Content.class));
    }

    // ==================== Pagination ====================

    @Test
    void getPublishedContents_shouldReturnPageWithHasNext() {
        Content c1 = buildContent(1L, "C1", ContentType.COMIC, ContentStatus.PUBLISHED);
        Content c2 = buildContent(2L, "C2", ContentType.NOVEL, ContentStatus.PUBLISHED);

        Page<Content> mockPage = new Page<>(1, 2);
        mockPage.setRecords(List.of(c1, c2));
        mockPage.setTotal(5);

        when(contentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageVO<ContentVO> result = contentService.getPublishedContents(1, 2, null);

        assertEquals(2, result.getRecords().size());
        assertEquals(5, result.getTotal());
        assertTrue(result.isHasNext());
    }

    @Test
    void getPublishedContents_lastPage_shouldHaveNoNext() {
        Content c1 = buildContent(1L, "C1", ContentType.COMIC, ContentStatus.PUBLISHED);

        Page<Content> mockPage = new Page<>(3, 2);
        mockPage.setRecords(List.of(c1));
        mockPage.setTotal(5);

        when(contentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageVO<ContentVO> result = contentService.getPublishedContents(3, 2, null);

        assertEquals(1, result.getRecords().size());
        assertFalse(result.isHasNext());
    }

    @Test
    void getPublishedContents_defaultsPageAndSize() {
        Page<Content> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.emptyList());
        mockPage.setTotal(0);

        when(contentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageVO<ContentVO> result = contentService.getPublishedContents(null, null, null);

        assertEquals(1, result.getPage());
        assertEquals(10, result.getSize());
    }

    @Test
    void getContents_shouldFilterByAllCriteria() {
        ContentQueryDTO query = new ContentQueryDTO();
        query.setContentType(ContentType.COMIC);
        query.setStatus(ContentStatus.PUBLISHED);
        query.setStorylineId(1L);
        query.setIsPaid(true);
        query.setPage(1);
        query.setSize(10);

        Page<Content> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.emptyList());
        mockPage.setTotal(0);

        when(contentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageVO<ContentManageVO> result = contentService.getContents(query);

        assertNotNull(result);
        verify(contentMapper).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    // ==================== Search ====================

    @Test
    void searchContents_shouldThrowOnBlankKeyword() {
        assertThrows(BusinessException.class,
                () -> contentService.searchContents("   ", 1, 10));
    }

    @Test
    void searchContents_shouldThrowOnNullKeyword() {
        assertThrows(BusinessException.class,
                () -> contentService.searchContents(null, 1, 10));
    }

    @Test
    void searchContents_shouldReturnResults() {
        Content c1 = buildContent(1L, "Dragon Story", ContentType.NOVEL, ContentStatus.PUBLISHED);

        Page<Content> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(c1));
        mockPage.setTotal(1);

        when(contentMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        PageVO<ContentVO> result = contentService.searchContents("Dragon", 1, 10);

        assertEquals(1, result.getRecords().size());
        assertEquals("Dragon Story", result.getRecords().get(0).getTitle());
    }

    // ==================== Content Detail ====================

    @Test
    void getContentDetail_comic_shouldReturnWithPages() {
        Content content = buildContent(1L, "Comic", ContentType.COMIC, ContentStatus.PUBLISHED);
        when(contentMapper.selectById(1L)).thenReturn(content);

        ComicPage page1 = new ComicPage();
        page1.setId(1L);
        page1.setContentId(1L);
        page1.setPageNumber(1);
        page1.setImageUrl("/files/comic-images/p1.jpg");
        page1.setDialogueText("Hello!");

        when(comicPageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(page1));

        ContentDetailVO result = contentService.getContentDetail(1L);

        assertEquals(1L, result.getId());
        assertEquals(ContentType.COMIC, result.getContentType());
        assertEquals(1, result.getComicPages().size());
        assertTrue(result.getNovelChapters().isEmpty());
    }

    @Test
    void getContentDetail_novel_shouldReturnWithChapters() {
        Content content = buildContent(1L, "Novel", ContentType.NOVEL, ContentStatus.PUBLISHED);
        when(contentMapper.selectById(1L)).thenReturn(content);

        NovelChapter ch1 = new NovelChapter();
        ch1.setId(1L);
        ch1.setContentId(1L);
        ch1.setChapterNumber(1);
        ch1.setChapterTitle("Chapter 1");
        ch1.setChapterText("Once upon a time...");

        when(novelChapterMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(ch1));

        ContentDetailVO result = contentService.getContentDetail(1L);

        assertEquals(1L, result.getId());
        assertEquals(ContentType.NOVEL, result.getContentType());
        assertTrue(result.getComicPages().isEmpty());
        assertEquals(1, result.getNovelChapters().size());
    }

    // ==================== Paid Content ====================

    @Test
    void setContentPaid_shouldSetPaidWithPrice() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PUBLISHED);
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        contentService.setContentPaid(1L, true, new BigDecimal("9.99"));

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        assertTrue(captor.getValue().getIsPaid());
        assertEquals(new BigDecimal("9.99"), captor.getValue().getPrice());
    }

    @Test
    void setContentPaid_shouldClearPriceWhenFree() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(true);
        content.setPrice(new BigDecimal("9.99"));
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentMapper.updateById(any(Content.class))).thenReturn(1);

        contentService.setContentPaid(1L, false, null);

        ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
        verify(contentMapper).updateById(captor.capture());
        assertFalse(captor.getValue().getIsPaid());
        assertNull(captor.getValue().getPrice());
    }

    @Test
    void setContentPaid_shouldThrowWhenPriceInvalid() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PUBLISHED);
        when(contentMapper.selectById(1L)).thenReturn(content);

        assertThrows(BusinessException.class,
                () -> contentService.setContentPaid(1L, true, BigDecimal.ZERO));
    }

    @Test
    void setContentPaid_shouldThrowWhenPriceNull() {
        Content content = buildContent(1L, "Test", ContentType.COMIC, ContentStatus.PUBLISHED);
        when(contentMapper.selectById(1L)).thenReturn(content);

        assertThrows(BusinessException.class,
                () -> contentService.setContentPaid(1L, true, null));
    }

    // ==================== Delete ====================

    @Test
    void deleteContent_comic_shouldDeletePagesAndContent() {
        Content content = buildContent(1L, "Comic", ContentType.COMIC, ContentStatus.PENDING_REVIEW);
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(comicPageMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(3);
        when(contentMapper.deleteById(1L)).thenReturn(1);

        contentService.deleteContent(1L);

        verify(comicPageMapper).delete(any(LambdaQueryWrapper.class));
        verify(contentMapper).deleteById(1L);
        verify(novelChapterMapper, never()).delete(any());
    }

    @Test
    void deleteContent_novel_shouldDeleteChaptersAndContent() {
        Content content = buildContent(1L, "Novel", ContentType.NOVEL, ContentStatus.PENDING_REVIEW);
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(novelChapterMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(5);
        when(contentMapper.deleteById(1L)).thenReturn(1);

        contentService.deleteContent(1L);

        verify(novelChapterMapper).delete(any(LambdaQueryWrapper.class));
        verify(contentMapper).deleteById(1L);
        verify(comicPageMapper, never()).delete(any());
    }

    // ==================== Access Control ====================

    @Test
    void checkContentAccess_freeContent_guestShouldPass() {
        Content content = buildContent(1L, "Free", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(false);
        when(contentMapper.selectById(1L)).thenReturn(content);

        assertDoesNotThrow(() -> contentService.checkContentAccess(1L, null));
    }

    @Test
    void checkContentAccess_freeContent_registeredUserShouldPass() {
        Content content = buildContent(1L, "Free", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(false);
        when(contentMapper.selectById(1L)).thenReturn(content);

        assertDoesNotThrow(() -> contentService.checkContentAccess(1L, 100L));
    }

    @Test
    void checkContentAccess_paidContent_guestShouldThrowAccessDenied() {
        Content content = buildContent(1L, "Paid", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(true);
        content.setPrice(new BigDecimal("9.99"));
        when(contentMapper.selectById(1L)).thenReturn(content);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> contentService.checkContentAccess(1L, null));
        assertTrue(ex.getMessage().contains("登录"));
    }

    @Test
    void checkContentAccess_paidContent_userNotUnlocked_shouldThrowAccessDenied() {
        Content content = buildContent(1L, "Paid", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(true);
        content.setPrice(new BigDecimal("9.99"));
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentUnlockMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> contentService.checkContentAccess(1L, 100L));
        assertTrue(ex.getMessage().contains("解锁"));
    }

    @Test
    void checkContentAccess_paidContent_userUnlocked_shouldPass() {
        Content content = buildContent(1L, "Paid", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(true);
        content.setPrice(new BigDecimal("9.99"));
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentUnlockMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertDoesNotThrow(() -> contentService.checkContentAccess(1L, 100L));
    }

    // ==================== Unlock Content ====================

    @Test
    void unlockContent_shouldDeductBalanceAndCreateRecord() {
        Content content = buildContent(1L, "Paid", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(true);
        content.setPrice(new BigDecimal("5.00"));
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentUnlockMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        User user = new User();
        user.setId(100L);
        user.setBalance(new BigDecimal("10.00"));
        when(userMapper.selectById(100L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(contentUnlockMapper.insert(any(ContentUnlock.class))).thenReturn(1);

        contentService.unlockContent(1L, 100L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(new BigDecimal("5.00"), userCaptor.getValue().getBalance());

        ArgumentCaptor<ContentUnlock> unlockCaptor = ArgumentCaptor.forClass(ContentUnlock.class);
        verify(contentUnlockMapper).insert(unlockCaptor.capture());
        assertEquals(100L, unlockCaptor.getValue().getUserId());
        assertEquals(1L, unlockCaptor.getValue().getContentId());
        assertEquals(new BigDecimal("5.00"), unlockCaptor.getValue().getPricePaid());
    }

    @Test
    void unlockContent_insufficientBalance_shouldThrow() {
        Content content = buildContent(1L, "Paid", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(true);
        content.setPrice(new BigDecimal("10.00"));
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentUnlockMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        User user = new User();
        user.setId(100L);
        user.setBalance(new BigDecimal("5.00"));
        when(userMapper.selectById(100L)).thenReturn(user);

        assertThrows(InsufficientBalanceException.class,
                () -> contentService.unlockContent(1L, 100L));

        verify(userMapper, never()).updateById(any());
        verify(contentUnlockMapper, never()).insert(any());
    }

    @Test
    void unlockContent_freeContent_shouldThrow() {
        Content content = buildContent(1L, "Free", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(false);
        when(contentMapper.selectById(1L)).thenReturn(content);

        assertThrows(BusinessException.class,
                () -> contentService.unlockContent(1L, 100L));
    }

    @Test
    void unlockContent_alreadyUnlocked_shouldThrow() {
        Content content = buildContent(1L, "Paid", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(true);
        content.setPrice(new BigDecimal("5.00"));
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentUnlockMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(BusinessException.class,
                () -> contentService.unlockContent(1L, 100L));
    }

    @Test
    void unlockContent_balanceExactlyEqualsPrice_shouldSucceed() {
        Content content = buildContent(1L, "Paid", ContentType.COMIC, ContentStatus.PUBLISHED);
        content.setIsPaid(true);
        content.setPrice(new BigDecimal("10.00"));
        when(contentMapper.selectById(1L)).thenReturn(content);
        when(contentUnlockMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        User user = new User();
        user.setId(100L);
        user.setBalance(new BigDecimal("10.00"));
        when(userMapper.selectById(100L)).thenReturn(user);
        when(userMapper.updateById(any(User.class))).thenReturn(1);
        when(contentUnlockMapper.insert(any(ContentUnlock.class))).thenReturn(1);

        contentService.unlockContent(1L, 100L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(BigDecimal.ZERO.setScale(2), userCaptor.getValue().getBalance().setScale(2));
    }

    // ==================== Helper ====================

    private Content buildContent(Long id, String title, ContentType type, ContentStatus status) {
        Content content = new Content();
        content.setId(id);
        content.setStorylineId(1L);
        content.setTitle(title);
        content.setContentType(type);
        content.setStatus(status);
        content.setCoverUrl("/files/covers/test.jpg");
        content.setIsPaid(false);
        content.setGeneratedAt(LocalDateTime.now());
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        if (status == ContentStatus.PUBLISHED) {
            content.setPublishedAt(LocalDateTime.now());
        }
        return content;
    }
}
