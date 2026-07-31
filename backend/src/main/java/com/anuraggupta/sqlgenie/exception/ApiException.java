package com.anuraggupta.sqlgenie.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all deliberately-thrown application exceptions.
 * Feature modules subclass this rather than throwing raw RuntimeExceptions,
 * so GlobalExceptionHandler can map every known failure to the right HTTP status.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
