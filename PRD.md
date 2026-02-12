# GeckoCIRCUITS Product Requirements Document

**Version:** 1.1.0
**Last Updated:** 2026-02-12
**Status:** Active Development

---

## 1. Product Vision

GeckoCIRCUITS is an open-source, Java 21 circuit simulator for power electronics, originally developed at ETH Zurich. It provides multi-domain simulation (electrical, thermal, EMI) with integration support for MATLAB/Simulink. The project follows a **dual-track strategy**: maintaining the mature desktop application for power users while adding modern web/API accessibility for automation, cloud deployment, and education.

## 2. Target Users

| Persona | Interface | Use Case |
|---------|-----------|----------|
| **Power electronics researchers** | Desktop GUI + RMI | Interactive circuit design, MATLAB/Simulink co-simulation |
| **University students** | Desktop GUI / Browser (future) | Learning power electronics through tutorials and examples |
| **Automation engineers** | REST API (planned) | CI/CD pipeline validation, batch parameter sweeps |
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
- GUI-free simulation engine suitable for headless operation (148 source classes, 31 test files, 737 tests)
- 70% JaCoCo coverage enforced via CI
- Key packages: `circuit.matrix`, `circuit.netlist`, `circuit.simulation`, `control.calculators`, `math`, `datacontainer`
- Validated by `CorePackageValidationTest` (zero GUI imports)

### 4.3 REST API (Planned)
- **Location:** `src/modules/gecko-rest-api/`
- Spring Boot 3.2.1 with OpenAPI/Swagger
- Planned endpoints: simulation CRUD, signal analysis (RMS, THD, FFT), health check

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
- **REST API** (planned): Headless operation for automation and cloud deployment
- **Native C**: JNI integration for custom components

## 6. Current Sprint Status

### Release History

| Version | Milestone | Key Deliverables |
|---------|-----------|-----------------|
| v0.1.0 | CI/CD Foundation | GitHub Actions pipeline, launcher scripts, distribution packaging |
| v0.2.0 | Documentation Site | MkDocs site with tutorials, examples structure |
| v0.3.0 | Scripting Content | GeckoSCRIPT, MATLAB, Python, Java Blocks tutorials |
| v0.4.0 | Test Coverage | JaCoCo enforcement (60%+ for core), 125 new tests |
| v0.5.0 | Developer Onboarding | Developer guide, contributor docs, example docs |
| v1.0.0 | Production Release | URL fixes, polished packaging |
| v1.1.0 | Multi-Module Build | Reactor build, zero-crossing detection, REST API test fixes |

### Latest Sprint (2026-02-12): Core Module Migration (math + datacontainer)
- Migrated 7 math test files to gecko-simulation-core module (test coverage preserved)
- Migrated 11 datacontainer source classes to gecko-simulation-core module
- Migrated 18 datacontainer test files to gecko-simulation-core module
- Core module now contains 148 source classes, 31 test files, 737 tests
- All reactor builds passing, coverage thresholds maintained

### Previous Sprint (2026-02-12): Static Analysis Cleanup
- Created `pmd-ruleset.xml` and `checkstyle.xml` config files, updated `pom.xml`
- Fixed 1,445 auto-fixable PMD violations across 330+ files:
  - UnnecessaryFullyQualifiedName (694), UselessParentheses (354), UnnecessaryImport (111), UnnecessaryModifier (104)
- Fixed 183 Tier 3 PMD violations: EmptyCatchBlock (20), EmptyControlStatement (44), UnnecessarySemicolon (34), UnnecessaryReturn (33), UnusedLocalVariable (28), UnusedPrivateMethod (13), UnusedPrivateField (11)
- Stripped trailing whitespace from 974 Java source files (13,359 lines)
- SpotBugs: 0 bugs (maintained), PMD: 823 violations (down from 3,443), Checkstyle: 4,632

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
4. Docker packaging for REST API

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
- PMD: Custom ruleset (`pmd-ruleset.xml`), 823 remaining violations (code-style, not bugs)
- Checkstyle: Project-tuned config (`checkstyle.xml`), 4,632 remaining violations

### Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Total tests (main) | 5,783 | 6,000+ |
| Core module coverage | 70%+ | 80%+ |
| losscalculation coverage | 61% | 65%+ |
| Docs site pages | 82+ | 100+ |
| Broken links | 0 | 0 |
| SpotBugs bugs | 0 | 0 |
| PMD violations | 823 | <500 |
| Checkstyle violations | 4,632 | <2,000 |
| REST API endpoints | 0 | 10+ |

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
