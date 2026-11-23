# Where things live

This page only points to where things are. The code shows how they work, [ADRs](adr/README.md) explain why, and [CONTEXT.md](https://github.com/tinix84/GeckoCIRCUITS/blob/main/CONTEXT.md) defines the terms.

## Builds

| Path | What |
|------|------|
| `pom.xml` | Desktop application (main build) |
| `pom-reactor.xml` | Reactor build covering the desktop, simulation core and REST API |
| `src/modules/gecko-simulation-core/` | GUI-free simulation core ([ADR-0013](adr/0013-gui-free-simulation-core.md)) |
| `src/modules/gecko-rest-api/` | Spring Boot REST API |
| `.github/workflows/` | CI, docs deploy, release and upstream sync |

## Desktop (`src/main/java/gecko/`)

| Path | What |
|------|------|
| `GeckoSim.java` | Entry point and operating-mode selection |
| `GeckoRemote*.java`, `GeckoCustomMMF.java` | MATLAB/Octave RMI and memory-mapped-file integration |
| `geckocircuits/allg/DesktopSimulationController.java` | Picks the Core or Legacy backend (`-Dgecko.backend`) |
| `geckocircuits/circuit/` | Circuit components, editors, Legacy backend matrices |
| `geckocircuits/control/` | CONTROL blocks and their panels |
| `geckocircuits/newscope/`, `geckocircuits/scope/` | Oscilloscope |
| `expressionscripting/`, `geckoscript/` | GeckoSCRIPT |
| `i18n/` | Translations |

## Simulation core (`src/modules/gecko-simulation-core/src/main/java/gecko/core/`)

| Path | What |
|------|------|
| `io/` | Circuit file parsing, topology and import issues |
| `preparation/` | Circuit preparation and Headless readiness ([ADR-0001](adr/0001-circuit-preparation-admission.md), [ADR-0002](adr/0002-circuit-preparation-interface.md)) |
| `simulation/` | Engine, Simulation session, Electrical time step, CONTROL runtime |
| `simulation/solver/` | Matrix solver and initial conditions |
| `circuit/` | Matrix stampers, netlist, loss calculation |
| `control/calculators/` | CONTROL calculators |
| `signal/` | Signal analysis (characteristics, Fourier) |

## REST API (`src/modules/gecko-rest-api/src/main/java/gecko/rest/`)

| Path | What |
|------|------|
| `controller/` | HTTP endpoints. The full contract is served at `/api-docs`. |
| `service/` | Simulation jobs, Batches, progress publishing |
| `config/` | Security, WebSocket and rate-limit configuration |
| `model/` | Request and response types |

The OpenAPI snapshot is [`docs/api/openapi.json`](api/openapi.json) (`scripts/snapshot-openapi.sh`). Clients are generated into `clients/` (`scripts/generate-sdks.sh`). Docker files are `src/modules/gecko-rest-api/Dockerfile` and `docker/`.

## Tests

| Path | What |
|------|------|
| `src/test/java/gecko/systemtests/RegressionTest.java` | The 21 education circuits checked against their Golden references |
| `src/test/resources/ipes/education/`, `src/test/resources/golden/` | Regression circuits and Golden references |
| `src/test/java/gecko/systemtests/SimulinkCoreParityTest.java` | Core vs Legacy backend parity |
| `src/test/java/gecko/geckocircuits/core/CorePackageValidationTest.java` | No GUI imports in the core |

## Documentation site

| Path | What |
|------|------|
| `mkdocs.yml`, `docs/` | Site source |
| `resources/` | Tutorial, example and article sources, copied into `docs/` by `scripts/sync-docs.py` |
| `scripts/generate-api-docs.py` | Generates `docs/api/core/` from the core module |
| `docs/releases/` | Release notes, one page per tag |
