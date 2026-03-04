package com.comicsai.model.vo;

import com.comicsai.model.entity.Content;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ContentManageVO {

    private Long id;
    private Long storylineId;
    private String title;
    private ContentType contentType;
    private ContentStatus status;
    private String coverUrl;
    private String description;
    private Boolean isPaid;
    private BigDecimal price;
    private Integer freeChapterCount;
    private BigDecimal defaultChapterPrice;
    private LocalDateTime generatedAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ContentManageVO() {}

    public static ContentManageVO fromContent(Content content) {
        ContentManageVO vo = new ContentManageVO();
        vo.setId(content.getId());
        vo.setStorylineId(content.getStorylineId());
        vo.setTitle(content.getTitle());
        vo.setContentType(content.getContentType());
        vo.setStatus(content.getStatus());
        vo.setCoverUrl(content.getCoverUrl());
        vo.setDescription(content.getDescription());
        vo.setIsPaid(content.getIsPaid());
        vo.setPrice(content.getPrice());
        vo.setFreeChapterCount(content.getFreeChapterCount());
        vo.setDefaultChapterPrice(content.getDefaultChapterPrice());
        vo.setGeneratedAt(content.getGeneratedAt());
        vo.setPublishedAt(content.getPublishedAt());
        vo.setCreatedAt(content.getCreatedAt());
        vo.setUpdatedAt(content.getUpdatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStorylineId() { return storylineId; }
    public void setStorylineId(Long storylineId) { this.storylineId = storylineId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getFreeChapterCount() { return freeChapterCount; }
    public void setFreeChapterCount(Integer freeChapterCount) { this.freeChapterCount = freeChapterCount; }

    public BigDecimal getDefaultChapterPrice() { return defaultChapterPrice; }
    public void setDefaultChapterPrice(BigDecimal defaultChapterPrice) { this.defaultChapterPrice = defaultChapterPrice; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
