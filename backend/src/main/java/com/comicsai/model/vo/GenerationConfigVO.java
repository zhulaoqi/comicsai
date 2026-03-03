package com.comicsai.model.vo;

import com.comicsai.model.entity.GenerationConfig;

import java.time.LocalDateTime;

public class GenerationConfigVO {

    private Long id;
    private Long storylineId;
    private String textProvider;
    private String textModel;
    private String imageProvider;
    private String imageModel;
    private Double temperature;
    private Integer maxTokens;
    private String imageStyle;
    private String imageSize;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public GenerationConfigVO() {}

    public static GenerationConfigVO fromEntity(GenerationConfig config) {
        if (config == null) return null;
        GenerationConfigVO vo = new GenerationConfigVO();
        vo.setId(config.getId());
        vo.setStorylineId(config.getStorylineId());
        vo.setTextProvider(config.getTextProvider());
        vo.setTextModel(config.getTextModel());
        vo.setImageProvider(config.getImageProvider());
        vo.setImageModel(config.getImageModel());
        vo.setTemperature(config.getTemperature());
        vo.setMaxTokens(config.getMaxTokens());
        vo.setImageStyle(config.getImageStyle());
        vo.setImageSize(config.getImageSize());
        vo.setCreatedAt(config.getCreatedAt());
        vo.setUpdatedAt(config.getUpdatedAt());
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStorylineId() { return storylineId; }
    public void setStorylineId(Long storylineId) { this.storylineId = storylineId; }

    public String getTextProvider() { return textProvider; }
    public void setTextProvider(String textProvider) { this.textProvider = textProvider; }

    public String getTextModel() { return textModel; }
    public void setTextModel(String textModel) { this.textModel = textModel; }

    public String getImageProvider() { return imageProvider; }
    public void setImageProvider(String imageProvider) { this.imageProvider = imageProvider; }

    public String getImageModel() { return imageModel; }
    public void setImageModel(String imageModel) { this.imageModel = imageModel; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public String getImageStyle() { return imageStyle; }
    public void setImageStyle(String imageStyle) { this.imageStyle = imageStyle; }

    public String getImageSize() { return imageSize; }
    public void setImageSize(String imageSize) { this.imageSize = imageSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
