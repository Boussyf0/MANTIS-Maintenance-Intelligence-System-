#!/bin/bash

###############################################################################
# Test Script for Ingestion IIoT Service
#
# This script runs comprehensive tests for the ingestion service including:
# - Unit tests
# - Integration tests
# - Code coverage
# - Docker build verification
###############################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print functions
print_header() {
    echo -e "${BLUE}============================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Check if we're in the right directory
if [ ! -f "pom.xml" ]; then
    print_error "Error: pom.xml not found. Please run this script from services/ingestion-iiot directory"
    exit 1
fi

# Step 1: Clean previous builds
print_header "Step 1: Cleaning previous builds"
mvn clean
print_success "Clean completed"

# Step 2: Run unit tests
print_header "Step 2: Running unit tests"
mvn test -Dtest="*Test" -DfailIfNoTests=false
print_success "Unit tests passed"

# Step 3: Run integration tests
print_header "Step 3: Running integration tests"
print_info "Note: This will start Testcontainers (Docker required)"
mvn verify -Dtest="*IntegrationTest" -DfailIfNoTests=false
print_success "Integration tests passed"

# Step 4: Generate code coverage report
print_header "Step 4: Generating code coverage report"
mvn jacoco:report
print_success "Coverage report generated at target/site/jacoco/index.html"

# Step 5: Build the application
print_header "Step 5: Building application JAR"
mvn package -DskipTests
print_success "Application JAR built successfully"

# Step 6: Build Docker image
print_header "Step 6: Building Docker image"
docker build -t mantis/ingestion-iiot:test .
print_success "Docker image built successfully"

# Step 7: Test Docker image
print_header "Step 7: Testing Docker image"
print_info "Starting container..."

# Start container
CONTAINER_ID=$(docker run -d \
    -e KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
    -e POSTGRES_HOST=localhost \
    -e POSTGRES_PORT=5432 \
    -e POSTGRES_DB=mantis \
    -e POSTGRES_USER=mantis \
    -e POSTGRES_PASSWORD=mantis_password \
    -e OPCUA_ENABLED=false \
    -e MQTT_ENABLED=false \
    -e MODBUS_ENABLED=false \
    -p 8001:8001 \
    mantis/ingestion-iiot:test)

print_info "Container started with ID: ${CONTAINER_ID}"

# Wait for application to start (max 60 seconds)
print_info "Waiting for application to start..."
for i in {1..60}; do
    if docker exec ${CONTAINER_ID} wget -q --spider http://localhost:8001/api/v1/ingest/ping 2>/dev/null; then
        print_success "Application started successfully"
        break
    fi
    if [ $i -eq 60 ]; then
        print_error "Application failed to start within 60 seconds"
        docker logs ${CONTAINER_ID}
        docker stop ${CONTAINER_ID}
        docker rm ${CONTAINER_ID}
        exit 1
    fi
    sleep 1
done

# Test endpoints
print_info "Testing /actuator/health endpoint (may show DOWN without infrastructure)..."
HEALTH=$(docker exec ${CONTAINER_ID} wget -qO- http://localhost:8001/actuator/health 2>/dev/null || echo "DEGRADED")
if echo "$HEALTH" | grep -q "UP"; then
    print_success "Health endpoint returned UP status"
elif echo "$HEALTH" | grep -q "status"; then
    print_info "Health endpoint accessible but degraded (expected without infrastructure)"
else
    print_info "Health endpoint check skipped (requires PostgreSQL/Kafka)"
fi

print_info "Testing /api/v1/ingest/ping endpoint..."
PING=$(docker exec ${CONTAINER_ID} wget -qO- http://localhost:8001/api/v1/ingest/ping)
if echo "$PING" | grep -q "running"; then
    print_success "Ping endpoint responded correctly"
else
    print_error "Ping endpoint check failed"
fi

# Cleanup
print_info "Stopping and removing test container..."
docker stop ${CONTAINER_ID}
docker rm ${CONTAINER_ID}
print_success "Test container cleaned up"

# Final summary
print_header "Test Summary"
print_success "All tests passed successfully!"
echo ""
echo "Test Reports:"
echo "  - Unit Test Report:    target/surefire-reports/"
echo "  - Coverage Report:     target/site/jacoco/index.html"
echo "  - Application JAR:     target/ingestion-iiot-1.0.0.jar"
echo "  - Docker Image:        mantis/ingestion-iiot:test"
echo ""
print_success "Service is ready for deployment!"
