# KeyVal Store Services

A distributed key-value store with off-heap storage, built with DropWizard 4 and Java 21.

## Features

- **Off-heap storage** using Java DirectByteBuffer to avoid GC pressure
- **Concurrent access** from multiple users/threads
- **Custom strategies** for keys and values
- **Namespace segmentation** with per-namespace strategies
- **Consistent hashing** for data partitioning across nodes
- **Configurable replication** for data redundancy
- **Configurable consistency levels** (ONE, QUORUM, ALL)
- **Node discovery** via admin port
- **REST API** with OpenAPI specification
- **SQLite** for namespace metadata persistence

## Architecture

### Module Structure

```
keyval-store-services/
├── keyval-core/              # Core domain models, interfaces, and strategies
├── keyval-storage/           # Off-heap storage implementation
├── keyval-cluster/           # Clustering, consistent hashing, replication
├── keyval-persistence/       # SQLite for metadata/namespace config
├── keyval-api/               # REST API with DropWizard 4
└── keyval-application/       # Main application entry point
```

### Key Components

#### 1. Core Module (`keyval-core`)

**Domain Models:**
- `Namespace` - Segments key-value pairs with custom strategies
- `KeyValue` - Represents a key-value pair with metadata
- `Node` - Represents a cluster node
- `ConsistencyLevel` - Defines consistency levels (ONE, QUORUM, ALL)

**Interfaces:**
- `KeyStrategy` - Custom key routing and hashing
- `ValueStrategy` - Custom value generation and transformation
- `StorageEngine` - Storage operations interface
- `ClusterManager` - Cluster membership management
- `PartitionManager` - Data partitioning with consistent hashing
- `ReplicationManager` - Data replication across nodes

**Strategy Implementations:**
- `HashKeyStrategy` - MurmurHash3-based key hashing
- `StringValueStrategy` - Simple string values
- `AtomicIncrementValueStrategy` - Auto-incrementing long values
- `JsonBlobValueStrategy` - JSON blob validation and storage

#### 2. Storage Module (`keyval-storage`)

**Off-Heap Storage:**
- `OffHeapStorageEngine` - Main storage engine using DirectByteBuffer
- `MemoryAllocator` - Manages native memory allocation/deallocation
- `KeyValueSerializer` - Serializes KeyValue objects to byte arrays

**Key Features:**
- Stores data in native memory (off-heap)
- Concurrent access with read/write locks
- Chunked memory allocation (256MB chunks)
- Memory usage tracking and statistics

#### 3. Cluster Module (`keyval-cluster`)

**Consistent Hashing:**
- `ConsistentHashRing` - Virtual node-based consistent hashing
- Configurable virtual nodes per physical node
- Even data distribution across nodes

**Replication:**
- `DefaultReplicationManager` - Handles data replication
- Configurable replication factor
- Configurable consistency levels
- Async replication with futures

**Cluster Management:**
- `DefaultClusterManager` - Node discovery and health checking
- Heartbeat mechanism
- Automatic node failure detection

#### 4. Persistence Module (`keyval-persistence`)

- SQLite database for namespace configurations
- JDBI for database access
- Stores namespace metadata, not actual KV data

#### 5. API Module (`keyval-api`)

**REST Endpoints:**

```
# Namespace Management
POST   /api/v1/namespaces           # Create namespace
GET    /api/v1/namespaces           # List namespaces
GET    /api/v1/namespaces/{ns}      # Get namespace info
DELETE /api/v1/namespaces/{ns}      # Delete namespace

# Key-Value Operations
PUT    /api/v1/namespaces/{ns}/kv/{key}   # Set key-value
GET    /api/v1/namespaces/{ns}/kv/{key}   # Get value
DELETE /api/v1/namespaces/{ns}/kv/{key}   # Delete key
GET    /api/v1/namespaces/{ns}/kv         # List keys

# Admin Operations (admin port)
GET    /admin/nodes                 # List cluster nodes
GET    /admin/health                # Health check
GET    /admin/metrics               # Metrics
POST   /admin/join                  # Join cluster
POST   /admin/leave                 # Leave cluster
```

**OpenAPI Specification:**
- Auto-generated from Swagger annotations
- Available at `/openapi.json`
- Swagger UI for API exploration

#### 6. Application Module (`keyval-application`)

- Main entry point
- DropWizard application
- Configuration management
- Integration of all modules

## Configuration

Example configuration (`config.yml`):

