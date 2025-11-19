package com.constelld.keyvalstore.api.metrics;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.common.TextFormat;
import io.prometheus.client.hotspot.DefaultExports;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.io.StringWriter;
import java.io.Writer;

/**
 * Prometheus metrics endpoint.
 */
@Path("/metrics")
public class PrometheusMetricsResource {

    private static final CollectorRegistry registry = CollectorRegistry.defaultRegistry;

    // Metrics
    public static final Counter requestsTotal = Counter.build()
            .name("keyvalstore_requests_total")
            .help("Total requests")
            .labelNames("operation", "namespace")
            .register(registry);

    public static final Histogram requestDuration = Histogram.build()
            .name("keyvalstore_request_duration_seconds")
            .help("Request duration in seconds")
            .labelNames("operation")
            .register(registry);

    public static final Gauge storageKeys = Gauge.build()
            .name("keyvalstore_keys_total")
            .help("Total number of keys")
            .register(registry);

    public static final Gauge storageMemory = Gauge.build()
            .name("keyvalstore_memory_bytes")
            .help("Memory usage in bytes")
            .register(registry);

    public static final Gauge clusterNodes = Gauge.build()
            .name("keyvalstore_cluster_nodes")
            .help("Number of cluster nodes")
            .labelNames("status")
            .register(registry);

    static {
        // Register JVM metrics
        DefaultExports.initialize();
    }

    @GET
    @Produces(TextFormat.CONTENT_TYPE_004)
    public String getMetrics() {
        try {
            Writer writer = new StringWriter();
            TextFormat.write004(writer, registry.metricFamilySamples());
            return writer.toString();
        } catch (Exception e) {
            return "# Error generating metrics: " + e.getMessage();
        }
    }
}
