# Content Strategy Pivot: From Business to Education

**Date:** January 26, 2026
**Decision:** Pivot from refactoring/modernization case studies to technical education

---

## What Changed

### Original Strategy (Abandoned)
**Focus:** Business-oriented content about the refactoring journey
- REST API modernization
- Test coverage improvements
- Strategic pivots and architecture decisions
- Consulting/business development focus

**Target Audience:** Engineers with legacy code problems, decision-makers
**Business Goal:** Generate consulting leads, subscriptions, hiring

### New Strategy (Current)
**Focus:** Educational content about circuit simulation internals
- Modified Nodal Analysis (MNA)
- Matrix stamping techniques
- Solver algorithms (Cholesky, Euler, etc.)
- Component models and implementations
- IPES file format
- Signal processing (FFT, THD)
- Actual code walkthroughs from `/dev` branch

**Target Audience:** Power electronics engineers/students learning circuit simulation
**Educational Goal:** Teach how GeckoCIRCUITS (and SPICE) actually works

---

## Why the Pivot?

User feedback: "I don't like the series we shall focus on the GECKO desktop version of the branch /dev and dissect each part of code, explain what is inside to have a 'learning by doing process' to give some examples: MNA, cholesky, different solver, ipes format, etc..."

This makes much more sense because:
1. **More valuable:** Teaching circuit simulation is unique content
2. **Better fit:** Aligns with open-source educational mission
3. **Stronger differentiation:** Not many sources explain SPICE internals with actual code
4. **Larger audience:** Students + practicing engineers + researchers
5. **Long-term impact:** Educational content has longer shelf life

---

## What Stays the Same

The **infrastructure** remains identical:
- ✅ 3 skill files (content-strategist, linkedin-writer, substack-article-writer)
- ✅ 2 Python tools (metrics_extractor, template_renderer)
- ✅ SEO keyword cache (needs updating for new topics)
- ✅ Directory structure
- ✅ Publishing cadence (weekly LinkedIn, bi-weekly Substack)
- ✅ 15 weeks / 15 posts / 7 articles

Only the **content topics** changed.

---

## New Content Calendar Highlights

### Foundation (Weeks 1-5)
- Week 1: What is Modified Nodal Analysis?
- Week 2: Matrix Stamping - Resistor example
- Week 3: Time-dependent stamping (Capacitors, Inductors)
- Week 4: Cholesky decomposition
- Week 5: Sparse matrices

### Intermediate (Weeks 6-10)
- Week 6: Three solvers (BE, TRZ, GS)
- Week 7: The simulation loop
- Week 8: Nonlinear components (Newton-Raphson)
- Week 9: Event-driven simulation (switches)
- Week 10: Component registry pattern

### Advanced (Weeks 11-15)
- Week 11: IPES file format
- Week 12: Signal processing (FFT, THD, CISPR16)
- Week 13: Control system integration
- Week 14: Adaptive time stepping
- Week 15: Series summary + next topics poll

---

## Updated SEO Keywords

**Old Keywords (Abandoned):**
- legacy code refactoring
- Spring Boot integration
- REST API development
- technical debt management
- incremental refactoring

**New Keywords (Active):**
- Modified Nodal Analysis
- circuit simulation
- SPICE simulation
- Cholesky decomposition
- Newton-Raphson method
- event-driven simulation
- matrix stamping
- power electronics
- numerical methods
- FFT THD harmonics

---

## Source Files Identified

The content now draws from actual GeckoCIRCUITS Java source code:

### MNA & Stamping
- `IMatrixStamper.java` - Interface with excellent documentation
- `ResistorStamper.java`, `CapacitorStamper.java`, `InductorStamper.java`
- `DiodeStamper.java` (nonlinear example)
- `StamperRegistry.java`

### Matrix Solving
- `CholeskyDecomposition.java`
- `SymmetricSparseMatrix.java`
- `Matrix.java`, `GeckoMatrix.java`

### Simulation
- `SimulationsKern.java` (main loop)
- `SolverType.java` (BE, TRZ, GS)
- `SolverSettings.java`, `SolverContext.java`

### Signal Processing
- `Cispr16Fft.java`
- THD/harmonic calculation code

### File Format
- `IpesFileable.java`
- `GeckoFileable.java`

### Control System
- `ReglerIntegrator.java`
- Control calculator classes

---

## Content Creation Workflow (Updated)

