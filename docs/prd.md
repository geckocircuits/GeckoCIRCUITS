# GeckoCIRCUITS Product Requirements Document

**Version:** 2.0.0
**Last Updated:** 2026-02-18
**Status:** v3.0.0 — Complete REST API Platform (32 endpoints, 7,426 tests)

---

## 1. Product Vision

GeckoCIRCUITS is an open-source, Java 21 circuit simulator for power electronics, originally developed at ETH Zurich. It provides multi-domain simulation (electrical, thermal, EMI) with integration support for MATLAB/Simulink. The project follows a **dual-track strategy**: maintaining the mature desktop application for power users while adding modern web/API accessibility for automation, cloud deployment, and education.

## 2. Target Users

| Persona | Interface | Use Case |
|---------|-----------|----------|
| **Power electronics researchers** | Desktop GUI + RMI | Interactive circuit design, MATLAB/Simulink co-simulation |
| **University students** | Desktop GUI / Browser (future) | Learning power electronics through tutorials and examples |
| **Automation engineers** | REST API (in progress) | CI/CD pipeline validation, batch parameter sweeps, loss calculations |
| **Educators** | Documentation site | Teaching with curated tutorials, examples, articles |

## 3. Supported Platforms

| Platform | Distribution | Launcher Script |
|----------|-------------|-----------------|
| Windows 10/11 | `GeckoCIRCUITS-1.0-windows.zip` | `run-gecko.bat` |
| Linux (Ubuntu, Fedora, Arch) | `GeckoCIRCUITS-1.0-linux.zip` | `run-gecko-linux.sh` |
| macOS (Intel/Apple Silicon) | `GeckoCIRCUITS-1.0-macos.zip` | `run-gecko-macos.sh` |
| WSL2 (Windows 11 WSLg) | `GeckoCIRCUITS-1.0-wsl.zip` | `run-gecko-wsl.sh` |

### System Requirements
- **Java**: OpenJDK 21 or later
- **Memory**: 4 GB RAM (8 GB recommended)
- **Storage**: 200 MB for application
- **Display**: 1280x720 resolution (HiDPI supported via `--hidpi` flag)

## 4. Product Components

### 4.1 Desktop Application (Production)
- Swing-based GUI with oscilloscope visualization
- Multi-domain simulation: electrical, thermal, EMI
- MATLAB/Simulink integration via RMI and memory-mapped files
- GraalVM JavaScript scripting engine (GeckoSCRIPT)
- 64+ control blocks (PI/PID, integrators, differentiators, limiters)
- Supports `.ipes` circuit files (gzip-compressed XML)

### 4.2 Simulation Core Module (In Progress)
- **Location:** `src/modules/gecko-simulation-core/`
- GUI-free simulation engine suitable for headless operation (192 source classes, 74 test files, 1,809 tests)
- 30% JaCoCo coverage enforced via CI (exceeds threshold)
- Key packages: `circuit.matrix`, `circuit.netlist`, `circuit.simulation`, `circuit.terminal`, `circuit.component`, `circuit.losscalculation`, `control.calculators`, `math`, `datacontainer`, `allg` (file paths + parameters)
- Circuit file parsing: TokenMap, CircuitFileConstants for .ipes file processing
- Loss calculation: Temperature-dependent loss curves, bilinear interpolation, switching/conduction losses
- Validated by `CorePackageValidationTest` (zero GUI imports)

### 4.3 REST API (v3.0.0 — Production Ready)
- **Location:** `src/modules/gecko-rest-api/`
- Spring Boot 3.2.1 with OpenAPI/Swagger documentation, Spring Security API key auth
- **Live Endpoints (32 total):**
  - **Loss Calculation (3):** switching, conduction, detailed (bilinear interpolation)
  - **Circuit File Operations (8):** parse, info, components, validate, delete, list, clone, update parameters
  - **Simulation Control (9):** submit, status, results, list, cancel, export, batch submit, stream (SSE), WebSocket info
  - **Batch Job Tracking (2):** GET/DELETE `/api/v1/simulations/batch/{batchId}`
  - **Signal Analysis (3):** characteristics (RMS/THD/AVG), Fourier/FFT, (harmonics)
  - **Health/Infrastructure (3):** `/api/v1/health`, `/actuator/health`, Swagger UI
  - **WebSocket (STOMP/SockJS):** `/ws` (SockJS), `/ws-raw`; subscribe to `/topic/simulations/{id}`
