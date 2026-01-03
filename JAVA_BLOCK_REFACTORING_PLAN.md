# Java Block Refactoring Plan

## Problem Statement

The Java Block functionality in GeckoCIRCUITS is currently broken:

1. **Double-click on Java Block does not open CodeWindow** - When users double-click a Java block in the circuit sheet, the code editor window should appear but it doesn't

2. **Simulations with Java Blocks fail** - Any circuit containing a Java block cannot run simulations

3. **Root Cause**: The code uses an outdated method to detect Java Compiler availability (`com.sun.tools.javac.Main`), which was:
   - Part of `tools.jar` in Java 8 and earlier
   - Bundled into JDK from Java 9+
   - Internal implementation class that may not be accessible in Java 21
   - Causes `compiler_toolsjar_missing` flag to be incorrectly set to `true`

## Analysis

### Current Behavior Flow

```
User double-clicks Java Block
    ↓
ReglerJavaFunction.openDialogWindow() (line 624)
    ↓
Checks if (GeckoSim.compiler_toolsjar_missing)  ← Problem: This is TRUE
    ↓
Shows error: "No tools.jar library found!"
    ↓
CodeWindow is NOT opened
```

### Simulation Flow (Also Broken)

```
Simulation starts
    ↓
JavaBlockCalculator.initializeAtSimulationStart() (line 304)
    ↓
AbstractJavaBlock.initialize() (line 127)
    ↓
Checks compile status, tries to compile
    ↓
Compilation fails because compiler is missing
    ↓
Simulation crashes or produces no output
```

### Root Cause in GeckoSim.java (lines 535-541)

```java
try {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    // What about 'tools.jar', which the CONTROL block 'JAVA-Function' needs as compiler?
    try {
        cl.loadClass("com.sun.tools.javac.Main");  // ← PROBLEM HERE
        compiler_toolsjar_missing = false;
    } catch (Exception e) {
        compiler_toolsjar_missing = true;  // ← Always true on Java 21
    }
}
```

**Issues with current approach:**
1. `com.sun.tools.javac.Main` is internal API
2. In Java 9+, tools.jar is merged into JDK
3. In Java 21, module system may restrict access
4. Looking for a specific implementation class is fragile

## Proposed Solution

### Phase 1: Fix Compiler Detection (Priority: CRITICAL)

**File**: `GeckoSim.java`

**Change**: Replace internal class loading with proper Java Compiler API detection

```java
// OLD CODE (lines 535-541):
try {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    try {
        cl.loadClass("com.sun.tools.javac.Main");
        compiler_toolsjar_missing = false;
    } catch (Exception e) {
        compiler_toolsjar_missing = true;
    }
}

// NEW CODE:
try {
    javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
    if (compiler != null) {
        compiler_toolsjar_missing = false;
    } else {
        compiler_toolsjar_missing = true;
    }
} catch (NoClassDefFoundError | SecurityException e) {
    compiler_toolsjar_missing = true;
}
```

**Benefits:**
- Uses standard Java Compiler API
- Works with Java 8, 9, 11, 17, 21+
- No dependency on internal implementation classes
- Properly detects JDK vs JRE

**Testing:**
1. Build with Java 21 JDK
2. Verify `compiler_toolsjar_missing` is `false`
3. Double-click Java block - should open CodeWindow
4. Run simulation with Java block - should work

### Phase 2: Update Compiler Version Detection (Priority: HIGH)

**File**: `CodeWindow.java` (lines 290-305)

**Current Issue**: References `tools_170.jar` and `tools_180.jar` which don't exist for Java 21

**Action**: Update message to mention Java 21

```java
// Update messages to include Java 21 version
+ "lib/tools.jar can be upgraded. It is recommended to replace lib/tools.jar with a Java 21 compatible version\nwhich you can "
```

### Phase 3: Remove tools.jar Dependency (Priority: MEDIUM)

**Files**: Multiple locations mentioning tools.jar

**Action**: Update documentation and error messages to reflect modern Java

1. Update error messages to not mention `tools.jar` specifically
2. Update documentation to state requirement for JDK (not just JRE)
3. Update build scripts to verify JDK availability

### Phase 4: Improve Error Handling (Priority: LOW)

**Files**: 
- `ReglerJavaFunction.java` (line 625-643)
- `CompileObject.java`

**Improvements:**

1. **Better error message when compiler is missing:**
   ```java
   JOptionPane.showMessageDialog(null,
       "Java Compiler not found!\n\n" +
       "GeckoCIRCUITS requires a JDK (not just JRE) to compile Java blocks.\n" +
       "Please ensure you're running with JDK 21 or later.\n\n" +
       "Current Java version: " + System.getProperty("java.version"),
       "Java Compiler Error",
       JOptionPane.ERROR_MESSAGE);
   ```

