#!/bin/bash
# Build Docker image for GeckoCIRCUITS REST API

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "========================================"
echo "Building GeckoCIRCUITS REST API Docker image"
echo "========================================"

# Image tag
IMAGE_NAME="geckocircuits/rest-api"
VERSION="1.0.0"
TAG="${IMAGE_NAME}:${VERSION}"
LATEST_TAG="${IMAGE_NAME}:latest"

echo ""
echo "Building image: $TAG"
echo ""

# Build using docker-compose (ensures consistent build)
docker-compose build gecko-rest-api

# Tag as latest
docker tag "$TAG" "$LATEST_TAG"

echo ""
echo "========================================"
echo "Build complete!"
echo "========================================"
echo "Image tags:"
echo "  - $TAG"
echo "  - $LATEST_TAG"
echo ""
echo "Image size:"
docker images "$IMAGE_NAME" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}"
echo ""
echo "To run the container:"
echo "  docker-compose up -d"
echo ""
echo "Or manually:"
echo "  docker run -p 8080:8080 $TAG"
echo ""