- **Authentication:** Optional API key (`X-API-Key` header); `gecko.api.auth-enabled=false` by default
- **Real Solver Backend:** HeadlessSimulationEngine uses full MNA solver (MatrixSolver, CircuitNetlist, DomainCoupler)
- **Batch Simulations:** Up to 100 concurrent simulations; linearSweep/logSweep/explicit parameter sets
- **Parameter Overrides:** Dot-notation (`ComponentName.parameterKey`) applied before simulation
- 224 tests passing
- Docker packaging available (multi-stage Alpine JRE 21 build, ~180MB image)

### 4.4 Documentation Site (Live)
- **URL:** https://tinix84.github.io/GeckoCIRCUITS/
- **Technology:** MkDocs with Material theme, deployed via GitHub Pages (gh-pages branch)
- Content synced from `resources/` via `scripts/sync-docs.py`
- Sections: Getting Started, Tutorials (9 series), Examples (6 categories), Articles (10 newsletters), API Reference

## 5. Core Features

### Circuit Simulation
- **Electrical domain**: R, L, C, diodes, MOSFETs, IGBTs, transformers
- **Thermal domain**: Junction temperature calculation, thermal networks, heatsink design
- **EMI domain**: EMI filter design and analysis
- **Solver methods**: Backward Euler (BE), Trapezoidal (TRZ), Gear-Shichman (GS)

### Control System Modeling
- 64+ control blocks (PI/PID controllers, integrators, differentiators, limiters)
- Signal processing (FFT, filters, math operations)
- PWM generation (carrier-based, space vector)
- State machines and logic blocks

### Visualization
- Real-time oscilloscope with multiple channels
- FFT spectrum analyzer, XY plots, Bode plots
- Export to CSV, images, SVG

### Scripting & Automation
- GeckoSCRIPT for batch simulations
- JavaScript expression evaluation (GraalVM)
- Parameter sweeps and optimization
- Custom Java blocks

### External Integration
- **MATLAB/Simulink**: RMI-based remote control, memory-mapped file communication
- **REST API** (v3.0.0 live): 32 endpoints, Spring Boot, Docker-ready, API key auth, WebSocket streaming
- **Native C**: JNI integration for custom components

## 6. Current Sprint Status

### Release History

**Upstream Lineage:**
| Version | Source | Description |
|---------|--------|-------------|
| v2.02 | geckocircuits/GeckoCIRCUITS | Last upstream release (not in this fork) |

**This Fork (Complete Release History):**
| Version | Date | Milestone | Key Deliverables |
|---------|------|-----------|-----------------|
| v2.03-spotbugs-clean | Jan 31, 2026 | Code Quality | All 1,096 SpotBugs violations fixed to zero |
| v2.04-repo-reorg | Feb 1, 2026 | Infrastructure | Repository reorganization with JDK 21 workflow fix |
| v2.10.0 | Feb 14, 2026 | Java 21 Migration (GROUND ZERO) | Upgrade to Java 21, fix deprecated APIs, PR #1 merge |
| v2.11.0 | Feb 14, 2026 | Core Module Foundation | gecko-simulation-core module, math/datacontainer/signal packages |
| v2.12.0 | Feb 14, 2026 | Static Analysis Sprint | PMD/Checkstyle config, 2,620 violations fixed (-76%) |
| v2.13.0 | Feb 14, 2026 | Terminal/Component Migration | Circuit parsing in core, TokenMap migration |
| v2.14.0 | Feb 14, 2026 | GeckoFile Migration (Sprint 4a) | Complete circuit file I/O headlessly |
| v2.15.0 | Feb 14, 2026 | Loss Calculation (Sprint 4b) | UserParameter abstraction, loss curves in core |
| v2.16.0 | Feb 14, 2026 | REST API Launch (Sprint 5) | First public REST API (8 endpoints, Docker) |
| v2.17.0 | Feb 14, 2026 | Release Automation | GitHub Actions workflows (5 platforms) |
| v2.18.0 | Feb 17, 2026 | Headless Simulation Engine (Sprint 6) | Real solver integration, domain coupling, simulation control API |
| v2.19.0 | Feb 18, 2026 | SSE Streaming + Batch + Analysis | SSE progress, batch simulations, signal analysis endpoints |
| v2.20.0 | Feb 18, 2026 | Batch Job Tracking + Circuit Mgmt | Batch status/cancel, circuit clone/parameter update |
| v2.21.0 | Feb 18, 2026 | WebSocket Streaming | STOMP/SockJS real-time simulation progress |
| v2.22.0 | Feb 18, 2026 | API Key Authentication | Spring Security, X-API-Key header, optional auth |
| **v3.0.0** | **Feb 18, 2026** | **Complete REST API Platform** | **32 endpoints, 7,426 tests, production-ready** |

