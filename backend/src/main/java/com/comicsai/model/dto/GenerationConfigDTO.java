package com.comicsai.model.dto;

import jakarta.validation.constraints.NotBlank;

public class GenerationConfigDTO {

    @NotBlank(message = "文本AI提供商不能为空")
    private String textProvider;

    @NotBlank(message = "文本模型名称不能为空")
    private String textModel;

    private String imageProvider;
    private String imageModel;
    private Double temperature;
    private Integer maxTokens;
    private Integer chapterWordCount;
    private String imageStyle;
    private String imageSize;

    public GenerationConfigDTO() {}

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
}
