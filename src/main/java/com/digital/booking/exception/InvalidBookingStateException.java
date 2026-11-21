package com.digital.booking.exception;

/**
 * Thrown when an action is not allowed in the booking's current state (e.g. cancelling a completed booking).
 */
public class InvalidBookingStateException extends RuntimeException {

    public InvalidBookingStateException(String message) {
        super(message);
    }

    public InvalidBookingStateException(String message, Throwable cause) {
        super(message, cause);
    }
}