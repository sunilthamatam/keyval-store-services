# KeyVal Store Helm Chart

This Helm chart deploys a distributed key-value store with off-heap storage and clustering support on Kubernetes.

## Features

- **Distributed Architecture**: StatefulSet-based deployment with configurable replicas
- **Off-Heap Storage**: Native memory storage to avoid GC overhead
- **Consistent Hashing**: Automatic data distribution across nodes
- **Configurable Replication**: Support for replication factor and consistency levels
- **Gossip Protocol**: Anti-entropy for cluster state synchronization
- **Persistence**: PersistentVolumeClaims for database and snapshots
- **Monitoring**: Prometheus metrics and health checks
- **High Availability**: Pod disruption budgets and anti-affinity rules

## Prerequisites

- Kubernetes 1.19+
- Helm 3.0+
- PersistentVolume provisioner support in the underlying infrastructure (if persistence is enabled)

## Installing the Chart

To install the chart with the release name `my-keyval-store`:

```bash
helm install my-keyval-store ./helm/keyval-store
```

The command deploys KeyVal Store on the Kubernetes cluster with default configuration. The [Parameters](#parameters) section lists the parameters that can be configured during installation.

## Uninstalling the Chart

To uninstall/delete the `my-keyval-store` deployment:

```bash
helm uninstall my-keyval-store
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Parameters

### Global Parameters

| Name | Description | Value |
|------|-------------|-------|
| `replicaCount` | Number of KeyVal Store replicas | `3` |
| `image.repository` | KeyVal Store image repository | `constelld/keyval-store` |
| `image.pullPolicy` | Image pull policy | `IfNotPresent` |
| `image.tag` | Image tag (defaults to chart appVersion) | `1.0.0` |

### Service Parameters

| Name | Description | Value |
|------|-------------|-------|
| `service.type` | Kubernetes service type | `ClusterIP` |
| `service.port` | Application service port | `8080` |
| `service.adminPort` | Admin service port | `9090` |

### Ingress Parameters

| Name | Description | Value |
|------|-------------|-------|
| `ingress.enabled` | Enable ingress controller resource | `false` |
| `ingress.className` | Ingress class name | `""` |
| `ingress.hosts[0].host` | Default host for the ingress resource | `keyval-store.local` |

### Persistence Parameters

| Name | Description | Value |
|------|-------------|-------|
| `persistence.enabled` | Enable persistence for database | `true` |
| `persistence.storageClass` | PVC Storage Class | `""` |
| `persistence.size` | PVC Storage Request size | `10Gi` |
| `snapshotPersistence.enabled` | Enable persistence for snapshots | `true` |
| `snapshotPersistence.size` | PVC Storage Request size for snapshots | `20Gi` |

### Application Configuration

| Name | Description | Value |
|------|-------------|-------|
| `config.cluster.replicationFactor` | Number of replicas per key | `3` |
| `config.cluster.virtualNodesPerNode` | Virtual nodes for consistent hashing | `150` |
| `config.storage.maxOffHeapMemory` | Maximum off-heap memory | `2GB` |
| `config.consistency.defaultReadLevel` | Default read consistency (ONE/QUORUM/ALL) | `QUORUM` |
| `config.consistency.defaultWriteLevel` | Default write consistency (ONE/QUORUM/ALL) | `QUORUM` |

### Resource Limits

| Name | Description | Value |
|------|-------------|-------|
| `resources.limits.cpu` | CPU limit | `2000m` |
| `resources.limits.memory` | Memory limit | `4Gi` |
| `resources.requests.cpu` | CPU request | `500m` |
| `resources.requests.memory` | Memory request | `2Gi` |

### Monitoring

| Name | Description | Value |
|------|-------------|-------|
| `serviceMonitor.enabled` | Create ServiceMonitor for Prometheus Operator | `false` |
| `serviceMonitor.interval` | Scrape interval | `30s` |

## Configuration and Installation Details

### Cluster Formation

The chart automatically configures seed nodes for cluster formation using the StatefulSet's headless service. Each pod discovers other pods via DNS:

```
<statefulset-name>-<ordinal>.<headless-service>.<namespace>.svc.cluster.local
```

### Persistence

By default, the chart creates PersistentVolumeClaims for:
- **Database**: SQLite database files (`10Gi` default)
- **Snapshots**: Backup snapshots (`20Gi` default)

To disable persistence:

```bash
helm install my-keyval-store ./helm/keyval-store \
  --set persistence.enabled=false \
  --set snapshotPersistence.enabled=false
```

### High Availability

The chart includes:
- Pod anti-affinity rules to spread pods across nodes
- Pod disruption budget (minimum 2 available by default)
- Readiness, liveness, and startup probes

### Monitoring with Prometheus

To enable Prometheus ServiceMonitor:

```bash
helm install my-keyval-store ./helm/keyval-store \
  --set serviceMonitor.enabled=true
```

Metrics are exposed at `/api/v1/admin/metrics`.

### Custom Configuration

Create a custom `values.yaml` file:

```yaml
replicaCount: 5

resources:
  limits:
    memory: 8Gi
  requests:
    memory: 4Gi

config:
  cluster:
    replicationFactor: 5
  storage:
    maxOffHeapMemory: 4GB
  consistency:
    defaultReadLevel: ALL
    defaultWriteLevel: QUORUM
```

Install with custom values:

```bash
helm install my-keyval-store ./helm/keyval-store -f custom-values.yaml
```

## Upgrading

To upgrade the KeyVal Store deployment:

```bash
helm upgrade my-keyval-store ./helm/keyval-store
```

## Examples

### Port Forwarding

```bash
kubectl port-forward svc/my-keyval-store 8080:8080 9090:9090
```

### Create a Namespace

```bash
curl -X POST http://localhost:8080/api/v1/namespaces \
  -H "Content-Type: application/json" \
  -d '{"namespace":"users","keyStrategy":"HASH","valueStrategy":"STRING"}'
```

### Store and Retrieve Values

```bash
# Store
curl -X PUT http://localhost:8080/api/v1/namespaces/users/kv/user123 \
  -H "Content-Type: text/plain" \
  -d 'John Doe'

# Retrieve
curl http://localhost:8080/api/v1/namespaces/users/kv/user123
```

### Check Cluster Status

```bash
curl http://localhost:8080/api/v1/admin/cluster/status
```

### View Metrics

```bash
curl http://localhost:8080/api/v1/admin/metrics
```

## Troubleshooting

### View Pod Logs

```bash
kubectl logs -f my-keyval-store-0
```

### Check Pod Status

```bash
kubectl get pods -l app.kubernetes.io/name=keyval-store
```

### Describe StatefulSet

```bash
kubectl describe statefulset my-keyval-store
```

### Check PersistentVolumeClaims

```bash
kubectl get pvc
```

## License

Copyright © 2025 Constelld
