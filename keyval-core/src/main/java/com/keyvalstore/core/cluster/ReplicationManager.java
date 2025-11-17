package com.keyvalstore.core.cluster;

import com.keyvalstore.core.model.ConsistencyLevel;
import com.keyvalstore.core.model.KeyValue;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for managing data replication across cluster nodes.
 */
public interface ReplicationManager {

    /**
     * Replicate a write operation to replica nodes.
     *
     * @param keyValue the key-value to replicate
     * @param consistencyLevel the consistency level
     * @return future that completes when replication satisfies consistency level
     */
    CompletableFuture<Void> replicateWrite(KeyValue keyValue, ConsistencyLevel consistencyLevel);

    /**
     * Replicate a delete operation to replica nodes.
     *
     * @param namespace the namespace
     * @param key the key
     * @param consistencyLevel the consistency level
     * @return future that completes when replication satisfies consistency level
     */
    CompletableFuture<Void> replicateDelete(String namespace, String key, ConsistencyLevel consistencyLevel);

    /**
     * Read from replicas with specified consistency level.
     *
     * @param namespace the namespace
     * @param key the key
     * @param consistencyLevel the consistency level
     * @return the key-value if found
     */
    CompletableFuture<KeyValue> replicatedRead(String namespace, String key, ConsistencyLevel consistencyLevel);

    /**
     * Get the replication factor.
     *
     * @return replication factor
     */
    int getReplicationFactor();
}
