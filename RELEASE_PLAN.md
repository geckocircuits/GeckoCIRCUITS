# GeckoCIRCUITS Release Plan

## Overview

This document outlines the release strategy for GeckoCIRCUITS based on semantic versioning aligned with the upstream project. The fork maintains v2.x series compatibility while adding modern features (GUI-free core, REST API, CI/CD).

## Version Lineage

**Upstream (geckocircuits/GeckoCIRCUITS):**
- v2.02 - Last upstream release (not in this fork)

**This Fork (tinix84/GeckoCIRCUITS):**
- v2.03-spotbugs-clean (Jan 31, 2026) - All 1,096 SpotBugs violations fixed
- v2.04-repo-reorg (Feb 1, 2026) - Repository reorganization with JDK 21 workflow
- v2.10.0+ - Modern development (GUI-free core, REST API, automation)
- v3.0.0 - Future "Marketing WOW" release (complete REST API platform)

## Current Status

- **Latest Release**: v2.17.0 (commit 6db364f1, Feb 14, 2026)
- **Latest Development**: Sprint 5 Phase 2A completed (commit 89b24ce5, Feb 17, 2026)
- **Next Release**: v2.18.0 (planned Q2 2026 - Real solver integration + simulation control API)
- **Ground Zero**: v2.10.0 (Java 21 migration, commit fd484fe1)
- **Total Releases**: 8 versions (v2.10.0 → v2.17.0)

---

## Released Versions (v2.10.0 → v2.17.0)

### v2.10.0 - Java 21 Migration ⚡ GROUND ZERO
**Release Date**: Feb 14, 2026
**Commit**: `fd484fe1`

**Major Changes:**
- Upgrade Maven compiler source/target to Java 21
- Fix URI.toURL() deprecated method calls
- Update SyntaxPane to v1.3.0 for Java 21 compatibility
- Restore Java Block functionality for Java 21
- Merge PR #1 feat/java21 branch

**System Requirements:**
- Java 21 or later (OpenJDK/Temurin recommended)

**Significance**: Foundation for all modern development - YOUR GROUND ZERO

---

### v2.11.0 - Core Module Foundation
**Release Date**: Feb 14, 2026
**Commit**: `f830c95f`

**Major Features:**
- gecko-simulation-core module created (GUI-free simulation engine)
- Math package migrated (7 classes: matrix operations, LU decomposition, FFT)
- Datacontainer package migrated (11 classes: signal storage, optimized caching)
- Signal analysis utilities (CharacteristicsCalculator, FourierGUIless, Cispr16Fft)
- Native C integration (7 classes, JNI bridge for scientific computing)
- I18n support (SelectableLanguages, 43 languages)
- Docker packaging for REST API

**Key Metrics:**
- Core module: 50+ classes migrated
- Core tests: 200+ tests
- JaCoCo coverage: 30%+ enforced
- Zero GUI imports (CorePackageValidationTest)

**Significance**: First headless-capable simulation engine

---

### v2.12.0 - Static Analysis and Code Quality Sprint
**Release Date**: Feb 14, 2026
**Commit**: `d69f9f5a`

**Major Changes:**
- PMD configuration (pmd-ruleset.xml with quickstart rules, 10 exclusions)
- Checkstyle configuration (checkstyle.xml, 150-char lines, relaxed naming)
- Fixed 2,620 PMD violations (empty blocks, unused code, stray imports)
- Fixed 694 UnnecessaryFullyQualifiedName violations across 111 files
- Fixed 565 style violations (UselessParentheses, UnnecessaryImport, UnnecessaryModifier)
- Strip trailing whitespace from 974 Java source files
- Third-party code exclusions (com/intel/mkl/)

**Key Metrics:**
- PMD violations: 3,443 → 823 (-76% reduction)
- Checkstyle violations: 56,673 → 4,632 (-92% reduction)
- SpotBugs: 0 bugs (maintained)

**Significance**: Established code quality baseline and CI enforcement

---

### v2.13.0 - Terminal and Component Package Migration
**Release Date**: Feb 14, 2026
**Commit**: `7a81b5ce`

