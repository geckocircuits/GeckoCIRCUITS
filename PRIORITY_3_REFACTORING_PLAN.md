# Priority 3: Abstraction Refactoring & Extraction Plan

**Status:** IN PROGRESS  
**Approach:** Option A - Refactor Abstractions First  
**Estimated Time:** 3-4 hours for refactoring + 1-2 hours for extraction = 4-6 hours total  
**Target:** Extract all 64 GUI-free circuitcomponents classes after refactoring

## Phase 1: Identify GUI-Free Abstractions (Already Done)

### Tier 1: Pure Interfaces (No GUI, Already Extracted)
- ✓ IComponentCalculator.java (interface signature needed)
- ✓ ControlTerminable.java
- ✓ PostCalculatable.java
- ✓ DirectCurrentCalculatable.java
- ✓ HistoryUpdatable.java
- ✓ PostProcessable.java
- ✓ AStampable.java (interface)
- ✓ BStampable.java (interface)

### Tier 2: Base Classes Requiring Refactoring (GUI-Coupled)

**Critical Base Classes (Must Extract Without GUI Imports)**
1. **AbstractComponentTyp** (in allg/) - Currently has GUI imports
   - Solution: Create `ch.technokrat.gecko.core.allg.AbstractComponentTyp` without GUI
   - Dependencies to handle: UserParameter, TextFieldBlock (GUI)
   - Strategy: Extract method signatures, defer UI rendering

2. **AbstractTypeInfo** (in circuit/) - Currently has GUI imports
   - Solution: Create pure `ch.technokrat.gecko.core.circuit.AbstractTypeInfo` 
   - Dependencies to handle: GUI rendering, I18nKeys
   - Strategy: Create interface `ITypeInfo`, implement pure version

3. **AbstractCircuitTerminal** (in circuit/) - Currently has GUI imports
   - Solution: Extract terminal interface separately
   - Dependencies to handle: GUI canvas rendering
   - Strategy: Pure interface + implementation

4. **AbstractCurrentSource, AbstractVoltageSource** (in circuit/) - GUI-coupled
   - Solution: Extract base interfaces first
   - Dependencies to handle: GUI-coupled calc classes
   - Strategy: Interface definition without implementation details

5. **AbstractResistor, AbstractInductor, AbstractCapacitor** (in circuit/) - GUI-coupled
   - Solution: Extract pure base classes
   - Dependencies to handle: Type info, calculation methods
   - Strategy: Pure interfaces for core methods

## Phase 2: Refactoring Strategy

### Step 1: Create Pure Interface Versions (No GUI, No Rendering)

**Location:** `gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/abstracts/`

```java
// 1. ITypeInfo.java - Pure interface for type information
public interface ITypeInfo {
    String getTypeString();
    String getTypeID();
    // NO GUI rendering methods
}

// 2. ITerminal.java - Pure interface for terminals
public interface ITerminal {
    void addCurrent(double current);
    double getVoltage();
    // NO canvas/rendering methods
}

// 3. ISourceCalculator.java - Pure calculator interface
public interface ISourceCalculator extends IComponentCalculator {
    void calculateNextStep(double timeStep);
    // NO GUI update methods
}

// 4. IPassiveCalculator.java - Pure passive component interface
public interface IPassiveCalculator extends IComponentCalculator {
    void calculateNextStep(double timeStep);
    // NO GUI coupling
}
```

### Step 2: Extract Minimal Base Classes (No GUI Coupling)

**Location:** Same `gecko-simulation-core/core/circuit/abstracts/`

```java
// 1. AbstractTypeInfo - Pure implementation
public abstract class AbstractTypeInfo implements ITypeInfo {
    protected String typeID;
    protected String typeString;
    
    @Override
    public String getTypeString() { return typeString; }
    
    @Override
    public String getTypeID() { return typeID; }
    // Only non-GUI methods
}

// 2. AbstractTerminal - Pure implementation
public abstract class AbstractTerminal implements ITerminal {
    protected double voltage;
    protected List<Double> currents;
    // Pure calculation, NO GUI
}

// 3. AbstractSourceCalculator - Pure calculation
public abstract class AbstractSourceCalculator implements ISourceCalculator {
    protected TimeFunction timeFunction;
    protected double currentValue;
    
    public abstract void calculateNextStep(double timeStep);
}
```

### Step 3: Update Circuitcomponents to Depend on Pure Abstractions

