# Option D: Full Architectural Refactoring Plan

## Overview

**Goal:** Refactor GeckoCIRCUITS original codebase to separate GUI concerns from simulation logic, enabling complete Priority 3+ extraction.

**Estimated Effort:** 6-8 hours (can be split across sessions)  
**Expected Outcome:** 160+ GUI-free classes extractable, clean modular architecture  
**Risk Level:** Medium (modifying original code, but with tests and incremental approach)

---

## Phase 1: Foundation - Refactor CircuitComponent Hierarchy (2-3 hours)

### 1.1 Analyze CircuitComponent Base Class

**Current State:**
```java
// src/main/java/.../circuit/circuitcomponents/CircuitComponent.java
public abstract class CircuitComponent<T extends AbstractTwoPortLKreisBlock> {
    // Mixed concerns:
    import java.awt.*;           // GUI - REMOVE
    import javax.swing.*;        // GUI - REMOVE
    
    // Simulation logic (KEEP)
    protected double _voltage, _current;
    protected int[] matrixIndices;
    abstract void stampConductances(BStampable stamp);
    abstract void calculateCurrent(double... vals);
    
    // GUI logic (EXTRACT TO SEPARATE CLASS)
    abstract void draw(Graphics2D g);
    abstract void showProperties(JDialog d);
}
```

**Target Architecture:**
```
CircuitComponent (original - simulation + GUI)
         ↓ refactor to
         ↓
┌────────────────────────────────────────┐
│  ICircuitCalculator (interface)        │  ← Pure simulation interface
│    - stampConductances()               │
│    - calculateCurrent()                │
│    - getVoltage(), getCurrent()        │
└────────────────────────────────────────┘
         ↑ implements
         │
┌────────────────────────────────────────┐
│  CircuitComponentCore (abstract)       │  ← Pure simulation logic (EXTRACTABLE)
│    - _voltage, _current, matrixIndices │
│    - Core calculation methods          │
│    - NO java.awt imports               │
└────────────────────────────────────────┘
         ↑ extends
         │
┌────────────────────────────────────────┐
│  CircuitComponent (GUI + Core)         │  ← Desktop GUI version (legacy)
│    - extends CircuitComponentCore      │
│    - draw(), showProperties()          │
│    - java.awt imports here ONLY        │
└────────────────────────────────────────┘
```

### 1.2 Create ICircuitCalculator Interface

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ICircuitCalculator.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.BStampable;

/**
 * Pure simulation interface for circuit components.
 * No GUI dependencies - can be extracted to gecko-simulation-core.
 */
public interface ICircuitCalculator {
    
    void init();
    
    void assignTerminalIndices();
    
    void stampConductanceMatrix(BStampable gMatrix);
    
    void stampSourceVector(BStampable bVector);
    
    void calculateCurrent(double dt);
    
    void calculateVoltage(double pot1, double pot2);
    
    double getVoltage();
    
    double getCurrent();
    
    double getPotential(int terminalNumber);
    
    void setMatrixIndices(int index1, int index2);
    
    int[] getMatrixIndices();
    
    void updateHistory(double time);
    
    void stepBack();
    
