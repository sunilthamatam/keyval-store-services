package com.constelld.keyvalstore.application;

import com.constelld.keyvalstore.api.health.StorageHealthCheck;
import com.constelld.keyvalstore.api.resource.AdminResource;
import com.constelld.keyvalstore.api.resource.KeyValueResource;
import com.constelld.keyvalstore.api.resource.NamespaceResource;
import com.constelld.keyvalstore.api.service.KeyValueService;
import com.constelld.keyvalstore.api.service.NamespaceService;
import com.constelld.keyvalstore.cluster.manager.DefaultClusterManager;
import com.constelld.keyvalstore.cluster.partition.ConsistentHashRing;
import com.constelld.keyvalstore.core.cluster.ClusterManager;
import com.constelld.keyvalstore.core.cluster.PartitionManager;
import com.constelld.keyvalstore.core.model.Node;
import com.constelld.keyvalstore.core.storage.StorageEngine;
import com.constelld.keyvalstore.core.strategy.AtomicIncrementValueStrategy;
import com.constelld.keyvalstore.core.strategy.JsonBlobValueStrategy;
import com.constelld.keyvalstore.core.strategy.StringValueStrategy;
import com.constelld.keyvalstore.core.strategy.ValueStrategy;
import com.constelld.keyvalstore.persistence.db.DatabaseManager;
import com.constelld.keyvalstore.persistence.repository.NamespaceRepository;
import com.constelld.keyvalstore.persistence.repository.NodeRepository;
import com.constelld.keyvalstore.storage.engine.OffHeapStorageEngine;
import com.smoketurner.dropwizard.swagger.SwaggerBundle;
import com.smoketurner.dropwizard.swagger.SwaggerBundleConfiguration;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Main DropWizard application for KeyVal Store.
 */
public class KeyValStoreApplication extends Application<KeyValStoreConfiguration> {
    private static final Logger log = LoggerFactory.getLogger(KeyValStoreApplication.class);

    private StorageEngine storageEngine;
    private ClusterManager clusterManager;
    private DatabaseManager databaseManager;

    public static void main(String[] args) throws Exception {
        new KeyValStoreApplication().run(args);
    }

    @Override
    public String getName() {
        return "keyval-store";
    }

    @Override
    public void initialize(Bootstrap<KeyValStoreConfiguration> bootstrap) {
        // Configure Swagger/OpenAPI
        bootstrap.addBundle(new SwaggerBundle<KeyValStoreConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(KeyValStoreConfiguration configuration) {
                return configuration.getSwagger();
            }
        });
    }

    @Override
    public void run(KeyValStoreConfiguration configuration, Environment environment) {
        log.info("Starting KeyVal Store Application...");

        // Initialize database
        databaseManager = new DatabaseManager(configuration.getDatabase().getUrl());
        NamespaceRepository namespaceRepository = new NamespaceRepository(databaseManager.getJdbi());
        NodeRepository nodeRepository = new NodeRepository(databaseManager.getJdbi());

        // Initialize storage engine
        long maxMemory = configuration.getStorage().getMaxOffHeapMemoryBytes();
        storageEngine = new OffHeapStorageEngine(maxMemory);
        log.info("Initialized storage engine with max memory: {} bytes ({} GB)",
                maxMemory, maxMemory / (1024.0 * 1024.0 * 1024.0));

        // Initialize cluster
        Node currentNode = new Node(
                configuration.getCluster().getNodeId(),
                configuration.getCluster().getHost(),
                configuration.getCluster().getPort(),
                configuration.getCluster().getAdminPort()
        );

        clusterManager = new DefaultClusterManager(currentNode);

        PartitionManager partitionManager = new ConsistentHashRing(
                configuration.getCluster().getVirtualNodesPerNode()
        );

        // Add current node to partition manager
        partitionManager.addNode(currentNode);

        // Start cluster manager
        clusterManager.start();

        // Join cluster if seed nodes are configured
        if (!configuration.getCluster().getSeedNodes().isEmpty()) {
            clusterManager.joinCluster(configuration.getCluster().getSeedNodes());
        }

        // Initialize value strategies
        Map<String, ValueStrategy> valueStrategies = new HashMap<>();
        valueStrategies.put("STRING", new StringValueStrategy());
        valueStrategies.put("ATOMIC_INCREMENT", new AtomicIncrementValueStrategy());
        valueStrategies.put("JSON_BLOB", new JsonBlobValueStrategy());

        // Initialize services
        NamespaceService namespaceService = new NamespaceService(namespaceRepository, storageEngine);
        KeyValueService keyValueService = new KeyValueService(storageEngine, namespaceRepository, valueStrategies);

        // Register resources
        environment.jersey().register(new NamespaceResource(namespaceService));
        environment.jersey().register(new KeyValueResource(keyValueService));
        environment.jersey().register(new AdminResource(clusterManager, storageEngine));

        // Register health checks
        environment.healthChecks().register("storage", new StorageHealthCheck(storageEngine));

        // Register shutdown hook
        environment.lifecycle().manage(new io.dropwizard.lifecycle.Managed() {
            @Override
            public void start() {
                log.info("KeyVal Store Application started successfully");
                log.info("Node ID: {}", currentNode.getId());
                log.info("Application port: {}", configuration.getCluster().getPort());
                log.info("Admin port: {}", configuration.getCluster().getAdminPort());
            }

            @Override
            public void stop() {
                log.info("Shutting down KeyVal Store Application...");

                if (clusterManager != null && clusterManager.isRunning()) {
                    clusterManager.leaveCluster();
                }

                if (storageEngine != null) {
                    storageEngine.shutdown();
                }

                if (databaseManager != null) {
                    databaseManager.close();
                }

                log.info("KeyVal Store Application shut down successfully");
            }
        });

        log.info("KeyVal Store Application configuration completed");
        log.info("OpenAPI/Swagger UI available at /swagger");
    }
}
