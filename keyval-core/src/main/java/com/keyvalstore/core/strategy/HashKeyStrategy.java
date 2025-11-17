package com.keyvalstore.core.strategy;

import com.google.common.hash.Hashing;
import com.keyvalstore.core.model.Node;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Hash-based key strategy using MurmurHash3 for consistent hashing.
 */
public class HashKeyStrategy implements KeyStrategy {
    private static final String TYPE = "HASH";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public long hash(String namespace, String key) {
        String combinedKey = namespace + ":" + key;
        return Hashing.murmur3_128()
                .hashString(combinedKey, StandardCharsets.UTF_8)
                .asLong();
    }

    @Override
    public List<Node> getTargetNodes(String namespace, String key, List<Node> availableNodes, int replicationFactor) {
        // This is a placeholder - actual implementation will be in PartitionManager
        // which uses the consistent hash ring
        throw new UnsupportedOperationException(
                "Use PartitionManager.getReplicaNodes() for actual node selection");
    }

    @Override
    public boolean validateKey(String key) {
        return key != null && !key.isEmpty() && key.length() <= 1024;
    }
}
