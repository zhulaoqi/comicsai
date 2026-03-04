package com.comicsai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("generation_config")
public class GenerationConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("storyline_id")
    private Long storylineId;

    @TableField("text_provider")
    private String textProvider;

    @TableField("text_model")
    private String textModel;

    @TableField("image_provider")
    private String imageProvider;

    @TableField("image_model")
    private String imageModel;

    @TableField("temperature")
    private Double temperature;

    @TableField("max_tokens")
    private Integer maxTokens;

    @TableField("chapter_word_count")
    private Integer chapterWordCount;

    @TableField("image_style")
    private String imageStyle;

    @TableField("image_size")
    private String imageSize;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public GenerationConfig() {}

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

    public Integer getChapterWordCount() { return chapterWordCount; }
    public void setChapterWordCount(Integer chapterWordCount) { this.chapterWordCount = chapterWordCount; }

    public String getImageStyle() { return imageStyle; }
    public void setImageStyle(String imageStyle) { this.imageStyle = imageStyle; }

    public String getImageSize() { return imageSize; }
    public void setImageSize(String imageSize) { this.imageSize = imageSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
