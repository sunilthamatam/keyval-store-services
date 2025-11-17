package com.keyvalstore.persistence.repository;

import com.keyvalstore.core.model.Node;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for node persistence.
 */
public class NodeRepository {
    private static final Logger log = LoggerFactory.getLogger(NodeRepository.class);

    private final Jdbi jdbi;

    public NodeRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /**
     * Save or update a node.
     */
    public void save(Node node) {
        jdbi.useHandle(handle -> {
            int updated = handle.createUpdate(
                "UPDATE nodes SET host = :host, port = :port, admin_port = :adminPort, " +
                "status = :status, last_seen = :lastSeen WHERE id = :id"
            )
            .bind("id", node.getId())
            .bind("host", node.getHost())
            .bind("port", node.getPort())
            .bind("adminPort", node.getAdminPort())
            .bind("status", node.getStatus().name())
            .bind("lastSeen", node.getLastSeen().toEpochMilli())
            .execute();

            if (updated == 0) {
                // Insert if not exists
                handle.createUpdate(
                    "INSERT INTO nodes (id, host, port, admin_port, status, last_seen) " +
                    "VALUES (:id, :host, :port, :adminPort, :status, :lastSeen)"
                )
                .bind("id", node.getId())
                .bind("host", node.getHost())
                .bind("port", node.getPort())
                .bind("adminPort", node.getAdminPort())
                .bind("status", node.getStatus().name())
                .bind("lastSeen", node.getLastSeen().toEpochMilli())
                .execute();

                log.debug("Inserted node: {}", node.getId());
            } else {
                log.debug("Updated node: {}", node.getId());
            }
        });
    }

    /**
     * Find node by ID.
     */
    public Optional<Node> findById(String id) {
        return jdbi.withHandle(handle ->
            handle.createQuery(
                "SELECT id, host, port, admin_port, status, last_seen FROM nodes WHERE id = :id"
            )
            .bind("id", id)
            .map((rs, ctx) -> new Node(
                rs.getString("id"),
                rs.getString("host"),
                rs.getInt("port"),
                rs.getInt("admin_port"),
                Node.NodeStatus.valueOf(rs.getString("status")),
                Instant.ofEpochMilli(rs.getLong("last_seen"))
            ))
            .findFirst()
        );
    }

    /**
     * Find all nodes.
     */
    public List<Node> findAll() {
        return jdbi.withHandle(handle ->
            handle.createQuery(
                "SELECT id, host, port, admin_port, status, last_seen FROM nodes ORDER BY last_seen DESC"
            )
            .map((rs, ctx) -> new Node(
                rs.getString("id"),
                rs.getString("host"),
                rs.getInt("port"),
                rs.getInt("admin_port"),
                Node.NodeStatus.valueOf(rs.getString("status")),
                Instant.ofEpochMilli(rs.getLong("last_seen"))
            ))
            .list()
        );
    }

    /**
     * Find active nodes.
     */
    public List<Node> findActiveNodes() {
        return jdbi.withHandle(handle ->
            handle.createQuery(
                "SELECT id, host, port, admin_port, status, last_seen FROM nodes " +
                "WHERE status = 'ACTIVE' ORDER BY last_seen DESC"
            )
            .map((rs, ctx) -> new Node(
                rs.getString("id"),
                rs.getString("host"),
                rs.getInt("port"),
                rs.getInt("admin_port"),
                Node.NodeStatus.valueOf(rs.getString("status")),
                Instant.ofEpochMilli(rs.getLong("last_seen"))
            ))
            .list()
        );
    }

    /**
     * Delete node by ID.
     */
    public boolean delete(String id) {
        int deleted = jdbi.withHandle(handle ->
            handle.createUpdate("DELETE FROM nodes WHERE id = :id")
                .bind("id", id)
                .execute()
        );

        if (deleted > 0) {
            log.info("Deleted node: {}", id);
        }
        return deleted > 0;
    }

    /**
     * Update node status.
     */
    public void updateStatus(String id, Node.NodeStatus status) {
        jdbi.useHandle(handle ->
            handle.createUpdate(
                "UPDATE nodes SET status = :status, last_seen = :lastSeen WHERE id = :id"
            )
            .bind("id", id)
            .bind("status", status.name())
            .bind("lastSeen", Instant.now().toEpochMilli())
            .execute()
        );
    }

    /**
     * Update last seen timestamp.
     */
    public void updateLastSeen(String id) {
        jdbi.useHandle(handle ->
            handle.createUpdate("UPDATE nodes SET last_seen = :lastSeen WHERE id = :id")
                .bind("id", id)
                .bind("lastSeen", Instant.now().toEpochMilli())
                .execute()
        );
    }

    /**
     * Delete nodes not seen since the given timestamp.
     */
    public int deleteStaleNodes(Instant notSeenSince) {
        return jdbi.withHandle(handle ->
            handle.createUpdate("DELETE FROM nodes WHERE last_seen < :threshold")
                .bind("threshold", notSeenSince.toEpochMilli())
                .execute()
        );
    }
}
