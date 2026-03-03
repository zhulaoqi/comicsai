package com.comicsai.model.vo;

import com.comicsai.model.entity.GenerationConfig;
import com.comicsai.model.entity.Storyline;
import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;

import java.time.LocalDateTime;

public class StorylineDetailVO {

    private Long id;
    private String title;
    private String genre;
    private ContentType contentType;
    private String characterSettings;
    private String worldview;
    private String plotOutline;
    private StorylineStatus status;
    private String latestChapterSummary;
    private Integer generatedCount;
    private GenerationConfigVO generationConfig;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public StorylineDetailVO() {}

    public static StorylineDetailVO fromStoryline(Storyline storyline) {
        return fromStoryline(storyline, null);
    }

    public static StorylineDetailVO fromStoryline(Storyline storyline, GenerationConfig config) {
        StorylineDetailVO vo = new StorylineDetailVO();
        vo.setId(storyline.getId());
        vo.setTitle(storyline.getTitle());
        vo.setGenre(storyline.getGenre());
        vo.setContentType(storyline.getContentType());
        vo.setCharacterSettings(storyline.getCharacterSettings());
        vo.setWorldview(storyline.getWorldview());
        vo.setPlotOutline(storyline.getPlotOutline());
        vo.setStatus(storyline.getStatus());
        vo.setLatestChapterSummary(storyline.getLatestChapterSummary());
        vo.setGeneratedCount(storyline.getGeneratedCount());
        vo.setGenerationConfig(GenerationConfigVO.fromEntity(config));
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

    public GenerationConfigVO getGenerationConfig() { return generationConfig; }
    public void setGenerationConfig(GenerationConfigVO generationConfig) { this.generationConfig = generationConfig; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
