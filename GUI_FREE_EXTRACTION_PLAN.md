# GUI-Free Class Extraction Sprint Plan

**Goal:** Extract 171+ GUI-free classes from the legacy monolith into `gecko-simulation-core`
**Executor:** Claude Haiku
**Estimated Sprints:** 8 sprints
**Target Module:** `gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/`

---

## Executive Summary

| Source Package | GUI-Free Classes | Target Package | Sprint |
|---------------|-----------------|----------------|--------|
| `circuit.matrix` | 15 | `core.circuit.matrix` | E1 |
| `circuit.netlist` | 4 | `core.circuit.netlist` | E1 |
| `circuit.simulation` | 5 | `core.circuit.simulation` | E1 |
| `math` | 7 | `core.math` | E2 |
| `control.calculators` | 71 (of 73) | `core.control.calculators` | E3-E4 |
| `circuit.losscalculation` | 18 (of 24) | `core.circuit.losscalculation` | E5 |
| `circuit` (main) | 54 (of 101) | `core.circuit` | E6-E7 |
| `api` | 4 | `core.api` | E8 |
| **Total** | **~175** | | |

---

## Prerequisites

Before starting extraction:

1. **Verify module structure exists:**
```bash
ls -la gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/
# Should see: api/, circuit/, control/, math/ package-info.java files
```

2. **Verify tests pass:**
```bash
cd /home/tinix/claude_wsl/GeckoCIRCUITS
mvn test -q 2>&1 | tail -5
# Expect: Tests run: ~2426, Failures: 0, Errors: 3 (environmental)
```

3. **Create backup:**
```bash
git add -A && git commit -m "Pre-extraction checkpoint"
```

---

## Extraction Strategy

For each class, follow this procedure:

### Step 1: Check GUI-Free Status
```bash
# Verify no GUI imports
grep -n "import java.awt\|import javax.swing" src/main/java/ch/technokrat/gecko/geckocircuits/PACKAGE/CLASS.java
# Should return empty
```

### Step 2: Copy to New Location
```bash
cp src/main/java/ch/technokrat/gecko/geckocircuits/PACKAGE/CLASS.java \
   gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/PACKAGE/CLASS.java
```

### Step 3: Update Package Declaration
Change:
```java
package ch.technokrat.gecko.geckocircuits.PACKAGE;
```
To:
```java
package ch.technokrat.gecko.core.PACKAGE;
```

### Step 4: Update Internal Imports
Change imports from `ch.technokrat.gecko.geckocircuits.*` to `ch.technokrat.gecko.core.*`

### Step 5: Verify Compilation
```bash
cd gecko-simulation-core
mvn compile -q
```

### Step 6: Update Legacy Module to Use Core
In the original class location, consider:
- Option A: Delete and add core as dependency
- Option B: Make wrapper class that extends core class
- Option C: Keep both during transition (deprecate legacy)

---

# SPRINT E1: Core Packages (Fully GUI-Free)

**Classes to Extract:** 24
**Target Date:** Day 1
**Risk:** Low (already validated as 100% GUI-free)

## E1.1: circuit.matrix (15 classes)

### Files to Extract

| File | Lines | Dependencies | Notes |
|------|-------|--------------|-------|
| `CapacitorBEStamper.java` | ~80 | IMatrixStamper | Backward Euler |
| `CapacitorGSStamper.java` | ~80 | IMatrixStamper | Gear-Shichman |
| `CapacitorTRZStamper.java` | ~80 | IMatrixStamper | Trapezoidal |
| `CurrentSourceStamper.java` | ~50 | IMatrixStamper | Ideal source |
| `DiodeStamper.java` | ~100 | IMatrixStamper | Nonlinear |
| `IMatrixStamper.java` | ~30 | (interface) | Core interface |
| `InductorBEStamper.java` | ~80 | IMatrixStamper | Backward Euler |
| `InductorGSStamper.java` | ~80 | IMatrixStamper | Gear-Shichman |
| `InductorTRZStamper.java` | ~80 | IMatrixStamper | Trapezoidal |
| `MutualInductanceStamper.java` | ~100 | IMatrixStamper | Coupled inductors |
| `ResistorStamper.java` | ~50 | IMatrixStamper | Simple R |
| `StamperRegistry.java` | ~100 | All stampers | Factory |
| `SwitchStamper.java` | ~70 | IMatrixStamper | Ideal switch |
| `TransformerStamper.java` | ~120 | IMatrixStamper | Ideal transformer |
| `VoltageSourceStamper.java` | ~60 | IMatrixStamper | Ideal source |

