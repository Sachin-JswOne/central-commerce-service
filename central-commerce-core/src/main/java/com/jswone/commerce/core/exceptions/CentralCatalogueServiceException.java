package com.jswone.commerce.core.exceptions;

import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

public class CentralCatalogueServiceException extends RuntimeException {

    private final HttpStatus httpStatus;

    public CentralCatalogueServiceException(String message, Throwable throwable, HttpStatus httpStatus) {
        super(message, throwable);
        this.httpStatus = httpStatus;
    }

    public CentralCatalogueServiceException(String message, HttpStatus httpStatus, Throwable throwable) {
        super(message, throwable);
        this.httpStatus = httpStatus;
    }

    public CentralCatalogueServiceException(String message, Throwable throwable) {
        super(message, throwable);
        this.httpStatus = INTERNAL_SERVER_ERROR;
    }

    public CentralCatalogueServiceException(String message) {
        super(message);
        this.httpStatus = INTERNAL_SERVER_ERROR;
    }

    public CentralCatalogueServiceException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }
}
