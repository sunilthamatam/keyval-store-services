package com.constelld.keyvalstore.application.integration;

import com.constelld.keyvalstore.application.KeyValStoreApplication;
import com.constelld.keyvalstore.application.KeyValStoreConfiguration;
import com.constelld.keyvalstore.api.dto.CreateNamespaceRequest;
import com.constelld.keyvalstore.api.dto.KeyValueResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for KeyValue Resource endpoints.
 * Tests CRUD operations on key-value pairs within namespaces.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
class KeyValueResourceIntegrationTest {

    private static final DropwizardAppExtension<KeyValStoreConfiguration> APP =
            new DropwizardAppExtension<>(
                    KeyValStoreApplication.class,
                    ResourceHelpers.resourceFilePath("test-config.yml")
            );

    private Client client;
    private String baseUrl;
    private static final String TEST_NAMESPACE = "kv-test-namespace";

    @BeforeEach
    void setUp() {
        client = APP.client();
        baseUrl = String.format("http://localhost:%d/api/v1", APP.getLocalPort());

        // Create a test namespace for each test
        CreateNamespaceRequest request = new CreateNamespaceRequest();
        request.setNamespace(TEST_NAMESPACE);
        request.setKeyStrategy("HASH");
        request.setValueStrategy("STRING");

        client.target(baseUrl + "/namespaces")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(request));
    }

    @Test
    void testPutAndGetValue() {
        String key = "test-key";
        String value = "test-value";

        // PUT key-value
        Response putResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.text(value));

        assertEquals(200, putResponse.getStatus());

        // GET key-value
        Response getResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, getResponse.getStatus());
        KeyValueResponse kvResponse = getResponse.readEntity(KeyValueResponse.class);
        assertNotNull(kvResponse);
        assertEquals(key, kvResponse.getKey());
        assertEquals(value, kvResponse.getValue());
        assertEquals(TEST_NAMESPACE, kvResponse.getNamespace());
    }

    @Test
    void testPutMultipleValues() {
        Map<String, String> testData = new HashMap<>();
        testData.put("key1", "value1");
        testData.put("key2", "value2");
        testData.put("key3", "value3");

        // PUT multiple key-values
        for (Map.Entry<String, String> entry : testData.entrySet()) {
            Response response = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + entry.getKey())
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.text(entry.getValue()));
            assertEquals(200, response.getStatus());
        }

        // Verify all values
        for (Map.Entry<String, String> entry : testData.entrySet()) {
            Response getResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + entry.getKey())
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            assertEquals(200, getResponse.getStatus());
            KeyValueResponse kvResponse = getResponse.readEntity(KeyValueResponse.class);
            assertEquals(entry.getValue(), kvResponse.getValue());
        }
    }

    @Test
    void testUpdateValue() {
        String key = "update-test-key";
        String initialValue = "initial-value";
        String updatedValue = "updated-value";

        // PUT initial value
        client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.text(initialValue));

        // Update the value
        Response updateResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.text(updatedValue));

        assertEquals(200, updateResponse.getStatus());

        // Verify updated value
        Response getResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .get();

        KeyValueResponse kvResponse = getResponse.readEntity(KeyValueResponse.class);
        assertEquals(updatedValue, kvResponse.getValue());
    }

    @Test
    void testDeleteValue() {
        String key = "delete-test-key";
        String value = "value-to-delete";

        // PUT value
        client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.text(value));

        // DELETE value
        Response deleteResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .delete();

        assertEquals(204, deleteResponse.getStatus());

        // Verify it's deleted
        Response getResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(404, getResponse.getStatus());
    }

    @Test
    void testGetNonExistentKey() {
        Response response = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/non-existent-key")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(404, response.getStatus());
    }

    @Test
    void testListKeysInNamespace() {
        // PUT multiple values
        for (int i = 0; i < 5; i++) {
            client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/list-key-" + i)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.text("value-" + i));
        }

        // List all keys
        Response response = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv")
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, response.getStatus());
        List<?> keys = response.readEntity(List.class);
        assertNotNull(keys);
        assertTrue(keys.size() >= 5);
    }

    @Test
    void testPutValueInNonExistentNamespace() {
        Response response = client.target(baseUrl + "/namespaces/non-existent-namespace/kv/test-key")
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.text("test-value"));

        // Should return error (404 or 500)
        assertTrue(response.getStatus() >= 400);
    }

    @Test
    void testPutEmptyValue() {
        String key = "empty-value-key";

        Response response = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.text(""));

        assertEquals(200, response.getStatus());

        // Verify empty value is stored
        Response getResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .get();

        KeyValueResponse kvResponse = getResponse.readEntity(KeyValueResponse.class);
        assertEquals("", kvResponse.getValue());
    }

    @Test
    void testPutLargeValue() {
        String key = "large-value-key";
        StringBuilder largeValue = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeValue.append("Lorem ipsum dolor sit amet ");
        }

        Response putResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.text(largeValue.toString()));

        assertEquals(200, putResponse.getStatus());

        // Verify large value is stored correctly
        Response getResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .get();

        KeyValueResponse kvResponse = getResponse.readEntity(KeyValueResponse.class);
        assertEquals(largeValue.toString(), kvResponse.getValue());
    }

    @Test
    void testConcurrentPuts() throws InterruptedException {
        String key = "concurrent-key";
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                        .request(MediaType.APPLICATION_JSON)
                        .put(Entity.text("value-" + index));
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify a value exists (should be one of the concurrent writes)
        Response getResponse = client.target(baseUrl + "/namespaces/" + TEST_NAMESPACE + "/kv/" + key)
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, getResponse.getStatus());
        KeyValueResponse kvResponse = getResponse.readEntity(KeyValueResponse.class);
        assertNotNull(kvResponse.getValue());
        assertTrue(kvResponse.getValue().startsWith("value-"));
    }
}
