#!/bin/bash
#
# GeckoCIRCUITS WSL Setup Script
#
# This script installs all dependencies required to build and run GeckoCIRCUITS
# on Windows Subsystem for Linux (WSL).
#
# Usage:
#   chmod +x scripts/setup-wsl.sh
#   ./scripts/setup-wsl.sh
#
# Requirements:
#   - Ubuntu 22.04 or 24.04 on WSL2
#   - sudo access
#

set -e

echo "============================================"
echo "GeckoCIRCUITS WSL Setup Script"
echo "============================================"
echo ""

# Check if running on WSL
if ! grep -qi microsoft /proc/version 2>/dev/null; then
    echo "Warning: This doesn't appear to be WSL. Continuing anyway..."
fi

# Check Ubuntu version
if [ -f /etc/os-release ]; then
    . /etc/os-release
    echo "Detected: $NAME $VERSION_ID"
else
    echo "Warning: Could not detect OS version"
fi

echo ""
echo "This script will install:"
echo "  - OpenJDK 21 (full JDK with GUI support)"
echo "  - Maven 3.x"
echo "  - Python 3 with pip"
echo "  - X11 libraries"
echo "  - Xvfb (virtual framebuffer)"
echo ""

read -p "Continue with installation? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

echo ""
echo "[1/5] Updating package lists..."
sudo apt-get update -qq

echo ""
echo "[2/5] Installing Java 21 (full JDK with GUI support)..."
# Install full JDK (not headless) for GUI/X11 support
sudo apt-get install -y openjdk-21-jdk

# Verify Java installation
echo ""
echo "Java installation:"
java -version 2>&1 | head -1
echo "JAVA_HOME: ${JAVA_HOME:-not set}"

# Check for X11 AWT library
if [ -f "/usr/lib/jvm/java-21-openjdk-amd64/lib/libawt_xawt.so" ]; then
    echo "X11 AWT library: OK"
else
    echo "Warning: X11 AWT library not found. GUI may not work."
fi

echo ""
echo "[3/5] Installing Maven..."
sudo apt-get install -y maven

echo ""
echo "Maven installation:"
mvn -version 2>&1 | head -1

echo ""
echo "[4/5] Installing Python 3..."
sudo apt-get install -y python3 python3-pip

echo ""
echo "Python installation:"
python3 --version

echo ""
echo "[5/5] Installing X11 libraries and Xvfb..."
sudo apt-get install -y \
    xvfb \
    libx11-6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    x11-utils

echo ""
echo "============================================"
echo "Installation Complete!"
echo "============================================"
echo ""
echo "Next steps:"
echo ""
echo "1. Build GeckoCIRCUITS:"
echo "   mvn clean package assembly:single -DskipTests"
echo ""
echo "2. Run GeckoCIRCUITS (with WSLg display):"
echo "   DISPLAY=:0 java -Xmx3G -Dpolyglot.js.nashorn-compat=true \\"
echo "       -jar target/gecko-1.0-jar-with-dependencies.jar"
echo ""
echo "3. Run validation script:"
echo "   DISPLAY=:0 python3 resources/validate_circuits.py"
echo ""
echo "   Or with Xvfb (headless):"
echo "   xvfb-run python3 resources/validate_circuits.py"
echo ""
echo "4. Run tests:"
echo "   mvn test"
echo ""

# Check for WSLg display
echo "Checking display configuration..."
if [ -S "/tmp/.X11-unix/X0" ]; then
    echo "  WSLg display found at :0"
    echo "  You can use: DISPLAY=:0"
else
    echo "  No WSLg display found."
    echo "  Use Xvfb: xvfb-run <command>"
fi

echo ""
echo "Setup complete!"
