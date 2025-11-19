# API and Data Flow

## REST API Endpoints

```mermaid
graph TB
    subgraph "Client Applications"
        WebApp[Web Application]
        Mobile[Mobile App]
        Service[Microservice]
    end

    subgraph "Load Balancer"
        LB[Load Balancer<br/>:80 or :443]
    end

    subgraph "API Endpoints - Port 8080"
        subgraph "Namespace API"
            NS_POST[POST /api/v1/namespaces<br/>Create Namespace]
            NS_GET[GET /api/v1/namespaces<br/>List Namespaces]
            NS_GET_ONE[GET /api/v1/namespaces/:name<br/>Get Namespace]
            NS_DELETE[DELETE /api/v1/namespaces/:name<br/>Delete Namespace]
        end

        subgraph "Key-Value API"
            KV_PUT[PUT /api/v1/namespaces/:ns/kv/:key<br/>Store Value]
            KV_GET[GET /api/v1/namespaces/:ns/kv/:key<br/>Retrieve Value]
            KV_DELETE[DELETE /api/v1/namespaces/:ns/kv/:key<br/>Delete Value]
            KV_LIST[GET /api/v1/namespaces/:ns/kv<br/>List Keys]
        end

        subgraph "Admin API"
            ADMIN_NODES[GET/POST/DELETE /api/v1/admin/cluster/nodes<br/>Node Management]
            ADMIN_STATUS[GET /api/v1/admin/cluster/status<br/>Cluster Status]
            ADMIN_GOSSIP[POST /api/v1/admin/cluster/gossip<br/>Trigger Gossip]
            ADMIN_SNAPSHOT[POST /api/v1/admin/snapshot/create<br/>Create Snapshot]
            ADMIN_SNAP_LIST[GET /api/v1/admin/snapshot/list<br/>List Snapshots]
            ADMIN_SNAP_LOAD[POST /api/v1/admin/snapshot/load<br/>Load Snapshot]
            ADMIN_METRICS[GET /api/v1/admin/metrics<br/>Prometheus Metrics]
        end
    end

    subgraph "Health & Monitoring - Port 9090"
        HEALTH[GET /healthcheck<br/>Application Health]
        ADMIN_TASKS[GET /tasks<br/>Admin Tasks]
        ADMIN_THREADS[GET /threads<br/>Thread Dump]
    end

    WebApp --> LB
    Mobile --> LB
    Service --> LB

    LB --> NS_POST
    LB --> KV_PUT
    LB --> ADMIN_NODES

    style NS_POST fill:#e1f5ff
    style KV_PUT fill:#ffe1f5
    style ADMIN_NODES fill:#fff4e1
    style HEALTH fill:#e1ffe1
```

## Complete Request Flow - Write Operation

```mermaid
sequenceDiagram
    participant Client
    participant LB as Load Balancer
    participant API as DropWizard API
    participant KVService as KeyValue Service
    participant NSService as Namespace Service
    participant Strategy as Value Strategy
    participant Storage as Storage Engine
    participant Replication as Replication Manager
    participant HashRing as Consistent Hash
    participant RemoteNode as Remote Node

    Client->>LB: PUT /api/v1/namespaces/users/kv/user123<br/>Body: {"name": "John", "email": "john@example.com"}

    LB->>API: Route to Node 1

    API->>KVService: put("users", "user123", jsonData)

    KVService->>NSService: getNamespace("users")
    NSService-->>KVService: Namespace(keyStrategy=HASH, valueStrategy=JSON_BLOB)

    KVService->>Strategy: validate(jsonData)
    Strategy-->>KVService: Valid

    KVService->>Storage: put(KeyValue)
    Storage-->>KVService: Success (version=1)

    Note over KVService: Replication Factor = 3

    KVService->>HashRing: getNodesForKey("user123")
    HashRing-->>KVService: [Node1, Node2, Node3]

    KVService->>Replication: replicate(KeyValue, [Node2, Node3], QUORUM)

    par Replicate to Node2 and Node3
        Replication->>RemoteNode: HTTP PUT /api/v1/namespaces/users/kv/user123
        RemoteNode-->>Replication: 200 OK (Node2)
        Replication->>RemoteNode: HTTP PUT /api/v1/namespaces/users/kv/user123
        RemoteNode-->>Replication: 200 OK (Node3)
    end

    Note over Replication: QUORUM achieved (2/3 nodes)

    Replication-->>KVService: Replication successful
    KVService-->>API: KeyValue response
    API-->>LB: 200 OK + JSON response
    LB-->>Client: Response
```

## Complete Request Flow - Read Operation

```mermaid
sequenceDiagram
    participant Client
    participant LB as Load Balancer
    participant API as DropWizard API
    participant KVService as KeyValue Service
    participant Storage as Storage Engine
    participant ReadRepair as Read Repair Manager
    participant RemoteNode1 as Remote Node 2
    participant RemoteNode2 as Remote Node 3

    Client->>LB: GET /api/v1/namespaces/users/kv/user123

    LB->>API: Route to Node 1

    API->>KVService: get("users", "user123")

    Note over KVService: Consistency Level = QUORUM

    par Query Node 1, Node 2, Node 3
        KVService->>Storage: get("users", "user123")
        Storage-->>KVService: KeyValue(version=5)

        KVService->>RemoteNode1: GET /api/v1/namespaces/users/kv/user123
        RemoteNode1-->>KVService: KeyValue(version=3) [STALE!]

        KVService->>RemoteNode2: GET /api/v1/namespaces/users/kv/user123
        RemoteNode2-->>KVService: KeyValue(version=5)
    end

    Note over KVService: Received 3 responses

    KVService->>ReadRepair: detectInconsistency([v5, v3, v5])
    ReadRepair-->>KVService: Version 3 is stale on Node 2

    KVService->>ReadRepair: repair(Node2, KeyValue(v=5))
    ReadRepair->>RemoteNode1: PUT (repair) KeyValue(v=5)
    RemoteNode1-->>ReadRepair: ACK
    ReadRepair-->>KVService: Repair complete

    KVService-->>API: KeyValue(version=5)
    API-->>LB: 200 OK + JSON response
    LB-->>Client: Response
```

