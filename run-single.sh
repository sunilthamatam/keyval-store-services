#!/bin/bash

# Run KeyVal Store in single-node mode

echo "Building project..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo "Starting KeyVal Store (single node)..."
java -XX:MaxDirectMemorySize=4g \
     -Xmx2g \
     -jar keyval-application/target/keyval-application-1.0.0-SNAPSHOT.jar \
     server keyval-application/src/main/resources/config.yml
