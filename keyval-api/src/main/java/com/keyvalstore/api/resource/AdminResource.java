package com.keyvalstore.api.resource;

import com.keyvalstore.api.dto.NodeResponse;
import com.keyvalstore.cluster.gossip.GossipMessage;
import com.keyvalstore.cluster.gossip.GossipProtocol;
import com.keyvalstore.core.cluster.ClusterManager;
import com.keyvalstore.core.model.Node;
import com.keyvalstore.core.storage.StorageEngine;
import com.keyvalstore.storage.snapshot.SnapshotManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Administrative operations")
public class AdminResource {
    private static final Logger log = LoggerFactory.getLogger(AdminResource.class);

    private final ClusterManager clusterManager;
    private final StorageEngine storageEngine;
    private final GossipProtocol gossipProtocol;
    private final SnapshotManager snapshotManager;

    public AdminResource(ClusterManager clusterManager, StorageEngine storageEngine,
                        GossipProtocol gossipProtocol, SnapshotManager snapshotManager) {
        this.clusterManager = clusterManager;
        this.storageEngine = storageEngine;
        this.gossipProtocol = gossipProtocol;
        this.snapshotManager = snapshotManager;
    }

    @GET
    @Path("/nodes")
    @Operation(summary = "List all cluster nodes")
    @ApiResponse(responseCode = "200", description = "Nodes retrieved successfully")
    public Response listNodes() {
        List<Node> nodes = clusterManager.getAllNodes();
        List<NodeResponse> responses = nodes.stream()
                .map(NodeResponse::new)
                .toList();

        return Response.ok(responses).build();
    }

    @GET
    @Path("/nodes/active")
    @Operation(summary = "List active cluster nodes")
    @ApiResponse(responseCode = "200", description = "Active nodes retrieved successfully")
    public Response listActiveNodes() {
        List<Node> nodes = clusterManager.getActiveNodes();
        List<NodeResponse> responses = nodes.stream()
                .map(NodeResponse::new)
                .toList();

        return Response.ok(responses).build();
    }

    @GET
    @Path("/health")
    @Operation(summary = "Health check")
    @ApiResponse(responseCode = "200", description = "Service is healthy")
    public Response health() {
        Node currentNode = clusterManager.getCurrentNode();
        StorageEngine.StorageStats stats = storageEngine.getStats();

        Map<String, Object> health = Map.of(
                "status", "UP",
                "nodeId", currentNode.getId(),
                "nodeStatus", currentNode.getStatus().name(),
                "storage", Map.of(
                        "totalKeys", stats.totalKeys(),
                        "memoryUsed", stats.totalMemoryUsed(),
                        "maxMemory", stats.maxMemory(),
                        "utilization", String.format("%.2f%%", stats.memoryUtilization() * 100)
                )
        );

        return Response.ok(health).build();
    }

    @GET
    @Path("/metrics")
    @Operation(summary = "Get metrics")
    @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully")
    public Response metrics() {
        StorageEngine.StorageStats stats = storageEngine.getStats();
        List<Node> activeNodes = clusterManager.getActiveNodes();

        Map<String, Object> metrics = Map.of(
                "storage", Map.of(
                        "totalKeys", stats.totalKeys(),
                        "memoryUsed", stats.totalMemoryUsed(),
                        "maxMemory", stats.maxMemory(),
                        "memoryUtilization", stats.memoryUtilization()
                ),
                "cluster", Map.of(
                        "totalNodes", clusterManager.getAllNodes().size(),
                        "activeNodes", activeNodes.size(),
                        "currentNode", clusterManager.getCurrentNode().getId()
                )
        );

        return Response.ok(metrics).build();
    }

    @POST
    @Path("/join")
    @Operation(summary = "Join the cluster")
    @ApiResponse(responseCode = "200", description = "Joined cluster successfully")
    public Response joinCluster(Map<String, List<String>> request) {
        List<String> seedNodes = request.get("seedNodes");

        if (seedNodes == null || seedNodes.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "seedNodes is required"))
                    .build();
        }

        clusterManager.joinCluster(seedNodes);

        return Response.ok(Map.of(
                "message", "Joined cluster",
                "nodeId", clusterManager.getCurrentNode().getId()
        )).build();
    }

    @POST
    @Path("/leave")
    @Operation(summary = "Leave the cluster")
    @ApiResponse(responseCode = "200", description = "Left cluster successfully")
    public Response leaveCluster() {
        clusterManager.leaveCluster();

        return Response.ok(Map.of(
                "message", "Left cluster",
                "nodeId", clusterManager.getCurrentNode().getId()
        )).build();
    }

    @POST
    @Path("/gossip")
    @Operation(summary = "Handle incoming gossip message")
    @ApiResponse(responseCode = "200", description = "Gossip processed successfully")
    public Response handleGossip(GossipMessage message) {
        try {
            GossipMessage response = gossipProtocol.handleIncomingGossip(message);
            return Response.ok(response).build();
        } catch (Exception e) {
            log.error("Failed to handle gossip: {}", e.getMessage(), e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/snapshot/create")
    @Operation(summary = "Create a data snapshot")
    @ApiResponse(responseCode = "200", description = "Snapshot created successfully")
    public Response createSnapshot(Map<String, List<String>> request) {
        try {
            List<String> namespaces = request.getOrDefault("namespaces", List.of());
            Path snapshotPath = snapshotManager.createSnapshot(storageEngine, namespaces);

            return Response.ok(Map.of(
                    "message", "Snapshot created",
                    "path", snapshotPath.toString(),
                    "filename", snapshotPath.getFileName().toString()
            )).build();
        } catch (Exception e) {
            log.error("Failed to create snapshot: {}", e.getMessage(), e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @GET
    @Path("/snapshot/list")
    @Operation(summary = "List available snapshots")
    @ApiResponse(responseCode = "200", description = "Snapshots listed successfully")
    public Response listSnapshots() {
        try {
            List<Path> snapshots = snapshotManager.listSnapshots();
            List<Map<String, String>> snapshotInfo = snapshots.stream()
                    .map(path -> Map.of(
                            "filename", path.getFileName().toString(),
                            "path", path.toString()
                    ))
                    .toList();

            return Response.ok(Map.of("snapshots", snapshotInfo)).build();
        } catch (Exception e) {
            log.error("Failed to list snapshots: {}", e.getMessage(), e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }

    @POST
    @Path("/snapshot/load")
    @Operation(summary = "Load a snapshot")
    @ApiResponse(responseCode = "200", description = "Snapshot loaded successfully")
    public Response loadSnapshot(Map<String, String> request) {
        try {
            String filename = request.get("filename");
            if (filename == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "filename is required"))
                        .build();
            }

            Path snapshotPath = snapshotManager.listSnapshots().stream()
                    .filter(path -> path.getFileName().toString().equals(filename))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + filename));

            long keysLoaded = snapshotManager.loadSnapshot(snapshotPath, storageEngine);

            return Response.ok(Map.of(
                    "message", "Snapshot loaded",
                    "keysLoaded", keysLoaded
            )).build();
        } catch (Exception e) {
            log.error("Failed to load snapshot: {}", e.getMessage(), e);
            return Response.serverError().entity(Map.of("error", e.getMessage())).build();
        }
    }
}
