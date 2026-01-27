# GeckoCIRCUITS Backend API Sprint Plan for Haiku Execution

**Created:** January 27, 2026  
**Goal:** Increase backend coverage and enable REST API access  
**Execution Model:** Each task is atomic and executable by Claude Haiku

---

## Overview - UPDATED BASELINES (January 27, 2026)

**IMPORTANT:** Coverage baselines from previous sessions were inaccurate. Current measurements from JaCoCo show actual coverage percentages. Targets have been rebaselined to be realistic and achievable.

| Phase | Package | Previous | **ACTUAL** | Realistic Target | Gap | Status | Est. Time |
|-------|---------|----------|----------|---------|-----|--------|-----------|
| 1 | `math` | 81.7% | **~71%** | **80%** | +9pp | 📊 Recalibrated | 1-2 hours |
| 2 | `control.calculators` | 57.8% | **~29%** | **50%** | +21pp | 📊 Recalibrated | 2-3 hours |
| 3 | `datacontainer` | 15.8% | **~17%** | **40%** | +23pp | 📊 Recalibrated | 2-3 hours |
| 4 | `gecko-rest-api` | 0 files | **4 files** | **MVP** | — | ✅ Tier 1 | 2-3 hours |

**Revised Total: ~8-12 hours (more accurate)**

**Key Reason for Variance:** Previous plan counted only extracted GUI-free calculators (64 classes at 100%) as "57.8%". Actual monolith calculator package coverage includes legacy code and untested internal classes.

---

## Session 3 Extended: Phase 2 Continuation Work (COMPLETED)

✅ **Test Files Created:** 4 new test files with 30 test methods
- LimitCalculatorTest.java (8 tests)
- HysteresisCalculatorTest.java (5 tests)  
- MathOperationCalculatorsTest.java (10 tests)
- LogicCalculatorsTest.java (7 tests)

✅ **Tests Passing:** All 30 tests compile and pass individually
✅ **Test Count:** 206+ test methods across 10 verified test files
✅ **Coverage Baseline:** ~29% (recalibrated from previous 57.8% estimate)

## Session 4: Tier 1 Architecture & Safety Gates (IN PROGRESS)

✅ **Maven Safety Gates Added:**
- gecko-simulation-core enforcer prevents GUI imports (was already in place)
- gecko-rest-api now has enforcer rules to reject Swing/AWT dependencies
- Build will fail if GUI libraries accidentally leak into REST API

✅ **Coverage Baselines Recalibrated:**
- Actual JaCoCo measurements show lower coverage than previous estimates
- Targets revised to be realistic: +9pp to +23pp improvements per phase
- Documented root cause: previous plan counted extracted code, not full package coverage

📊 **Key Metrics (January 27, 2026):**
- Phase 1 advancement: ~71% math (target 80%, +9pp gap)
- Phase 2 progression: ~29% calculators (target 50%, +21pp gap)
- Phase 3 readiness: ~17% datacontainer (target 40%, +23pp gap)
- REST API: Ready to implement (pom.xml complete, safety gates added)

---

# Strategic Priorities & Recommendations (UPDATED January 27, 2026)

## 🎯 Revised Action Sequence

### P0: REST API MVP Foundation - UNBLOCK FRONTEND ⚡
**Effort:** 2-3 hours | **Impact:** Running API server for frontend development

**Why First (not coverage):**
- Frontend developers need mock API to build against
- REST API scaffolding enables parallel development
- Safety gates already in place (prevents GUI leakage)
- Keeps team unblocked while coverage tests are being written

**Deliverables:**
- Application.java (Spring Boot entry point)
- HealthController (/api/health endpoint)
- DTOs for basic requests/responses
- application.properties (server config)
- Swagger documentation endpoint
- Integration test that verifies server starts

### P1: DataContainer Tests - HIGH COVERAGE IMPACT 💪
**Effort:** 2-3 hours | **Impact:** +23pp (17%→40%, highest gain per hour)

