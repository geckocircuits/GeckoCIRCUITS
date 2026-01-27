# GUI-Free Extraction: Quick Reference & Next Steps

**Last Updated:** January 27, 2026  
**Progress:** 77 classes extracted / 175+ target = **44% complete**

---

## Current Status at a Glance

✓ **COMPLETE & COMPILING:**
- Sprint E2: Math module (7 classes)
- Sprint E3-E4: Control calculators (64 classes)
- Total: **77 GUI-free classes** in `gecko-simulation-core`

⊘ **DEFERRED (Complex Dependencies):**
- Sprint E1: Circuit stampers & netlist (21 classes)
- Sprint E5: Loss calculation (18 classes)  
- Sprint E6-E7: Circuit main components (54 classes)
- Sprint E8: API package (4 classes)

---

## Immediate Next Actions

### Priority 1: Extract Core Dependency Classes (2-3 hours)

These are **required** by multiple deferred sprints:

**Location:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/`

**Classes to Extract:**
1. `GridPoint.java` - Simple coordinate class (no deps)
2. `ComponentDirection.java` - Enum (no deps)
3. `ComponentState.java` - Enum (no deps)  
4. `SpecialTyp.java` - Enum (no deps)
5. `NameAlreadyExistsException.java` - Exception (no deps)

**Command Template:**
```bash
cd /home/tinix/claude_wsl/GeckoCIRCUITS
mkdir -p gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit

# Copy files
cp src/main/java/ch/technokrat/gecko/geckocircuits/circuit/{GridPoint,ComponentDirection,ComponentState,SpecialTyp,NameAlreadyExistsException}.java \
   gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit/

# Update packages (all in circuit main package)
find gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/circuit -maxdepth 1 -name "*.java" \
  -exec sed -i 's/package ch\.technokrat\.gecko\.geckocircuits\.circuit;/package ch.technokrat.gecko.core.circuit;/g' {} \;

# Test compilation
cd gecko-simulation-core && mvn compile -q
```

### Priority 2: Extract Interfaces (2 hours)

**Classes to Extract:**
- `ComponentCoupable.java`
- `ComponentTerminable.java`
- `ControlTerminable.java`
- `CurrentMeasurable.java`
- `DirectVoltageMeasurable.java`
- `GlobalTerminable.java`
- `PostCalculatable.java`

Same extraction procedure as Priority 1.

### Priority 3: Extract circuitcomponents Base Classes (2-3 hours)

**Location:** `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/`

**Classes to Check/Extract:**
- `AbstractComponentTyp.java` (check: depends on allg)
- `AbstractCircuitBlockInterface.java` (check: may have GUI deps)
- `AbstractCapacitor.java`
- `AbstractInductor.java`
- `AbstractSemiconductor.java`

**IMPORTANT:** Run GUI check first:
```bash
grep -l "import java.awt\|import javax.swing" \
  src/main/java/ch/technokrat/gecko/geckocircuits/circuit/circuitcomponents/{Abstract*,Resistor*}.java
```

Only extract those without GUI imports.

---

## Extraction Procedure Template

Use this for each new batch:

### Step 1: Verify GUI-Free Status
```bash
grep -l "import java.awt\|import javax.swing" src/main/java/ch/technokrat/gecko/geckocircuits/PACKAGE/*.java
# Should return empty (exit code 1)
```

### Step 2: Create Target Directory
```bash
mkdir -p gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/PACKAGE/SUBPACKAGE
```

### Step 3: Copy Files
```bash
cp src/main/java/ch/technokrat/gecko/geckocircuits/PACKAGE/SUBPACKAGE/*.java \
   gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/PACKAGE/SUBPACKAGE/
```

### Step 4: Update Package Declarations
```bash
find gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/PACKAGE -name "*.java" \
  -exec sed -i 's/package ch\.technokrat\.gecko\.geckocircuits\.PACKAGE\(.*\);/package ch.technokrat.gecko.core.PACKAGE\1;/g' {} \;
```

### Step 5: Update Imports
```bash
find gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/PACKAGE -name "*.java" \
  -exec sed -i 's/import ch\.technokrat\.gecko\.geckocircuits\./import ch.technokrat.gecko.core./g' {} \;
```

### Step 6: Compile & Verify
```bash
cd gecko-simulation-core
mvn compile -q 2>&1 | head -100
# If errors: remove problematic files and retry
# If success: proceed to commit
```

### Step 7: Commit
```bash
cd ..
git add -A
git commit -m "Sprint EX: Extract NN classes from PACKAGE

Extracted: (list classes)
Dependencies: (any additional classes needed)
Status: Compilation verified"
```

---

## Known Issues & Workarounds

### Issue: "package ch.technokrat.gecko.core.XXXX does not exist"

**Cause:** Extracted class depends on classes not yet extracted

**Solution:**
1. Check the import statement to find missing class
2. Extract that class first
3. Verify the dependency class is in the target package
4. Re-run compilation

**Example:**
```
[ERROR] package ch.technokrat.gecko.core.circuit.circuitcomponents does not exist
→ Need to extract circuitcomponents classes first
→ OR find them in a different package path
```

### Issue: "cannot find symbol class XXX"

**Cause:** Referenced class exists but not in expected package

**Solution:**
1. Find actual location: `grep -r "class XXX" src/main/java/`
2. If it's in `geckocircuits.DIFFERENT_PACKAGE`, may need to extract that too
3. If it's a GUI class, may need to create an interface

### Issue: Sed command doesn't match (no replacements made)

**Cause:** Package name pattern doesn't match exactly

**Solution:**
1. Check actual package statement: `head -20 file.java | grep package`
2. Adjust regex to match exactly
3. Test with: `grep -n "package" gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/PACKAGE/*.java`

---

## Useful Diagnostic Commands

### Count extracted classes by package
```bash
find gecko-simulation-core/src/main/java/ch/technokrat/gecko/core -name "*.java" -type f | \
  cut -d'/' -f12 | sort | uniq -c | sort -rn
```

### Find all external dependencies (potential issues)
```bash
grep -h "^import ch.technokrat.gecko" gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/**/*.java | \
  sed 's/import //;s/\..*//' | sort | uniq
