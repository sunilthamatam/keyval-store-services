package com.constelld.keyvalstore.storage.snapshot;

import com.constelld.keyvalstore.core.model.KeyValue;
import com.constelld.keyvalstore.core.storage.StorageEngine;
import com.constelld.keyvalstore.storage.serializer.KeyValueSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Manages snapshots of the storage engine to disk for persistence.
 * Snapshots are compressed and can be used for backup and recovery.
 */
public class SnapshotManager {
    private static final Logger log = LoggerFactory.getLogger(SnapshotManager.class);

    private final Path snapshotDirectory;
    private final KeyValueSerializer serializer;

    public SnapshotManager(String snapshotDirectoryPath) {
        this.snapshotDirectory = Paths.get(snapshotDirectoryPath);
        this.serializer = new KeyValueSerializer();

        // Create directory if it doesn't exist
        try {
            Files.createDirectories(snapshotDirectory);
            log.info("Snapshot directory: {}", snapshotDirectory.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create snapshot directory", e);
            throw new RuntimeException("Failed to create snapshot directory", e);
        }
    }

    /**
     * Create a snapshot of all data in the storage engine.
     * The snapshot is compressed and saved to disk.
     *
     * @param storageEngine the storage engine to snapshot
     * @param namespaces list of namespaces to include
     * @return the path to the created snapshot file
     */
    public Path createSnapshot(StorageEngine storageEngine, List<String> namespaces) throws IOException {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                .replaceAll(":", "-"); // File-safe timestamp
        String filename = String.format("snapshot-%s.gz", timestamp);
        Path snapshotFile = snapshotDirectory.resolve(filename);

        log.info("Creating snapshot: {}", snapshotFile.getFileName());

        long totalKeys = 0;

        try (FileOutputStream fos = new FileOutputStream(snapshotFile.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzos)) {

            // Write header
            dos.writeInt(1); // Version
            dos.writeInt(namespaces.size()); // Number of namespaces

            // Write each namespace
            for (String namespace : namespaces) {
                dos.writeUTF(namespace);

                List<String> keys = storageEngine.listKeys(namespace, 0, Integer.MAX_VALUE);
                dos.writeInt(keys.size()); // Number of keys in this namespace

                // Write each key-value pair
                for (String key : keys) {
                    storageEngine.get(namespace, key).ifPresent(keyValue -> {
                        try {
                            byte[] serialized = serializer.serialize(keyValue);
                            dos.writeInt(serialized.length);
                            dos.write(serialized);
                        } catch (IOException e) {
                            log.error("Failed to serialize key-value: {}", key, e);
                        }
                    });
                }

                totalKeys += keys.size();
            }
        }

        log.info("Snapshot created: {} ({} keys)", snapshotFile.getFileName(), totalKeys);
        return snapshotFile;
    }

    /**
     * Load a snapshot from disk and restore it to the storage engine.
     *
     * @param snapshotFile the snapshot file to load
     * @param storageEngine the storage engine to restore to
     * @return the number of keys restored
     */
    public long loadSnapshot(Path snapshotFile, StorageEngine storageEngine) throws IOException {
        if (!Files.exists(snapshotFile)) {
            throw new FileNotFoundException("Snapshot file not found: " + snapshotFile);
        }

        log.info("Loading snapshot: {}", snapshotFile.getFileName());

        long totalKeys = 0;

        try (FileInputStream fis = new FileInputStream(snapshotFile.toFile());
             GZIPInputStream gzis = new GZIPInputStream(fis);
             DataInputStream dis = new DataInputStream(gzis)) {

            // Read header
            int version = dis.readInt();
            if (version != 1) {
                throw new IOException("Unsupported snapshot version: " + version);
            }

            int namespaceCount = dis.readInt();

            // Read each namespace
            for (int i = 0; i < namespaceCount; i++) {
                String namespace = dis.readUTF();
                int keyCount = dis.readInt();

                // Read each key-value pair
                for (int j = 0; j < keyCount; j++) {
                    int dataLength = dis.readInt();
                    byte[] data = new byte[dataLength];
                    dis.readFully(data);

                    KeyValue keyValue = serializer.deserialize(data);
                    storageEngine.put(keyValue.getNamespace(), keyValue.getKey(), keyValue.getValue());
                    totalKeys++;
                }
            }
        }

        log.info("Snapshot loaded: {} ({} keys)", snapshotFile.getFileName(), totalKeys);
        return totalKeys;
    }

    /**
     * List all available snapshots.
     *
     * @return list of snapshot file paths
     */
    public List<Path> listSnapshots() throws IOException {
        List<Path> snapshots = new ArrayList<>();

        try (var stream = Files.list(snapshotDirectory)) {
            stream.filter(path -> path.toString().endsWith(".gz"))
                    .sorted((a, b) -> b.compareTo(a)) // Newest first
                    .forEach(snapshots::add);
        }

        return snapshots;
    }

    /**
     * Delete old snapshots, keeping only the specified number of most recent snapshots.
     *
     * @param keepCount number of snapshots to keep
     * @return number of snapshots deleted
     */
    public int pruneOldSnapshots(int keepCount) throws IOException {
        List<Path> snapshots = listSnapshots();

        if (snapshots.size() <= keepCount) {
            return 0;
        }

        int deleted = 0;
        for (int i = keepCount; i < snapshots.size(); i++) {
            Path snapshot = snapshots.get(i);
            if (Files.deleteIfExists(snapshot)) {
                log.info("Deleted old snapshot: {}", snapshot.getFileName());
                deleted++;
            }
        }

        return deleted;
    }

    /**
     * Get the most recent snapshot file.
     *
     * @return the most recent snapshot, or null if no snapshots exist
     */
    public Path getLatestSnapshot() throws IOException {
        List<Path> snapshots = listSnapshots();
        return snapshots.isEmpty() ? null : snapshots.get(0);
    }
}
