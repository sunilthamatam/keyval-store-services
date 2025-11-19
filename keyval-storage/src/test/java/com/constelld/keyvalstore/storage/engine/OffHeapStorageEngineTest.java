package com.constelld.keyvalstore.storage.engine;

import com.constelld.keyvalstore.core.model.KeyValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OffHeapStorageEngineTest {

    private OffHeapStorageEngine storage;

    @BeforeEach
    void setUp() {
        // 100MB for testing
        storage = new OffHeapStorageEngine(100 * 1024 * 1024);
    }

    @AfterEach
    void tearDown() {
        if (storage != null) {
            storage.shutdown();
        }
    }

    @Test
    void testPutAndGet() {
        String namespace = "test";
        String key = "key1";
        byte[] value = "value1".getBytes(StandardCharsets.UTF_8);

        KeyValue kv = storage.put(namespace, key, value);
        assertThat(kv).isNotNull();
        assertThat(kv.getNamespace()).isEqualTo(namespace);
        assertThat(kv.getKey()).isEqualTo(key);

        Optional<KeyValue> retrieved = storage.get(namespace, key);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getValue()).isEqualTo(value);
    }

    @Test
    void testDelete() {
        String namespace = "test";
        String key = "key1";
        byte[] value = "value1".getBytes(StandardCharsets.UTF_8);

        storage.put(namespace, key, value);
        assertThat(storage.exists(namespace, key)).isTrue();

        boolean deleted = storage.delete(namespace, key);
        assertThat(deleted).isTrue();
        assertThat(storage.exists(namespace, key)).isFalse();
    }

    @Test
    void testListKeys() {
        String namespace = "test";

        storage.put(namespace, "key1", "value1".getBytes(StandardCharsets.UTF_8));
        storage.put(namespace, "key2", "value2".getBytes(StandardCharsets.UTF_8));
        storage.put(namespace, "key3", "value3".getBytes(StandardCharsets.UTF_8));

        List<String> keys = storage.listKeys(namespace, 0, 10);
        assertThat(keys).hasSize(3);
        assertThat(keys).contains("key1", "key2", "key3");
    }

    @Test
    void testCount() {
        String namespace = "test";

        storage.put(namespace, "key1", "value1".getBytes(StandardCharsets.UTF_8));
        storage.put(namespace, "key2", "value2".getBytes(StandardCharsets.UTF_8));

        long count = storage.count(namespace);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void testClear() {
        String namespace = "test";

        storage.put(namespace, "key1", "value1".getBytes(StandardCharsets.UTF_8));
        storage.put(namespace, "key2", "value2".getBytes(StandardCharsets.UTF_8));

        storage.clear(namespace);

        long count = storage.count(namespace);
        assertThat(count).isZero();
    }

    @Test
    void testUpdateValue() {
        String namespace = "test";
        String key = "key1";
        byte[] value1 = "value1".getBytes(StandardCharsets.UTF_8);
        byte[] value2 = "value2".getBytes(StandardCharsets.UTF_8);

        storage.put(namespace, key, value1);
        storage.put(namespace, key, value2);

        Optional<KeyValue> retrieved = storage.get(namespace, key);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getValue()).isEqualTo(value2);
    }
}