### For LinkedIn Posts:
1. **Identify Java source file** for the week (e.g., `ResistorStamper.java`)
2. **Read the implementation** - understand the algorithm
3. **Extract code snippet** (5-15 lines of actual Java)
4. **Explain the concept:**
   - What problem does it solve?
   - How does the code work?
   - Why this approach?
5. **Use linkedin-writer skill** to format as 400-500 word post
6. **Include:**
   - Hook (simple explanation)
   - Code walkthrough (with line numbers from file)
   - Application to real circuits
   - CTA for next week

### For Substack Articles:
1. **Gather multiple code files** for the topic
2. **Create worked example:**
   - Simple circuit (draw diagram)
   - Math formulation (equations)
   - Code implementation (actual GeckoCIRCUITS)
   - Results (numbers, plots if available)
3. **Use substack-article-writer skill** for 2,500-3,000 words
4. **Include:**
   - Theory section (MNA, integration, etc.)
   - Implementation walkthrough (multiple files)
   - Complete example (step-by-step)
   - Exercises for readers

---

## Example: Week 2 LinkedIn Post

**Topic:** Matrix Stamping - How a Resistor Writes to the A Matrix

**Source File:** `ResistorStamper.java`

**Code to Show:**
```java
// From ResistorStamper.java
public void stampMatrixA(double[][] a, int nodeX, int nodeY, ...) {
    double conductance = 1.0 / resistance;
    a[nodeX][nodeX] += conductance;
    a[nodeY][nodeY] += conductance;
    a[nodeX][nodeY] -= conductance;
    a[nodeY][nodeX] -= conductance;
}
```

**Explanation:**
- Resistor R between nodes x and y
- Conductance G = 1/R
- Symmetric 4-entry stamp pattern
- Represents Ohm's law in conductance form
- Why it works: KCL at each node

**Hook:** "Matrix 'stamping' is the coolest name for adding numbers to a matrix. Here's how a resistor does it in GeckoCIRCUITS."

---

## Success Metrics (Updated)

**Educational Impact:**
- Help engineers understand how SPICE/GeckoCIRCUITS works
- Enable developers to extend GeckoCIRCUITS with custom components
- Support students learning power electronics simulation
- Build open-source community

**Engagement Metrics:**
- LinkedIn: Technical discussions, questions about implementation
- Substack: Requests for specific topics, code questions
- GitHub: Increased contributions to GeckoCIRCUITS repo
- Academic: Citations in papers/theses

**Not Measuring:**
- Consulting leads (not the goal anymore)
- Business development
- Revenue targets

---

## Tools Update Needed

### SEO Keywords JSON
**Status:** Needs update
**Action:** Replace business keywords with technical/educational keywords

**Before:**
```json
"primary_keywords": [
  "legacy code refactoring",
  "REST API development",
  ...
]
```

**After:**
```json
"primary_keywords": [
  "Modified Nodal Analysis",
  "circuit simulation",
  "SPICE simulation",
  "Cholesky decomposition",
  ...
]
```

### LinkedIn Hashtags
**Before:** `#LegacyCode #Refactoring #TechDebt #ModernizationStrategy`
**After:** `#CircuitSimulation #MNA #SPICE #PowerElectronics #NumericalMethods`

### Metrics Extractor
**Status:** Still useful
**Use Case:** Extract statistics from code files instead of documentation
```bash
# Count lines in stamper files
find src -name "*Stamper.java" -exec wc -l {} +

# Get git stats for simulation package
git log --oneline src/main/java/ch/technokrat/gecko/geckocircuits/circuit/
```

---

## Next Steps

1. ✅ **Content calendar updated** - New topics for all 15 weeks
2. ⚠️ **Update SEO keywords** - Replace business with technical keywords
3. 📝 **Start Week 1 content** - LinkedIn Post #1 about MNA
4. 📝 **Prepare Article #1** - MNA deep dive with code examples
5. 📖 **Read source files** - Study implementations before writing

---

## Key Takeaway

This pivot makes the content strategy **much stronger**:
- ✅ Unique value (not many people explain SPICE internals)
- ✅ Educational mission (aligns with open source)
- ✅ Larger audience (students + engineers + researchers)
- ✅ Better differentiation (actual code, not just theory)
- ✅ Longer shelf life (fundamentals don't change)

The infrastructure stays the same. Only content topics changed. We're ready to execute.

---

**Status:** Pivot complete, ready to create educational content
**First Action:** Write LinkedIn Post #1 about Modified Nodal Analysis
**Timeline:** 15 weeks starting now
