package com.keyvalstore.core.exception;

/**
 * Base exception for key-value store operations.
 */
public class KeyValStoreException extends RuntimeException {
    public KeyValStoreException(String message) {
        super(message);
    }

    public KeyValStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
