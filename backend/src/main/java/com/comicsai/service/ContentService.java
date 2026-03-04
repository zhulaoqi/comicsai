package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.comicsai.common.exception.AccessDeniedException;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.common.exception.EntityNotFoundException;
import com.comicsai.common.exception.IllegalStateTransitionException;
import com.comicsai.common.exception.InsufficientBalanceException;
import com.comicsai.mapper.ChapterUnlockMapper;
import com.comicsai.mapper.ComicPageMapper;
import com.comicsai.mapper.ContentMapper;
import com.comicsai.mapper.ContentUnlockMapper;
import com.comicsai.mapper.NovelChapterMapper;
import com.comicsai.mapper.UserMapper;
import com.comicsai.model.dto.ContentCreateDTO;
import com.comicsai.model.dto.ContentQueryDTO;
import com.comicsai.model.dto.ContentUpdateDTO;
import com.comicsai.model.entity.ChapterUnlock;
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
import com.comicsai.model.vo.NovelChapterVO;
import com.comicsai.model.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ContentService {

    private final ContentMapper contentMapper;
    private final ComicPageMapper comicPageMapper;
    private final NovelChapterMapper novelChapterMapper;
    private final ContentUnlockMapper contentUnlockMapper;
    private final ChapterUnlockMapper chapterUnlockMapper;
    private final UserMapper userMapper;

    public ContentService(ContentMapper contentMapper,
                          ComicPageMapper comicPageMapper,
                          NovelChapterMapper novelChapterMapper,
                          ContentUnlockMapper contentUnlockMapper,
                          ChapterUnlockMapper chapterUnlockMapper,
                          UserMapper userMapper) {
        this.contentMapper = contentMapper;
        this.comicPageMapper = comicPageMapper;
        this.novelChapterMapper = novelChapterMapper;
        this.contentUnlockMapper = contentUnlockMapper;
        this.chapterUnlockMapper = chapterUnlockMapper;
        this.userMapper = userMapper;
    }

    // ==================== CRUD Operations ====================

    @Transactional
    public Content createContent(ContentCreateDTO dto) {
        Content content = new Content();
        content.setStorylineId(dto.getStorylineId());
        content.setTitle(dto.getTitle());
        content.setContentType(dto.getContentType());
        content.setStatus(ContentStatus.PENDING_REVIEW);
        content.setCoverUrl(dto.getCoverUrl());
        content.setDescription(dto.getDescription());
        content.setIsPaid(false);
        content.setGeneratedAt(LocalDateTime.now());
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());
        contentMapper.insert(content);
        return content;
    }

    public Content getContentById(Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new EntityNotFoundException("内容", contentId);
        }
        return content;
    }

    @Transactional
    public void updateContent(Long contentId, ContentUpdateDTO dto) {
        Content content = getContentById(contentId);
        content.setTitle(dto.getTitle());
        if (dto.getCoverUrl() != null) {
            content.setCoverUrl(dto.getCoverUrl());
        }
        if (dto.getDescription() != null) {
            content.setDescription(dto.getDescription());
        }
        content.setUpdatedAt(LocalDateTime.now());
        contentMapper.updateById(content);
    }

    @Transactional
    public void deleteContent(Long contentId) {
        Content content = getContentById(contentId);
        // Delete associated comic pages or novel chapters
        if (content.getContentType() == ContentType.COMIC) {
            LambdaQueryWrapper<ComicPage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ComicPage::getContentId, contentId);
            comicPageMapper.delete(wrapper);
        } else {
            LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NovelChapter::getContentId, contentId);
            novelChapterMapper.delete(wrapper);
        }
        contentMapper.deleteById(contentId);
    }

    // ==================== Status Transitions ====================

    @Transactional
    public void transitionStatus(Long contentId, ContentStatus targetStatus) {
        Content content = getContentById(contentId);
        ContentStatus currentStatus = content.getStatus();

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new IllegalStateTransitionException(currentStatus.getValue(), targetStatus.getValue());
        }

        content.setStatus(targetStatus);
        content.setUpdatedAt(LocalDateTime.now());

        if (targetStatus == ContentStatus.PUBLISHED) {
            content.setPublishedAt(LocalDateTime.now());
        }

        contentMapper.updateById(content);
    }

    public void reviewContent(Long contentId, String action) {
        ContentStatus targetStatus;
        if ("approve".equalsIgnoreCase(action)) {
            targetStatus = ContentStatus.PENDING_PUBLISH;
        } else if ("reject".equalsIgnoreCase(action)) {
            targetStatus = ContentStatus.REJECTED;
        } else {
            throw new BusinessException(400, "无效的审核操作: " + action);
        }
        transitionStatus(contentId, targetStatus);
    }

    @Transactional
    public void batchReviewContents(List<Long> contentIds, String action) {
        for (Long contentId : contentIds) {
            reviewContent(contentId, action);
        }
    }

    public void toggleContentStatus(Long contentId, String action) {
        ContentStatus targetStatus;
        if ("offline".equalsIgnoreCase(action)) {
            targetStatus = ContentStatus.OFFLINE;
        } else if ("online".equalsIgnoreCase(action)) {
            targetStatus = ContentStatus.PUBLISHED;
        } else {
            throw new BusinessException(400, "无效的操作: " + action);
        }
        transitionStatus(contentId, targetStatus);
    }

    // ==================== Pagination Queries ====================

    public PageVO<ContentVO> getPublishedContents(Integer page, Integer size, ContentType type) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Content::getStatus, ContentStatus.PUBLISHED);
        if (type != null) {
            wrapper.eq(Content::getContentType, type);
        }
        wrapper.orderByDesc(Content::getPublishedAt);

        IPage<Content> pageResult = contentMapper.selectPage(new Page<>(page, size), wrapper);
        List<ContentVO> records = pageResult.getRecords().stream()
                .map(ContentVO::fromContent)
                .toList();

        return new PageVO<>(records, pageResult.getTotal(), page, size);
    }

    public PageVO<ContentManageVO> getContents(ContentQueryDTO query) {
        int page = query.getPage() != null && query.getPage() >= 1 ? query.getPage() : 1;
        int size = query.getSize() != null && query.getSize() >= 1 ? query.getSize() : 10;

        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        if (query.getContentType() != null) {
            wrapper.eq(Content::getContentType, query.getContentType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Content::getStatus, query.getStatus());
        }
        if (query.getStorylineId() != null) {
            wrapper.eq(Content::getStorylineId, query.getStorylineId());
        }
        if (query.getIsPaid() != null) {
            wrapper.eq(Content::getIsPaid, query.getIsPaid());
        }
        wrapper.orderByDesc(Content::getCreatedAt);

        IPage<Content> pageResult = contentMapper.selectPage(new Page<>(page, size), wrapper);
        List<ContentManageVO> records = pageResult.getRecords().stream()
                .map(ContentManageVO::fromContent)
                .toList();

        return new PageVO<>(records, pageResult.getTotal(), page, size);
    }

    public PageVO<ContentVO> searchContents(String keyword, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(400, "搜索关键词不能为空");
        }

        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Content::getStatus, ContentStatus.PUBLISHED);
        wrapper.and(w -> w.like(Content::getTitle, keyword)
                .or()
                .like(Content::getDescription, keyword));
        wrapper.orderByDesc(Content::getPublishedAt);

        IPage<Content> pageResult = contentMapper.selectPage(new Page<>(page, size), wrapper);
        List<ContentVO> records = pageResult.getRecords().stream()
                .map(ContentVO::fromContent)
                .toList();

        return new PageVO<>(records, pageResult.getTotal(), page, size);
    }

    // ==================== Content Detail ====================

    public ContentDetailVO getContentDetail(Long contentId) {
        Content content = getContentById(contentId);

        List<ComicPage> comicPages = Collections.emptyList();
        List<NovelChapter> novelChapters = Collections.emptyList();

        if (content.getContentType() == ContentType.COMIC) {
            LambdaQueryWrapper<ComicPage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ComicPage::getContentId, contentId);
            wrapper.orderByAsc(ComicPage::getPageNumber);
            comicPages = comicPageMapper.selectList(wrapper);
        } else if (content.getContentType() == ContentType.NOVEL) {
            LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NovelChapter::getContentId, contentId);
            wrapper.orderByAsc(NovelChapter::getChapterNumber);
            novelChapters = novelChapterMapper.selectList(wrapper);
        }

        return ContentDetailVO.fromContent(content, comicPages, novelChapters);
    }

    // ==================== Access Control ====================

    /**
     * Check if a user can access the given content.
     * - Free content: accessible by everyone (guest or registered user)
     * - Paid content: guest → AccessDeniedException (login prompt)
     * - Paid content + registered user who hasn't unlocked → AccessDeniedException (unlock prompt)
     * - Paid content + registered user who has unlocked → allowed
     */
    public void checkContentAccess(Long contentId, Long userId) {
        Content content = getContentById(contentId);

        // Free content is accessible by everyone
        if (!Boolean.TRUE.equals(content.getIsPaid())) {
            return;
        }

        // Paid content: guest must login
        if (userId == null) {
            throw new AccessDeniedException("请登录后访问付费内容");
        }

        // Paid content: check if user has unlocked
        if (!hasUnlocked(userId, contentId)) {
            throw new AccessDeniedException("该内容为付费内容，请先解锁");
        }
    }

    /**
     * Check if a user has unlocked a specific content.
     */
    public boolean hasUnlocked(Long userId, Long contentId) {
        LambdaQueryWrapper<ContentUnlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ContentUnlock::getUserId, userId);
        wrapper.eq(ContentUnlock::getContentId, contentId);
        return contentUnlockMapper.selectCount(wrapper) > 0;
    }

    /**
     * Unlock paid content for a user.
     * Checks balance >= price, deducts balance, creates unlock record.
     */
    @Transactional
    public void unlockContent(Long contentId, Long userId) {
        Content content = getContentById(contentId);

        // Only paid content needs unlocking
        if (!Boolean.TRUE.equals(content.getIsPaid())) {
            throw new BusinessException(400, "该内容为免费内容，无需解锁");
        }

        // Check if already unlocked
        if (hasUnlocked(userId, contentId)) {
            throw new BusinessException(400, "该内容已解锁");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new EntityNotFoundException("用户", userId);
        }

        BigDecimal price = content.getPrice();
        if (price == null) {
            price = BigDecimal.ZERO;
        }

        // Check balance
        if (user.getBalance().compareTo(price) < 0) {
            throw new InsufficientBalanceException("余额不足，请先充值");
        }

        // Deduct balance
        user.setBalance(user.getBalance().subtract(price));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // Create unlock record
        ContentUnlock unlock = new ContentUnlock();
        unlock.setUserId(userId);
        unlock.setContentId(contentId);
        unlock.setPricePaid(price);
        unlock.setUnlockedAt(LocalDateTime.now());
        contentUnlockMapper.insert(unlock);
    }

    // ==================== Paid Content Management ====================

    @Transactional
    public void setContentPaid(Long contentId, Boolean isPaid, BigDecimal price) {
        Content content = getContentById(contentId);
        content.setIsPaid(isPaid);
        if (Boolean.TRUE.equals(isPaid)) {
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(400, "付费内容价格必须大于0");
            }
            content.setPrice(price);
        } else {
            content.setPrice(null);
        }
        content.setUpdatedAt(LocalDateTime.now());
        contentMapper.updateById(content);
    }

    @Transactional
    public void setContentPaidExtended(Long contentId, Boolean isPaid, BigDecimal price,
                                       Integer freeChapterCount, BigDecimal defaultChapterPrice) {
        Content content = getContentById(contentId);
        content.setIsPaid(isPaid);
        if (Boolean.TRUE.equals(isPaid)) {
            content.setPrice(price);
            content.setFreeChapterCount(freeChapterCount != null ? freeChapterCount : 0);
            content.setDefaultChapterPrice(defaultChapterPrice);
        } else {
            content.setPrice(null);
            content.setFreeChapterCount(0);
            content.setDefaultChapterPrice(null);
        }
        content.setUpdatedAt(LocalDateTime.now());
        contentMapper.updateById(content);
    }

    @Transactional
    public void setChapterPrice(Long chapterId, BigDecimal price) {
        NovelChapter chapter = novelChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new EntityNotFoundException("章节", chapterId);
        }
        chapter.setPrice(price);
        novelChapterMapper.updateById(chapter);
    }

    @Transactional
    public void batchSetContentPaid(List<Long> contentIds, Boolean isPaid, BigDecimal price) {
        for (Long contentId : contentIds) {
            setContentPaid(contentId, isPaid, price);
        }
    }

    // ==================== Chapter-Level Access Control ====================

    public ContentDetailVO getContentDetailForReader(Long contentId, Long userId) {
        Content content = getContentById(contentId);

        List<ComicPage> comicPages = Collections.emptyList();
        if (content.getContentType() == ContentType.COMIC) {
            LambdaQueryWrapper<ComicPage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ComicPage::getContentId, contentId);
            wrapper.orderByAsc(ComicPage::getPageNumber);
            comicPages = comicPageMapper.selectList(wrapper);
        }

        ContentDetailVO vo = ContentDetailVO.fromContent(content, comicPages, Collections.emptyList());

        if (content.getContentType() == ContentType.NOVEL) {
            LambdaQueryWrapper<NovelChapter> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NovelChapter::getContentId, contentId);
            wrapper.orderByAsc(NovelChapter::getChapterNumber);
            List<NovelChapter> chapters = novelChapterMapper.selectList(wrapper);

            boolean isVip = isUserVip(userId);
            Set<Long> unlockedChapterIds = getUnlockedChapterIds(userId, chapters);

            List<NovelChapterVO> chapterVOs = chapters.stream().map(ch -> {
                boolean accessible = isChapterAccessible(content, ch, userId, isVip, unlockedChapterIds);
                BigDecimal effectivePrice = getEffectiveChapterPrice(content, ch);
                return NovelChapterVO.fromChapter(ch, accessible, effectivePrice);
            }).toList();

            vo.setNovelChapterVOs(chapterVOs);
        }

        return vo;
    }

    private boolean isChapterAccessible(Content content, NovelChapter chapter,
                                        Long userId, boolean isVip, Set<Long> unlockedChapterIds) {
        if (!Boolean.TRUE.equals(content.getIsPaid())) {
            return true;
        }
        int freeCount = content.getFreeChapterCount() != null ? content.getFreeChapterCount() : 0;
        if (chapter.getChapterNumber() != null && chapter.getChapterNumber() <= freeCount) {
            return true;
        }
        if (isVip) {
            return true;
        }
        return unlockedChapterIds.contains(chapter.getId());
    }

    private BigDecimal getEffectiveChapterPrice(Content content, NovelChapter chapter) {
        if (chapter.getPrice() != null) {
            return chapter.getPrice();
        }
        return content.getDefaultChapterPrice();
    }

    private boolean isUserVip(Long userId) {
        if (userId == null) return false;
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        return user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(LocalDateTime.now());
    }

    private Set<Long> getUnlockedChapterIds(Long userId, List<NovelChapter> chapters) {
        if (userId == null || chapters.isEmpty()) return Collections.emptySet();
        List<Long> chapterIds = chapters.stream().map(NovelChapter::getId).toList();
        LambdaQueryWrapper<ChapterUnlock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChapterUnlock::getUserId, userId);
        wrapper.in(ChapterUnlock::getChapterId, chapterIds);
        List<ChapterUnlock> unlocks = chapterUnlockMapper.selectList(wrapper);
        Set<Long> set = new HashSet<>();
        for (ChapterUnlock u : unlocks) {
            set.add(u.getChapterId());
        }
        return set;
    }

    @Transactional
    public void unlockChapter(Long chapterId, Long userId) {
        NovelChapter chapter = novelChapterMapper.selectById(chapterId);
        if (chapter == null) {
            throw new EntityNotFoundException("章节", chapterId);
        }

        Content content = getContentById(chapter.getContentId());

        if (!Boolean.TRUE.equals(content.getIsPaid())) {
            throw new BusinessException(400, "该内容为免费内容，无需解锁");
        }

        int freeCount = content.getFreeChapterCount() != null ? content.getFreeChapterCount() : 0;
        if (chapter.getChapterNumber() != null && chapter.getChapterNumber() <= freeCount) {
            throw new BusinessException(400, "该章节为免费章节，无需解锁");
        }

        if (isUserVip(userId)) {
            throw new BusinessException(400, "VIP用户可免费阅读，无需解锁");
        }

        LambdaQueryWrapper<ChapterUnlock> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(ChapterUnlock::getUserId, userId);
        existWrapper.eq(ChapterUnlock::getChapterId, chapterId);
        if (chapterUnlockMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException(400, "该章节已解锁");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new EntityNotFoundException("用户", userId);
        }

        BigDecimal price = getEffectiveChapterPrice(content, chapter);
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            price = BigDecimal.ZERO;
        }

        if (price.compareTo(BigDecimal.ZERO) > 0 && user.getBalance().compareTo(price) < 0) {
            throw new InsufficientBalanceException("余额不足，请先充值");
        }

        if (price.compareTo(BigDecimal.ZERO) > 0) {
            user.setBalance(user.getBalance().subtract(price));
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
        }

        ChapterUnlock unlock = new ChapterUnlock();
        unlock.setUserId(userId);
        unlock.setChapterId(chapterId);
        unlock.setPricePaid(price);
        unlock.setUnlockedAt(LocalDateTime.now());
        chapterUnlockMapper.insert(unlock);
    }
}
