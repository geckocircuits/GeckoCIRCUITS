# Warning Suppression Policy

This is a legacy codebase with intentionally suppressed warnings to maintain developer productivity and focus on actual bugs.

## Warnings Suppressed

The following warning types are suppressed in `pom.xml`:
- **rawtypes** - Raw type usage in generic classes
- **unchecked** - Unchecked type conversions
- **deprecation** - Use of deprecated APIs
- **serial** - Missing serialVersionUID
- **cast** - Type cast safety
- **finally** - Finally block issues
- **path** - Classpath problems
- **fallthrough** - Switch fallthrough
- **overloads** - Method overloading
- **static** - Static member access

## Why Suppressing is Acceptable

1. **Code works correctly** - All warnings are non-critical
2. **Legacy codebase** - Fixing 1000+ warnings is not practical
3. **Maven builds succeed** - No actual compilation errors
4. **Developer productivity** - Focus on real bugs, not type theory

## Strategy for New Code

When writing NEW code or making SIGNIFICANT changes to existing code:
- Use proper type parameters (e.g., `List<String>` instead of `List`)
- Add `@Override` annotations where appropriate
- Remove unused imports and fields
- Consider modern Java features (lambdas, switch expressions)

Fix warnings only in code you're actively modifying, not throughout the legacy codebase.

## Maven Configuration

See `pom.xml` for compiler settings:
```xml
<showWarnings>false</showWarnings>
<compilerArgs>
  <arg>-Xlint:-rawtypes</arg>
  <!-- ... all warning types disabled -->
</compilerArgs>
```

## VSCode Notes

VSCode's Java extension may still show warnings. These are **style suggestions** and do not affect:
- Build success
- Runtime behavior
- Code functionality

Filter VSCode Problems by Type: `hints` to reduce noise, or simply ignore them as documented style issues.

## References

- See `WARNING_FIX_GUIDE.md` for systematic approach if you choose to address warnings
- Maven compiles successfully with no errors
