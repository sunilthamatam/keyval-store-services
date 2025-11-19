package com.constelld.keyvalstore.core.strategy;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AtomicIncrementValueStrategyTest {

    @Test
    void testGenerateNextValue() {
        AtomicIncrementValueStrategy strategy = new AtomicIncrementValueStrategy();

        // Generate first value
        byte[] value1 = strategy.generateNextValue("ns", "key1", null, Map.of());
        long long1 = ByteBuffer.wrap(value1).getLong();
        assertThat(long1).isEqualTo(1L);

        // Generate next value
        byte[] value2 = strategy.generateNextValue("ns", "key1", value1, Map.of());
        long long2 = ByteBuffer.wrap(value2).getLong();
        assertThat(long2).isEqualTo(2L);

        // Generate next value
        byte[] value3 = strategy.generateNextValue("ns", "key1", value2, Map.of());
        long long3 = ByteBuffer.wrap(value3).getLong();
        assertThat(long3).isEqualTo(3L);
    }

    @Test
    void testProcessValue() {
        AtomicIncrementValueStrategy strategy = new AtomicIncrementValueStrategy();

        byte[] value = ByteBuffer.allocate(Long.BYTES).putLong(42L).array();
        byte[] processed = strategy.processValue("ns", "key1", value, Map.of());

        long result = ByteBuffer.wrap(processed).getLong();
        assertThat(result).isEqualTo(42L);
    }

    @Test
    void testValidateValue() {
        AtomicIncrementValueStrategy strategy = new AtomicIncrementValueStrategy();

        byte[] validValue = ByteBuffer.allocate(Long.BYTES).putLong(1L).array();
        assertThat(strategy.validateValue(validValue)).isTrue();

        assertThat(strategy.validateValue(new byte[4])).isFalse();
        assertThat(strategy.validateValue(null)).isFalse();
    }

    @Test
    void testGetType() {
        AtomicIncrementValueStrategy strategy = new AtomicIncrementValueStrategy();
        assertThat(strategy.getType()).isEqualTo("ATOMIC_INCREMENT");
    }
}
