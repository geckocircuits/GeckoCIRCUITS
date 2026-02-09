# GeckoCIRCUITS Product Requirements Document

## Overview

GeckoCIRCUITS is an open-source circuit simulator for power electronics, originally developed at ETH Zurich. It provides multi-domain simulation capabilities including electrical, thermal, and EMI analysis, with integration support for MATLAB/Simulink.

## Target Users

1. **Power Electronics Engineers** - Design and simulate converters, inverters, rectifiers
2. **Researchers** - Academic research in power electronics topologies
3. **Educators** - Teaching power electronics concepts with interactive simulations
4. **Students** - Learning circuit behavior through hands-on simulation

## Supported Platforms

| Platform | Distribution | Launcher Script |
|----------|-------------|-----------------|
| Windows 10/11 | `GeckoCIRCUITS-x.x-windows.zip` | `run-gecko.bat` |
| Linux (Ubuntu, Fedora, Arch) | `GeckoCIRCUITS-x.x-linux.zip` | `run-gecko-linux.sh` |
| macOS (Intel/Apple Silicon) | `GeckoCIRCUITS-x.x-macos.zip` | `run-gecko-macos.sh` |
| WSL2 (Windows 11 WSLg) | `GeckoCIRCUITS-x.x-wsl.zip` | `run-gecko-wsl.sh` |

## System Requirements

### Minimum Requirements
- **Java**: OpenJDK 21 or later
- **Memory**: 4 GB RAM (8 GB recommended)
- **Storage**: 200 MB for application, additional space for circuit files
- **Display**: 1280x720 resolution

### HiDPI Support
- 4K/Retina displays supported via `--hidpi` flag
- Automatic scaling with `-Dsun.java2d.uiScale=2`

## Core Features

### 1. Circuit Simulation
- **Electrical domain**: Passive components (R, L, C), semiconductors (diodes, MOSFETs, IGBTs), transformers
- **Thermal domain**: Junction temperature calculation, thermal networks
- **EMI domain**: EMI filter design and analysis
- **Solver methods**: Backward Euler, Trapezoidal, Gear-Shichman

### 2. Control System Modeling
- 64+ control blocks (PI/PID controllers, integrators, differentiators, limiters)
- Signal processing (FFT, filters, math operations)
- PWM generation (carrier-based, space vector)
- State machines and logic blocks

### 3. Visualization
- Real-time oscilloscope with multiple channels
- FFT spectrum analyzer
- XY plots and Bode plots
- Export to CSV, images, SVG

### 4. Scripting & Automation
- GeckoSCRIPT for batch simulations
- JavaScript expression evaluation (GraalVM)
- Parameter sweeps and optimization
- Custom Java blocks

### 5. External Integration
- **MATLAB/Simulink**: RMI-based remote control, memory-mapped file communication
- **REST API**: Headless operation for automation and cloud deployment
- **Native C**: JNI integration for custom components

## Distribution Packages

### Platform Packages
Each platform package includes:
- `GeckoCIRCUITS.jar` - Main application (fat JAR with all dependencies)
- Platform-specific launcher script with proper JVM options
- README with quick start instructions

### Examples Package
Separate download containing:
- **Tutorials** (organized by topic):
  - `1xx_getting_started/` - First simulation, basic circuits, PWM basics
  - `2xx_dcdc_converters/` - Buck, Boost, Buck-Boost converters
  - `3xx_acdc_rectifiers/` - Diode rectifiers, PFC, Vienna rectifier
  - `4xx_dcac_inverters/` - Single-phase and three-phase inverters
  - `5xx_thermal_simulation/` - Loss calculation, junction temperature
  - `6xx_emi_emc/` - EMI filter design
  - `7xx_scripting_automation/` - GeckoSCRIPT, MATLAB integration
  - `8xx_advanced_topics/` - Motor drives, matrix converters, optimization
- **Application examples** - Real-world circuit designs
- **Articles** - Technical articles with supporting circuit files

## Build System

### Development Build
```bash
mvn clean package assembly:single -DskipTests
```

### Distribution Build
```bash
# All platforms
mvn clean package -Pdist-all -DskipTests

# Single platform
mvn clean package -Pdist-windows -DskipTests
mvn clean package -Pdist-linux -DskipTests
mvn clean package -Pdist-macos -DskipTests
mvn clean package -Pdist-wsl -DskipTests
mvn clean package -Pdist-examples -DskipTests
```

### Build Scripts
- `_build/build-distributions.sh` - Linux/macOS/WSL build script
- `_build/build-distributions.bat` - Windows build script
- `_build/assembly/` - Maven assembly descriptors

## Architecture

### Module Structure
```
GeckoCIRCUITS/
├── src/main/java/           # Main application source
├── src/test/java/           # Test sources
├── src/modules/
│   ├── gecko-simulation-core/   # GUI-free simulation engine
│   └── gecko-rest-api/          # REST API module
├── scripts/                 # Platform launcher scripts
├── _build/                  # Distribution build configuration
│   ├── assembly/            # Maven assembly descriptors
│   └── README-*.txt         # Platform-specific READMEs
└── resources/               # Example circuits and documentation
```

### Key Components
- **GeckoSim** - Main entry point, mode selection
- **MainWindow** - Primary Swing GUI
- **SchematicEditor** - Circuit diagram editor
- **SimulationRunner** - Simulation execution engine
- **DataContainer** - Signal storage and caching

## Quality Assurance

### Testing
- Unit tests for calculators and core components
- Integration tests with real circuit files
- API consistency tests for remote interface
- GUI-free validation tests for core packages

### Code Quality
```bash
mvn spotbugs:check    # Static analysis
mvn checkstyle:check  # Style checking
mvn pmd:check         # Code quality rules
```

### Coverage Targets
- Core packages: 70%+ line coverage
- Matrix stampers: 85%+
- Netlist building: 99%+

## Roadmap

### Current Focus
1. GUI-free core extraction for headless operation
2. REST API development for cloud deployment
3. Test coverage improvement
4. Cross-platform distribution packaging

### Future Enhancements
- Web-based simulation interface
- Cloud simulation service
- Enhanced MATLAB/Python integration
- Component library expansion

## File Formats

| Extension | Description |
|-----------|-------------|
| `.ipes` | Circuit schematic (gzip-compressed XML) |
| `.scl` | GeckoSCRIPT source files |
| `.gmd` | GeckoMAGNETICS design files |
| `.gmw` | GeckoMAGNETICS waveform files |
| `.prp` | Application properties |

## Support & Resources

- **Documentation**: See `resources/tutorials/` and `resources/articles/`
- **Issue Tracking**: GitHub Issues
- **Source Code**: GitHub repository

## License

Open-source software. See LICENSE file for details.
