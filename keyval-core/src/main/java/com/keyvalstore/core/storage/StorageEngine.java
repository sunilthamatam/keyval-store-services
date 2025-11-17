package com.keyvalstore.core.storage;

import com.keyvalstore.core.model.KeyValue;

import java.util.List;
import java.util.Optional;

/**
 * Interface for the storage engine that manages off-heap key-value storage.
 */
public interface StorageEngine {

    /**
     * Store a key-value pair.
     *
     * @param namespace the namespace
     * @param key the key
     * @param value the value
     * @return the stored KeyValue
     */
    KeyValue put(String namespace, String key, byte[] value);

    /**
     * Retrieve a value by key.
     *
     * @param namespace the namespace
     * @param key the key
     * @return the value if found
     */
    Optional<KeyValue> get(String namespace, String key);

    /**
     * Delete a key-value pair.
     *
     * @param namespace the namespace
     * @param key the key
     * @return true if deleted, false if not found
     */
    boolean delete(String namespace, String key);

    /**
     * Check if a key exists.
     *
     * @param namespace the namespace
     * @param key the key
     * @return true if exists
     */
    boolean exists(String namespace, String key);

    /**
     * List all keys in a namespace (paginated).
     *
     * @param namespace the namespace
     * @param offset starting offset
     * @param limit maximum number of keys
     * @return list of keys
     */
    List<String> listKeys(String namespace, int offset, int limit);

    /**
     * Get the number of keys in a namespace.
     *
     * @param namespace the namespace
     * @return count of keys
     */
    long count(String namespace);

    /**
     * Clear all keys in a namespace.
     *
     * @param namespace the namespace
     */
    void clear(String namespace);

    /**
     * Get storage statistics.
     *
     * @return storage stats
     */
    StorageStats getStats();

    /**
     * Shutdown the storage engine and release resources.
     */
    void shutdown();

    /**
     * Storage statistics.
     */
    record StorageStats(
            long totalKeys,
            long totalMemoryUsed,
            long maxMemory,
            double memoryUtilization
    ) {}
}