**Roadmap:**
| Version | Target | Description |
|---------|--------|-------------|
| v3.1.0 | Q2 2026 | Rate limiting, JWT tokens, pagination, WebSocket auth, enhanced circuit parsing |
| v3.2.0 | Q3 2026 | Web UI Launch (React + TypeScript circuit editor) |
| v4.0.0 | Q1 2027 | Cloud Deployment (Kubernetes, multi-tenant SaaS) |
| v5.0.0 | Q3 2027 | Machine Learning Integration (AI-assisted design) |

**Note:** For detailed release planning, version numbering strategy, and full changelog, see [RELEASE_PLAN.md](RELEASE_PLAN.md).

### Latest Sprint (2026-02-18): v3.0.0 — Complete REST API Platform

**MILESTONE: All v3.0.0 targets ACHIEVED (Feb 18, 2026)**
- 32 production-ready REST API endpoints (target was 30+)
- 7,426 total tests (5,373 main + 1,837 core + 224 API)
- Spring Security API key authentication
- WebSocket/STOMP real-time streaming
- Batch simulation with linearSweep/logSweep
- Signal analysis (characteristics, Fourier/FFT)
- Circuit clone/update endpoints
- Parameter override applicator (dot-notation, pre-simulation)
- Enhanced circuit parsing: real .ipes parameter extraction fixed (space-separated format)

**v2.19.0 (Feb 18, 2026): SSE Streaming + Batch + Analysis**
- **ParameterOverrideApplicator**: Dot-notation pre-simulation overrides (`R1.resistance=10.0`)
- **SSE Progress Streaming**: `GET /api/v1/simulations/{id}/stream` (text/event-stream)
- **Batch Simulation**: `POST /api/v1/simulations/batch` — linearSweep, logSweep, or explicit parameterSets; up to 100 concurrent
- **Signal Analysis**: `POST /api/v1/analysis/characteristics` (RMS/THD/AVG/min/max), `POST /api/v1/analysis/fourier` (FFT harmonics); input via raw data or simulationId+signalName
- REST API: 157 → 169 tests

**v2.20.0 (Feb 18, 2026): Batch Job Tracking + Circuit Management**
- **Batch Status**: `GET /DELETE /api/v1/simulations/batch/{batchId}` — per-job progress, failedIds
- **Circuit Clone**: `POST /api/v1/circuits/{id}/clone` with optional parameter overrides
- **Circuit Parameter Update**: `PUT /api/v1/circuits/{id}/parameters` — partial update (duration/timeStep/solverType)
- REST API: 169 → 191 tests

**v2.21.0 (Feb 18, 2026): WebSocket Streaming**
- **STOMP broker**: `/ws` (SockJS) and `/ws-raw`; subscribe to `/topic/simulations/{id}`
- `SimulationProgressMessage` DTO: simulationId, progress, currentTime, endTime, step, totalSteps, status
- `WebSocketProgressService`: dual SSE+WebSocket broadcasts from SimulationService
- `GET /api/v1/simulations/{id}/ws-info` endpoint
- REST API: 191 → 204 tests

