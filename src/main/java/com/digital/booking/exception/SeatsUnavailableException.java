package com.digital.booking.exception;

/**
 * Thrown when the requested seats are already taken or cannot be held.
 */
public class SeatsUnavailableException extends RuntimeException {

    public SeatsUnavailableException(String message) {
        super(message);
    }

    public SeatsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}