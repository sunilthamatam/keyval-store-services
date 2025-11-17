package com.keyvalstore.cluster.replication;

import com.keyvalstore.cluster.client.ClusterHttpClient;
import com.keyvalstore.core.model.KeyValue;
import com.keyvalstore.core.model.Node;
import com.keyvalstore.core.storage.StorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Read repair manager to fix inconsistencies detected during reads.
 * When reading from multiple replicas, if inconsistencies are detected,
 * this manager repairs the out-of-date replicas.
 */
public class ReadRepairManager {
    private static final Logger log = LoggerFactory.getLogger(ReadRepairManager.class);

    private final StorageEngine localStorage;
    private final ClusterHttpClient httpClient;
    private final Node currentNode;

    public ReadRepairManager(StorageEngine localStorage, Node currentNode) {
        this.localStorage = localStorage;
        this.httpClient = new ClusterHttpClient();
        this.currentNode = currentNode;
    }

    /**
     * Perform read repair by comparing values from multiple replicas.
     * The most recent version (highest version number) is considered correct.
     *
     * @param namespace the namespace
     * @param key the key
     * @param replicaNodes the replica nodes
     * @return the most recent value
     */
    public CompletableFuture<KeyValue> performReadRepair(String namespace, String key, List<Node> replicaNodes) {
        log.debug("Performing read repair for key '{}' in namespace '{}'", key, namespace);

        // Read from all replicas
        List<CompletableFuture<KeyValue>> readFutures = new ArrayList<>();

        for (Node node : replicaNodes) {
            CompletableFuture<KeyValue> future;

            if (node.equals(currentNode)) {
                // Local read
                future = CompletableFuture.completedFuture(
                        localStorage.get(namespace, key).orElse(null)
                );
            } else {
                // Remote read
                future = httpClient.remoteGet(node, namespace, key)
                        .thenApply(optValue -> {
                            // This is simplified - in reality we'd deserialize to KeyValue
                            return null; // Would parse JSON response
                        });
            }

            readFutures.add(future);
        }

        // Wait for all reads to complete
        return CompletableFuture.allOf(readFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // Collect all values
                    List<KeyValue> values = readFutures.stream()
                            .map(CompletableFuture::join)
                            .filter(val -> val != null)
                            .toList();

                    if (values.isEmpty()) {
                        return null;
                    }

                    // Find the most recent version
                    KeyValue mostRecent = values.stream()
                            .max((a, b) -> Long.compare(a.getVersion(), b.getVersion()))
                            .orElse(null);

                    if (mostRecent == null) {
                        return null;
                    }

                    // Check if repair is needed
                    boolean repairNeeded = values.stream()
                            .anyMatch(val -> val.getVersion() < mostRecent.getVersion());

                    if (repairNeeded) {
                        log.info("Inconsistency detected for key '{}', performing repair", key);
                        repairOutdatedReplicas(mostRecent, replicaNodes);
                    }

                    return mostRecent;
                });
    }

    /**
     * Repair outdated replicas by writing the most recent value.
     */
    private void repairOutdatedReplicas(KeyValue correctValue, List<Node> replicaNodes) {
        for (Node node : replicaNodes) {
            if (node.equals(currentNode)) {
                // Update local copy
                try {
                    localStorage.put(correctValue.getNamespace(), correctValue.getKey(), correctValue.getValue());
                    log.debug("Repaired local replica for key '{}'", correctValue.getKey());
                } catch (Exception e) {
                    log.error("Failed to repair local replica: {}", e.getMessage());
                }
            } else {
                // Repair remote replica
                httpClient.replicatePut(node, correctValue)
                        .thenAccept(success -> {
                            if (success) {
                                log.debug("Repaired replica on node {} for key '{}'",
                                        node.getId(), correctValue.getKey());
                            } else {
                                log.warn("Failed to repair replica on node {} for key '{}'",
                                        node.getId(), correctValue.getKey());
                            }
                        });
            }
        }
    }

    public void shutdown() {
        httpClient.shutdown();
    }
}