**Why High Priority:**
- Highest absolute coverage gain (+23pp)
- Smallest scope (data containers, no complex mocks needed)
- Unblocks API data serialization testing
- Tests are straightforward to write

**Targets:**
- DataContainerSimple test (basic storage)
- SignalDataContainer test (time series)
- AverageValue edge cases
- ContainerStatus state tests

### P2: Math Package Tests - QUICK WIN 🚀
**Effort:** 1-2 hours | **Impact:** +9pp (71%→80%, quick completion)

**Why Second:**
- Smallest gap to realistic target (+9pp)
- Pure math, no external dependencies
- Fast test writing (edge cases only)
- Completes Phase 1 deliverable

**Targets:**
- BigMatrix edge cases (non-square matrices, singular)
- Polynomial evaluation edge cases
- NComplex division by zero handling
- LU decomposition precision tests

### P3: Calculators Tests - COVERAGE BREADTH 🏁
**Effort:** 2-3 hours | **Impact:** +21pp (29%→50%, realistic mid-range)

**Why Third:**
- Medium complexity (depends on Tier 2 foundations)
- Requires understanding calculator patterns
- Multiple calculator types to test
- Can be parallelized across different calculator families

**Targets:**
- PI/PID controller tests (proportional+integral)
- PT1/PT2 filter tests (continuous-time systems)
- Integrator edge cases
- Logic calculator batch tests

### P4: Core Module Architecture - OPTIONAL ⚙️
**Effort:** Defer to Phase 2 | **Impact:** Clean module structure

**Current Status:** Core module already exists with safety gates. Classes stay in monolith for now.
- Moving classes with complex interdependencies (math, matrix, netlist) would break builds
- Better approach: Create wrapper facades once core module is mature
- Document in README: "Core module design is evolutionary, not big-bang"

---

## 📈 Expected Timeline for Realistic Milestones

Following revised P0 → P1 → P2 → P3 sequence:

| Phase | Current | Target | Time Est | Cumulative | Justification |
|-------|---------|--------|----------|-----------|---------------|
| **REST API MVP** | 0 API | Runnable | 2-3 hrs | 2-3 hrs | Unblocks frontend, enables integration testing |
| **DataContainer** | 17% | 40% | 2-3 hrs | 4-6 hrs | Highest impact (+23pp), smallest scope |
| **Math** | 71% | 80% | 1-2 hrs | 5-8 hrs | Quickest to complete (+9pp) |
| **Calculators** | 29% | 50% | 2-3 hrs | 7-11 hrs | Broader scope, medium complexity |
| **Optional: Core Extract** | Mixed | Clean | Deferred | — | Evolutionary, not critical for MVP |

**Total realistic time to core coverage milestones: 7-11 hours**

**Key Difference from Previous Plan:**
- Previous: 11 hours with unrealistic 70%+ gains per phase
- Revised: 7-11 hours with realistic 9-23pp gains per phase
- Focus on unblocking frontend first (REST API MVP)
- Sequential coverage improvements in order of complexity

---

# Strategic Context

## 1. Immediate Priority: Expand `gecko-simulation-core`

The `gecko-simulation-core` module exists with **87 files** but the legacy monolith has **854 files**. 

**Current extraction progress:**
```
gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/
├── circuit/           # ✅ Core component interfaces  
│   ├── AbstractCapacitorCore.java
│   ├── AbstractResistorCore.java
│   ├── ICircuitCalculator.java
│   └── circuitcomponents/   # Component cores
├── control/
│   └── calculators/   # ✅ 64 GUI-free calculators (COMPLETE!)
├── math/              # Needs population
└── api/               # Public interfaces
```

**Action Items (via these sprints):**
1. Move `circuit.matrix` (15 classes, 77% coverage) → `gecko-simulation-core`
2. Move `circuit.netlist` (4 classes, 99% coverage) → `gecko-simulation-core`
3. Move `circuit.simulation` (5 classes, 97% coverage) → `gecko-simulation-core`
4. Move `math` (7 classes, 55% coverage) → `gecko-simulation-core`

---

## 2. Focus Coverage Increases on These Packages