    SolverType getSolverType();
}
```

### 1.3 Create CircuitComponentCore Abstract Class

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitComponentCore.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.BStampable;

/**
 * Core simulation logic for circuit components.
 * NO GUI IMPORTS - extractable to gecko-simulation-core.
 */
public abstract class CircuitComponentCore implements ICircuitCalculator {
    
    protected int[] matrixIndices = new int[]{-1, -1};
    protected double _current;
    protected double _voltage;
    protected double _potential1;
    protected double _potential2;
    protected double _potOld1;
    protected double _potOld2;
    protected double _oldCurrent;
    protected double _oldOldCurrent;
    
    // History for stepping back
    protected double prev_time = -1;
    protected boolean stepped_back = false;
    protected double[][] var_history;
    protected int steps_saved = 2;
    protected int steps_reversed = 0;
    
    // Constants
    public static final double DEFAULT_U_FORWARD = 0.6;
    public static final double DEFAULT_R_ON = 10e-3;
    public static final double DEFAULT_R_OFF = 10e6;
    
    protected static double disturbanceValue;
    protected final SolverType _solverType;
    
    protected CircuitComponentCore(SolverType solverType) {
        this._solverType = solverType;
        this.var_history = new double[steps_saved][9];
    }
    
    @Override
    public void init() {
        _current = 0;
        _oldCurrent = 0;
        _oldOldCurrent = 0;
        _voltage = 0;
        _potential1 = 0;
        _potential2 = 0;
        _potOld1 = 0;
        _potOld2 = 0;
    }
    
    @Override
    public double getVoltage() {
        return _voltage;
    }
    
    @Override
    public double getCurrent() {
        return _current;
    }
    
    @Override
    public double getPotential(int terminalNumber) {
        switch (terminalNumber) {
            case 0: return _potential1;
            case 1: return _potential2;
            default: return 0;
        }
    }
    
    @Override
    public void setMatrixIndices(int index1, int index2) {
        matrixIndices[0] = index1;
        matrixIndices[1] = index2;
    }
    
    @Override
    public int[] getMatrixIndices() {
        return matrixIndices;
    }
    
    @Override
    public SolverType getSolverType() {
        return _solverType;
    }
    
    public double getOldCurrent() {
        return _oldCurrent;
    }
    
    // Abstract methods for specific component implementations
    @Override
    public abstract void stampConductanceMatrix(BStampable gMatrix);
    
    @Override
    public abstract void stampSourceVector(BStampable bVector);
    
    @Override
    public abstract void calculateCurrent(double dt);
    
    @Override
    public abstract void calculateVoltage(double pot1, double pot2);
    
    @Override
    public abstract void updateHistory(double time);
    
    @Override
    public abstract void stepBack();
}
```

### 1.4 Refactor Original CircuitComponent

**Modified:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/CircuitComponent.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.allg.MainWindow;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.CircuitComponentCore;
import java.awt.*;  // GUI imports stay HERE in subclass

/**
 * GUI-enabled circuit component for desktop application.
 * Extends CircuitComponentCore (pure simulation logic).
 */
public abstract class CircuitComponent<T extends AbstractTwoPortLKreisBlock> 
    extends CircuitComponentCore {

    protected final AbstractTerminal _term1;
    protected AbstractTerminal _term2;
    protected final T _parent;

    public CircuitComponent(final T parent) {
        super(MainWindow._solverSettings.SOLVER_TYPE.getValue());
        _parent = parent;
        _term1 = parent.XIN.get(0);
        _term2 = parent.YOUT.get(0);
    }

    @Override
    public void assignTerminalIndices() {
        matrixIndices[0] = _term1.getIndex();
        matrixIndices[1] = _term2.getIndex();
    }
    
    // GUI-specific methods stay here
    public abstract void draw(Graphics2D g, Rectangle bounds);
    public abstract void showProperties();
    
    // Getter for parent (GUI-specific)
    public T getParent() {
        return _parent;
    }
}
```

### 1.5 Tasks Summary - Phase 1

| Task | File | Action | Time Est. |
|------|------|--------|-----------|
| 1.1 | ICircuitCalculator.java | CREATE new interface | 15 min |
| 1.2 | CircuitComponentCore.java | CREATE new abstract class | 30 min |
| 1.3 | CircuitComponent.java | MODIFY to extend Core | 45 min |
| 1.4 | Test compilation | Run `mvn compile` | 15 min |
| 1.5 | Fix cascading changes | Update 32 subclasses | 60 min |

**Phase 1 Total: 2.5-3 hours**

---

## Phase 2: Refactor Abstract Component Classes (2 hours)

### 2.1 Pattern to Apply

For each abstract component class, create Core + GUI split:

```
AbstractResistor (GUI-coupled)
        ↓
AbstractResistorCore (pure) + AbstractResistor (extends Core, adds GUI)
```

### 2.2 Classes to Refactor

| Original Class | Core Class | GUI Class | Dependent Files |
|----------------|------------|-----------|-----------------|
| AbstractResistor | AbstractResistorCore | AbstractResistor | 5 |
| AbstractInductor | AbstractInductorCore | AbstractInductor | 5 |
| AbstractCapacitor | AbstractCapacitorCore | AbstractCapacitor | 3 |
| AbstractCurrentSource | AbstractCurrentSourceCore | AbstractCurrentSource | 3 |
| AbstractVoltageSource | AbstractVoltageSourceCore | AbstractVoltageSource | 4 |
| AbstractSwitch | AbstractSwitchCore | AbstractSwitch | 4 |
| AbstractMotor | AbstractMotorCore | AbstractMotor | 3 |

### 2.3 Example: AbstractResistorCore

**File:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/AbstractResistorCore.java`

