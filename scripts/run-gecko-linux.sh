#!/bin/bash
#
# GeckoCIRCUITS Launcher for Linux
#
# Usage:
#   ./run-gecko-linux.sh                    - Start GeckoCIRCUITS
#   ./run-gecko-linux.sh circuit.ipes       - Open a circuit file
#   ./run-gecko-linux.sh --hidpi            - Start with HiDPI scaling (4K displays)
#   ./run-gecko-linux.sh --headless         - Run headless with Xvfb

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# JAR file location
JAR_FILE="$PROJECT_DIR/target/gecko-1.0-jar-with-dependencies.jar"

# Default JVM options
JVM_OPTS="-Xmx3G -Dpolyglot.js.nashorn-compat=true"

# Better font rendering on Linux
JVM_OPTS="$JVM_OPTS -Dawt.useSystemAAFontSettings=on"

# Parse arguments
CIRCUIT_FILE=""
HIDPI=""
HEADLESS=""

show_help() {
    echo "GeckoCIRCUITS Launcher for Linux"
    echo ""
    echo "Usage:"
    echo "  $0 [options] [circuit.ipes]"
    echo ""
    echo "Options:"
    echo "  --hidpi     Enable HiDPI scaling for 4K displays"
    echo "  --headless  Run with Xvfb (virtual framebuffer)"
    echo "  -h, --help  Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                        Start GeckoCIRCUITS"
    echo "  $0 my_circuit.ipes        Open a circuit file"
    echo "  $0 --hidpi                Start with HiDPI scaling"
    echo "  $0 --headless             Run headless (for testing)"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --hidpi)
            HIDPI=1
            shift
            ;;
        --headless)
            HEADLESS=1
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
    echo "Install Java 21:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-21-jdk"
    echo "  Fedora:        sudo dnf install java-21-openjdk"
    echo "  Arch:          sudo pacman -S jdk21-openjdk"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -lt 21 ]]; then
    echo "Warning: Java 21+ recommended (found: $JAVA_VERSION)"
fi

# Check DISPLAY for GUI mode
if [[ -z "$HEADLESS" && -z "$DISPLAY" ]]; then
    echo "Warning: DISPLAY not set. GUI may not work."
    echo "  Set DISPLAY or use --headless flag"
fi

# Display startup info
echo "============================================"
echo "GeckoCIRCUITS Launcher (Linux)"
echo "============================================"
echo "JAR: $JAR_FILE"
[[ -n "$HIDPI" ]] && echo "HiDPI: enabled"
[[ -n "$HEADLESS" ]] && echo "Mode: headless (Xvfb)"
[[ -n "$CIRCUIT_FILE" ]] && echo "Circuit: $CIRCUIT_FILE"
echo ""

# Build command
if [[ -n "$CIRCUIT_FILE" ]]; then
    CMD="java $JVM_OPTS -jar \"$JAR_FILE\" \"$CIRCUIT_FILE\""
else
    CMD="java $JVM_OPTS -jar \"$JAR_FILE\""
fi

# Run GeckoCIRCUITS
if [[ -n "$HEADLESS" ]]; then
    # Check for xvfb-run
    if ! command -v xvfb-run &> /dev/null; then
        echo "Error: xvfb-run not found"
        echo "  Install with: sudo apt install xvfb"
        exit 1
    fi
    eval "xvfb-run -a $CMD"
else
    eval "$CMD"
fi
