#!/bin/bash
#
# Build GeckoCIRCUITS distribution packages
#
# Usage:
#   ./_build/build-distributions.sh          Build all distributions
#   ./_build/build-distributions.sh windows  Build Windows only
#   ./_build/build-distributions.sh examples Build examples only
#
# Output: target/

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Parse arguments
TARGETS="${1:-all}"

echo "============================================"
echo "GeckoCIRCUITS Distribution Builder"
echo "============================================"
echo ""

case "$TARGETS" in
    all)
        echo "Building all distributions..."
        mvn clean package -Pdist-all -DskipTests
        ;;
    windows)
        echo "Building Windows distribution..."
        mvn clean package -Pdist-windows -DskipTests
        ;;
    linux)
        echo "Building Linux distribution..."
        mvn clean package -Pdist-linux -DskipTests
        ;;
    macos)
        echo "Building macOS distribution..."
        mvn clean package -Pdist-macos -DskipTests
        ;;
    wsl)
        echo "Building WSL distribution..."
        mvn clean package -Pdist-wsl -DskipTests
        ;;
    examples)
        echo "Building Examples distribution..."
        mvn clean package -Pdist-examples -DskipTests
        ;;
    *)
        echo "Unknown target: $TARGETS"
        echo "Valid targets: all, windows, linux, macos, wsl, examples"
        exit 1
        ;;
esac

echo ""
echo "============================================"
echo "Distribution packages created:"
echo "============================================"
ls -lh target/GeckoCIRCUITS-*.zip 2>/dev/null || echo "No packages found"
