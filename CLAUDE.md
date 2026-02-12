# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GeckoCIRCUITS is a Java 21 circuit simulator for power electronics. It supports multi-domain simulation (electrical, thermal, EMI) and integrates with MATLAB/Simulink via RMI or memory-mapped files.

## Build & Run Commands

```bash
# Build main app with dependencies (creates target/gecko-1.0-jar-with-dependencies.jar)
mvn clean package assembly:single

# Build skipping tests
mvn clean package assembly:single -DskipTests

# Run tests (main project only)
mvn test

# Run ALL modules (main + simulation-core + rest-api) via reactor
mvn -f pom-reactor.xml test

# Build a single sub-module
mvn -f pom-reactor.xml test -pl src/modules/gecko-simulation-core
mvn -f pom-reactor.xml test -pl src/modules/gecko-rest-api

# Run single test class
mvn test -Dtest=ClassName

# Run tests with coverage report (output: target/site/jacoco/index.html)
mvn clean test jacoco:report
```

## Running GeckoCIRCUITS

### Using Launcher Scripts (Recommended)

Platform-specific launcher scripts are provided in `scripts/`:

```bash
# Windows
scripts\run-gecko.bat
scripts\run-gecko.bat --hidpi              # For 4K displays
scripts\run-gecko.bat circuit.ipes         # Open circuit file

# Linux
./scripts/run-gecko-linux.sh
./scripts/run-gecko-linux.sh --hidpi
./scripts/run-gecko-linux.sh --headless    # For CI/testing with Xvfb

# macOS
./scripts/run-gecko-macos.sh
./scripts/run-gecko-macos.sh --hidpi       # For Retina displays

# WSL (Windows Subsystem for Linux)
./scripts/run-gecko-wsl.sh
./scripts/run-gecko-wsl.sh --hidpi
```

### Manual Execution

```bash
# Run application
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar

# Run with HiDPI support
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -Dsun.java2d.uiScale=2 -jar target/gecko-1.0-jar-with-dependencies.jar

# Load specific circuit file
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar path/to/file.ipes
```

## Building Distribution Packages

Distribution packages can be built for each platform:

```bash
# Build all distributions (Windows, Linux, macOS, WSL, Examples)
mvn clean package -Pdist-all -DskipTests

# Build single platform
mvn clean package -Pdist-windows -DskipTests
mvn clean package -Pdist-linux -DskipTests
mvn clean package -Pdist-macos -DskipTests
mvn clean package -Pdist-wsl -DskipTests
mvn clean package -Pdist-examples -DskipTests

# Or use the build script
./_build/build-distributions.sh          # Build all
./_build/build-distributions.sh windows  # Single platform
```

Output packages in `target/`:
- `GeckoCIRCUITS-1.0-windows.zip` - Windows distribution
- `GeckoCIRCUITS-1.0-linux.zip` - Linux distribution
- `GeckoCIRCUITS-1.0-macos.zip` - macOS distribution
- `GeckoCIRCUITS-1.0-wsl.zip` - WSL distribution (includes setup script)
- `GeckoCIRCUITS-1.0-examples.zip` - Example circuits and tutorials

## Architecture

### Entry Point & Operating Modes
- **GeckoSim** (`gecko.GeckoSim`) - Main class handling startup and operating mode selection
- Operating modes (defined in `OperatingMode` enum):
  - **STANDALONE** - Desktop GUI application (default)
  - **REMOTE** - RMI-based remote control for MATLAB/Octave integration
  - **MMF** - Memory-mapped file communication for high-performance data exchange
  - **SIMULINK** - Direct Simulink integration
  - **EXTERNAL** - External tool integration

### Key Package Structure

**`gecko.geckocircuits/`** - Main simulation engine:
- `allg/` - Global settings, file management, dialogs
- `circuit/` - Circuit components, terminals, netlist generation, matrix operations
- `control/` - 64+ calculator classes for control blocks (PI/PID, integration, gain, limiters)
- `datacontainer/` - Signal storage with optimized caching (IntegerMatrixCache, ShortMatrixCache)
- `math/` - Matrix operations, FFT utilities
- `newscope/` & `scope/` - Oscilloscope visualization
- `nativec/` - Native C integration layer