### Extraction Commands

```bash
# Create target directory
mkdir -p gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/matrix

# Copy all matrix files
for f in CapacitorBEStamper CapacitorGSStamper CapacitorTRZStamper \
         CurrentSourceStamper DiodeStamper IMatrixStamper \
         InductorBEStamper InductorGSStamper InductorTRZStamper \
         MutualInductanceStamper ResistorStamper StamperRegistry \
         SwitchStamper TransformerStamper VoltageSourceStamper; do
    cp src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/${f}.java \
       gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/matrix/
done
```

### Package Update Script

```bash
# Update package declarations
find gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/matrix -name "*.java" \
    -exec sed -i 's/package ch\.technokrat\.gecko\.geckocircuits\.circuit\.matrix;/package ch.technokrat.gecko.core.circuit.matrix;/g' {} \;

# Update internal imports
find gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/matrix -name "*.java" \
    -exec sed -i 's/import ch\.technokrat\.gecko\.geckocircuits\./import ch.technokrat.gecko.core./g' {} \;
```

---

## E1.2: circuit.netlist (4 classes)

### Files to Extract

| File | Lines | Dependencies | Notes |
|------|-------|--------------|-------|
| `NetListBuilder.java` | ~200 | - | Builder pattern |
| `NetListLK.java` | ~300 | - | Main netlist |
| `NodeMapping.java` | ~100 | - | Terminal→node map |
| `CircuitGraphValidator.java` | ~150 | - | Topology validation |

### Extraction Commands

```bash
mkdir -p gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/netlist

for f in NetListBuilder NetListLK NodeMapping CircuitGraphValidator; do
    if [ -f "src/main/java/ch/technokrat/gecko/geckocircuits/circuit/netlist/${f}.java" ]; then
        cp src/main/java/ch/technokrat/gecko/geckocircuits/circuit/netlist/${f}.java \
           gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/netlist/
    fi
done

# List actual files (names may differ)
ls src/main/java/ch/technokrat/gecko/geckocircuits/circuit/netlist/
```

---

## E1.3: circuit.simulation (2-5 classes)

### Files to Extract

| File | Lines | Notes |
|------|-------|-------|
| `SolverContext.java` | ~150 | Solver configuration |
| `SimulationState.java` | ~200 | State variables |

### Extraction Commands

```bash
mkdir -p gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/simulation

# List actual files first
ls src/main/java/ch/technokrat/gecko/geckocircuits/circuit/simulation/

# Copy simulation files
for f in $(ls src/main/java/ch/technokrat/gecko/geckocircuits/circuit/simulation/*.java | xargs -n1 basename | sed 's/.java//'); do
    cp src/main/java/ch/technokrat/gecko/geckocircuits/circuit/simulation/${f}.java \
       gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/simulation/
done
```

---

## E1 Verification

After E1 completion:

```bash
cd gecko-simulation-core
mvn compile -q
# Should compile without errors

# Count files
find src -name "*.java" | wc -l
# Expected: ~24 (including package-info.java files)

# Run core tests
mvn test -q
```

---

# SPRINT E2: Math Package (7 classes)

**Classes to Extract:** 7
**Risk:** Low

## Files to Extract

