package com.comicsai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.comicsai.common.exception.BusinessException;
import com.comicsai.mapper.*;
import com.comicsai.model.dto.AnalyticsQueryDTO;
import com.comicsai.model.dto.RechargeQueryDTO;
import com.comicsai.model.dto.TokenCostQueryDTO;
import com.comicsai.model.entity.*;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ViewEventMapper viewEventMapper;

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private TokenUsageMapper tokenUsageMapper;

    @Mock
    private StorylineMapper storylineMapper;

    @Mock
    private RechargeRecordMapper rechargeRecordMapper;

    @Mock
    private ContentUnlockMapper contentUnlockMapper;

    @Mock
    private UserMapper userMapper;

    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(viewEventMapper, contentMapper, tokenUsageMapper, storylineMapper,
                rechargeRecordMapper, contentUnlockMapper, userMapper);
    }

    // ==================== recordViewEvent Tests ====================

    @Test
    void recordViewEvent_shouldInsertEventWithUserIdAndTimestamp() {
        when(viewEventMapper.insert(any(ViewEvent.class))).thenReturn(1);

        analyticsService.recordViewEvent(1L, 100L);

        ArgumentCaptor<ViewEvent> captor = ArgumentCaptor.forClass(ViewEvent.class);
        verify(viewEventMapper).insert(captor.capture());
        ViewEvent event = captor.getValue();
        assertEquals(1L, event.getContentId());
        assertEquals(100L, event.getUserId());
        assertNotNull(event.getViewedAt());
        assertNull(event.getDurationSeconds());
    }

    @Test
    void recordViewEvent_shouldAllowNullUserId_forAnonymousUsers() {
        when(viewEventMapper.insert(any(ViewEvent.class))).thenReturn(1);

        analyticsService.recordViewEvent(5L, null);

        ArgumentCaptor<ViewEvent> captor = ArgumentCaptor.forClass(ViewEvent.class);
        verify(viewEventMapper).insert(captor.capture());
        ViewEvent event = captor.getValue();
        assertEquals(5L, event.getContentId());
        assertNull(event.getUserId());
        assertNotNull(event.getViewedAt());
    }

    @Test
    void recordViewEvent_shouldThrowWhenContentIdIsNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> analyticsService.recordViewEvent(null, 1L));
        assertEquals(400, ex.getCode());
    }

    // ==================== recordReadDuration Tests ====================

    @Test
    void recordReadDuration_shouldUpdateExistingEventWithDuration() {
        ViewEvent existing = buildViewEvent(1L, 10L, 100L, null);
        when(viewEventMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(existing));
        when(viewEventMapper.updateById(any(ViewEvent.class))).thenReturn(1);

        analyticsService.recordReadDuration(10L, 100L, 120);

        ArgumentCaptor<ViewEvent> captor = ArgumentCaptor.forClass(ViewEvent.class);
        verify(viewEventMapper).updateById(captor.capture());
        assertEquals(120, captor.getValue().getDurationSeconds());
    }

    @Test
    void recordReadDuration_shouldCreateNewEventWhenNoMatchingViewEvent() {
        when(viewEventMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(viewEventMapper.insert(any(ViewEvent.class))).thenReturn(1);

        analyticsService.recordReadDuration(10L, 100L, 60);

        ArgumentCaptor<ViewEvent> captor = ArgumentCaptor.forClass(ViewEvent.class);
        verify(viewEventMapper).insert(captor.capture());
        ViewEvent event = captor.getValue();
        assertEquals(10L, event.getContentId());
        assertEquals(100L, event.getUserId());
        assertEquals(60, event.getDurationSeconds());
        assertNotNull(event.getViewedAt());
    }

    @Test
    void recordReadDuration_shouldThrowWhenContentIdIsNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> analyticsService.recordReadDuration(null, 1L, 60));
        assertEquals(400, ex.getCode());
    }

    @Test
    void recordReadDuration_shouldThrowWhenDurationIsNull() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> analyticsService.recordReadDuration(1L, 1L, null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void recordReadDuration_shouldThrowWhenDurationIsNegative() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> analyticsService.recordReadDuration(1L, 1L, -5));
        assertEquals(400, ex.getCode());
    }

    @Test
    void recordReadDuration_shouldAcceptZeroDuration() {
        ViewEvent existing = buildViewEvent(1L, 10L, 100L, null);
        when(viewEventMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(existing));
        when(viewEventMapper.updateById(any(ViewEvent.class))).thenReturn(1);

        analyticsService.recordReadDuration(10L, 100L, 0);

        ArgumentCaptor<ViewEvent> captor = ArgumentCaptor.forClass(ViewEvent.class);
        verify(viewEventMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getDurationSeconds());
    }

    // ==================== getUsageAnalytics Tests ====================

    @Test
    void getUsageAnalytics_shouldReturnEmptyWhenNoEvents() {
        when(viewEventMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        AnalyticsQueryDTO query = new AnalyticsQueryDTO();
        UsageAnalyticsVO result = analyticsService.getUsageAnalytics(query);

        assertEquals(0L, result.getTotalViews());
        assertEquals(0L, result.getUniqueViewers());
        assertEquals(0.0, result.getAverageDurationSeconds());
        assertTrue(result.getContentUsageList().isEmpty());
    }

    @Test
    void getUsageAnalytics_shouldCalculateTotalViewsAndUniqueViewers() {
        List<ViewEvent> events = List.of(
                buildViewEvent(1L, 10L, 100L, 30),
                buildViewEvent(2L, 10L, 200L, 60),
                buildViewEvent(3L, 10L, 100L, 45)  // same user as event 1
        );
        when(viewEventMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(events);

        Content content = buildContent(10L, "Test Comic", ContentType.COMIC, false);
        when(contentMapper.selectBatchIds(any())).thenReturn(List.of(content));

        AnalyticsQueryDTO query = new AnalyticsQueryDTO();
        UsageAnalyticsVO result = analyticsService.getUsageAnalytics(query);

        assertEquals(3L, result.getTotalViews());
        assertEquals(2L, result.getUniqueViewers()); // userId 100 and 200
        assertEquals(45.0, result.getAverageDurationSeconds()); // (30+60+45)/3
    }

    @Test
    void getUsageAnalytics_shouldHandleAnonymousViewersInUniqueCount() {
        List<ViewEvent> events = List.of(
                buildViewEvent(1L, 10L, null, 30),   // anonymous
                buildViewEvent(2L, 10L, 100L, 60),
                buildViewEvent(3L, 10L, null, 45)    // anonymous
        );
        when(viewEventMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(events);

        Content content = buildContent(10L, "Test", ContentType.COMIC, false);
        when(contentMapper.selectBatchIds(any())).thenReturn(List.of(content));

        AnalyticsQueryDTO query = new AnalyticsQueryDTO();
        UsageAnalyticsVO result = analyticsService.getUsageAnalytics(query);

        assertEquals(3L, result.getTotalViews());
        assertEquals(1L, result.getUniqueViewers()); // only userId 100 is non-null unique
    }

    @Test
    void getUsageAnalytics_shouldCalculateAverageDurationOnlyFromEventsWithDuration() {
        List<ViewEvent> events = List.of(
                buildViewEvent(1L, 10L, 100L, 60),
                buildViewEvent(2L, 10L, 200L, null),  // no duration
                buildViewEvent(3L, 10L, 300L, 120)
        );
        when(viewEventMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(events);

        Content content = buildContent(10L, "Test", ContentType.NOVEL, true);
        when(contentMapper.selectBatchIds(any())).thenReturn(List.of(content));

        AnalyticsQueryDTO query = new AnalyticsQueryDTO();
        UsageAnalyticsVO result = analyticsService.getUsageAnalytics(query);

        assertEquals(3L, result.getTotalViews());
        assertEquals(90.0, result.getAverageDurationSeconds()); // (60+120)/2
    }

    @Test
    void getUsageAnalytics_shouldReturnPerContentBreakdown() {
        List<ViewEvent> events = List.of(
                buildViewEvent(1L, 10L, 100L, 30),
                buildViewEvent(2L, 10L, 200L, 60),
                buildViewEvent(3L, 20L, 100L, 90)
        );
        when(viewEventMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(events);

        Content content10 = buildContent(10L, "Comic A", ContentType.COMIC, false);
        Content content20 = buildContent(20L, "Novel B", ContentType.NOVEL, true);
        when(contentMapper.selectBatchIds(any())).thenReturn(List.of(content10, content20));

        AnalyticsQueryDTO query = new AnalyticsQueryDTO();
        UsageAnalyticsVO result = analyticsService.getUsageAnalytics(query);

        assertEquals(2, result.getContentUsageList().size());

        // Content 10 has 2 views, content 20 has 1 view - sorted by views desc
        ContentUsageVO first = result.getContentUsageList().get(0);
        assertEquals(10L, first.getContentId());
        assertEquals(2L, first.getTotalViews());
        assertEquals(2L, first.getUniqueViewers());
        assertEquals("Comic A", first.getTitle());

        ContentUsageVO second = result.getContentUsageList().get(1);
        assertEquals(20L, second.getContentId());
        assertEquals(1L, second.getTotalViews());
    }

    @Test
    void getUsageAnalytics_shouldFilterByContentType() {
        Content comicContent = buildContent(10L, "Comic", ContentType.COMIC, false);
        when(contentMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(comicContent));

        List<ViewEvent> events = List.of(
                buildViewEvent(1L, 10L, 100L, 30)
        );
        when(viewEventMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(events);
        when(contentMapper.selectBatchIds(any())).thenReturn(List.of(comicContent));

        AnalyticsQueryDTO query = new AnalyticsQueryDTO();
        query.setContentType(ContentType.COMIC);
        UsageAnalyticsVO result = analyticsService.getUsageAnalytics(query);

        assertEquals(1L, result.getTotalViews());
    }

    @Test
    void getUsageAnalytics_shouldReturnEmptyWhenNoContentMatchesFilter() {
        when(contentMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        AnalyticsQueryDTO query = new AnalyticsQueryDTO();
        query.setIsPaid(true);
        UsageAnalyticsVO result = analyticsService.getUsageAnalytics(query);

        assertEquals(0L, result.getTotalViews());
        assertEquals(0L, result.getUniqueViewers());
        assertTrue(result.getContentUsageList().isEmpty());
    }

    // ==================== Helpers ====================

    private ViewEvent buildViewEvent(Long id, Long contentId, Long userId, Integer durationSeconds) {
        ViewEvent event = new ViewEvent();
        event.setId(id);
        event.setContentId(contentId);
        event.setUserId(userId);
        event.setDurationSeconds(durationSeconds);
        event.setViewedAt(LocalDateTime.now());
        return event;
    }

    private Content buildContent(Long id, String title, ContentType type, boolean isPaid) {
        Content content = new Content();
        content.setId(id);
        content.setTitle(title);
        content.setContentType(type);
        content.setIsPaid(isPaid);
        return content;
    }

    // ==================== getTokenCostAnalytics Tests ====================

    @Test
    void getTokenCostAnalytics_shouldReturnEmptyWhenNoData() {
        when(tokenUsageMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        TokenCostQueryDTO query = new TokenCostQueryDTO();
        TokenCostAnalyticsVO result = analyticsService.getTokenCostAnalytics(query);

        assertEquals(0, result.getTotalInputTokens());
        assertEquals(0, result.getTotalOutputTokens());
        assertEquals(BigDecimal.ZERO, result.getTotalEstimatedCost());
        assertTrue(result.getProviderModelCosts().isEmpty());
        assertTrue(result.getStorylineCosts().isEmpty());
        assertTrue(result.getDailyTrend().isEmpty());
    }

    @Test
    void getTokenCostAnalytics_shouldAggregateTotalsCorrectly() {
        TokenUsage u1 = buildTokenUsage(1L, "OpenAI", "gpt-4", 100, 50, new BigDecimal("0.10"), 1L, LocalDateTime.of(2024, 1, 15, 10, 0));
        TokenUsage u2 = buildTokenUsage(2L, "OpenAI", "gpt-4", 200, 100, new BigDecimal("0.20"), 1L, LocalDateTime.of(2024, 1, 15, 11, 0));
        when(tokenUsageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(u1, u2));
        when(storylineMapper.selectBatchIds(any())).thenReturn(List.of(buildStoryline(1L, "Story A")));

        TokenCostQueryDTO query = new TokenCostQueryDTO();
        TokenCostAnalyticsVO result = analyticsService.getTokenCostAnalytics(query);

        assertEquals(300, result.getTotalInputTokens());
        assertEquals(150, result.getTotalOutputTokens());
        assertEquals(new BigDecimal("0.30"), result.getTotalEstimatedCost());
    }

    @Test
    void getTokenCostAnalytics_shouldGroupByProviderAndModel() {
        TokenUsage u1 = buildTokenUsage(1L, "OpenAI", "gpt-4", 100, 50, new BigDecimal("0.10"), 1L, LocalDateTime.of(2024, 1, 15, 10, 0));
        TokenUsage u2 = buildTokenUsage(2L, "Qwen", "qwen-max", 200, 100, new BigDecimal("0.05"), 1L, LocalDateTime.of(2024, 1, 15, 11, 0));
        when(tokenUsageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(u1, u2));
        when(storylineMapper.selectBatchIds(any())).thenReturn(List.of(buildStoryline(1L, "Story A")));

        TokenCostQueryDTO query = new TokenCostQueryDTO();
        TokenCostAnalyticsVO result = analyticsService.getTokenCostAnalytics(query);

        assertEquals(2, result.getProviderModelCosts().size());
        ProviderModelCostVO first = result.getProviderModelCosts().get(0);
        assertEquals("OpenAI", first.getProviderName());
        assertEquals("gpt-4", first.getModelName());
        assertEquals(1L, first.getCallCount());
    }

    @Test
    void getTokenCostAnalytics_shouldGroupByStoryline() {
        TokenUsage u1 = buildTokenUsage(1L, "OpenAI", "gpt-4", 100, 50, new BigDecimal("0.10"), 1L, LocalDateTime.of(2024, 1, 15, 10, 0));
        TokenUsage u2 = buildTokenUsage(2L, "OpenAI", "gpt-4", 200, 100, new BigDecimal("0.20"), 2L, LocalDateTime.of(2024, 1, 15, 11, 0));
        when(tokenUsageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(u1, u2));
        when(storylineMapper.selectBatchIds(any())).thenReturn(List.of(buildStoryline(1L, "Story A"), buildStoryline(2L, "Story B")));

        TokenCostQueryDTO query = new TokenCostQueryDTO();
        TokenCostAnalyticsVO result = analyticsService.getTokenCostAnalytics(query);

        assertEquals(2, result.getStorylineCosts().size());
        assertEquals("Story B", result.getStorylineCosts().get(0).getStorylineTitle());
        assertEquals("Story A", result.getStorylineCosts().get(1).getStorylineTitle());
    }

    @Test
    void getTokenCostAnalytics_shouldBuildDailyTrendSortedByDate() {
        TokenUsage u1 = buildTokenUsage(1L, "OpenAI", "gpt-4", 100, 50, new BigDecimal("0.10"), 1L, LocalDateTime.of(2024, 1, 16, 10, 0));
        TokenUsage u2 = buildTokenUsage(2L, "OpenAI", "gpt-4", 200, 100, new BigDecimal("0.20"), 1L, LocalDateTime.of(2024, 1, 15, 11, 0));
        when(tokenUsageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(u1, u2));
        when(storylineMapper.selectBatchIds(any())).thenReturn(List.of(buildStoryline(1L, "Story A")));

        TokenCostQueryDTO query = new TokenCostQueryDTO();
        TokenCostAnalyticsVO result = analyticsService.getTokenCostAnalytics(query);

        assertEquals(2, result.getDailyTrend().size());
        assertEquals(LocalDate.of(2024, 1, 15), result.getDailyTrend().get(0).getDate());
        assertEquals(LocalDate.of(2024, 1, 16), result.getDailyTrend().get(1).getDate());
    }

    @Test
    void getTokenCostAnalytics_shouldFilterByDateRange() {
        when(tokenUsageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        TokenCostQueryDTO query = new TokenCostQueryDTO();
        query.setStartDate(LocalDate.of(2024, 1, 1));
        query.setEndDate(LocalDate.of(2024, 1, 31));
        analyticsService.getTokenCostAnalytics(query);

        verify(tokenUsageMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void getTokenCostAnalytics_shouldExcludeNullStorylineFromStorylineAggregation() {
        TokenUsage u1 = buildTokenUsage(1L, "OpenAI", "gpt-4", 100, 50, new BigDecimal("0.10"), null, LocalDateTime.of(2024, 1, 15, 10, 0));
        when(tokenUsageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(u1));

        TokenCostQueryDTO query = new TokenCostQueryDTO();
        TokenCostAnalyticsVO result = analyticsService.getTokenCostAnalytics(query);

        assertTrue(result.getStorylineCosts().isEmpty());
        assertEquals(100, result.getTotalInputTokens());
    }

    private TokenUsage buildTokenUsage(Long id, String provider, String model, Integer inputTokens,
                                        Integer outputTokens, BigDecimal cost, Long storylineId, LocalDateTime calledAt) {
        TokenUsage usage = new TokenUsage();
        usage.setId(id);
        usage.setProviderName(provider);
        usage.setModelName(model);
        usage.setInputTokens(inputTokens);
        usage.setOutputTokens(outputTokens);
        usage.setEstimatedCost(cost);
        usage.setStorylineId(storylineId);
        usage.setCalledAt(calledAt);
        return usage;
    }

    private Storyline buildStoryline(Long id, String title) {
        Storyline s = new Storyline();
        s.setId(id);
        s.setTitle(title);
        return s;
    }

    // ==================== getRechargeAnalytics Tests ====================

    @Test
    void getRechargeAnalytics_shouldReturnEmptyWhenNoRecords() {
        when(rechargeRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        RechargeQueryDTO query = new RechargeQueryDTO();
        RechargeAnalyticsVO result = analyticsService.getRechargeAnalytics(query);

        assertEquals(0L, result.getTotalRechargeCount());
        assertEquals(BigDecimal.ZERO, result.getTotalRechargeAmount());
        assertEquals(BigDecimal.ZERO, result.getAverageRechargeAmount());
        assertTrue(result.getRechargeUsers().isEmpty());
    }

    @Test
    void getRechargeAnalytics_shouldCalculateSummaryCorrectly() {
        List<RechargeRecord> records = List.of(
                buildRechargeRecord(1L, 100L, new BigDecimal("50.00")),
                buildRechargeRecord(2L, 100L, new BigDecimal("30.00")),
                buildRechargeRecord(3L, 200L, new BigDecimal("100.00"))
        );
        when(rechargeRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(records);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                buildUser(100L, "Alice", "alice@test.com"),
                buildUser(200L, "Bob", "bob@test.com")
        ));
        when(contentUnlockMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        RechargeQueryDTO query = new RechargeQueryDTO();
        RechargeAnalyticsVO result = analyticsService.getRechargeAnalytics(query);

        assertEquals(3L, result.getTotalRechargeCount());
        assertEquals(new BigDecimal("180.00"), result.getTotalRechargeAmount());
        assertEquals(new BigDecimal("60.00"), result.getAverageRechargeAmount());
    }

    @Test
    void getRechargeAnalytics_shouldBuildPerUserRechargeList() {
        List<RechargeRecord> records = List.of(
                buildRechargeRecord(1L, 100L, new BigDecimal("50.00")),
                buildRechargeRecord(2L, 100L, new BigDecimal("30.00")),
                buildRechargeRecord(3L, 200L, new BigDecimal("100.00"))
        );
        when(rechargeRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(records);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                buildUser(100L, "Alice", "alice@test.com"),
                buildUser(200L, "Bob", "bob@test.com")
        ));
        when(contentUnlockMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        RechargeQueryDTO query = new RechargeQueryDTO();
        RechargeAnalyticsVO result = analyticsService.getRechargeAnalytics(query);

        assertEquals(2, result.getRechargeUsers().size());
        // Sorted by total recharge amount desc: Bob(100) > Alice(80)
        RechargeUserVO first = result.getRechargeUsers().get(0);
        assertEquals(200L, first.getUserId());
        assertEquals("Bob", first.getNickname());
        assertEquals(1L, first.getRechargeCount());
        assertEquals(new BigDecimal("100.00"), first.getTotalRechargeAmount());

        RechargeUserVO second = result.getRechargeUsers().get(1);
        assertEquals(100L, second.getUserId());
        assertEquals("Alice", second.getNickname());
        assertEquals(2L, second.getRechargeCount());
        assertEquals(new BigDecimal("80.00"), second.getTotalRechargeAmount());
    }

    @Test
    void getRechargeAnalytics_shouldIncludeContentUnlockStats() {
        List<RechargeRecord> records = List.of(
                buildRechargeRecord(1L, 100L, new BigDecimal("50.00"))
        );
        when(rechargeRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(records);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(buildUser(100L, "Alice", "alice@test.com")));

        List<ContentUnlock> unlocks = List.of(
                buildContentUnlock(1L, 100L, 10L, new BigDecimal("5.00")),
                buildContentUnlock(2L, 100L, 20L, new BigDecimal("10.00"))
        );
        when(contentUnlockMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(unlocks);

        RechargeQueryDTO query = new RechargeQueryDTO();
        RechargeAnalyticsVO result = analyticsService.getRechargeAnalytics(query);

        assertEquals(1, result.getRechargeUsers().size());
        RechargeUserVO userVO = result.getRechargeUsers().get(0);
        assertEquals(2L, userVO.getUnlockCount());
        assertEquals(new BigDecimal("15.00"), userVO.getTotalSpent());
    }

    @Test
    void getRechargeAnalytics_shouldFilterByDateRange() {
        when(rechargeRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        RechargeQueryDTO query = new RechargeQueryDTO();
        query.setStartDate(LocalDate.of(2024, 1, 1));
        query.setEndDate(LocalDate.of(2024, 1, 31));
        analyticsService.getRechargeAnalytics(query);

        verify(rechargeRecordMapper).selectList(any(LambdaQueryWrapper.class));
    }

    private RechargeRecord buildRechargeRecord(Long id, Long userId, BigDecimal amount) {
        RechargeRecord record = new RechargeRecord();
        record.setId(id);
        record.setUserId(userId);
        record.setAmount(amount);
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }

    private User buildUser(Long id, String nickname, String email) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setEmail(email);
        return user;
    }

    private ContentUnlock buildContentUnlock(Long id, Long userId, Long contentId, BigDecimal pricePaid) {
        ContentUnlock unlock = new ContentUnlock();
        unlock.setId(id);
        unlock.setUserId(userId);
        unlock.setContentId(contentId);
        unlock.setPricePaid(pricePaid);
        unlock.setUnlockedAt(LocalDateTime.now());
        return unlock;
    }
}
