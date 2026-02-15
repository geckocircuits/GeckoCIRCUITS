# Architecture Document

**Project:** GeckoCIRCUITS
**Last Updated:** 2026-02-14
**Status:** Dual-track architecture (Desktop + API)

---

## 1. System Overview

GeckoCIRCUITS is a multi-domain circuit simulator built in Java 21. The architecture follows a dual-track approach: maintaining the mature Swing desktop application while extracting a GUI-free simulation core for headless/API use.

```
┌─────────────────────────────────────────────────────────────┐
│                    GeckoCIRCUITS System                       │
│                                                               │
│  ┌──────────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │  Desktop App     │  │  REST API    │  │  Documentation│  │
│  │  (Swing GUI)     │  │  (Spring)    │  │  (MkDocs)     │  │
│  │  PRODUCTION      │  │  PLANNED     │  │  LIVE         │  │
│  └────────┬─────────┘  └──────┬───────┘  └───────────────┘  │
│           │                   │                               │
│  ┌────────▼───────────────────▼──────────────────────────┐   │
│  │            gecko-simulation-core                        │   │
│  │            (GUI-free shared library)                    │   │
│  │            IN PROGRESS (179 classes)                    │   │
│  └────────────────────────────────────────────────────────┘   │
└───────────────────────────────────────────────────────────────┘
```

## 2. Module Structure

### 2.1 Multi-Module Build (`pom-reactor.xml`)

```
GeckoCIRCUITS/
├── pom.xml                          # Main desktop application
├── pom-reactor.xml                  # Reactor build (all modules)
├── src/modules/
│   ├── gecko-simulation-core/       # GUI-free simulation engine
│   │   ├── pom.xml
│   │   └── src/main/java/gecko/core/
│   │       ├── circuit/             # Component cores, matrix, netlist, simulation
│   │       ├── control/calculators/ # 64 calculator classes (PI, PID, gain, etc.)
│   │       ├── datacontainer/       # Signal storage (11 classes)
│   │       ├── math/               # Matrix operations, LU decomposition
│   │       └── api/                # Public interfaces
│   └── gecko-rest-api/              # Spring Boot REST API (scaffold)
│       ├── pom.xml                  # Spring Boot 3.2.1, depends on core
│       └── src/
```

### 2.2 Main Application (854 files)

```
src/main/java/gecko/
├── GeckoSim.java                    # Entry point, mode selection
├── geckocircuits/
│   ├── allg/                        # Global settings, file management, dialogs
│   ├── circuit/                     # Circuit components, terminals, netlist, matrix
│   │   ├── matrix/                  # MNA matrix stampers (15 classes, 85% coverage)
│   │   ├── netlist/                 # Netlist building (4 classes, 99% coverage)
│   │   ├── simulation/             # Simulation engine (5 classes, 97% coverage)
│   │   ├── losscalculation/        # Loss models (9 classes)
│   │   └── ...                     # Component editors, terminals
│   ├── control/                     # Control blocks
│   │   ├── calculators/            # 64+ calculator classes (GUI-free)
│   │   └── ...                     # Control panels, Java blocks
│   ├── datacontainer/              # Signal storage, caching
│   ├── math/                       # Matrix ops, FFT, LU decomposition
│   ├── newscope/                   # Oscilloscope visualization
│   └── scope/                      # Legacy scope
├── i18n/                           # Internationalization (968+ keys)
└── expressionscripting/            # GraalVM JavaScript evaluation
```

## 3. Core Simulation Engine

### 3.1 Simulation Flow

```
.ipes file → Parser → Circuit Model → Netlist → MNA Matrix → Solver → Results
                                          │
                                    ┌─────▼─────┐
                                    │ Stampers   │
                                    │ (R, L, C,  │
                                    │  switches) │
                                    └────────────┘
```

1. **Parser**: Reads gzip-compressed `.ipes` XML circuit files
2. **Netlist Builder**: Converts schematic to node/branch representation
3. **MNA Stampers**: Each component stamps its contribution into the system matrix
4. **Solver**: Backward Euler (BE), Trapezoidal (TRZ), or Gear-Shichman (GS) integration
5. **Results**: Time-domain waveforms stored in `DataContainer` classes

### 3.2 Key Interfaces

```java
// Circuit component stamping (Modified Nodal Analysis)
IMatrixStamper {
    stampMatrixA(double[][] a, ...)    // Stamp conductance matrix
    stampVectorB(double[] b, ...)      // Stamp source vector
    calculateCurrent(...)              // Post-solve current calculation
    getAdmittanceWeight(...)           // Diagonal weight for convergence
}

// Solver configuration
SolverContext {
    SOLVER_BE = 0   // Backward Euler (default)
    SOLVER_TRZ = 1  // Trapezoidal (more accurate)
    SOLVER_GS = 2   // Gear-Shichman (stiff systems)
}

// Component registry (Strategy pattern)
StamperRegistry.getStamper(CircuitTyp) → IMatrixStamper
```

