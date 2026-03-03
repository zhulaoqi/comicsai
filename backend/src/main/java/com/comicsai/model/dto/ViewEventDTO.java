package com.comicsai.model.dto;

import jakarta.validation.constraints.NotNull;

public class ViewEventDTO {

    @NotNull(message = "内容ID不能为空")
    private Long contentId;

    public ViewEventDTO() {}

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
}
