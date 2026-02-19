package com.jswone.commerce.core.exceptions;

import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public class GcsServiceException extends RuntimeException {

    private final HttpStatus httpStatus;

    public GcsServiceException(String message, Throwable throwable, HttpStatus httpStatus) {
        super(message, throwable);
        this.httpStatus = httpStatus;
    }

    public GcsServiceException(String message, HttpStatus httpStatus, Throwable throwable) {
        super(message, throwable);
        this.httpStatus = httpStatus;
    }

    public GcsServiceException(String message, Throwable throwable) {
        super(message, throwable);
        this.httpStatus = INTERNAL_SERVER_ERROR;
    }

    public GcsServiceException(String message) {
        super(message);
        this.httpStatus = INTERNAL_SERVER_ERROR;
    }

    public GcsServiceException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }
}
