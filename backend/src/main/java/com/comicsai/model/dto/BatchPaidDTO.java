package com.comicsai.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class BatchPaidDTO {

    @NotEmpty(message = "内容ID列表不能为空")
    private List<Long> contentIds;

    @NotNull(message = "付费状态不能为空")
    private Boolean isPaid;

    private BigDecimal price;

    public BatchPaidDTO() {}

    public List<Long> getContentIds() { return contentIds; }
    public void setContentIds(List<Long> contentIds) { this.contentIds = contentIds; }

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
