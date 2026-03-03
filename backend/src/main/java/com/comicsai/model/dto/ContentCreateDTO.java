package com.comicsai.model.dto;

import com.comicsai.model.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ContentCreateDTO {

    @NotNull(message = "故事线ID不能为空")
    private Long storylineId;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotNull(message = "内容类型不能为空")
    private ContentType contentType;

    @NotBlank(message = "封面URL不能为空")
    private String coverUrl;

    private String description;

    public ContentCreateDTO() {}

    public Long getStorylineId() { return storylineId; }
    public void setStorylineId(Long storylineId) { this.storylineId = storylineId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
