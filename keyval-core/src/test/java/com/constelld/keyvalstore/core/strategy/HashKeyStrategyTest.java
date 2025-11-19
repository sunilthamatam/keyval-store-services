package com.constelld.keyvalstore.core.strategy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashKeyStrategyTest {

    @Test
    void testHashConsistency() {
        HashKeyStrategy strategy = new HashKeyStrategy();

        String namespace = "test";
        String key = "key1";

        long hash1 = strategy.hash(namespace, key);
        long hash2 = strategy.hash(namespace, key);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void testDifferentKeysProduceDifferentHashes() {
        HashKeyStrategy strategy = new HashKeyStrategy();

        String namespace = "test";
        long hash1 = strategy.hash(namespace, "key1");
        long hash2 = strategy.hash(namespace, "key2");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void testValidateKey() {
        HashKeyStrategy strategy = new HashKeyStrategy();

        assertThat(strategy.validateKey("validKey")).isTrue();
        assertThat(strategy.validateKey("")).isFalse();
        assertThat(strategy.validateKey(null)).isFalse();
    }

    @Test
    void testGetType() {
        HashKeyStrategy strategy = new HashKeyStrategy();
        assertThat(strategy.getType()).isEqualTo("HASH");
    }
}