**Major Features:**
- Circuit terminal package migrated (3 classes, 138 tests)
- Circuit component package migrated (3 classes, 206 tests)
- TokenMap migrated (41 tests, .ipes circuit file parsing)
- ComponentIdentifiable interface for deserialization
- SerializationUtils for ASCII format (.ipes file I/O)
- Circuit file parsing utilities complete in core

**Key Metrics:**
- Core module: 100+ classes
- Core tests: 400+ tests
- Zero GUI imports enforced (CorePackageValidationTest)
- Circuit file round-trip: Functional

**Significance**: Core module reaches critical mass for REST API development

---

### v2.14.0 - GeckoFile Migration (Sprint 4a)
**Release Date**: Feb 14, 2026
**Commit**: `86c2a65a`

**Major Features:**
- GeckoFile migrated to gecko-simulation-core
- ExternalStorageConverter interface pattern for GUI decoupling
- HiLoData dual-version compatibility resolved
- TokenMap dual-version issue resolved
- Full circuit file I/O available in core module (headless)
- Reflection fallback for backward compatibility

**Key Metrics:**
- Core tests: 500+ tests
- Circuit file round-trip: Fully working headlessly
- GUI decoupling: LossFileAccessor pattern established

**Significance**: Core module can load/save .ipes files without GUI dependencies

---

### v2.15.0 - Loss Calculation Migration (Sprint 4b)
**Release Date**: Feb 14, 2026
**Commit**: `ab5f214a`

**Major Features:**
- Loss calculation calculators migrated (10 classes: switching, conduction, resistor)
- UserParameterCore interface abstraction (9 methods, GUI-free)
- UserParameterCoreImpl with Builder pattern
- Loss curve classes migrated (4 classes, 180+ tests)
  - LossCurve, SwitchingLossCurve, LeitverlusteMesskurve
- DetailedLossLookupTable with bilinear interpolation
- LossFileAccessor pattern for GUI decoupling
- Temperature-dependent loss modeling
- JUnit 4 → JUnit 5 conversion for all tests

**Key Metrics:**
- Core tests: 700+ tests
- Loss calculation: Fully headless-capable
- TokenMap serialization: Fixed format compatibility

**Significance**: Power electronics loss modeling available in core module

---

### v2.16.0 - REST API Launch (Sprint 5) 🎯
**Release Date**: Feb 14, 2026
**Commit**: `f147d54f`

**Major Features:**
- REST API loss calculation endpoints (3 endpoints):
  - POST /api/v1/loss/switching - Voltage/energy scaling
  - POST /api/v1/loss/conduction - Resistance model loss
  - POST /api/v1/loss/detailed - Temperature-dependent curve interpolation
- REST API circuit file operations (3 endpoints):
  - POST /api/v1/circuits/parse - Upload .ipes files
  - GET /api/v1/circuits/{id}/components - List circuit components
  - POST /api/v1/circuits/{id}/validate - Circuit validation
- Spring Boot 3.2.1 with OpenAPI/Swagger documentation
- Docker packaging ready (multi-stage Alpine JRE 21, ~180MB)
- Uses gecko-simulation-core (DetailedLossLookupTable, curves, calculators)

**Key Metrics:**
- REST API tests: 78 tests passing
- Endpoints: 8 functional
- Docker: Production-ready
- API documentation: Swagger UI available

**Significance**: 🚀 **FIRST PUBLIC REST API RELEASE**

---

### v2.17.0 - Release Automation (CURRENT) 🎯
**Release Date**: Feb 14, 2026
**Commit**: `6db364f1`

**Major Features:**
- GitHub Actions automated release workflow (release.yml)
- Platform-specific build workflows:
  - build-windows.yml - Windows desktop package
  - build-macos.yml - macOS desktop package
  - build-linux-wsl.yml - Linux and WSL packages with X11 support
- Matrix strategy for parallel builds (5 platforms simultaneously)
- Automatic GitHub release creation on version tag push
- Manual dispatch for testing individual platforms
- Distribution packages with platform-specific launchers:
  - Windows: run-gecko.bat
  - Linux: run-gecko-linux.sh
  - macOS: run-gecko-macos.sh
  - WSL: run-gecko-wsl.sh (X11 forwarding)
  - Examples: Circuit files + tutorials
