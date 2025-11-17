package com.keyvalstore.core.exception;

/**
 * Exception thrown when a namespace is not found.
 */
public class NamespaceNotFoundException extends KeyValStoreException {
    public NamespaceNotFoundException(String namespace) {
        super("Namespace not found: " + namespace);
    }
}