**v2.22.0 (Feb 18, 2026): API Key Authentication**
- `ApiKeyProperties` (`@ConfigurationProperties(gecko.api)`): comma-separated keys, `isValidKey()`
- `ApiKeyAuthFilter` (`OncePerRequestFilter`): validates `X-API-Key` header; public paths bypass
- `SecurityConfig`: stateless, CSRF disabled; `gecko.api.auth-enabled=false` default
- REST API: 204 → 224 tests (20 new security tests)

**Sprint 6 (Feb 17, 2026): Headless Simulation Engine (v2.18.0)**
- MatrixSolver, ComponentCurrentCalculator, InitialConditionSolver, DomainCoupler in core
- Real MNA solver replaces HeadlessSimulationEngine placeholder
- Core module: 1,711 → 1,809 tests; REST API simulation endpoints wired to real solver

### Previous Sprint (2026-02-14): Sprint 4b - Loss Calculation Migration

**Phase 2B: UserParameter Abstraction + Curve Class Migration (COMPLETED)**
- Created UserParameterCore abstraction for headless parameter management:
  - UserParameterCore<T> interface (9 essential methods) in gecko.core.allg
  - UserParameterCoreImpl<T> with Builder pattern for core module
  - UserParameterGUIAdapter<T> for backward compatibility with main project
- Migrated 4 loss curve classes (376 LOC) to gecko.core.circuit.losscalculation:
  - LossCurve - Abstract base class for temperature-dependent loss curves
  - SwitchingLossCurve - Switching loss measurement curves with blocking voltage
  - LeitverlusteMesskurve - Conduction loss measurement curves
  - DetailedLossLookupTable - Bilinear interpolation for loss lookup
- Migrated 7 test files with 180+ test cases, converted JUnit 4 → JUnit 5
- Fixed TokenMap 2D array serialization format ("[][]" identifier + dimensions)
- Updated 12 main project files with new import paths
- Core module: 183 classes, 59 test files, 1,686 tests, 30%+ coverage maintained
- **Build Status:** Core SUCCESS (1,686 tests) + Main SUCCESS (5,373 tests) = 7,059 tests passing

**Phase 1: GUI-Free Loss Calculator Migration (COMPLETED)**
- Migrated 10 GUI-free loss calculation classes to gecko.core.circuit.losscalculation:
  - Calculators: SwitchingLossCalculator, ConductionLossCalculator
  - Interfaces: AbstractLossCalculator, AbstractLossCalculatorFabric, LossCalculatable, LossCalculationSplittable, LossFileAccessor
  - Data models: LossComponent, LossCalculationDetail, LossContainer
- Migrated 10 test files, converted JUnit 4 → JUnit 5
- Core module: 179 classes, 52 test files, 1,524 tests

### Previous Sprint (2026-02-14): Sprint 4a - GeckoFile Migration

- Migrated GeckoFile (750 LOC) from main project to gecko.core.allg
- Created interface injection pattern for GUI abstraction:
  - ExternalStorageConverter interface (gecko.core.allg) - abstracts GUI dialog for external storage
  - DialogExternalStorageConverter (main) - GUI adapter delegating to DialogMakeExternal
  - ComponentIdentifiable interface (gecko.core.circuit) - abstracts component identification for deserialization
- Refactored GeckoFile with constructor injection and reflection fallback for backward compatibility
- Migrated GeckoFileTest (31 tests) from JUnit 4 to JUnit 5
- Updated 29 files with new import paths
- **TokenMap dual-version resolution:** Removed duplicate TokenMap from main project, unified on gecko.core.circuit.TokenMap (78 files updated)
- **HiLoData dual-version resolution:** Removed duplicate HiLoData from newscope, unified on gecko.core.datacontainer.HiLoData (41 files updated)
- Core module: 179 classes, 52 test files, 1,307 tests, 30%+ coverage maintained
- All modules compile and test successfully: 7,137 tests passing (1,307 core + 5,752 main + 78 rest-api)