| Package | Current | Target | Why Priority |
|---------|---------|--------|--------------|
| `math` | 55% | 85% | Core for FFT, complex numbers, matrix ops - API essential |
| `control.calculators` | 41% | 75% | Signal processing (PI, PT1, integrators) - API essential |
| `datacontainer` | 15% | 70% | Simulation results container - API data export |
| `circuit.losscalculation` | 24% | 60% | Thermal analysis backend (future phase) |

**Packages to SKIP (GUI-heavy, low API value):**
- `newscope` / `scope` - Pure visualization (0-3% coverage OK)
- `allg` - Window management, dialogs
- `i18n.translationtoolbox` - Translation UI
- Any `*Dialog`, `*Panel`, `*Editor` classes

---

## 3. API Boundary Enforcement Already in Place

### Validation Tests (from CORE_API_BOUNDARY.md)
- **171 GUI-free classes** already validated
- `CorePackageValidationTest` enforces no GUI imports in core packages
- `gecko-simulation-core/pom.xml` has Maven Enforcer blocking AWT/Swing

### Key Interfaces Available for API:
```java
// From gecko-simulation-core
ICircuitCalculator       // Component calculation contract
IMatrixStamper          // MNA matrix stamping
AbstractSignalCalculator // Control block base
SolverContext           // Solver configuration
StamperRegistry         // Component factory
```

### Prohibited Imports in Core:
```java
// FORBIDDEN in core packages - tests will fail if these appear
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.applet.*;
```

---

## 4. REST API Module Status (`gecko-rest-api`)

**Current:** Module directory exists but **NO Java source files**  
**Goal:** Create proper Spring Boot REST API depending on `gecko-simulation-core`

**Target Endpoints:**
| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/simulations` | POST | Start simulation |
| `/api/v1/simulations/{id}` | GET | Get status/results |
| `/api/v1/simulations/{id}/signals` | GET | List available signals |
| `/api/v1/analysis/rms` | POST | Calculate RMS |
| `/api/v1/analysis/thd` | POST | Calculate THD |
| `/api/v1/analysis/harmonics` | POST | Harmonic analysis |
| `/api/health` | GET | Health check |

---

## 5. Coverage Summary by Package (Current State)

| Package | Coverage | API-Ready? | Notes |
|---------|----------|------------|-------|
| `i18n.resources` | **99%** | ✅ Yes | |
| `circuit.netlist` | **99%** | ✅ Yes | Core for parsing |
| `circuit.simulation` | **97%** | ✅ Yes | Solver engine |
| `circuit.api` | **97%** | ✅ Yes | Public API |
| `circuit.component` | **93%** | ✅ Yes | |
| `circuit.terminal` | **77%** | ✅ Yes | |
| `circuit.matrix` | **77%** | ✅ Yes | MNA matrices |
| `math` | **55%** | ✅ Yes | **← PHASE 1** |
| `control.calculators` | **41%** | ✅ Yes | **← PHASE 2** |
| `datacontainer` | **15%** | ⚠️ Partial | **← PHASE 3** |
| `circuit` (main) | **6%** | ⚠️ Mixed | GUI+Core mixed |
| `control` (main) | **4%** | ⚠️ Mixed | GUI+Core mixed |
| `allg` | **4%** | ❌ GUI-heavy | Skip |
| `newscope` | **3%** | ❌ Pure GUI | Skip |
| `scope` | **0%** | ❌ Pure GUI | Skip |

---

# PHASE 1: Math Package (55% → 85%)

## Current State
- **Location:** `src/main/java/ch/technokrat/gecko/geckocircuits/math/`
- **Files:** 7 (NComplex, Matrix, LUDecomposition, BigLUDecomposition, BigMatrix, CholeskyDecomposition, Polynomials)
- **Lines:** 2,717
- **Existing Tests:** 5 test files, 1,366 lines
- **GUI Dependencies:** None ✅ (Pure math, API-ready)

## Tasks

### MATH-1: Analyze BigMatrix.java Coverage Gaps
```
PROMPT FOR HAIKU:
Read the file /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/BigMatrix.java
and list all public methods. Then check if tests exist at
/home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/math/
Identify which methods lack test coverage.
OUTPUT: List of untested methods with line numbers.
```

### MATH-2: Create BigMatrixTest.java
```
PROMPT FOR HAIKU:
Create test file: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/math/BigMatrixTest.java
Test these methods from BigMatrix.java:
- Constructor tests (various sizes)
- get/set element operations
- Matrix multiplication
- Matrix addition/subtraction
- Identity matrix creation
- Transpose operation
Use JUnit 4 style (import org.junit.Test, import static org.junit.Assert.*)
Follow existing test patterns from MatrixTest.java
```

### MATH-3: Create BigLUDecompositionTest.java
```
PROMPT FOR HAIKU:
Create test file: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/math/BigLUDecompositionTest.java
Reference: BigLUDecomposition.java performs LU decomposition for BigDecimal matrices.
Test:
- Decomposition of simple 2x2, 3x3 matrices
- solve() method with known solutions
- Singular matrix detection
- Numerical precision (compare with regular LUDecomposition)
Pattern: Follow LUDecompositionTest.java structure
```

### MATH-4: Expand NComplexTest.java - Edge Cases
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/math/NComplexTest.java
Add these missing tests to the file:
- Division by zero handling
- Multiplication with pure imaginary numbers
- Square root of negative real numbers
- abs() with very large/small numbers
- equals() and hashCode() contract
- nicePrint() output format validation
```

