package com.keyvalstore.core.cluster;

import com.keyvalstore.core.model.Node;

import java.util.List;

/**
 * Interface for managing data partitioning using consistent hashing.
 */
public interface PartitionManager {

    /**
     * Add a node to the hash ring.
     *
     * @param node the node to add
     */
    void addNode(Node node);

    /**
     * Remove a node from the hash ring.
     *
     * @param node the node to remove
     */
    void removeNode(Node node);

    /**
     * Get the primary node for a key.
     *
     * @param key the key
     * @return the primary node
     */
    Node getPrimaryNode(String key);

    /**
     * Get replica nodes for a key.
     *
     * @param key the key
     * @param replicationFactor number of replicas
     * @return ordered list of nodes (primary first, then replicas)
     */
    List<Node> getReplicaNodes(String key, int replicationFactor);

    /**
     * Get all nodes in the ring.
     *
     * @return list of nodes
     */
    List<Node> getAllNodes();

    /**
     * Get the number of virtual nodes per physical node.
     *
     * @return virtual nodes count
     */
    int getVirtualNodesPerNode();
}
