package com.constelld.keyvalstore.core.strategy;

import com.constelld.keyvalstore.core.model.Node;

import java.util.List;

/**
 * Strategy for handling key routing and hashing.
 * Determines which node(s) should store a particular key.
 */
public interface KeyStrategy {

    /**
     * Get the type identifier for this strategy.
     */
    String getType();

    /**
     * Calculate the hash for a given key.
     * This hash is used for consistent hashing to determine node placement.
     *
     * @param namespace the namespace
     * @param key the key
     * @return hash value
     */
    long hash(String namespace, String key);

    /**
     * Determine which nodes should store the key based on the hash ring.
     *
     * @param namespace the namespace
     * @param key the key
     * @param availableNodes list of available nodes
     * @param replicationFactor number of replicas
     * @return ordered list of nodes that should store this key (primary first)
     */
    List<Node> getTargetNodes(String namespace, String key, List<Node> availableNodes, int replicationFactor);

    /**
     * Validate the key format for this strategy.
     *
     * @param key the key to validate
     * @return true if valid
     */
    boolean validateKey(String key);
}
