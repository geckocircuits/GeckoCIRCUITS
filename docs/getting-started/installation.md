---
title: Installation
description: Install GeckoCIRCUITS on Windows, Linux, or macOS
---

# Installation Guide

GeckoCIRCUITS runs on Windows, Linux, and macOS. Follow the instructions for your operating system.

## Prerequisites

### Java 21

GeckoCIRCUITS requires Java 21 or later.

=== "Windows"

    1. Download [Eclipse Temurin JDK 21](https://adoptium.net/temurin/releases/?version=21)
    2. Run the installer
    3. Verify installation:
    ```batch
    java -version
    ```

=== "Linux (Ubuntu/Debian)"

    ```bash
    sudo apt update
    sudo apt install openjdk-21-jdk
    java -version
    ```

=== "Linux (Fedora/RHEL)"

    ```bash
    sudo dnf install java-21-openjdk-devel
    java -version
    ```

=== "macOS"

    ```bash
    # Using Homebrew
    brew install openjdk@21

    # Add to PATH
    echo 'export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"' >> ~/.zshrc
    source ~/.zshrc

    java -version
    ```

!!! note "Expected Output"
    ```
    openjdk version "21.0.x" 2024-xx-xx
    OpenJDK Runtime Environment (build 21.0.x+xx)
    OpenJDK 64-Bit Server VM (build 21.0.x+xx, mixed mode)
    ```

## Installation Methods

### Method 1: Download Release (Recommended)

1. Go to [Releases](https://github.com/geckocircuits/geckocircuits/releases)
2. Download the latest `GeckoCIRCUITS-x.x-<platform>.zip`
3. Extract to your desired location
4. Run the launcher script

### Method 2: Build from Source

```bash
# Clone repository
git clone https://github.com/geckocircuits/geckocircuits.git
cd geckocircuits

# Build with Maven
mvn clean package assembly:single -DskipTests

# The JAR is created at:
# target/gecko-1.0-jar-with-dependencies.jar
```

!!! tip "Maven Installation"
    If you don't have Maven installed:

    === "Windows"
        Download from [maven.apache.org](https://maven.apache.org/download.cgi) and add to PATH

    === "Linux"
        ```bash
        sudo apt install maven  # Debian/Ubuntu
        sudo dnf install maven  # Fedora
        ```

    === "macOS"
        ```bash
        brew install maven
        ```

## Running GeckoCIRCUITS

### Using Launcher Scripts

=== "Windows"

    ```batch
    scripts\run-gecko.bat

    :: With HiDPI support (4K displays)
    scripts\run-gecko.bat --hidpi

    :: Open specific circuit
    scripts\run-gecko.bat path\to\circuit.ipes
    ```

=== "Linux"

    ```bash
    ./scripts/run-gecko-linux.sh

    # With HiDPI support
    ./scripts/run-gecko-linux.sh --hidpi

    # Headless mode (for CI/servers)
    ./scripts/run-gecko-linux.sh --headless
    ```

=== "macOS"

    ```bash
    ./scripts/run-gecko-macos.sh

    # With Retina support
    ./scripts/run-gecko-macos.sh --hidpi
    ```

=== "WSL"

    ```bash
    ./scripts/run-gecko-wsl.sh

    # First time: run setup
    ./scripts/setup-wsl.sh
    ```

### Direct Java Execution

```bash
java -Xmx3G \
     -Dpolyglot.js.nashorn-compat=true \
     -jar target/gecko-1.0-jar-with-dependencies.jar
```

!!! info "JVM Options"
    - `-Xmx3G` - Allocate 3GB memory (adjust based on your system)
    - `-Dpolyglot.js.nashorn-compat=true` - Enable JavaScript scripting
    - `-Dsun.java2d.uiScale=2` - HiDPI scaling (add for 4K displays)

## Verifying Installation

1. Launch GeckoCIRCUITS
2. Go to **Help > About** to verify version
3. Open a sample circuit: **File > Open > resources/tutorials/1xx_getting_started/101_first_simulation/ex_1.ipes**
4. Click **Run** (or press ++f5++)
5. If waveforms appear in the scope, installation is successful!

## Troubleshooting

### Common Issues

| Problem | Solution |
|---------|----------|
| "Java not found" | Ensure Java 21 is installed and in PATH |
| Application won't start | Check Java version: `java -version` |
| Blank window on Linux | Install `libxrender1 libxtst6` |
| Slow on 4K display | Add `--hidpi` flag or `-Dsun.java2d.uiScale=2` |
| Out of memory | Increase `-Xmx` value (e.g., `-Xmx4G`) |

### WSL-Specific Issues

```bash
# Install X server support
sudo apt install x11-apps

# Set DISPLAY variable
export DISPLAY=:0

# Or for WSLg (Windows 11)
export DISPLAY=:0
export WAYLAND_DISPLAY=wayland-0
```

## Next Steps

- [Quick Start](quickstart.md) - Run your first simulation
- [User Interface](interface.md) - Learn the application layout
