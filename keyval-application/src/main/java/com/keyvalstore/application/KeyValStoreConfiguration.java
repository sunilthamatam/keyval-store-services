package com.keyvalstore.application;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Main configuration for KeyVal Store application.
 */
public class KeyValStoreConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty("cluster")
    private ClusterConfiguration cluster = new ClusterConfiguration();

    @Valid
    @NotNull
    @JsonProperty("storage")
    private StorageConfiguration storage = new StorageConfiguration();

    @Valid
    @NotNull
    @JsonProperty("consistency")
    private ConsistencyConfiguration consistency = new ConsistencyConfiguration();

    @Valid
    @NotNull
    @JsonProperty("database")
    private DatabaseConfiguration database = new DatabaseConfiguration();

    public ClusterConfiguration getCluster() {
        return cluster;
    }

    public void setCluster(ClusterConfiguration cluster) {
        this.cluster = cluster;
    }

    public StorageConfiguration getStorage() {
        return storage;
    }

    public void setStorage(StorageConfiguration storage) {
        this.storage = storage;
    }

    public ConsistencyConfiguration getConsistency() {
        return consistency;
    }

    public void setConsistency(ConsistencyConfiguration consistency) {
        this.consistency = consistency;
    }

    public DatabaseConfiguration getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseConfiguration database) {
        this.database = database;
    }

    /**
     * Cluster configuration.
     */
    public static class ClusterConfiguration {
        @JsonProperty
        private String nodeName = "node1";

        @JsonProperty
        private String nodeId = "node1";

        @JsonProperty
        private String host = "localhost";

        @JsonProperty
        private int port = 8080;

        @JsonProperty
        private int adminPort = 9090;

        @JsonProperty
        private List<String> seedNodes = new ArrayList<>();

        @JsonProperty
        private int replicationFactor = 3;

        @JsonProperty
        private int virtualNodesPerNode = 150;

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getAdminPort() {
            return adminPort;
        }

        public void setAdminPort(int adminPort) {
            this.adminPort = adminPort;
        }

        public List<String> getSeedNodes() {
            return seedNodes;
        }

        public void setSeedNodes(List<String> seedNodes) {
            this.seedNodes = seedNodes;
        }

        public int getReplicationFactor() {
            return replicationFactor;
        }

        public void setReplicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
        }

        public int getVirtualNodesPerNode() {
            return virtualNodesPerNode;
        }

        public void setVirtualNodesPerNode(int virtualNodesPerNode) {
            this.virtualNodesPerNode = virtualNodesPerNode;
        }
    }

    /**
     * Storage configuration.
     */
    public static class StorageConfiguration {
        @JsonProperty
        private String maxOffHeapMemory = "2GB";

        public String getMaxOffHeapMemory() {
            return maxOffHeapMemory;
        }

        public void setMaxOffHeapMemory(String maxOffHeapMemory) {
            this.maxOffHeapMemory = maxOffHeapMemory;
        }

        /**
         * Parse memory size string to bytes.
         */
        public long getMaxOffHeapMemoryBytes() {
            String size = maxOffHeapMemory.toUpperCase();

            if (size.endsWith("GB")) {
                return Long.parseLong(size.substring(0, size.length() - 2)) * 1024L * 1024L * 1024L;
            } else if (size.endsWith("MB")) {
                return Long.parseLong(size.substring(0, size.length() - 2)) * 1024L * 1024L;
            } else if (size.endsWith("KB")) {
                return Long.parseLong(size.substring(0, size.length() - 2)) * 1024L;
            } else {
                return Long.parseLong(size);
            }
        }
    }

    /**
     * Consistency configuration.
     */
    public static class ConsistencyConfiguration {
        @JsonProperty
        private String defaultReadLevel = "QUORUM";

        @JsonProperty
        private String defaultWriteLevel = "QUORUM";

        public String getDefaultReadLevel() {
            return defaultReadLevel;
        }

        public void setDefaultReadLevel(String defaultReadLevel) {
            this.defaultReadLevel = defaultReadLevel;
        }

        public String getDefaultWriteLevel() {
            return defaultWriteLevel;
        }

        public void setDefaultWriteLevel(String defaultWriteLevel) {
            this.defaultWriteLevel = defaultWriteLevel;
        }
    }

    /**
     * Database configuration.
     */
    public static class DatabaseConfiguration {
        @JsonProperty
        private String driverClass = "org.sqlite.JDBC";

        @JsonProperty
        private String url = "jdbc:sqlite:keyvalstore.db";

        public String getDriverClass() {
            return driverClass;
        }

        public void setDriverClass(String driverClass) {
            this.driverClass = driverClass;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
