---
title: Download
description: Download GeckoCIRCUITS releases
---

# Download GeckoCIRCUITS

## Latest Release

[:material-download: Download GeckoCIRCUITS v1.0](https://github.com/geckocircuits/geckocircuits/releases/latest){ .md-button .md-button--primary }

## System Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| **OS** | Windows 10, Linux, macOS 10.15+ | Windows 11, Ubuntu 22.04+, macOS 12+ |
| **Java** | Java 21 | Java 21 (Temurin recommended) |
| **RAM** | 4 GB | 8 GB+ |
| **Disk** | 500 MB | 1 GB |
| **Display** | 1280x720 | 1920x1080+ |

## Platform Downloads

=== "Windows"

    **GeckoCIRCUITS-1.0-windows.zip**

    1. Download the Windows package
    2. Extract to desired location
    3. Run `run-gecko.bat`

    ```batch
    scripts\run-gecko.bat
    ```

=== "Linux"

    **GeckoCIRCUITS-1.0-linux.zip**

    1. Download the Linux package
    2. Extract: `unzip GeckoCIRCUITS-1.0-linux.zip`
    3. Make executable: `chmod +x scripts/run-gecko-linux.sh`
    4. Run: `./scripts/run-gecko-linux.sh`

    ```bash
    chmod +x scripts/run-gecko-linux.sh
    ./scripts/run-gecko-linux.sh
    ```

=== "macOS"

    **GeckoCIRCUITS-1.0-macos.zip**

    1. Download the macOS package
    2. Extract the archive
    3. Run `run-gecko-macos.sh`

    ```bash
    ./scripts/run-gecko-macos.sh
    ```

=== "WSL"

    **GeckoCIRCUITS-1.0-wsl.zip**

    1. Download the WSL package
    2. Extract in WSL filesystem
    3. Run setup: `./scripts/setup-wsl.sh`
    4. Run: `./scripts/run-gecko-wsl.sh`

## Examples Package

[:material-folder-download: Download Examples (GeckoCIRCUITS-1.0-examples.zip)](https://github.com/geckocircuits/geckocircuits/releases/latest){ .md-button }

Contains 100+ ready-to-run circuit files:

- Basic topologies (Buck, Boost, Flyback, Forward)
- Power supplies (LLC, DAB, PFC)
- Motor drives (BLDC, PMSM, Induction)
- Automotive (EV Charger, OBC, Traction)
- Thermal analysis examples

## Build from Source

```bash
# Clone repository
git clone https://github.com/geckocircuits/geckocircuits.git
cd geckocircuits

# Build with Maven
mvn clean package assembly:single -DskipTests

# Run
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar
```

## Previous Versions

| Version | Date | Notes |
|---------|------|-------|
| 1.0 | 2026-02 | Current release |

## Checksums

Verify your download:

```
SHA256 (GeckoCIRCUITS-1.0-windows.zip) = [checksum]
SHA256 (GeckoCIRCUITS-1.0-linux.zip) = [checksum]
SHA256 (GeckoCIRCUITS-1.0-macos.zip) = [checksum]
```

## License

GeckoCIRCUITS is open source software. See [LICENSE](https://github.com/geckocircuits/geckocircuits/blob/main/LICENSE) for details.
