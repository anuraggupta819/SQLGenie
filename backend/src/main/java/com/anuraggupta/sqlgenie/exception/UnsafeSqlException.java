package com.anuraggupta.sqlgenie.exception;

import org.springframework.http.HttpStatus;

public class UnsafeSqlException extends ApiException {
    public UnsafeSqlException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
