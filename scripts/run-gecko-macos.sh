#!/bin/bash
#
# GeckoCIRCUITS Launcher for macOS
#
# Usage:
#   ./run-gecko-macos.sh                    - Start GeckoCIRCUITS
#   ./run-gecko-macos.sh circuit.ipes       - Open a circuit file
#   ./run-gecko-macos.sh --hidpi            - Start with HiDPI scaling (Retina)
#   ./run-gecko-macos.sh --hidpi circuit.ipes

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# JAR file location
JAR_FILE="$PROJECT_DIR/target/gecko-1.0-jar-with-dependencies.jar"

# Default JVM options
JVM_OPTS="-Xmx3G -Dpolyglot.js.nashorn-compat=true"

# macOS-specific options for better rendering
JVM_OPTS="$JVM_OPTS -Dapple.laf.useScreenMenuBar=true"
JVM_OPTS="$JVM_OPTS -Dapple.awt.application.name=GeckoCIRCUITS"

# Parse arguments
CIRCUIT_FILE=""
HIDPI=""

show_help() {
    echo "GeckoCIRCUITS Launcher for macOS"
    echo ""
    echo "Usage:"
    echo "  $0 [options] [circuit.ipes]"
    echo ""
    echo "Options:"
    echo "  --hidpi     Enable HiDPI scaling for Retina displays"
    echo "  -h, --help  Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                        Start GeckoCIRCUITS"
    echo "  $0 my_circuit.ipes        Open a circuit file"
    echo "  $0 --hidpi                Start with HiDPI scaling"
    echo "  $0 --hidpi circuit.ipes   HiDPI with circuit file"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --hidpi)
            HIDPI=1
            shift
            ;;
        -h|--help)
            show_help
            ;;
        *)
            CIRCUIT_FILE="$1"
            shift
            ;;
    esac
done

# Add HiDPI scaling if requested
if [[ -n "$HIDPI" ]]; then
    JVM_OPTS="$JVM_OPTS -Dsun.java2d.uiScale=2"
fi

# Check if JAR exists
if [[ ! -f "$JAR_FILE" ]]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo ""
    echo "Please build the project first:"
    echo "  cd $PROJECT_DIR"
    echo "  mvn clean package assembly:single -DskipTests"
    exit 1
fi

# Check Java installation
if ! command -v java &> /dev/null; then
    echo "Error: Java not found in PATH"
    echo ""
    echo "Install Java 21 with Homebrew:"
    echo "  brew install openjdk@21"
    echo ""
    echo "Or download from: https://adoptium.net/"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -lt 21 ]]; then
    echo "Warning: Java 21+ recommended (found: $JAVA_VERSION)"
fi

# Display startup info
echo "============================================"
echo "GeckoCIRCUITS Launcher (macOS)"
echo "============================================"
echo "JAR: $JAR_FILE"
[[ -n "$HIDPI" ]] && echo "HiDPI: enabled"
[[ -n "$CIRCUIT_FILE" ]] && echo "Circuit: $CIRCUIT_FILE"
echo ""

# Run GeckoCIRCUITS
if [[ -n "$CIRCUIT_FILE" ]]; then
    java $JVM_OPTS -jar "$JAR_FILE" "$CIRCUIT_FILE"
else
    java $JVM_OPTS -jar "$JAR_FILE"
fi
