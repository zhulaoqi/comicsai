package com.comicsai.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public class BatchReviewDTO {

    @NotEmpty(message = "内容ID列表不能为空")
    private List<Long> contentIds;

    @NotBlank(message = "审核操作不能为空")
    @Pattern(regexp = "(?i)approve|reject", message = "审核操作必须为 approve 或 reject")
    private String action;

    public BatchReviewDTO() {}

    public List<Long> getContentIds() { return contentIds; }
    public void setContentIds(List<Long> contentIds) { this.contentIds = contentIds; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
