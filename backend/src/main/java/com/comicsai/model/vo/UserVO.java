package com.comicsai.model.vo;

import java.math.BigDecimal;

public class UserVO {

    private Long id;
    private String email;
    private String nickname;
    private BigDecimal balance;

    public UserVO() {}

    public UserVO(Long id, String email, String nickname, BigDecimal balance) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.balance = balance;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public static UserVO fromUser(com.comicsai.model.entity.User user) {
        return new UserVO(user.getId(), user.getEmail(), user.getNickname(), user.getBalance());
    }
}
