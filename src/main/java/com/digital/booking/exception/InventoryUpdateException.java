package com.digital.booking.exception;

/**
 * Thrown when updating seat inventory fails (e.g. DB or cache update error).
 */
public class InventoryUpdateException extends RuntimeException {

    public InventoryUpdateException(String message) {
        super(message);
    }

    public InventoryUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}