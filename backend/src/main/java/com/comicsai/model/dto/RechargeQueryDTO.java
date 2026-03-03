package com.comicsai.model.dto;

import java.time.LocalDate;

public class RechargeQueryDTO {

    private LocalDate startDate;
    private LocalDate endDate;

    public RechargeQueryDTO() {}

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
