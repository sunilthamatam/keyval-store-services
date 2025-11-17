package com.keyvalstore.cluster.replication;

import com.keyvalstore.cluster.client.ClusterHttpClient;
import com.keyvalstore.core.cluster.PartitionManager;
import com.keyvalstore.core.cluster.ReplicationManager;
import com.keyvalstore.core.exception.ClusterException;
import com.keyvalstore.core.model.ConsistencyLevel;
import com.keyvalstore.core.model.KeyValue;
import com.keyvalstore.core.model.Node;
import com.keyvalstore.core.storage.StorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of replication manager with actual HTTP-based replication.
 * Handles replication of data across cluster nodes.
 */
public class DefaultReplicationManager implements ReplicationManager {
    private static final Logger log = LoggerFactory.getLogger(DefaultReplicationManager.class);

    private final int replicationFactor;
    private final PartitionManager partitionManager;
    private final StorageEngine localStorage;
    private final Node currentNode;
    private final ExecutorService executorService;
    private final ClusterHttpClient httpClient;

    public DefaultReplicationManager(
            int replicationFactor,
            PartitionManager partitionManager,
            StorageEngine localStorage,
            Node currentNode) {
        this.replicationFactor = replicationFactor;
        this.partitionManager = partitionManager;
        this.localStorage = localStorage;
        this.currentNode = currentNode;
        this.executorService = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()));
        this.httpClient = new ClusterHttpClient();

        log.info("DefaultReplicationManager initialized with replication factor: {}",
                replicationFactor);
    }

    @Override
    public CompletableFuture<Void> replicateWrite(KeyValue keyValue, ConsistencyLevel consistencyLevel) {
        String key = keyValue.getNamespace() + ":" + keyValue.getKey();
        List<Node> replicaNodes = partitionManager.getReplicaNodes(key, replicationFactor);

        int requiredAcks = consistencyLevel.getRequiredReplicas(replicationFactor);
        log.debug("Replicating write for key '{}' to {} nodes (required acks: {})",
                key, replicaNodes.size(), requiredAcks);

        return replicateToNodes(keyValue, replicaNodes, requiredAcks);
    }

    @Override
    public CompletableFuture<Void> replicateDelete(String namespace, String key, ConsistencyLevel consistencyLevel) {
        String compositeKey = namespace + ":" + key;
        List<Node> replicaNodes = partitionManager.getReplicaNodes(compositeKey, replicationFactor);

        int requiredAcks = consistencyLevel.getRequiredReplicas(replicationFactor);
        log.debug("Replicating delete for key '{}' to {} nodes (required acks: {})",
                compositeKey, replicaNodes.size(), requiredAcks);

        return deleteFromNodes(namespace, key, replicaNodes, requiredAcks);
    }

    @Override
    public CompletableFuture<KeyValue> replicatedRead(String namespace, String key, ConsistencyLevel consistencyLevel) {
        String compositeKey = namespace + ":" + key;
        List<Node> replicaNodes = partitionManager.getReplicaNodes(compositeKey, replicationFactor);

        int requiredReplicas = consistencyLevel.getRequiredReplicas(replicationFactor);
        log.debug("Reading key '{}' from {} nodes (required: {})",
                compositeKey, replicaNodes.size(), requiredReplicas);

        return readFromNodes(namespace, key, replicaNodes, requiredReplicas);
    }

    @Override
    public int getReplicationFactor() {
        return replicationFactor;
    }

    /**
     * Replicate a key-value to multiple nodes using HTTP.
     */
    private CompletableFuture<Void> replicateToNodes(KeyValue keyValue, List<Node> nodes, int requiredAcks) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        AtomicInteger ackCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Node node : nodes) {
            CompletableFuture<Boolean> future;

            if (node.equals(currentNode)) {
                // Local write
                future = CompletableFuture.supplyAsync(() -> {
                    try {
                        localStorage.put(keyValue.getNamespace(), keyValue.getKey(), keyValue.getValue());
                        log.trace("Local write successful for key '{}'", keyValue.getKey());
                        return true;
                    } catch (Exception e) {
                        log.error("Local write failed for key '{}': {}", keyValue.getKey(), e.getMessage());
                        return false;
                    }
                }, executorService);
            } else {
                // Remote write via HTTP
                future = httpClient.replicatePut(node, keyValue);
            }

            future.thenAccept(success -> {
                if (success) {
                    if (ackCount.incrementAndGet() >= requiredAcks && !result.isDone()) {
                        result.complete(null);
                    }
                } else {
                    if (failureCount.incrementAndGet() > (nodes.size() - requiredAcks) && !result.isDone()) {
                        result.completeExceptionally(
                                new ClusterException("Failed to achieve required consistency level"));
                    }
                }
            });

            futures.add(future);
        }

        // Ensure all futures complete or timeout
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, t) -> {
                    if (!result.isDone()) {
                        if (ackCount.get() >= requiredAcks) {
                            result.complete(null);
                        } else {
                            result.completeExceptionally(
                                    new ClusterException("Failed to achieve required consistency level"));
                        }
                    }
                });

        return result;
    }

    /**
     * Delete a key from multiple nodes using HTTP.
     */
    private CompletableFuture<Void> deleteFromNodes(String namespace, String key, List<Node> nodes, int requiredAcks) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        AtomicInteger ackCount = new AtomicInteger(0);

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Node node : nodes) {
            CompletableFuture<Boolean> future;

            if (node.equals(currentNode)) {
                // Local delete
                future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return localStorage.delete(namespace, key);
                    } catch (Exception e) {
                        log.error("Local delete failed for key '{}': {}", key, e.getMessage());
                        return false;
                    }
                }, executorService);
            } else {
                // Remote delete via HTTP
                future = httpClient.replicateDelete(node, namespace, key);
            }

            future.thenAccept(success -> {
                if (success && ackCount.incrementAndGet() >= requiredAcks && !result.isDone()) {
                    result.complete(null);
                }
            });

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((v, t) -> {
                    if (!result.isDone()) {
                        if (ackCount.get() >= requiredAcks) {
                            result.complete(null);
                        }
                    }
                });

        return result;
    }

    /**
     * Read from multiple nodes.
     */
    private CompletableFuture<KeyValue> readFromNodes(String namespace, String key, List<Node> nodes, int requiredReplicas) {
        CompletableFuture<KeyValue> result = new CompletableFuture<>();
        AtomicInteger responseCount = new AtomicInteger(0);

        for (Node node : nodes) {
            CompletableFuture.runAsync(() -> {
                try {
                    KeyValue value;
                    if (node.equals(currentNode)) {
                        value = localStorage.get(namespace, key).orElse(null);
                    } else {
                        // Remote read (would use HTTP client in real implementation)
                        value = null;
                    }

                    if (value != null && !result.isDone()) {
                        result.complete(value);
                    } else if (responseCount.incrementAndGet() >= requiredReplicas && !result.isDone()) {
                        result.complete(null);
                    }
                } catch (Exception e) {
                    log.error("Failed to read from node {}: {}", node.getId(), e.getMessage());
                }
            }, executorService);
        }

        return result;
    }

    public void shutdown() {
        executorService.shutdown();
        httpClient.shutdown();
    }
}
