# Multi-stage build for KeyVal Store

# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom files first for better layer caching
COPY pom.xml .
COPY keyval-core/pom.xml keyval-core/
COPY keyval-storage/pom.xml keyval-storage/
COPY keyval-cluster/pom.xml keyval-cluster/
COPY keyval-persistence/pom.xml keyval-persistence/
COPY keyval-api/pom.xml keyval-api/
COPY keyval-application/pom.xml keyval-application/

# Download dependencies (cached if pom files haven't changed)
RUN mvn dependency:go-offline -B

# Copy source code
COPY keyval-core/src keyval-core/src
COPY keyval-storage/src keyval-storage/src
COPY keyval-cluster/src keyval-cluster/src
COPY keyval-persistence/src keyval-persistence/src
COPY keyval-api/src keyval-api/src
COPY keyval-application/src keyval-application/src

# Build the application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# Stage 2: Create runtime image
FROM eclipse-temurin:21-jre-jammy

# Install required packages
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Create app user and group
RUN groupadd -r appuser && useradd -r -g appuser -u 1000 appuser

# Create application directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /build/keyval-application/target/keyval-application-*.jar /app/keyval-application.jar

# Create directories for data, logs, and snapshots
RUN mkdir -p /data/db /data/logs /data/snapshots && \
    chown -R appuser:appuser /app /data

# Switch to non-root user
USER appuser

# Expose ports
# 8080: Application HTTP port
# 9090: Admin HTTP port
EXPOSE 8080 9090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:9090/healthcheck || exit 1

# Set JVM options
ENV JAVA_OPTS="-Xmx2g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/data/logs"

# Default command
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/keyval-application.jar server /config/config.yml"]

# Labels
LABEL maintainer="Constelld Team <support@constelld.com>" \
      description="Distributed Key-Value Store with Off-Heap Storage" \
      version="1.0.0" \
      org.opencontainers.image.source="https://github.com/sunilthamatam/keyval-store-services" \
      org.opencontainers.image.vendor="Constelld" \
      org.opencontainers.image.title="KeyVal Store" \
      org.opencontainers.image.description="A distributed key-value store with off-heap storage and clustering support" \
      org.opencontainers.image.version="1.0.0"
