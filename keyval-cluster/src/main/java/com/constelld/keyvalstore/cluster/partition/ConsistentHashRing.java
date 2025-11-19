package com.constelld.keyvalstore.cluster.partition;

import com.google.common.hash.Hashing;
import com.constelld.keyvalstore.core.cluster.PartitionManager;
import com.constelld.keyvalstore.core.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Consistent hash ring implementation for data partitioning.
 * Uses virtual nodes to ensure even distribution.
 */
public class ConsistentHashRing implements PartitionManager {
    private static final Logger log = LoggerFactory.getLogger(ConsistentHashRing.class);

    private final int virtualNodesPerNode;
    private final ConcurrentSkipListMap<Long, Node> ring;
    private final Map<String, Node> nodeMap; // nodeId -> Node
    private final ReadWriteLock lock;

    public ConsistentHashRing(int virtualNodesPerNode) {
        this.virtualNodesPerNode = virtualNodesPerNode;
        this.ring = new ConcurrentSkipListMap<>();
        this.nodeMap = new HashMap<>();
        this.lock = new ReentrantReadWriteLock();

        log.info("ConsistentHashRing initialized with {} virtual nodes per physical node",
                virtualNodesPerNode);
    }

    @Override
    public void addNode(Node node) {
        lock.writeLock().lock();
        try {
            if (nodeMap.containsKey(node.getId())) {
                log.warn("Node {} already exists in the ring", node.getId());
                return;
            }

            // Add virtual nodes
            for (int i = 0; i < virtualNodesPerNode; i++) {
                String virtualNodeKey = node.getId() + "#" + i;
                long hash = hash(virtualNodeKey);
                ring.put(hash, node);
            }

            nodeMap.put(node.getId(), node);
            log.info("Added node {} to hash ring with {} virtual nodes",
                    node.getId(), virtualNodesPerNode);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeNode(Node node) {
        lock.writeLock().lock();
        try {
            if (!nodeMap.containsKey(node.getId())) {
                log.warn("Node {} does not exist in the ring", node.getId());
                return;
            }

            // Remove virtual nodes
            for (int i = 0; i < virtualNodesPerNode; i++) {
                String virtualNodeKey = node.getId() + "#" + i;
                long hash = hash(virtualNodeKey);
                ring.remove(hash);
            }

            nodeMap.remove(node.getId());
            log.info("Removed node {} from hash ring", node.getId());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Node getPrimaryNode(String key) {
        lock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                throw new IllegalStateException("Hash ring is empty");
            }

            long hash = hash(key);
            Map.Entry<Long, Node> entry = ring.ceilingEntry(hash);

            // Wrap around if necessary
            if (entry == null) {
                entry = ring.firstEntry();
            }

            return entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Node> getReplicaNodes(String key, int replicationFactor) {
        lock.readLock().lock();
        try {
            if (ring.isEmpty()) {
                throw new IllegalStateException("Hash ring is empty");
            }

            int actualReplicationFactor = Math.min(replicationFactor, nodeMap.size());
            List<Node> replicas = new ArrayList<>(actualReplicationFactor);
            Set<String> addedNodeIds = new HashSet<>();

            long hash = hash(key);
            Long currentHash = hash;

            // Get unique nodes (skip virtual nodes of same physical node)
            while (replicas.size() < actualReplicationFactor) {
                Map.Entry<Long, Node> entry = ring.ceilingEntry(currentHash);

                // Wrap around
                if (entry == null) {
                    entry = ring.firstEntry();
                    if (entry == null) {
                        break;
                    }
                }

                Node node = entry.getValue();
                if (!addedNodeIds.contains(node.getId())) {
                    replicas.add(node);
                    addedNodeIds.add(node.getId());
                }

                // Move to next position in ring
                currentHash = entry.getKey() + 1;

                // Prevent infinite loop
                if (currentHash == hash && replicas.size() > 0) {
                    break;
                }
            }

            log.debug("Selected {} replica nodes for key '{}'", replicas.size(), key);
            return replicas;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Node> getAllNodes() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(nodeMap.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int getVirtualNodesPerNode() {
        return virtualNodesPerNode;
    }

    /**
     * Hash a string using MurmurHash3.
     */
    private long hash(String key) {
        return Hashing.murmur3_128()
                .hashString(key, StandardCharsets.UTF_8)
                .asLong();
    }

    public int getRingSize() {
        return ring.size();
    }

    public int getPhysicalNodeCount() {
        return nodeMap.size();
    }
}
