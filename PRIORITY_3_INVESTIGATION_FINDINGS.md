# Priority 3 Investigation Findings - Circuitcomponents Base Classes

**Date:** January 27, 2026  
**Status:** Investigation Complete - Strategy Revision Needed  
**Commit:** d60834e (Last verified working state)

## Executive Summary

Investigation of Priority 3 (Extract circuitcomponents base classes) revealed significant interdependencies that prevent wholesale extraction of GUI-free component classes. While 64 GUI-free classes exist in `circuitcomponents/`, attempting to extract them en masse creates unresolvable circular dependencies. A more selective, dependency-aware approach is required.

## Current Progress (Session 2)

### Completed
- **Priority 1-2:** ✓ COMPLETE
  - GridPoint.java (enum) - No dependencies
  - ComponentState.java (enum) - No dependencies  
  - NameAlreadyExistsException.java (exception) - No dependencies
  - ControlTerminable.java (interface) - Pure, no external deps
  - PostCalculatable.java (interface) - Pure, no external deps
  
- **Total Extracted:** 85 classes (77 from Session 1 + 5 new + 3 math utilities)
- **Compilation Status:** ✓ VERIFIED
- **Git Commit:** `d60834e` - "Priority 1-2: Extract 5 core value types and pure interfaces"

### Investigated (Not Extracted)
- 64 GUI-free circuitcomponent classes identified
- Initial extraction attempt: 64 classes → 54 after removing GUI-coupled classes
- Final result: Massive compilation failures due to interdependencies

## Key Findings - Why Priority 3 is Blocked

### Dependency Analysis

**Critical Blocking Dependencies:**

1. **AbstractComponentTyp** (NOT GUI-free)
   - Required by: CircuitTyp, SpecialTyp
   - Status: Has GUI imports (java.awt, javax.swing)
   - Impact: Cannot extract CircuitTyp without AbstractComponentTyp

2. **AbstractTypeInfo** (NOT GUI-free)
   - Required by: CircuitTyp, TerminalCircuit, ThermalTypeInfo, ReluctanceTypeInfo, ReluctanceAndCircuitTypeInfo
   - Status: Has GUI imports
   - Impact: Blocks 15+ classes that depend on AbstractTypeInfo

3. **AbstractCircuitTerminal, AbstractCircuitGlobalTerminal** (NOT GUI-free)
   - Required by: TerminalCircuit, RelTerminal, ReluctanceGlobalTerminal
   - Status: Have GUI imports
   - Impact: Terminal-related classes blocked

4. **AbstractCurrentSource, AbstractInductor, AbstractResistor, AbstractSwitch** (NOT GUI-free)
   - Required by: 30+ calculator and circuit implementation classes
   - Status: All have GUI imports
   - Impact: Calculator classes cannot function in isolation

5. **CircuitComponent** (Base class dependency)
   - Required by: All concrete circuit component implementations
   - Status: Has multiple GUI imports
   - Impact: Most circuitcomponents depend on this

6. **I18nKeys, ConnectorType** (Localization/UI)
   - Required by: Type info classes
   - Status: Not yet extracted
   - Impact: Blocks type hierarchy extraction

### Attempted Extractions

| Class | Result | Reason |
|-------|--------|--------|
| AStampable | Failed | Requires BVector |
| BStampable | Failed | Interdependent with A |
| IComponentCalculator | Failed | Requires SolverType, CurrentCalculatable |
| CircuitTyp | Failed | Requires AbstractComponentTyp, AbstractTypeInfo |
| TerminalCircuit | Failed | Requires AbstractCircuitTerminal, AbstractTypeInfo |
| ThermalTypeInfo | Failed | Requires AbstractTypeInfo, AbstractBlockInterface, I18nKeys |
| ResistorCalculator | Failed | Requires CircuitComponent, AbstractResistor |
| DiodeCalculator | Failed | Requires CircuitComponent, Diode |
| InductorCalculator | Failed | Requires AbstractInductor, SolverType |
| CurrentSourceCalculator | Failed | Requires AbstractCurrentSource, TimeFunction |

### Key Classes with 1-2 GUI Imports

```java
AbstractCircuitBlockInterface.java - 1 GUI import
AbstractTwoPortLKreisBlock.java - 1 GUI import
AbstractVoltageSource.java - 1 GUI import
AbstractResistor.java - 2 GUI imports
AbstractSwitch.java - GUI import
AbstractSemiconductor.java - GUI import
AbstractMotorIMCommon.java - GUI import
```

