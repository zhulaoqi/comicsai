package com.comicsai.common.exception;

public class AiProviderException extends RuntimeException {

    private final String providerName;

    public AiProviderException(String providerName, String message) {
        super(message);
        this.providerName = providerName;
    }

    public AiProviderException(String providerName, String message, Throwable cause) {
        super(message, cause);
        this.providerName = providerName;
    }

    public String getProviderName() {
        return providerName;
    }
}
