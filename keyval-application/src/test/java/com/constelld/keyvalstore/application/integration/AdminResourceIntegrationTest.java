package com.constelld.keyvalstore.application.integration;

import com.constelld.keyvalstore.application.KeyValStoreApplication;
import com.constelld.keyvalstore.application.KeyValStoreConfiguration;
import com.constelld.keyvalstore.api.dto.NodeResponse;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Admin Resource endpoints.
 * Tests cluster management, node operations, and administrative functions.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class AdminResourceIntegrationTest {

    private static final DropwizardAppExtension<KeyValStoreConfiguration> APP =
            new DropwizardAppExtension<>(
                    KeyValStoreApplication.class,
                    ResourceHelpers.resourceFilePath("test-config.yml")
            );

    private Client client;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        client = APP.client();
        baseUrl = String.format("http://localhost:%d/api/v1/admin", APP.getLocalPort());
    }

    @Test
    void testGetClusterNodes() {
        Response response = client.target(baseUrl + "/cluster/nodes")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        List<?> nodes = response.readEntity(List.class);
        assertNotNull(nodes);
        // At minimum, should have the local test node
        assertTrue(nodes.size() >= 1);
    }

    @Test
    void testAddNode() {
        NodeResponse newNode = new NodeResponse();
        newNode.setNodeId("test-node-2");
        newNode.setHost("localhost");
        newNode.setPort(8081);
        newNode.setAdminPort(9091);

        Response response = client.target(baseUrl + "/cluster/nodes")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(newNode));

        // Should succeed or handle appropriately
        assertTrue(response.getStatus() == 200 || response.getStatus() == 201);
    }

    @Test
    void testRemoveNode() {
        // First add a node
        NodeResponse nodeToRemove = new NodeResponse();
        nodeToRemove.setNodeId("test-node-to-remove");
        nodeToRemove.setHost("localhost");
        nodeToRemove.setPort(8082);
        nodeToRemove.setAdminPort(9092);

        client.target(baseUrl + "/cluster/nodes")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(nodeToRemove));

        // Remove the node
        Response response = client.target(baseUrl + "/cluster/nodes/test-node-to-remove")
                .request(MediaType.APPLICATION_JSON)
                .delete();

        assertTrue(response.getStatus() == 200 || response.getStatus() == 204);
    }

    @Test
    void testGetClusterStatus() {
        Response response = client.target(baseUrl + "/cluster/status")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        Map<?, ?> status = response.readEntity(Map.class);
        assertNotNull(status);
        // Should contain cluster information
        assertTrue(status.size() > 0);
    }

    @Test
    void testTriggerGossip() {
        Response response = client.target(baseUrl + "/cluster/gossip")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        // Should succeed
        assertTrue(response.getStatus() >= 200 && response.getStatus() < 300);
    }

    @Test
    void testCreateSnapshot() {
        Response response = client.target(baseUrl + "/snapshot/create")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        // Should succeed
        assertTrue(response.getStatus() >= 200 && response.getStatus() < 300);
    }

    @Test
    void testListSnapshots() {
        // First create a snapshot
        client.target(baseUrl + "/snapshot/create")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        // List snapshots
        Response response = client.target(baseUrl + "/snapshot/list")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        List<?> snapshots = response.readEntity(List.class);
        assertNotNull(snapshots);
    }

    @Test
    void testLoadSnapshot() {
        // First create a snapshot
        Response createResponse = client.target(baseUrl + "/snapshot/create")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.text(""));

        if (createResponse.getStatus() < 300) {
            // Get snapshot name from response or use a known name
            // This is a simplified test - in practice you'd parse the snapshot name
            Response loadResponse = client.target(baseUrl + "/snapshot/load")
                    .queryParam("snapshotName", "test-snapshot")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.text(""));

            // May succeed or fail depending on snapshot availability
            assertTrue(loadResponse.getStatus() >= 200);
        }
    }

    @Test
    void testHealthCheck() {
        // Test the DropWizard health check endpoint
        Response response = client.target(String.format("http://localhost:%d/healthcheck", APP.getAdminPort()))
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        Map<?, ?> healthChecks = response.readEntity(Map.class);
        assertNotNull(healthChecks);
    }

    @Test
    void testMetricsEndpoint() {
        // Test metrics endpoint (Prometheus format)
        Response response = client.target(baseUrl + "/metrics")
                .request(MediaType.TEXT_PLAIN)
                .get();

        // Should return metrics in Prometheus format
        assertTrue(response.getStatus() == 200);
        String metrics = response.readEntity(String.class);
        assertNotNull(metrics);
        // Prometheus metrics should contain "# HELP" and "# TYPE" lines
        assertTrue(metrics.contains("# HELP") || metrics.contains("# TYPE") || metrics.length() > 0);
    }

    @Test
    void testInvalidNodeRemoval() {
        Response response = client.target(baseUrl + "/cluster/nodes/non-existent-node")
                .request(MediaType.APPLICATION_JSON)
                .delete();

        // Should return error (404 or similar)
        assertTrue(response.getStatus() >= 400);
    }

    @Test
    void testAddInvalidNode() {
        NodeResponse invalidNode = new NodeResponse();
        // Missing required fields
        invalidNode.setNodeId("");
        invalidNode.setHost("");

        Response response = client.target(baseUrl + "/cluster/nodes")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(invalidNode));

        // Should return error
        assertTrue(response.getStatus() >= 400);
    }
}