```java
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.BStampable;

/**
 * Core resistor simulation logic - NO GUI.
 * Extractable to gecko-simulation-core.
 */
public abstract class AbstractResistorCore extends CircuitComponentCore {
    
    protected double _resistance = 1.0;
    protected double _conductance = 1.0;
    
    protected AbstractResistorCore(SolverType solverType) {
        super(solverType);
    }
    
    public double getResistance() {
        return _resistance;
    }
    
    public void setResistance(double resistance) {
        this._resistance = resistance;
        this._conductance = 1.0 / resistance;
    }
    
    public double getConductance() {
        return _conductance;
    }
    
    @Override
    public void stampConductanceMatrix(BStampable gMatrix) {
        int i = matrixIndices[0];
        int j = matrixIndices[1];
        
        if (i >= 0) gMatrix.addToA(i, i, _conductance);
        if (j >= 0) gMatrix.addToA(j, j, _conductance);
        if (i >= 0 && j >= 0) {
            gMatrix.addToA(i, j, -_conductance);
            gMatrix.addToA(j, i, -_conductance);
        }
    }
    
    @Override
    public void stampSourceVector(BStampable bVector) {
        // Resistors don't contribute to source vector
    }
    
    @Override
    public void calculateCurrent(double dt) {
        _current = _voltage * _conductance;
    }
    
    @Override
    public void calculateVoltage(double pot1, double pot2) {
        _potOld1 = _potential1;
        _potOld2 = _potential2;
        _potential1 = pot1;
        _potential2 = pot2;
        _voltage = pot1 - pot2;
    }
}
```

### 2.4 Tasks Summary - Phase 2

| Task | Action | Time Est. |
|------|--------|-----------|
| 2.1 | Create AbstractResistorCore | 20 min |
| 2.2 | Create AbstractInductorCore | 25 min |
| 2.3 | Create AbstractCapacitorCore | 20 min |
| 2.4 | Create AbstractCurrentSourceCore | 20 min |
| 2.5 | Create AbstractVoltageSourceCore | 20 min |
| 2.6 | Create AbstractSwitchCore | 25 min |
| 2.7 | Update original classes to extend Core | 30 min |
| 2.8 | Test compilation | 20 min |

**Phase 2 Total: 2-2.5 hours**

---

## Phase 3: Refactor Type Info Classes (1.5 hours)

### 3.1 AbstractTypeInfo Separation

**Problem:** `AbstractTypeInfo` has `I18nKeys` dependency (UI internationalization).

**Solution:** Create `ITypeInfo` interface (already done!) and `TypeInfoCore`:

```java
// Already exists in gecko-simulation-core
public interface ITypeInfo {
    String getTypeName();
    String getTypeDescription();
    ComponentCategory getCategory();
}

// NEW: Core implementation without I18nKeys
public abstract class TypeInfoCore implements ITypeInfo {
    protected final String typeName;
    protected final String typeDescription;
    protected final ComponentCategory category;
    
    protected TypeInfoCore(String name, String desc, ComponentCategory cat) {
        this.typeName = name;
        this.typeDescription = desc;
        this.category = cat;
    }
    
    @Override
    public String getTypeName() { return typeName; }
    
    @Override
    public String getTypeDescription() { return typeDescription; }
    
    @Override
    public ComponentCategory getCategory() { return category; }
}
```

### 3.2 Classes to Refactor

| Original Class | Core Class | I18n Handling |
|----------------|------------|---------------|
| AbstractTypeInfo | TypeInfoCore | Move to GUI layer |
| CircuitTypeInfo | CircuitTypeInfoCore | Move to GUI layer |
| AbstractCircuitTypeInfo | AbstractCircuitTypeInfoCore | Move to GUI layer |

### 3.3 Tasks Summary - Phase 3