- PMD violations: 823 → 496 (-40%, total -86% from baseline)
- Code quality improvements:
  - ForLoopCanBeForeach (94 violations fixed)
  - CloseResource (52 violations fixed)
  - CompareObjectsWithEquals (31 violations fixed)
  - 6 violation categories eliminated to 0

**Key Metrics:**
- CI/CD: Fully automated release pipeline
- Platforms: 5 distributions per release
- Artifact retention: 30-90 days
- PMD violations: 496 (code-style only)
- SpotBugs: 0 bugs maintained

**Significance**: 🎉 **Release engineering complete - production-grade CI/CD pipeline**

---

## Future Releases Roadmap

### v2.18.0 - v2.2x Series (Q2-Q3 2026)

Incremental REST API feature additions:

**Prerequisites (Completed):**
- ✅ Sprint 5 Phase 1: Loss calculation endpoints (3 endpoints, commit 41fe6900)
- ✅ Sprint 5 Phase 2A: Circuit file operations (6 endpoints, commit 89b24ce5)

**v2.18.0 - Real Solver Integration & Simulation Control**
**Target:** Q2 2026 (6-8 weeks development)
**Focus:** Extract SimulationsKern logic to gecko-simulation-core, enable headless circuit simulation

**Phase 1: Architecture Preparation (Week 1)**
- Create core simulation package structure (solver/, netlist/, coupling/)
- Update HeadlessSimulationEngine interface (pause, resume, detailed progress)
- Test infrastructure (RLC circuits, validation suite, benchmarks)

**Phase 2-5: Solver Migration (Weeks 2-7)**
- Phase 2: Matrix solver migration (LKMatrices → MatrixSolver)
- Phase 3: Netlist building (NetListLK, NetzlisteCONTROL)
- Phase 4: Domain coupling (LK-CONTROL-THERM interactions)
- Phase 5: Real solver integration (replace placeholder in HeadlessSimulationEngine)

**Phase 6: API Enhancements (Week 8)**
- Solver type selection (backward-euler, trapezoidal, gear-shichman)
- SSE streaming for real-time progress updates
- Enhanced progress metrics with ETA
- Parameter override application

**New Simulation Endpoints:**
- PATCH /api/v1/simulations/{id}/pause - Pause simulation
- PATCH /api/v1/simulations/{id}/resume - Resume simulation
- POST /api/v1/simulations/{id}/step - Single time step
- GET /api/v1/simulations/{id}/stream - SSE progress stream

**v2.19.0 - Real-time Data Streaming**
- WebSocket endpoint: ws://api/v1/simulations/{id}/stream
- Real-time oscilloscope data
- Live parameter monitoring

**v2.20.0 - Advanced Analysis Endpoints**
- POST /api/v1/analysis/fft - FFT spectrum
- POST /api/v1/analysis/thd - Total Harmonic Distortion
- POST /api/v1/analysis/rms - RMS calculations

**v2.21.0 - Batch Operations**
- POST /api/v1/batch/parameter-sweep
- POST /api/v1/batch/optimization
- Job progress tracking

---

### v3.0.0 - Complete REST API Platform 🚀 MARKETING WOW
**Target Date**: Q3 2026 (August-September)

**Significance**: 🎯 **MAJOR RELEASE** - First production-ready REST API with breaking changes

**Breaking Changes:**
- REST API versioning: /api/v1 → /api/v2
- Authentication required for all endpoints
- Circuit file format updates (optional metadata fields)
- Deprecated legacy RMI interface removal (if applicable)

#### 1. Complete REST API Endpoints (30+ total)

**Simulation Control:**
- POST /api/v2/simulations - Create new simulation
- POST /api/v2/simulations/{id}/start - Start simulation
- POST /api/v2/simulations/{id}/pause - Pause simulation
- POST /api/v2/simulations/{id}/stop - Stop simulation
- GET /api/v2/simulations/{id}/status - Get simulation status
- GET /api/v2/simulations/{id}/results - Get simulation results
- DELETE /api/v2/simulations/{id} - Delete simulation

**Real-time Data Streaming:**
- WebSocket: ws://api/v2/simulations/{id}/stream
- Real-time oscilloscope data
- Live parameter monitoring
- Event notifications (convergence, warnings, errors)

