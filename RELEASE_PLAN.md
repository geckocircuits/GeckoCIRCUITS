# GeckoCIRCUITS Release Plan

## Overview

This document outlines the release strategy for GeckoCIRCUITS, identifying major milestones as release candidates based on completed work since v1.0.0 (Feb 11, 2026).

## Current Status

- **Latest Release**: v1.0.0 (commit f5d8fbae, Feb 11, 2026)
- **Commits Since v1.0.0**: 58 commits
- **Current HEAD**: ec32bdcd (Feb 14, 2026)

## Proposed Releases

### v1.1.0 - Core Module Extraction Sprint
**Target Commit**: `7fce3f5d` (already has v1.1.0 tag in history)
**Release Date**: TBD

**Major Features:**
- Multi-module reactor build (pom-reactor.xml)
- Zero-crossing detection improvements
- REST API test fixes
- Maven groupId simplification (ch.technokrat.gecko → gecko)

**Significance**: Foundation for modular architecture

---

### v1.2.0 - GUI-Free Core Module (Phase 1)
**Target Commit**: `f830c95f` "Migrate math tests and datacontainer classes to gecko-simulation-core"
**Estimated Release**: Q1 2026

**Major Features:**
- gecko-simulation-core module created
- Math package migrated (7 classes, matrix operations, FFT)
- Datacontainer package migrated (11 classes, signal storage)
- Signal analysis utilities (3 classes, 29 tests)
- Native C integration (7 classes, 46 tests)
- I18n support (SelectableLanguages)
- Docker packaging for REST API

**Key Metrics:**
- Core module: 50+ classes migrated
- Core tests: 200+ tests
- SpotBugs: 0 bugs
- JaCoCo coverage: 30%+ enforced

**Significance**: First headless-capable simulation engine

---

### v1.3.0 - Static Analysis & Code Quality Sprint
**Target Commit**: `d69f9f5a` "Fix 2,620 PMD violations via auto-fixes and ruleset tuning"
**Estimated Release**: Q1 2026

**Major Features:**
- PMD configuration (pmd-ruleset.xml)
- Checkstyle configuration (checkstyle.xml)
- Fixed 2,620 PMD violations (empty blocks, unused code, etc.)
- Fixed 694 UnnecessaryFullyQualifiedName violations
- Fixed 565 style violations (UselessParentheses, UnnecessaryImport)
- Strip trailing whitespace from 974 files

**Key Metrics:**
- PMD violations: 3,443 → 823 (-76%)
- Checkstyle violations: 56,673 → 4,632 (-92%)
- SpotBugs: 0 bugs (maintained)

**Significance**: Established code quality baseline

---

### v1.4.0 - Core Module Expansion (Terminal & Component)
**Target Commit**: `7a81b5ce` "Complete Tier 1 package migration sprint: terminal + component to gecko-simulation-core"
**Estimated Release**: Q1 2026

**Major Features:**
- Circuit terminal package migrated (3 classes, 138 tests)
- Circuit component package migrated (3 classes, 206 tests)
- TokenMap migrated (41 tests, .ipes file parsing)
- ComponentIdentifiable interface
- SerializationUtils for .ipes file I/O

**Key Metrics:**
- Core module: 100+ classes
- Core tests: 400+ tests
- Zero GUI imports enforced (CorePackageValidationTest)

**Significance**: Core module reaches critical mass for REST API

---

### v1.5.0 - GeckoFile Migration (Sprint 4a)
**Target Commit**: `86c2a65a` "Sprint 4a: Migrate GeckoFile to gecko-simulation-core"
**Estimated Release**: Q2 2026

**Major Features:**
- GeckoFile migrated with ExternalStorageConverter pattern
- HiLoData dual-version compatibility resolved
- TokenMap dual-version issue resolved (partial)
- Full circuit file I/O in core module

**Key Metrics:**
- Core tests: 500+ tests
- Circuit file round-trip: ✅ working

**Significance**: Core module can load/save .ipes files headlessly

---

### v1.6.0 - Loss Calculation Migration (Sprint 4b)
**Target Commit**: `ab5f214a` "Sprint 4b Phase 2B: Complete UserParameter abstraction and curve class migration"
**Estimated Release**: Q2 2026

