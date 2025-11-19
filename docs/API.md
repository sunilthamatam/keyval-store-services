# KeyVal Store API Documentation

This document provides information about the KeyVal Store REST API and how to use the OpenAPI specification.

## OpenAPI Specification

The complete OpenAPI 3.0 specification is available at:
- **YAML Format**: [docs/openapi.yaml](openapi.yaml)

## Viewing the API Documentation

### Option 1: Swagger UI (Recommended)

View the interactive API documentation using Swagger UI:

```bash
# Using Docker
docker run -p 8081:8080 \
  -e SWAGGER_JSON=/openapi.yaml \
  -v $(pwd)/docs/openapi.yaml:/openapi.yaml \
  swaggerapi/swagger-ui

# Then open: http://localhost:8081
```

### Option 2: Swagger Editor

1. Visit [Swagger Editor](https://editor.swagger.io/)
2. File → Import File → Select `docs/openapi.yaml`
3. View and edit the specification interactively

### Option 3: Redoc

```bash
# Using npx
npx @redocly/cli preview-docs docs/openapi.yaml

# Or using Docker
docker run -p 8080:80 \
  -e SPEC_URL=openapi.yaml \
  -v $(pwd)/docs/openapi.yaml:/usr/share/nginx/html/openapi.yaml \
  redocly/redoc
```

### Option 4: VSCode Extension

1. Install the "OpenAPI (Swagger) Editor" extension
2. Open `docs/openapi.yaml` in VSCode
3. Use the preview pane for interactive documentation

## API Overview

### Base URL

```
http://localhost:8080
```

### API Endpoints

#### Namespace Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/namespaces` | List all namespaces |
| `POST` | `/api/v1/namespaces` | Create a new namespace |
| `GET` | `/api/v1/namespaces/{namespace}` | Get namespace details |
| `DELETE` | `/api/v1/namespaces/{namespace}` | Delete a namespace |

#### Key-Value Operations

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/namespaces/{namespace}/kv` | List all keys |
| `GET` | `/api/v1/namespaces/{namespace}/kv/{key}` | Get value by key |
| `PUT` | `/api/v1/namespaces/{namespace}/kv/{key}` | Store/update value |
| `DELETE` | `/api/v1/namespaces/{namespace}/kv/{key}` | Delete key-value |

#### Cluster Administration

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/admin/cluster/nodes` | List cluster nodes |
| `POST` | `/api/v1/admin/cluster/nodes` | Add node to cluster |
| `DELETE` | `/api/v1/admin/cluster/nodes/{nodeId}` | Remove node from cluster |
| `GET` | `/api/v1/admin/cluster/status` | Get cluster status |
| `POST` | `/api/v1/admin/cluster/gossip` | Trigger gossip protocol |

#### Snapshot Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/admin/snapshot/create` | Create a snapshot |
| `GET` | `/api/v1/admin/snapshot/list` | List available snapshots |
| `POST` | `/api/v1/admin/snapshot/load` | Load a snapshot |

#### Monitoring

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/v1/admin/metrics` | Prometheus metrics |
| `GET` | `/healthcheck` | Health check (admin port 9090) |

## Quick Start Examples

### 1. Create a Namespace

```bash
curl -X POST http://localhost:8080/api/v1/namespaces \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "users",
    "keyStrategy": "HASH",
    "valueStrategy": "JSON_BLOB"
  }'
```

**Response:**
```json
{
  "name": "users",
  "keyStrategy": "HASH",
  "valueStrategy": "JSON_BLOB",
  "createdAt": "2025-01-19T10:30:00.000Z"
}
```

### 2. Store a Value

```bash
curl -X PUT http://localhost:8080/api/v1/namespaces/users/kv/user:123 \
  -H "Content-Type: text/plain" \
  -d '{"name": "John Doe", "email": "john@example.com", "age": 30}'
