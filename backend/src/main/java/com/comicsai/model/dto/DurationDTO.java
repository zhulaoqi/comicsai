package com.comicsai.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class DurationDTO {

    @NotNull(message = "内容ID不能为空")
    private Long contentId;

    @NotNull(message = "阅读时长不能为空")
    @Min(value = 0, message = "阅读时长不能为负数")
    private Integer durationSeconds;

    public DurationDTO() {}

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
}