**Advanced Analysis:**
- POST /api/v2/analysis/fft - FFT spectrum analysis
- POST /api/v2/analysis/thd - Total Harmonic Distortion
- POST /api/v2/analysis/rms - RMS value calculation
- POST /api/v2/analysis/power-quality - Power quality metrics
- POST /api/v2/analysis/bode - Bode plot generation

**Batch Operations:**
- POST /api/v2/batch/parameter-sweep - Parameter sweep automation
- POST /api/v2/batch/optimization - Optimization runs
- GET /api/v2/batch/{id}/progress - Batch job progress
- GET /api/v2/batch/{id}/results - Batch results

**Circuit Management:**
- POST /api/v2/circuits - Upload circuit
- GET /api/v2/circuits/{id} - Get circuit details
- PUT /api/v2/circuits/{id} - Update circuit
- DELETE /api/v2/circuits/{id} - Delete circuit
- GET /api/v2/circuits - List circuits (pagination, search)

#### 2. Security & Authentication
- JWT-based authentication
- API key management
- Rate limiting (100 req/min per user, configurable)
- Role-based access control (RBAC)
- OAuth2 integration (Google, GitHub, Microsoft)
- Audit logging

#### 3. Production Infrastructure
- Horizontal scaling support (stateless API design)
- Redis caching layer
- PostgreSQL database for metadata
- Prometheus metrics export
- ELK stack logging integration
- Health check endpoints (/health, /metrics, /info)
- Graceful shutdown
- Request timeout management
- Circuit breaker pattern

#### 4. Documentation & Developer Experience
- Interactive API documentation (Swagger UI)
- Postman collection with examples
- Client SDK generation (Java, Python, JavaScript)
- Tutorial: "Building a Web UI for GeckoCIRCUITS"
- Tutorial: "Automated Testing with REST API"
- Tutorial: "Parameter Sweep Automation"
- Video demonstrations
- Migration guide from v1 API

**Key Metrics:**
- REST API endpoints: 30+ functional
- WebSocket: Real-time streaming <100ms latency
- Performance: <100ms response time (p95)
- Uptime: 99.9% SLA target
- Documentation: 100% API coverage
- Test coverage: 80%+ for API layer

**Timeline:**
- Sprint 6: Simulation control endpoints (6 weeks)
- Sprint 7: WebSocket streaming (4 weeks)
- Sprint 8: Authentication & security (4 weeks)
- Sprint 9: Advanced analysis endpoints (4 weeks)
- Sprint 10: Production infrastructure (6 weeks)
- Sprint 11: Documentation & testing (3 weeks)

**Total Estimated Effort:** 6-7 months

---

### v3.1.0 - Web UI Launch (Target: Q4 2026)

**Significance:** Browser-based circuit editor and simulator

**Major Features:**
- React 18 + TypeScript web application
- Circuit editor with drag-and-drop components
- Real-time oscilloscope visualization (D3.js)
- Parameter editing and tuning
- Circuit library browser
- Tutorial integration
- Responsive design (desktop, tablet)
- PWA support for offline use

**Technologies:**
- React 18 + TypeScript
- Material-UI (MUI) components
- D3.js for visualization
- WebSocket for real-time data
- Redux for state management

**Key Metrics:**
- Browser support: Chrome, Firefox, Safari, Edge (last 2 versions)
- Mobile support: Tablet-optimized (iPad, Android tablets)
- Performance: 60 FPS oscilloscope rendering
- Accessibility: WCAG 2.1 Level AA

---

### v3.2.0 - Cloud Deployment (Target: Q1 2027)

**Significance:** Multi-tenant SaaS platform

**Major Features:**
- AWS/Azure/GCP deployment scripts
- Kubernetes orchestration (Helm charts)
- Auto-scaling based on load (HPA + cluster autoscaler)
- Multi-tenant isolation (namespace-based)
- User workspace management
- Circuit sharing and collaboration
- Marketplace for circuit libraries
- Usage analytics and billing integration

**Infrastructure:**
- Kubernetes cluster (EKS/AKS/GKE)
- Terraform/Pulumi deployment
- CI/CD with GitHub Actions + ArgoCD
- Monitoring with Grafana + Prometheus
- Cost optimization (spot instances, right-sizing)
- Backup and disaster recovery

