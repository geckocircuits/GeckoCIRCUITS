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

# Build Docker image for REST API
./scripts/build-docker.sh

# Run REST API container
docker-compose up -d
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

### Running REST API via Docker

The `gecko-rest-api` module can be deployed as a Docker container:

```bash
# Quick start with docker-compose
docker-compose up -d

# Build Docker image manually
./scripts/build-docker.sh

# Run container with custom port mapping
docker run -p 8080:8080 gecko-rest-api:latest

# Stop container
docker-compose down
```

See `scripts/build-docker.sh` and `scripts/run-docker.sh` for advanced options.

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
- **Main project** (`/`, `pom.xml`) - Full desktop application with Swing GUI (5,373 tests)
- **gecko-simulation-core** (`src/modules/gecko-simulation-core/`) - GUI-free simulation engine (192 classes, 1,809 tests, 30%+ coverage enforced)
- **gecko-rest-api (`src/modules/gecko-rest-api/`) - Spring Boot 3.2.1 REST API with OpenAPI/Swagger (224 tests, 32 endpoints live, Docker packaging available, production-ready)

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

The `gecko-simulation-core` module contains 35 test files with 1,122 test cases covering the GUI-free simulation engine.

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

### Static Analysis Status (2026-02-17)
| Tool | Config | Violations | Notes |
|------|--------|-----------|-------|
| SpotBugs | Default + 204 `@SuppressFBWarnings` | **0 bugs** | Clean |
| PMD | `pmd-ruleset.xml` (quickstart rules, 10 excluded rules, allowCommentedBlocks) | **496** ✅ | **<500 TARGET ACHIEVED!** Down 86% from original 3,443. Maintained across v2.18.0 |
| Checkstyle | `checkstyle.xml` (150-char lines) | **4,632** | Down from 56,673 with default Sun config |

Third-party code (`com/intel/mkl/`) is excluded from both PMD and Checkstyle.

**PMD Excluded Rules**: EmptyCatchBlock, LocalVariableNamingConventions, MethodNamingConventions, FormalParameterNamingConventions, SingularField, OneDeclarationPerLine, FinalParameterInAbstractMethod, GuardLogStatement, LiteralsFirstInComparisons, UnnecessaryImport (false positives)

## Current Development Focus

### Strategic Direction (Dual-Track Approach)
The project maintains the desktop application while adding modern web accessibility:
- **Desktop** - Mature Swing GUI for power users, researchers, MATLAB/Simulink integration
- **REST API** (in progress) - Spring Boot 3.2.1 server for automation, cloud deployment, CI/CD pipelines
- **Shared Core** ✅ - `gecko-simulation-core` module extracted (183 classes, 1,686 tests)

### Active Initiatives
1. **Real Solver Integration (Sprint 6)** - ✅ COMPLETE: Extracted SimulationsKern logic to core module (MatrixSolver, ComponentCurrentCalculator, InitialConditionSolver, DomainCoupler) with 360+ tests
2. **Test Coverage Improvement** - JaCoCo coverage thresholds enforced for core packages (30%+ minimum, 1,809 tests in core)
3. **REST API v3.0.0 Complete** — 32 endpoints, 224 tests, security, WebSocket, batch, analysis

### GUI-Free Validated Packages
These packages are confirmed GUI-free and safe for headless/API use:
- `circuit.matrix` (15 classes) - MNA matrix stampers
- `circuit.netlist` (6 classes) - Netlist building (CircuitNetlist, NetlistBuilder) ✨ SPRINT 6
- `circuit.simulation` (13 classes) - Simulation engine + solver + progress tracking ✨ SPRINT 6
- `circuit.simulation.solver` - Real solver integration (MatrixSolver, ComponentCurrentCalculator, InitialConditionSolver, DomainCoupler) ✨ SPRINT 6 NEW
- `circuit.terminal` (3 classes, 138 tests) - Connection path routing and validation
- `circuit.component` (3 classes, 206 tests) - Parameter and terminal registries
- `circuit` TokenMap (41 tests) - Circuit file parsing for .ipes files (migrated to gecko.core.circuit)
- `circuit` ComponentIdentifiable (interface) - Component identification for deserialization
- `circuit.losscalculation` (14 classes, 180+ tests) - Power electronics loss calculation (Sprint 4b)
- `control.calculators` (71 classes) - All control block calculators
- `datacontainer` (11 classes) - Signal data storage with optimized caching
- `math` (7 classes) - Matrix operations, LU decomposition, FFT
- `nativec` (7 classes, 46 tests) - Native C/C++ library integration via JNI
- `signal` (3 classes, 29 tests) - Signal analysis utilities (CharacteristicsCalculator, FourierGUIless, Cispr16Fft)
- `io` SerializationUtils - ASCII format serialization for .ipes files
- `i18n` SelectableLanguages (43 languages) - Internationalization support
- `allg` (10 classes, 90+ tests) - GeckoFile, ExternalStorageConverter, UserParameterCore, SolverType, OperatingMode, LaunchBrowser, GlobalFilePathes, CircuitFileConstants (Sprint 4b)
- `core` GeckoRuntimeException - Custom runtime exception

