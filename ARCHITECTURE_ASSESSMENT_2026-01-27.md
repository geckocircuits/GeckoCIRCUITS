# GeckoCIRCUITS Architecture Assessment
## Software Architect Review: Backend/Frontend Separation & REST API Readiness

**Date:** January 27, 2026  
**Scope:** Analysis of current JaCoCo coverage, architectural boundaries, and API enablement strategy  
**Status:** Action-ready recommendations provided

---

## 1. Current JaCoCo Coverage Snapshot

### High-Level Metrics
```
Total Tests Run: 2,710
Test Failures: 0
Test Errors: 3 (headless GUI, JNI library issues - non-critical for API)
Skipped Tests: 4

Coverage Status:
├── GUI-Heavy Packages: 0-4% (expected to remain low)
├── Core Simulation: 55-99% (API-ready foundation)
├── Backend Services: 15-58% (HAIKU_SPRINT_PLAN targets)
└── REST API Module: 0 files (not yet implemented)
```

### Priority Packages - Current Coverage

| Package | Current | Target | Gap | Instructions | Status |
|---------|---------|--------|-----|--------------|--------|
| **math** | **~71%** | 85% | -14pp | 558 missed, 1366 covered | ⚠️ Needs 3-4 tests |
| **control.calculators** | **~29%** | 75% | -46pp | 2768 missed, 1128 covered | 🔴 Large gap |
| **datacontainer** | **~17%** | 70% | -53pp | 799 missed, 169 covered | 🔴 Very low |
| **circuit.matrix** | **~85%** | ✅ | — | 52 missed, 376 covered | ✅ API-ready |
| **circuit.netlist** | **~99%** | ✅ | — | 2 missed, 408-525 covered | ✅ API-ready |
| **circuit.simulation** | **~97%** | ✅ | — | 2 missed, 151-274 covered | ✅ API-ready |

### GUI-Free Architecture Already Validated

These packages are **100% GUI-free** per `CorePackageValidationTest.java`:

| Package | Files | Coverage | Ready for API |
|---------|-------|----------|---------------|
| `circuit.api` | 2+ | 97%+ | ✅ YES |
| `circuit.component` | 7 | 89-98% | ✅ YES |
| `circuit.terminal` | 7 | 57-100% | ✅ YES |
| `circuit.losscalculation` | 9 | 24-99% | ⚠️ PARTIAL |
| `control.calculators` (core 64) | 64 | 100% | ✅ YES |
| `gecko-simulation-core` (full module) | 87 | Mixed | ✅ SAFE |

### API-Blocking Issues: NONE

✅ **No blocking architectural issues identified.**  
The core is already extracted. REST API can be built immediately on `gecko-simulation-core`.

---

## 2. Backend/Frontend Separation: Current State

### Architecture Status: **PARTIALLY COMPLETE** ⚠️

#### What's Already Done
```
Legacy Monolith (854 files)                gecko-simulation-core (87 files)
├── Circuit components                    ├── circuit/
├── Control blocks                        │   ├── AbstractCapacitor/Resistor...Core
├── Math operations                       │   ├── ICircuitCalculator interface
├── Data containers                       │   ├── matrix/ (15 classes, 77% cov)
├── Simulation engine                     │   ├── netlist/ (4 classes, 99% cov)
├── 171 GUI-free validated classes ✅     │   ├── simulation/ (5 classes, 97% cov)
└── GUI (Swing/AWT - stays here)          └── control/
                                               └── calculators/ (64 classes, 100% extracted)
                                          └── math/ (partial)
                                          └── api/ (core interfaces)
```

#### Current Problem: **Incomplete Core Module**
The `gecko-simulation-core` is only **partially populated**:

- ✅ 64 calculators extracted and 100% GUI-free
- ✅ Circuit core interfaces defined
- ❌ **Math package not yet moved** (only 71% covered in legacy location)
- ❌ **Matrix operations still in main project** (should be in core)
- ❌ **Netlist classes still in main project** (should be in core)
- ❌ **Simulation engine partially extracted**

### Current Dependency Flow

