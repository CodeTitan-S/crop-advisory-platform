package com.college.cropadvisory.exception;

import org.springframework.http.HttpStatus;

/** The caller is authenticated but not allowed to perform the action. Maps to HTTP 403. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
