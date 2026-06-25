package com.jiber.backend.news.client;

public class NaverNewsClientException extends RuntimeException {

    private final Integer statusCode;
    private final String responseBody;

    public NaverNewsClientException(String message, Throwable cause) {
        this(message, null, null, cause);
    }

    public NaverNewsClientException(String message, Integer statusCode, Throwable cause) {
        this(message, statusCode, null, cause);
    }

    public NaverNewsClientException(String message, Integer statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody == null ? "" : responseBody;
    }

    public boolean isCredentialError() {
        return statusCode != null && (statusCode == 401 || statusCode == 403);
    }

    public boolean hasEmptyScopeError() {
        return responseBody.contains("\"errorCode\":\"024\"") || responseBody.contains("Scopes are Empty");
    }
}