### 3.3 Operating Modes

Defined in `OperatingMode` enum, selected at startup:

| Mode | Description | Interface |
|------|-------------|-----------|
| STANDALONE | Desktop GUI (default) | Swing |
| REMOTE | RMI server for MATLAB/Octave | `GeckoRemoteInterface` |
| MMF | Memory-mapped file communication | `GeckoCustomMMF` |
| SIMULINK | Direct Simulink integration | S-Function bridge |
| EXTERNAL | External tool integration | Custom protocol |

## 4. GUI-Free Boundary

### 4.1 Validated Packages (179 classes in core module)

Enforced by `CorePackageValidationTest` - build fails if GUI imports detected:

| Package | Classes | Tests | Status |
|---------|---------|-------|--------|
| `circuit.matrix` | 15 | 183 | API-ready |
| `circuit.netlist` | 4 | 89 | API-ready |
| `circuit.simulation` | 5 | 91 | API-ready |
| `circuit.terminal` | 3 | 138 | API-ready ✨ NEW |
| `circuit.component` | 3 | 206 | API-ready ✨ NEW |
| `control.calculators` | 71 | ~320 | API-ready (2 GUI exceptions) |
| `math` | 7 | 97 | API-ready |
| `datacontainer` | 11 | ~180 | API-ready |
| `circuit.losscalculation` | 20 | - | Deferred (needs HiLoData migration) |
| `allg` | 8 | 36 | API-ready (GeckoFile + interfaces) ✨ NEW |
| `circuit` (main) | 54 | - | Partial (41 GUI classes) |

### 4.2 GUI Decoupling Pattern

For packages that mix computation and GUI classes, the project uses **interface injection** to decouple computation code from GUI singletons:

```
┌──────────────────────────┐     ┌─────────────────────────────┐
│ VerlustBerechnungDetailed │────►│ LossFileAccessor (interface)│
│ (computation, GUI-free)   │     ├─────────────────────────────┤
└──────────────────────────┘     │ getFile(hash)               │
                                  │ maintain(file)              │
    ┌─────────────────────────┐   │ addFile(file)               │
    │ MainWindowLossFileAccessor│  │ getOpenFileName()           │
    │ (GUI adapter)            │──►│ getFilesByExtension(ext)    │
    │ delegates to MainWindow  │   └─────────────────────────────┘
    └─────────────────────────┘
```

- **Default constructor** chains to `MainWindowLossFileAccessor` (preserves all existing callers)
- **Injectable constructor** accepts any `LossFileAccessor` (enables headless testing with mocks)
- Pattern is replicable for other classes with `MainWindow` static dependencies

**GeckoFile Pattern (External Storage):**
```
┌──────────────────────────┐     ┌──────────────────────────────────┐
│ GeckoFile                 │────►│ ExternalStorageConverter (interface)│
│ (file handling, GUI-free) │     ├──────────────────────────────────┤
└──────────────────────────┘     │ promptForExternalPath()          │
                                  └──────────────────────────────────┘
    ┌───────────────────────────┐              ▲
    │DialogExternalStorageConverter│             │
    │ (GUI adapter)              │──────────────┘
    │ delegates to DialogMakeExternal│
    └───────────────────────────┘
```

- **Default constructor** uses reflection fallback to GUI dialog (preserves backward compatibility)
- **Injectable constructor** accepts `ExternalStorageConverter` (enables headless/API use)
- **ComponentIdentifiable interface** abstracts component identification for deserialization

### 4.3 Prohibited Imports in Core

```
java.awt.*
javax.swing.*
java.awt.event.*
javax.swing.event.*
java.applet.*
```

### 4.4 Extraction Status