### MATH-5: Expand MatrixTest.java - Operations
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/math/MatrixTest.java
Add tests for any untested operations:
- Matrix inverse
- Determinant calculation
- Eigenvalue computation (if exists)
- Copy constructor
- toString() format
```

### MATH-6: Expand PolynomialsTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/math/Polynomials.java
Then expand /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/math/PolynomialsTest.java with:
- Polynomial evaluation at various points
- Root finding (if method exists)
- Coefficient manipulation
- Polynomial multiplication
- Edge cases: zero polynomial, constant polynomial
```

### MATH-7: Verify All Math Tests Pass
```
PROMPT FOR HAIKU:
Run command: cd /home/tinix/claude_wsl/GeckoCIRCUITS && mvn test -Dtest="ch.technokrat.gecko.geckocircuits.math.*Test" -q
Report: number of tests run, failures, errors
If failures: identify and fix the failing test
```

### MATH-8: Generate Math Package Coverage Report
```
PROMPT FOR HAIKU:
Run commands:
1. cd /home/tinix/claude_wsl/GeckoCIRCUITS && mvn test jacoco:report -Dtest="ch.technokrat.gecko.geckocircuits.math.*Test" -q
2. grep -A5 "ch.technokrat.gecko.geckocircuits.math" target/site/jacoco/jacoco.csv | head -20
Report: coverage percentage for math package
Target: 85% instruction coverage
```

---

# PHASE 2: Control Calculators (41% → 75%)

## Current State (Session 3 Extended)
- **Location:** `src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/`
- **Core Extracted:** 64 calculators already in `gecko-simulation-core`
- **Existing Tests:** Multiple test files (**176 tests passing**, up from 161)
- **Current Coverage:** **57.8%** (5,851/10,117 instructions) - up from 42.1%
- **Gap to Target:** **17.2 percentage points** (need 1,736 more covered instructions)
- **GUI Dependencies:** 2 classes (SmallSignalCalculator, SpaceVectorCalculator)

## Tasks

### CALC-1: Identify Untested Calculators
```
PROMPT FOR HAIKU:
List all .java files in /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/
Then list all *Test.java files in /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/
OUTPUT: List calculators without corresponding test files (e.g., if FooCalculator.java exists but FooCalculatorTest.java doesn't)
```

