package com.comicsai.model.vo;

import com.comicsai.model.entity.Content;
import com.comicsai.model.enums.ContentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ContentVO {

    private Long id;
    private String title;
    private ContentType contentType;
    private String coverUrl;
    private String description;
    private Boolean isPaid;
    private BigDecimal price;
    private LocalDateTime publishedAt;

    public ContentVO() {}

    public static ContentVO fromContent(Content content) {
        ContentVO vo = new ContentVO();
        vo.setId(content.getId());
        vo.setTitle(content.getTitle());
        vo.setContentType(content.getContentType());
        vo.setCoverUrl(content.getCoverUrl());
        vo.setDescription(content.getDescription());
        vo.setIsPaid(content.getIsPaid());
        vo.setPrice(content.getPrice());
        vo.setPublishedAt(content.getPublishedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
}
