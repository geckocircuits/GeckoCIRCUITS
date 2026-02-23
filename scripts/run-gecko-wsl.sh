#!/bin/bash
#
# GeckoCIRCUITS Launcher for WSL (Windows Subsystem for Linux)
#
# Usage:
#   ./run-gecko-wsl.sh                    - Start GeckoCIRCUITS
#   ./run-gecko-wsl.sh circuit.ipes       - Open a circuit file
#   ./run-gecko-wsl.sh --hidpi            - Start with HiDPI scaling (4K displays)
#   ./run-gecko-wsl.sh --headless         - Run headless with Xvfb
#
# Requirements:
#   - WSL2 with WSLg (Windows 11 or Windows 10 with WSLg)
#   - Or X server on Windows (VcXsrv, X410, etc.)
#   - Java 21+ installed in WSL

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# JAR file location
JAR_FILE="$PROJECT_DIR/target/gecko-1.0-jar-with-dependencies.jar"

# Default JVM options
JVM_OPTS="-Xmx3G -Dpolyglot.js.nashorn-compat=true"

# Better font rendering on Linux/WSL
JVM_OPTS="$JVM_OPTS -Dawt.useSystemAAFontSettings=on"

# Parse arguments
CIRCUIT_FILE=""
HIDPI=""
HEADLESS=""

show_help() {
    echo "GeckoCIRCUITS Launcher for WSL"
    echo ""
    echo "Usage:"
    echo "  $0 [options] [circuit.ipes]"
    echo ""
    echo "Options:"
    echo "  --hidpi     Enable HiDPI scaling for 4K displays"
    echo "  --headless  Run with Xvfb (virtual framebuffer)"
    echo "  -h, --help  Show this help message"
    echo ""
    echo "Display Configuration:"
    echo "  WSLg (Windows 11):  Automatic, uses DISPLAY=:0"
    echo "  X server on Windows: Set DISPLAY=<IP>:0"
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

# Check if running on WSL
if ! grep -qi microsoft /proc/version 2>/dev/null; then
    echo "Warning: This doesn't appear to be WSL"
    echo "  Consider using run-gecko-linux.sh instead"
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
    echo "Run the setup script first:"
    echo "  ./scripts/setup-wsl.sh"
    echo ""
    echo "Or install manually:"
    echo "  sudo apt install openjdk-21-jdk"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
if [[ "$JAVA_VERSION" -lt 21 ]]; then
    echo "Warning: Java 21+ recommended (found: $JAVA_VERSION)"
fi

# Configure DISPLAY for WSL
if [[ -z "$HEADLESS" ]]; then
    # Check for WSLg (Windows 11)
    if [[ -S "/tmp/.X11-unix/X0" ]]; then
        # WSLg is available
        if [[ -z "$DISPLAY" ]]; then
            export DISPLAY=:0
            echo "Using WSLg display: $DISPLAY"
        fi
    elif [[ -z "$DISPLAY" ]]; then
        # Try to detect Windows host IP for X server
        WIN_HOST=$(grep -m 1 nameserver /etc/resolv.conf | awk '{print $2}')
        if [[ -n "$WIN_HOST" ]]; then
            echo "Warning: DISPLAY not set"
            echo "  For X server on Windows, try:"
            echo "  export DISPLAY=$WIN_HOST:0"
            echo ""
            echo "  Or use --headless flag for Xvfb"
        else
            echo "Warning: DISPLAY not set and could not detect Windows host"
            echo "  Use --headless flag for Xvfb"
        fi
    fi
fi

# Display startup info
echo "============================================"
echo "GeckoCIRCUITS Launcher (WSL)"
echo "============================================"
echo "JAR: $JAR_FILE"
echo "DISPLAY: ${DISPLAY:-not set}"
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