### CALC-2: Create IntegratorCalculationTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/IntegratorCalculation.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/IntegratorCalculationTest.java
Test:
- Integration of constant signal → linear ramp
- Integration of sine → -cosine
- Initial condition handling
- Reset functionality
- dt (timestep) variations
```

### CALC-3: Create PICalculatorTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PICalculator.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/PICalculatorTest.java
Test:
- Proportional-only response (Ki=0)
- Integral-only response (Kp=0)
- Combined PI response
- Step response characteristics
- Anti-windup behavior (if implemented)
```

### CALC-4: Create PT1CalculatorTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT1Calculator.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT1CalculatorTest.java
Test:
- Step response (63.2% at t=tau)
- Steady state gain
- Various time constants
- Initial condition handling
```

### CALC-5: Create PT2CalculatorTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT2Calculator.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/PT2CalculatorTest.java
Test:
- Underdamped response (zeta < 1)
- Critically damped (zeta = 1)
- Overdamped (zeta > 1)
- Natural frequency verification
```

### CALC-6: Create LimitCalculatorTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/LimitCalculatorInternal.java
and /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/LimitCalculatorExternal.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/LimitCalculatorTest.java
Test:
- Upper limit clamping
- Lower limit clamping
- Within-range passthrough
- Symmetric limits
- Asymmetric limits
```

### CALC-7: Create HysteresisCalculatorTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/HysteresisCalculatorInternal.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/HysteresisCalculatorTest.java
Test:
- Rising edge switching
- Falling edge switching
- Hysteresis band width
- State memory across multiple cycles
```

### CALC-8: Create SampleHoldCalculatorTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SampleHoldCalculator.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/SampleHoldCalculatorTest.java
Test:
- Sample on trigger edge
- Hold value between samples
- Multiple trigger edges
- Initial value handling
```

### CALC-9: Create SignalCalculatorSinusTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorSinus.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/SignalCalculatorSinusTest.java
Test:
- Amplitude correctness
- Frequency correctness (period = 1/f)
- Phase offset
- DC offset
- Zero crossing times
```

### CALC-10: Create MathOperationCalculatorsTest.java (Batch)
```
PROMPT FOR HAIKU:
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/MathOperationCalculatorsTest.java
Batch test for simple math calculators:
- SinCalculator, CosCalculator, TanCalculator
- ASinCalculator, ACosCalculator, ATanCalculator
- ExpCalculator, LnCalculator
- SqrtCalculator, SquareCalculator
- AbsCalculator, SignumCalculator
Each: test known values (e.g., sin(0)=0, sin(π/2)=1)
```

### CALC-11: Create LogicCalculatorsTest.java (Batch)
```
PROMPT FOR HAIKU:
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/control/calculators/LogicCalculatorsTest.java
Batch test for logic calculators:
- AndTwoPortCalculator: truth table
- OrCalculatorTwoInputs: truth table
- NotCalculator: inversion
- XORCalculator: exclusive or
- GreaterThanCalculator, GreaterEqualCalculator
- EqualCalculatorTwoInputs
Test with 0.0/1.0 as false/true
```

### CALC-12: Run Calculator Tests and Report Coverage
```
PROMPT FOR HAIKU:
Run commands:
1. cd /home/tinix/claude_wsl/GeckoCIRCUITS && mvn test -Dtest="ch.technokrat.gecko.geckocircuits.control.calculators.*Test" -q 2>&1 | tail -20
2. mvn jacoco:report -q
3. grep "control.calculators" target/site/jacoco/jacoco.csv | head -5
Report: test count, pass/fail, coverage percentage
Target: 75% instruction coverage
```

---

# PHASE 3: DataContainer (15% → 70%)

## Current State
- **Location:** `src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/`
- **Files:** 32 total
- **GUI Files (skip):** ArrowIcon, DataContainerTable, DataContainerTableModel, DataJunkCompressable, DataTableFrame
- **GUI-Free Files:** 27 (target for testing)
- **Existing Tests:** 5 files

## Tasks

### DATA-1: Identify Testable DataContainer Classes
```
PROMPT FOR HAIKU:
List files in /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/
SKIP these GUI-coupled files: ArrowIcon.java, DataContainerTable.java, DataContainerTableModel.java, DataJunkCompressable.java, DataTableFrame.java, *.form
For each remaining file, check if it has a corresponding test in
/home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/
OUTPUT: List of untested GUI-free classes
```

