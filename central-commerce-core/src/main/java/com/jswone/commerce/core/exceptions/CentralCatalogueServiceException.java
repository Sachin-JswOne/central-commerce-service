package com.jswone.commerce.core.exceptions;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import java.io.Serial;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CentralCatalogueServiceException extends RuntimeException {

  @Serial private static final long serialVersionUID = 6305119194042283081L;
  private final HttpStatus httpStatus;

  public CentralCatalogueServiceException(
      String message, Throwable throwable, HttpStatus httpStatus) {
    super(message, throwable);
    this.httpStatus = httpStatus;
  }

  public CentralCatalogueServiceException(
      String message, HttpStatus httpStatus, Throwable throwable) {
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
}