### Previous Sprint (2026-02-13): Phase 3 Signal Analysis Migration
- Created gecko.core.signal package for signal analysis utilities
- Migrated 4 classes to support REST API signal analysis endpoints:
  - GeckoInvalidArgumentException (gecko.core) - Exception for invalid arguments
  - CharacteristicsCalculator (gecko.core.signal) - RMS, THD, AVG, MIN/MAX, ripple, distortion factor
  - FourierGUIless (gecko.core.signal) - GUI-less Fourier analysis for GeckoSCRIPT
  - Cispr16Fft (gecko.core.signal) - FFT computation with Blackman filtering
- Added 4 comprehensive test files with 66 tests (1,276 total):
  - TechFormatTest (gecko.core.allg) - 41 tests for engineering notation parsing
  - CharacteristicsCalculatorTest - 11 tests for signal characteristics
  - FourierGUIlessTest - 6 tests for Fourier analysis
  - Cispr16FftTest - 12 tests for FFT algorithms
- Core module: 176 classes, 50 test files, 1,276 tests, 30%+ coverage maintained

### Previous Sprint (2026-02-13): Phase 2 Native C Integration
- Migrated 7 Native C/C++ integration classes to gecko-simulation-core:
  - CompileStatus enum (compilation status tracking)
  - InterfaceNativeCWrapper (JNI interface for native functions)
  - CompiledClassContainer (compiled class storage with TokenMap support)
  - NativeCClassLoader (garbage-collectable classloader for native wrappers)
  - NativeCLibraryFile (library file management with timestamp tracking)
  - NativeCWrapper (JNI native method declarations)
  - NativeCBlock (lifecycle management for native library integration)
- Created gecko.core.nativec package for scientific computing
- Added 46 comprehensive tests (1,207 total, was 1,161)
- Core module: 183 classes, 1,207 tests, 30%+ coverage maintained

### Previous Sprint (2026-02-13): Phase 1 Utilities Migration (Enums + i18n)
- Migrated 4 foundational utility classes to gecko-simulation-core:
  - OperatingMode enum (6 modes: STANDALONE, SIMULINK, EXTERNAL, REMOTE, MMF, HEADLESS)
  - LaunchBrowser utility (cross-platform browser launching)
  - GeckoRuntimeException (custom runtime exception with OutOfMemoryError support)
  - SelectableLanguages enum (43 supported languages with ISO 639-1 codes)
- Created gecko.core.i18n package for internationalization support
- Added 39 comprehensive tests (1,161 total, was 1,122)
- Core module: 177 classes, 1,161 tests, 30%+ coverage maintained

### Previous Sprint (2026-02-13): Serialization Utilities & expressionscripting Assessment
- Created SerializationUtils in gecko.core.io (7 utility methods for .ipes file serialization)
- Extracted from ProjectData.appendAsString() - enables future GeckoFile migration without full ProjectData abstraction
- Assessed expressionscripting package (5 classes): 4/5 files completely commented out, package deprecated, not worth migrating
- Core module: 171 classes, 1,122 tests, 30%+ coverage maintained

### Previous Sprint (2026-02-13): Phase 1 Migration - Circuit File Parsing
- Migrated TokenMap to gecko-simulation-core (689 lines, 41 tests) - Key class for parsing .ipes circuit files
- Created CircuitFileConstants for circuit file parsing constants (NIX, SEPARATOR)
- Migrated GlobalFilePathes (47 lines) - Static file path variables
- Assessment: GeckoFile and GeckoFileable deferred (dependencies on ProjectData static refs, DialogMakeExternal GUI)
- Core module: 170 classes, 1,122 tests, 30%+ coverage maintained

### Previous Sprint (2026-02-13): Tier 1 Package Migration (terminal + component)
- Migrated `circuit.terminal` package to gecko-simulation-core (3 source classes, 3 test files, 138 tests)
  - Created minimal GUI-free `ConnectorType` enum in core
  - ConnectionPath, ConnectionValidator, ITerminalPosition (path routing and validation logic)
- Migrated `circuit.component` package to gecko-simulation-core (3 source classes, 3 test files, 206 tests)
  - ParameterRegistry, ParameterSerializer, TerminalRegistry (component data management)
