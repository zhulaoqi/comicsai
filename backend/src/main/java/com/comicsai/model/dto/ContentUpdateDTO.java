package com.comicsai.model.dto;

import jakarta.validation.constraints.NotBlank;

public class ContentUpdateDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String coverUrl;
    private String description;

    public ContentUpdateDTO() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