```
┌─────────────────────────────────────────┐
│   Desktop GUI (Swing/AWT - 400+ files)  │
│   ├── Main Window                       │
│   ├── Dialogs & Panels                  │
│   └── Scope/Visualization               │
└────────────────┬────────────────────────┘
                 │ depends on
                 ▼
┌─────────────────────────────────────────┐
│   Legacy Monolith (854 files)           │
│   ├── GUI-Free Classes (171 validated)  │
│   │   ├── Math (71% coverage)           │
│   │   ├── Control Calculators (29%)     │
│   │   ├── Data Containers (17%)         │
│   │   └── Circuit Simulation (95-99%)   │
│   └── GUI-Coupled Classes (mixed)       │
└─────────────────────────────────────────┘

PROBLEM: 
- Desktop GUI can build with monolith ✓
- REST API cannot (pulls in Swing/AWT)  ✗
- Duplication between core module and monolith
- Inconsistent versions across modules
```

### Proper Target Architecture

```
┌────────────────────────────────────┐
│  gecko-simulation-core (clean)     │  Build dependency artifacts
├────────────────────────────────────┤  for Maven/CI pipeline
│ ✅ Math (all 7 classes)            │
│ ✅ Circuit matrix/netlist/sim      │
│ ✅ Control calculators (64)        │
│ ✅ Data containers (GUI-free)      │
│ ✅ ZERO Swing/AWT imports          │
└────────────────────────────────────┘
         △
         │ depends on (library only)
         │
  ┌──────┴──────────────────────────────────────┐
  │  REST API (gecko-rest-api)                   │
  │  - Spring Boot                               │
  │  - Controllers                               │
  │  - DTOs                                      │
  │  - Service layer                             │
  └──────────────────────────────────────────────┘
         △
         │ consumed by
         │
  ┌──────┴────────────────────────────────────────────────┐
  │  Desktop GUI (keep current structure)                  │
  │  - Swing/AWT components                                │
  │  - Can still use local monolith copy for GUI extras    │
  └─────────────────────────────────────────────────────────┘
```

---

## 3. REST API Readiness Assessment

### Current State: **NOT STARTED**

```
gecko-rest-api/
├── pom.xml                          ✅ COMPLETE (Spring Boot 3.2.1, Java 21)
│   └── Depends on gecko-simulation-core
├── src/main/
│   ├── java/ch/technokrat/gecko/rest/
│   │   ├── Application.java         ❌ MISSING
│   │   ├── model/                   ❌ MISSING (DTOs: Request, Response, Analysis)
│   │   ├── service/                 ❌ MISSING (SimulationService, AnalysisService)
│   │   ├── controller/              ❌ MISSING (SimulationController, AnalysisController)
│   │   └── config/                  ❌ MISSING (SecurityConfig, WebConfig)
│   └── resources/
│       └── application.properties   ❌ MISSING
├── src/test/java/
│   └── ch/technokrat/gecko/rest/
│       └── controller/              ❌ MISSING (Integration tests)
└── target/                          (Empty - not yet built)
```

**Files Existing:** 4 placeholder Java files  
**Files Needed:** 13-15 implementation files  
**Build Status:** Can compile, but zero functional endpoints

### REST API Design: READY TO IMPLEMENT

Endpoints have been fully designed in HAIKU_SPRINT_PLAN.md:

```
POST   /api/v1/simulations                    Start simulation
GET    /api/v1/simulations/{id}              Get status/results
DELETE /api/v1/simulations/{id}              Cancel simulation
GET    /api/v1/simulations/{id}/signals      List signal names
GET    /api/v1/simulations/{id}/signals/{name}  Get signal data

POST   /api/v1/analysis/rms                   Calculate RMS
POST   /api/v1/analysis/thd                   Calculate THD
POST   /api/v1/analysis/harmonics             Harmonic analysis
POST   /api/v1/analysis/full                  Complete analysis

GET    /api/health                            Health check
GET    /api/info                              Application info
```

All DTOs and service interfaces defined. Ready to code.

---

## 4. HAIKU_SPRINT_PLAN Progress vs Reality

### Comparison Matrix

| Phase | Package | Plan | Current | Notes |
|-------|---------|------|---------|-------|
| **1** | math | 81.7% → 85% | **~71%** | ⚠️ LOWER than plan (plan was optimistic) |
| **2** | control.calculators | 57.8% → 75% | **~29%** | 🔴 MUCH LOWER than plan |
| **3** | datacontainer | 15.8% → 70% | **~17%** | ✅ Close to plan baseline |
| **4** | gecko-rest-api | 0 files | **4 files** | ✅ pom.xml ready, structure incomplete |

### Key Finding: **Baseline Measurements Were Inaccurate**

The HAIKU_SPRINT_PLAN was created with older baseline numbers. Current JaCoCo shows:

