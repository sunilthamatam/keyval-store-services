package com.constelld.keyvalstore.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.constelld.keyvalstore.core.model.Namespace;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Namespace information")
public class NamespaceResponse {

    @Schema(description = "Namespace ID")
    private final String id;

    @Schema(description = "Namespace name")
    private final String name;

    @Schema(description = "Key strategy type")
    private final String keyStrategyType;

    @Schema(description = "Value strategy type")
    private final String valueStrategyType;

    @Schema(description = "Configuration")
    private final Map<String, Object> configuration;

    @Schema(description = "Creation timestamp")
    private final Instant createdAt;

    public NamespaceResponse(Namespace namespace) {
        this.id = namespace.getId();
        this.name = namespace.getName();
        this.keyStrategyType = namespace.getKeyStrategyType();
        this.valueStrategyType = namespace.getValueStrategyType();
        this.configuration = namespace.getConfiguration();
        this.createdAt = namespace.getCreatedAt();
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
}
