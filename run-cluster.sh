#!/bin/bash

# Run KeyVal Store in cluster mode (3 nodes)

echo "Building project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Starting 3-node cluster..."

# Start Node 1
echo "Starting Node 1 on port 8081..."
java -XX:MaxDirectMemorySize=4g \
     -Xmx2g \
     -jar keyval-application/target/keyval-application-1.0.0-SNAPSHOT.jar \
     server keyval-application/config-node1.yml > node1.log 2>&1 &
NODE1_PID=$!
echo "Node 1 started (PID: $NODE1_PID)"

# Wait for node 1 to start
sleep 5

# Start Node 2
echo "Starting Node 2 on port 8082..."
java -XX:MaxDirectMemorySize=4g \
     -Xmx2g \
     -jar keyval-application/target/keyval-application-1.0.0-SNAPSHOT.jar \
     server keyval-application/config-node2.yml > node2.log 2>&1 &
NODE2_PID=$!
echo "Node 2 started (PID: $NODE2_PID)"

# Wait for node 2 to start
sleep 5

# Start Node 3
echo "Starting Node 3 on port 8083..."
java -XX:MaxDirectMemorySize=4g \
     -Xmx2g \
     -jar keyval-application/target/keyval-application-1.0.0-SNAPSHOT.jar \
     server keyval-application/config-node3.yml > node3.log 2>&1 &
NODE3_PID=$!
echo "Node 3 started (PID: $NODE3_PID)"

echo ""
echo "Cluster is running!"
echo "Node 1: http://localhost:8081 (Admin: http://localhost:9091)"
echo "Node 2: http://localhost:8082 (Admin: http://localhost:9092)"
echo "Node 3: http://localhost:8083 (Admin: http://localhost:9093)"
echo ""
echo "PIDs: $NODE1_PID, $NODE2_PID, $NODE3_PID"
echo "Logs: node1.log, node2.log, node3.log"
echo ""
echo "To stop the cluster:"
echo "  kill $NODE1_PID $NODE2_PID $NODE3_PID"