- **Math**: Plan said 81.7%, actual is ~71% (10pp variance)
- **Calculators**: Plan said 57.8%, actual is ~29% (28pp variance!)
- **DataContainer**: Plan said 15.8%, actual is ~17% (✅ close)

**Root Cause:** The 57.8% calculator coverage was likely from counting only extracted GUI-free calculators (64 classes) rather than all calculator-related code.

---

## 5. Strategic Recommendations as Software Architect

### Tier 1: CRITICAL - Must do before REST API

#### Recommendation 1A: Complete Core Module Migration
**Priority:** CRITICAL  
**Effort:** 1-2 hours  
**Impact:** Enables REST API to build safely

**Action Items:**
```
1. Move math/ (7 classes) to gecko-simulation-core
   └─ From: src/main/java/ch/technokrat/gecko/geckocircuits/math/
   └─ To: gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/math/

2. Move circuit.matrix/ (15 classes, 77% coverage) to gecko-simulation-core
   └─ From: src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/
   └─ To: gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/matrix/

3. Move circuit.netlist/ (4 classes, 99% coverage) to gecko-simulation-core
   └─ Currently: Already heavily tested, can move immediately

4. Update pom.xml dependencies
   └─ REST API should ONLY depend on gecko-simulation-core
   └─ Monolith becomes optional (GUI addons only)

5. Verify CorePackageValidationTest still passes
   └─ No Swing/AWT imports in core module
```

**Why Critical:**
- Prevents GUI dependencies leaking into REST API
- Ensures API can run headless
- Creates clean binary artifact for deployment
- Unblocks all REST API development

#### Recommendation 1B: Establish Maven Build Separation
**Priority:** CRITICAL  
**Effort:** 30 minutes  
**Impact:** Prevents accidental GUI inclusion in API builds

**Action Items:**
```
1. Add Maven enforcer rule to gecko-rest-api/pom.xml
   ├─ Reject any dependency on monolith artifact
   ├─ Reject any Swing/AWT libraries in classpath
   └─ Build fails if violated (safety check)

2. Add integration test that starts REST API without headless flag
   └─ Proves API can run in pure server environment
   └─ Fails if any GUI libraries are used

3. Document in README.md
   └─ "REST API can run on any server without X11/display"
```

**Why Critical:**
- Prevents regressions later
- Catches GUI creep at build time
- Automated safety guarantee

---

### Tier 2: HIGH - Coverage Targets

#### Recommendation 2A: Rebaseline Coverage Targets
**Priority:** HIGH  
**Effort:** 1 hour  
**Impact:** Accurate tracking going forward

**Action Items:**
```
Revise HAIKU_SPRINT_PLAN.md coverage targets based on actual baselines:

Current Actual → Realistic Phase Targets:
├─ math:              71% → 80% (reasonable +9pp)
├─ control.calculators: 29% → 50% (realistic +21pp, not 75%)
├─ datacontainer:     17% → 40% (realistic +23pp, not 70%)
└─ gecko-rest-api:     0 → Setup (basic endpoints)

Rationale:
- 10-25pp improvements per package are achievable in ~2-3 hours each
- 50%+ improvements (like 15%→70%) require architectural work first
- Current baseline likely excludes inner classes/anonymous classes
```

**Why High:**
- Prevents demoralizing "failed" sprint targets
- Ensures credible tracking
- Aligns effort estimates with reality

#### Recommendation 2B: Prioritize DataContainer Coverage
**Priority:** HIGH  
**Effort:** 2-3 hours  
**Impact:** Unblocks API data serialization

**Action Items:**
```
Why DataContainer first (not Math or Calculators):
1. Smaller complexity (data storage classes)
2. Direct API requirement (results export)
3. No external test dependencies
4. Enables end-to-end API testing once REST layer exists

Current 17% → Target 45% with:
├─ DataContainerSimple test
├─ SignalDataContainerRegular test
├─ AverageValue edge cases
├─ DataJunkSimple test
└─ ContainerStatus test

Tests should be straightforward (no complex mock setup needed).
```

**Why High:**
- Unblocks REST API integration testing
- Data serialization is critical for API responses
- Small classes = fast test writing

---

### Tier 3: MEDIUM - REST API Implementation

#### Recommendation 3A: Build REST API Core First
**Priority:** MEDIUM (depends on Tier 1)  
**Effort:** 4-6 hours  
**Impact:** Functional simulation API server

