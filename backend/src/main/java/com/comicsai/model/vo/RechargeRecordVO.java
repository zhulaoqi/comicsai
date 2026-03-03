package com.comicsai.model.vo;

import com.comicsai.model.entity.RechargeRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RechargeRecordVO {

    private Long id;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    public RechargeRecordVO() {}

    public RechargeRecordVO(Long id, BigDecimal amount, BigDecimal balanceAfter, LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public static RechargeRecordVO fromEntity(RechargeRecord record) {
        return new RechargeRecordVO(record.getId(), record.getAmount(),
                record.getBalanceAfter(), record.getCreatedAt());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