---

### v4.0.0 - Machine Learning Integration (Target: Q2 2027)

**Significance:** AI-assisted circuit design and optimization

**Major Features:**
- Circuit optimization using reinforcement learning
- Automated component selection based on specifications
- Anomaly detection in simulation results
- Predictive maintenance modeling
- Training data generation from simulation runs
- Pre-trained models for common topologies (buck converter, inverters, etc.)
- Neural network surrogate models for fast approximations

**Technologies:**
- TensorFlow/PyTorch integration
- Python microservice for ML inference
- GPU acceleration support (CUDA)
- Model versioning with MLflow
- Model deployment with TorchServe/TF Serving

**Use Cases:**
- Design optimization: "Find optimal component values for target efficiency"
- Fault prediction: "Predict component failure based on operating conditions"
- Fast simulation: "Replace slow thermal model with neural network"

---

## Long-Term Vision (2027-2028)

### Educational Platform Expansion
- Interactive tutorials with embedded simulator
- Certification programs (Power Electronics Fundamentals, etc.)
- Virtual laboratory for universities
- Competition platform for students (circuit design challenges)
- Integration with LMS (Moodle, Canvas, Blackboard)
- SCORM-compliant content packages

### Industry Partnerships
- Semiconductor vendor integrations (Infineon, Wolfspeed, ON Semi, STMicro)
- Component library partnerships
- Enterprise licensing model (per-seat, per-server, site license)
- Professional support tiers (email, phone, dedicated engineer)
- Training and consulting services

### Research Collaboration
- Academic paper citation tracking
- Research dataset sharing (simulation results, validation data)
- Reproducible research workflows (Docker + circuit files)
- Integration with research tools (Jupyter, MATLAB Online, Mathematica)
- Grant-funded development partnerships

---

## Release Frequency Target

**Goal:** Predictable release cadence for stakeholder planning

- **Major releases (X.0.0):** Annually (breaking changes, major features, marketing events)
- **Minor releases (X.Y.0):** Quarterly (new features, enhancements, backward-compatible)
- **Patch releases (X.Y.Z):** As needed (bug fixes, security updates, hotfixes)

**Example Timeline:**
- 2026 Q1: v2.10.0 - v2.13.0 (core module foundation)
- 2026 Q2: v2.14.0 - v2.17.0 (loss calculation + REST API + automation)
- 2026 Q3: v2.18.0 - v2.21.0 (incremental REST API features)
- 2026 Q3: v3.0.0 (complete REST API platform - MARKETING WOW)
- 2026 Q4: v3.1.0 (web UI launch)
- 2027 Q1: v3.2.0 (cloud deployment)
- 2027 Q2: v4.0.0 (machine learning integration)

---

## Version Numbering Scheme

Following Semantic Versioning 2.0.0:

- **MAJOR** (X.0.0): Incompatible API changes, major architectural shifts, marketing milestones
- **MINOR** (X.Y.0): New features, backward-compatible changes, significant milestones
- **PATCH** (X.Y.Z): Bug fixes, security updates, backward-compatible fixes

**Guidelines:**
- Each milestone-based sprint = MINOR version bump
- Multiple smaller sprints can be combined into one MINOR version
- REST API endpoint additions = MINOR bump
- API breaking changes = MAJOR bump
- Critical bug fixes = PATCH release (out-of-band)

**Examples:**
- v2.10.0 → v2.11.0: New core module (major feature)
- v2.16.0 → v2.17.0: Release automation (significant milestone)
- v2.17.0 → v3.0.0: Complete REST API with breaking changes (major)
- v3.0.0 → v3.0.1: Fix JWT authentication bug (patch)

---

## Release Checklist Template

For each release:

### Pre-Release
- [ ] All tests passing (mvn -f pom-reactor.xml test)
- [ ] Documentation updated (PRD.md, ARCHITECTURE.md, CLAUDE.md, RELEASE_PLAN.md)
- [ ] Changelog drafted (CHANGELOG.md)
- [ ] Version numbers updated in pom.xml files
- [ ] SpotBugs: 0 bugs (mvn spotbugs:check)
- [ ] JaCoCo coverage: ≥30% for core (mvn verify)
- [ ] Breaking changes documented (if MAJOR release)
- [ ] Migration guide written (if MAJOR release)

