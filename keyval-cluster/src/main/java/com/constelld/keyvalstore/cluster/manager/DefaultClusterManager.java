package com.constelld.keyvalstore.cluster.manager;

import com.constelld.keyvalstore.core.cluster.ClusterManager;
import com.constelld.keyvalstore.core.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Default implementation of cluster manager.
 * Manages cluster membership and node discovery.
 */
public class DefaultClusterManager implements ClusterManager {
    private static final Logger log = LoggerFactory.getLogger(DefaultClusterManager.class);

    private final Node currentNode;
    private final ConcurrentHashMap<String, Node> nodes;
    private final ScheduledExecutorService scheduledExecutor;
    private volatile boolean running;

    // Health check interval (30 seconds)
    private static final long HEALTH_CHECK_INTERVAL_SECONDS = 30;
    // Node timeout (90 seconds)
    private static final long NODE_TIMEOUT_SECONDS = 90;

    public DefaultClusterManager(Node currentNode) {
        this.currentNode = currentNode;
        this.nodes = new ConcurrentHashMap<>();
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        this.running = false;

        // Add current node
        this.nodes.put(currentNode.getId(), currentNode);

        log.info("DefaultClusterManager initialized for node {}", currentNode.getId());
    }

    @Override
    public void start() {
        if (running) {
            log.warn("ClusterManager is already running");
            return;
        }

        running = true;

        // Start health check scheduler
        scheduledExecutor.scheduleAtFixedRate(
                this::performHealthCheck,
                HEALTH_CHECK_INTERVAL_SECONDS,
                HEALTH_CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );

        // Update current node status
        updateNodeStatus(currentNode.getId(), Node.NodeStatus.ACTIVE);

        log.info("ClusterManager started");
    }

    @Override
    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        scheduledExecutor.shutdown();

        log.info("ClusterManager stopped");
    }

    @Override
    public Node getCurrentNode() {
        return currentNode;
    }

    @Override
    public List<Node> getActiveNodes() {
        return nodes.values().stream()
                .filter(node -> node.getStatus() == Node.NodeStatus.ACTIVE)
                .toList();
    }

    @Override
    public List<Node> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    @Override
    public Optional<Node> getNode(String nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    @Override
    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        log.info("Added node {} to cluster", node.getId());
    }

    @Override
    public void removeNode(String nodeId) {
        Node removed = nodes.remove(nodeId);
        if (removed != null) {
            log.info("Removed node {} from cluster", nodeId);
        }
    }

    @Override
    public void updateNodeStatus(String nodeId, Node.NodeStatus status) {
        nodes.computeIfPresent(nodeId, (id, node) -> {
            Node updated = node.withStatus(status).updateLastSeen();
            log.debug("Updated node {} status to {}", nodeId, status);
            return updated;
        });
    }

    @Override
    public void joinCluster(List<String> seedNodes) {
        if (seedNodes == null || seedNodes.isEmpty()) {
            log.info("No seed nodes provided, starting as standalone node");
            return;
        }

        log.info("Joining cluster with seed nodes: {}", seedNodes);

        // In a real implementation, this would:
        // 1. Connect to seed nodes via admin port
        // 2. Exchange cluster membership information
        // 3. Add discovered nodes to the cluster
        // For now, we'll simulate this

        for (String seedNodeAddress : seedNodes) {
            try {
                // Parse host:port
                String[] parts = seedNodeAddress.split(":");
                String host = parts[0];
                int adminPort = Integer.parseInt(parts[1]);

                // Simulate discovering a node
                // In real implementation, this would be an HTTP call to the admin port
                log.info("Discovered seed node at {}", seedNodeAddress);

                // Would add the discovered node here
                // addNode(discoveredNode);

            } catch (Exception e) {
                log.error("Failed to connect to seed node {}: {}", seedNodeAddress, e.getMessage());
            }
        }
    }

    @Override
    public void leaveCluster() {
        log.info("Node {} leaving cluster", currentNode.getId());
        updateNodeStatus(currentNode.getId(), Node.NodeStatus.LEAVING);

        // In a real implementation:
        // 1. Notify other nodes about leaving
        // 2. Transfer data to other replicas
        // 3. Wait for acknowledgment
        // 4. Shutdown

        stop();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Perform health check on all nodes.
     */
    private void performHealthCheck() {
        if (!running) {
            return;
        }

        log.debug("Performing health check on {} nodes", nodes.size());

        long now = System.currentTimeMillis();
        long timeoutMillis = NODE_TIMEOUT_SECONDS * 1000;

        for (Node node : nodes.values()) {
            // Skip current node
            if (node.equals(currentNode)) {
                // Update current node's last seen
                updateNodeStatus(currentNode.getId(), Node.NodeStatus.ACTIVE);
                continue;
            }

            // Check if node has timed out
            long lastSeenMillis = node.getLastSeen().toEpochMilli();
            if (now - lastSeenMillis > timeoutMillis) {
                log.warn("Node {} has timed out (last seen: {})", node.getId(), node.getLastSeen());
                updateNodeStatus(node.getId(), Node.NodeStatus.DOWN);
            } else {
                // In real implementation, ping the node
                // For now, assume node is still active if not timed out
            }
        }
    }
}
