package com.college.cropadvisory.exception;

import org.springframework.http.HttpStatus;

/** The requested resource does not exist. Maps to HTTP 404. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
