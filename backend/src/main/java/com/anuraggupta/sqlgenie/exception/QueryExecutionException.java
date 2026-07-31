package com.anuraggupta.sqlgenie.exception;

import org.springframework.http.HttpStatus;

public class QueryExecutionException extends ApiException {
    public QueryExecutionException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
