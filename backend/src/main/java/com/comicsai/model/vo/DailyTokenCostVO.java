package com.comicsai.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DailyTokenCostVO {

    private LocalDate date;
    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal estimatedCost;
    private Long callCount;

    public DailyTokenCostVO() {}

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public Long getCallCount() { return callCount; }
    public void setCallCount(Long callCount) { this.callCount = callCount; }
}