| File | Lines | Dependencies | Notes |
|------|-------|--------------|-------|
| `Matrix.java` | ~400 | - | Dense matrix + LU |
| `NComplex.java` | ~200 | - | Complex numbers |
| `GlobalMatrixMath.java` | ~150 | - | Static utilities |
| `SparseMatrix.java` | ~300 | - | CSR format |
| `FFT.java` | ~250 | NComplex | Fast Fourier Transform |
| `LUDecomposition.java` | ~200 | Matrix | Extracted from Matrix |
| `EigenvalueDecomposition.java` | ~300 | Matrix | Optional |

### Extraction Commands

```bash
mkdir -p gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/math

# List actual files
ls src/main/java/ch/technokrat/gecko/geckocircuits/math/

# Copy math files
for f in $(ls src/main/java/ch/technokrat/gecko/geckocircuits/math/*.java | xargs -n1 basename | sed 's/.java//'); do
    cp src/main/java/ch/technokrat/gecko/geckocircuits/math/${f}.java \
       gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/math/
done

# Update packages
find gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/math -name "*.java" \
    -exec sed -i 's/package ch\.technokrat\.gecko\.geckocircuits\.math;/package ch.technokrat.gecko.core.math;/g' {} \;
```

---

# SPRINT E3: Control Calculators Part 1 (35 classes)

**Classes to Extract:** 35 (of 71 GUI-free)
**Risk:** Medium (large number of interconnected classes)

## E3.1: Signal Generation Calculators

| File | Dependencies | Notes |
|------|--------------|-------|
| `AbstractControlCalculatable.java` | - | Base class |
| `SineCalculator.java` | AbstractControlCalculatable | |
| `CosineCalculator.java` | AbstractControlCalculatable | |
| `SquareWaveCalculator.java` | AbstractControlCalculatable | |
| `TriangleWaveCalculator.java` | AbstractControlCalculatable | |
| `SawtoothCalculator.java` | AbstractControlCalculatable | |
| `ConstantCalculator.java` | AbstractControlCalculatable | |
| `StepCalculator.java` | AbstractControlCalculatable | |
| `RampCalculator.java` | AbstractControlCalculatable | |
| `NoiseCalculator.java` | AbstractControlCalculatable | |
| `PWMCalculator.java` | AbstractControlCalculatable | |

## E3.2: Mathematical Calculators

| File | Dependencies | Notes |
|------|--------------|-------|
| `AddCalculator.java` | AbstractControlCalculatable | |
| `SubtractCalculator.java` | AbstractControlCalculatable | |
| `MultiplyCalculator.java` | AbstractControlCalculatable | |
| `DivideCalculator.java` | AbstractControlCalculatable | |
| `GainCalculator.java` | AbstractControlCalculatable | |
| `AbsoluteValueCalculator.java` | AbstractControlCalculatable | |
| `SignCalculator.java` | AbstractControlCalculatable | |
| `LimiterCalculator.java` | AbstractControlCalculatable | |
| `DeadZoneCalculator.java` | AbstractControlCalculatable | |
| `MinCalculator.java` | AbstractControlCalculatable | |
| `MaxCalculator.java` | AbstractControlCalculatable | |
| `SqrtCalculator.java` | AbstractControlCalculatable | |
| `PowerCalculator.java` | AbstractControlCalculatable | |
| `ExpCalculator.java` | AbstractControlCalculatable | |
| `LogCalculator.java` | AbstractControlCalculatable | |

## E3.3: Integration/Differentiation

| File | Dependencies | Notes |
|------|--------------|-------|
| `IntegratorCalculator.java` | AbstractControlCalculatable | |
| `DifferentiatorCalculator.java` | AbstractControlCalculatable | |
| `PT1Calculator.java` | AbstractControlCalculatable | First order |
| `PT2Calculator.java` | AbstractControlCalculatable | Second order |
| `PIDCalculator.java` | AbstractControlCalculatable | Full PID |
| `PICalculator.java` | AbstractControlCalculatable | |
| `PDCalculator.java` | AbstractControlCalculatable | |
| `LeadLagCalculator.java` | AbstractControlCalculatable | |
| `TransferFunctionCalculator.java` | AbstractControlCalculatable | Generic |