- Assessed `circuit.losscalculation` migration complexity: deferred due to dependencies on ProjectData, TokenMap, GeckoFile (not yet in core)
- Core module: 168 classes (from 148), 1,081 tests (from 737), 30%+ coverage maintained

### Previous Sprint (2026-02-13): Docker Packaging for REST API
- Created multi-stage Dockerfile for gecko-rest-api module (Alpine JRE 21, ~180MB final image)
- Added docker-compose.yml for local development and testing
- Created Docker build and run scripts (`build-docker.sh`, `run-docker.sh`)
- Updated REST API README with comprehensive Docker documentation (including Docker Desktop deployment guide)
- Dockerfile optimized with layer caching, non-root user, and health checks

### Previous Sprint (2026-02-12): Core Module Migration (math + datacontainer)
- Migrated 7 math test files to gecko-simulation-core module (test coverage preserved)
- Migrated 11 datacontainer source classes to gecko-simulation-core module
- Migrated 18 datacontainer test files to gecko-simulation-core module
- Core module now contains 148 source classes, 31 test files, 737 tests
- All reactor builds passing, coverage thresholds maintained

### Previous Sprint (2026-02-12 to 2026-02-15): Static Analysis Cleanup
- Created `pmd-ruleset.xml` and `checkstyle.xml` config files, updated `pom.xml`
- Fixed 1,445 auto-fixable PMD violations across 330+ files:
  - UnnecessaryFullyQualifiedName (694), UselessParentheses (354), UnnecessaryImport (111), UnnecessaryModifier (104)
- Fixed 183 Tier 3 PMD violations: EmptyCatchBlock (20), EmptyControlStatement (44), UnnecessarySemicolon (34), UnnecessaryReturn (33), UnusedLocalVariable (28), UnusedPrivateMethod (13), UnusedPrivateField (11)
- Fixed 38 ReturnEmptyCollectionRatherThanNull violations across 11 files (GeckoRemoteMMFObject, GeckoExternal, AbstractGeckoCustom, ComponentPositioner, and 7 others)
- **Achieved <500 PMD violations target: 823 → 496 (-40% in one sprint, -86% from original 3,443)**
- Fixed 327 PMD violations total (145 code-style, 88 high-value, 94 modernization):
  - **6 violation categories eliminated to 0:**
    - ForLoopCanBeForeach (94): Modernized all traditional for-loops across 6 batches (43 files)
    - PreserveStackTrace (38): All exception chains now preserve stack traces
    - SimplifyBooleanReturns (29): Simplified boolean logic
    - CompareObjectsWithEquals (31): Changed == to .equals() for object comparisons
    - CloseResource (52): All resource leaks fixed with try-with-resources (30 fixed, 5 documented false positives, 17 earlier)
    - IdenticalCatchBranches (22): Merged to multi-catch statements
  - UnnecessaryImport (116): Excluded in ruleset (false positives)
- Stripped trailing whitespace from 974 Java source files (13,359 lines)
- Fixed GitHub Actions CI: reactor build + proper dependency resolution (all modules now build successfully)
- **SpotBugs: 0 bugs (maintained), PMD: 496 violations ✅ <500 TARGET ACHIEVED, Checkstyle: 4,632**

### Previous Sprint (2026-02-12): LossCalculation GUI Decoupling
- Introduced `LossFileAccessor` interface to decouple `VerlustBerechnungDetailed` from `MainWindow`
- Replaced 11 static `MainWindow` references with injectable accessor pattern
- Added 22 new tests for previously-untestable file I/O methods (mock-based)
- `losscalculation` package coverage: 51% -> 61% (exceeds 60% threshold)
- `VerlustBerechnungDetailed` coverage: 92% instruction, 87% branch
- Added 8 new test files for losscalculation package (including existing untracked tests)

### Previous Sprint (2026-02-12): Package Rename
- Removed `ch.technokrat` from all packages: `ch.technokrat.*` -> `gecko.*`
- Updated 1,391 files (1,288 Java, 21 config/scripts/docs, 5 .ipes, 2 JNI native)
- Rebuilt JNI native libraries with new symbol names
- All modules compile and pass tests (reactor build verified)

