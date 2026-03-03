package com.comicsai.common.exception;

public class IllegalStateTransitionException extends RuntimeException {

    public IllegalStateTransitionException(String message) {
        super(message);
    }

    public IllegalStateTransitionException(String fromStatus, String toStatus) {
        super("非法状态转换: " + fromStatus + " → " + toStatus);
    }
}
