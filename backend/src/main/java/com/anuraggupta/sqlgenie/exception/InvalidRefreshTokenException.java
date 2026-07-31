package com.anuraggupta.sqlgenie.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ApiException {
    public InvalidRefreshTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
