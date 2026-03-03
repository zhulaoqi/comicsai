package com.comicsai.model.dto;

import java.time.LocalDate;

public class TokenCostQueryDTO {

    private LocalDate startDate;
    private LocalDate endDate;
    private String providerName;
    private Long storylineId;

    public TokenCostQueryDTO() {}

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public Long getStorylineId() { return storylineId; }
    public void setStorylineId(Long storylineId) { this.storylineId = storylineId; }
}