## Namespace Creation Flow

```mermaid
sequenceDiagram
    participant Client
    participant API as DropWizard API
    participant NSService as Namespace Service
    participant Validator as Strategy Validator
    participant NSRepo as Namespace Repository
    participant DB as SQLite Database

    Client->>API: POST /api/v1/namespaces<br/>{<br/>  "namespace": "products",<br/>  "keyStrategy": "HASH",<br/>  "valueStrategy": "JSON_BLOB"<br/>}

    API->>NSService: createNamespace(request)

    NSService->>Validator: validateKeyStrategy("HASH")
    Validator-->>NSService: Valid

    NSService->>Validator: validateValueStrategy("JSON_BLOB")
    Validator-->>NSService: Valid

    NSService->>NSRepo: save(Namespace)
    NSRepo->>DB: INSERT INTO namespaces (...)
    DB-->>NSRepo: Success
    NSRepo-->>NSService: Namespace entity

    NSService-->>API: NamespaceResponse
    API-->>Client: 200 OK + NamespaceResponse
```

## Admin - Cluster Node Addition

```mermaid
sequenceDiagram
    participant Admin as Admin Client
    participant API as Admin API
    participant ClusterMgr as Cluster Manager
    participant NodeRepo as Node Repository
    participant HashRing as Consistent Hash Ring
    participant Gossip as Gossip Protocol
    participant NewNode as New Node

    Admin->>API: POST /api/v1/admin/cluster/nodes<br/>{<br/>  "nodeId": "node4",<br/>  "host": "10.0.0.4",<br/>  "port": 8080,<br/>  "adminPort": 9090<br/>}

    API->>ClusterMgr: addNode(NodeRequest)

    ClusterMgr->>NodeRepo: save(Node)
    NodeRepo-->>ClusterMgr: Success

    ClusterMgr->>HashRing: addNode(Node)
    Note over HashRing: Add 150 virtual nodes<br/>Recalculate token ranges

    ClusterMgr->>Gossip: notifyNodeJoin(Node)
    Note over Gossip: Next gossip round will<br/>propagate node information

    Gossip->>NewNode: Initial sync
    NewNode-->>Gossip: ACK

    ClusterMgr-->>API: NodeResponse
    API-->>Admin: 200 OK
```

## Error Handling Flow

```mermaid
graph TB
    Request[Client Request]

    Request --> Validation{Valid Request?}

    Validation -->|Invalid| BadRequest[400 Bad Request<br/>ErrorResponse]

    Validation -->|Valid| NSCheck{Namespace Exists?}

    NSCheck -->|No| NotFound[404 Not Found<br/>NamespaceNotFoundException]

    NSCheck -->|Yes| Operation{Perform Operation}

    Operation -->|Storage Error| ServerError[500 Internal Server Error<br/>StorageException]

    Operation -->|Cluster Error| ServiceUnavailable[503 Service Unavailable<br/>ClusterException]

    Operation -->|Success| Success[200 OK / 201 Created<br/>Response DTO]

    style BadRequest fill:#ffcccc
    style NotFound fill:#ffeecc
    style ServerError fill:#ffcccc
    style ServiceUnavailable fill:#ffcccc
    style Success fill:#ccffcc
```

## API Error Responses

All error responses follow this format:

```json
{
  "code": 404,
  "message": "Namespace not found: users",
  "details": "The requested namespace 'users' does not exist. Create it first using POST /api/v1/namespaces",
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### HTTP Status Codes

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | Successful GET, PUT operations |
| 201 | Created | Successful POST (namespace creation) |
| 204 | No Content | Successful DELETE operations |
| 400 | Bad Request | Invalid input, malformed JSON, invalid strategy |
| 404 | Not Found | Namespace or key doesn't exist |
| 409 | Conflict | Namespace already exists |
| 500 | Internal Server Error | Storage engine failure, unexpected errors |
| 503 | Service Unavailable | Cluster unavailable, replication failure |

## Prometheus Metrics

Exposed at `/api/v1/admin/metrics` in Prometheus text format:

```
# HELP keyval_requests_total Total number of requests
# TYPE keyval_requests_total counter
keyval_requests_total{endpoint="put",namespace="users"} 12345

# HELP keyval_request_duration_seconds Request duration in seconds
# TYPE keyval_request_duration_seconds histogram
keyval_request_duration_seconds_bucket{endpoint="get",le="0.01"} 1000
keyval_request_duration_seconds_bucket{endpoint="get",le="0.05"} 2500

# HELP keyval_storage_used_bytes Storage used in bytes
# TYPE keyval_storage_used_bytes gauge
keyval_storage_used_bytes{namespace="users"} 1048576

# HELP keyval_replication_lag_seconds Replication lag in seconds
# TYPE keyval_replication_lag_seconds gauge
keyval_replication_lag_seconds{source_node="node1",target_node="node2"} 0.05
```

## Health Check Response

```json
{
  "storage": {
    "healthy": true,
    "message": "Off-heap storage operational",
    "usedMemory": "512MB",
    "maxMemory": "2GB"
  },
  "cluster": {
    "healthy": true,
    "message": "Cluster operational",
    "activeNodes": 3,
    "totalNodes": 3
  },
  "database": {
    "healthy": true,
    "message": "Database connected"
  },
  "deadlocks": {
    "healthy": true
  }
}
```
