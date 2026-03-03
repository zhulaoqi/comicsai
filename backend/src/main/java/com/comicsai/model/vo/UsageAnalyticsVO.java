package com.comicsai.model.vo;

import java.util.List;

public class UsageAnalyticsVO {

    private Long totalViews;
    private Long uniqueViewers;
    private Double averageDurationSeconds;
    private List<ContentUsageVO> contentUsageList;

    public UsageAnalyticsVO() {}

    public Long getTotalViews() { return totalViews; }
    public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }

    public Long getUniqueViewers() { return uniqueViewers; }
    public void setUniqueViewers(Long uniqueViewers) { this.uniqueViewers = uniqueViewers; }

    public Double getAverageDurationSeconds() { return averageDurationSeconds; }
    public void setAverageDurationSeconds(Double averageDurationSeconds) { this.averageDurationSeconds = averageDurationSeconds; }

    public List<ContentUsageVO> getContentUsageList() { return contentUsageList; }
    public void setContentUsageList(List<ContentUsageVO> contentUsageList) { this.contentUsageList = contentUsageList; }
}
