package com.comicsai.model.vo;

import java.math.BigDecimal;
import java.util.List;

public class TokenCostAnalyticsVO {

    private Integer totalInputTokens;
    private Integer totalOutputTokens;
    private BigDecimal totalEstimatedCost;
    private List<ProviderModelCostVO> providerModelCosts;
    private List<StorylineCostVO> storylineCosts;
    private List<DailyTokenCostVO> dailyTrend;

    public TokenCostAnalyticsVO() {}

    public Integer getTotalInputTokens() { return totalInputTokens; }
    public void setTotalInputTokens(Integer totalInputTokens) { this.totalInputTokens = totalInputTokens; }

    public Integer getTotalOutputTokens() { return totalOutputTokens; }
    public void setTotalOutputTokens(Integer totalOutputTokens) { this.totalOutputTokens = totalOutputTokens; }

    public BigDecimal getTotalEstimatedCost() { return totalEstimatedCost; }
    public void setTotalEstimatedCost(BigDecimal totalEstimatedCost) { this.totalEstimatedCost = totalEstimatedCost; }

    public List<ProviderModelCostVO> getProviderModelCosts() { return providerModelCosts; }
    public void setProviderModelCosts(List<ProviderModelCostVO> providerModelCosts) { this.providerModelCosts = providerModelCosts; }

    public List<StorylineCostVO> getStorylineCosts() { return storylineCosts; }
    public void setStorylineCosts(List<StorylineCostVO> storylineCosts) { this.storylineCosts = storylineCosts; }

    public List<DailyTokenCostVO> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<DailyTokenCostVO> dailyTrend) { this.dailyTrend = dailyTrend; }
}
