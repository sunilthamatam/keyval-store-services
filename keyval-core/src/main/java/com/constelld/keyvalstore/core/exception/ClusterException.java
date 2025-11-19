package com.constelld.keyvalstore.core.exception;

/**
 * Exception thrown when cluster operations fail.
 */
public class ClusterException extends KeyValStoreException {
    public ClusterException(String message) {
        super(message);
    }

    public ClusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