**`gecko.i18n/`** - Internationalization (968+ translation keys)

**`gecko.expressionscripting/`** - GraalVM JavaScript expression evaluation

### Multi-Module Structure (Reactor: `pom-reactor.xml`)
- **Main project** (`/`, `pom.xml`) - Full desktop application with Swing GUI
- **gecko-simulation-core** (`src/modules/gecko-simulation-core/`) - GUI-free simulation engine (137+ classes, 70% coverage enforced)
- **gecko-rest-api** (`src/modules/gecko-rest-api/`) - Spring Boot 3.2.1 REST API with OpenAPI/Swagger

### External Integration
- `GeckoRemoteInterface` - RMI interface for remote method calls
- `GeckoCustomMMF` - Memory-mapped file communication
- `GeckoRemoteRegistry` - RMI registry management

## File Formats

- `.ipes` - Circuit files (gzip-compressed)
- `.form` - NetBeans GUI Designer files (use NetBeans to edit)
- `.prp` - Properties configuration files

## Testing

Test categories:
- **Unit tests** - Calculator and component tests
- **Integration tests** - `ModelResultsTest` loads and simulates real circuit files
- **API tests** - `GeckoRemoteTest` validates remote interface consistency

Test mode is controlled by `GeckoSim._isTestingMode` flag.

## GUI Development

Swing dialogs use NetBeans GUI Designer `.form` files. To edit:
1. Open project in NetBeans
2. Shift+double-click `.form` file to open Design view
3. Edit components visually, save to regenerate Java code

## Key Dependencies

- GraalVM Polyglot 24.1.1 (JavaScript scripting - requires `-Dpolyglot.js.nashorn-compat=true`)
- JTransforms 2.4 (FFT operations)
- Apache Batik 1.7 (SVG generation)
- JNA 5.18.1 (native library access)
- Log4j2 2.24.3 (logging)

## Documentation Site

Live at: https://tinix84.github.io/GeckoCIRCUITS/ (deployed via gh-pages branch)

```bash
# Sync resources/ content (tutorials, examples, articles) into docs/
python scripts/sync-docs.py

# Generate API documentation report from Javadoc in gecko-simulation-core
python scripts/generate-api-docs.py

# Build docs site (catches broken links with --strict)
mkdocs build --strict

# Preview docs locally
mkdocs serve

# Deploy to GitHub Pages
mkdocs gh-deploy --force
```

**Important:** `docs/tutorials/index.md` and `docs/examples/index.md` are hand-maintained (not overwritten by sync-docs.py) because they use different path conventions than the source READMEs.

## Code Quality Tools

```bash
# SpotBugs analysis (0 bugs enforced)
mvn spotbugs:check

# Checkstyle (custom config: checkstyle.xml)
mvn checkstyle:check

# PMD (custom ruleset: pmd-ruleset.xml)
mvn pmd:check
```

### Static Analysis Status (2026-02-12)
| Tool | Config | Violations | Notes |
|------|--------|-----------|-------|
| SpotBugs | Default + 204 `@SuppressFBWarnings` | **0 bugs** | Clean |
| PMD | `pmd-ruleset.xml` (quickstart rules) | **3,444** | Code-style only, no bugs |
| Checkstyle | `checkstyle.xml` (150-char lines) | **4,661** | Down from 56,673 with default Sun config |

Third-party code (`com/intel/mkl/`) is excluded from both PMD and Checkstyle.

## Current Development Focus

### Strategic Direction (Dual-Track Approach)
The project maintains the desktop application while adding modern web accessibility:
- **Desktop** - Mature Swing GUI for power users, researchers, MATLAB/Simulink integration
- **REST API** (planned) - Spring Boot server for automation, cloud deployment, CI/CD pipelines
- **Shared Core** (planned) - `gecko-simulation-core` module to be extracted for both interfaces

