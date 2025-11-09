# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder

LABEL maintainer="MANTIS Team"
LABEL stage="builder"

WORKDIR /build

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests in Docker build - run separately in CI)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

LABEL maintainer="MANTIS Team"
LABEL description="MANTIS IIoT Ingestion Service"
LABEL version="1.0.0"

# Install wget for health checks (Debian-based)
RUN apt-get update && \
    apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN groupadd -r mantis && useradd -r -g mantis mantis

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /build/target/*.jar app.jar

# Create directories for logs and buffer
RUN mkdir -p /app/logs /tmp/mantis-buffer && \
    chown -R mantis:mantis /app /tmp/mantis-buffer

# Switch to non-root user
USER mantis

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8001/actuator/health || exit 1

# Expose application port
EXPOSE 8001

# JVM options for containerized environment
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heap-dump.hprof \
    -XX:+ExitOnOutOfMemoryError \
    -XX:InitialRAMPercentage=50.0 \
    -XX:MaxRAMPercentage=80.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Duser.timezone=UTC"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
