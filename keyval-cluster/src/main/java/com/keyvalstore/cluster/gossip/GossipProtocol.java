package com.keyvalstore.cluster.gossip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyvalstore.core.cluster.ClusterManager;
import com.keyvalstore.core.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Gossip protocol for anti-entropy and cluster state synchronization.
 * Periodically exchanges cluster state with random nodes to detect failures
 * and propagate updates.
 */
public class GossipProtocol {
    private static final Logger log = LoggerFactory.getLogger(GossipProtocol.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ClusterManager clusterManager;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final Random random;

    // Gossip every 5 seconds
    private static final long GOSSIP_INTERVAL_SECONDS = 5;
    // Number of random nodes to gossip with
    private static final int GOSSIP_FANOUT = 3;

    private volatile boolean running;

    public GossipProtocol(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.random = new Random();
        this.running = false;
    }

    /**
     * Start the gossip protocol.
     */
    public void start() {
        if (running) {
            log.warn("Gossip protocol is already running");
            return;
        }

        running = true;

        scheduler.scheduleAtFixedRate(
                this::performGossipRound,
                GOSSIP_INTERVAL_SECONDS,
                GOSSIP_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("Gossip protocol started (interval: {}s, fanout: {})",
                GOSSIP_INTERVAL_SECONDS, GOSSIP_FANOUT);
    }

    /**
     * Stop the gossip protocol.
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        scheduler.shutdown();

        log.info("Gossip protocol stopped");
    }

    /**
     * Perform one round of gossip.
     */
    private void performGossipRound() {
        if (!running) {
            return;
        }

        try {
            List<Node> activeNodes = clusterManager.getActiveNodes();
            Node currentNode = clusterManager.getCurrentNode();

            // Remove current node from the list
            List<Node> otherNodes = activeNodes.stream()
                    .filter(node -> !node.equals(currentNode))
                    .toList();

            if (otherNodes.isEmpty()) {
                log.trace("No other nodes to gossip with");
                return;
            }

            // Select random nodes to gossip with
            int fanout = Math.min(GOSSIP_FANOUT, otherNodes.size());
            for (int i = 0; i < fanout; i++) {
                Node target = otherNodes.get(random.nextInt(otherNodes.size()));
                gossipWith(target);
            }
        } catch (Exception e) {
            log.error("Error during gossip round: {}", e.getMessage(), e);
        }
    }

    /**
     * Gossip with a specific node.
     */
    private void gossipWith(Node target) {
        try {
            Node currentNode = clusterManager.getCurrentNode();
            List<Node> allNodes = clusterManager.getAllNodes();

            // Create gossip message
            GossipMessage message = GossipMessage.createSyncMessage(currentNode, allNodes);
            String messageJson = OBJECT_MAPPER.writeValueAsString(message);

            // Send to target node
            String url = String.format("http://%s:%d/admin/gossip",
                    target.getHost(), target.getAdminPort());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(messageJson))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200) {
                            handleGossipResponse(response.body());
                        } else {
                            log.debug("Gossip to node {} failed: HTTP {}",
                                    target.getId(), response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        log.debug("Gossip to node {} failed: {}", target.getId(), ex.getMessage());
                        return null;
                    });

            log.trace("Sent gossip message to node {}", target.getId());
        } catch (Exception e) {
            log.error("Failed to gossip with node {}: {}", target.getId(), e.getMessage());
        }
    }

    /**
     * Handle gossip response from another node.
     */
    private void handleGossipResponse(String responseBody) {
        try {
            GossipMessage response = OBJECT_MAPPER.readValue(responseBody, GossipMessage.class);

            // Merge cluster state
            for (Node node : response.getClusterNodes()) {
                if (!clusterManager.getNode(node.getId()).isPresent()) {
                    log.info("Discovered new node via gossip: {}", node.getId());
                    clusterManager.addNode(node);
                }
            }
        } catch (Exception e) {
            log.error("Failed to handle gossip response: {}", e.getMessage());
        }
    }

    /**
     * Handle incoming gossip message.
     */
    public GossipMessage handleIncomingGossip(GossipMessage message) {
        log.trace("Received gossip from node {}", message.getSender().getId());

        // Update sender's last seen time
        clusterManager.getNode(message.getSender().getId())
                .ifPresent(node -> clusterManager.updateNodeStatus(
                        node.getId(), node.getStatus()));

        // Merge cluster nodes
        for (Node node : message.getClusterNodes()) {
            if (!clusterManager.getNode(node.getId()).isPresent()) {
                log.info("Discovered new node via gossip: {}", node.getId());
                clusterManager.addNode(node);
            }
        }

        // Send ACK with our cluster state
        Node currentNode = clusterManager.getCurrentNode();
        List<Node> allNodes = clusterManager.getAllNodes();

        return GossipMessage.createSyncMessage(currentNode, allNodes);
    }

    public boolean isRunning() {
        return running;
    }
}
