package com.comicsai.model.vo;

public class AdminLoginVO {

    private String token;
    private Long id;
    private String email;
    private String nickname;

    public AdminLoginVO() {}

    public AdminLoginVO(String token, Long id, String email, String nickname) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.nickname = nickname;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
}
