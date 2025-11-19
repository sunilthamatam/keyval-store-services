package com.constelld.keyvalstore.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Schema(description = "Request to create a new namespace")
public class CreateNamespaceRequest {

    @NotBlank
    @Schema(description = "Namespace name", example = "users", required = true)
    private final String name;

    @NotBlank
    @Schema(description = "Key strategy type (HASH)", example = "HASH", required = true)
    private final String keyStrategyType;

    @NotBlank
    @Schema(description = "Value strategy type (STRING, ATOMIC_INCREMENT, JSON_BLOB)",
            example = "JSON_BLOB", required = true)
    private final String valueStrategyType;

    @Schema(description = "Additional configuration")
    private final Map<String, Object> configuration;

    @JsonCreator
    public CreateNamespaceRequest(
            @JsonProperty("name") String name,
            @JsonProperty("keyStrategyType") String keyStrategyType,
            @JsonProperty("valueStrategyType") String valueStrategyType,
            @JsonProperty("configuration") Map<String, Object> configuration) {
        this.name = name;
        this.keyStrategyType = keyStrategyType;
        this.valueStrategyType = valueStrategyType;
        this.configuration = configuration != null ? configuration : Map.of();
    }

    public String getName() {
        return name;
    }

    public String getKeyStrategyType() {
        return keyStrategyType;
    }

    public String getValueStrategyType() {
        return valueStrategyType;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }
}
