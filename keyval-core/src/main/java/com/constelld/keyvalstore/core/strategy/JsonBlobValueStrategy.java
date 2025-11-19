package com.constelld.keyvalstore.core.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JSON blob value strategy - validates and stores JSON data.
 */
public class JsonBlobValueStrategy implements ValueStrategy {
    private static final String TYPE = "JSON_BLOB";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public byte[] processValue(String namespace, String key, byte[] value, Map<String, Object> configuration) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("Value cannot be null or empty for JSON_BLOB strategy");
        }

        // Validate JSON
        if (!validateValue(value)) {
            throw new IllegalArgumentException("Value is not valid JSON");
        }

        return value;
    }

    @Override
    public byte[] generateNextValue(String namespace, String key, byte[] currentValue, Map<String, Object> configuration) {
        throw new UnsupportedOperationException("JSON_BLOB strategy does not support auto-generation");
    }

    @Override
    public boolean validateValue(byte[] value) {
        if (value == null || value.length == 0) {
            return false;
        }
        try {
            String jsonString = new String(value, StandardCharsets.UTF_8);
            OBJECT_MAPPER.readTree(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
