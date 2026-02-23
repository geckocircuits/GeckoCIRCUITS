# GeckoCIRCUITS Solver Feature Improvement Plan

## Objective
Enhance GeckoCIRCUITS simulation engine based on gap analysis with SIMBA's advanced solver capabilities.

---

## Gap Analysis: SIMBA vs GeckoCIRCUITS

### SIMBA Key Technologies
1. **OTSF (Optimal Time-Step Finder)** - Adaptive time stepping based on circuit dynamics
2. **NDETE (Next Discontinuity Event Time Estimator)** - ML-based switching event prediction
3. **Zero-Crossing Interpolation** - Precise event location at exact switching moments
4. **Sparse Matrix Solver** - Linear O(n) scaling with circuit nodes
5. **DC Initialization** - Pre-transient steady-state analysis
6. **DSET (Dual-Stage ElectroThermal)** - Separated electrical/thermal phases
7. **Sampled Control Mode** - Digital controller emulation with fixed sample periods

### GeckoCIRCUITS Current State
| Capability | Implementation | Location |
|------------|----------------|----------|
| Solver Types | BE, TRZ, Gear-Shichman | `SolverType.java`, `SolverContext.java` |
| Time Stepping | Fixed dt only | `SolverSettings.java` |
| Matrix Solution | Dense LU with caching | `LUDecomposition.java`, `LUDecompositionCache.java` |
| Switch Handling | Re-stamp on state change | `StateTransitionValidator.java` |
| Thermal Coupling | Synchronized domains | `SimulationsKern.java` |
| Control Sync | Every time step | `NetzlisteCONTROL.java` |

### Gap Summary

| Feature | SIMBA | GeckoCIRCUITS | Priority | Effort |
|---------|-------|---------------|----------|--------|
| Adaptive Time Step | ✅ OTSF | ❌ Fixed | **HIGH** | High |
| Event Prediction | ✅ NDETE | ❌ None | MEDIUM | High |
| Zero-Crossing Detection | ✅ Interpolation | ❌ None | **HIGH** | Medium |
| DC Operating Point | ✅ Pre-transient | ❌ None | **HIGH** | Medium |
| Sparse Matrix | ✅ Native sparse | ⚠️ Dense | MEDIUM | High |
| Sampled Control | ✅ Both modes | ⚠️ Sync only | LOW | Low |
| State-Space Analysis | ⚠️ Limited | ❌ None | LOW | Medium |

---

## Architecture Decision: Java vs Native Languages

### Language Comparison for Circuit Solvers

| Factor | Java (Current) | C++/Native | Rust | WASM |
|--------|---------------|------------|------|------|
| **Sparse Solvers** | EJML, MTJ | KLU, SuiteSparse, Eigen | nalgebra, faer | Limited |
| **Performance** | 0.5-0.8x native | 1.0x baseline | 0.95-1.0x | 0.3-0.7x |
| **Integration** | Native | JNI/JNA overhead | JNI/JNA | Browser only |
| **Development Speed** | Fast | Slow | Medium | Medium |
| **Memory Control** | GC pauses | Full control | Full control | Limited |
| **Existing Code** | 100k+ lines | Rewrite | Rewrite | Rewrite |

### Arguments for Staying with Java

1. **Existing Codebase** - 100k+ lines of working simulation code
2. **EJML Sparse** - Pure Java sparse solver, competitive performance
3. **GraalVM Native Image** - Can compile to native binary (no JVM)
4. **JNA/Panama** - Can call native KLU/SuiteSparse if needed
5. **GUI Integration** - Swing GUI tightly coupled to solver
6. **Cross-Platform** - Single JAR runs everywhere

### Arguments for Native (C++/Rust)

1. **KLU/SuiteSparse** - Industry-standard sparse solvers (SPICE uses these)
2. **Predictable Latency** - No GC pauses during simulation
3. **SIMD Vectorization** - Better auto-vectorization
4. **Memory Efficiency** - 2-4x less memory for large matrices
5. **Ecosystem** - More numerical libraries available