**Major Features:**
- Loss calculation calculators migrated (10 classes)
- UserParameterCore interface abstraction
- Loss curve classes migrated (4 classes, 180+ tests)
- Detailed loss lookup table
- LossFileAccessor pattern for GUI decoupling

**Key Metrics:**
- Core tests: 700+ tests
- Loss calculation: Fully headless-capable

**Significance**: Power electronics loss modeling available in core

---

### v1.7.0 - REST API Launch (Sprint 5)
**Target Commit**: `f147d54f` "Update documentation after Sprint 5 Phase 1 completion"
**Estimated Release**: Q2 2026

**Major Features:**
- REST API loss calculation endpoints
  - GET /api/v1/loss-calculation/components
  - GET /api/v1/loss-calculation/curves
  - POST /api/v1/loss-calculation/calculate
- REST API circuit file operations
  - POST /api/v1/circuits/parse (upload .ipes)
  - GET /api/v1/circuits/{id}/components
  - POST /api/v1/circuits/{id}/validate
- Spring Boot 3.2.1
- OpenAPI/Swagger documentation
- Docker deployment ready

**Key Metrics:**
- REST API tests: 78 tests
- Endpoints: 8 functional
- Docker: Production-ready

**Significance**: **First public REST API release**

---

### v1.8.0 - Code Quality Sprint (PMD <500)
**Target Commit**: `1e4eaa0a` "Achieve <500 PMD violations target: 823 → 496 (-40%)"
**Estimated Release**: Q2 2026

**Major Features:**
- Fixed 327 PMD violations across multiple phases
- Eliminated 6 violation categories to 0:
  - ForLoopCanBeForeach (94 violations)
  - CloseResource (52 violations)
  - PreserveStackTrace (38 violations)
  - CompareObjectsWithEquals (31 violations)
  - SimplifyBooleanReturns (29 violations)
  - IdenticalCatchBranches (22 violations)
- Fixed resource leaks (try-with-resources)
- Fixed incorrect object comparisons
- Fixed exception handling patterns

**Key Metrics:**
- PMD violations: 823 → 496 (-40%)
- Total reduction from baseline: 3,443 → 496 (-86%)
- SpotBugs: 0 bugs (maintained)

**Significance**: Production-grade code quality

---

### v1.9.0 - Release Automation (CURRENT)
**Target Commit**: `ec32bdcd` "Add GitHub Actions workflows for desktop release packages"
**Estimated Release**: Q2 2026 (Ready NOW)

**Major Features:**
- GitHub Actions automated release workflow
- Platform-specific build workflows:
  - Windows distribution (run-gecko.bat)
  - Linux distribution (run-gecko-linux.sh)
  - macOS distribution (run-gecko-macos.sh)
  - WSL distribution (run-gecko-wsl.sh with X11)
  - Examples package (circuits + tutorials)
- Matrix strategy for parallel builds
- Automatic GitHub release creation on version tags
- Manual dispatch for testing

**Key Metrics:**
- CI/CD: Fully automated
- Platforms: 5 distributions
- Artifact retention: 30-90 days

**Significance**: **Release engineering complete - ready for v2.0.0**

---

## Recommended Release Strategy

### Immediate Actions (February 2026)

1. **Tag v1.9.0 at HEAD** (ec32bdcd)
   ```bash
   git tag -a v1.9.0 -m "v1.9.0: Release automation and desktop packaging"
   git push origin v1.9.0
   ```
   - **Rationale**: Capture current release infrastructure
   - **Triggers**: Automated release workflow will build all 5 platforms
   - **Outcome**: First automated multi-platform release

2. **Test Release Workflow**
   - Monitor GitHub Actions after v1.9.0 push
   - Verify all 5 platform packages build successfully
   - Download and test packages on each platform
   - Verify release notes formatting

### Short-Term (March 2026)