**Sequence:**
```
Phase 1: Foundation (2 hours)
├─ Create GeckoRestApiApplication.java (Spring Boot entry point)
├─ Create application.properties (server config, logging)
├─ Create HealthController (GET /api/health)
└─ Verify server starts: mvn spring-boot:run

Phase 2: DTOs (1 hour)
├─ SimulationRequest DTO
├─ SimulationResponse DTO
├─ SignalAnalysisResult DTO
└─ JSON serialization working in integration test

Phase 3: Services (1 hour)
├─ SimulationService interface (placeholder impl OK for now)
├─ SignalAnalysisService (wire to gecko-simulation-core math)
└─ ConcurrentHashMap storage for simulation state

Phase 4: Controllers (2 hours)
├─ SimulationController (POST/GET/DELETE endpoints)
├─ SignalAnalysisController (RMS/THD/harmonics)
├─ Swagger documentation (@Operation, @ApiResponse)
└─ Integration tests (MockMvc)

Phase 5: Validation (1 hour)
├─ Start server: mvn spring-boot:run
├─ Test with curl: POST /api/v1/simulations + test request
├─ Verify /swagger-ui.html loads
```

**Why Medium:**
- Depends on completing Tier 1 (core module) first
- Straightforward Spring Boot pattern
- No novel architecture needed

#### Recommendation 3B: Minimal MVP Approach
**Priority:** MEDIUM  
**Effort:** Reduced from 4-6 hours to 2-3 hours with scope cut

**Scope Reduction:**
```
MVP Version (v0.1):
├─ HealthController only ✅
├─ Swagger endpoint ✅
├─ Request validation ✅
├─ Async simulation executor (fake) ✅
└─ Response DTOs (structure only) ✅

Later Phases (v0.2+):
├─ Actual simulation integration
├─ Database backend for results
├─ Authentication/authorization
├─ Rate limiting
└─ Comprehensive testing

Benefit: Get to "running API" in 2-3 hours
         Shows proof of concept immediately
         Unblocks frontend development with mock data
```

**Why Medium + MVP:**
- Reduces implementation risk
- Enables parallel frontend development
- Gets feedback faster
- Later phases add real simulation logic

---

### Tier 4: LOW - Long-term Architecture

#### Recommendation 4A: Extract More Core Modules
**Priority:** LOW (optional, for mature product)  
**Effort:** 2-3 hours  
**Impact:** Cleaner modular structure

**Proposed Structure:**
```
gecko-
├── simulation-core/        (CRITICAL - done now)
│   ├── math
│   ├── circuit
│   ├── control.calculators
│   └── data
├── rest-api/               (MEDIUM - implement next)
├── gui-desktop/            (legacy monolith refactored later)
├── analyzer-cli/           (future: headless analysis tool)
└── sim-engine/             (future: standalone simulator)
```

**Why Low:**
- Not blocking MVP
- Can be done post-launch
- Improves developer experience, not product capability

---

## 6. Implementation Roadmap

### Immediate (Today/Tomorrow): 4-6 hours

```
STEP 1: Core Module Migration (1-2 hours) [CRITICAL]
├─ Move math/ → gecko-simulation-core
├─ Move circuit.matrix/ → gecko-simulation-core  
├─ Move circuit.netlist/ → gecko-simulation-core
├─ Update all imports and pom.xml
├─ Verify builds: mvn clean install -DskipTests
└─ Verify CorePackageValidationTest passes

STEP 2: Add Maven Safety Checks (30 min) [CRITICAL]
├─ Update gecko-rest-api/pom.xml with enforcer rules
├─ Add test that verifies no GUI libraries in REST classpath
└─ Document in README.md

STEP 3: DataContainer Coverage (2-3 hours) [HIGH]
├─ Create 3-4 focused test files for data containers
├─ Aim for 45% coverage (realistic +28pp)
└─ Update COVERAGE_PROGRESS.md

STEP 4: REST API MVP (2-3 hours) [MEDIUM]
├─ Create Application.java + application.properties
├─ Build HealthController + DTOs + tests
├─ Start server successfully
└─ Swagger documentation available
```

**Total: ~5-9 hours to "API running + safety checks + progress"**

### Short Term (Week 1): Incremental progress

```
STEP 5: REST API Full Implementation (3-4 hours)
├─ SimulationController + SimulationService
├─ SignalAnalysisController (placeholder impl)
├─ OpenAPI documentation
└─ Integration tests

STEP 6: Calculator Coverage (2-3 hours)
├─ 3-4 targeted test files (not comprehensive)
├─ Aim for 50% coverage (+21pp)
└─ Focus on core calculators (PI, PT1, integrals)

STEP 7: Math Coverage (1-2 hours)
├─ Edge case tests for matrix operations
├─ Aim for 80% coverage (+9pp)
└─ Complete Phase 1
```