### Extraction Commands

```bash
mkdir -p gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/control/calculators

# First, check actual file names in the package
ls src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/ | head -40

# Extract base class first (critical)
cp src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/AbstractControlCalculatable.java \
   gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/control/calculators/

# Then copy signal generators (adjust names based on actual files)
# ... continue with actual file names
```

### Important: Exclude GUI-Coupled Classes

Do NOT extract these files (they have GUI dependencies):
- `DEMUXCalculator.java` - depends on ReglerDemux (GUI)
- `SpaceVectorCalculator.java` - depends on SpaceVectorDisplay (GUI)

Verification:
```bash
# Verify these have GUI imports
grep -l "import java.awt\|import javax.swing" \
    src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/*.java
# Should show only DEMUXCalculator.java and SpaceVectorCalculator.java
```

---

# SPRINT E4: Control Calculators Part 2 (36 classes)

**Classes to Extract:** 36 (remaining GUI-free)

## E4.1: Signal Analysis Calculators

| File | Dependencies | Notes |
|------|--------------|-------|
| `RMSCalculator.java` | AbstractControlCalculatable | Root mean square |
| `THDCalculator.java` | AbstractControlCalculatable, FFT | Total harmonic distortion |
| `MeanCalculator.java` | AbstractControlCalculatable | Average |
| `PeakCalculator.java` | AbstractControlCalculatable | Peak detect |
| `FFTCalculator.java` | AbstractControlCalculatable, FFT | Frequency analysis |
| `HarmonicCalculator.java` | AbstractControlCalculatable, FFT | Individual harmonics |
| `PowerCalculator.java` | AbstractControlCalculatable | P, Q, S |
| `PowerFactorCalculator.java` | AbstractControlCalculatable | cos(phi) |

## E4.2: Logic Calculators

| File | Dependencies | Notes |
|------|--------------|-------|
| `ANDCalculator.java` | AbstractControlCalculatable | |
| `ORCalculator.java` | AbstractControlCalculatable | |
| `NOTCalculator.java` | AbstractControlCalculatable | |
| `XORCalculator.java` | AbstractControlCalculatable | |
| `NANDCalculator.java` | AbstractControlCalculatable | |
| `NORCalculator.java` | AbstractControlCalculatable | |
| `ComparatorCalculator.java` | AbstractControlCalculatable | |
| `HysteresisComparatorCalculator.java` | AbstractControlCalculatable | |
| `SRFlipFlopCalculator.java` | AbstractControlCalculatable | |
| `DFlipFlopCalculator.java` | AbstractControlCalculatable | |
| `JKFlipFlopCalculator.java` | AbstractControlCalculatable | |
| `CounterCalculator.java` | AbstractControlCalculatable | |

## E4.3: Utility Calculators

| File | Dependencies | Notes |
|------|--------------|-------|
| `SampleHoldCalculator.java` | AbstractControlCalculatable | |
| `DelayCalculator.java` | AbstractControlCalculatable | |
| `MuxCalculator.java` | AbstractControlCalculatable | |
| `SelectorCalculator.java` | AbstractControlCalculatable | |
| `LookupTableCalculator.java` | AbstractControlCalculatable | |
| `InterpolatorCalculator.java` | AbstractControlCalculatable | |
| `HysteresisCalculator.java` | AbstractControlCalculatable | |
| `QuantizerCalculator.java` | AbstractControlCalculatable | |
| `RateLimiterCalculator.java` | AbstractControlCalculatable | |
| `SlewRateLimiterCalculator.java` | AbstractControlCalculatable | |

---

# SPRINT E5: Loss Calculation (18 classes)

**Classes to Extract:** 18 (of 24 total)
**Risk:** Medium

## GUI-Free Classes to Extract