```
gecko-simulation-core (183 classes extracted):
  ├── circuit/              68 classes
  │   ├── matrix/          15 classes (MNA stampers)
  │   ├── netlist/         4 classes (netlist building)
  │   ├── simulation/      5 classes (simulation engine)
  │   ├── terminal/        3 classes (ConnectionPath, ConnectionValidator, ITerminalPosition)
  │   ├── component/       3 classes (ParameterRegistry, ParameterSerializer, TerminalRegistry)
  │   ├── losscalculation/ 14 classes (loss curves, interpolation, calculators) ✨ SPRINT 4B
  │   │   ├── LossCurve, SwitchingLossCurve, LeitverlusteMesskurve (temp-dependent curves)
  │   │   ├── DetailedLossLookupTable (bilinear interpolation)
  │   │   ├── SwitchingLossCalculator, ConductionLossCalculator (loss computation)
  │   │   ├── LossComponent, LossCalculationDetail, LossContainer (data models)
  │   │   └── Interfaces: AbstractLossCalculator, LossFileAccessor, LossCalculatable
  │   └── circuitcomponents/ 22 component cores
  ├── control/calculators/ 71 calculators (PI, PID, gain, limit, integrators, etc.)
  ├── datacontainer/       11 classes (signal storage, caching)
  ├── math/                7 classes (matrix ops, LU decomposition, FFT)
  ├── nativec/             7 classes (Native C/C++ integration via JNI)
  ├── signal/              3 classes (CharacteristicsCalculator, FourierGUIless, Cispr16Fft)
  ├── io/                  1 class (SerializationUtils - .ipes file ASCII serialization)
  ├── i18n/                1 class (SelectableLanguages - 43 supported languages)
  ├── api/                 Public interfaces
  ├── allg/                10 classes ✨ SPRINT 4B
  │   ├── GeckoFile (file handling for Java blocks, loss models, nonlinear characteristics)
  │   ├── UserParameterCore, UserParameterCoreImpl (headless parameter abstraction)
  │   ├── ExternalStorageConverter (interface for GUI abstraction)
  │   ├── GlobalFilePathes, CircuitFileConstants, SolverType, OperatingMode, LaunchBrowser
  ├── GeckoRuntimeException (top-level)
  └── Circuit file parsing: TokenMap (41 tests) - migrated to gecko.core.circuit

Tests (59 test files, 1,686 tests):
  ├── circuit/losscalculation/ 10 test files (180+ tests) ✨ SPRINT 4B
  │   ├── Loss curve tests (LossCurve, SwitchingLossCurve, LeitverlusteMesskurve)
  │   ├── Interpolation tests (DetailedLossLookupTable)
  │   ├── Calculator tests (Switching, Conduction, Resistor)
  │   └── Interface tests (LossCalculatable, LossCalculationSplittable)
  ├── circuit/terminal/    3 test files (138 tests)
  ├── circuit/component/   3 test files (206 tests)
  ├── circuit/             2 test files (72 tests - TokenMap + ComponentIdentifiable)
  ├── allg/                4 test files (90+ tests - UserParameter, GeckoFile, LaunchBrowser, TechFormat) ✨ SPRINT 4B
  ├── circuit/matrix/      8 test files
  ├── control/calc.        15 test files
  ├── datacontainer/       18 test files
  ├── math/                7 test files
  ├── nativec/             5 test files (46 tests)
  ├── signal/              3 test files (29 tests)
  ├── i18n/                1 test file (17 tests - SelectableLanguages)
  └── core/                1 test file (8 tests - GeckoRuntimeException)
```

## 5. Documentation Architecture

### 5.1 Content Pipeline

```
resources/                          docs/                      GitHub Pages
├── tutorials/                      ├── getting-started/       (gh-pages branch)
│   ├── 1xx_getting_started/  ──►   ├── tutorials/
│   ├── 2xx_dcdc_converters/  ──►   │   ├── dcdc/
│   ├── 6xx_emi_emc/          ──►   │   ├── emi/
│   └── ...                         │   └── ...
├── examples/                 ──►   ├── examples/
├── articles/                 ──►   ├── articles/
│   ├── *.md                        │   ├── *.md (sanitized names)
│   ├── img/                        │   ├── img/ (copied)
│   └── ipes_files/                 │   └── ipes_files/ (copied)
└── README.md files                 └── api/core/ (generated)

Scripts:
  sync-docs.py           → Copies resources/ to docs/, converts links
  generate-api-docs.py   → Extracts Javadoc from core module to docs/api/
```

### 5.2 Hand-Maintained Pages

These files are NOT overwritten by `sync-docs.py` (path convention mismatch):
- `docs/tutorials/index.md` - Tutorial landing page with correct docs paths
- `docs/examples/index.md` - Examples landing page with correct docs paths

## 6. External Integration

### 6.1 RMI (Remote Method Invocation)
- `GeckoRemoteInterface` exposes simulation control to MATLAB/Octave
- `GeckoRemoteRegistry` manages RMI registry lifecycle
- Validated by `GeckoRemoteTest` (interface consistency checks)

### 6.2 Memory-Mapped Files
- `GeckoCustomMMF` enables high-performance data exchange
- Used for Simulink co-simulation with minimal latency

### 6.3 REST API (In Progress - Sprint 5)
**Live Endpoints (Phase 1 - Loss Calculation):**
```
POST   /api/v1/loss/switching           Switching loss (voltage/energy scaling)
POST   /api/v1/loss/conduction          Conduction loss (resistance model)
POST   /api/v1/loss/detailed            Detailed loss (temperature interpolation)
```

