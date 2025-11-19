package com.constelld.keyvalstore.core.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents a key-value pair with metadata.
 */
public class KeyValue {
    private final String namespace;
    private final String key;
    private final byte[] value;
    private final Instant timestamp;
    private final long version;

    public KeyValue(String namespace, String key, byte[] value, Instant timestamp, long version) {
        this.namespace = Objects.requireNonNull(namespace, "namespace cannot be null");
        this.key = Objects.requireNonNull(key, "key cannot be null");
        this.value = Objects.requireNonNull(value, "value cannot be null");
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.version = version;
    }

    public KeyValue(String namespace, String key, byte[] value) {
        this(namespace, key, value, Instant.now(), 1L);
    }

    public String getNamespace() {
        return namespace;
    }

    public String getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getVersion() {
        return version;
    }

    public KeyValue withValue(byte[] newValue) {
        return new KeyValue(namespace, key, newValue, Instant.now(), version + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyValue keyValue = (KeyValue) o;
        return Objects.equals(namespace, keyValue.namespace) &&
                Objects.equals(key, keyValue.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, key);
    }

    @Override
    public String toString() {
        return "KeyValue{" +
                "namespace='" + namespace + '\'' +
                ", key='" + key + '\'' +
                ", timestamp=" + timestamp +
                ", version=" + version +
                '}';
    }
}
