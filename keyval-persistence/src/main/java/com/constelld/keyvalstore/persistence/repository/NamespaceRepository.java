package com.constelld.keyvalstore.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.constelld.keyvalstore.core.model.Namespace;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for namespace persistence.
 */
public class NamespaceRepository {
    private static final Logger log = LoggerFactory.getLogger(NamespaceRepository.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Jdbi jdbi;

    public NamespaceRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * Create a new namespace.
     */
    public Namespace create(String name, String keyStrategyType, String valueStrategyType,
                           Map<String, Object> configuration) {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.now();

        Namespace namespace = new Namespace(
            id, name, keyStrategyType, valueStrategyType, configuration, createdAt
        );

        jdbi.useHandle(handle -> {
            handle.createUpdate(
                "INSERT INTO namespaces (id, name, key_strategy_type, value_strategy_type, configuration, created_at) " +
                "VALUES (:id, :name, :keyStrategyType, :valueStrategyType, :configuration, :createdAt)"
            )
            .bind("id", id)
            .bind("name", name)
            .bind("keyStrategyType", keyStrategyType)
            .bind("valueStrategyType", valueStrategyType)
            .bind("configuration", serializeConfig(configuration))
            .bind("createdAt", createdAt.toEpochMilli())
            .execute();
        });

        log.info("Created namespace: {} (id: {})", name, id);
        return namespace;
    }

    /**
     * Find namespace by ID.
     */
    public Optional<Namespace> findById(String id) {
        return jdbi.withHandle(handle ->
            handle.createQuery(
                "SELECT id, name, key_strategy_type, value_strategy_type, configuration, created_at " +
                "FROM namespaces WHERE id = :id"
            )
            .bind("id", id)
            .map((rs, ctx) -> new Namespace(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("key_strategy_type"),
                rs.getString("value_strategy_type"),
                deserializeConfig(rs.getString("configuration")),
                Instant.ofEpochMilli(rs.getLong("created_at"))
            ))
            .findFirst()
        );
    }

    /**
     * Find namespace by name.
     */
    public Optional<Namespace> findByName(String name) {
        return jdbi.withHandle(handle ->
            handle.createQuery(
                "SELECT id, name, key_strategy_type, value_strategy_type, configuration, created_at " +
                "FROM namespaces WHERE name = :name"
            )
            .bind("name", name)
            .map((rs, ctx) -> new Namespace(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("key_strategy_type"),
                rs.getString("value_strategy_type"),
                deserializeConfig(rs.getString("configuration")),
                Instant.ofEpochMilli(rs.getLong("created_at"))
            ))
            .findFirst()
        );
    }

    /**
     * List all namespaces.
     */
    public List<Namespace> findAll() {
        return jdbi.withHandle(handle ->
            handle.createQuery(
                "SELECT id, name, key_strategy_type, value_strategy_type, configuration, created_at " +
                "FROM namespaces ORDER BY created_at DESC"
            )
            .map((rs, ctx) -> new Namespace(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("key_strategy_type"),
                rs.getString("value_strategy_type"),
                deserializeConfig(rs.getString("configuration")),
                Instant.ofEpochMilli(rs.getLong("created_at"))
            ))
            .list()
        );
    }

    /**
     * Delete namespace by ID.
     */
    public boolean delete(String id) {
        int deleted = jdbi.withHandle(handle ->
            handle.createUpdate("DELETE FROM namespaces WHERE id = :id")
                .bind("id", id)
                .execute()
        );

        if (deleted > 0) {
            log.info("Deleted namespace with id: {}", id);
        }
        return deleted > 0;
    }

    /**
     * Delete namespace by name.
     */
    public boolean deleteByName(String name) {
        int deleted = jdbi.withHandle(handle ->
            handle.createUpdate("DELETE FROM namespaces WHERE name = :name")
                .bind("name", name)
                .execute()
        );

        if (deleted > 0) {
            log.info("Deleted namespace: {}", name);
        }
        return deleted > 0;
    }

    /**
     * Check if namespace exists by name.
     */
    public boolean existsByName(String name) {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT COUNT(*) FROM namespaces WHERE name = :name")
                .bind("name", name)
                .mapTo(Integer.class)
                .one()
        ) > 0;
    }

    private String serializeConfig(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize configuration", e);
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> deserializeConfig(String json) {
        if (json == null || json.isEmpty()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize configuration", e);
            return Map.of();
        }
    }
}
