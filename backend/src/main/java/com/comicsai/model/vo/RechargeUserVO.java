package com.comicsai.model.vo;

import java.math.BigDecimal;

public class RechargeUserVO {

    private Long userId;
    private String nickname;
    private String email;
    private Long rechargeCount;
    private BigDecimal totalRechargeAmount;
    private Long unlockCount;
    private BigDecimal totalSpent;

    public RechargeUserVO() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getRechargeCount() { return rechargeCount; }
    public void setRechargeCount(Long rechargeCount) { this.rechargeCount = rechargeCount; }

    public BigDecimal getTotalRechargeAmount() { return totalRechargeAmount; }
    public void setTotalRechargeAmount(BigDecimal totalRechargeAmount) { this.totalRechargeAmount = totalRechargeAmount; }

    public Long getUnlockCount() { return unlockCount; }
    public void setUnlockCount(Long unlockCount) { this.unlockCount = unlockCount; }

    public BigDecimal getTotalSpent() { return totalSpent; }
    public void setTotalSpent(BigDecimal totalSpent) { this.totalSpent = totalSpent; }
}