```

### Check for lingering GUI imports (should be empty)
```bash
grep -r "import java.awt\|import javax.swing" gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/
# Should return nothing
```

### Find classes that reference a specific dependency
```bash
grep -r "NotCalculateableMarker\|ReglerDemux\|IsDtChangeSensitive" \
  src/main/java/ch/technokrat/gecko/geckocircuits/control/calculators/*.java | cut -d':' -f1 | sort | uniq
```

---

## Sprint Execution Order (Recommended)

```
CURRENT: E2 ✓, E3-E4 ✓ (77 classes)
NEXT:    Core enums/exceptions/interfaces (Priority 1-2, 4-5 hours)
THEN:    E1a (circuit.matrix, 15 classes, 3 hours)
THEN:    E1b (circuit.netlist, 4 classes, 2 hours)
THEN:    E6-E7 (circuit main, 54 classes, 8+ hours)
THEN:    E5 (loss calculation, 18 classes, 2 hours)
FINALLY: E8 (API, 4 classes, 1 hour)
```

**Total Estimated Remaining:** 20-25 hours

---

## Git Workflow

### Before Starting
```bash
git status                    # Should be clean
git pull                      # Get latest changes
```

### During Work
```bash
# Test frequently
cd gecko-simulation-core && mvn compile -q

# Commit by sprint/category
git add -A
git commit -m "Sprint EX description"
```

### After Completion
```bash
# Quick summary
find gecko-simulation-core/src/main/java/ch/technokrat/gecko/core -name "*.java" | wc -l
# Run tests if available
mvn test
```

---

## Reference: Completed Extraction Details

### E2: Math (7 classes)
- Location: `gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/math/`
- Status: ✓ Complete
- Dependencies: TechFormat (core/allg)

### E3-E4: Control Calculators (64 classes)
- Location: `gecko-simulation-core/src/main/java/ch/technokrat/gecko/core/control/calculators/`
- Status: ✓ Complete
- Dependencies: AbstractControlCalculatable (self-contained)
- Excluded: 9 calculators (see EXTRACTION_EXECUTION_SUMMARY.md)

---

## Common Questions

**Q: Why was I asked to defer certain classes?**  
A: Classes with complex interdependencies (like circuit.matrix) should be extracted after their dependencies are available. This prevents circular import issues and makes compilation debugging easier.

**Q: Should I extract deprecated classes?**  
A: Yes, extract them as-is. Mark with `@Deprecated` if not already marked. The refactoring phase can remove them later.

**Q: What if a class has a small GUI import I can remove?**  
A: Extract without modification. Let the architect decide on refactoring strategy. Stick to pure extraction first.

**Q: Can I extract partial classes?**  
A: No, always extract complete classes. If a class is too coupled, mark it for deferred extraction.

**Q: Should I update javadoc/comments?**  
A: No, extract as-is. Documentation improvements are a separate phase.

---

## Support & Escalation

### If you encounter errors:
1. Check EXTRACTION_EXECUTION_SUMMARY.md for dependency details
2. Run `mvn compile -q 2>&1 | head -100` to see actual errors
3. Identify the missing class from error message
4. Extract that class first
5. Re-attempt compilation

### If stuck:
- Reference the "Extraction Procedure Template" section
- Look at Sprint E2-E4 examples in git history: `git log --oneline | head -5`
- Check actual file locations: `find src -name "CLASSNAME.java" -type f`

---

**Next Sprint Recommendation:** Start with Priority 1 (Core Enums) - it's low-risk and enables multiple other sprints.

**Estimated Time:** 2-3 hours for high-value, low-complexity extractions.

Good luck! 🚀
