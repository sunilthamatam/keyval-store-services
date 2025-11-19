package com.constelld.keyvalstore.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.constelld.keyvalstore.core.model.KeyValue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Schema(description = "Key-value pair response")
public class KeyValueResponse {

    @Schema(description = "Namespace")
    private final String namespace;

    @Schema(description = "Key")
    private final String key;

    @Schema(description = "Value (as string)")
    @JsonRawValue
    private final String value;

    @Schema(description = "Timestamp")
    private final Instant timestamp;

    @Schema(description = "Version")
    private final long version;

    public KeyValueResponse(KeyValue keyValue) {
        this.namespace = keyValue.getNamespace();
        this.key = keyValue.getKey();
        this.value = new String(keyValue.getValue(), StandardCharsets.UTF_8);
        this.timestamp = keyValue.getTimestamp();
        this.version = keyValue.getVersion();
    }

    @JsonProperty("namespace")
    public String getNamespace() {
        return namespace;
    }

    @JsonProperty("key")
    public String getKey() {
        return key;
    }

    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    @JsonProperty("timestamp")
    public Instant getTimestamp() {
        return timestamp;
    }

    @JsonProperty("version")
    public long getVersion() {
        return version;
    }
}
