package com.keyvalstore.core.exception;

/**
 * Exception thrown when storage operations fail.
 */
public class StorageException extends KeyValStoreException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