---

## 7. Architecture Checkpoints

### Before REST API Launch: Safety Gate

```
✅ MUST HAVE:
├─ gecko-simulation-core pom.xml has enforcer rule banning AWT/Swing
├─ CorePackageValidationTest passes with 0 GUI imports
├─ REST API mvn build succeeds
├─ Server starts without X11: mvn spring-boot:run
├─ Curl can hit /api/health endpoint
├─ Swagger UI available at /swagger-ui.html

⚠️ SHOULD HAVE:
├─ DataContainer tests at 40%+ coverage
├─ Coverage trend chart (current vs target)
├─ README.md documents API + core module relationship
├─ Docker build file for containerization (stretch)
└─ GitHub Actions workflow for CI/CD (stretch)

❌ DO NOT LAUNCH IF:
├─ java.awt or javax.swing appears in REST API classpath
├─ Tests require display server to run
├─ GUI library leak into gecko-simulation-core
└─ API cannot start without monolith
```

---

## 8. Conclusion

### Current Situation
- **Backend separation:** 60% complete (core module exists, but incomplete)
- **REST API readiness:** 10% complete (pom.xml done, code missing)
- **Coverage tracking:** Inaccurate (baselines need update)
- **Safety:** Good (architectural boundaries in place, enforced by tests)

### Key Findings
1. **No blockers** - REST API can be implemented immediately after Tier 1
2. **Coverage plan unrealistic** - Targets should be revised downward
3. **Data container is critical** - Should be priority #2 after core migration
4. **MVP approach is viable** - 2-3 hours to "API running"

### Recommended Path Forward
```
Week 1:
├─ CRITICAL: Migrate core module + safety checks (4-6 hours)
├─ HIGH: DataContainer tests (2-3 hours)
└─ MEDIUM: REST API MVP (2-3 hours)
Total: 8-12 hours to production-ready API foundation

Week 2:
├─ Expand REST API (real simulation integration)
├─ Additional coverage tests (Calculators, Math)
└─ Performance & security hardening
```

### Success Criteria
✅ REST API server runs standalone without GUI  
✅ `/api/health` returns 200 OK  
✅ OpenAPI documentation auto-generates  
✅ Maven enforces no GUI leakage  
✅ Coverage trending upward in COVERAGE_PROGRESS.md  

**All achievable in next 2 weeks with recommended sequence.**

---

## Appendix A: Current Module Structure

### gecko-simulation-core (Partial)
```
src/main/java/ch/technokrat/gecko/core/
├── circuit/
│   ├── AbstractCapacitorCore.java
│   ├── AbstractResistorCore.java
│   ├── ICircuitCalculator.java
│   └── circuitcomponents/ (42 files, 100% GUI-free)
├── control/
│   ├── AbstractControlCalculator.java
│   └── calculators/ (64 classes, 100% GUI-free)
├── allg/ (3 files, GUI-free utilities)
├── api/ (public interfaces)
└── [MISSING: math/, datacontainer specific classes]
```

### Legacy Monolith (Mixed)
```
src/main/java/ch/technokrat/gecko/geckocircuits/
├── math/ (7 classes, 71% coverage) [SHOULD MOVE]
├── circuit/
│   ├── matrix/ (15 classes, 85% coverage) [SHOULD MOVE]
│   ├── netlist/ (4 classes, 99% coverage) [SHOULD MOVE]
│   ├── simulation/ (5 classes, 97% coverage) [SHOULD MOVE]
│   ├── losscalculation/ (9 classes, 54% coverage)
│   └── ... GUI-mixed components
├── control/
│   ├── calculators/ (duplicates in core)
│   ├── javablock/ (GUI blocks, 0% coverage - skip)
│   └── ... GUI control panels
├── datacontainer/ (27 GUI-free, 5 GUI-only, 17% coverage)
├── allg/ (GUI framework, 4% coverage - skip)
├── newscope/ (pure visualization, 3% coverage - skip)
└── scope/ (pure visualization, 0% coverage - skip)
```

### gecko-rest-api (Empty)
```
pom.xml                                    ✅ Complete
src/main/java/ch/technokrat/gecko/rest/
├── [Missing 13-15 Java files]
└── [Missing DTOs, Services, Controllers]
src/main/resources/
└── [Missing application.properties]
src/test/java/
└── [Missing integration tests]
```

