package com.comicsai.model.vo;

import com.comicsai.model.entity.Storyline;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;

import java.time.LocalDateTime;

public class StorylineVO {

    private Long id;
    private String title;
    private String genre;
    private ContentType contentType;
    private StorylineStatus status;
    private Integer generatedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StorylineVO() {}

    public static StorylineVO fromStoryline(Storyline storyline) {
        StorylineVO vo = new StorylineVO();
        vo.setId(storyline.getId());
        vo.setTitle(storyline.getTitle());
        vo.setGenre(storyline.getGenre());
        vo.setContentType(storyline.getContentType());
        vo.setStatus(storyline.getStatus());
        vo.setGeneratedCount(storyline.getGeneratedCount());
        vo.setCreatedAt(storyline.getCreatedAt());
        vo.setUpdatedAt(storyline.getUpdatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public StorylineStatus getStatus() { return status; }
    public void setStatus(StorylineStatus status) { this.status = status; }

    public Integer getGeneratedCount() { return generatedCount; }
    public void setGeneratedCount(Integer generatedCount) { this.generatedCount = generatedCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