2. **Graceful degradation:**
   - Show warning but don't crash application
   - Allow circuit to load (just can't use Java blocks)
   - Display status on Java block (e.g., "Compiler unavailable")

3. **Diagnostic information:**
   - Log Java version and vendor
   - Log compiler availability
   - Check for JDK vs JRE

### Phase 5: Testing (Priority: HIGH)

**Files**: 
- `src/test/java/ch/technokrat/systemtests/ModelResultsTest.java`

**Actions:**

1. Enable currently disabled test (line 77):
   ```java
   @Test
   // Remove: @Ignore("Java script engine issue, java code block does not work")
   public void opAmp(){
       openRunAssert(OPAMP_PATH + "OpAmp.ipes");
   }
   ```

2. Create new test for Java Block compilation:
   ```java
   @Test
   public void testJavaBlockCompilation() {
       // Create a simple Java block
       // Compile it
       // Verify compilation succeeded
   }
   ```

3. Test with example circuits:
   - `resources/JAVA_Block/demo_JAVA_Block.ipes`
   - `resources/JAVA_Block/BuckBoost_thermal_with_java.ipes`

### Phase 6: Documentation (Priority: MEDIUM)

**Files**: 
- `README.md`
- `JAVA_BLOCK_DOCUMENTATION.md`
- `CLAUDE.md`

**Actions:**

1. Update system requirements:
   ```markdown
   ### Requirements
   - Java 21 (JDK required for Java Block feature)
   - For circuits using Java blocks, full JDK is required (JRE is insufficient)
   ```

2. Update troubleshooting section:
   ```markdown
   ### Java Block Not Working
   If Java blocks don't compile:
   1. Verify you're using JDK, not JRE: `java -version`
   2. Check JDK path is correct
   3. Ensure Java 21 or later
   ```

## Implementation Order

### Quick Fix (Same Day)
1. **Phase 1**: Fix compiler detection in `GeckoSim.java`
2. Test: Build, run, double-click Java block

### Short-term (1-2 days)
3. **Phase 2**: Update version messages in `CodeWindow.java`
4. **Phase 3**: Update error messages in `ReglerJavaFunction.java`
5. Test: Run example Java block circuits

### Medium-term (1 week)
6. **Phase 4**: Improve error handling
7. **Phase 5**: Enable and add tests
8. Test: Full test suite

### Long-term (2 weeks)
9. **Phase 6**: Update documentation
10. Review and cleanup

## Testing Checklist

### After Phase 1
- [ ] Application starts without errors
- [ ] `GeckoSim.compiler_toolsjar_missing` is `false` (verify with debug output)
- [ ] Double-clicking a Java block opens CodeWindow
- [ ] CodeWindow shows tabs correctly (Source, Imports, Variables, Init, etc.)
- [ ] Can type code in CodeWindow

### After Phase 2-3
- [ ] Error messages are helpful and accurate
- [ ] No mention of outdated tools.jar versions
- [ ] Running with JRE shows appropriate error

### After Phase 4-5
- [ ] Simulation runs successfully with Java block
- [ ] Example circuits work:
  - [ ] `demo_JAVA_Block.ipes`
  - [ ] `BuckBoost_thermal_with_java.ipes`
  - [ ] `ThreePhase-VSR_10kW_thermal_with_java.ipes`
- [ ] Unit tests pass
- [ ] System tests pass

### Cross-platform Testing
- [ ] Windows: Test on Windows 10/11
- [ ] Linux: Test on Ubuntu/Debian
- [ ] macOS: Test on macOS 13/14

## Risk Assessment

### High Risk
- None (changes are to fix broken functionality, not to change working behavior)

### Medium Risk
- Phase 1 changes may affect other parts of codebase that check `compiler_toolsjar_missing`
- Should verify `ReglerNativeC` also works (line 403-428 in `ReglerNativeC.java`)

### Low Risk
- Documentation and message updates
- Test enablement

## Rollback Plan

If Phase 1 causes issues:

1. Keep old compiler detection code as fallback:
   ```java
   boolean compilerFound = false;
   
   // Try new method (Java 9+)
   try {
       javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
       if (compiler != null) {
           compilerFound = true;
       }
   } catch (Exception e) {
       // Fall back to old method
   }
   
   // Fallback to old method (Java 8)
   if (!compilerFound) {
       try {
           ClassLoader cl = ClassLoader.getSystemClassLoader();
           cl.loadClass("com.sun.tools.javac.Main");
           compilerFound = true;
       } catch (Exception e) {
           compilerFound = false;
       }
   }
   
   compiler_toolsjar_missing = !compilerFound;
   ```

2. Add system property to force old behavior:
   ```bash
   java -Dgecko.use.old.compiler.detection=true -jar gecko.jar
   ```

## Success Criteria

1. ✅ Java Block CodeWindow opens on double-click
2. ✅ Can compile Java code without errors
3. ✅ Simulations with Java blocks run successfully
4. ✅ All example circuits work
5. ✅ Tests pass
6. ✅ Cross-platform compatibility maintained
7. ✅ Documentation updated
8. ✅ No regression in other features

## Notes

- The Java Block feature is heavily used in power electronics research (see example circuits)
- Fixing this will restore functionality for many research workflows
- The fix aligns with modern Java practices and removes legacy dependencies
- Consider adding a startup check to warn users if running with JRE instead of JDK

## References

- Java Compiler API: https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/tools/package-summary.html
- ToolProvider documentation: https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/tools/ToolProvider.html
- Java 9+ module system changes: JEP 220 - Modular Run-Time Images