### Recommendation: Hybrid Architecture

**Keep Java application layer, add optional native acceleration for matrix solving:**

```
┌─────────────────────────────────────────┐
│           Java Application              │
│  ┌─────────────┐  ┌─────────────────┐   │
│  │  Swing GUI  │  │  Control Logic  │   │
│  └─────────────┘  └─────────────────┘   │
│           │              │              │
│           ▼              ▼              │
│  ┌─────────────────────────────────┐    │
│  │     Java Solver Interface       │    │
│  │  (SimulationsKern, LKMatrices)  │    │
│  └─────────────────────────────────┘    │
│           │              │              │
│     ┌─────┴─────┐  ┌─────┴─────┐        │
│     ▼           ▼  ▼           ▼        │
│  ┌──────┐  ┌──────────────────────┐     │
│  │ EJML │  │  Native via JNA/FFM  │     │
│  │(pure)│  │  (KLU/SuiteSparse)   │     │
│  └──────┘  └──────────────────────┘     │
└─────────────────────────────────────────┘
```

### Implementation Strategy

| Phase | Approach | Benefit |
|-------|----------|---------|
| **Phase 1** | EJML sparse (pure Java) | Works everywhere, no native dependencies |
| **Phase 2** | Optional KLU via JNA | 2-5x speedup for large circuits (1000+ nodes) |
| **Phase 3** | GraalVM native-image | CLI/headless mode without JVM startup |

### Why Not Full Rewrite

- **Effort**: 2-3 year rewrite of solver + GUI
- **Diminishing Returns**: EJML sparse is sufficient for 90% of circuits
- **Algorithm vs Compute**: Adaptive stepping, zero-crossing are algorithmic improvements—language won't help
- **80/20 Rule**: Native matrix backend gives 80% of performance benefit with 10% of effort

### When Native Makes Sense

| Use Case | Recommendation |
|----------|----------------|
| Circuits < 100 nodes | Java (EJML) sufficient |
| Circuits 100-1000 nodes | Java (EJML sparse) |
| Circuits > 1000 nodes | Native KLU backend |
| Real-time HIL simulation | Native required |
| Embedded deployment | Native or GraalVM |
| Web deployment | Consider WASM subset |

### Available Java Sparse Libraries

| Library | License | Features |
|---------|---------|----------|
| **EJML** | Apache 2.0 | Sparse LU, QR, Cholesky; pure Java |
| **MTJ** | LGPL | Wraps netlib (BLAS/LAPACK) |
| **la4j** | Apache 2.0 | Basic sparse support |
| **ojAlgo** | MIT | Good sparse performance |

### Native Libraries (via JNA/FFM)

| Library | License | Features |
|---------|---------|----------|
| **SuiteSparse/KLU** | LGPL | Industry standard for circuit simulation |
| **Eigen** | MPL2 | C++ template library, very fast |
| **Intel MKL** | Proprietary | Highly optimized BLAS/LAPACK |

---

## Proposed Feature Improvements

### Phase 1: DC Operating Point Analysis (High Priority, Medium Effort)

**What:** Pre-simulation steady-state analysis to find initial conditions

**Why:**
- Eliminates startup transients
- Faster convergence to periodic steady-state
- Essential for efficiency analysis

**Implementation:**
1. Create `DCOperatingPointSolver.java` in `/circuit/analysis/`
2. Replace time-varying elements:
   - Capacitors → Open circuit (or initial voltage sources)
   - Inductors → Short circuit (or initial current sources)
   - AC sources → DC equivalent (average or RMS)
3. Solve DC MNA equation: A_dc * x = b_dc
4. Use solution as initial conditions for transient

**Key Files to Modify:**
- `SimulationsKern.java` - Add DC init phase before transient
- `SolverSettings.java` - Add `enableDCInit` flag
- NEW: `circuit/analysis/DCOperatingPointSolver.java`

