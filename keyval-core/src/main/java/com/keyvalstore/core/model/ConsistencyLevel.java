package com.keyvalstore.core.model;

/**
 * Defines consistency levels for read and write operations.
 */
public enum ConsistencyLevel {
    /**
     * Only one replica needs to respond.
     * Fastest but least consistent.
     */
    ONE(1),

    /**
     * Quorum of replicas must respond (N/2 + 1).
     * Balanced consistency and availability.
     */
    QUORUM(Integer.MAX_VALUE), // Calculated based on replication factor

    /**
     * All replicas must respond.
     * Strongest consistency but slowest.
     */
    ALL(Integer.MAX_VALUE);

    private final int requiredReplicas;

    ConsistencyLevel(int requiredReplicas) {
        this.requiredReplicas = requiredReplicas;
    }

    /**
     * Calculate the number of required replicas based on replication factor.
     */
    public int getRequiredReplicas(int replicationFactor) {
        return switch (this) {
            case ONE -> 1;
            case QUORUM -> (replicationFactor / 2) + 1;
            case ALL -> replicationFactor;
        };
    }

    public int getRequiredReplicas() {
        return requiredReplicas;
    }
}