| Task | Action | Time Est. |
|------|--------|-----------|
| 3.1 | Create TypeInfoCore | 20 min |
| 3.2 | Create CircuitTypeInfoCore | 20 min |
| 3.3 | Refactor AbstractTypeInfo hierarchy | 30 min |
| 3.4 | Update dependent classes | 20 min |
| 3.5 | Test compilation | 15 min |

**Phase 3 Total: 1.5-2 hours**

---

## Phase 4: Extract All Priority 3 Classes (1 hour)

### 4.1 Mass Extraction

Once Core classes exist, bulk extraction becomes straightforward:

```bash
# Copy all Core classes to gecko-simulation-core
cp src/main/java/.../circuit/CircuitComponentCore.java \
   gecko-simulation-core/src/main/java/.../core/circuit/

cp src/main/java/.../circuit/AbstractResistorCore.java \
   gecko-simulation-core/src/main/java/.../core/circuit/

# ... repeat for all Core classes

# Update package names
find gecko-simulation-core/src -name "*Core.java" \
  -exec sed -i 's/package ch.technokrat.gecko.geckocircuits/package ch.technokrat.gecko.core/' {} \;

# Update imports
find gecko-simulation-core/src -name "*.java" \
  -exec sed -i 's/import ch.technokrat.gecko.geckocircuits/import ch.technokrat.gecko.core/' {} \;
```

### 4.2 Expected Extraction Count

| Category | Class Count | Status After Refactoring |
|----------|-------------|--------------------------|
| Core interfaces | 10 | ✅ Extractable |
| CircuitComponentCore | 1 | ✅ Extractable |
| Abstract*Core classes | 7 | ✅ Extractable |
| Calculator classes | 40 | ✅ Extractable |
| TypeInfoCore classes | 5 | ✅ Extractable |
| Utility classes | 30 | ✅ Extractable |
| Characteristic classes | 15 | ✅ Extractable |
| State/Enum classes | 20 | ✅ Extractable |
| **TOTAL** | **~130** | **All GUI-free** |

Plus existing 85 = **~215 total extracted classes**

### 4.3 Tasks Summary - Phase 4

| Task | Action | Time Est. |
|------|--------|-----------|
| 4.1 | Copy Core classes | 10 min |
| 4.2 | Update packages/imports | 15 min |
| 4.3 | Test compilation | 15 min |
| 4.4 | Fix any remaining issues | 20 min |

**Phase 4 Total: 1 hour**

---

## Implementation Order

### Session A (3 hours)

1. **Phase 1.1-1.4:** Create ICircuitCalculator, CircuitComponentCore, modify CircuitComponent
2. **Phase 1.5:** Fix cascading changes in subclasses
3. **Checkpoint:** Verify original codebase still compiles

### Session B (3 hours)

1. **Phase 2:** Create all Abstract*Core classes
2. **Phase 3:** Refactor TypeInfo hierarchy
3. **Checkpoint:** Verify original codebase still compiles

### Session C (2 hours)

1. **Phase 4:** Mass extraction to gecko-simulation-core
2. **Final verification:** All 215+ classes compile
3. **Documentation:** Update extraction status

---

## Risk Mitigation

### 1. Breaking Changes

**Risk:** Refactoring original classes may break existing functionality.

**Mitigation:**
- Create Core classes FIRST (additive)
- THEN modify originals to extend Core
- Run `mvn compile` after each step
- Run tests: `mvn test` (if available)

### 2. Circular Dependencies

**Risk:** Core classes might accidentally import GUI classes.

**Mitigation:**
- Strict rule: Core classes NEVER import from `java.awt`, `javax.swing`
- Verify with: `grep -r "import java.awt\|import javax.swing" */Core.java`
- Code review each Core class before committing

### 3. Regression

**Risk:** Simulation behavior might change after refactoring.

**Mitigation:**
- Core classes contain EXACT same logic as originals
- Only difference is inheritance hierarchy
- Create simple test circuits to verify before/after behavior matches

---

## Verification Checklist

### After Phase 1:

- [ ] `ICircuitCalculator.java` created with no GUI imports
- [ ] `CircuitComponentCore.java` created with no GUI imports
- [ ] `CircuitComponent.java` modified to extend Core
- [ ] `mvn compile` succeeds on original codebase
- [ ] All 32 CircuitComponent subclasses still work

### After Phase 2:

