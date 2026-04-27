package com.jswone.commerce.core.exceptions;

import org.springframework.http.HttpStatus;

public class AccountMasterException extends RuntimeException {

    private final HttpStatus httpStatus;

    public AccountMasterException(String message, Throwable throwable, HttpStatus httpStatus) {
        super(message, throwable);
        this.httpStatus = httpStatus;
    }

    public AccountMasterException(String message, HttpStatus httpStatus, Throwable throwable) {
        super(message, throwable);
        this.httpStatus = httpStatus;
    }

    public AccountMasterException(String message, Throwable throwable) {
        super(message, throwable);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public AccountMasterException(String message) {
        super(message);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public AccountMasterException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }
}
