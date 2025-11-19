package com.constelld.keyvalstore.core.exception;

/**
 * Exception thrown when a key is not found.
 */
public class KeyNotFoundException extends KeyValStoreException {
    public KeyNotFoundException(String namespace, String key) {
        super("Key not found in namespace '" + namespace + "': " + key);
    }
}