### DATA-2: Create AbstractDataContainerTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/AbstractDataContainer.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/AbstractDataContainerTest.java
Test via concrete subclass (e.g., DataContainerSimple):
- Data storage and retrieval
- Size/capacity handling
- getValue(row, col) method
- getDataLength() method
```

### DATA-3: Create DataContainerFourierTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerFourier.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerFourierTest.java
Test:
- Fourier data storage
- Frequency component access
- Magnitude/phase calculation
- DC component handling
```

### DATA-4: Create DataContainerGlobalTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerGlobal.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataContainerGlobalTest.java
Test:
- Global data registration
- Data access patterns
- Memory management
- Cleanup/reset functionality
```

### DATA-5: Create DataJunkSimpleTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataJunkSimple.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/DataJunkSimpleTest.java
Test:
- Data chunk storage
- Append operations
- Read back verification
- Boundary conditions
```

### DATA-6: Create SignalDataContainerTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/SignalDataContainerRegular.java
and /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/SignalDataContainerMean.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/SignalDataContainerTest.java
Test:
- Regular signal storage (time series)
- Mean value computation
- Sample rate handling
- Time index lookup
```

### DATA-7: Expand AverageValueTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/AverageValueTest.java
Add tests for:
- Running average calculation
- Window size variations
- Edge cases (empty data, single value)
- Reset functionality
```

### DATA-8: Create IntegerMatrixCacheTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/IntegerMatrixCache.java
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/ShortMatrixCache.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/IntegerMatrixCacheTest.java
Test:
- Cache allocation
- Get/release patterns
- Memory reuse verification
- Thread safety (basic)
```

### DATA-9: Create ContainerStatusTest.java
```
PROMPT FOR HAIKU:
Read /home/tinix/claude_wsl/GeckoCIRCUITS/src/main/java/ch/technokrat/gecko/geckocircuits/datacontainer/ContainerStatus.java
Create test: /home/tinix/claude_wsl/GeckoCIRCUITS/src/test/java/ch/technokrat/gecko/geckocircuits/datacontainer/ContainerStatusTest.java
Test all status states/transitions (likely an enum or state class)
```

### DATA-10: Run DataContainer Tests and Report Coverage
```
PROMPT FOR HAIKU:
Run commands:
1. cd /home/tinix/claude_wsl/GeckoCIRCUITS && mvn test -Dtest="ch.technokrat.gecko.geckocircuits.datacontainer.*Test" -q 2>&1 | tail -20
2. mvn jacoco:report -q
3. grep "datacontainer" target/site/jacoco/jacoco.csv | head -10
Report: test count, pass/fail, coverage percentage
Target: 70% instruction coverage
```

---

# PHASE 4: Populate gecko-rest-api Module

## Current State
- **Location:** `/home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/`
- **Status:** Module directory exists, but NO Java source files
- **Goal:** Create proper REST API structure depending on `gecko-simulation-core`

## Tasks

### REST-1: Create Module Structure
```
PROMPT FOR HAIKU:
Create directory structure:
/home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/
/home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/controller/
/home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/service/
/home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/model/
/home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/config/
/home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/resources/
/home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/test/java/ch/technokrat/gecko/rest/
```

### REST-2: Create pom.xml for gecko-rest-api
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/pom.xml
Contents: Maven POM with:
- groupId: com.technokrat.gecko
- artifactId: gecko-rest-api
- version: 1.0-SNAPSHOT
- packaging: jar
- Parent: Spring Boot 3.2.x starter parent
- Dependencies:
  - spring-boot-starter-web
  - spring-boot-starter-validation
  - springdoc-openapi-starter-webmvc-ui (for Swagger)
  - gecko-simulation-core (local dependency)
  - spring-boot-starter-test (test scope)
- Java 21 compiler settings
```

