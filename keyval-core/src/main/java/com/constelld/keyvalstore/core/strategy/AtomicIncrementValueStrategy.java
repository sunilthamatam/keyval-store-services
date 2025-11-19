package com.constelld.keyvalstore.core.strategy;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Atomic increment value strategy - auto-generates incrementing long values.
 */
public class AtomicIncrementValueStrategy implements ValueStrategy {
    private static final String TYPE = "ATOMIC_INCREMENT";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public byte[] processValue(String namespace, String key, byte[] value, Map<String, Object> configuration) {
        if (value != null && value.length > 0) {
            // Use provided value if it's a valid long
            if (validateValue(value)) {
                return value;
            }
            throw new IllegalArgumentException("Value must be a valid 8-byte long for ATOMIC_INCREMENT strategy");
        }
        // Auto-generate starting from 1
        return longToBytes(1L);
    }

    @Override
    public byte[] generateNextValue(String namespace, String key, byte[] currentValue, Map<String, Object> configuration) {
        long current = currentValue != null ? bytesToLong(currentValue) : 0L;
        return longToBytes(current + 1);
    }

    @Override
    public boolean validateValue(byte[] value) {
        return value != null && value.length == Long.BYTES;
    }

    private byte[] longToBytes(long value) {
        return ByteBuffer.allocate(Long.BYTES).putLong(value).array();
    }

    private long bytesToLong(byte[] value) {
        return ByteBuffer.wrap(value).getLong();
    }
}
