package com.anuraggupta.sqlgenie.exception;

import org.springframework.http.HttpStatus;

public class DuplicateFavoriteNameException extends ApiException {
    public DuplicateFavoriteNameException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