**Planned Endpoints (Phase 2-3):**
```
POST   /api/v1/circuit/load             Load and parse .ipes circuit files
GET    /api/v1/circuit/info             Circuit metadata extraction
POST   /api/v1/signal/fft               Fast Fourier Transform
POST   /api/v1/signal/cispr16           EMI analysis
POST   /api/v1/signal/characteristics   RMS, THD, min/max calculation
POST   /api/v1/simulations              Start simulation (existing)
GET    /api/v1/simulations/{id}         Get status/results (existing)
GET    /api/health                       Health check (existing)
```

**Implementation Details:**
- Uses gecko-simulation-core classes (DetailedLossLookupTable, SwitchingLossCurve, LeitverlusteMesskurve)
- OpenAPI/Swagger documentation at http://localhost:8080/swagger-ui.html
- Jakarta Bean Validation for request parameters
- 94 tests passing (16 new loss calculation tests)

**Docker Support:**
- Multi-stage Dockerfile using Alpine JRE 21 (~180MB image)
- `docker-compose.yml` for local development
- Production-ready for cloud deployment (AWS ECS, Kubernetes, Cloud Run)

## 7. Build & CI Pipeline

### 7.1 Build Commands
```bash
mvn clean package assembly:single          # Desktop JAR with dependencies
mvn -f pom-reactor.xml test                # All modules (reactor)
mvn clean test jacoco:report               # Coverage report
mvn clean package -Pdist-all -DskipTests   # Platform distributions
mkdocs build --strict                      # Docs site (catches broken links)
mkdocs gh-deploy --force                   # Deploy to GitHub Pages
```

### 7.2 Quality Gates
- JaCoCo: 30%+ instruction coverage on gecko-simulation-core (`mvn verify`)
- CorePackageValidationTest: No GUI imports in core
- MkDocs strict mode: No broken internal links
- SpotBugs: 0 bugs (204 inline `@SuppressFBWarnings` annotations)
- PMD: Custom ruleset `pmd-ruleset.xml` (quickstart rules, 10 excluded rules, allowCommentedBlocks, excludes `com/intel/mkl/`), **496 violations** ✅ **<500 TARGET ACHIEVED** (code-style only, down 86% from original 3,443 after systematic cleanup: 327 violations fixed, 6 categories eliminated to 0, 116 false positives excluded)
- Checkstyle: Custom config `checkstyle.xml` (150-char lines, relaxed naming), 4,632 violations

### 7.3 Release Automation
GitHub Actions workflows automate the release process:
- **`release.yml`** - Automated release triggered by version tags (v*), builds all 5 platforms in parallel, creates GitHub release
- **`build-windows.yml`**, **`build-macos.yml`**, **`build-linux-wsl.yml`** - Manual dispatch workflows for testing individual platform builds

**Distribution packages:**
- Windows: `GeckoCIRCUITS-*-windows.zip` (run-gecko.bat)
- Linux: `GeckoCIRCUITS-*-linux.zip` (run-gecko-linux.sh)
- macOS: `GeckoCIRCUITS-*-macos.zip` (run-gecko-macos.sh)
- WSL: `GeckoCIRCUITS-*-wsl.zip` (run-gecko-wsl.sh with X11 support)
- Examples: `GeckoCIRCUITS-*-examples.zip` (circuit files + tutorials)

For detailed release planning and version strategy, see [RELEASE_PLAN.md](RELEASE_PLAN.md).

## 8. Key Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| GraalVM Polyglot | 24.1.1 | JavaScript scripting (GeckoSCRIPT) |
| JTransforms | 2.4 | FFT operations |
| Apache Batik | 1.7 | SVG generation |
| JNA | 5.18.1 | Native library access |
| Log4j2 | 2.24.3 | Logging |
| Spring Boot | 3.2.1 | REST API (gecko-rest-api module) |
| MkDocs Material | latest | Documentation site theme |

## 9. Architecture Roadmap

### Near-Term
- ✅ Complete core module migration (math, datacontainer, matrix, losscalculation, signal)
- ✅ Implement REST API Phase 1 (loss calculation endpoints)
- 🔄 REST API Phase 2: Circuit file operations (TokenMap, GeckoFile integration)
- 🔄 REST API Phase 3: Signal analysis endpoints (FFT, CISPR16, characteristics)
- Apply `UserParameterCore` pattern to other GUI-coupled computation classes
- Maven enforcer already prevents GUI leakage into API module ✅

### Mid-Term
- Desktop `--rest-server` mode (GUI + API simultaneously)
- Python SDK for REST API
- Enhanced Docker deployment (health checks, multi-arch builds)

### Long-Term
- WebAssembly browser edition (GraalVM Native Image or TeaVM)
- React/WebGL frontend for browser-based simulation
- Progressive Web App for offline use

---

*This document reflects the current architecture. Updated after each sprint/push.*