```

**Response:**
```json
{
  "namespace": "users",
  "key": "user:123",
  "value": "{\"name\": \"John Doe\", \"email\": \"john@example.com\", \"age\": 30}",
  "version": 1,
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### 3. Retrieve a Value

```bash
curl http://localhost:8080/api/v1/namespaces/users/kv/user:123
```

**Response:**
```json
{
  "namespace": "users",
  "key": "user:123",
  "value": "{\"name\": \"John Doe\", \"email\": \"john@example.com\", \"age\": 30}",
  "version": 1,
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### 4. List All Keys

```bash
curl http://localhost:8080/api/v1/namespaces/users/kv
```

**Response:**
```json
[
  "user:123",
  "user:456",
  "user:789"
]
```

### 5. Get Cluster Status

```bash
curl http://localhost:8080/api/v1/admin/cluster/status
```

**Response:**
```json
{
  "totalNodes": 3,
  "aliveNodes": 3,
  "suspectedNodes": 0,
  "deadNodes": 0,
  "replicationFactor": 3,
  "virtualNodesPerNode": 150,
  "dataDistribution": {
    "node1": 33.2,
    "node2": 33.5,
    "node3": 33.3
  }
}
```

### 6. Create a Snapshot

```bash
curl -X POST http://localhost:8080/api/v1/admin/snapshot/create
```

**Response:**
```json
{
  "snapshotName": "snapshot-2025-01-19T10-30-00.gz",
  "size": 1048576,
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### 7. Check Health

```bash
curl http://localhost:9090/healthcheck
```

**Response:**
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

## Consistency Levels

Control read/write consistency using the `consistency` query parameter:

### ONE (Fastest)
```bash
curl -X PUT "http://localhost:8080/api/v1/namespaces/users/kv/user:123?consistency=ONE" \
  -H "Content-Type: text/plain" \
  -d "John Doe"
```

### QUORUM (Balanced - Default)
```bash
curl -X PUT "http://localhost:8080/api/v1/namespaces/users/kv/user:123?consistency=QUORUM" \
  -H "Content-Type: text/plain" \
  -d "John Doe"
```

### ALL (Strongest)
```bash
curl -X PUT "http://localhost:8080/api/v1/namespaces/users/kv/user:123?consistency=ALL" \
  -H "Content-Type: text/plain" \
  -d "John Doe"
```

## Value Strategies

### STRING Strategy
Simple string values:
```bash
curl -X PUT http://localhost:8080/api/v1/namespaces/users/kv/name:john \
  -H "Content-Type: text/plain" \
  -d "John Doe"
```

### JSON_BLOB Strategy
JSON objects stored as strings:
```bash
curl -X PUT http://localhost:8080/api/v1/namespaces/users/kv/user:123 \
  -H "Content-Type: text/plain" \
  -d '{"name": "John Doe", "email": "john@example.com"}'
```

### ATOMIC_INCREMENT Strategy
Atomic counters:
```bash
# First PUT initializes the counter
curl -X PUT http://localhost:8080/api/v1/namespaces/counters/kv/page:views \
  -H "Content-Type: text/plain" \
  -d "0"

# Subsequent PUTs increment automatically
curl -X PUT http://localhost:8080/api/v1/namespaces/counters/kv/page:views \
  -H "Content-Type: text/plain" \
  -d "increment"
```

## Error Handling

All errors follow a consistent format:

```json
{
  "code": 404,
  "message": "Namespace not found: users",
  "details": "The requested namespace 'users' does not exist",
  "timestamp": "2025-01-19T10:30:00.000Z"
}
```

### Common HTTP Status Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200 | OK | Successful GET, PUT operations |
| 201 | Created | Successful POST (resource creation) |
| 204 | No Content | Successful DELETE operations |
| 400 | Bad Request | Invalid input, malformed JSON |
| 404 | Not Found | Namespace or key doesn't exist |
| 409 | Conflict | Resource already exists |
| 500 | Internal Server Error | Unexpected server error |
| 503 | Service Unavailable | Cluster unavailable, consistency not achievable |

## Pagination

For endpoints returning lists, use `limit` and `offset`:

```bash
# Get first 10 keys
curl "http://localhost:8080/api/v1/namespaces/users/kv?limit=10&offset=0"

# Get next 10 keys
curl "http://localhost:8080/api/v1/namespaces/users/kv?limit=10&offset=10"
```

## Metrics

Prometheus metrics are exposed at `/api/v1/admin/metrics`:

```bash
curl http://localhost:8080/api/v1/admin/metrics
```

Key metrics include:
- `keyval_requests_total` - Total request count by endpoint and namespace
- `keyval_request_duration_seconds` - Request latency histogram
- `keyval_storage_used_bytes` - Storage usage per namespace
- `keyval_replication_lag_seconds` - Replication lag between nodes

## Code Generation

Generate client libraries from the OpenAPI spec:

### Java
```bash
openapi-generator-cli generate \
  -i docs/openapi.yaml \
  -g java \
  -o client/java
```

### Python
```bash
openapi-generator-cli generate \
  -i docs/openapi.yaml \
  -g python \
  -o client/python
```

### Go
```bash
openapi-generator-cli generate \
  -i docs/openapi.yaml \
  -g go \
  -o client/go
```

### TypeScript/JavaScript
```bash
openapi-generator-cli generate \
  -i docs/openapi.yaml \
  -g typescript-fetch \
  -o client/typescript
```

## Testing the API

### Using Postman

1. Import the OpenAPI spec into Postman
2. Click "Import" → "File" → Select `docs/openapi.yaml`
3. Postman will create a collection with all endpoints

### Using HTTPie

```bash
# Create namespace
http POST localhost:8080/api/v1/namespaces \
  namespace=users \
  keyStrategy=HASH \
  valueStrategy=STRING

# Store value
echo "John Doe" | http PUT localhost:8080/api/v1/namespaces/users/kv/user:123

# Get value
http GET localhost:8080/api/v1/namespaces/users/kv/user:123
```

## API Versioning

The API is currently at version 1 (`/api/v1/`). Future versions will be available at:
- `/api/v2/` (when available)
- `/api/v3/` (when available)

Version 1 will be maintained for backward compatibility.

## Rate Limiting

Currently, no rate limiting is enforced. For production deployments, consider implementing rate limiting at the ingress/load balancer level.

## Security

The API currently does not require authentication. For production use:

1. Enable TLS/HTTPS at the ingress level
2. Implement API key authentication
3. Use network policies to restrict access
4. Enable audit logging

## Support

For issues or questions:
- GitHub Issues: https://github.com/sunilthamatam/keyval-store-services/issues
- Email: support@constelld.com
