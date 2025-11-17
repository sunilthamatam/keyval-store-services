package com.keyvalstore.api.resource;

import com.keyvalstore.api.dto.NodeResponse;
import com.keyvalstore.core.cluster.ClusterManager;
import com.keyvalstore.core.model.Node;
import com.keyvalstore.core.storage.StorageEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin", description = "Administrative operations")
public class AdminResource {

    private final ClusterManager clusterManager;
    private final StorageEngine storageEngine;

    public AdminResource(ClusterManager clusterManager, StorageEngine storageEngine) {
        this.clusterManager = clusterManager;
        this.storageEngine = storageEngine;
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
}
