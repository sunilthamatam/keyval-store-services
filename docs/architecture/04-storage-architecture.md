# Storage Architecture

## Off-Heap Memory Management

This diagram shows how the storage engine manages native memory outside the JVM heap.

```mermaid
graph TB
    subgraph "JVM Heap"
        App[Application Code]
        StorageEngine[OffHeapStorageEngine]
        Allocator[MemoryAllocator]
        IndexMap[ConcurrentHashMap<br/>Key Index]
    end

    subgraph "Native Memory (Off-Heap)"
        subgraph "Chunk 1 - 256MB"
            C1D1[Data Block 1]
            C1D2[Data Block 2]
            C1Dn[Data Block N]
        end

        subgraph "Chunk 2 - 256MB"
            C2D1[Data Block 1]
            C2D2[Data Block 2]
            C2Dn[Data Block N]
        end

        subgraph "Chunk N - 256MB"
            CND1[Data Block 1]
            CND2[Data Block 2]
            CNDn[Data Block N]
        end
    end

    App -->|PUT/GET| StorageEngine
    StorageEngine -->|Allocate| Allocator
    StorageEngine -->|Index Lookup| IndexMap

    IndexMap -.->|Points to| C1D1
    IndexMap -.->|Points to| C2D1
    IndexMap -.->|Points to| CND1

    Allocator -->|Creates| C1D1
    Allocator -->|Creates| C2D1
    Allocator -->|Creates| CND1

    style App fill:#e1f5ff
    style StorageEngine fill:#ffe1f5
    style IndexMap fill:#fff4e1
    style C1D1 fill:#e1ffe1
    style C2D1 fill:#e1ffe1
    style CND1 fill:#e1ffe1
```

## Data Block Structure

Each data block in off-heap memory has the following binary layout:

```mermaid
graph LR
    subgraph "Data Block Structure"
        Header[Header<br/>32 bytes]
        Namespace[Namespace<br/>Variable]
        Key[Key<br/>Variable]
        Value[Value<br/>Variable]
    end

    Header --> Namespace
    Namespace --> Key
    Key --> Value

    style Header fill:#e1f5ff
    style Namespace fill:#ffe1f5
    style Key fill:#fff4e1
    style Value fill:#e1ffe1
```

### Header Format (32 bytes)

| Offset | Size | Field | Description |
|--------|------|-------|-------------|
| 0 | 4 bytes | Namespace Length | Length of namespace string |
| 4 | 4 bytes | Key Length | Length of key string |
| 8 | 4 bytes | Value Length | Length of value data |
| 12 | 8 bytes | Version | Version number for conflict resolution |
| 20 | 8 bytes | Timestamp | Unix timestamp (milliseconds) |
| 28 | 4 bytes | Checksum | CRC32 checksum (optional) |

## Storage Operations

### Write (PUT) Operation

```mermaid
sequenceDiagram
    participant Client
    participant StorageEngine
    participant Serializer
    participant Allocator
    participant IndexMap
    participant DirectBuffer

    Client->>StorageEngine: put(namespace, key, value)

    StorageEngine->>Serializer: serialize(KeyValue)
    Serializer-->>StorageEngine: byte[] serialized

    StorageEngine->>Allocator: allocate(size)
    Allocator->>DirectBuffer: ByteBuffer.allocateDirect(size)
    DirectBuffer-->>Allocator: DirectByteBuffer
    Allocator-->>StorageEngine: MemoryPointer

    StorageEngine->>DirectBuffer: put(serialized bytes)

    StorageEngine->>IndexMap: put(key, pointer)

    StorageEngine-->>Client: Success
```

### Read (GET) Operation

```mermaid
sequenceDiagram
    participant Client
    participant StorageEngine
    participant IndexMap
    participant DirectBuffer
    participant Serializer

    Client->>StorageEngine: get(namespace, key)

    StorageEngine->>IndexMap: get(key)
    IndexMap-->>StorageEngine: MemoryPointer

    alt Key Found
        StorageEngine->>DirectBuffer: read(pointer)
        DirectBuffer-->>StorageEngine: byte[] serialized

        StorageEngine->>Serializer: deserialize(bytes)
        Serializer-->>StorageEngine: KeyValue object

        StorageEngine-->>Client: KeyValue
    else Key Not Found
        StorageEngine-->>Client: null / KeyNotFoundException
    end
```

### Delete Operation

```mermaid
sequenceDiagram
    participant Client
    participant StorageEngine
    participant IndexMap
    participant Allocator

    Client->>StorageEngine: delete(namespace, key)

    StorageEngine->>IndexMap: remove(key)
    IndexMap-->>StorageEngine: MemoryPointer

    alt Key Found
        StorageEngine->>Allocator: free(pointer)
        Note over Allocator: Mark memory as available<br/>(add to free list)

        StorageEngine-->>Client: Success (204)
    else Key Not Found
        StorageEngine-->>Client: Not Found (404)
    end
```

## Memory Allocation Strategy

```mermaid
graph TB
    subgraph "Allocation Request"
        Request[Allocation Request<br/>Size: 512 bytes]
    end

    subgraph "MemoryAllocator"
        FreeList[Free Block List]
        ChunkManager[Chunk Manager]
    end

    subgraph "Decision Process"
        CheckFree{Free block<br/>available?}
        CheckSize{Fits in<br/>current chunk?}
        CreateChunk[Create new<br/>256MB chunk]
    end

    Request --> CheckFree

    CheckFree -->|Yes| FreeList
    FreeList --> Reuse[Reuse block]

    CheckFree -->|No| CheckSize

    CheckSize -->|Yes| ChunkManager
    ChunkManager --> Allocate[Allocate from<br/>current chunk]

    CheckSize -->|No| CreateChunk
    CreateChunk --> ChunkManager

    style Request fill:#e1f5ff
    style FreeList fill:#ffe1f5
    style Reuse fill:#e1ffe1
    style CreateChunk fill:#fff4e1
```