These are the fundamental abstractions that MUST be refactored before any dependent classes can be extracted.

## Strategy Revision: Three Options

### Option A: Refactor and Extract (Recommended)
**Effort:** 3-4 hours  
**Steps:**
1. Extract base abstractions WITHOUT GUI imports using refactoring
   - Create `core.circuit.abstracts.*` package
   - Extract interfaces: `IComponentCalculator`, `IStampable`, `ITerminal`
   - Create pure abstract classes without GUI coupling
2. Extract circuitcomponents that depend only on refactored abstractions
3. **Benefit:** Full isolation of 64+ classes, proper architecture

### Option B: Stub Extraction (Faster)
**Effort:** 1-2 hours  
**Steps:**
1. Create stub versions of GUI-dependent classes in core.circuit.stubs
   - AbstractComponentTyp (stub)
   - AbstractTypeInfo (stub)
   - CircuitComponent (stub)
2. Extract 64 classes depending only on stubs
3. **Benefit:** Faster extraction, maintains original dependencies visible
4. **Risk:** Stubs may diverge from originals during maintenance

### Option C: Skip to Priority 4
**Effort:** Immediate  
**Steps:**
1. Defer Priority 3 (circuitcomponents base classes) to Phase 2
2. Proceed to Priority 4: Complete remaining sprints (E1, E5-E8)
3. Come back to Priority 3 after understanding full dependency graph
4. **Benefit:** Unblock other extractions (loss calculation, circuit main)
5. **Risk:** Priority 3 integration harder later without context

## Recommendation

**Proceed with Option A (Refactor and Extract)** because:
1. Creates clean, separation-of-concerns architecture
2. GUI-free abstractions enable true GUI independence
3. Better long-term maintainability
4. Establishes patterns for future modularization
5. Only 3-4 additional hours for complete Priority 3

## Next Steps (If Continuing Priority 3)

1. **Identify pure abstractions:**
   ```
   IComponentCalculator
   IStampable (AStampable/BStampable)
   ITerminalInterface
   ITypeInfo
   ```

2. **Create core abstractions without GUI:**
   ```java
   // New files in gecko-simulation-core/core/circuit/abstracts/
   - IComponentCalculator.java (already extracted)
   - IStampable.java (interface version of AStampable)
   - ITerminal.java (pure interface)
   - ITypeInfo.java (pure interface)
   ```

3. **Refactor circuitcomponents to depend on interfaces, not implementations**

4. **Then extract all 64 GUI-free classes**

## Files for Future Extraction

**Tier 1 (No external deps, extract now if continuing):**
- GridPoint.java ✓
- ComponentState.java ✓
- NameAlreadyExistsException.java ✓
- ControlTerminable.java ✓
- PostCalculatable.java ✓

**Tier 2 (Requires refactored abstractions):**
- AStampable.java (→ rename to IStampable)
- BStampable.java
- BVector.java
- DiodeSegment.java
- SwitchState.java (removed earlier, had issues)
- *57 others depending on abstract base classes*

**Tier 3 (Requires parent class extraction first):**
- Calculators (ResistorCalculator, CapacitorCalculator, etc.) - 30+
- Circuits (ResistorCircuit, InductorWOCoupling, etc.) - 15+
- Type infos (ThermalTypeInfo, ReluctanceTypeInfo, etc.) - 10+
- Source implementations (VoltageSourceElectric, CurrentSourceCircuit, etc.) - 9+

## Lessons Learned

1. **GUI coupling is deep:** Even "calculation" classes reference GUI types for rendering/display
2. **Interface-first design needed:** The original code mixed concerns (computation + UI)
3. **Batch extraction has limits:** Can't just copy 64 classes; must do dependency-aware extraction
4. **Need refactoring tools:** Original architects didn't anticipate GUI-free extraction
5. **Priority reordering justified:** Doing Priority 4 first may be more efficient

## Decision Required

- **Continue Priority 3 (refactor first)?** → Proceed with Option A
- **Skip to Priority 4?** → Document findings, revisit after understanding full graph
- **Hybrid approach?** → Do Tier 1 (5 classes) now, come back to Tiers 2-3 later

**Estimated completion if continuing:** 
- Refactor abstractions: 3-4 hours
- Extract 64 classes: 1-2 hours
- Total Priority 3: 4-6 hours additional

**Current session status:**
- Time elapsed: ~2 hours (investigation)
- Classes extracted: 5 new + 80 carried forward = 85 total
- Compilation: ✓ VERIFIED
- Next decision point: Choose continuation strategy
