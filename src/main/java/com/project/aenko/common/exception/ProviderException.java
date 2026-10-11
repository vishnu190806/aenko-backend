package com.project.aenko.common.exception;

public class ProviderException extends RuntimeException {
    private final int upstreamStatus;

    public ProviderException(String message) {
        super(message);
        this.upstreamStatus = 0;
    }

    public ProviderException(String message, int upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = 0;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
