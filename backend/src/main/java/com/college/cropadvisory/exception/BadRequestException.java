package com.college.cropadvisory.exception;

import org.springframework.http.HttpStatus;

/** The request is malformed or missing required input. Maps to HTTP 400. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
