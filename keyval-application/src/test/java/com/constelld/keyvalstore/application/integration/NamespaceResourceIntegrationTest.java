package com.constelld.keyvalstore.application.integration;

import com.constelld.keyvalstore.application.KeyValStoreApplication;
import com.constelld.keyvalstore.application.KeyValStoreConfiguration;
import com.constelld.keyvalstore.api.dto.CreateNamespaceRequest;
import com.constelld.keyvalstore.api.dto.NamespaceResponse;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Namespace Resource endpoints.
 * Tests the full application stack including REST API, service layer, and persistence.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class NamespaceResourceIntegrationTest {

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
        baseUrl = String.format("http://localhost:%d/api/v1", APP.getLocalPort());
    }

    @Test
    void testCreateNamespace() {
        // Create namespace request
        CreateNamespaceRequest request = new CreateNamespaceRequest();
        request.setNamespace("test-namespace");
        request.setKeyStrategy("HASH");
        request.setValueStrategy("STRING");

        // Create namespace
        Response response = client.target(baseUrl + "/namespaces")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(request));

        assertEquals(200, response.getStatus());
        NamespaceResponse namespace = response.readEntity(NamespaceResponse.class);
        assertNotNull(namespace);
        assertEquals("test-namespace", namespace.getName());
        assertEquals("HASH", namespace.getKeyStrategy());
        assertEquals("STRING", namespace.getValueStrategy());
    }

    @Test
    void testGetNamespace() {
        // First create a namespace
        CreateNamespaceRequest request = new CreateNamespaceRequest();
        request.setNamespace("get-test-namespace");
        request.setKeyStrategy("HASH");
        request.setValueStrategy("JSON_BLOB");

        client.target(baseUrl + "/namespaces")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(request));

        // Get the namespace
        Response response = client.target(baseUrl + "/namespaces/get-test-namespace")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        NamespaceResponse namespace = response.readEntity(NamespaceResponse.class);
        assertNotNull(namespace);
        assertEquals("get-test-namespace", namespace.getName());
        assertEquals("HASH", namespace.getKeyStrategy());
        assertEquals("JSON_BLOB", namespace.getValueStrategy());
    }

    @Test
    void testListNamespaces() {
        // Create multiple namespaces
        for (int i = 0; i < 3; i++) {
            CreateNamespaceRequest request = new CreateNamespaceRequest();
            request.setNamespace("list-namespace-" + i);
            request.setKeyStrategy("HASH");
            request.setValueStrategy("STRING");

            client.target(baseUrl + "/namespaces")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(request));
        }

        // List all namespaces
        Response response = client.target(baseUrl + "/namespaces")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        List<?> namespaces = response.readEntity(List.class);
        assertNotNull(namespaces);
        assertTrue(namespaces.size() >= 3);
    }

    @Test
    void testDeleteNamespace() {
        // First create a namespace
        CreateNamespaceRequest request = new CreateNamespaceRequest();
        request.setNamespace("delete-test-namespace");
        request.setKeyStrategy("HASH");
        request.setValueStrategy("STRING");

        client.target(baseUrl + "/namespaces")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(request));

        // Delete the namespace
        Response deleteResponse = client.target(baseUrl + "/namespaces/delete-test-namespace")
                .request(MediaType.APPLICATION_JSON)
                .delete();

        assertEquals(204, deleteResponse.getStatus());

        // Verify it's deleted
        Response getResponse = client.target(baseUrl + "/namespaces/delete-test-namespace")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(404, getResponse.getStatus());
    }

    @Test
    void testGetNonExistentNamespace() {
        Response response = client.target(baseUrl + "/namespaces/non-existent")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(404, response.getStatus());
    }

    @Test
    void testCreateNamespaceWithInvalidStrategy() {
        CreateNamespaceRequest request = new CreateNamespaceRequest();
        request.setNamespace("invalid-namespace");
        request.setKeyStrategy("INVALID_KEY_STRATEGY");
        request.setValueStrategy("STRING");

        Response response = client.target(baseUrl + "/namespaces")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(request));

        // Should return error (400 or 500 depending on validation)
        assertTrue(response.getStatus() >= 400);
    }
}
