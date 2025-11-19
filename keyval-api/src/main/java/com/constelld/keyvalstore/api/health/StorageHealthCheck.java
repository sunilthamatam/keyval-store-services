package com.constelld.keyvalstore.api.health;

import com.codahale.metrics.health.HealthCheck;
import com.constelld.keyvalstore.core.storage.StorageEngine;

/**
 * Health check for storage engine.
 */
public class StorageHealthCheck extends HealthCheck {

    private final StorageEngine storageEngine;
    private static final double WARNING_THRESHOLD = 0.8;
    private static final double CRITICAL_THRESHOLD = 0.95;

    public StorageHealthCheck(StorageEngine storageEngine) {
        this.storageEngine = storageEngine;
    }

    @Override
    protected Result check() {
        try {
            StorageEngine.StorageStats stats = storageEngine.getStats();
            double utilization = stats.memoryUtilization();

            if (utilization >= CRITICAL_THRESHOLD) {
                return Result.unhealthy("Memory utilization critical: %.2f%%", utilization * 100);
            } else if (utilization >= WARNING_THRESHOLD) {
                return Result.healthy("Memory utilization warning: %.2f%%", utilization * 100);
            } else {
                return Result.healthy("Memory utilization: %.2f%%", utilization * 100);
            }
        } catch (Exception e) {
            return Result.unhealthy("Failed to check storage health: " + e.getMessage());
        }
    }
}