| File | Lines | Notes |
|------|-------|-------|
| `AbstractLossCalculation.java` | ~150 | Base class |
| `ConductionLoss.java` | ~100 | Interface |
| `SwitchingLoss.java` | ~100 | Interface |
| `DiodeConductionLoss.java` | ~120 | |
| `DiodeSwitchingLoss.java` | ~150 | Reverse recovery |
| `IGBTConductionLoss.java` | ~120 | |
| `IGBTSwitchingLoss.java` | ~180 | Eon, Eoff |
| `MOSFETConductionLoss.java` | ~100 | Rds_on |
| `MOSFETSwitchingLoss.java` | ~150 | |
| `ThermalModel.java` | ~200 | Rth, Cth |
| `ThermalNetwork.java` | ~250 | Foster/Cauer |
| `LossDataInterpolation.java` | ~150 | Lookup tables |
| `LossCurve.java` | ~100 | Single curve |
| `LossDataset.java` | ~120 | Multiple curves |
| `TemperatureDependence.java` | ~80 | Scaling |
| `TotalLossCalculator.java` | ~100 | Sum all losses |
| `AverageLossCalculator.java` | ~80 | Period average |
| `JunctionTemperatureCalculator.java` | ~150 | Tj estimation |

## GUI Panel Classes (DO NOT EXTRACT)

These classes have Swing dependencies and must remain in desktop module:

- `DetailedConductionLossPanel.java`
- `DetailedSwitchingLossesPanel.java`  
- `DetailledLossPanel.java`
- `DialogVerlusteDetail.java`
- `JPanelLossDataInterpolationSettings.java`
- `LossCurveTemperaturePanel.java`

### Verification Before Extraction

```bash
# List all files and check GUI status
for f in $(ls src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/*.java | xargs -n1 basename); do
    if grep -q "import java.awt\|import javax.swing" "src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/$f"; then
        echo "GUI: $f"
    else
        echo "OK:  $f"
    fi
done
```

---

# SPRINT E6: Circuit Main Package Part 1 (27 classes)

**Classes to Extract:** 27 (of 54 GUI-free in circuit main)
**Risk:** High (many interdependencies)

## E6.1: Core Value Types (12 classes)

| File | Dependencies | Notes |
|------|--------------|-------|
| `GridPoint.java` | - | Position in grid |
| `ComponentDirection.java` | - | Enum: N/E/S/W |
| `ComponentState.java` | - | Enum: states |
| `CircuitSourceType.java` | - | Enum: source types |
| `ControlSourceType.java` | - | Enum: control sources |
| `EnumTerminalLocation.java` | - | Enum: terminal positions |
| `LabelPriority.java` | - | Enum: label priorities |
| `SpecialTyp.java` | - | Enum: special components |
| `TokenMap.java` | - | String key-value map |
| `UniqueObjectIdentifer.java` | - | ID generator |
| `NameAlreadyExistsException.java` | - | Exception |
| `MapList.java` | - | Utility collection |

## E6.2: Interfaces (8 classes)

| File | Dependencies | Notes |
|------|--------------|-------|
| `ComponentCoupable.java` | - | Coupling interface |
| `ComponentTerminable.java` | - | Terminal interface |
| `ControlTerminable.java` | - | Control terminal |
| `CurrentMeasurable.java` | - | Current measurement |
| `DirectVoltageMeasurable.java` | - | Voltage measurement |
| `GlobalTerminable.java` | - | Global terminal |
| `Enabled.java` | - | Enable/disable |
| `PostCalculatable.java` | - | Post-processing |

## E6.3: Domain Classes (7 classes)

| File | Dependencies | Notes |
|------|--------------|-------|
| `TimeFunction.java` | - | Interface |
| `TimeFunctionConstant.java` | TimeFunction | Constant value |
| `Label.java` | - | Text label |
| `Labable.java` | - | Interface |
| `PotentialArea.java` | - | Node potential |
| `PotentialCoupable.java` | - | Interface |
| `Schliessable.java` | - | Interface |

### Extraction Order

