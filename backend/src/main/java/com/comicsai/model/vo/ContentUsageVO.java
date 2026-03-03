package com.comicsai.model.vo;

import com.comicsai.model.enums.ContentType;

public class ContentUsageVO {

    private Long contentId;
    private String title;
    private ContentType contentType;
    private Boolean isPaid;
    private Long totalViews;
    private Long uniqueViewers;
    private Double averageDurationSeconds;

    public ContentUsageVO() {}

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

    public Long getTotalViews() { return totalViews; }
    public void setTotalViews(Long totalViews) { this.totalViews = totalViews; }

    public Long getUniqueViewers() { return uniqueViewers; }
    public void setUniqueViewers(Long uniqueViewers) { this.uniqueViewers = uniqueViewers; }

    public Double getAverageDurationSeconds() { return averageDurationSeconds; }
    public void setAverageDurationSeconds(Double averageDurationSeconds) { this.averageDurationSeconds = averageDurationSeconds; }
}