### Previous Sprint (2026-02-11): Documentation Overhaul
- Fixed 95+ broken internal links across docs site
- Integrated 10 newsletter articles from `resources/articles/`
- Added EMI/EMC and Advanced Topics tutorial sections
- Created Javadoc extraction script (`scripts/generate-api-docs.py`)
- Generated Core Module API reference (4 pages)
- Deployed site to GitHub Pages (gh-pages branch)

### In Progress
- Core module extraction: 148 classes extracted, math and datacontainer migration complete
- GUI decoupling: `LossFileAccessor` pattern established, replicable for other packages
- Test coverage growth: 5,783 tests (main project), targeting further coverage gains
- Documentation site maintenance

### Planned (Next Sprints)
1. REST API MVP (health endpoint, simulation CRUD, signal analysis)
2. Additional core package migrations (identify remaining GUI-free candidates)
3. Coverage targets: losscalculation 65%, additional core packages 70%+

## 7. Quality Assurance

### Testing
- Unit tests for calculators and core components
- Integration tests with real circuit files (`ModelResultsTest`)
- API consistency tests (`GeckoRemoteTest`)
- GUI-free validation tests (`CorePackageValidationTest`)

### Quality Gates
- JaCoCo: 60%+ instruction coverage on core packages
- `CorePackageValidationTest`: Zero GUI imports in core module
- `mkdocs build --strict`: Zero broken links in documentation
- SpotBugs: 0 bugs enforced (204 inline `@SuppressFBWarnings` annotations)
- PMD: Custom ruleset (`pmd-ruleset.xml`), **496 remaining violations** ✅ **<500 TARGET ACHIEVED** (down 86% from original 3,443)
- Checkstyle: Project-tuned config (`checkstyle.xml`), 4,632 remaining violations

### Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Total tests (main) | 5,783 | 6,000+ |
| Core module classes | 168 | 200+ |
| Core module tests | 1,081 | 1,200+ |
| Core module coverage | 30%+ | 40%+ |
| losscalculation coverage | 61% | 65%+ |
| Docs site pages | 82+ | 100+ |
| Broken links | 0 | 0 |
| SpotBugs bugs | 0 | 0 |
| PMD violations | **496** ✅ | <500 |
| Checkstyle violations | 4,632 | <2,000 |
| REST API endpoints | 8 | 10+ |

## 8. Content Inventory

### Tutorials (9 series, 36 tutorials)
- 1xx Getting Started (7), 2xx DC-DC Converters (4), 3xx AC-DC Rectifiers (3)
- 4xx DC-AC Inverters (3), 5xx Thermal Simulation (3), 6xx EMI/EMC (2)
- 7xx Scripting & Automation (6), 8xx Advanced Topics (4), 9xx Magnetics & Mechanical (4)

### Examples (6 categories)
- Basic Topologies, Power Supplies, Motor Drives, Thermal, Automotive, Renewable Energy

### Articles (10 newsletters)
- Technical papers from 2009-2010 covering topology debugging, control techniques, EMI analysis

### API Reference (4 pages)
- Core module overview, Simulation Engine, Matrix Stampers, Control Calculators

## 9. Distribution Packages

Each platform package includes:
- `GeckoCIRCUITS.jar` - Main application (fat JAR with all dependencies)
- Platform-specific launcher script with proper JVM options
- README with quick start instructions

Build commands:
```bash
mvn clean package -Pdist-all -DskipTests    # All platforms
mvn clean package -Pdist-windows -DskipTests # Single platform
```

## 10. File Formats

| Extension | Description |
|-----------|-------------|
| `.ipes` | Circuit schematic (gzip-compressed XML) |
| `.scl` | GeckoSCRIPT source files |
| `.gmd` | GeckoMAGNETICS design files |
| `.prp` | Application properties |

## 11. License

Open-source software (GPL v3). See LICENSE file for details.

---

*This document is kept in sync with development progress. Updated after each sprint/push.*
