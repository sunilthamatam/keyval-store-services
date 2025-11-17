package com.keyvalstore.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.keyvalstore.core.model.Node;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Cluster node information")
public class NodeResponse {

    @Schema(description = "Node ID")
    private final String id;

    @Schema(description = "Host")
    private final String host;

    @Schema(description = "Port")
    private final int port;

    @Schema(description = "Admin port")
    private final int adminPort;

    @Schema(description = "Node status")
    private final String status;

    @Schema(description = "Last seen timestamp")
    private final Instant lastSeen;

    public NodeResponse(Node node) {
        this.id = node.getId();
        this.host = node.getHost();
        this.port = node.getPort();
        this.adminPort = node.getAdminPort();
        this.status = node.getStatus().name();
        this.lastSeen = node.getLastSeen();
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("host")
    public String getHost() {
        return host;
    }

    @JsonProperty("port")
    public int getPort() {
        return port;
    }

    @JsonProperty("adminPort")
    public int getAdminPort() {
        return adminPort;
    }

    @JsonProperty("status")
    public String getStatus() {
        return status;
    }

    @JsonProperty("lastSeen")
    public Instant getLastSeen() {
        return lastSeen;
    }
}
