package com.jiber.backend.publicdata;

public class PublicDataClientException extends RuntimeException {

    public PublicDataClientException(String message) {
        super(message);
    }

    public PublicDataClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
