package com.comicsai.model.dto;

import com.comicsai.model.enums.ContentType;

import java.time.LocalDate;

public class AnalyticsQueryDTO {

    private LocalDate startDate;
    private LocalDate endDate;
    private ContentType contentType;
    private Boolean isPaid;

    public AnalyticsQueryDTO() {}

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }
}
