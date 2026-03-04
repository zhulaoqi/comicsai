package com.comicsai.model.dto;

import java.math.BigDecimal;

public class ChapterPriceDTO {

    private BigDecimal price;

    public ChapterPriceDTO() {}

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
}