### Active Initiatives
1. **Test Coverage Improvement** - JaCoCo coverage thresholds enforced for core packages (60%+ minimum)
2. **GUI-Free Core Extraction** - Decoupling computation classes from GUI singletons for `gecko-simulation-core`
3. **REST API Design** - API specification documented, implementation planned

### GUI-Free Validated Packages
These packages are confirmed GUI-free and safe for headless/API use:
- `circuit.matrix` (15 classes, 65% coverage) - MNA matrix stampers
- `circuit.netlist` (4 classes, 89% coverage) - Netlist building
- `circuit.simulation` (5 classes, 84% coverage) - Simulation engine
- `circuit.component` (86% coverage) - Component definitions
- `control.calculators` (64 classes, 81% coverage) - All control block calculators
- `datacontainer` (71% coverage) - Signal data storage
- `math` (7 classes, 81% coverage) - Matrix operations, LU decomposition

### GUI Decoupling Pattern: LossFileAccessor
The `circuit.losscalculation` package uses a `LossFileAccessor` interface to decouple `VerlustBerechnungDetailed` from `MainWindow` static access. The pattern:
- `LossFileAccessor` - Interface for file I/O operations (getFile, maintain, addFile, getOpenFileName)
- `MainWindowLossFileAccessor` - GUI adapter (delegates to `MainWindow._fileManager`)
- Injectable constructor enables headless testing with mock implementations
- This pattern can be replicated for other packages with `MainWindow` dependencies

### Architectural Boundaries
The `CorePackageValidationTest` enforces that core packages have no GUI imports (`java.awt`, `javax.swing`). Any violation fails the build.
JaCoCo coverage check (`mvn verify`) enforces 60%+ instruction coverage on core packages.

## Session Journals

Development journals are stored in `.claude/journals/` with detailed context:
- `STRATEGIC_ROADMAP_DUAL_TRACK.md` - Long-term architecture vision
- `ARCHITECTURE_ASSESSMENT_2026-01-27.md` - Current state analysis
- `CORE_API_BOUNDARY.md` - GUI-free package documentation
- `QUICK_REFERENCE.md` - Current session status and metrics
- `OPCODE_GUIDE.md` - Complete development guide

## Project Documents

Keep these documents updated after each sprint/push:
- **`PRD.md`** - Product requirements, sprint status, release history, success metrics
- **`ARCHITECTURE.md`** - System architecture, module structure, GUI-free boundary, integration points
- **`CLAUDE.md`** (this file) - Build commands, development context, session continuity

A PostToolUse hook in `.claude/settings.json` reminds to update these after `git commit` or `git push`.

## Recent Git Activity

Recent commits:
- `ea309a7` Update project docs after static analysis cleanup sprint
- `9ffedb0` Fix 183 Tier 3 PMD violations: empty blocks, unused code, stray semicolons
- `b8b8f1f` Strip trailing whitespace from 974 Java source files
- `50f62f7` Fix 565 PMD violations: UselessParentheses, UnnecessaryImport, UnnecessaryModifier
- `84388dd` Fix 694 UnnecessaryFullyQualifiedName PMD violations across 111 files
- `49da720` Add PMD and Checkstyle configuration files with third-party exclusions
- `7d91719` Update project docs after losscalculation GUI decoupling sprint

## Key Interfaces for Headless Operation

```java
// Circuit component stamping (MNA)
IMatrixStamper - stampMatrixA(), stampVectorB(), calculateCurrent()

// Solver configuration
SolverContext - SOLVER_BE (Backward Euler), SOLVER_TRZ (Trapezoidal), SOLVER_GS (Gear-Shichman)

// Component registry
StamperRegistry - getStamper(CircuitTyp)

// Loss file I/O abstraction (GUI-free)
LossFileAccessor - getFile(hash), maintain(file), addFile(file), getOpenFileName()