### Release
- [ ] Create annotated tag: `git tag -a vX.Y.Z -m "vX.Y.Z: Release title and description"`
- [ ] Push tag: `git push origin vX.Y.Z`
- [ ] Monitor GitHub Actions build (5-10 minutes)
- [ ] Verify all 5 platform packages uploaded to release
- [ ] Test download and installation on each platform

### Post-Release
- [ ] Update documentation site (https://tinix84.github.io/GeckoCIRCUITS/)
- [ ] Announce release (mailing list, forum, social media, blog post)
- [ ] Close milestone in issue tracker
- [ ] Update RELEASE_PLAN.md for next version
- [ ] Create milestone for next release
- [ ] Update Docker Hub images (if REST API changed)

---

## Release Notes Template

```markdown
# GeckoCIRCUITS vX.Y.Z

**Release Date**: YYYY-MM-DD

## Highlights

[3-5 sentence summary of major changes and their impact]

## What's New

### Features
- Feature 1 description with rationale
- Feature 2 description with rationale
- Feature 3 description with rationale

### Improvements
- Improvement 1 description
- Improvement 2 description

### Bug Fixes
- Fix 1 description (#issue-number)
- Fix 2 description (#issue-number)

### Documentation
- Documentation improvement 1
- Documentation improvement 2

## Breaking Changes

[List any breaking changes with migration instructions, or "None"]

## Deprecations

[List any deprecated features with replacement suggestions, or "None"]

## Downloads

- **Windows**: `GeckoCIRCUITS-X.Y.Z-windows.zip` (SHA256: ...)
- **Linux**: `GeckoCIRCUITS-X.Y.Z-linux.zip` (SHA256: ...)
- **macOS**: `GeckoCIRCUITS-X.Y.Z-macos.zip` (SHA256: ...)
- **WSL**: `GeckoCIRCUITS-X.Y.Z-wsl.zip` (SHA256: ...)
- **Examples**: `GeckoCIRCUITS-X.Y.Z-examples.zip` (SHA256: ...)

## Installation

See platform-specific READMEs in each package.

## System Requirements

- Java 21 or later
- For WSL: X server (VcXsrv, Xming, or WSLg)
- Memory: 4 GB RAM (8 GB recommended)
- Storage: 200 MB for application

## Known Issues

- Issue 1 description and workaround
- Issue 2 description and workaround

[Or "None"]

## Contributors

Thank you to all contributors for this release:
- Contributor 1 (@username)
- Contributor 2 (@username)

## Full Changelog

[Link to detailed changelog or commit range]
[Example: https://github.com/tinix84/GeckoCIRCUITS/compare/v2.16.0...v2.17.0]

## Support

- Documentation: https://tinix84.github.io/GeckoCIRCUITS/
- Issues: https://github.com/tinix84/GeckoCIRCUITS/issues
- Discussions: https://github.com/tinix84/GeckoCIRCUITS/discussions
```

---

## Appendix: Commit Statistics

**Since v2.10.0 (Ground Zero → v2.17.0):**
- Total commits: 60+
- Duration: 6 months (Aug 2025 → Feb 2026)
- Major sprints: 5 (Core foundation, Static analysis, Terminal/component, GeckoFile, Loss calculation, REST API, Automation)
- Files changed: 500+ files
- Lines changed: 15,000+ lines
- Test coverage increase: +1,400 tests in core module

**Key Achievements:**
- ✅ Core module: 183 classes, 1,686 tests (30%+ coverage enforced)
- ✅ REST API: 8 endpoints, 78 tests, Docker ready
- ✅ Code quality: 86% PMD reduction (3,443 → 496)
- ✅ CI/CD: Fully automated release pipeline (5 platforms)
- ✅ Zero SpotBugs violations maintained
- ✅ Zero GUI imports in core (enforced by tests)

---

## Contact

For questions about releases:
- Open an issue: https://github.com/tinix84/GeckoCIRCUITS/issues
- Discussions: https://github.com/tinix84/GeckoCIRCUITS/discussions
- Email: [maintainer email if public]

---

**Document Version**: 2.0
**Last Updated**: 2026-02-14
**Maintainer**: tinix84
