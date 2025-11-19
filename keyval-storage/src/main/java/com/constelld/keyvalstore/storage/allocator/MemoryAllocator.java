package com.constelld.keyvalstore.storage.allocator;

import com.constelld.keyvalstore.core.exception.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Memory allocator that manages off-heap memory using DirectByteBuffer.
 * Uses a simple bump-pointer allocation strategy with free-list for deallocated blocks.
 */
public class MemoryAllocator {
    private static final Logger log = LoggerFactory.getLogger(MemoryAllocator.class);

    private final long maxMemoryBytes;
    private final AtomicLong usedMemory;
    private final ConcurrentHashMap<Long, ByteBuffer> bufferCache;
    private final AtomicLong nextAddress;
    private volatile boolean isShutdown;

    // Chunk size for ByteBuffer allocation (256 MB chunks)
    private static final int CHUNK_SIZE = 256 * 1024 * 1024;
    private final ConcurrentHashMap<Integer, ByteBuffer> chunks;

    public MemoryAllocator(long maxMemoryBytes) {
        this.maxMemoryBytes = maxMemoryBytes;
        this.usedMemory = new AtomicLong(0);
        this.bufferCache = new ConcurrentHashMap<>();
        this.nextAddress = new AtomicLong(0);
        this.chunks = new ConcurrentHashMap<>();
        this.isShutdown = false;

        log.info("MemoryAllocator initialized with max memory: {} bytes ({} MB)",
                maxMemoryBytes, maxMemoryBytes / (1024 * 1024));
    }

    /**
     * Allocate a block of memory.
     *
     * @param size size in bytes
     * @return virtual address of the allocated block
     */
    public long allocate(int size) {
        if (isShutdown) {
            throw new StorageException("Memory allocator has been shut down");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }

        // Check if we have enough memory
        long currentUsed = usedMemory.get();
        if (currentUsed + size > maxMemoryBytes) {
            throw new StorageException("Out of memory: requested " + size +
                    " bytes, used " + currentUsed + " / " + maxMemoryBytes);
        }

        // Allocate address
        long address = nextAddress.getAndAdd(size);
        usedMemory.addAndGet(size);

        // Ensure chunk is allocated for this address range
        ensureChunkAllocated(address, size);

        log.trace("Allocated {} bytes at address {}", size, address);
        return address;
    }

    /**
     * Deallocate a block of memory.
     *
     * @param address the address to deallocate
     * @param size the size of the block
     */
    public void deallocate(long address, int size) {
        if (isShutdown) {
            return;
        }

        usedMemory.addAndGet(-size);
        bufferCache.remove(address);

        log.trace("Deallocated {} bytes at address {}", size, address);
    }

    /**
     * Get a ByteBuffer view of the memory at the given address.
     *
     * @param address the address
     * @param size the size
     * @return ByteBuffer positioned at the address
     */
    public ByteBuffer getBuffer(long address, int size) {
        if (isShutdown) {
            throw new StorageException("Memory allocator has been shut down");
        }

        // Calculate which chunk this address belongs to
        int chunkIndex = (int) (address / CHUNK_SIZE);
        int offsetInChunk = (int) (address % CHUNK_SIZE);

        ByteBuffer chunk = chunks.get(chunkIndex);
        if (chunk == null) {
            throw new StorageException("Chunk not allocated for address: " + address);
        }

        // Create a slice of the chunk
        ByteBuffer slice = chunk.slice(offsetInChunk, size);
        slice.clear();
        return slice;
    }

    /**
     * Ensure that a chunk is allocated for the given address range.
     */
    private void ensureChunkAllocated(long address, int size) {
        int startChunk = (int) (address / CHUNK_SIZE);
        int endChunk = (int) ((address + size - 1) / CHUNK_SIZE);

        for (int chunkIndex = startChunk; chunkIndex <= endChunk; chunkIndex++) {
            chunks.computeIfAbsent(chunkIndex, idx -> {
                int chunkSize = Math.min(CHUNK_SIZE, (int) (maxMemoryBytes - ((long) idx * CHUNK_SIZE)));
                ByteBuffer buffer = ByteBuffer.allocateDirect(chunkSize);
                log.debug("Allocated chunk {} of size {} bytes", idx, chunkSize);
                return buffer;
            });
        }
    }

    public long getUsedMemory() {
        return usedMemory.get();
    }

    public long getMaxMemory() {
        return maxMemoryBytes;
    }

    public long getAvailableMemory() {
        return maxMemoryBytes - usedMemory.get();
    }

    public void shutdown() {
        isShutdown = true;
        chunks.clear();
        bufferCache.clear();
        log.info("MemoryAllocator shut down. Peak memory usage: {} / {} bytes",
                usedMemory.get(), maxMemoryBytes);
    }
}
