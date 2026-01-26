# Circuit Package Refactoring and Test Plan

## Executive Summary

Comprehensive plan to refactor and test `ch.technokrat.gecko.geckocircuits.circuit` - the second largest package with **2% coverage** (889/41,036 instructions). Target: **38% coverage** through 8 sprints.

---

## Current State

| Metric | Value |
|--------|-------|
| Instructions | 2% (889 of 41,036) |
| Branches | 0% (0 of 3,612) |
| Classes | 26/137 covered |
| Lines | 230/7,843 covered |
| Total Files | 262 classes (~36,871 LOC) |

### Package Structure
- **Main package**: 92 files (17,841 LOC)
- **circuitcomponents/**: 138 files (15,137 LOC)
- **losscalculation/**: 22 files (2,398 LOC)
- **matrix/**: 10 files (1,495 LOC) - **99% covered** (success model)

### God Classes to Decompose
1. `SchematischeEingabe2.java` (2,245 LOC) - Editor UI
2. `LKMatrices.java` (1,523 LOC) - Matrix builder
3. `AbstractBlockInterface.java` (1,105 LOC) - Component base

---

## Sprint Plan

### Sprint 1: Semiconductor Stampers (Target: +8%)

**Goal**: Extract stamping logic from LKMatrices following the successful DiodeStamper pattern

**Files to Create**:
```
src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/
  ThyristorStamper.java
  IGBTStamper.java
  MOSFETStamper.java
  IdealSwitchStamper.java

src/test/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/
  ThyristorStamperTest.java
  IGBTStamperTest.java
  MOSFETStamperTest.java
  IdealSwitchStamperTest.java
```

**Reference Files**:
- Pattern: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/DiodeStamper.java`
- Interface: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/IStatefulStamper.java`
- Source: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LKMatrices.java` (extract LK_THYR, LK_IGBT, LK_MOSFET cases)

**Implementation Details**:

Each stamper follows the same pattern as DiodeStamper:
1. Implements `IStatefulStamper` interface
2. Has ON/OFF state with corresponding resistances
3. Stamps A matrix as: `a[x][x] += 1/R`, `a[y][y] += 1/R`, `a[x][y] -= 1/R`, `a[y][x] -= 1/R`
4. Stamps B vector for forward voltage: `b[x] += uf/R`, `b[y] -= uf/R`
5. Has state transition logic based on gate signal and voltage/current conditions

**Key Differences from DiodeStamper**:
- **ThyristorStamper**: Gate-controlled turn-on, current-zero turn-off, reverse recovery delay
- **IGBTStamper**: Gate-controlled turn-on AND turn-off, forward voltage drop
- **MOSFETStamper**: Gate-controlled bidirectional, no forward voltage (pure resistor)
- **IdealSwitchStamper**: Gate-controlled only, no voltage/current dependent switching

**LKMatrices Code Locations** (lines to extract):
- MOSFET: Lines 154-166 (A matrix), Lines 381-385 (B vector), Lines 630-640 (state update)
- IGBT: Lines 228-236 (A matrix), Lines 458-466 (B vector), Lines 967-1035 (state update)
- THYR: Lines 230-236 (A matrix), Lines 460-466 (B vector), Lines 918-965 (state update)

---

### Sprint 2: NetList Extraction (Target: +5%)

**Goal**: Extract testable units from NetListLK (583 LOC)

**Files to Create**:
```
src/main/java/ch/technokrat/gecko/geckocircuits/circuit/netlist/
  INetList.java
  NodeIndexer.java
  LabelResolver.java
  MutualCouplingRegistry.java

src/test/java/ch/technokrat/gecko/geckocircuits/circuit/netlist/
  NodeIndexerTest.java
  LabelResolverTest.java
  MutualCouplingRegistryTest.java
```

**Source File**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/NetListLK.java`

**Extraction Points**:
- `NodeIndexer`: Node numbering and ground reference logic
- `LabelResolver`: Component label to index mapping
- `MutualCouplingRegistry`: Mutual inductance coupling management

---

### Sprint 3: AbstractBlockInterface Decomposition (Target: +6%)

**Goal**: Extract cohesive managers from 1,105 LOC god class

**Files to Create**:
```
src/main/java/ch/technokrat/gecko/geckocircuits/circuit/component/
  ParameterRegistry.java
  ParameterSerializer.java
  ComponentPositionManager.java
  TerminalRegistry.java

src/test/java/ch/technokrat/gecko/geckocircuits/circuit/component/
  ParameterRegistryTest.java
  ParameterSerializerTest.java
  ComponentPositionManagerTest.java
  TerminalRegistryTest.java
```

**Source File**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractBlockInterface.java`

**Extraction Points**:
- `ParameterRegistry`: UserParameter management and access
- `ParameterSerializer`: Import/export of component parameters
- `ComponentPositionManager`: Position, rotation, mirroring logic
- `TerminalRegistry`: Terminal connection management

---

### Sprint 4: SimulationsKern Refactoring (Target: +5%)

**Goal**: Extract coordinators from simulation kernel (863 LOC)

**Files to Create**:
```
src/main/java/ch/technokrat/gecko/geckocircuits/circuit/simulation/
  ISimulationStep.java
  DataTransferCoordinator.java
  SwitchActionChecker.java
  ControlledSourceUpdater.java

src/test/java/ch/technokrat/gecko/geckocircuits/circuit/simulation/
  DataTransferCoordinatorTest.java
  SwitchActionCheckerTest.java
  ControlledSourceUpdaterTest.java
```

**Source File**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SimulationsKern.java`

**Extraction Points**:
- `DataTransferCoordinator`: Data flow between control and circuit
- `SwitchActionChecker`: Gate signal processing for switches
- `ControlledSourceUpdater`: Signal-controlled source value updates

---

### Sprint 5: Calculator Standardization (Target: +6%)

**Goal**: Align Calculator pattern with Stamper pattern

**Files to Modify/Create**:
```
src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/
  IComponentCalculator.java (new interface)
  CapacitorCalculator.java (refactor - 279 LOC)
  InductorCalculator.java (refactor)

src/test/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/
  CapacitorCalculatorTest.java
  InductorCalculatorTest.java
```

**Interface Definition**:
```java
public interface IComponentCalculator {
    void calculateCurrent(double[] nodeVoltages, double dt);
    void updateParameters(double time);
    double getCurrent();
}
```

---

### Sprint 6: Loss Calculation Coverage (Target: +4%)

**Goal**: Extract pure calculation logic from losscalculation/

**Files to Create**:
```
src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/
  ConductionLossCalculator.java
  SwitchingLossCalculator.java

src/test/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/
  ConductionLossCalculatorTest.java
  SwitchingLossCalculatorTest.java
  LossPropertiesTest.java
```

**Source File**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/losscalculation/VerlustBerechnungDetailed.java` (464 LOC)

**Extraction Points**:
- `ConductionLossCalculator`: P = I^2 * R_on calculations
- `SwitchingLossCalculator`: E_on/E_off energy loss interpolation

---

### Sprint 7: Terminal/Connection Cleanup (Target: +3%)

**Goal**: Standardize terminal hierarchy

**Files to Create**:
```
src/main/java/ch/technokrat/gecko/geckocircuits/circuit/terminal/
  ITerminalPosition.java
  ConnectionPath.java
  ConnectionValidator.java

src/test/java/ch/technokrat/gecko/geckocircuits/circuit/terminal/
  ConnectionPathTest.java
  ConnectionValidatorTest.java
```

**Source File**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/Verbindung.java` (505 LOC)

---

### Sprint 8: Integration & Cleanup (Target: +3%)

**Goal**: End-to-end tests and code quality

**Files to Create**:
```
src/test/java/ch/technokrat/gecko/geckocircuits/circuit/integration/
  SimpleCircuitSimulationTest.java
  SwitchingCircuitTest.java
  MatrixIntegrationTest.java
```

**Cleanup Tasks**:
- Remove 123 System.out.println statements
- Replace 27 bare catch(Exception) blocks
- Add SLF4J logging where appropriate

---

## Success Metrics

| Metric | Current | Mid-Point (Sprint 4) | Final (Sprint 8) |
|--------|---------|---------------------|------------------|
| Instructions | 2% | 18% | 38% |
| Branches | 0% | 12% | 25% |
| Classes Covered | 26 | 60 | 90 |
| Lines Covered | 230 | 2,500 | 5,000 |

---

## Critical Reference Files

1. **Pattern Reference**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/DiodeStamper.java` - 99% covered, follow this pattern
2. **Interface Template**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/IStatefulStamper.java` - interface for stateful components
3. **Primary Extraction Target**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LKMatrices.java` - 1,523 LOC matrix builder
4. **Simulation Core**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/SimulationsKern.java` - 863 LOC orchestrator
5. **Component Base**: `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractBlockInterface.java` - 1,105 LOC god class

---

## Verification Commands

After each sprint:
```bash
# Run tests
mvn test -Dtest="**/circuit/**Test"

# Generate coverage report
mvn jacoco:report

# View coverage
open target/site/jacoco/ch.technokrat.gecko.geckocircuits.circuit/index.html
```

### Golden Test Cases (create before refactoring)
1. Simple RC circuit - verify voltage decay
2. Buck converter - verify switching waveforms
3. Induction motor startup - verify current transients

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Breaking simulation accuracy | Golden test cases before extraction |
| .ipes file compatibility | Keep serialization in original location |
| GUI dependencies | Extract interfaces first, adapter pattern |
| Complex interdependencies | Feature flags for gradual rollout |

---

## Implementation Notes for AI Models

### For Sprint 1 (Semiconductor Stampers):

**Step-by-step instructions**:

1. **Read the pattern files first**:
   - `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/DiodeStamper.java`
   - `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/IStatefulStamper.java`
   - `src/test/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/DiodeStamperTest.java`

2. **Read the extraction source**:
   - `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/LKMatrices.java` (lines 150-240, 450-470, 910-1040)

3. **Create stampers in this order**:
   - `IdealSwitchStamper.java` (simplest - gate controlled only)
   - `MOSFETStamper.java` (gate controlled, no forward voltage)
   - `IGBTStamper.java` (gate controlled with forward voltage)
   - `ThyristorStamper.java` (most complex - gate trigger, current-zero turn-off, reverse recovery)

4. **Create corresponding tests** following `DiodeStamperTest.java` pattern

5. **Run verification**:
   ```bash
   mvn test -Dtest="**/matrix/*StamperTest"
   mvn jacoco:report
   ```

### Key Parameter Indices (from component classes):
- `parameter[0]` = current resistance (rD)
- `parameter[1]` = forward voltage (uf)
- `parameter[2]` = ON resistance (rON)
- `parameter[3]` = OFF resistance (rOFF)
- `parameter[8]` = gate signal (0 or 1)
- `parameter[9]` = reverse recovery time (thyristor only)
- `parameter[11]` = last switch time (thyristor only)

---

## File Locations Summary

```
GeckoCIRCUITS/
├── src/main/java/ch/technokrat/gecko/geckocircuits/circuit/
│   ├── matrix/                    # Stamper implementations (target: 99% coverage)
│   │   ├── IMatrixStamper.java
│   │   ├── IStatefulStamper.java
│   │   ├── DiodeStamper.java      # PATTERN TO FOLLOW
│   │   ├── ThyristorStamper.java  # Sprint 1 - CREATE
│   │   ├── IGBTStamper.java       # Sprint 1 - CREATE
│   │   ├── MOSFETStamper.java     # Sprint 1 - CREATE
│   │   └── IdealSwitchStamper.java # Sprint 1 - CREATE
│   ├── netlist/                   # Sprint 2 - CREATE
│   ├── component/                 # Sprint 3 - CREATE
│   ├── simulation/                # Sprint 4 - CREATE
│   ├── terminal/                  # Sprint 7 - CREATE
│   ├── LKMatrices.java           # EXTRACTION SOURCE
│   ├── SimulationsKern.java      # EXTRACTION SOURCE
│   └── AbstractBlockInterface.java # EXTRACTION SOURCE
├── src/test/java/ch/technokrat/gecko/geckocircuits/circuit/
│   ├── matrix/
│   │   ├── DiodeStamperTest.java  # TEST PATTERN TO FOLLOW
│   │   └── *StamperTest.java      # Sprint 1 - CREATE
│   └── integration/               # Sprint 8 - CREATE
└── pom.xml
```
