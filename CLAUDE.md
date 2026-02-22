# GeckoCIRCUITS — Java 21 power electronics circuit simulator with REST API

**Stack**: Java 21 (Maven, Spring Boot)

## Build & Test
```bash
mvn clean package assembly:single        # Build main JAR with dependencies
mvn test                                 # Run tests (main project only)
mvn -f pom-reactor.xml test              # Run ALL modules (main + simulation-core + rest-api)
mvn -f pom-reactor.xml test -pl src/modules/gecko-simulation-core  # Single sub-module
mvn -f pom-reactor.xml test -pl src/modules/gecko-rest-api
```

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
2. Update docs on push: `mkdocs build` / `mkdocs gh-deploy --force`

## Important Notes
- Multi-module: main (pom.xml) + simulation-core + rest-api (pom-reactor.xml)
- .form files: edit with NetBeans GUI Designer only
- .ipes files: gzip-compressed circuit files
- Tests: 7,434 total (5,373 main + 1,837 core + 224 API)
- Core module: 216 source classes, 80 test files
- Static analysis: SpotBugs 0 bugs, PMD <500 violations (496 current)
- Docs site: https://tinix84.github.io/GeckoCIRCUITS/
- REST API v3.0.0: 32 endpoints, Spring Boot 3.2.1, Docker-ready
- `CorePackageValidationTest` enforces zero GUI imports in gecko-simulation-core
