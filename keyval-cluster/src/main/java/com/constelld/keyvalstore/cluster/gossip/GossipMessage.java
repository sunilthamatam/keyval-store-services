package com.constelld.keyvalstore.cluster.gossip;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.constelld.keyvalstore.core.model.Node;

import java.util.List;
import java.util.Map;

/**
 * Gossip message for cluster state synchronization.
 */
public class GossipMessage {

    public enum MessageType {
        SYNC,           // Full state sync
        UPDATE,         // Incremental update
        ACK             // Acknowledgment
    }

    private final MessageType type;
    private final Node sender;
    private final long timestamp;
    private final List<Node> clusterNodes;
    private final Map<String, Object> metadata;

    @JsonCreator
    public GossipMessage(
            @JsonProperty("type") MessageType type,
            @JsonProperty("sender") Node sender,
            @JsonProperty("timestamp") long timestamp,
            @JsonProperty("clusterNodes") List<Node> clusterNodes,
            @JsonProperty("metadata") Map<String, Object> metadata) {
        this.type = type;
        this.sender = sender;
        this.timestamp = timestamp;
        this.clusterNodes = clusterNodes;
        this.metadata = metadata;
    }

    public static GossipMessage createSyncMessage(Node sender, List<Node> clusterNodes) {
        return new GossipMessage(
                MessageType.SYNC,
                sender,
                System.currentTimeMillis(),
                clusterNodes,
                Map.of()
        );
    }

    public static GossipMessage createAck(Node sender) {
        return new GossipMessage(
                MessageType.ACK,
                sender,
                System.currentTimeMillis(),
                List.of(),
                Map.of()
        );
    }

    @JsonProperty("type")
    public MessageType getType() {
        return type;
    }

    @JsonProperty("sender")
    public Node getSender() {
        return sender;
    }

    @JsonProperty("timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    @JsonProperty("clusterNodes")
    public List<Node> getClusterNodes() {
        return clusterNodes;
    }

    @JsonProperty("metadata")
    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
