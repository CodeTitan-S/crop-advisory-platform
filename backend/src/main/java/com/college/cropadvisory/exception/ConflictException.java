package com.college.cropadvisory.exception;

import org.springframework.http.HttpStatus;

/** The request conflicts with current state (e.g. an illegal status transition). Maps to HTTP 409. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
