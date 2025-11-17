package com.keyvalstore.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a namespace that segments key-value pairs.
 * Each namespace has its own key and value strategies.
 */
public class Namespace {
    private final String id;
    private final String name;
    private final String keyStrategyType;
    private final String valueStrategyType;
    private final Map<String, Object> configuration;
    private final Instant createdAt;

    @JsonCreator
    public Namespace(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("keyStrategyType") String keyStrategyType,
            @JsonProperty("valueStrategyType") String valueStrategyType,
            @JsonProperty("configuration") Map<String, Object> configuration,
            @JsonProperty("createdAt") Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.keyStrategyType = Objects.requireNonNull(keyStrategyType, "keyStrategyType cannot be null");
        this.valueStrategyType = Objects.requireNonNull(valueStrategyType, "valueStrategyType cannot be null");
        this.configuration = configuration != null ? Map.copyOf(configuration) : Map.of();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("keyStrategyType")
    public String getKeyStrategyType() {
        return keyStrategyType;
    }

    @JsonProperty("valueStrategyType")
    public String getValueStrategyType() {
        return valueStrategyType;
    }

    @JsonProperty("configuration")
    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    @JsonProperty("createdAt")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Namespace namespace = (Namespace) o;
        return Objects.equals(id, namespace.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Namespace{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", keyStrategyType='" + keyStrategyType + '\'' +
                ", valueStrategyType='" + valueStrategyType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
