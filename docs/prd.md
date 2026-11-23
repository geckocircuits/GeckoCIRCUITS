# GeckoCIRCUITS Product Requirements

What the product must do. Status, scope and plans are tracked in [GitHub issues and milestones](https://github.com/tinix84/GeckoCIRCUITS/milestones). Decisions are recorded in [ADRs](adr/README.md), and domain terms are defined in [CONTEXT.md](https://github.com/tinix84/GeckoCIRCUITS/blob/main/CONTEXT.md).

## 1. Vision

GeckoCIRCUITS is an open-source circuit simulator for power electronics, originally developed at ETH Zurich. It simulates electrical, thermal, magnetic and CONTROL domains together. It keeps the mature desktop application for interactive work and adds a headless REST API for automation, cloud deployment and teaching ([ADR-0013](adr/0013-gui-free-simulation-core.md)).

## 2. Users

| Persona | Interface | Needs |
|---------|-----------|-------|
| Power electronics researchers | Desktop GUI, MATLAB/Simulink | Interactive circuit design and co-simulation |
| University students | Desktop GUI, browser | Learning through tutorials and examples |
| Automation engineers | REST API, generated clients | Batch parameter sweeps, loss calculations, CI validation |
| Educators | Documentation site | Curated tutorials, examples and articles |

## 3. Platforms

The desktop application must run on Windows 10/11, Linux, macOS (Intel and Apple Silicon) and WSL2 (WSLg).
Each platform package contains the fat JAR, a launcher script with suitable JVM options, and a README.

System requirements: OpenJDK 21 or later, 4 GB RAM (8 GB recommended), and a display of at least 1280×720. HiDPI displays are supported through the launcher's `--hidpi` option.

## 4. Product components

### 4.1 Desktop application
- Schematic editor, oscilloscope, and analysis tools (FFT, XY, Bode)
- Electrical, thermal, magnetic and CONTROL domains in one circuit
- MATLAB/Simulink integration over RMI and memory-mapped files
- GeckoSCRIPT (JavaScript) for scripting and parameter sweeps
- Opens and saves Circuit files (`.ipes`)

### 4.2 Simulation core
- Runs any supported Circuit file without a GUI or display
- Contains no GUI dependency ([ADR-0013](adr/0013-gui-free-simulation-core.md))
- Is the single engine used by both the desktop and the REST API ([ADR-0007](adr/0007-local-simulation-session.md), [ADR-0010](adr/0010-pull-based-frontend-single-owner-session.md))
- Results agree with the v2.02 Golden references within 1% ([ADR-0011](adr/0011-numerical-reference-compatibility.md))

### 4.3 REST API
- Loss calculation, Circuit file operations, Simulation jobs and Batches, progress streaming, and signal analysis
- Optional authentication with API keys or JWT, and role-based access
- The OpenAPI contract is served at `/api-docs`, and the snapshot at [`docs/api/openapi.json`](api/openapi.json) is the source for generated clients
- Ships as a Docker image

### 4.4 Documentation site
- Published at https://tinix84.github.io/GeckoCIRCUITS/
- Getting started guide, tutorials, examples, articles, and the API reference
- `mkdocs build --strict` passes with no broken links

## 5. Simulation features

- **Electrical:** R, L, C, diodes, MOSFETs, IGBTs, thyristors, transformers
- **Thermal:** junction temperatures, thermal networks, heatsinks
- **Magnetic:** reluctance models and coupled inductors
- **CONTROL:** PI/PID, integrators, limiters, PWM, logic and state machines, signal processing
- **Integration methods:** Backward Euler, trapezoidal, Gear-Shichman
- **Export:** CSV, images, SVG

## 6. Quality requirements

- The 21 education circuits must match their v2.02 Golden references within 1% before any merge to `main`.
- The simulation core must contain no GUI imports, as checked by `CorePackageValidationTest`.
- SpotBugs must report zero bugs.
- The build must enforce a coverage floor for the simulation core ([#66](https://github.com/tinix84/GeckoCIRCUITS/issues/66)).
- Static-analysis targets are tracked in [#64](https://github.com/tinix84/GeckoCIRCUITS/issues/64) (PMD) and [#65](https://github.com/tinix84/GeckoCIRCUITS/issues/65) (Checkstyle).

## 7. File formats

| Extension | Content |
|-----------|---------|
| `.ipes` | Circuit file (gzip-compressed) |
| `.scl` | GeckoSCRIPT source |
| `.gmd` | GeckoMAGNETICS design |
| `.prp` | Application properties |

## 8. License

GPL v3. See `LICENSE`.
