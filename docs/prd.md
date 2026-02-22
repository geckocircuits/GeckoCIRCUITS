# GeckoCIRCUITS Product Requirements Document

**Version:** 3.0.0
**Last Updated:** 2026-02-22
**Status:** v3.0.0 — Complete REST API Platform (32 endpoints, 7,434 tests)

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

### 4.2 Simulation Core Module (Production)
- **Location:** `src/modules/gecko-simulation-core/`
- GUI-free simulation engine suitable for headless operation (216 source classes, 80 test files, 1,837 tests)
- 30% JaCoCo coverage enforced via CI (exceeds threshold)
- Key packages: `circuit.matrix`, `circuit.netlist`, `simulation`, `circuit.terminal`, `circuit.component`, `circuit.losscalculation`, `control.calculators`, `math`, `datacontainer`, `allg`, `io`, `signal`, `nativec`
- Circuit file parsing: TokenMap, CircuitFileParser, CircuitModel for .ipes file processing
- Loss calculation: Temperature-dependent loss curves, bilinear interpolation, switching/conduction losses
- Validated by `CorePackageValidationTest` (zero GUI imports)

### 4.3 REST API (v3.0.0 — Production Ready)
- **Location:** `src/modules/gecko-rest-api/`
- Spring Boot 3.2.1 with OpenAPI/Swagger documentation, Spring Security API key auth
- **Live Endpoints (32 total):**
  - **Loss Calculation (3):** switching, conduction, detailed (bilinear interpolation)
  - **Circuit File Operations (9):** parse, info, components, validate, raw, delete, list, clone, update parameters
  - **Simulation Control (13):** submit, status, results, results/{signal}, progress, stream (SSE), ws-info, export, list, cancel, batch submit, batch status, batch cancel
  - **Signal Analysis (3):** characteristics (RMS/THD/AVG), Fourier/FFT, quick RMS
  - **Health/Info (3):** `/api/health`, `/api/info`, `/api/docs`
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
| **v3.0.0** | **Feb 18, 2026** | **Complete REST API Platform** | **32 endpoints, 7,434 tests, production-ready** |

**Roadmap (GitHub Issues):**

| Version | Target | Epic | Features |
|---------|--------|------|----------|
| v3.1.0 | Q2 2026 | [#17](https://github.com/geckocircuits/GeckoCIRCUITS/issues/17) | Rate limiting [#24], JWT [#25], pagination [#26], WebSocket auth [#27], parsing [#28], RBAC [#29], SDKs [#30] |
| v3.2.0 | Q3 2026 | [#18](https://github.com/geckocircuits/GeckoCIRCUITS/issues/18) | React app [#31], circuit editor [#32], oscilloscope [#33], PWA [#34] |
| v4.0.0 | Q1 2027 | [#19](https://github.com/geckocircuits/GeckoCIRCUITS/issues/19) | Kubernetes [#35], multi-tenant [#36], Redis/Postgres [#37], observability [#38] |
| v5.0.0 | Q3 2027 | [#20](https://github.com/geckocircuits/GeckoCIRCUITS/issues/20) | RL optimization [#39], surrogate models [#40], component selection [#41] |

**Long-term vision:** Educational platform [#21], industry partnerships [#22], research collaboration [#23]

**Note:** For detailed roadmap, release process, and issue tracking, see [docs/roadmap.md](roadmap.md).

### Latest Sprint (2026-02-18): v3.0.0 — Complete REST API Platform

**MILESTONE: All v3.0.0 targets ACHIEVED (Feb 18, 2026)**
- 32 production-ready REST API endpoints (target was 30+)
- 7,434 total tests (5,373 main + 1,837 core + 224 API)
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

### Previous Sprints (Summary)

| Sprint | Date | Key Deliverable |
|--------|------|-----------------|
| Sprint 4b: Loss Calculation | Feb 14 | UserParameterCore abstraction, 4 loss curve classes migrated, bilinear interpolation |
| Sprint 4a: GeckoFile Migration | Feb 14 | GeckoFile (750 LOC) to core, interface injection pattern, TokenMap/HiLoData unification |
| Signal Analysis Migration | Feb 13 | gecko.core.signal package (CharacteristicsCalculator, FourierGUIless, Cispr16Fft) |
| Native C Integration | Feb 13 | 7 JNI integration classes to gecko.core.nativec |
| Utilities Migration | Feb 13 | OperatingMode, LaunchBrowser, GeckoRuntimeException, SelectableLanguages |
| Serialization & File Parsing | Feb 13 | SerializationUtils, TokenMap, CircuitFileConstants, GlobalFilePathes |
| Terminal + Component Migration | Feb 13 | circuit.terminal (3 classes), circuit.component (3 classes) |
| Docker Packaging | Feb 13 | Multi-stage Dockerfile, docker-compose.yml, Alpine JRE 21 (~180MB) |
| Core Module Foundation | Feb 12 | math + datacontainer migration (148 classes, 737 tests) |
| Static Analysis Cleanup | Feb 12-15 | PMD 3,443 → 496 (-86%), SpotBugs 0, 6 categories eliminated |
| LossCalculation GUI Decoupling | Feb 12 | LossFileAccessor interface, coverage 51% → 61% |
| Package Rename | Feb 12 | ch.technokrat.* → gecko.* (1,391 files) |
| Documentation Overhaul | Feb 11 | 95+ broken links fixed, 10 newsletter articles, GitHub Pages deployment |

### In Progress (v3.1.0 Roadmap)
- Rate limiting and request throttling
- JWT token authentication (complement to API key auth)
- Pagination for list endpoints
- WebSocket authentication
- Enhanced circuit parsing (additional component types)

### Planned (Future)
1. **v3.2.0 (Q3 2026):** Web UI Launch — React + TypeScript circuit editor
2. **v4.0.0 (Q1 2027):** Cloud Deployment — Kubernetes, multi-tenant SaaS
3. **v5.0.0 (Q3 2027):** Machine Learning Integration — AI-assisted circuit design

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

| Metric | Current | Target (v3.1.0) |
|--------|---------|-----------------|
| Total tests | **7,434** (5,373 + 1,837 + 224) | 7,500+ |
| Core module classes | **216** ✅ | 230+ |
| Core module test files | **80** | 85+ |
| Core module tests | **1,837** ✅ | 2,000+ |
| Core module coverage | 30%+ | 40%+ |
| Main app files | 832 | — |
| REST API endpoints | **32** ✅ | 35+ |
| REST API tests | **224** | 250+ |
| Docs site pages | 82+ | 100+ |
| Broken links | 0 | 0 |
| SpotBugs bugs | 0 | 0 |
| PMD violations | **496** ✅ | <400 |
| Checkstyle violations | 4,632 | <2,000 |

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