## Snapshot Architecture

```mermaid
graph TB
    subgraph "Snapshot Creation"
        Trigger[Snapshot Trigger<br/>Admin API / Scheduled]
        SnapshotMgr[Snapshot Manager]
        StorageEngine[Storage Engine]
        Serializer[Serializer]
        Compress[GZIP Compressor]
        Disk[Disk Storage<br/>snapshots/*.gz]
    end

    subgraph "Snapshot Format"
        Header[Snapshot Header<br/>Version, Timestamp, Count]
        Data[Serialized KeyValues]
        Footer[Checksum]
    end

    Trigger --> SnapshotMgr
    SnapshotMgr --> StorageEngine
    StorageEngine -->|Iterate all KVs| Serializer
    Serializer --> Header
    Serializer --> Data
    Serializer --> Footer
    Data --> Compress
    Compress --> Disk

    style Trigger fill:#e1f5ff
    style SnapshotMgr fill:#ffe1f5
    style Compress fill:#fff4e1
    style Disk fill:#e1ffe1
```

### Snapshot File Structure

```
snapshots/
├── snapshot-2025-01-19T10-30-00.gz
├── snapshot-2025-01-19T11-00-00.gz
└── snapshot-2025-01-19T11-30-00.gz
```

Each snapshot contains:
1. **Header**: Version (4 bytes), Timestamp (8 bytes), Entry Count (4 bytes)
2. **Data Blocks**: Serialized KeyValue objects (one per entry)
3. **Footer**: SHA-256 checksum (32 bytes)

## Snapshot Restore Flow

```mermaid
sequenceDiagram
    participant Admin
    participant SnapshotMgr
    participant FileSystem
    participant Decompress
    participant Deserializer
    participant StorageEngine

    Admin->>SnapshotMgr: loadSnapshot(filename)

    SnapshotMgr->>FileSystem: read(snapshot.gz)
    FileSystem-->>SnapshotMgr: byte[] compressed

    SnapshotMgr->>Decompress: decompress(data)
    Decompress-->>SnapshotMgr: byte[] decompressed

    SnapshotMgr->>Deserializer: parseHeader()
    SnapshotMgr->>StorageEngine: clear() [Optional]

    loop For each entry
        SnapshotMgr->>Deserializer: deserialize(entry)
        Deserializer-->>SnapshotMgr: KeyValue
        SnapshotMgr->>StorageEngine: put(kv)
    end

    SnapshotMgr->>Deserializer: verifyChecksum()

    SnapshotMgr-->>Admin: Success
```

## Memory Management Best Practices

### Advantages of Off-Heap Storage

1. **No GC Pressure**: Data stored outside JVM heap doesn't trigger garbage collection
2. **Large Capacity**: Not limited by JVM heap size (`-Xmx`)
3. **Predictable Performance**: No GC pause times
4. **Process Survival**: Data persists across JVM restarts (with snapshots)

### Trade-offs

1. **Manual Management**: Must explicitly free memory
2. **Serialization Overhead**: Convert objects to/from byte arrays
3. **No Type Safety**: Raw byte buffers require careful handling
4. **Limited by System RAM**: Total memory constrained by physical RAM

### Configuration

```yaml
storage:
  maxOffHeapMemory: 2GB  # Maximum native memory to use
  chunkSize: 256MB       # Size of each DirectByteBuffer chunk
  enableSnapshots: true   # Enable snapshot functionality
  snapshotDirectory: ./snapshots
```

## Concurrent Access

```mermaid
graph TB
    subgraph "Concurrency Control"
        RWLock[ReadWriteLock<br/>Per Namespace]
        Readers[Multiple Readers]
        Writer[Single Writer]
    end

    subgraph "Operations"
        Read1[Read Thread 1]
        Read2[Read Thread 2]
        Read3[Read Thread 3]
        Write1[Write Thread]
    end

    Read1 -->|Shared Lock| RWLock
    Read2 -->|Shared Lock| RWLock
    Read3 -->|Shared Lock| RWLock
    Write1 -->|Exclusive Lock| RWLock

    RWLock -->|Allow| Readers
    RWLock -->|Allow| Writer

    style RWLock fill:#ffe1f5
    style Readers fill:#e1ffe1
    style Writer fill:#e1f5ff
```

### Lock Granularity

- **Namespace-level locks**: Each namespace has its own ReadWriteLock
- **Read operations**: Multiple concurrent readers allowed
- **Write operations**: Exclusive access, blocks all readers and other writers
- **Lock-free index**: ConcurrentHashMap for key-to-pointer mapping

## Performance Characteristics

| Operation | Time Complexity | Concurrency |
|-----------|----------------|-------------|
| PUT | O(1) average | Write lock required |
| GET | O(1) average | Read lock (shared) |
| DELETE | O(1) average | Write lock required |
| LIST | O(n) | Read lock (shared) |
| Snapshot Create | O(n) | Read lock (shared) |
| Snapshot Load | O(n) | Write lock required |

Where n = number of entries in the namespace.
