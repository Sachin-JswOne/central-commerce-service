package com.jswone.commerce.core.exceptions;


import com.jswone.commerce.core.enums.ErrorType;
import lombok.Getter;

@Getter
public class ParsingException extends RuntimeException {

    private final String message;

    private final ErrorType type;

    public ParsingException(String message, ErrorType type) {
        super(message);
        this.message = message;
        this.type = type;
    }

    public ParsingException(String message, ErrorType type, Throwable throwable) {
        super(message, throwable);
        this.message = message;
        this.type = type;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }


}