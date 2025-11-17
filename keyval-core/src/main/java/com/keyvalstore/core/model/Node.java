package com.keyvalstore.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a node in the cluster.
 */
public class Node {
    private final String id;
    private final String host;
    private final int port;
    private final int adminPort;
    private final NodeStatus status;
    private final Instant lastSeen;

    @JsonCreator
    public Node(
            @JsonProperty("id") String id,
            @JsonProperty("host") String host,
            @JsonProperty("port") int port,
            @JsonProperty("adminPort") int adminPort,
            @JsonProperty("status") NodeStatus status,
            @JsonProperty("lastSeen") Instant lastSeen) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.host = Objects.requireNonNull(host, "host cannot be null");
        this.port = port;
        this.adminPort = adminPort;
        this.status = status != null ? status : NodeStatus.STARTING;
        this.lastSeen = lastSeen != null ? lastSeen : Instant.now();
    }

    public Node(String id, String host, int port, int adminPort) {
        this(id, host, port, adminPort, NodeStatus.STARTING, Instant.now());
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
    public NodeStatus getStatus() {
        return status;
    }

    @JsonProperty("lastSeen")
    public Instant getLastSeen() {
        return lastSeen;
    }

    public Node withStatus(NodeStatus newStatus) {
        return new Node(id, host, port, adminPort, newStatus, Instant.now());
    }

    public Node updateLastSeen() {
        return new Node(id, host, port, adminPort, status, Instant.now());
    }

    public String getAddress() {
        return host + ":" + port;
    }

    public String getAdminAddress() {
        return host + ":" + adminPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", adminPort=" + adminPort +
                ", status=" + status +
                ", lastSeen=" + lastSeen +
                '}';
    }

    public enum NodeStatus {
        STARTING,
        ACTIVE,
        LEAVING,
        DOWN
    }
}