3. **Backfill Historical Tags** (optional)
   ```bash
   # Tag major milestones for documentation purposes
   git tag -a v1.2.0 f830c95f -m "v1.2.0: GUI-free core module foundation"
   git tag -a v1.3.0 d69f9f5a -m "v1.3.0: Static analysis and code quality"
   git tag -a v1.4.0 7a81b5ce -m "v1.4.0: Terminal and component migration"
   git tag -a v1.5.0 86c2a65a -m "v1.5.0: GeckoFile migration (Sprint 4a)"
   git tag -a v1.6.0 ab5f214a -m "v1.6.0: Loss calculation migration (Sprint 4b)"
   git tag -a v1.7.0 f147d54f -m "v1.7.0: REST API launch (Sprint 5)"
   git tag -a v1.8.0 1e4eaa0a -m "v1.8.0: Code quality sprint (<500 PMD)"

   git push origin v1.2.0 v1.3.0 v1.4.0 v1.5.0 v1.6.0 v1.7.0 v1.8.0
   ```
   - **Rationale**: Document architectural evolution
   - **Note**: Won't trigger release builds (commits are old)
   - **Benefit**: Clear version history for changelog generation

### Medium-Term (Q2 2026)

4. **Plan v2.0.0 Release**
   - Target: Complete REST API feature parity with desktop
   - Criteria:
     - [ ] All core packages migrated (circuit components, control blocks)
     - [ ] REST API simulation endpoints (start, step, stop, results)
     - [ ] WebSocket support for real-time data streaming
     - [ ] Authentication/authorization
     - [ ] Rate limiting
     - [ ] Production deployment guide
   - Timeline: 2-3 months

## Release Checklist Template

For each release:

### Pre-Release
- [ ] All tests passing (mvn -f pom-reactor.xml test)
- [ ] Documentation updated (PRD.md, ARCHITECTURE.md, CLAUDE.md)
- [ ] Changelog drafted (CHANGELOG.md)
- [ ] Version numbers updated in pom.xml files
- [ ] SpotBugs: 0 bugs (mvn spotbugs:check)
- [ ] JaCoCo coverage: ≥30% for core (mvn verify)

### Release
- [ ] Create annotated tag: `git tag -a vX.Y.Z -m "vX.Y.Z: Release title"`
- [ ] Push tag: `git push origin vX.Y.Z`
- [ ] Monitor GitHub Actions build
- [ ] Verify all platform packages uploaded
- [ ] Test download and installation on each platform

