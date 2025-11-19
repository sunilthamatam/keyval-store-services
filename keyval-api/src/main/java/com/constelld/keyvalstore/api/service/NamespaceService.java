package com.constelld.keyvalstore.api.service;

import com.constelld.keyvalstore.core.exception.NamespaceNotFoundException;
import com.constelld.keyvalstore.core.model.Namespace;
import com.constelld.keyvalstore.core.storage.StorageEngine;
import com.constelld.keyvalstore.persistence.repository.NamespaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Service layer for namespace operations.
 */
public class NamespaceService {
    private static final Logger log = LoggerFactory.getLogger(NamespaceService.class);

    private final NamespaceRepository namespaceRepository;
    private final StorageEngine storageEngine;

    public NamespaceService(NamespaceRepository namespaceRepository, StorageEngine storageEngine) {
        this.namespaceRepository = namespaceRepository;
        this.storageEngine = storageEngine;
    }

    /**
     * Create a new namespace.
     */
    public Namespace createNamespace(String name, String keyStrategyType, String valueStrategyType,
                                    Map<String, Object> configuration) {
        // Check if namespace already exists
        if (namespaceRepository.existsByName(name)) {
            throw new IllegalArgumentException("Namespace already exists: " + name);
        }

        // Validate strategy types
        validateStrategyTypes(keyStrategyType, valueStrategyType);

        // Create namespace
        Namespace namespace = namespaceRepository.create(name, keyStrategyType, valueStrategyType, configuration);

        log.info("Created namespace: {}", name);
        return namespace;
    }

    /**
     * Get namespace by name.
     */
    public Namespace getNamespace(String name) {
        return namespaceRepository.findByName(name)
                .orElseThrow(() -> new NamespaceNotFoundException(name));
    }

    /**
     * List all namespaces.
     */
    public List<Namespace> listNamespaces() {
        return namespaceRepository.findAll();
    }

    /**
     * Delete a namespace.
     */
    public void deleteNamespace(String name) {
        // Check if namespace exists
        Namespace namespace = getNamespace(name);

        // Clear all data in the namespace
        storageEngine.clear(namespace.getName());

        // Delete namespace metadata
        boolean deleted = namespaceRepository.deleteByName(name);

        if (deleted) {
            log.info("Deleted namespace: {}", name);
        }
    }

    /**
     * Get namespace statistics.
     */
    public Map<String, Object> getNamespaceStats(String name) {
        Namespace namespace = getNamespace(name);

        long keyCount = storageEngine.count(namespace.getName());

        return Map.of(
                "namespace", namespace.getName(),
                "keyCount", keyCount,
                "keyStrategyType", namespace.getKeyStrategyType(),
                "valueStrategyType", namespace.getValueStrategyType()
        );
    }

    private void validateStrategyTypes(String keyStrategyType, String valueStrategyType) {
        // Validate key strategy
        List<String> validKeyStrategies = List.of("HASH");
        if (!validKeyStrategies.contains(keyStrategyType)) {
            throw new IllegalArgumentException("Invalid key strategy type: " + keyStrategyType);
        }

        // Validate value strategy
        List<String> validValueStrategies = List.of("STRING", "ATOMIC_INCREMENT", "JSON_BLOB");
        if (!validValueStrategies.contains(valueStrategyType)) {
            throw new IllegalArgumentException("Invalid value strategy type: " + valueStrategyType);
        }
    }
}