### GUI Decoupling Patterns

**LossFileAccessor Pattern:**
The `circuit.losscalculation` package uses a `LossFileAccessor` interface to decouple `VerlustBerechnungDetailed` from `MainWindow` static access:
- `LossFileAccessor` - Interface for file I/O operations (getFile, maintain, addFile, getOpenFileName)
- `MainWindowLossFileAccessor` - GUI adapter (delegates to `MainWindow._fileManager`)
- Injectable constructor enables headless testing with mock implementations

**UserParameterCore Pattern (Sprint 4b Phase 2B):**
The `allg` package uses `UserParameterCore` interface to abstract GUI-enabled parameter classes:
- `UserParameterCore<T>` - Interface with 9 essential methods (getValue, setValueWithoutUndo, readFromTokenMap, etc.)
- `UserParameterCoreImpl<T>` - Headless implementation with Builder pattern for core module
- `UserParameterGUIAdapter<T>` - Adapter wrapping GUI `UserParameter` for backward compatibility
- Enables loss curves and other data models to work in headless environments

**GeckoFile Pattern:**
The `allg` package uses `ExternalStorageConverter` interface to abstract GUI dialogs for external file storage:
- `ExternalStorageConverter` - Interface for prompting user for external file path
- `DialogExternalStorageConverter` - GUI adapter (delegates to `DialogMakeExternal`)
- Default constructor uses reflection fallback for backward compatibility
- Injectable constructor accepts custom converter for headless/API use

These patterns can be replicated for other packages with GUI dependencies.

### Architectural Boundaries
The `CorePackageValidationTest` enforces that core packages have no GUI imports (`java.awt`, `javax.swing`). Any violation fails the build.
JaCoCo coverage check (`mvn verify`) enforces 30%+ instruction coverage on core packages.

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
- **`RELEASE_PLAN.md`** - Release strategy, version roadmap, candidate commits for tagging
- **`CLAUDE.md`** (this file) - Build commands, development context, session continuity

A PostToolUse hook in `.claude/settings.json` reminds to update these after `git commit` or `git push`.

## Recent Git Activity

Recent commits (v2.18.0 Sprint 6 - Headless Simulation Engine):
- `2742d869` **v2.18.0: Real solver integration complete (Phase 6)**
  - Domain coupling (Electrical ↔ Control ↔ Thermal)
  - API enhancements (SimulationRequest/Response with solver type, progress details)
  - All 7,315 tests passing (1,809 core + 5,373 main + 133 API)
  - Commits: c13ea573, 5f51ddb0, 9549fe80, 91cd6dd3, 2742d869
- `91cd6dd3` Real solver integration - Replace HeadlessSimulationEngine placeholder
  - buildMatrixA → buildVectorB → solve → calculateComponentCurrents → updateNodePotentials
  - ~100 integration tests validating solver accuracy
- `9549fe80` Phases 2-3: Matrix solver + netlist building migration to core
  - MatrixSolver: MNA stamping, LU decomposition, BE/TRZ/GS history
  - CircuitNetlist: topology, parameters, magnetic couplings
  - NetlistBuilder: factory pattern
  - ControlNetlist: headless control execution
  - ~90 tests for netlist validation
