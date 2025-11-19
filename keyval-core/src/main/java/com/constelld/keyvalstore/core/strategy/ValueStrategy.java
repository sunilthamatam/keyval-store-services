package com.constelld.keyvalstore.core.strategy;

import java.util.Map;

/**
 * Strategy for handling value generation and transformation.
 * Different strategies can implement atomic increments, JSON blobs, or custom logic.
 */
public interface ValueStrategy {

    /**
     * Get the type identifier for this strategy.
     */
    String getType();

    /**
     * Process/transform a value before storage.
     * This can be used for validation, transformation, or generation.
     *
     * @param namespace the namespace
     * @param key the key
     * @param value the input value (can be null for auto-generation)
     * @param configuration namespace-specific configuration
     * @return the processed value
     */
    byte[] processValue(String namespace, String key, byte[] value, Map<String, Object> configuration);

    /**
     * Generate the next value (for strategies like atomic increment).
     *
     * @param namespace the namespace
     * @param key the key
     * @param currentValue the current value (null if key doesn't exist)
     * @param configuration namespace-specific configuration
     * @return the next value
     */
    byte[] generateNextValue(String namespace, String key, byte[] currentValue, Map<String, Object> configuration);

    /**
     * Validate the value format for this strategy.
     *
     * @param value the value to validate
     * @return true if valid
     */
    boolean validateValue(byte[] value);
}
