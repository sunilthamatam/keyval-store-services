# Cluster Architecture

## Multi-Node Cluster Topology

This diagram shows how multiple nodes form a distributed cluster.

```mermaid
graph TB
    subgraph "Client Requests"
        Client1[Client 1]
        Client2[Client 2]
        Client3[Client 3]
    end

    subgraph "Load Balancer"
        LB[Load Balancer / Service Discovery]
    end

    subgraph "Cluster Nodes"
        subgraph "Node 1 - localhost:8080"
            N1App[DropWizard App :8080]
            N1Admin[Admin :9090]
            N1Storage[Off-Heap Storage<br/>~2GB]
            N1Cluster[Cluster Manager]
            N1DB[(SQLite)]
        end

        subgraph "Node 2 - localhost:8081"
            N2App[DropWizard App :8081]
            N2Admin[Admin :9091]
            N2Storage[Off-Heap Storage<br/>~2GB]
            N2Cluster[Cluster Manager]
            N2DB[(SQLite)]
        end

        subgraph "Node 3 - localhost:8082"
            N3App[DropWizard App :8082]
            N3Admin[Admin :9092]
            N3Storage[Off-Heap Storage<br/>~2GB]
            N3Cluster[Cluster Manager]
            N3DB[(SQLite)]
        end
    end

    Client1 -->|HTTP| LB
    Client2 -->|HTTP| LB
    Client3 -->|HTTP| LB

    LB -->|Route| N1App
    LB -->|Route| N2App
    LB -->|Route| N3App

    N1Cluster <-.->|Gossip<br/>5s interval| N2Cluster
    N2Cluster <-.->|Gossip<br/>5s interval| N3Cluster
    N3Cluster <-.->|Gossip<br/>5s interval| N1Cluster

    N1Cluster -->|Replicate| N2App
    N1Cluster -->|Replicate| N3App
    N2Cluster -->|Replicate| N1App
    N2Cluster -->|Replicate| N3App
    N3Cluster -->|Replicate| N1App
    N3Cluster -->|Replicate| N2App

    N1App --> N1Storage
    N2App --> N2Storage
    N3App --> N3Storage

    N1App --> N1DB
    N2App --> N2DB
    N3App --> N3DB

    style N1App fill:#e1f5ff
    style N2App fill:#e1f5ff
    style N3App fill:#e1f5ff
    style N1Storage fill:#ffe1f5
    style N2Storage fill:#ffe1f5
    style N3Storage fill:#ffe1f5
```

## Consistent Hashing and Data Distribution

```mermaid
graph LR
    subgraph "Hash Ring (0 to 2^32-1)"
        Ring((Consistent Hash Ring))
    end

    subgraph "Virtual Nodes"
        N1V1[Node1-VN001]
        N1V2[Node1-VN002]
        N1V150[Node1-VN150]
        N2V1[Node2-VN001]
        N2V2[Node2-VN002]
        N2V150[Node2-VN150]
        N3V1[Node3-VN001]
        N3V2[Node3-VN002]
        N3V150[Node3-VN150]
    end

    subgraph "Data Keys"
        K1[Key: user:123<br/>Hash: 1234567890]
        K2[Key: order:456<br/>Hash: 2345678901]
        K3[Key: product:789<br/>Hash: 3456789012]
    end

    K1 -->|Routed to| N1V1
    K2 -->|Routed to| N2V2
    K3 -->|Routed to| N3V1

    N1V1 --> Ring
    N1V2 --> Ring
    N1V150 --> Ring
    N2V1 --> Ring
    N2V2 --> Ring
    N2V150 --> Ring
    N3V1 --> Ring
    N3V2 --> Ring
    N3V150 --> Ring

    style Ring fill:#fff4e1
    style K1 fill:#e1ffe1
    style K2 fill:#e1ffe1
    style K3 fill:#e1ffe1
```

## Replication Strategy

