package com.constelld.keyvalstore.storage.engine;

import com.constelld.keyvalstore.core.exception.StorageException;
import com.constelld.keyvalstore.core.model.KeyValue;
import com.constelld.keyvalstore.core.storage.StorageEngine;
import com.constelld.keyvalstore.storage.allocator.MemoryAllocator;
import com.constelld.keyvalstore.storage.serializer.KeyValueSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Off-heap storage engine using DirectByteBuffer for storing key-value pairs.
 * This implementation stores data in native memory to avoid GC pressure.
 */
public class OffHeapStorageEngine implements StorageEngine {
    private static final Logger log = LoggerFactory.getLogger(OffHeapStorageEngine.class);

    // Index: namespace:key -> MemoryBlock
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, MemoryBlock>> namespaceIndex;

    private final MemoryAllocator memoryAllocator;
    private final KeyValueSerializer serializer;
    private final ReadWriteLock globalLock;
    private final AtomicLong totalKeys;
    private volatile boolean isShutdown;

    public OffHeapStorageEngine(long maxMemoryBytes) {
        this.namespaceIndex = new ConcurrentHashMap<>();
        this.memoryAllocator = new MemoryAllocator(maxMemoryBytes);
        this.serializer = new KeyValueSerializer();
        this.globalLock = new ReentrantReadWriteLock();
        this.totalKeys = new AtomicLong(0);
        this.isShutdown = false;

        log.info("OffHeapStorageEngine initialized with max memory: {} bytes", maxMemoryBytes);
    }

    @Override
    public KeyValue put(String namespace, String key, byte[] value) {
        checkNotShutdown();
        globalLock.readLock().lock();
        try {
            ConcurrentHashMap<String, MemoryBlock> nsMap = namespaceIndex
                    .computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());

            KeyValue keyValue = new KeyValue(namespace, key, value);
            byte[] serialized = serializer.serialize(keyValue);

            // Check if key already exists
            MemoryBlock existingBlock = nsMap.get(key);
            if (existingBlock != null) {
                // Deallocate old block
                memoryAllocator.deallocate(existingBlock.address, existingBlock.size);
            } else {
                totalKeys.incrementAndGet();
            }

            // Allocate new block
            long address = memoryAllocator.allocate(serialized.length);
            MemoryBlock block = new MemoryBlock(address, serialized.length);

            // Write to off-heap memory
            ByteBuffer buffer = memoryAllocator.getBuffer(address, serialized.length);
            buffer.put(serialized);

            nsMap.put(key, block);

            log.debug("Stored key '{}' in namespace '{}' at address {} (size: {} bytes)",
                    key, namespace, address, serialized.length);

            return keyValue;
        } catch (Exception e) {
            throw new StorageException("Failed to put key-value", e);
        } finally {
            globalLock.readLock().unlock();
        }
    }

    @Override
    public Optional<KeyValue> get(String namespace, String key) {
        checkNotShutdown();
        globalLock.readLock().lock();
        try {
            ConcurrentHashMap<String, MemoryBlock> nsMap = namespaceIndex.get(namespace);
            if (nsMap == null) {
                return Optional.empty();
            }

            MemoryBlock block = nsMap.get(key);
            if (block == null) {
                return Optional.empty();
            }

            // Read from off-heap memory
            ByteBuffer buffer = memoryAllocator.getBuffer(block.address, block.size);
            byte[] data = new byte[block.size];
            buffer.get(data);

            KeyValue keyValue = serializer.deserialize(data);
            return Optional.of(keyValue);
        } catch (Exception e) {
            throw new StorageException("Failed to get key-value", e);
        } finally {
            globalLock.readLock().unlock();
        }
    }

    @Override
    public boolean delete(String namespace, String key) {
        checkNotShutdown();
        globalLock.readLock().lock();
        try {
            ConcurrentHashMap<String, MemoryBlock> nsMap = namespaceIndex.get(namespace);
            if (nsMap == null) {
                return false;
            }

            MemoryBlock block = nsMap.remove(key);
            if (block == null) {
                return false;
            }

            // Deallocate memory
            memoryAllocator.deallocate(block.address, block.size);
            totalKeys.decrementAndGet();

            log.debug("Deleted key '{}' from namespace '{}' at address {}",
                    key, namespace, block.address);

            return true;
        } catch (Exception e) {
            throw new StorageException("Failed to delete key-value", e);
        } finally {
            globalLock.readLock().unlock();
        }
    }

    @Override
    public boolean exists(String namespace, String key) {
        checkNotShutdown();
        ConcurrentHashMap<String, MemoryBlock> nsMap = namespaceIndex.get(namespace);
        return nsMap != null && nsMap.containsKey(key);
    }

    @Override
    public List<String> listKeys(String namespace, int offset, int limit) {
        checkNotShutdown();
        ConcurrentHashMap<String, MemoryBlock> nsMap = namespaceIndex.get(namespace);
        if (nsMap == null) {
            return Collections.emptyList();
        }

        return nsMap.keySet().stream()
                .sorted()
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public long count(String namespace) {
        checkNotShutdown();
        ConcurrentHashMap<String, MemoryBlock> nsMap = namespaceIndex.get(namespace);
        return nsMap != null ? nsMap.size() : 0;
    }

    @Override
    public void clear(String namespace) {
        checkNotShutdown();
        globalLock.writeLock().lock();
        try {
            ConcurrentHashMap<String, MemoryBlock> nsMap = namespaceIndex.remove(namespace);
            if (nsMap != null) {
                // Deallocate all blocks
                for (MemoryBlock block : nsMap.values()) {
                    memoryAllocator.deallocate(block.address, block.size);
                    totalKeys.decrementAndGet();
                }
                log.info("Cleared namespace '{}' ({} keys)", namespace, nsMap.size());
            }
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    @Override
    public StorageStats getStats() {
        long memoryUsed = memoryAllocator.getUsedMemory();
        long maxMemory = memoryAllocator.getMaxMemory();
        double utilization = (double) memoryUsed / maxMemory;

        return new StorageStats(
                totalKeys.get(),
                memoryUsed,
                maxMemory,
                utilization
        );
    }

    @Override
    public void shutdown() {
        globalLock.writeLock().lock();
        try {
            if (isShutdown) {
                return;
            }

            log.info("Shutting down OffHeapStorageEngine...");

            // Deallocate all memory
            for (Map.Entry<String, ConcurrentHashMap<String, MemoryBlock>> nsEntry : namespaceIndex.entrySet()) {
                for (MemoryBlock block : nsEntry.getValue().values()) {
                    memoryAllocator.deallocate(block.address, block.size);
                }
            }

            namespaceIndex.clear();
            memoryAllocator.shutdown();
            isShutdown = true;

            log.info("OffHeapStorageEngine shut down successfully");
        } finally {
            globalLock.writeLock().unlock();
        }
    }

    private void checkNotShutdown() {
        if (isShutdown) {
            throw new StorageException("Storage engine has been shut down");
        }
    }

    /**
     * Represents a block of allocated memory.
     */
    private static class MemoryBlock {
        final long address;
        final int size;

        MemoryBlock(long address, int size) {
            this.address = address;
            this.size = size;
        }
    }
}
