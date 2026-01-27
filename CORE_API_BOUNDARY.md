# Core API Boundary Documentation

## Overview

This document describes the architectural boundaries between the **GUI-free core** (backend) 
and the **desktop GUI** (frontend) in GeckoCIRCUITS. These boundaries enable future headless 
operation and REST API access to the simulation engine.

## GUI-Free Core Packages

The following packages have been validated as **GUI-free** and can be used in headless 
environments (servers, CLI tools, REST APIs):

### Priority 1: 100% GUI-Free (Validated Sprint 15)

| Package | Classes | Coverage | Purpose |
|---------|---------|----------|---------|
| `circuit.matrix` | 15 | 77% | MNA matrix stampers for circuit simulation |
| `circuit.netlist` | 4 | 99% | Netlist building and parsing |
| `circuit.simulation` | 2 | 97% | Simulation engine components |
| `math` | 7 | 55% | Matrix operations, LU decomposition |

**Total: 28 GUI-free classes with 100% boundary enforcement**

### Priority 2: Partially GUI-Free (Validated Sprint 15)

| Package | GUI-Free | GUI-Coupled | Percentage | Notes |
|---------|----------|-------------|------------|-------|
| `control.calculators` | 71 | 2 | **97%** | DEMUXCalculator, SpaceVectorCalculator use GUI |
| `circuit.losscalculation` | 18 | 6 | **75%** | Panel classes are GUI-only |
| `circuit` (main) | 54 | 41 | **57%** | Core simulation vs. editors/dialogs |

**Total: 143 additional GUI-free classes validated**

## API Validation Tests

The core packages are validated by two test suites:

### CorePackageValidationTest
Location: `src/test/java/ch/technokrat/gecko/geckocircuits/core/CorePackageValidationTest.java`

- Scans source files for GUI imports (`java.awt`, `javax.swing`)
- Verifies package boundaries are maintained
- Documents expected class counts

### CoreApiIntegrationTest
Location: `src/test/java/ch/technokrat/gecko/geckocircuits/core/CoreApiIntegrationTest.java`

- Demonstrates complete circuit simulation without GUI
- Tests Matrix operations and LU decomposition
- Tests MNA stamping via IMatrixStamper interface
- Simulates RC circuit with Backward Euler integration

## Key Interfaces

### IMatrixStamper
The core interface for circuit component stamping:

```java
public interface IMatrixStamper {
    void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                      double[] parameter, double dt);
    void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                      double[] parameter, double dt, double time,
                      double[] previousValues);
    double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                           double[] parameter, double dt, double previousCurrent);
    double getAdmittanceWeight(double parameter, double dt);
}
```

### SolverContext
Encapsulates solver configuration:

```java
public class SolverContext {
    public static final int SOLVER_BE = 0;   // Backward Euler
    public static final int SOLVER_TRZ = 1;  // Trapezoidal
    public static final int SOLVER_GS = 2;   // Gear-Shichman
    
    public double getCapacitorConductance(double capacitance);
    public double getInductorConductance(double inductance);
    public double getTrapezoidalScale();
}
```

### StamperRegistry
Strategy pattern for component stampers:

```java
StamperRegistry registry = StamperRegistry.createDefault();
IMatrixStamper resistor = registry.getStamper(CircuitTyp.LK_R);
resistor.stampMatrixA(matrix, node1, node2, 0, new double[]{1000.0}, dt);
```

## Prohibited Imports in Core Packages

The following imports are **forbidden** in core packages:

```java
// FORBIDDEN in core packages
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.applet.*;
```

If any of these appear, `CorePackageValidationTest` will fail.

## Future Work

### Phase 1: Complete (Sprint 15)
- ✅ Validated core packages are GUI-free
- ✅ Created boundary validation tests
- ✅ Documented clean interfaces

### Phase 2: Extract gecko-core Module
- [ ] Create `gecko-core/pom.xml` with minimal dependencies
- [ ] Move GUI-free packages to separate Maven module
- [ ] Update main module to depend on gecko-core

### Phase 3: REST API
- [ ] Create `gecko-rest-api` module
- [ ] Expose simulation through JAX-RS endpoints
- [ ] Enable headless circuit simulation via HTTP

## Test Results (Sprint 15)

```
Tests run: 2387, Failures: 0, Errors: 3 (environmental)
- Added: 6 new validation tests (Priority 2)
- Total validation tests: 13
- GUI-free classes validated: 171 (28 core + 143 partial)
```

### Validation Test Coverage

| Test | Package | Classes Validated |
|------|---------|-------------------|
| `matrixPackageIsGuiFree` | circuit.matrix | 15 |
| `netlistPackageIsGuiFree` | circuit.netlist | 4 |
| `simulationPackageIsGuiFree` | circuit.simulation | 2 |
| `mathPackageIsGuiFree` | math | 7 |
| `calculatorsPackageIsMostlyGuiFree` | control.calculators | 71 |
| `losscalculationPackageIsMostlyGuiFree` | circuit.losscalculation | 18 |
| `circuitMainPackageGuiFreeClassesRemainGuiFree` | circuit | 54 |

## References

- [COVERAGE_PROGRESS.md](COVERAGE_PROGRESS.md) - Test coverage metrics
- [STRATEGIC_ROADMAP_DUAL_TRACK.md](STRATEGIC_ROADMAP_DUAL_TRACK.md) - Architecture roadmap
- [BACKEND_COVERAGE_PLAN.md](BACKEND_COVERAGE_PLAN.md) - Backend coverage strategy
