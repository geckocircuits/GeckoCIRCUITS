# Architecture Document

**Project:** GeckoCIRCUITS
**Last Updated:** 2026-02-12
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
│  │            IN PROGRESS (148 classes)                    │   │
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

### 4.1 Validated Packages (171 GUI-free classes)

Enforced by `CorePackageValidationTest` - build fails if GUI imports detected:

| Package | Classes | Coverage | Status |
|---------|---------|----------|--------|
| `circuit.matrix` | 15 | 85% | API-ready |
| `circuit.netlist` | 4 | 99% | API-ready |
| `circuit.simulation` | 5 | 97% | API-ready |
| `control.calculators` | 71 | ~81% | API-ready (2 GUI exceptions) |
| `math` | 7 | ~71% | API-ready |
| `circuit.losscalculation` | 18+2 | 61% | Partial (6 GUI classes, 18 computation classes GUI-free) |
| `circuit` (main) | 54 | ~57% | Partial (41 GUI classes) |

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
gecko-simulation-core (148 classes extracted):
  ├── circuit/         42 component cores
  ├── control/calc.    64 calculators (PI, PID, gain, limit, integrators, etc.)
  ├── datacontainer/   11 classes (signal storage, caching)
  ├── math/            7 classes (matrix ops, LU decomposition, FFT)
  ├── api/             Public interfaces
  └── allg/            3 GUI-free utilities

Tests (31 test files):
  ├── circuit/matrix/  8 test files
  ├── control/calc.    15 test files
  └── math/            7 test files
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

### 6.3 REST API (Planned)
```
POST   /api/v1/simulations              Start simulation
GET    /api/v1/simulations/{id}         Get status/results
DELETE /api/v1/simulations/{id}         Cancel simulation
GET    /api/v1/simulations/{id}/signals Signal data
POST   /api/v1/analysis/rms             RMS calculation
POST   /api/v1/analysis/thd             THD calculation
POST   /api/v1/analysis/harmonics       Harmonic analysis
GET    /api/health                       Health check
```

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
- PMD: Custom ruleset `pmd-ruleset.xml` (quickstart rules, 9 excluded rules, allowCommentedBlocks, excludes `com/intel/mkl/`), 823 violations
- Checkstyle: Custom config `checkstyle.xml` (150-char lines, relaxed naming), 4,632 violations

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
- Apply `LossFileAccessor` pattern to other GUI-coupled computation classes
- Complete core module migration (math, datacontainer, matrix, losscalculation)
- Implement REST API MVP with real simulation integration
- Add Maven enforcer rules to prevent GUI leakage into API module

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
