package com.constelld.keyvalstore.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "Error message")
    private final String message;

    @Schema(description = "HTTP status code")
    private final int statusCode;

    @Schema(description = "Timestamp")
    private final long timestamp;

    public ErrorResponse(String message, int statusCode) {
        this.message = message;
        this.statusCode = statusCode;
        this.timestamp = System.currentTimeMillis();
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("statusCode")
    public int getStatusCode() {
        return statusCode;
    }

    @JsonProperty("timestamp")
    public long getTimestamp() {
        return timestamp;
    }
}
