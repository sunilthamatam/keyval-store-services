package com.constelld.keyvalstore.api.service;

import com.constelld.keyvalstore.core.exception.KeyNotFoundException;
import com.constelld.keyvalstore.core.exception.NamespaceNotFoundException;
import com.constelld.keyvalstore.core.model.ConsistencyLevel;
import com.constelld.keyvalstore.core.model.KeyValue;
import com.constelld.keyvalstore.core.model.Namespace;
import com.constelld.keyvalstore.core.storage.StorageEngine;
import com.constelld.keyvalstore.core.strategy.ValueStrategy;
import com.constelld.keyvalstore.persistence.repository.NamespaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service layer for key-value operations.
 */
public class KeyValueService {
    private static final Logger log = LoggerFactory.getLogger(KeyValueService.class);

    private final StorageEngine storageEngine;
    private final NamespaceRepository namespaceRepository;
    private final Map<String, ValueStrategy> valueStrategies;

    public KeyValueService(
            StorageEngine storageEngine,
            NamespaceRepository namespaceRepository,
            Map<String, ValueStrategy> valueStrategies) {
        this.storageEngine = storageEngine;
        this.namespaceRepository = namespaceRepository;
        this.valueStrategies = valueStrategies;
    }

    /**
     * Put a key-value pair.
     */
    public KeyValue put(String namespaceName, String key, String value, ConsistencyLevel consistencyLevel) {
        Namespace namespace = getNamespace(namespaceName);

        // Get value strategy
        ValueStrategy valueStrategy = valueStrategies.get(namespace.getValueStrategyType());
        if (valueStrategy == null) {
            throw new IllegalArgumentException("Unknown value strategy: " + namespace.getValueStrategyType());
        }

        // Process value
        byte[] valueBytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : null;
        byte[] processedValue = valueStrategy.processValue(
                namespaceName, key, valueBytes, namespace.getConfiguration()
        );

        // Store
        KeyValue keyValue = storageEngine.put(namespaceName, key, processedValue);

        log.debug("Put key '{}' in namespace '{}'", key, namespaceName);
        return keyValue;
    }

    /**
     * Increment a value (for atomic increment strategy).
     */
    public KeyValue increment(String namespaceName, String key, ConsistencyLevel consistencyLevel) {
        Namespace namespace = getNamespace(namespaceName);

        if (!"ATOMIC_INCREMENT".equals(namespace.getValueStrategyType())) {
            throw new IllegalArgumentException("Namespace does not support atomic increment");
        }

        ValueStrategy valueStrategy = valueStrategies.get(namespace.getValueStrategyType());

        // Get current value
        Optional<KeyValue> current = storageEngine.get(namespaceName, key);
        byte[] currentValue = current.map(KeyValue::getValue).orElse(null);

        // Generate next value
        byte[] nextValue = valueStrategy.generateNextValue(
                namespaceName, key, currentValue, namespace.getConfiguration()
        );

        // Store
        KeyValue keyValue = storageEngine.put(namespaceName, key, nextValue);

        log.debug("Incremented key '{}' in namespace '{}'", key, namespaceName);
        return keyValue;
    }

    /**
     * Get a value by key.
     */
    public Optional<KeyValue> get(String namespaceName, String key, ConsistencyLevel consistencyLevel) {
        // Verify namespace exists
        getNamespace(namespaceName);

        return storageEngine.get(namespaceName, key);
    }

    /**
     * Delete a key.
     */
    public boolean delete(String namespaceName, String key, ConsistencyLevel consistencyLevel) {
        // Verify namespace exists
        getNamespace(namespaceName);

        boolean deleted = storageEngine.delete(namespaceName, key);

        if (deleted) {
            log.debug("Deleted key '{}' from namespace '{}'", key, namespaceName);
        }

        return deleted;
    }

    /**
     * List keys in a namespace.
     */
    public List<String> listKeys(String namespaceName, int offset, int limit) {
        // Verify namespace exists
        getNamespace(namespaceName);

        return storageEngine.listKeys(namespaceName, offset, limit);
    }

    /**
     * Check if a key exists.
     */
    public boolean exists(String namespaceName, String key) {
        // Verify namespace exists
        getNamespace(namespaceName);

        return storageEngine.exists(namespaceName, key);
    }

    /**
     * Get count of keys in namespace.
     */
    public long count(String namespaceName) {
        // Verify namespace exists
        getNamespace(namespaceName);

        return storageEngine.count(namespaceName);
    }

    private Namespace getNamespace(String name) {
        return namespaceRepository.findByName(name)
                .orElseThrow(() -> new NamespaceNotFoundException(name));
    }
}
