package com.example.sintese_api.exception;

public class GeminiException extends RuntimeException {

    private final Integer upstreamStatus;
    private final String upstreamBody;

    public GeminiException(String message) {
        this(message, null, null, null);
    }

    public GeminiException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    public GeminiException(String message, Integer upstreamStatus, String upstreamBody) {
        this(message, null, upstreamStatus, upstreamBody);
    }

    private GeminiException(String message, Throwable cause,
                            Integer upstreamStatus, String upstreamBody) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
        this.upstreamBody = upstreamBody;
    }

    public Integer getUpstreamStatus() { return upstreamStatus; }
    public String getUpstreamBody() { return upstreamBody; }
}