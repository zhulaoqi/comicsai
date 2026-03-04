package com.comicsai.model.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaidDTO {

    @NotNull(message = "付费状态不能为空")
    private Boolean isPaid;

    private BigDecimal price;

    private Integer freeChapterCount;

    private BigDecimal defaultChapterPrice;

    public PaidDTO() {}

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getFreeChapterCount() { return freeChapterCount; }
    public void setFreeChapterCount(Integer freeChapterCount) { this.freeChapterCount = freeChapterCount; }

    public BigDecimal getDefaultChapterPrice() { return defaultChapterPrice; }
    public void setDefaultChapterPrice(BigDecimal defaultChapterPrice) { this.defaultChapterPrice = defaultChapterPrice; }
}
