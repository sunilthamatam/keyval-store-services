package com.constelld.keyvalstore.core.cluster;

import com.constelld.keyvalstore.core.model.Node;

import java.util.List;
import java.util.Optional;

/**
 * Interface for managing cluster membership and node discovery.
 */
public interface ClusterManager {

    /**
     * Start the cluster manager and begin node discovery.
     */
    void start();

    /**
     * Stop the cluster manager.
     */
    void stop();

    /**
     * Get the current node.
     *
     * @return the current node
     */
    Node getCurrentNode();

    /**
     * Get all active nodes in the cluster.
     *
     * @return list of active nodes
     */
    List<Node> getActiveNodes();

    /**
     * Get all nodes in the cluster (including inactive).
     *
     * @return list of all nodes
     */
    List<Node> getAllNodes();

    /**
     * Get a node by ID.
     *
     * @param nodeId the node ID
     * @return the node if found
     */
    Optional<Node> getNode(String nodeId);

    /**
     * Add a node to the cluster.
     *
     * @param node the node to add
     */
    void addNode(Node node);

    /**
     * Remove a node from the cluster.
     *
     * @param nodeId the node ID
     */
    void removeNode(String nodeId);

    /**
     * Update node status.
     *
     * @param nodeId the node ID
     * @param status the new status
     */
    void updateNodeStatus(String nodeId, Node.NodeStatus status);

    /**
     * Join the cluster by connecting to seed nodes.
     *
     * @param seedNodes list of seed node addresses (host:port)
     */
    void joinCluster(List<String> seedNodes);

    /**
     * Leave the cluster gracefully.
     */
    void leaveCluster();

    /**
     * Check if the cluster manager is running.
     *
     * @return true if running
     */
    boolean isRunning();
}
