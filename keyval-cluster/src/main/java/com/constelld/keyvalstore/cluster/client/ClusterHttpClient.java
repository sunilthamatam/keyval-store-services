package com.constelld.keyvalstore.cluster.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.constelld.keyvalstore.core.model.KeyValue;
import com.constelld.keyvalstore.core.model.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for communicating with remote cluster nodes.
 */
public class ClusterHttpClient {
    private static final Logger log = LoggerFactory.getLogger(ClusterHttpClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient;
    private final Duration timeout;

    public ClusterHttpClient(Duration timeout) {
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
    }

    public ClusterHttpClient() {
        this(Duration.ofSeconds(5));
    }

    /**
     * Replicate a PUT operation to a remote node.
     */
    public CompletableFuture<Boolean> replicatePut(Node node, KeyValue keyValue) {
        try {
            String url = String.format("http://%s:%d/api/v1/namespaces/%s/kv/%s",
                    node.getHost(), node.getPort(), keyValue.getNamespace(), keyValue.getKey());

            String valueJson = new String(keyValue.getValue(), StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("X-Replication", "true") // Mark as replication request
                    .PUT(HttpRequest.BodyPublishers.ofString(valueJson))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            log.trace("Replicated PUT to node {} for key {}", node.getId(), keyValue.getKey());
                            return true;
                        } else {
                            log.warn("Failed to replicate PUT to node {}: HTTP {}", node.getId(), response.statusCode());
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Error replicating PUT to node {}: {}", node.getId(), ex.getMessage());
                        return false;
                    });
        } catch (Exception e) {
            log.error("Failed to create replication request", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Replicate a DELETE operation to a remote node.
     */
    public CompletableFuture<Boolean> replicateDelete(Node node, String namespace, String key) {
        try {
            String url = String.format("http://%s:%d/api/v1/namespaces/%s/kv/%s",
                    node.getHost(), node.getPort(), namespace, key);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("X-Replication", "true")
                    .DELETE()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            log.trace("Replicated DELETE to node {} for key {}", node.getId(), key);
                            return true;
                        } else {
                            log.warn("Failed to replicate DELETE to node {}: HTTP {}", node.getId(), response.statusCode());
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Error replicating DELETE to node {}: {}", node.getId(), ex.getMessage());
                        return false;
                    });
        } catch (Exception e) {
            log.error("Failed to create replication request", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Read a value from a remote node.
     */
    public CompletableFuture<Optional<String>> remoteGet(Node node, String namespace, String key) {
        try {
            String url = String.format("http://%s:%d/api/v1/namespaces/%s/kv/%s",
                    node.getHost(), node.getPort(), namespace, key);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() == 200) {
                            return Optional.of(response.body());
                        } else if (response.statusCode() == 404) {
                            return Optional.empty();
                        } else {
                            log.warn("Failed to read from node {}: HTTP {}", node.getId(), response.statusCode());
                            return Optional.empty();
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Error reading from node {}: {}", node.getId(), ex.getMessage());
                        return Optional.empty();
                    });
        } catch (Exception e) {
            log.error("Failed to create read request", e);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    /**
     * Send a health check ping to a remote node.
     */
    public CompletableFuture<Boolean> ping(Node node) {
        try {
            String url = String.format("http://%s:%d/admin/health", node.getHost(), node.getAdminPort());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> response.statusCode() == 200)
                    .exceptionally(ex -> false);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Join a cluster by contacting a seed node.
     */
    public CompletableFuture<Boolean> joinCluster(Node seedNode, Node currentNode) {
        try {
            String url = String.format("http://%s:%d/admin/cluster/join",
                    seedNode.getHost(), seedNode.getAdminPort());

            String nodeJson = OBJECT_MAPPER.writeValueAsString(currentNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(nodeJson))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 300) {
                            log.info("Successfully joined cluster via seed node {}", seedNode.getId());
                            return true;
                        } else {
                            log.warn("Failed to join cluster via seed node {}: HTTP {}",
                                    seedNode.getId(), response.statusCode());
                            return false;
                        }
                    })
                    .exceptionally(ex -> {
                        log.error("Error joining cluster via seed node {}: {}", seedNode.getId(), ex.getMessage());
                        return false;
                    });
        } catch (Exception e) {
            log.error("Failed to create join request", e);
            return CompletableFuture.completedFuture(false);
        }
    }

    public void shutdown() {
        // HttpClient doesn't require explicit shutdown
        log.info("ClusterHttpClient shut down");
    }
}