```mermaid
sequenceDiagram
    participant Client
    participant Node1 as Node 1 (Coordinator)
    participant Node2 as Node 2 (Replica)
    participant Node3 as Node 3 (Replica)

    Note over Client,Node3: Write with Replication Factor = 3, Consistency = QUORUM

    Client->>Node1: PUT /namespaces/users/kv/user123 (value)

    Note over Node1: 1. Write to local storage
    Node1->>Node1: Store in Off-Heap Memory

    Note over Node1: 2. Determine replicas via Consistent Hash
    Note over Node1: 3. Replicate to N-1 nodes (2 nodes)

    par Parallel Replication
        Node1->>Node2: HTTP PUT /replicate (async)
        Node1->>Node3: HTTP PUT /replicate (async)
    end

    Node2->>Node2: Store in Off-Heap Memory
    Node3->>Node3: Store in Off-Heap Memory

    Node2-->>Node1: ACK (200 OK)
    Node3-->>Node1: ACK (200 OK)

    Note over Node1: Wait for QUORUM (2/3) responses

    Node1-->>Client: 200 OK (Write successful)
```

## Gossip Protocol Flow

```mermaid
sequenceDiagram
    participant Node1
    participant Node2
    participant Node3

    Note over Node1,Node3: Every 5 seconds, each node initiates gossip

    Note over Node1: Select 3 random peers (fanout)

    Node1->>Node2: GossipMessage {<br/>  nodeId: "node1",<br/>  version: 123,<br/>  status: "ALIVE",<br/>  dataChecksums: {...}<br/>}

    Node1->>Node3: GossipMessage {<br/>  nodeId: "node1",<br/>  version: 123,<br/>  status: "ALIVE",<br/>  dataChecksums: {...}<br/>}

    Note over Node2: Compare checksums
    alt Checksums match
        Node2-->>Node1: ACK (no action needed)
    else Checksums differ
        Node2->>Node2: Trigger Read Repair
        Node2-->>Node1: Request missing/updated data
        Node1-->>Node2: Send data
    end

    Note over Node3: Update node status
    Node3->>Node3: Mark Node1 as ALIVE
    Node3-->>Node1: ACK
```

## Read Repair Mechanism

```mermaid
sequenceDiagram
    participant Client
    participant Node1 as Node 1 (Coordinator)
    participant Node2 as Node 2
    participant Node3 as Node 3

    Note over Client,Node3: Read with Read Repair enabled

    Client->>Node1: GET /namespaces/users/kv/user123

    Note over Node1: Query multiple replicas

    par Parallel Read
        Node1->>Node1: Read local (v=5)
        Node1->>Node2: Read remote (v=3)
        Node1->>Node3: Read remote (v=5)
    end

    Node1-->>Node1: Version 5
    Node2-->>Node1: Version 3 (stale!)
    Node3-->>Node1: Version 5

    Note over Node1: Detect version mismatch
    Note over Node1: Select highest version (v=5)

    Node1->>Node2: Repair: PUT user123 (v=5)
    Node2->>Node2: Update to v=5
    Node2-->>Node1: ACK

    Node1-->>Client: Return data (v=5)
```

## Node Failure and Recovery

```mermaid
stateDiagram-v2
    [*] --> Alive: Node starts

    Alive --> Suspected: Gossip timeout<br/>(no heartbeat)

    Suspected --> Alive: Heartbeat received
    Suspected --> Dead: Timeout expired<br/>(30s)

    Dead --> Alive: Node rejoins cluster
    Dead --> Removed: Admin removes node

    Removed --> [*]

    note right of Alive
        Node participates in:
        - Read/Write operations
        - Replication
        - Gossip protocol
    end note

    note right of Suspected
        Node temporarily excluded:
        - No new writes routed
        - Existing data retained
        - Gossip continues
    end note

    note right of Dead
        Node marked dead:
        - Excluded from routing
        - Data re-replicated
        - Hash ring updated
    end note
```

## Configuration Parameters

### Cluster Settings

| Parameter | Default | Description |
|-----------|---------|-------------|
| `replicationFactor` | 3 | Number of replicas per key |
| `virtualNodesPerNode` | 150 | Virtual nodes for consistent hashing |
| `seedNodes` | [] | Initial nodes to join |
| `gossipInterval` | 5000ms | Gossip protocol interval |
| `gossipFanout` | 3 | Number of peers per gossip round |

### Consistency Levels

| Level | Reads | Writes | Description |
|-------|-------|--------|-------------|
| `ONE` | 1 node | 1 node | Fastest, lowest consistency |
| `QUORUM` | N/2+1 nodes | N/2+1 nodes | Balance of speed and consistency |
| `ALL` | N nodes | N nodes | Slowest, highest consistency |

### Port Assignments

| Node | Application Port | Admin Port |
|------|-----------------|------------|
| Node 1 | 8080 | 9090 |
| Node 2 | 8081 | 9091 |
| Node 3 | 8082 | 9092 |
