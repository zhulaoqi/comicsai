package com.comicsai.model.vo;

import java.math.BigDecimal;

public class StorylineCostVO {

    private Long storylineId;
    private String storylineTitle;
    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal estimatedCost;
    private Long callCount;

    public StorylineCostVO() {}

    public Long getStorylineId() { return storylineId; }
    public void setStorylineId(Long storylineId) { this.storylineId = storylineId; }

    public String getStorylineTitle() { return storylineTitle; }
    public void setStorylineTitle(String storylineTitle) { this.storylineTitle = storylineTitle; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public Long getCallCount() { return callCount; }
    public void setCallCount(Long callCount) { this.callCount = callCount; }
}
