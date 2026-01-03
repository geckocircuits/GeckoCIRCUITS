# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GeckoCIRCUITS is a fast circuit simulator optimized for power electronics applications. It's written in Java and provides multi-domain simulation capabilities including thermo-electrical simulation, EMI filter design, and thermal design tools. The application can run standalone, as an applet, or integrate with external tools like MATLAB/Simulink.

## Build Commands

### Building the project
```bash
mvn clean package assembly:single
```

This creates `target/gecko-1.0-jar-with-dependencies.jar` with all dependencies included.

### Running tests
```bash
mvn test
```

Note: All 159 tests now pass successfully (0 failures, 0 skipped).

### Quick build (skip tests)
```bash
mvn clean package assembly:single -DskipTests
```

## Running the Application

### Standard execution
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar
```

### HiDPI displays
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -Dsun.java2d.uiScale=2 -jar target/gecko-1.0-jar-with-dependencies.jar
```

### Remote access mode
```bash
java -jar gecko-1.0-jar-with-dependencies.jar -p [port_number]
```

### Memory-mapped file access
```bash
java -jar gecko-1.0-jar-with-dependencies.jar -mm [filename] [filesize]
```

### Loading a circuit file
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar path/to/file.ipes
```

## VSCode Development

The project includes VSCode configuration for easy development:
- **Tasks**: Maven build and test tasks available via `Ctrl+Shift+B` or Task menu
- **Launch**: Multiple run configurations (standard, HiDPI, with file, etc.)
- **Terminal**: Git Bash is configured as the default terminal
- See [.vscode/README.md](.vscode/README.md) for complete VSCode guide

## Architecture

### Main Entry Point
- **GeckoSim.java** (`ch.technokrat.gecko.GeckoSim`): Main class that initializes the application
  - Handles different operating modes: STANDALONE, REMOTE, MMF (Memory-Mapped File), SIMULINK, EXTERNAL
  - Manages memory settings and JVM restart if more memory is required
  - Initializes the main window (`Fenster`)

### Key Package Structure

- **ch.technokrat.gecko**: Core application classes
  - Remote interface classes (`GeckoRemote*`) for external tool integration
  - Memory-mapped file support (`GeckoMemoryMappedFile`, `GeckoCustomMMF`)
  - Control calculation interfaces

- **ch.technokrat.gecko.geckocircuits**: Main simulation components
  - `allg/`: General classes (dialogs, file management, global settings)
  - `circuit/`: Circuit sheet, components, terminals, couplings
  - `control/`: Control blocks and systems
  - `datacontainer/`: Data storage and management
  - `math/`: Mathematical utilities
  - `newscope/` & `scope/`: Oscilloscope/visualization
  - `nativec/`: Native C integration

- **ch.technokrat.gecko.geckoscript**: Scripting support

- **ch.technokrat.gecko.i18n**: Internationalization

- **ch.technokrat.expressionscripting**: Expression and JavaScript support

### Operating Modes

The simulator supports multiple operating modes defined in `OperatingMode`:
- **STANDALONE**: Normal desktop application
- **REMOTE**: Remote access via RMI (for MATLAB integration)
- **MMF**: Memory-mapped file communication
- **SIMULINK**: Integration with MATLAB Simulink
- **EXTERNAL**: External tool integration

### File Formats

- `.ipes`: GeckoCIRCUITS circuit files (compressed GZIP format)
- Circuit files in `resources/` directory provide examples of various power electronic topologies

### External Integration

GeckoCIRCUITS can be controlled from external tools (particularly MATLAB/Simulink):
- **Remote interface**: Uses Java RMI for method calls over network
- **Memory-mapped files**: Shared memory communication for high-performance data exchange
- See `GeckoRemoteRegistry`, `GeckoCustomRemote`, `GeckoCustomMMF` classes

### Dependencies

Key dependencies (see pom.xml):
- **GraalVM JavaScript engine** 23.1.0 (for scripting support, requires `-Dpolyglot.js.nashorn-compat=true`)
- **JTransforms** 2.4 (FFT operations)
- **Apache Batik** 1.7 (SVG generation)
- **SyntaxPane** 1.2.0 (code editor)
- **JNA** 5.14.0 (native access)
- **Log4j** 1.2.17 (logging)
- **JUnit** 4.12 (testing)

## Development Notes

### Java Version
- **Required: JDK 21** (project is compiled for Java 21)
- Maven compiler configuration: source/target 21
- GraalVM JavaScript engine for scripting support (requires `-Dpolyglot.js.nashorn-compat=true`)

### Property Files
- Default properties: `/defaultProperties.prp` (in JAR)
- User properties: `GeckoProperties.prp` (in user's local app data directory)
- Properties include memory settings, recent files, and application configuration

### Memory Management
- Default memory requirement can be configured in properties
- `JavaMemoryRestart` class handles automatic JVM restart with increased memory if needed
- Memory settings are critical for large simulations

### GUI Framework
- Swing-based GUI
- Main window class: `Fenster`
- Custom file chooser: `GeckoFileChooser`
- Supports both windowed and applet modes

### Circuit Examples
The `resources/` directory contains extensive examples:
- DC/DC converters (buck, boost, buck-boost, Cuk, SEPIC, flyback, etc.)
- AC/DC rectifiers (Vienna, Swiss, three-phase VSR, PFC)
- DC/AC inverters (single-phase, three-phase)
- AC/AC converters (matrix converters)
- Located in subdirectories like `Education_ETHZ/`, `Topologies/`, `education_www.ipes.ethz.ch/`

## Important Considerations

### Platform Compatibility
- Cross-platform: Windows, Linux, Solaris
- Special handling for Linux window managers (GNOME, MATE)
- HiDPI support available via system property

### Testing
- JUnit 4.12 tests in `src/test/`
- All 159 tests now pass successfully (0 failures, 0 skipped)
- Test mode controlled by `GeckoSim._isTestingMode` flag
- Run tests via VSCode task (`Ctrl+Shift+P` → "Run Test Task") or `mvn test`

**Test Categories:**
1. **Unit tests**: Calculator and component tests
2. **Integration tests**: ModelResultsTest - loads and simulates real circuit files
3. **API tests**: GeckoRemoteTest - verifies GeckoRemote API consistency

**GeckoRemoteTest Details:**
- Validates that `GeckoRemote` wrapper methods match `GeckoRemoteInterface` interface
- Ensures all required remote API methods exist with correct signatures
- Critical for MATLAB/Octave integration and external tool access
- Uses GeckoRemoteIntWithoutExc interface (internal proxy, no exception declarations)

### Licensing
- GPLv3 for open source use
- Commercial license available from Gecko-Research GmbH
- See LICENSE and AUTHORS.txt files
