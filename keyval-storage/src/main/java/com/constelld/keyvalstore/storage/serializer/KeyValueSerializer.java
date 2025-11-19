package com.constelld.keyvalstore.storage.serializer;

import com.constelld.keyvalstore.core.model.KeyValue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Serializer for KeyValue objects to byte arrays for storage.
 *
 * Format:
 * [namespace_length(4)][namespace][key_length(4)][key][value_length(4)][value][timestamp(8)][version(8)]
 */
public class KeyValueSerializer {

    public byte[] serialize(KeyValue keyValue) {
        byte[] namespaceBytes = keyValue.getNamespace().getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = keyValue.getKey().getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = keyValue.getValue();

        int totalSize = 4 + namespaceBytes.length +
                4 + keyBytes.length +
                4 + valueBytes.length +
                8 + 8; // timestamp + version

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);

        // Namespace
        buffer.putInt(namespaceBytes.length);
        buffer.put(namespaceBytes);

        // Key
        buffer.putInt(keyBytes.length);
        buffer.put(keyBytes);

        // Value
        buffer.putInt(valueBytes.length);
        buffer.put(valueBytes);

        // Timestamp (milliseconds since epoch)
        buffer.putLong(keyValue.getTimestamp().toEpochMilli());

        // Version
        buffer.putLong(keyValue.getVersion());

        return buffer.array();
    }

    public KeyValue deserialize(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // Namespace
        int namespaceLength = buffer.getInt();
        byte[] namespaceBytes = new byte[namespaceLength];
        buffer.get(namespaceBytes);
        String namespace = new String(namespaceBytes, StandardCharsets.UTF_8);

        // Key
        int keyLength = buffer.getInt();
        byte[] keyBytes = new byte[keyLength];
        buffer.get(keyBytes);
        String key = new String(keyBytes, StandardCharsets.UTF_8);

        // Value
        int valueLength = buffer.getInt();
        byte[] value = new byte[valueLength];
        buffer.get(value);

        // Timestamp
        long timestampMillis = buffer.getLong();
        Instant timestamp = Instant.ofEpochMilli(timestampMillis);

        // Version
        long version = buffer.getLong();

        return new KeyValue(namespace, key, value, timestamp, version);
    }
}
