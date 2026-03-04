package com.comicsai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("content")
public class Content {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("storyline_id")
    private Long storylineId;

    @TableField("title")
    private String title;

    @TableField("content_type")
    private ContentType contentType;

    @TableField("status")
    private ContentStatus status;

    @TableField("cover_url")
    private String coverUrl;

    @TableField("description")
    private String description;

    @TableField("is_paid")
    private Boolean isPaid;

    @TableField("free_chapter_count")
    private Integer freeChapterCount;

    @TableField("default_chapter_price")
    private BigDecimal defaultChapterPrice;

    @TableField("price")
    private BigDecimal price;

    @TableField("generated_at")
    private LocalDateTime generatedAt;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Content() {}

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

    public Integer getFreeChapterCount() { return freeChapterCount; }
    public void setFreeChapterCount(Integer freeChapterCount) { this.freeChapterCount = freeChapterCount; }

    public BigDecimal getDefaultChapterPrice() { return defaultChapterPrice; }
    public void setDefaultChapterPrice(BigDecimal defaultChapterPrice) { this.defaultChapterPrice = defaultChapterPrice; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