### Post-Release
- [ ] Update website (https://tinix84.github.io/GeckoCIRCUITS/)
- [ ] Announce release (mailing list, forum, etc.)
- [ ] Close milestone in issue tracker
- [ ] Update RELEASE_PLAN.md for next version

## Version Numbering Scheme

Following Semantic Versioning 2.0.0:

- **MAJOR** (2.0.0): Incompatible API changes, major architectural shifts
- **MINOR** (1.X.0): New features, backward-compatible changes
- **PATCH** (1.0.X): Bug fixes, backward-compatible fixes

**Examples:**
- v1.2.0: New core module (minor - new feature)
- v1.7.0: REST API launch (minor - new feature)
- v2.0.0: Breaking API changes, complete REST API (major)

## Release Notes Template

```markdown
# GeckoCIRCUITS vX.Y.Z

**Release Date**: YYYY-MM-DD

## Highlights

[3-5 sentence summary of major changes]

## What's New

### Features
- Feature 1 description
- Feature 2 description

### Improvements
- Improvement 1 description
- Improvement 2 description

### Bug Fixes
- Fix 1 description
- Fix 2 description

## Breaking Changes

[List any breaking changes, or "None"]

## Downloads

- **Windows**: `GeckoCIRCUITS-X.Y.Z-windows.zip`
- **Linux**: `GeckoCIRCUITS-X.Y.Z-linux.zip`
- **macOS**: `GeckoCIRCUITS-X.Y.Z-macos.zip`
- **WSL**: `GeckoCIRCUITS-X.Y.Z-wsl.zip`
- **Examples**: `GeckoCIRCUITS-X.Y.Z-examples.zip`

## Installation

See platform-specific READMEs in each package.

## Requirements

- Java 21 or later
- For WSL: X server (VcXsrv, Xming, or WSLg)

## Known Issues

[List any known issues, or "None"]

## Contributors

[List contributors for this release]

## Full Changelog

[Link to detailed changelog or commit range]
```

## Release Execution Status

### Completed Releases (2026-02-14)

**Tags Created and Pushed:**
- ✅ v1.2.0 (f830c95f) - GUI-free core module foundation
- ✅ v1.3.0 (d69f9f5a) - Static analysis and code quality sprint
- ✅ v1.4.0 (7a81b5ce) - Terminal and component package migration
- ✅ v1.5.0 (86c2a65a) - GeckoFile migration (Sprint 4a)
- ✅ v1.6.0 (ab5f214a) - Loss calculation migration (Sprint 4b)
- ✅ v1.7.0 (f147d54f) - REST API launch (Sprint 5)
- ✅ v1.8.0 (1e4eaa0a) - Code quality sprint (PMD <500)
- ✅ v1.9.0 (ec32bdcd) - Release automation (CURRENT)

**GitHub Actions Status:**
- 8 release workflows triggered automatically
- All platforms building in parallel (Windows, Linux, macOS, WSL, Examples)
- Check progress: https://github.com/tinix84/GeckoCIRCUITS/actions

**Expected Artifacts:**
- 40 distribution packages total (8 releases × 5 platforms each)
- All packages available in GitHub Releases: https://github.com/tinix84/GeckoCIRCUITS/releases

---

## Future Releases Roadmap

### v2.0.0 - Complete REST API Platform (Target: Q3 2026)

**Significance:** **MAJOR RELEASE** - First production-ready REST API with breaking changes

**Breaking Changes:**
- REST API versioning: /api/v1 → /api/v2
- Authentication required for all endpoints
- Circuit file format updates (optional metadata fields)
- Deprecated legacy RMI interface removal (if applicable)

**Major Features:**

#### 1. Complete REST API Endpoints
- **Simulation Control:**
  - POST /api/v2/simulations - Create new simulation
  - POST /api/v2/simulations/{id}/start - Start simulation
  - POST /api/v2/simulations/{id}/pause - Pause simulation
  - POST /api/v2/simulations/{id}/stop - Stop simulation
  - GET /api/v2/simulations/{id}/status - Get simulation status
  - GET /api/v2/simulations/{id}/results - Get simulation results

- **Real-time Data Streaming:**
  - WebSocket endpoint: ws://api/v2/simulations/{id}/stream
  - Real-time oscilloscope data
  - Live parameter monitoring
  - Event notifications (convergence, warnings, errors)

- **Advanced Analysis:**
  - POST /api/v2/analysis/fft - FFT spectrum analysis
  - POST /api/v2/analysis/thd - Total Harmonic Distortion
  - POST /api/v2/analysis/rms - RMS value calculation
  - POST /api/v2/analysis/power-quality - Power quality metrics

- **Batch Operations:**
  - POST /api/v2/batch/parameter-sweep - Parameter sweep automation
  - POST /api/v2/batch/optimization - Optimization runs
  - GET /api/v2/batch/{id}/progress - Batch job progress

#### 2. Security & Authentication
- JWT-based authentication
- API key management
- Rate limiting (100 req/min per user)
- Role-based access control (RBAC)
- OAuth2 integration

#### 3. Production Infrastructure
- Horizontal scaling support
- Redis caching layer
- PostgreSQL database for metadata
- Prometheus metrics export
- ELK stack logging integration
- Health check endpoints
- Graceful shutdown

#### 4. Documentation & Developer Experience
- Interactive API documentation (Swagger UI)
- Postman collection
- Client SDK generation (Java, Python, JavaScript)
- Tutorial: "Building a Web UI for GeckoCIRCUITS"
- Tutorial: "Automated Testing with REST API"
- Video demonstrations

**Key Metrics:**
- REST API endpoints: 30+ functional
- WebSocket: Real-time streaming
- Performance: <100ms response time (p95)
- Uptime: 99.9% SLA
- Documentation: 100% API coverage

**Timeline:**
- Sprint 6: Simulation control endpoints (6 weeks)
- Sprint 7: WebSocket streaming (4 weeks)
- Sprint 8: Authentication & security (4 weeks)
- Sprint 9: Advanced analysis endpoints (4 weeks)
- Sprint 10: Production infrastructure (6 weeks)
- Sprint 11: Documentation & testing (3 weeks)

**Total Estimated Effort:** 6-7 months

---

### v2.1.0 - Web UI Launch (Target: Q4 2026)

**Significance:** Browser-based circuit editor and simulator

**Major Features:**
- React-based web UI
- Circuit editor with drag-and-drop
- Real-time oscilloscope visualization
- Parameter editing and tuning
- Circuit library browser
- Tutorial integration
- Responsive design (desktop, tablet)

**Technologies:**
- React 18
- TypeScript
- D3.js for visualization
- WebSocket for real-time data
- Material-UI components

**Key Metrics:**
- Browser support: Chrome, Firefox, Safari, Edge (last 2 versions)
- Mobile support: Tablet-optimized
- Performance: 60 FPS oscilloscope rendering
- Accessibility: WCAG 2.1 Level AA

---

### v2.2.0 - Cloud Deployment (Target: Q1 2027)

**Significance:** Multi-tenant SaaS platform

**Major Features:**
- AWS/Azure deployment scripts
- Kubernetes orchestration
- Auto-scaling based on load
- Multi-tenant isolation
- User workspace management
- Circuit sharing and collaboration
- Marketplace for circuit libraries

**Infrastructure:**
- Kubernetes cluster
- Terraform deployment
- CI/CD with GitHub Actions
- Monitoring with Grafana
- Cost optimization

---

### v3.0.0 - Machine Learning Integration (Target: Q2 2027)

**Significance:** AI-assisted circuit design and optimization

**Major Features:**
- Circuit optimization using ML
- Automated component selection
- Anomaly detection in simulations
- Predictive maintenance modeling
- Training data from simulation results
- Pre-trained models for common topologies

**Technologies:**
- TensorFlow/PyTorch integration
- Python microservice for ML
- GPU acceleration support
- Model versioning and deployment

---

## Long-Term Vision (2027-2028)

### Educational Platform Expansion
- Interactive tutorials with embedded simulator
- Certification programs
- Virtual laboratory for universities
- Competition platform for students
- Integration with LMS (Moodle, Canvas)

### Industry Partnerships
- Semiconductor vendor integrations (Infineon, Wolfspeed, etc.)
- Component library partnerships
- Enterprise licensing model
- Professional support tiers

### Research Collaboration
- Academic paper citation tracking
- Research dataset sharing
- Reproducible research workflows
- Integration with research tools (Jupyter, MATLAB Online)

---

## Release Frequency Target

**Goal:** Predictable release cadence

- **Major releases (X.0.0):** Annually (breaking changes, major features)
- **Minor releases (X.Y.0):** Quarterly (new features, enhancements)
- **Patch releases (X.Y.Z):** As needed (bug fixes, security updates)

**Example Timeline:**
- 2026 Q2: v1.9.0 (current)
- 2026 Q3: v2.0.0 (complete REST API)
- 2026 Q4: v2.1.0 (web UI)
- 2027 Q1: v2.2.0 (cloud deployment)
- 2027 Q2: v3.0.0 (ML integration)

---

## Appendix: Commit Statistics

**Since v1.0.0 (f5d8fbae → ec32bdcd):**
- Total commits: 58
- Duration: 3 days (Feb 11 → Feb 14, 2026)
- Major sprints: 4 (Sprint 4a, 4b, 5, PMD cleanup)
- Files changed: 500+ files
- Lines changed: 10,000+ lines
- Test coverage increase: +500 tests in core module

**Key Achievements:**
- ✅ Core module: 183 classes, 1,686 tests
- ✅ REST API: 8 endpoints, 78 tests
- ✅ Code quality: 86% PMD reduction
- ✅ CI/CD: Fully automated release pipeline
- ✅ Zero SpotBugs violations maintained
- ✅ Docker: Production-ready containers

## Contact

For questions about releases, contact the maintainer or open an issue on GitHub.
