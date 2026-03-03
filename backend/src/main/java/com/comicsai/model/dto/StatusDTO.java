package com.comicsai.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class StatusDTO {

    @NotBlank(message = "操作不能为空")
    @Pattern(regexp = "(?i)online|offline", message = "操作必须为 online 或 offline")
    private String action;

    public StatusDTO() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
