package com.keyvalstore.persistence.db;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Database manager for SQLite database initialization and management.
 */
public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final Jdbi jdbi;

    public DatabaseManager(String jdbcUrl) {
        this.jdbi = Jdbi.create(jdbcUrl);
        this.jdbi.installPlugin(new SqlObjectPlugin());

        log.info("DatabaseManager initialized with URL: {}", jdbcUrl);
        initializeSchema();
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    /**
     * Initialize database schema from migration files.
     */
    private void initializeSchema() {
        try {
            log.info("Initializing database schema...");

            // Read and execute schema SQL
            String schemaSql = readResource("/db/migration/V1__initial_schema.sql");

            jdbi.useHandle(handle -> {
                handle.createScript(schemaSql).execute();
            });

            log.info("Database schema initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize database schema", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Read a resource file as string.
     */
    private String readResource(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read resource: " + resourcePath, e);
        }
    }

    /**
     * Close database connections.
     */
    public void close() {
        log.info("Closing database connections");
        // JDBI doesn't require explicit closing of the factory
    }
}