**Critical**: Extract in dependency order:
1. Enums and exceptions first (no dependencies)
2. Interfaces second
3. Domain classes last

```bash
mkdir -p gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit

# Step 1: Enums
for f in GridPoint ComponentDirection ComponentState CircuitSourceType \
         ControlSourceType EnumTerminalLocation LabelPriority SpecialTyp; do
    cp src/main/java/ch/technokrat/gecko/geckocircuits/circuit/${f}.java \
       gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/ 2>/dev/null || echo "Not found: $f"
done

# Step 2: Utilities
for f in TokenMap UniqueObjectIdentifer NameAlreadyExistsException MapList; do
    cp src/main/java/ch/technokrat/gecko/geckocircuits/circuit/${f}.java \
       gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/ 2>/dev/null || echo "Not found: $f"
done

# Step 3: Interfaces
for f in ComponentCoupable ComponentTerminable ControlTerminable \
         CurrentMeasurable DirectVoltageMeasurable GlobalTerminable \
         Enabled PostCalculatable; do
    cp src/main/java/ch/technokrat/gecko/geckocircuits/circuit/${f}.java \
       gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/ 2>/dev/null || echo "Not found: $f"
done
```

---

# SPRINT E7: Circuit Main Package Part 2 (27 classes)

**Classes to Extract:** 27 (remaining GUI-free)

## E7.1: Matrix and Solver Classes (10 classes)

| File | Dependencies | Notes |
|------|--------------|-------|
| `GeckoMatrix.java` | - | Matrix wrapper |
| `AbstractCachedMatrix.java` | - | Caching base |
| `CachedMatrix.java` | AbstractCachedMatrix | Dense cached |
| `LKMatrices.java` | - | LK matrix container |
| `LUDecompositionCache.java` | - | LU cache |
| `SymmetricSparseMatrix.java` | - | Sparse symmetric |
| `SymmetricDoubleSparseMatrix.java` | - | Double sparse |
| `Paradiso.java` | - | Sparse solver interface |
| `PardisoCachedMatrix.java` | - | Pardiso wrapper |
| `SolverSettings.java` | - | Solver configuration |

## E7.2: Circuit Description Classes (10 classes)

| File | Dependencies | Notes |
|------|--------------|-------|
| `NetListContainer.java` | - | Netlist wrapper |
| `NetListLK.java` | - | Main netlist |
| `NetzlisteAllg.java` | - | General netlist |
| `SimulationsKern.java` | - | **Critical**: Simulation engine |
| `GeckoFileable.java` | - | File interface |
| `IpesFileable.java` | GeckoFileable | IPES format |
| `AbstractTypeInfo.java` | - | Type info base |
| `CircuitTypeInfo.java` | AbstractTypeInfo | Circuit type |
| `ElementDisplayProperties.java` | - | Display config |
| `HiddenSubCircuitable.java` | - | Subcircuit interface |

## E7.3: Coupling and Labeling (7 classes)

| File | Dependencies | Notes |
|------|--------------|-------|
| `ComponentCoupling.java` | ComponentCoupable | Coupling impl |
| `PotentialCoupling.java` | PotentialCoupable | Potential coupling |
| `CircuitLabel.java` | Label | Circuit label |
| `LabelConflictResolver.java` | - | Name conflicts |
| `InvisibleEdit.java` | - | Edit helper |
| `SubCircuitTerminable.java` | - | Subcircuit terminal |
| `ConnectorType.java` | - | Connector type |

### Note on SimulationsKern

`SimulationsKern.java` is the heart of the simulation engine. Before extraction:

1. **Check GUI dependencies:**
```bash
grep "import java.awt\|import javax.swing" \
    src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SimulationsKern.java
```

2. **If GUI-coupled**, create a GUI-free interface:
```java
// In core module
public interface ISimulationEngine {
    void initialize(NetListLK netlist, SolverSettings settings);
    void step(double dt);
    double[] getNodeVoltages();
    void stop();
}
```

---

