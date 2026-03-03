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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ViewEventMapper viewEventMapper;
    private final ContentMapper contentMapper;
    private final TokenUsageMapper tokenUsageMapper;
    private final StorylineMapper storylineMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final ContentUnlockMapper contentUnlockMapper;
    private final UserMapper userMapper;

    public AnalyticsService(ViewEventMapper viewEventMapper, ContentMapper contentMapper,
                            TokenUsageMapper tokenUsageMapper, StorylineMapper storylineMapper,
                            RechargeRecordMapper rechargeRecordMapper, ContentUnlockMapper contentUnlockMapper,
                            UserMapper userMapper) {
        this.viewEventMapper = viewEventMapper;
        this.contentMapper = contentMapper;
        this.tokenUsageMapper = tokenUsageMapper;
        this.storylineMapper = storylineMapper;
        this.rechargeRecordMapper = rechargeRecordMapper;
        this.contentUnlockMapper = contentUnlockMapper;
        this.userMapper = userMapper;
    }

    public void recordViewEvent(Long contentId, Long userId) {
        if (contentId == null) {
            throw new BusinessException(400, "内容ID不能为空");
        }
        ViewEvent event = new ViewEvent();
        event.setContentId(contentId);
        event.setUserId(userId);
        event.setViewedAt(LocalDateTime.now());
        viewEventMapper.insert(event);
    }

    public void recordReadDuration(Long contentId, Long userId, Integer durationSeconds) {
        if (contentId == null) {
            throw new BusinessException(400, "内容ID不能为空");
        }
        if (durationSeconds == null || durationSeconds < 0) {
            throw new BusinessException(400, "阅读时长必须为非负整数");
        }

        // Find the most recent view event for this content+user to update duration
        LambdaQueryWrapper<ViewEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ViewEvent::getContentId, contentId);
        if (userId != null) {
            wrapper.eq(ViewEvent::getUserId, userId);
        } else {
            wrapper.isNull(ViewEvent::getUserId);
        }
        wrapper.isNull(ViewEvent::getDurationSeconds);
        wrapper.orderByDesc(ViewEvent::getViewedAt);
        wrapper.last("LIMIT 1");

        List<ViewEvent> events = viewEventMapper.selectList(wrapper);
        if (!events.isEmpty()) {
            ViewEvent event = events.get(0);
            event.setDurationSeconds(durationSeconds);
            viewEventMapper.updateById(event);
        } else {
            // No matching view event found, create a new one with duration
            ViewEvent event = new ViewEvent();
            event.setContentId(contentId);
            event.setUserId(userId);
            event.setDurationSeconds(durationSeconds);
            event.setViewedAt(LocalDateTime.now());
            viewEventMapper.insert(event);
        }
    }

    public UsageAnalyticsVO getUsageAnalytics(AnalyticsQueryDTO query) {
        // Build content filter set based on contentType and isPaid
        Set<Long> filteredContentIds = getFilteredContentIds(query.getContentType(), query.getIsPaid());

        // Build view event query with time range
        LambdaQueryWrapper<ViewEvent> eventWrapper = new LambdaQueryWrapper<>();
        if (query.getStartDate() != null) {
            eventWrapper.ge(ViewEvent::getViewedAt, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            eventWrapper.le(ViewEvent::getViewedAt, query.getEndDate().atTime(LocalTime.MAX));
        }
        if (filteredContentIds != null) {
            if (filteredContentIds.isEmpty()) {
                return buildEmptyAnalytics();
            }
            eventWrapper.in(ViewEvent::getContentId, filteredContentIds);
        }

        List<ViewEvent> events = viewEventMapper.selectList(eventWrapper);

        return buildUsageAnalytics(events, query.getContentType(), query.getIsPaid());
    }

    private Set<Long> getFilteredContentIds(ContentType contentType, Boolean isPaid) {
        if (contentType == null && isPaid == null) {
            return null; // No content filtering needed
        }
        QueryWrapper<Content> contentWrapper = new QueryWrapper<>();
        if (contentType != null) {
            contentWrapper.eq("content_type", contentType.getValue());
        }
        if (isPaid != null) {
            contentWrapper.eq("is_paid", isPaid);
        }
        List<Content> contents = contentMapper.selectList(contentWrapper);
        return contents.stream().map(Content::getId).collect(Collectors.toSet());
    }

    private UsageAnalyticsVO buildUsageAnalytics(List<ViewEvent> events, ContentType contentType, Boolean isPaid) {
        UsageAnalyticsVO analytics = new UsageAnalyticsVO();

        if (events.isEmpty()) {
            return buildEmptyAnalytics();
        }

        analytics.setTotalViews((long) events.size());

        // Unique viewers: count distinct non-null userIds
        long uniqueViewers = events.stream()
                .map(ViewEvent::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        analytics.setUniqueViewers(uniqueViewers);

        // Average duration: only from events that have duration recorded
        OptionalDouble avgDuration = events.stream()
                .map(ViewEvent::getDurationSeconds)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average();
        analytics.setAverageDurationSeconds(avgDuration.orElse(0.0));

        // Build per-content usage list
        Map<Long, List<ViewEvent>> eventsByContent = events.stream()
                .collect(Collectors.groupingBy(ViewEvent::getContentId));

        // Load content metadata for the content IDs
        Set<Long> contentIds = eventsByContent.keySet();
        Map<Long, Content> contentMap = loadContentMap(contentIds);

        List<ContentUsageVO> contentUsageList = new ArrayList<>();
        for (Map.Entry<Long, List<ViewEvent>> entry : eventsByContent.entrySet()) {
            Long contentId = entry.getKey();
            List<ViewEvent> contentEvents = entry.getValue();
            Content content = contentMap.get(contentId);

            ContentUsageVO usage = new ContentUsageVO();
            usage.setContentId(contentId);
            usage.setTitle(content != null ? content.getTitle() : "Unknown");
            usage.setContentType(content != null ? content.getContentType() : null);
            usage.setIsPaid(content != null ? content.getIsPaid() : null);
            usage.setTotalViews((long) contentEvents.size());

            long contentUniqueViewers = contentEvents.stream()
                    .map(ViewEvent::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            usage.setUniqueViewers(contentUniqueViewers);

            OptionalDouble contentAvgDuration = contentEvents.stream()
                    .map(ViewEvent::getDurationSeconds)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .average();
            usage.setAverageDurationSeconds(contentAvgDuration.orElse(0.0));

            contentUsageList.add(usage);
        }

        // Sort by total views descending
        contentUsageList.sort((a, b) -> Long.compare(b.getTotalViews(), a.getTotalViews()));
        analytics.setContentUsageList(contentUsageList);

        return analytics;
    }

    private Map<Long, Content> loadContentMap(Set<Long> contentIds) {
        if (contentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Content> contents = contentMapper.selectBatchIds(contentIds);
        return contents.stream().collect(Collectors.toMap(Content::getId, c -> c));
    }

    private UsageAnalyticsVO buildEmptyAnalytics() {
        UsageAnalyticsVO analytics = new UsageAnalyticsVO();
        analytics.setTotalViews(0L);
        analytics.setUniqueViewers(0L);
        analytics.setAverageDurationSeconds(0.0);
        analytics.setContentUsageList(Collections.emptyList());
        return analytics;
    }


    public TokenCostAnalyticsVO getTokenCostAnalytics(TokenCostQueryDTO query) {
        LambdaQueryWrapper<TokenUsage> wrapper = new LambdaQueryWrapper<>();
        if (query.getStartDate() != null) {
            wrapper.ge(TokenUsage::getCalledAt, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(TokenUsage::getCalledAt, query.getEndDate().atTime(LocalTime.MAX));
        }
        if (query.getProviderName() != null) {
            wrapper.eq(TokenUsage::getProviderName, query.getProviderName());
        }
        if (query.getStorylineId() != null) {
            wrapper.eq(TokenUsage::getStorylineId, query.getStorylineId());
        }

        List<TokenUsage> usages = tokenUsageMapper.selectList(wrapper);

        if (usages.isEmpty()) {
            return buildEmptyTokenCostAnalytics();
        }

        TokenCostAnalyticsVO vo = new TokenCostAnalyticsVO();

        // Totals
        int totalInput = usages.stream().mapToInt(u -> u.getInputTokens() != null ? u.getInputTokens() : 0).sum();
        int totalOutput = usages.stream().mapToInt(u -> u.getOutputTokens() != null ? u.getOutputTokens() : 0).sum();
        BigDecimal totalCost = usages.stream()
                .map(u -> u.getEstimatedCost() != null ? u.getEstimatedCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        vo.setTotalInputTokens(totalInput);
        vo.setTotalOutputTokens(totalOutput);
        vo.setTotalEstimatedCost(totalCost);

        // Provider/Model aggregation
        Map<String, List<TokenUsage>> byProviderModel = usages.stream()
                .collect(Collectors.groupingBy(u -> u.getProviderName() + "|" + u.getModelName()));
        List<ProviderModelCostVO> providerModelCosts = new ArrayList<>();
        for (Map.Entry<String, List<TokenUsage>> entry : byProviderModel.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            List<TokenUsage> group = entry.getValue();
            ProviderModelCostVO pm = new ProviderModelCostVO();
            pm.setProviderName(parts[0]);
            pm.setModelName(parts.length > 1 ? parts[1] : null);
            pm.setInputTokens(group.stream().mapToInt(u -> u.getInputTokens() != null ? u.getInputTokens() : 0).sum());
            pm.setOutputTokens(group.stream().mapToInt(u -> u.getOutputTokens() != null ? u.getOutputTokens() : 0).sum());
            pm.setEstimatedCost(group.stream().map(u -> u.getEstimatedCost() != null ? u.getEstimatedCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
            pm.setCallCount((long) group.size());
            providerModelCosts.add(pm);
        }
        providerModelCosts.sort((a, b) -> b.getEstimatedCost().compareTo(a.getEstimatedCost()));
        vo.setProviderModelCosts(providerModelCosts);

        // Storyline aggregation
        Map<Long, List<TokenUsage>> byStoryline = usages.stream()
                .filter(u -> u.getStorylineId() != null)
                .collect(Collectors.groupingBy(TokenUsage::getStorylineId));
        Set<Long> storylineIds = byStoryline.keySet();
        Map<Long, String> storylineTitleMap = loadStorylineTitleMap(storylineIds);
        List<StorylineCostVO> storylineCosts = new ArrayList<>();
        for (Map.Entry<Long, List<TokenUsage>> entry : byStoryline.entrySet()) {
            List<TokenUsage> group = entry.getValue();
            StorylineCostVO sc = new StorylineCostVO();
            sc.setStorylineId(entry.getKey());
            sc.setStorylineTitle(storylineTitleMap.getOrDefault(entry.getKey(), "Unknown"));
            sc.setInputTokens(group.stream().mapToInt(u -> u.getInputTokens() != null ? u.getInputTokens() : 0).sum());
            sc.setOutputTokens(group.stream().mapToInt(u -> u.getOutputTokens() != null ? u.getOutputTokens() : 0).sum());
            sc.setEstimatedCost(group.stream().map(u -> u.getEstimatedCost() != null ? u.getEstimatedCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
            sc.setCallCount((long) group.size());
            storylineCosts.add(sc);
        }
        storylineCosts.sort((a, b) -> b.getEstimatedCost().compareTo(a.getEstimatedCost()));
        vo.setStorylineCosts(storylineCosts);

        // Daily trend
        Map<LocalDate, List<TokenUsage>> byDate = usages.stream()
                .collect(Collectors.groupingBy(u -> u.getCalledAt().toLocalDate()));
        List<DailyTokenCostVO> dailyTrend = new ArrayList<>();
        for (Map.Entry<LocalDate, List<TokenUsage>> entry : byDate.entrySet()) {
            List<TokenUsage> group = entry.getValue();
            DailyTokenCostVO daily = new DailyTokenCostVO();
            daily.setDate(entry.getKey());
            daily.setInputTokens(group.stream().mapToInt(u -> u.getInputTokens() != null ? u.getInputTokens() : 0).sum());
            daily.setOutputTokens(group.stream().mapToInt(u -> u.getOutputTokens() != null ? u.getOutputTokens() : 0).sum());
            daily.setEstimatedCost(group.stream().map(u -> u.getEstimatedCost() != null ? u.getEstimatedCost() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add));
            daily.setCallCount((long) group.size());
            dailyTrend.add(daily);
        }
        dailyTrend.sort(Comparator.comparing(DailyTokenCostVO::getDate));
        vo.setDailyTrend(dailyTrend);

        return vo;
    }

    private Map<Long, String> loadStorylineTitleMap(Set<Long> storylineIds) {
        if (storylineIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Storyline> storylines = storylineMapper.selectBatchIds(storylineIds);
        return storylines.stream().collect(Collectors.toMap(Storyline::getId, Storyline::getTitle));
    }

    private TokenCostAnalyticsVO buildEmptyTokenCostAnalytics() {
        TokenCostAnalyticsVO vo = new TokenCostAnalyticsVO();
        vo.setTotalInputTokens(0);
        vo.setTotalOutputTokens(0);
        vo.setTotalEstimatedCost(BigDecimal.ZERO);
        vo.setProviderModelCosts(Collections.emptyList());
        vo.setStorylineCosts(Collections.emptyList());
        vo.setDailyTrend(Collections.emptyList());
        return vo;
    }

    public RechargeAnalyticsVO getRechargeAnalytics(RechargeQueryDTO query) {
        // Build recharge record query with time range
        LambdaQueryWrapper<RechargeRecord> rechargeWrapper = new LambdaQueryWrapper<>();
        if (query.getStartDate() != null) {
            rechargeWrapper.ge(RechargeRecord::getCreatedAt, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            rechargeWrapper.le(RechargeRecord::getCreatedAt, query.getEndDate().atTime(LocalTime.MAX));
        }

        List<RechargeRecord> records = rechargeRecordMapper.selectList(rechargeWrapper);

        if (records.isEmpty()) {
            return buildEmptyRechargeAnalytics();
        }

        RechargeAnalyticsVO vo = new RechargeAnalyticsVO();

        // Summary
        long totalCount = records.size();
        BigDecimal totalAmount = records.stream()
                .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageAmount = totalAmount.divide(BigDecimal.valueOf(totalCount), 2, java.math.RoundingMode.HALF_UP);

        vo.setTotalRechargeCount(totalCount);
        vo.setTotalRechargeAmount(totalAmount);
        vo.setAverageRechargeAmount(averageAmount);

        // Group recharge records by user
        Map<Long, List<RechargeRecord>> byUser = records.stream()
                .collect(Collectors.groupingBy(RechargeRecord::getUserId));

        // Load user info
        Set<Long> userIds = byUser.keySet();
        Map<Long, User> userMap = loadUserMap(userIds);

        // Build content unlock query with same time range for consumption stats
        LambdaQueryWrapper<ContentUnlock> unlockWrapper = new LambdaQueryWrapper<>();
        unlockWrapper.in(ContentUnlock::getUserId, userIds);
        if (query.getStartDate() != null) {
            unlockWrapper.ge(ContentUnlock::getUnlockedAt, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            unlockWrapper.le(ContentUnlock::getUnlockedAt, query.getEndDate().atTime(LocalTime.MAX));
        }
        List<ContentUnlock> unlocks = contentUnlockMapper.selectList(unlockWrapper);
        Map<Long, List<ContentUnlock>> unlocksByUser = unlocks.stream()
                .collect(Collectors.groupingBy(ContentUnlock::getUserId));

        // Build per-user recharge info
        List<RechargeUserVO> rechargeUsers = new ArrayList<>();
        for (Map.Entry<Long, List<RechargeRecord>> entry : byUser.entrySet()) {
            Long userId = entry.getKey();
            List<RechargeRecord> userRecords = entry.getValue();
            User user = userMap.get(userId);

            RechargeUserVO userVO = new RechargeUserVO();
            userVO.setUserId(userId);
            userVO.setNickname(user != null ? user.getNickname() : "Unknown");
            userVO.setEmail(user != null ? user.getEmail() : "Unknown");
            userVO.setRechargeCount((long) userRecords.size());
            userVO.setTotalRechargeAmount(userRecords.stream()
                    .map(r -> r.getAmount() != null ? r.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            List<ContentUnlock> userUnlocks = unlocksByUser.getOrDefault(userId, Collections.emptyList());
            userVO.setUnlockCount((long) userUnlocks.size());
            userVO.setTotalSpent(userUnlocks.stream()
                    .map(u -> u.getPricePaid() != null ? u.getPricePaid() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            rechargeUsers.add(userVO);
        }

        // Sort by total recharge amount descending
        rechargeUsers.sort((a, b) -> b.getTotalRechargeAmount().compareTo(a.getTotalRechargeAmount()));
        vo.setRechargeUsers(rechargeUsers);

        return vo;
    }

    private Map<Long, User> loadUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    private RechargeAnalyticsVO buildEmptyRechargeAnalytics() {
        RechargeAnalyticsVO vo = new RechargeAnalyticsVO();
        vo.setTotalRechargeCount(0L);
        vo.setTotalRechargeAmount(BigDecimal.ZERO);
        vo.setAverageRechargeAmount(BigDecimal.ZERO);
        vo.setRechargeUsers(Collections.emptyList());
        return vo;
    }

}
