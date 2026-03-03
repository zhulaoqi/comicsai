package com.comicsai.model.vo;

import java.math.BigDecimal;
import java.util.List;

public class RechargeAnalyticsVO {

    private Long totalRechargeCount;
    private BigDecimal totalRechargeAmount;
    private BigDecimal averageRechargeAmount;
    private List<RechargeUserVO> rechargeUsers;

    public RechargeAnalyticsVO() {}

    public Long getTotalRechargeCount() { return totalRechargeCount; }
    public void setTotalRechargeCount(Long totalRechargeCount) { this.totalRechargeCount = totalRechargeCount; }

    public BigDecimal getTotalRechargeAmount() { return totalRechargeAmount; }
    public void setTotalRechargeAmount(BigDecimal totalRechargeAmount) { this.totalRechargeAmount = totalRechargeAmount; }

    public BigDecimal getAverageRechargeAmount() { return averageRechargeAmount; }
    public void setAverageRechargeAmount(BigDecimal averageRechargeAmount) { this.averageRechargeAmount = averageRechargeAmount; }

    public List<RechargeUserVO> getRechargeUsers() { return rechargeUsers; }
    public void setRechargeUsers(List<RechargeUserVO> rechargeUsers) { this.rechargeUsers = rechargeUsers; }
}
