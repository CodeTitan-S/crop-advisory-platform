package com.college.cropadvisory.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for application errors that map to a specific HTTP status.
 * Extending {@link RuntimeException} keeps service code free of checked-exception noise.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
