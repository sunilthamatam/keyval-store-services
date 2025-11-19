package com.constelld.keyvalstore.cluster.partition;

import com.constelld.keyvalstore.core.model.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistentHashRingTest {

    private ConsistentHashRing hashRing;

    @BeforeEach
    void setUp() {
        hashRing = new ConsistentHashRing(3); // 3 virtual nodes for testing
    }

    @Test
    void testAddNode() {
        Node node1 = new Node("node1", "localhost", 8081, 9091);

        hashRing.addNode(node1);

        assertThat(hashRing.getPhysicalNodeCount()).isEqualTo(1);
        assertThat(hashRing.getRingSize()).isEqualTo(3); // 3 virtual nodes
    }

    @Test
    void testRemoveNode() {
        Node node1 = new Node("node1", "localhost", 8081, 9091);

        hashRing.addNode(node1);
        assertThat(hashRing.getPhysicalNodeCount()).isEqualTo(1);

        hashRing.removeNode(node1);
        assertThat(hashRing.getPhysicalNodeCount()).isZero();
        assertThat(hashRing.getRingSize()).isZero();
    }

    @Test
    void testGetPrimaryNode() {
        Node node1 = new Node("node1", "localhost", 8081, 9091);
        Node node2 = new Node("node2", "localhost", 8082, 9092);

        hashRing.addNode(node1);
        hashRing.addNode(node2);

        Node primary = hashRing.getPrimaryNode("someKey");
        assertThat(primary).isIn(node1, node2);

        // Same key should always return same node
        Node primary2 = hashRing.getPrimaryNode("someKey");
        assertThat(primary2).isEqualTo(primary);
    }

    @Test
    void testGetReplicaNodes() {
        Node node1 = new Node("node1", "localhost", 8081, 9091);
        Node node2 = new Node("node2", "localhost", 8082, 9092);
        Node node3 = new Node("node3", "localhost", 8083, 9093);

        hashRing.addNode(node1);
        hashRing.addNode(node2);
        hashRing.addNode(node3);

        List<Node> replicas = hashRing.getReplicaNodes("someKey", 2);

        assertThat(replicas).hasSize(2);
        assertThat(replicas).doesNotHaveDuplicates();

        // Same key should always return same replicas
        List<Node> replicas2 = hashRing.getReplicaNodes("someKey", 2);
        assertThat(replicas2).isEqualTo(replicas);
    }

    @Test
    void testGetAllNodes() {
        Node node1 = new Node("node1", "localhost", 8081, 9091);
        Node node2 = new Node("node2", "localhost", 8082, 9092);

        hashRing.addNode(node1);
        hashRing.addNode(node2);

        List<Node> nodes = hashRing.getAllNodes();
        assertThat(nodes).hasSize(2);
        assertThat(nodes).contains(node1, node2);
    }
}