**Acceptance Criteria:**
- Buck converter starts at steady-state output voltage
- No startup transient when DC init enabled
- Configurable via simulation settings dialog

---

### Phase 2: Zero-Crossing Detection & Interpolation (High Priority, Medium Effort)

**What:** Precise detection of switching events with interpolation to exact crossing time

**Why:**
- Eliminates artificial ringing from imprecise switch timing
- Improves accuracy without reducing time step
- Critical for resonant converter simulation

**Implementation:**
1. After each time step, check if any switch crossed threshold
2. If crossing detected, use linear/quadratic interpolation to find exact time
3. Roll back to crossing time, update switch state, continue

**Algorithm:**
```
for each stateful component:
    if state_changed:
        t_cross = interpolate_crossing(t_prev, t_now, v_prev, v_now, threshold)
        rollback_to(t_cross)
        update_switch_state()
        re_solve_from(t_cross)
```

**Key Files to Modify:**
- `StateTransitionValidator.java` - Add interpolation logic
- `SimulationsKern.java` - Add rollback capability
- NEW: `circuit/simulation/ZeroCrossingDetector.java`

**Acceptance Criteria:**
- Diode turn-on/off occurs at exact voltage threshold
- No negative diode current spikes
- LLC resonant converter waveforms match theory

---

### Phase 3: Adaptive Time Stepping (High Priority, High Effort)

**What:** Automatic time step adjustment based on circuit dynamics

**Why:**
- Fast simulation during steady portions
- Small steps only when needed (switching, transients)
- 10-100x speedup for many circuits

**Implementation:**

**3a. Local Truncation Error (LTE) Estimation**
```java
// Compare BE and TRZ solutions for error estimate
x_be = solve_backward_euler(dt)
x_trz = solve_trapezoidal(dt)
lte = norm(x_trz - x_be) / 3  // Richardson extrapolation
```

**3b. Step Size Controller**
```java
// PI controller for step size
dt_new = dt * (tol / lte)^(1/order) * safety_factor
dt_new = clamp(dt_new, dt_min, dt_max)
```

**3c. Event-Based Step Reduction**
- Reduce step when approaching predicted switching time
- Use derivative of control signals to predict events

**Key Files to Modify:**
- `SolverSettings.java` - Add adaptive settings (tol, dt_min, dt_max)
- `SimulationsKern.java` - Main adaptive loop
- NEW: `circuit/simulation/AdaptiveStepController.java`
- NEW: `circuit/simulation/LTEEstimator.java`

**Acceptance Criteria:**
- Automatic step reduction at switching events
- Step increase during steady-state
- User-configurable tolerance
- Backward compatible (fixed dt still works)

---

### Phase 4: Sparse Matrix Solver (Medium Priority, High Effort)

**What:** Replace dense LU with sparse matrix operations

**Why:**
- O(n) vs O(n³) scaling for large circuits
- Enables simulation of microgrids, MMC converters
- Memory efficiency for 1000+ node circuits

**Implementation Options:**

**Option A: Integrate Existing Library**
- Use `ejml` (Efficient Java Matrix Library) sparse module
- Already a Java library, easy integration

**Option B: Custom KLU-style Solver**
- Implement sparse LU with AMD ordering
- More control, optimized for circuit matrices

**Key Files to Modify:**
- `LUDecomposition.java` - Add sparse variant
- `LKMatrices.java` - Support sparse storage
- `pom.xml` - Add ejml-sparse dependency (if Option A)

**Acceptance Criteria:**
- MMC with 100 submodules simulates in reasonable time
- Memory usage scales linearly with circuit size
- Fallback to dense for small circuits

---

### Phase 5: Sampled Control Mode (Low Priority, Low Effort)

**What:** Execute control blocks at fixed sample rate, not every simulation step

**Why:**
- Realistic digital controller behavior
- Proper modeling of ADC/DAC delays
- Essential for control loop stability analysis

