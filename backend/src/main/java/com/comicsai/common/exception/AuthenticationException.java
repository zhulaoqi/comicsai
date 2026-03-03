package com.comicsai.common.exception;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException() {
        super("未认证，请先登录");
    }
}
