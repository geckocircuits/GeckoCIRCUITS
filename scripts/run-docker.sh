#!/bin/bash
# Run GeckoCIRCUITS REST API Docker container

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

# Configuration
IMAGE_NAME="geckocircuits/rest-api:latest"
CONTAINER_NAME="gecko-rest-api"
PORT=8080

echo "========================================"
echo "Running GeckoCIRCUITS REST API"
echo "========================================"

# Check if container is already running
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Container '$CONTAINER_NAME' already exists."
    echo "Stopping and removing..."
    docker stop "$CONTAINER_NAME" 2>/dev/null || true
    docker rm "$CONTAINER_NAME" 2>/dev/null || true
fi

echo ""
echo "Starting container..."
echo "  Image: $IMAGE_NAME"
echo "  Port: $PORT"
echo ""

# Run container
docker run -d \
    --name "$CONTAINER_NAME" \
    -p "${PORT}:8080" \
    -v "${PROJECT_ROOT}/resources/examples:/app/circuits:ro" \
    -v "${PROJECT_ROOT}/logs:/app/logs" \
    -e JAVA_OPTS="-Xmx1g -Xms512m" \
    -e LOG_LEVEL=INFO \
    --restart unless-stopped \
    "$IMAGE_NAME"

echo "Container started successfully!"
echo ""
echo "========================================"
echo "Access points:"
echo "========================================"
echo "  Health check: http://localhost:${PORT}/api/health"
echo "  API info:     http://localhost:${PORT}/api/info"
echo "  Swagger UI:   http://localhost:${PORT}/swagger-ui.html"
echo "  OpenAPI spec: http://localhost:${PORT}/api-docs"
echo ""
echo "Logs:"
echo "  docker logs -f $CONTAINER_NAME"
echo ""
echo "Stop:"
echo "  docker stop $CONTAINER_NAME"
echo ""