**Files to Refactor (First 10):**
1. CircuitComponent.java - Change from GUI-coupled to pure interface
2. Diode.java - Depend on IPassiveCalculator instead of GUI class
3. ResistorCalculator.java - Depend on IComponentCalculator
4. CapacitorCalculator.java - Depend on IComponentCalculator
5. InductorCalculator.java - Depend on IComponentCalculator
6. CurrentSourceCalculator.java - Depend on ISourceCalculator
7. VoltageSourceCalculator.java - Depend on ISourceCalculator
8. TerminalCircuit.java - Depend on ITerminal instead of GUI
9. ThermalTypeInfo.java - Depend on AbstractTypeInfo (pure)
10. ReluctanceTypeInfo.java - Depend on AbstractTypeInfo (pure)

### Step 4: Extract Refactored Classes to Core

**Process:**
1. Copy pure abstraction classes to core
2. Copy updated calculator implementations
3. Update all package names and imports
4. Verify compilation

## Phase 3: Bulk Extraction (After Refactoring)

Once abstractions are in place, extract remaining 50+ classes:

**Category 1: Calculators (30 classes)**
- ResistorCalculator ✓
- CapacitorCalculator ✓
- InductorCalculator ✓
- DiodeCalculator ✓
- IGBTCalculator ✓
- IdealSwitchCalculator ✓
- ThyristorCalculator ✓
- *23 more source/component calculators*

**Category 2: Circuits (15 classes)**
- ResistorCircuit
- CapacitorCircuit
- InductorWOCoupling
- CurrentSourceCircuit
- VoltageSourceElectric
- *10 more circuit implementations*

**Category 3: Type Infos (10 classes)**
- ThermalTypeInfo
- ReluctanceTypeInfo
- SpecialTypeInfo
- CircuitTyp
- *6 more type info classes*

**Category 4: Support Classes (9 classes)**
- DiodeCharacteristic
- DiodeSegment
- SourceType
- TextInfoType
- SwitchState
- *4 more support classes*

## Implementation Timeline

**Hour 1 (Phase 2.1-2.2): Create Pure Interfaces & Base Classes**
- Create `abstracts/` subdirectory
- Implement ITypeInfo, ITerminal, ISourceCalculator, IPassiveCalculator
- Implement AbstractTypeInfo, AbstractTerminal, AbstractSourceCalculator
- Test compilation (should be clean)

**Hour 2 (Phase 2.3): Refactor Key Circuitcomponents**
- Extract and refactor top 10 circuitcomponents
- Update dependencies to use pure abstractions
- Test compilation in gecko-simulation-core

**Hour 3 (Phase 3.1-3.2): Bulk Extract Category 1 & 2**
- Copy all 30 calculators
- Copy all 15 circuits
- Update package names and imports
- Test compilation

**Hour 4 (Phase 3.3-3.4): Bulk Extract Category 3 & 4**
- Copy all 10 type infos
- Copy all 9 support classes
- Final compilation verification
- Git commit with all 64+ classes

## Success Criteria

- ✓ All 64+ GUI-free circuitcomponents extracted to core
- ✓ Zero unresolved dependencies
- ✓ Maven compilation successful
- ✓ All extracted code has zero GUI imports
- ✓ Proper abstraction hierarchy in place
- ✓ Clean git history with clear commits

## Risk Mitigation

**Risk:** Refactored abstractions diverge from originals during refactoring
**Mitigation:** Keep original files visible, side-by-side refactoring with validation

**Risk:** Missed GUI-coupled dependencies
**Mitigation:** Grep all files after refactoring for java.awt, javax.swing, SwingUtilities

**Risk:** Compilation failures mid-extraction
**Mitigation:** Extract in batches (10-15 at a time), verify after each batch

## Commands to Execute

```bash
# Phase 2: Create abstractions
mkdir -p gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/abstracts/

# Copy and refactor from original sources
grep -l "public interface\|public abstract class" src/main/java/ch/technokrat/gecko/geckocircuits/circuit/*.java | \
  head -10 | xargs -I {} cp {} gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/

# Phase 3: Bulk extract remaining classes
grep -L "import java.awt\|import javax.swing" src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/*.java | \
  xargs -I {} cp {} gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/

# Update all package declarations
find gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit -name "*.java" \
  -exec sed -i 's/package ch\.technokrat\.gecko\.geckocircuits/package ch.technokrat.gecko.core/g' {} \;

# Verify compilation
cd gecko-simulation-core && mvn compile -q
```

## Current State

**Extracted (8 files):**
- GridPoint.java
- ComponentState.java
- NameAlreadyExistsException.java
- ControlTerminable.java
- PostCalculatable.java
- DirectCurrentCalculatable.java
- HistoryUpdatable.java
- PostProcessable.java

**Next: Begin Phase 2.1**
