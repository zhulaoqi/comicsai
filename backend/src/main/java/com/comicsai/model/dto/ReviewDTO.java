package com.comicsai.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ReviewDTO {

    @NotBlank(message = "审核操作不能为空")
    @Pattern(regexp = "(?i)approve|reject", message = "审核操作必须为 approve 或 reject")
    private String action;

    public ReviewDTO() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
