package com.jswone.commerce.core.exceptions;

import com.jswone.commerce.core.model.request.ProductSkuRequest;
import org.springframework.http.HttpStatus;

public class ProductSelectorException extends RuntimeException {

    private static final long serialVersionUID = 6405119194042287889L;
    private final ProductSkuRequest productSkuRequest;

    private final HttpStatus httpStatus;

    public ProductSelectorException(
            ProductSkuRequest productSkuRequest,
            String message,
            HttpStatus httpStatus,
            Throwable throwable) {
        super(message, throwable);
        this.productSkuRequest = productSkuRequest;
        this.httpStatus = httpStatus;
    }

    public ProductSelectorException(
            ProductSkuRequest productSkuRequest, String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.productSkuRequest = productSkuRequest;
    }

    public HttpStatus getHttpStatus() {
        return this.httpStatus;
    }

    public ProductSkuRequest getProductSkuRequest() {
        return this.productSkuRequest;
    }
}