```yaml
server:
  applicationConnectors:
    - type: http
      port: 8080
  adminConnectors:
    - type: http
      port: 9090

cluster:
  nodeName: node1
  nodeId: ${NODE_ID:-node1}
  host: ${HOST:-localhost}
  port: 8080
  adminPort: 9090
  seedNodes:
    - localhost:9091
    - localhost:9092
  replicationFactor: 3
  virtualNodesPerNode: 150

storage:
  maxOffHeapMemory: 2GB

consistency:
  defaultReadLevel: QUORUM
  defaultWriteLevel: QUORUM

database:
  driverClass: org.sqlite.JDBC
  url: jdbc:sqlite:keyvalstore.db
```

## Building

```bash
# Build all modules
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build application JAR
cd keyval-application
mvn package
```

## Running

```bash
# Single node
java -jar keyval-application/target/keyval-application-1.0.0-SNAPSHOT.jar server config.yml

# With custom JVM args for off-heap memory
java -XX:MaxDirectMemorySize=4g \
     -Xmx2g \
     -jar keyval-application/target/keyval-application-1.0.0-SNAPSHOT.jar \
     server config.yml

# Cluster node 1
java -jar keyval-application/target/keyval-application-1.0.0-SNAPSHOT.jar \
     server config-node1.yml

# Cluster node 2
java -jar keyval-application/target/keyval-application-1.0.0-SNAPSHOT.jar \
     server config-node2.yml
```

## Usage Examples

### Create a Namespace

```bash
curl -X POST http://localhost:8080/api/v1/namespaces \
  -H "Content-Type: application/json" \
  -d '{
    "name": "users",
    "keyStrategyType": "HASH",
    "valueStrategyType": "JSON_BLOB",
    "configuration": {}
  }'
```

### Store a Key-Value

```bash
curl -X PUT http://localhost:8080/api/v1/namespaces/users/kv/user123 \
  -H "Content-Type: application/json" \
  -d '{"name": "John Doe", "email": "john@example.com"}'
```

### Get a Value

```bash
curl http://localhost:8080/api/v1/namespaces/users/kv/user123
```

### Delete a Key

```bash
curl -X DELETE http://localhost:8080/api/v1/namespaces/users/kv/user123
```

### Atomic Increment Example

```bash
# Create namespace with atomic increment strategy
curl -X POST http://localhost:8080/api/v1/namespaces \
  -H "Content-Type: application/json" \
  -d '{
    "name": "counters",
    "keyStrategyType": "HASH",
    "valueStrategyType": "ATOMIC_INCREMENT"
  }'

# Increment counter
curl -X PUT http://localhost:8080/api/v1/namespaces/counters/kv/page_views
```

## Development Phases

### Phase 1: Core + Storage ✅
- [x] Maven multi-module structure
- [x] Core domain models and interfaces
- [x] Off-heap storage with DirectByteBuffer
- [x] Basic PUT/GET/DELETE operations
- [x] Strategy implementations

### Phase 2: Cluster ✅
- [x] Consistent hashing implementation
- [x] Partition manager
- [x] Replication manager
- [x] Cluster manager

### Phase 3: API (TODO)
- [ ] DropWizard REST API
- [ ] Namespace management endpoints
- [ ] Key-value operation endpoints
- [ ] OpenAPI specification
- [ ] Swagger UI integration

### Phase 4: Persistence (TODO)
- [ ] SQLite database setup
- [ ] Namespace repository
- [ ] Node repository
- [ ] Schema migrations

### Phase 5: Application (TODO)
- [ ] Main application class
- [ ] Configuration handling
- [ ] Module integration
- [ ] Startup/shutdown hooks

### Phase 6: Advanced Features (TODO)
- [ ] HTTP client for remote replication
- [ ] Admin port listener
- [ ] Health checks
- [ ] Metrics collection
- [ ] Read repair mechanism
- [ ] Anti-entropy (gossip protocol)

## Technology Stack

- **Java 21** - Modern Java features
- **DropWizard 4** - REST framework
- **DirectByteBuffer** - Off-heap memory
- **Guava** - Consistent hashing utilities
- **SQLite** - Metadata persistence
- **JDBI** - Database access
- **Jackson** - JSON serialization
- **Swagger** - API documentation
- **SLF4J/Logback** - Logging
- **JUnit 5** - Testing

## Design Decisions

### Off-Heap Storage
Using DirectByteBuffer instead of libraries like Chronicle Map provides:
- Full control over memory layout
- Custom serialization strategies
- Learning opportunity for low-level memory management
- Reduced external dependencies

### Consistent Hashing
- Virtual nodes ensure even distribution
- Minimal data movement when nodes join/leave
- Configurable virtual nodes per physical node (default: 150)

### Configurable Consistency
- ONE: Fastest, least consistent
- QUORUM: Balanced (N/2 + 1 replicas)
- ALL: Strongest consistency, slowest

### No Authentication
- Designed for internal/trusted networks
- Can be added later with DropWizard auth modules

## Contributing

This is a learning/demo project. Contributions welcome!

## License

MIT License
