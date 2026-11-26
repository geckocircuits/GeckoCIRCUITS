# GeckoCIRCUITS — Java 21 power electronics circuit simulator with REST API

**Stack**: Java 21 (Maven, Spring Boot)

## Branch & Repository Strategy

### Repositories

| Repo | Purpose |
|------|---------|
| [geckocircuits/GeckoCIRCUITS](https://github.com/geckocircuits/GeckoCIRCUITS) | **Upstream** — original repository, community contributions via PR |
| [tinix84/GeckoCIRCUITS](https://github.com/tinix84/GeckoCIRCUITS) | **Fork** — primary development, releases, and experimentation |

### Branches

| Branch | Package | Description | CI gates |
|--------|---------|-------------|----------|
| [geckocircuits/main](https://github.com/geckocircuits/GeckoCIRCUITS/tree/main) | `ch.technokrat.gecko` | Upstream stable v2.02 baseline. Reference-quality codebase. Contributions via PR only. | Compile + unit tests + 21 regression |
| [geckocircuits/dev](https://github.com/geckocircuits/GeckoCIRCUITS/tree/dev) | `ch.technokrat.gecko` | Upstream hotfix/maintenance branch. PRs to main only after CI passes. | Compile + unit tests |
| [geckocircuits/main_gecko2026](https://github.com/geckocircuits/GeckoCIRCUITS/tree/main_gecko2026) | `gecko` | Mirror of `tinix84/main`. Experimental branch for community contributions. **Never merged to upstream main.** | Compile + all modules + 21 regression tests + static analysis |
| [tinix84/main](https://github.com/tinix84/GeckoCIRCUITS/tree/main) | `gecko` | Fork stable branch. Releases are tagged here. Code has passed regression testing. | Compile + all modules + 21 regression tests |
| [tinix84/dev](https://github.com/tinix84/GeckoCIRCUITS/tree/dev) | `gecko` | Fork development branch. Active development, fixes, features. | Compile + unit tests + 21 regression tests |

### Merge Flow

```
tinix84/dev ──PR──> tinix84/main ──sync (v* tag)──> geckocircuits/main_gecko2026
                                                           ↑
                                                community PRs

geckocircuits/dev ──PR──> geckocircuits/main
        ↑
   hotfix PRs (e.g., #49)
```

- `tinix84/dev` → `tinix84/main`: via PR, gated by 21 regression tests (1% tolerance vs v2.02 golden refs)
- `tinix84/main` → `main_gecko2026`: automated via `.github/workflows/sync-to-upstream.yml` on `v*` tags (requires `UPSTREAM_PAT` secret)
- `geckocircuits/dev` → `geckocircuits/main`: via PR (hotfixes + regression framework ported to `ch.technokrat.gecko`)
- Community → `main_gecko2026`: via PR with CI gates
- Community → `geckocircuits/main`: via PR, must not break v2.02 compatibility

### Package History

| Codebase | Java package | Java version | Notes |
|----------|-------------|--------------|-------|
| geckocircuits/main, geckocircuits/dev | `ch.technokrat.gecko` | Java 8 | Original package structure, v2.02 era |
| tinix84/dev, tinix84/main, main_gecko2026 | `gecko` | Java 21 | Refactored package (post-v2.02) |

### Archive Branches

| Branch | SHA | Purpose |
|--------|-----|---------|
| [geckocircuits/main_gecko2026_old](https://github.com/geckocircuits/GeckoCIRCUITS/tree/main_gecko2026_old) | `fde75701` | Backup of original `main_gecko2026` (`ch.technokrat.gecko` package, pre-restructure) |

## Build & Test
```bash
mvn clean package assembly:single        # Build main JAR with dependencies
mvn test                                 # Run tests (main project only, excludes regression)
mvn test -Pregression                    # Run ONLY regression tests (21 topologies, needs Xvfb)
mvn -f pom-reactor.xml test              # Run ALL modules (main + simulation-core + rest-api)
mvn -f pom-reactor.xml test -pl src/modules/gecko-simulation-core  # Single sub-module
mvn -f pom-reactor.xml test -pl src/modules/gecko-rest-api
```

## Regression Test Framework
- 21 `.ipes` education circuits in `src/test/resources/ipes/education/`
- Golden reference CSVs in `src/test/resources/golden/` (generated from v2.02)
- `RegressionTest.java`: JUnit 5 `@ParameterizedTest`, `@Tag("regression")`, 2-min timeout per circuit
- `GoldenReferenceHelper.java`: CSV read/write + 1% relative tolerance comparison
- `GoldenRefGenerator.java`: tool to regenerate golden CSVs from any build
- `scripts/generate-golden-refs.sh`: shell script to regenerate from v2.02 baseline
- Regenerate golden refs: run on a machine with desktop (v2.02 GUI doesn't start under WSL2/Xvfb)

## Key Documents
Each kind of statement has one owner (`~/.claude/doc-authority-map.md`):
- [CONTEXT.md](CONTEXT.md): what domain terms mean
- [ADRs](docs/adr/README.md): why a decision was made (indexed in the Decision Log below)
- [PRD](docs/prd.md): what the product must do
- [Architecture](docs/architecture.md): where things live (pointers only)
- [Releasing](docs/releasing.md): how to cut a release
- [GitHub issues and milestones](https://github.com/tinix84/GeckoCIRCUITS/milestones): scope, status, and plans

Never record counts, status, or progress in tracked docs. Measure them, or put them in an issue.
Step-by-step plans are temporary: keep them in the gitignored `docs/superpowers/plans/`.

## Skills
Central pool at `/home/tinix/claude_wsl/agents_pool/`:
```bash
python -m src.cli list           # List all skills
python -m src.cli run sw-arch .  # Run architecture analysis
```

## Task Protocol
1. Run tests: `mvn test` (main), `mvn -f pom-reactor.xml test` (all modules)
2. Run regression tests before merging to main: `mvn test -Pregression`
3. Update docs on push: `mkdocs build` / `mkdocs gh-deploy --force`

## Important Notes
- Multi-module: main (pom.xml) + simulation-core + rest-api (pom-reactor.xml)
- .form files: edit with NetBeans GUI Designer only
- .ipes files: gzip-compressed circuit files
- Static-analysis targets: #64 (PMD), #65 (Checkstyle)
- Docs site: https://tinix84.github.io/GeckoCIRCUITS/
- REST API contract: served at `/api-docs`, with a snapshot in `docs/api/openapi.json`
- `CorePackageValidationTest` enforces zero GUI imports in gecko-simulation-core

## Python Environment

**Do not create `.venv` directories.** Use conda instead:
- `conda activate ntbees2` (default)
- `conda activate social_env` (social media work)

Never run `python -m venv`, `uv venv`, or `virtualenv`.

## Decision Log

| Date | Decision |
|---|---|
| 2026-11-04 | [ADR-0001: Circuit preparation admission contract](docs/adr/0001-circuit-preparation-admission.md) |
| 2026-11-04 | [ADR-0002: Circuit preparation interface](docs/adr/0002-circuit-preparation-interface.md) |
| 2026-11-07 | [ADR-0003: CONTROL dependency ordering](docs/adr/0003-control-dependency-ordering.md) |
| 2026-11-07 | [ADR-0004: Recorded signal analysis](docs/adr/0004-recorded-signal-analysis.md) |
| 2026-11-08 | [ADR-0005: Shared CONTROL delay history](docs/adr/0005-shared-control-delay-history.md) |
| 2026-11-09 | [ADR-0006: Scoped simulation retrieval](docs/adr/0006-scoped-simulation-retrieval.md) |
| 2026-11-09 | [ADR-0007: Local simulation session](docs/adr/0007-local-simulation-session.md) |
| 2026-11-09 | [ADR-0008: Controlled source compatibility](docs/adr/0008-controlled-source-compatibility.md) |
| 2026-11-12 | [ADR-0010: Pull-based frontend and single-owner session](docs/adr/0010-pull-based-frontend-single-owner-session.md) |
| 2026-11-13 | [ADR-0011: Numerical reference compatibility](docs/adr/0011-numerical-reference-compatibility.md) |
| 2026-11-17 | [ADR-0012: Isolated electrical groups](docs/adr/0012-isolated-electrical-groups.md) |
| 2026-02-12 | [ADR-0013: GUI-free simulation core](docs/adr/0013-gui-free-simulation-core.md) |
| 2026-02-05 | [ADR-0014: Java solver with optional native sparse backend](docs/adr/0014-java-solver-with-optional-native-sparse.md) |
| 2026-11-24 | [ADR-0016: Locate diode switching inside a step with electrical sub-steps](docs/adr/0016-zero-crossing-event-location.md) |
| 2026-11-24 | [ADR-0017: Find periodic steady state by brute-force period iteration](docs/adr/0017-periodic-steady-state.md) |
| 2026-11-24 | [ADR-0018: Adapt the electrical step between fixed CONTROL grid points](docs/adr/0018-adaptive-electrical-step.md) |
| 2026-11-24 | [ADR-0019: Headless circuit import writer](docs/adr/0019-headless-circuit-import-writer.md) |
| 2026-11-24 | [ADR-0020: Native linear AC analysis](docs/adr/0020-native-linear-ac-analysis.md) |
| 2026-11-24 | [ADR-0021: Order the enabled LU like the dense reference or like EJML](docs/adr/0021-sparse-numerical-compatibility.md) |
| 2026-11-24 | [ADR-0022: Native ASC hierarchy and scalar bindings](docs/adr/0022-native-asc-hierarchy.md) |
