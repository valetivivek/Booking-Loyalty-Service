package com.vivek.bookingloyalty.common;

/** Thrown when a caller tries to access a resource they do not own. Maps to HTTP 403. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
