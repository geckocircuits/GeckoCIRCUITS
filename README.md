# GeckoCIRCUITS

**Open-source circuit simulator for power electronics** with multi-domain simulation: electrical, thermal, and EMI -- all in one tool.

Originally developed at ETH Zurich's Power Electronic Systems Laboratory by Andreas Muesing, Andrija Stupar, and Uwe Drofenik.

## Key Features

- **Multi-domain simulation** -- electrical + thermal + EMI in a single environment
- **Fast simulation engine** -- optimized for power electronics (MNA-based solvers: Backward Euler, Trapezoidal, Gear-Shichman)
- **Visual circuit editor** -- drag-and-drop schematic capture with built-in oscilloscope
- **64+ control blocks** -- PI/PID, integrators, limiters, lookup tables, state machines, and more
- **Thermal analysis** -- junction temperature, loss calculation, heatsink design
- **EMI filter design** -- common-mode/differential-mode filter analysis
- **MATLAB/Simulink integration** -- remote control via RMI or memory-mapped files
- **Scriptable** -- built-in JavaScript (GraalVM) and Java block scripting
- **Cross-platform** -- runs on Windows, Linux, and macOS

## Quick Start

### Prerequisites

- **Java 21** or later ([Adoptium Temurin](https://adoptium.net/) recommended)

### Download and Run

1. Download `gecko-1.0-jar-with-dependencies.jar` from the [Releases](https://github.com/tinix84/GeckoCIRCUITS/releases) page
2. Run:
   ```bash
   java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar gecko-1.0-jar-with-dependencies.jar
   ```
3. Open an example circuit from `File > Open` (see [Examples](#examples) below)
4. Click the **Simulate** button (or press F5)

### Platform Launcher Scripts

Convenience scripts are provided in `scripts/`:

| Platform | Command |
|----------|---------|
| Windows | `scripts\run-gecko.bat` |
| Linux | `./scripts/run-gecko-linux.sh` |
| macOS | `./scripts/run-gecko-macos.sh` |
| WSL | `./scripts/run-gecko-wsl.sh` |

Add `--hidpi` for high-DPI / Retina displays.

## Examples

The `resources/` directory contains ready-to-simulate circuits:

| Example | Description | Location |
|---------|-------------|----------|
| Buck Converter | Basic DC-DC step-down | `resources/tutorials/2xx_dcdc_converters/201_buck_converter/` |
| Boost Converter | DC-DC step-up | `resources/tutorials/2xx_dcdc_converters/202_boost_converter/` |
| Three-Phase Inverter | DC-AC conversion | `resources/tutorials/4xx_dcac_inverters/402_three_phase_inverter/` |
| Thermal Simulation | Junction temperature analysis | `resources/tutorials/5xx_thermal_simulation/502_junction_temperature/` |
| PFC Boost | Power factor correction | `resources/tutorials/3xx_acdc_rectifiers/302_pfc_basics/` |
| Vienna Rectifier | Three-phase AC-DC | `resources/tutorials/3xx_acdc_rectifiers/303_vienna_rectifier/` |
| EMI Filters | CM/DM filter design | `resources/tutorials/6xx_emi_emc/602_cm_dm_filters/` |

## Build from Source

```bash
# Clone
git clone https://github.com/tinix84/GeckoCIRCUITS.git
cd GeckoCIRCUITS

# Build (produces target/gecko-1.0-jar-with-dependencies.jar)
mvn clean package assembly:single -DskipTests

# Run
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar

# Run tests
mvn test
```

Requires: Java 21 JDK + Maven 3.6+

## Logging Configuration

GeckoCIRCUITS uses **Apache Log4j2** for structured console and file logging.

### Output Locations
- **Console**: Logs are outputted to stdout.
- **File**: Logs are saved in a rolling file at `logs/gecko.log` (automatically rolled over at 10 MB, up to 10 archives / 100 MB max).

### Configuring Log Levels at Startup
You can configure the log level dynamically using the `gecko.log.level` system property:

| Level | VM Option / Command | Description |
|-------|---------------------|-------------|
| **Disabled** | `-Dgecko.log.level=OFF` | Silences all logging output |
| **Error Only** | `-Dgecko.log.level=ERROR` | Logs only error events |
| **Standard (Default)** | `-Dgecko.log.level=INFO` | Logs standard informational events |
| **Full / Debug** | `-Dgecko.log.level=TRACE` | Logs verbose debug and trace info |

Example running the JAR with error-only logging:
```bash
java -Dgecko.log.level=ERROR -jar target/gecko-1.0-jar-with-dependencies.jar
```

To redirect the logs to a custom directory, set `-Dgecko.log.dir=/path/to/logs`.

## Architecture

```
GeckoCIRCUITS
├── Simulation Engine (MNA matrix, LU decomposition, 3 solver types)
├── Circuit Components (R, L, C, diodes, MOSFETs, IGBTs, transformers, ...)
├── Control Blocks (64+ calculators: PI, PID, integrator, gain, limiter, ...)
├── Thermal Domain (loss models, thermal networks, heatsink simulation)
├── EMI Domain (CISPR filters, CM/DM analysis)
├── Visualization (oscilloscope, FFT, THD, RMS analysis)
└── Integration (RMI for MATLAB, memory-mapped files, JavaScript scripting)
```

## Documentation

Full documentation is available at **[tinix84.github.io/GeckoCIRCUITS](https://tinix84.github.io/GeckoCIRCUITS/)**:

- [Getting Started Guide](https://tinix84.github.io/GeckoCIRCUITS/getting-started/)
- [Tutorials](https://tinix84.github.io/GeckoCIRCUITS/tutorials/) (DC-DC, AC-DC, DC-AC, thermal, scripting)
- [Examples Library](https://tinix84.github.io/GeckoCIRCUITS/examples/) (125+ circuit files)
- [API Reference](https://tinix84.github.io/GeckoCIRCUITS/api/) (GeckoSCRIPT, RMI, REST)
- [Developer Guide](https://tinix84.github.io/GeckoCIRCUITS/resources/developer-guide/)

## Contributing

Contributions are welcome! See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide.

```bash
# Run tests before submitting
mvn test

# Check code quality
mvn spotbugs:check
```

## License

This project is dual-licensed:

- **Open Source**: [GNU General Public License v3.0](LICENSE) -- free for academic, research, and open-source use
- **Commercial**: Contact for commercial licensing terms

Copyright (c) ETH Zurich, Power Electronic Systems Laboratory. Originally developed by Andreas Muesing, Andrija Stupar, and Uwe Drofenik.

## Links

- [Documentation](https://tinix84.github.io/GeckoCIRCUITS/)
- [Original SourceForge project](https://sourceforge.net/projects/geckocircuits/)
- [Technokrat contributions](https://github.com/technokrat/gecko) (HiDPI support)
