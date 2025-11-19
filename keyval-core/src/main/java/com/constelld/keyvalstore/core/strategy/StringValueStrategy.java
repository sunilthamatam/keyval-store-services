package com.constelld.keyvalstore.core.strategy;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Simple string value strategy - stores values as-is.
 */
public class StringValueStrategy implements ValueStrategy {
    private static final String TYPE = "STRING";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public byte[] processValue(String namespace, String key, byte[] value, Map<String, Object> configuration) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("Value cannot be null or empty for STRING strategy");
        }
        return value;
    }

    @Override
    public byte[] generateNextValue(String namespace, String key, byte[] currentValue, Map<String, Object> configuration) {
        throw new UnsupportedOperationException("STRING strategy does not support auto-generation");
    }

    @Override
    public boolean validateValue(byte[] value) {
        if (value == null || value.length == 0) {
            return false;
        }
        // Try to decode as UTF-8
        try {
            new String(value, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