### REST-3: Create Application Entry Point
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/GeckoRestApiApplication.java
Contents:
- Spring Boot @SpringBootApplication class
- Main method with SpringApplication.run()
- Basic configuration for JSON serialization
```

### REST-4: Create application.properties
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/resources/application.properties
Contents:
- server.port=8080
- spring.application.name=gecko-rest-api
- springdoc.api-docs.path=/api-docs
- springdoc.swagger-ui.path=/swagger-ui.html
- logging.level.ch.technokrat=DEBUG
```

### REST-5: Create SimulationRequest DTO
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/model/SimulationRequest.java
Contents:
- Fields: circuitFile (String), simulationTime (double), timeStep (double), parameters (Map<String,Double>)
- Jakarta validation annotations (@NotNull, @Positive)
- Getters, setters, builder pattern
```

### REST-6: Create SimulationResponse DTO
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/model/SimulationResponse.java
Contents:
- Fields: simulationId (String), status (enum), startTime (Instant), endTime (Instant), results (Map<String, double[]>), errors (List<String>)
- Status enum: PENDING, RUNNING, COMPLETED, FAILED
- Getters, setters
```

### REST-7: Create SignalAnalysisResult DTO
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/model/SignalAnalysisResult.java
Contents:
- Fields: signalName (String), rmsValue (double), meanValue (double), peakValue (double), thd (double), fundamentalFrequency (double), harmonics (Map<Integer, Double>)
- Constructor, getters
```

### REST-8: Create SimulationService Interface
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/service/SimulationService.java
Contents: Interface with methods:
- SimulationResponse runSimulation(SimulationRequest request)
- SimulationResponse getSimulationStatus(String simulationId)
- void cancelSimulation(String simulationId)
- List<String> getAvailableSignals(String simulationId)
- double[] getSignalData(String simulationId, String signalName)
```

### REST-9: Create SimulationServiceImpl
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/service/SimulationServiceImpl.java
Contents:
- @Service annotated implementation of SimulationService
- ConcurrentHashMap for simulation state storage
- Placeholder methods that return mock data (real integration comes later)
- TODO comments marking where gecko-simulation-core integration goes
```

### REST-10: Create SignalAnalysisService
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/service/SignalAnalysisService.java
Contents: @Service class with methods:
- SignalAnalysisResult analyzeSignal(double[] signal, double sampleRate)
- double calculateRMS(double[] signal)
- double calculateTHD(double[] signal, double fundamentalFreq)
- Map<Integer, Double> calculateHarmonics(double[] signal, double fundamentalFreq, int maxHarmonic)
Use gecko-simulation-core math classes where possible
```

### REST-11: Create SimulationController
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/controller/SimulationController.java
Contents:
- @RestController @RequestMapping("/api/v1/simulations")
- POST /api/v1/simulations - start simulation
- GET /api/v1/simulations/{id} - get status
- DELETE /api/v1/simulations/{id} - cancel
- GET /api/v1/simulations/{id}/signals - list signals
- GET /api/v1/simulations/{id}/signals/{name} - get signal data
- OpenAPI annotations (@Operation, @ApiResponse)
- Inject SimulationService
```

### REST-12: Create SignalAnalysisController
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/controller/SignalAnalysisController.java
Contents:
- @RestController @RequestMapping("/api/v1/analysis")
- POST /api/v1/analysis/rms - calculate RMS of uploaded signal
- POST /api/v1/analysis/thd - calculate THD
- POST /api/v1/analysis/harmonics - harmonic analysis
- POST /api/v1/analysis/full - complete analysis
- OpenAPI annotations
- Inject SignalAnalysisService
```