- [ ] All 7 Abstract*Core classes created
- [ ] All original Abstract* classes extend their Core
- [ ] `mvn compile` succeeds on original codebase
- [ ] `grep -r "import java.awt" *Core.java` returns nothing

### After Phase 3:

- [ ] TypeInfoCore hierarchy created
- [ ] Original TypeInfo classes extend Core
- [ ] I18nKeys dependencies moved to GUI layer only
- [ ] `mvn compile` succeeds

### After Phase 4:

- [ ] 130+ Core classes copied to gecko-simulation-core
- [ ] All packages/imports updated
- [ ] `mvn compile` succeeds on gecko-simulation-core
- [ ] Total extracted: 215+ classes
- [ ] Zero GUI imports in gecko-simulation-core

---

## File Structure After Refactoring

### Original Codebase (preserved, still works):

```
src/main/java/ch/technokrat/gecko/geckocircuits/
├── circuit/
│   ├── ICircuitCalculator.java        ← NEW (pure interface)
│   ├── CircuitComponentCore.java      ← NEW (pure abstract)
│   ├── AbstractResistorCore.java      ← NEW (pure abstract)
│   ├── AbstractInductorCore.java      ← NEW (pure abstract)
│   ├── ... (other Core classes)
│   ├── TypeInfoCore.java              ← NEW (pure abstract)
│   └── circuitcomponents/
│       ├── CircuitComponent.java      ← MODIFIED (extends Core)
│       ├── AbstractResistor.java      ← MODIFIED (extends Core)
│       └── ... (all still work with GUI)
```

### Extracted Module (gecko-simulation-core):

```
gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/
├── allg/
│   ├── SolverType.java
│   └── ... (utilities)
├── circuit/
│   ├── ICircuitCalculator.java
│   ├── CircuitComponentCore.java
│   ├── AbstractResistorCore.java
│   ├── AbstractInductorCore.java
│   ├── AbstractCapacitorCore.java
│   ├── AbstractCurrentSourceCore.java
│   ├── AbstractVoltageSourceCore.java
│   ├── AbstractSwitchCore.java
│   ├── TypeInfoCore.java
│   ├── ResistorCalculator.java        ← Now extends AbstractResistorCore
│   ├── InductorCalculator.java        ← Now extends AbstractInductorCore
│   └── ... (130+ calculator classes)
├── control/
│   └── ... (77 already extracted)
└── math/
    └── ... (utilities)
```

---

## Commands Reference

### Phase 1 Commands:

```bash
# Create new files
touch src/main/java/ch/technokrat/gecko/geckocircuits/circuit/ICircuitCalculator.java
touch src/main/java/ch/technokrat/gecko/geckocircuits/circuit/CircuitComponentCore.java

# Verify no GUI imports in Core files
grep -r "import java.awt\|import javax.swing" src/main/java/ch/technokrat/gecko/geckocircuits/circuit/*Core.java

# Test compilation
mvn compile -q

# Count affected files
grep -l "extends CircuitComponent" src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/*.java | wc -l
```

### Phase 4 Commands:

```bash
# Copy Core classes
cp src/main/java/ch/technokrat/gecko/geckocircuits/circuit/*Core.java \
   gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/

# Update packages
find gecko-simulation-core/src -name "*.java" \
  -exec sed -i 's/package ch\.technokrat\.gecko\.geckocircuits/package ch.technokrat.gecko.core/' {} \;

# Update imports
find gecko-simulation-core/src -name "*.java" \
  -exec sed -i 's/import ch\.technokrat\.gecko\.geckocircuits/import ch.technokrat.gecko.core/' {} \;

# Verify
cd gecko-simulation-core && mvn compile -q && echo "✅ Success"
```

---

## Summary

**Option D provides:**
- ✅ Architecturally correct separation of concerns
- ✅ 215+ GUI-free classes (vs. current 85)
- ✅ Clean modular codebase
- ✅ Foundation for headless/server deployment
- ✅ Improved maintainability long-term

**Effort:** 6-8 hours across 2-3 sessions  
**Risk:** Medium (careful incremental approach)  
**Value:** High (permanent architectural improvement)

---

## Next Step

**Ready to begin Phase 1?**

I can start by creating `ICircuitCalculator.java` and `CircuitComponentCore.java` right now.