# SPRINT E8: API Package (4 classes)

**Classes to Extract:** 4
**Risk:** Low

## Files to Extract

| File | Lines | Notes |
|------|-------|-------|
| `IMatrixStamper.java` | ~30 | Already in matrix |
| `ISimulationEngine.java` | ~50 | Create if needed |
| `ICircuitBuilder.java` | ~40 | Create if needed |
| `SimulationResult.java` | ~80 | Result container |

---

## Post-Extraction Tasks

### 1. Update Legacy Module pom.xml

Add dependency on core module:

```xml
<dependency>
    <groupId>com.technokrat.gecko</groupId>
    <artifactId>gecko-simulation-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Update Imports in Legacy Code

Create script to update imports:

```bash
# Find all Java files that import from extracted packages
grep -rl "import ch.technokrat.gecko.geckocircuits.circuit.matrix" src/main/java/ | \
    xargs sed -i 's/import ch\.technokrat\.gecko\.geckocircuits\.circuit\.matrix\./import ch.technokrat.gecko.core.circuit.matrix./g'
```

### 3. Run Full Test Suite

```bash
cd /home/tinix/claude_wsl/GeckoCIRCUITS
mvn clean test -q
# All tests should still pass
```

### 4. Update CorePackageValidationTest

Modify the test to validate the core module instead of inline packages.

---

## Verification Checklist

After each sprint:

- [ ] Core module compiles: `cd gecko-simulation-core && mvn compile`
- [ ] Core module tests pass: `mvn test`
- [ ] Legacy module still compiles: `cd .. && mvn compile -pl gecko`
- [ ] Legacy tests still pass: `mvn test -pl gecko`
- [ ] No GUI imports in core: `grep -r "import java.awt" gecko-simulation-core/`
- [ ] Git commit created: `git add -A && git commit -m "Sprint EX complete"`

---

## GUI Classes Reference (DO NOT EXTRACT)

These 47 classes in `circuit` main package have GUI dependencies:

```
AbstractBlockInterface.java
AbstractCircuitSheetComponent.java
AbstractTerminal.java
AwtGraphicsAdapter.java
CircuitSheet.java
DataTablePanel.java
DataTablePanelParameters.java
DialogCircuitComponent.java
DialogGlobalTerminal.java
DialogModule.java
DialogNonLinearity.java
GeckoUndoableEditAdapter.java
IDStringDialog.java
KnotenLabel.java
MyTableCellEditor.java
MyTableCellRenderer.java
NonLinearDialogPanel.java
SchematicComponentSelection2.java
SchematicEditor2.java
SchematicTextInfo.java
TerminalControl.java
TerminalControlBidirectional.java
TerminalControlInput.java
TerminalControlOutput.java
TerminalFixedPosition.java
TerminalFixedPositionInvisible.java
TerminalHiddenSubcircuit.java
TerminalInterface.java
TerminalRelativeFixedDirection.java
TerminalRelativePosition.java
TerminalRelativePositionReluctance.java
TerminalSubCircuitBlock.java
TerminalToWrap.java
TerminalTwoPortComponent.java
TerminalTwoPortRelativeFixedDirection.java
TerminalVerbindung.java
ToolBar.java
Verbindung.java
VerbindungShortConnector.java
WirePathCalculator.java
WorksheetSize.java
```

These remain in the `gecko-desktop` module.

---

## Estimated Timeline

| Sprint | Duration | Cumulative Classes |
|--------|----------|-------------------|
| E1 | 1 day | 24 |
| E2 | 0.5 days | 31 |
| E3 | 1 day | 66 |
| E4 | 1 day | 102 |
| E5 | 1 day | 120 |
| E6 | 1 day | 147 |
| E7 | 1 day | 174 |
| E8 | 0.5 days | 178 |
| **Total** | **7-8 days** | **~178 classes** |

---

*Document created: January 27, 2026*
*For execution by: Claude Haiku*
*Prerequisites: gecko-simulation-core module structure must exist*