**Implementation:**
```java
class SampledControlExecutor {
    double samplePeriod;  // e.g., 100 µs for 10 kHz
    double lastSampleTime;

    void executeIfDue(double t) {
        if (t >= lastSampleTime + samplePeriod) {
            controlNL.berechneZeitschritt(samplePeriod, t);
            lastSampleTime = t;
        }
    }
}
```

**Key Files to Modify:**
- `NetzlisteCONTROL.java` - Add sample rate setting
- `SimulationsKern.java` - Conditional control execution

**Acceptance Criteria:**
- Control blocks execute at specified sample rate
- Sample-and-hold behavior visible in waveforms
- Configurable per control subsystem

---

## Implementation Priority & Roadmap

### Sprint 1: Foundation (Weeks 1-2)
- [ ] DC Operating Point Analysis
- [ ] Zero-Crossing Detection (basic linear interpolation)

### Sprint 2: Core Improvements (Weeks 3-4)
- [ ] Adaptive Time Stepping (LTE-based)
- [ ] Event prediction from control signals

### Sprint 3: Scalability (Weeks 5-6)
- [ ] Sparse Matrix Solver integration
- [ ] Performance benchmarking

### Sprint 4: Control Enhancements (Week 7)
- [ ] Sampled Control Mode
- [ ] Documentation & examples

---

## Key Files Summary

| New File | Purpose |
|----------|---------|
| `circuit/analysis/DCOperatingPointSolver.java` | DC steady-state solver |
| `circuit/simulation/ZeroCrossingDetector.java` | Event interpolation |
| `circuit/simulation/AdaptiveStepController.java` | Step size control |
| `circuit/simulation/LTEEstimator.java` | Truncation error estimation |

| Modified File | Changes |
|---------------|---------|
| `SimulationsKern.java` | DC init, adaptive loop, zero-crossing |
| `SolverSettings.java` | New solver options |
| `StateTransitionValidator.java` | Interpolation hooks |
| `NetzlisteCONTROL.java` | Sampled execution |
| `LUDecomposition.java` | Sparse support |

---

## Verification Plan

### Unit Tests
- `DCOperatingPointSolverTest.java` - RC circuit DC solution
- `ZeroCrossingDetectorTest.java` - Interpolation accuracy
- `AdaptiveStepControllerTest.java` - Step size adaptation

### Integration Tests
- Buck converter: Compare fixed vs adaptive (same accuracy, faster)
- LLC resonant: Zero-crossing improves waveform quality
- PFC boost: DC init eliminates startup

### Benchmarks
- 10-node circuit: Baseline performance
- 100-node circuit: Sparse vs dense comparison
- 1000-node MMC: Scalability test

---

## References

### SIMBA Documentation
- Simulation Engine: https://doc.simba.io/simulation_engine/
- Technical Resources: https://aesim-tech.github.io/simba-technical-resources/

### GeckoCIRCUITS Solver
- `SolverType.java`, `SolverContext.java` - Solver configuration
- `SimulationsKern.java` - Main simulation loop
- `LKMatrices.java` - Matrix assembly and MNA
- `LUDecompositionCache.java` - LU caching for performance

### Numerical Methods
- Backward Euler, Trapezoidal Rule, Gear-Shichman methods
- Modified Nodal Analysis (MNA)
- LU Decomposition with partial pivoting

### Sparse Solver Libraries
- **EJML**: https://ejml.org/ (Pure Java, Apache 2.0)
- **SuiteSparse/KLU**: https://people.engr.tamu.edu/davis/suitesparse.html (Circuit-optimized)
- **Eigen**: https://eigen.tuxfamily.org/ (C++ template library)

### Java Native Integration
- **JNA**: https://github.com/java-native-access/jna
- **Project Panama (FFM)**: https://openjdk.org/projects/panama/ (Java 21+)
- **GraalVM Native Image**: https://www.graalvm.org/native-image/
