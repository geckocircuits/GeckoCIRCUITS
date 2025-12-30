# Warning Suppression Guide

All warnings are currently suppressed in `pom.xml`. To enable one warning rule at a time and fix the issues:

## Step 1: Enable a specific warning

Remove the corresponding `-Xlint:-<warning>` from the compiler args in `pom.xml`:

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <compilerArgs>
      <!-- Remove this line to enable raw type warnings -->
      <arg>-Xlint:-rawtypes</arg>
      <!-- Remove this line to enable unchecked warnings -->
      <arg>-Xlint:-unchecked</arg>
      <!-- ... etc -->
    </compilerArgs>
  </configuration>
</plugin>
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

## Re-enable all warnings

After fixing all individual warnings, remove ALL compilerArgs to enable full checking:

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <source>21</source>
    <target>21</target>
    <release>21</release>
    <!-- Remove all compilerArgs when done -->
  </configuration>
</plugin>
```
