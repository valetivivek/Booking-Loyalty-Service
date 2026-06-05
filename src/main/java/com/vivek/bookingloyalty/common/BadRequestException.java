package com.vivek.bookingloyalty.common;

/** Thrown for invalid business operations (e.g. completing a cancelled booking). Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
