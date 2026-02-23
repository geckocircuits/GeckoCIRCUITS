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
| [geckocircuits/main](https://github.com/geckocircuits/GeckoCIRCUITS/tree/main) | `ch.technokrat.gecko` | Upstream stable v2.02 baseline. Reference-quality codebase. Contributions via PR only. | Compile + 162 unit tests + 21 regression |
| [geckocircuits/dev](https://github.com/geckocircuits/GeckoCIRCUITS/tree/dev) | `ch.technokrat.gecko` | Upstream hotfix/maintenance branch. PRs to main only after CI passes. | Compile + unit tests |
| [geckocircuits/main_gecko2026](https://github.com/geckocircuits/GeckoCIRCUITS/tree/main_gecko2026) | `gecko` | Mirror of `tinix84/main`. Experimental branch for community contributions. **Never merged to upstream main.** | Compile + all modules + 21 regression tests + static analysis |
| [tinix84/main](https://github.com/tinix84/GeckoCIRCUITS/tree/main) | `gecko` | Fork stable branch. All releases up to v3.0.0. Code has passed regression testing. | Compile + all modules + 21 regression tests |
| [tinix84/dev](https://github.com/tinix84/GeckoCIRCUITS/tree/dev) | `gecko` | Fork development branch. Active development, fixes, features. | Compile + 5,373 unit tests + 21 regression tests |

### Merge Flow

```
tinix84/dev ──PR──> tinix84/main ──sync (v* tag)──> geckocircuits/main_gecko2026
                                                           ↑
                                                community PRs

geckocircuits/dev ──PR──> geckocircuits/main
        ↑
   hotfix PRs (e.g., #49)
```

- `tinix84/dev` → `tinix84/main`: via PR #36, gated by 21 regression tests (1% tolerance vs v2.02 golden refs)
- `tinix84/main` → `main_gecko2026`: automated via `.github/workflows/sync-to-upstream.yml` on `v*` tags (requires `UPSTREAM_PAT` secret)
- `geckocircuits/dev` → `geckocircuits/main`: via PR #50 (hotfixes + regression framework ported to `ch.technokrat.gecko`)
- Community → `main_gecko2026`: via PR with CI gates
- Community → `geckocircuits/main`: via PR, must not break v2.02 compatibility

### Package History

| Codebase | Java package | Java version | Notes |
|----------|-------------|--------------|-------|
| geckocircuits/main, geckocircuits/dev | `ch.technokrat.gecko` | Java 8 | Original package structure, v2.02 era |
| tinix84/dev, tinix84/main, main_gecko2026 | `gecko` | Java 21 | Refactored package (post-v2.02) |

### PR Status

| PR | Repo | Status | Description |
|----|------|--------|-------------|
| [#36](https://github.com/tinix84/GeckoCIRCUITS/pull/36) | tinix84 | Open | dev → main: regression framework + bug #48/#49 fixes |
| [#49](https://github.com/geckocircuits/GeckoCIRCUITS/pull/49) | geckocircuits | Merged → dev | Swing thread safety + daemon thread + phase normalization |
| [#50](https://github.com/geckocircuits/GeckoCIRCUITS/pull/50) | geckocircuits | Open | dev → main: PR #49 + regression framework (ch.technokrat.gecko) |

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
- [PRD](docs/prd.md) — requirements, sprint status, release history
- [Architecture](docs/architecture.md) — module structure, GUI-free boundary
- [Roadmap](docs/roadmap.md) — future releases, GitHub issues, release process

## Sprint Plans
Convention: `docs/sprints/sprint-*.md`

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
- Tests: 7,434 total (5,373 main + 1,837 core + 224 API) + 21 regression
- Core module: 216 source classes, 80 test files
- Static analysis: SpotBugs 0 bugs, PMD <500 violations (496 current)
- Docs site: https://tinix84.github.io/GeckoCIRCUITS/
- REST API v3.0.0: 32 endpoints, Spring Boot 3.2.1, Docker-ready
- `CorePackageValidationTest` enforces zero GUI imports in gecko-simulation-core
