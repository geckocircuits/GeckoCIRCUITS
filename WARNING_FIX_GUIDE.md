# Warning Fix Guide

All warnings are currently suppressed in `pom.xml`. To enable one warning rule at a time and fix the issues systematically:

## VSCode Warning Display Issue

**Important:** VSCode's Java extension performs independent analysis from the Maven compiler. The Maven compiler suppression in `pom.xml` does NOT prevent VSCode from showing warnings.

### Option 1: Filter warnings in VSCode (Quick)

In VSCode Problems panel (Ctrl+Shift+M), click the filter icon and type to hide specific warnings:
- Type: `rawtypes` - hides raw type warnings
- Type: `unchecked` - hides unchecked warnings
- Type: `unused` - hides unused variable/import warnings
- Type: `hints` - hides all style hints

### Option 2: Configure VSCode to ignore problem types

Add to `.vscode/settings.json`:
```json
"java.errors.incompleteClasspath.severity": "ignore",
"java.errors.typeParameter": "ignore",  // For raw types
"java.errors.unused": "ignore",  // For unused code
"java.errors.deprecation": "ignore",  // For deprecated usage
"java.completion.enabled": false,  // Disable completion warnings
```

### Option 3: Systematic fix (Recommended)

Fix warnings one category at a time:

## Step 1: Enable a specific warning type

Remove the corresponding `-Xlint:-<warning>` from `pom.xml` compiler args:

```xml
<!-- Remove this line to enable raw type warnings -->
<arg>-Xlint:-rawtypes</arg>
<!-- Remove this line to enable unchecked warnings -->
<arg>-Xlint:-unchecked</arg>
<!-- ... etc -->
```

## Step 2: Recompile to see the warnings

```bash
mvn clean compile
```

## Step 3: Find all occurrences of this warning type

Search the codebase for the specific warning pattern. Example: for raw types, search for:
- `CircuitComponent<` without type parameter
- `UserParameter<` without type parameter
- `DialogElementLK<` without type parameter
- `JComboBox<` without type parameter

## Step 4: Fix all instances

Apply the appropriate fixes:
- **Raw types**: Add type parameters (e.g., `CircuitComponent<AbstractTwoPortLKreisBlock>`)
- **Unused**: Remove unused imports, fields, or methods
- **Redundant**: Remove redundant interfaces
- **Override**: Add `@Override` annotations
- **Lambda**: Convert anonymous classes to lambdas

## Step 5: Recompile and verify

```bash
mvn clean compile
```

Check that all warnings of that type are gone before moving to the next rule.

## Step 6: Repeat for next warning type

Move to the next `-Xlint` option and repeat steps 1-5.

## Warning Types to Fix (in order of complexity)

1. **rawtypes** - Raw type usage
   - Add type parameters to generic classes
   - Example: `List<String>` instead of `List`

2. **unchecked** - Unchecked operations
   - Add `@SuppressWarnings("unchecked")` where type safety cannot be guaranteed
   - Consider using typed collections

3. **unused** - Unused code
   - Remove unused imports
   - Remove unused fields and methods
   - Add `@SuppressWarnings("unused")` for intentionally unused params

4. **deprecation** - Deprecated APIs
   - Replace `newInstance()` with `getDeclaredConstructor().newInstance()`
   - Update to newer API calls

5. **serial** - Missing serialVersionUID
   - Add `private static final long serialVersionUID = 1L;` to Serializable classes

6. **cast** - Type casts
   - Add explicit type checks before casts
   - Use `instanceof` where appropriate

7. **path** - Classpath issues
   - Ensure all dependencies are available
   - Fix missing imports

8. **fallthrough** - Missing break statements
   - Add `// fallthrough` comments or break statements in switch cases

9. **overloads** - Method overloading issues
   - Review method signatures for conflicts

10. **static** - Static member access
   - Use `ClassName.staticMember` instead of `instance.staticMember`

## Additional VSCode-Specific Warnings (hints)

These are style suggestions from VSCode's Java extension:
- **Leaking this in constructor** - Passing `this` before super() call (often OK)
- **Convert switch to rule switch** - Java 12+ feature (can ignore)
- **Anonymous to lambda** - Modernization suggestion (can ignore)
- **Constant naming** - Style preference (e.g., `DEF_uF` is intentional)

To hide these "hints" in VSCode, filter by Type: `hints` in the Problems panel.

## Re-enable all warnings

After fixing all individual warnings, remove ALL compilerArgs to enable full checking:

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <source>21</source>
    <target>21</target>
    <release>21</release>
    <!-- Remove all <compilerArgs> when done -->
  </configuration>
</plugin>
```

## Summary

- Maven settings only affect Maven builds, not VSCode analysis
- VSCode shows warnings independently of `pom.xml` settings
- To work efficiently, fix ONE warning category at a time
- Use VSCode Problems filter to see only the warnings you're currently fixing
- Consider ignoring "hints" type (style suggestions) if they don't affect functionality
