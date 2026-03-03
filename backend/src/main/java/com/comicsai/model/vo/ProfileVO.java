package com.comicsai.model.vo;

import java.math.BigDecimal;
import java.util.List;

public class ProfileVO {

    private Long id;
    private String email;
    private String nickname;
    private BigDecimal balance;
    private List<RechargeRecordVO> rechargeRecords;
    private List<UnlockRecordVO> unlockRecords;

    public ProfileVO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public List<RechargeRecordVO> getRechargeRecords() { return rechargeRecords; }
    public void setRechargeRecords(List<RechargeRecordVO> rechargeRecords) { this.rechargeRecords = rechargeRecords; }

    public List<UnlockRecordVO> getUnlockRecords() { return unlockRecords; }
    public void setUnlockRecords(List<UnlockRecordVO> unlockRecords) { this.unlockRecords = unlockRecords; }
}
