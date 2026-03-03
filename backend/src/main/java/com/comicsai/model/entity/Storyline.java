package com.comicsai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;

import java.time.LocalDateTime;

@TableName("storyline")
public class Storyline {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("genre")
    private String genre;

    @TableField("content_type")
    private ContentType contentType;

    @TableField("character_settings")
    private String characterSettings;

    @TableField("worldview")
    private String worldview;

    @TableField("plot_outline")
    private String plotOutline;

    @TableField("status")
    private StorylineStatus status;

    @TableField("latest_chapter_summary")
    private String latestChapterSummary;

    @TableField("generated_count")
    private Integer generatedCount;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Storyline() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public String getCharacterSettings() { return characterSettings; }
    public void setCharacterSettings(String characterSettings) { this.characterSettings = characterSettings; }

    public String getWorldview() { return worldview; }
    public void setWorldview(String worldview) { this.worldview = worldview; }

    public String getPlotOutline() { return plotOutline; }
    public void setPlotOutline(String plotOutline) { this.plotOutline = plotOutline; }

    public StorylineStatus getStatus() { return status; }
    public void setStatus(StorylineStatus status) { this.status = status; }

    public String getLatestChapterSummary() { return latestChapterSummary; }
    public void setLatestChapterSummary(String latestChapterSummary) { this.latestChapterSummary = latestChapterSummary; }

    public Integer getGeneratedCount() { return generatedCount; }
    public void setGeneratedCount(Integer generatedCount) { this.generatedCount = generatedCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