### REST-13: Create HealthController
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/controller/HealthController.java
Contents:
- @RestController
- GET /api/health - returns {"status": "UP", "version": "1.0.0"}
- GET /api/info - returns application info
```

### REST-14: Create SimulationControllerTest
```
PROMPT FOR HAIKU:
Create file: /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/test/java/ch/technokrat/gecko/rest/controller/SimulationControllerTest.java
Contents:
- @SpringBootTest @WebMvcTest tests
- Test POST /api/v1/simulations with valid request
- Test GET /api/v1/simulations/{id} returns correct status
- Test validation errors for invalid requests
- Use MockMvc and @MockBean for SimulationService
```

### REST-15: Verify Module Builds
```
PROMPT FOR HAIKU:
Run commands:
1. cd /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api && mvn clean compile -q
2. mvn test -q
3. If errors: fix compilation issues
Report: Build success/failure, any required fixes
```

---

# Execution Checklist

## Pre-Flight
- [ ] Verify Maven works: `mvn --version`
- [ ] Verify tests run: `mvn test -Dtest="*MatrixTest" -q`
- [ ] Verify JaCoCo works: `mvn jacoco:report`

## Phase 1: Math (Tasks MATH-1 through MATH-8)
- [ ] MATH-1: Analyze gaps
- [ ] MATH-2: BigMatrixTest
- [ ] MATH-3: BigLUDecompositionTest
- [ ] MATH-4: NComplexTest expansion
- [ ] MATH-5: MatrixTest expansion
- [ ] MATH-6: PolynomialsTest expansion
- [ ] MATH-7: Verify all pass
- [ ] MATH-8: Coverage report → Target 85%

## Phase 2: Calculators (Tasks CALC-1 through CALC-12)
- [ ] CALC-1: Identify gaps
- [ ] CALC-2: IntegratorCalculationTest
- [ ] CALC-3: PICalculatorTest
- [ ] CALC-4: PT1CalculatorTest
- [ ] CALC-5: PT2CalculatorTest
- [ ] CALC-6: LimitCalculatorTest
- [ ] CALC-7: HysteresisCalculatorTest
- [ ] CALC-8: SampleHoldCalculatorTest
- [ ] CALC-9: SignalCalculatorSinusTest
- [ ] CALC-10: MathOperationCalculatorsTest
- [ ] CALC-11: LogicCalculatorsTest
- [ ] CALC-12: Coverage report → Target 75%

## Phase 3: DataContainer (Tasks DATA-1 through DATA-10)
- [ ] DATA-1: Identify testable classes
- [ ] DATA-2: AbstractDataContainerTest
- [ ] DATA-3: DataContainerFourierTest
- [ ] DATA-4: DataContainerGlobalTest
- [ ] DATA-5: DataJunkSimpleTest
- [ ] DATA-6: SignalDataContainerTest
- [ ] DATA-7: AverageValueTest expansion
- [ ] DATA-8: IntegerMatrixCacheTest
- [ ] DATA-9: ContainerStatusTest
- [ ] DATA-10: Coverage report → Target 70%

## Phase 4: REST API (Tasks REST-1 through REST-15)
- [ ] REST-1: Directory structure
- [ ] REST-2: pom.xml
- [ ] REST-3: Application class
- [ ] REST-4: application.properties
- [ ] REST-5: SimulationRequest DTO
- [ ] REST-6: SimulationResponse DTO
- [ ] REST-7: SignalAnalysisResult DTO
- [ ] REST-8: SimulationService interface
- [ ] REST-9: SimulationServiceImpl
- [ ] REST-10: SignalAnalysisService
- [ ] REST-11: SimulationController
- [ ] REST-12: SignalAnalysisController
- [ ] REST-13: HealthController
- [ ] REST-14: SimulationControllerTest
- [ ] REST-15: Verify build

---

# Quick Reference: Haiku Prompts

## Pattern for Test Creation
```
Read [source file path]
Create test file: [test file path]
Test these scenarios:
- [scenario 1]
- [scenario 2]
Use JUnit 4 style. Follow patterns from existing tests.
```

## Pattern for Coverage Check
```
Run: cd /home/tinix/claude_wsl/GeckoCIRCUITS && mvn test jacoco:report -Dtest="[test pattern]" -q
Extract coverage from: target/site/jacoco/jacoco.csv
Report percentage for [package name]
```

## Pattern for Debugging Failures
```
Run: mvn test -Dtest="[specific test]" -e
If failure: read the source file being tested
Identify the bug in the test
Fix the assertion or test logic
```