- `5f51ddb0` Phase 1 test infrastructure: RLC circuits, validation suite, benchmarks
- `c13ea573` Phase 1: Architecture preparation (SimulationProgress, HeadlessSimulationEngine enhancements)
- `1e4eaa0a` **Achieve <500 PMD violations target: 823 → 496 (-40%)**
  - CompareObjectsWithEquals (31): Use .equals() for object comparisons
  - CloseResource (35): All resource leaks fixed with try-with-resources
  - IdenticalCatchBranches (22): Merged to multi-catch statements
  - **6 violation categories eliminated to 0: ForLoopCanBeForeach, PreserveStackTrace, SimplifyBooleanReturns, CompareObjectsWithEquals, CloseResource, IdenticalCatchBranches**
- `26679691` Complete ForLoopCanBeForeach cleanup: all 94 violations fixed
  - Batch 3-6: control (16), datacontainer/nativec (3), newscope (13), scope/geckoscript (25)
  - Modernized all traditional for-loops to enhanced for-each syntax
  - 29 files modified, 100% ForLoopCanBeForeach violations eliminated
  - PMD violations: 677 → 583 (-29%)
- `538d025f, 1e42c450, c3668e7c` Fix GitHub Actions CI workflow
  - Use reactor build with install to populate .m2 repository
  - Add missing imports for CircuitSourceType and SourceType tests
  - CI now passing: all modules build and test successfully
- `a600bf1e` Fix 55 high-value PMD violations: resource leaks and exception handling
  - CloseResource (17): Fixed resource leaks with try-with-resources pattern
  - PreserveStackTrace (38): All exception chains now preserve stack traces
  - Files: GeckoMemoryMappedFile, Matrix, TechFormat, GeckoRemote, and 22 others
  - Impact: Prevents file descriptor leaks, improves debugging significantly
- `8057c904` Fix 59 PMD violations and exclude 116 false positives
  - UnnecessaryImport (116): Excluded false positives in pmd-ruleset.xml
  - SimplifyBooleanReturns (29): All boolean return logic simplified
  - ForLoopCanBeForeach (30): Modernized loops in 17 files (57 remaining)
  - PMD violations: 823 → 694 (-15.7%)
- `682318d3` Update project documentation after PMD violation fixes
  - Updated CLAUDE.md, PRD.md, ARCHITECTURE.md with static analysis status
- `f3ac9b01` Fix CI workflow jacoco:report POM specification
  - Explicitly specify `-f pom.xml` for jacoco report after reactor build
  - Prevents module confusion in GitHub Actions workflow
- `2b3add3c` Fix 38 ReturnEmptyCollectionRatherThanNull PMD violations
  - Replaced `return null;` with empty collections across 11 files
  - Affected: GeckoRemoteMMFObject (22), GeckoExternal (2), AbstractGeckoCustom (3), ComponentPositioner (2), and 7 others
  - PMD violations reduced from 861 to 823 (code-style only, no bug-prevention issues remaining)
- `41fe6900` Sprint 5 Phase 1: Implement loss calculation REST endpoints
  - 3 endpoints: POST /api/v1/loss/{switching,conduction,detailed}
  - Uses gecko-simulation-core (DetailedLossLookupTable, curves)
  - 94 tests passing (16 new), OpenAPI/Swagger docs
  - Validates GUI-free architecture works for REST API
- `ab5f214a` Sprint 4b Phase 2B: UserParameter abstraction + curve class migration
  - Created UserParameterCore interface for headless parameters
  - Migrated LossCurve, SwitchingLossCurve, LeitverlusteMesskurve, DetailedLossLookupTable
  - Fixed TokenMap 2D array serialization format
  - 1,686 core tests + 5,373 main tests passing
- `b2f277da` Sprint 4b Phase 1: Migrate GUI-free losscalculation classes to core module
- `e26eda78` Update PRD.md: Remove HiLoData known issue, add resolution details
- `0df8ba91` Resolve HiLoData dual-version compatibility issue
- `fa0c3638` Update documentation after Sprint 4a GeckoFile migration
- `86c2a65a` Sprint 4a: Migrate GeckoFile to gecko-simulation-core
- `cfd7f579` Phase 3: Migrate signal analysis utilities to core module
- `19e4bb5a` Phase 2: Native C integration for scientific computing
- `99a2e661` Phase 1 utilities migration: enums + i18n support

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
