package com.jswone.commerce.core.exceptions;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.springframework.http.HttpStatus;

public class CentralCommerceServiceException extends RuntimeException {

  private static final long serialVersionUID = 6405119194042283089L;
  private final HttpStatus httpStatus;

  public CentralCommerceServiceException(
      String message, Throwable throwable, HttpStatus httpStatus) {
    super(message, throwable);
    this.httpStatus = httpStatus;
  }

  public CentralCommerceServiceException(
      String message, HttpStatus httpStatus, Throwable throwable) {
    super(message, throwable);
    this.httpStatus = httpStatus;
  }

  public CentralCommerceServiceException(String message, Throwable throwable) {
    super(message, throwable);
    this.httpStatus = INTERNAL_SERVER_ERROR;
  }

  public CentralCommerceServiceException(String message) {
    super(message);
    this.httpStatus = INTERNAL_SERVER_ERROR;
  }

  public CentralCommerceServiceException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public HttpStatus getHttpStatus() {
    return this.httpStatus;
  }
}
