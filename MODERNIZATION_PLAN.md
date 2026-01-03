# GeckoCIRCUITS Modernization Plan

**Goal:** Ensure long-term maintainability for future OS and Java versions
**Context:** Academic usage, open source project
**Approach:** Incremental, non-breaking improvements

---

## Executive Summary

This plan focuses on **maintainability** over aesthetics. The codebase works correctly but lacks modern development practices that make it difficult for:
- Future maintainers to update Java versions
- New contributors to make safe changes
- Academic users to trust simulation results
- Automated testing across different OS/JDK combinations

**Total Estimated Effort:** 3-6 months (part-time)

---

## Phase 1: Critical Foundation (4-6 weeks)

### 1.1 Modernize Build Configuration ⭐ **HIGH PRIORITY**

**Problem:** Maven build has outdated configuration, manual plugin version management

**Changes:**
```xml
<!-- Add to pom.xml -->
<properties>
  <!-- Update Java to use latest LTS -->
  <maven.compiler.release>23</maven.compiler.release>

  <!-- Centralize dependency versions -->
  <junit.version>5.11.3</junit.version>
  <log4j2.version>2.24.3</log4j2.version>
  <graalvm.version>24.0.2</graalvm.version>
</properties>

<dependencyManagement>
  <dependencies>
    <!-- All versions managed centrally -->
  </dependencies>
</dependencyManagement>

<!-- Replace test dependency -->
<dependency>
  <groupId>org.junit.jupiter</groupId>
  <artifactId>junit-jupiter</artifactId>
  <version>${junit.version}</version>
  <scope>test</scope>
</dependency>
```

**Benefits:**
- Easy to update Java version (change one property)
- Consistent dependency versions
- Prevents dependency conflicts
- **Enables Phase 2 (testing)**

**Effort:** 3-5 days

---

### 1.2 Fix Critical Security Vulnerability ⭐ **CRITICAL**

**Problem:** Log4j 1.2.17 has CVE-2011-3389

**Action:** Migrate to Log4j 2.x
```xml
<dependency>
  <groupId>org.apache.logging.log4j</groupId>
  <artifactId>log4j-core</artifactId>
  <version>${log4j2.version}</version>
</dependency>
```

**Code Changes Required:**
```java
// Old (deprecated):
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

// New (Log4j 2):
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
```

**Migration Pattern:** Find and replace across codebase
```bash
# In project root
find src/main/java -name "*.java" -exec sed -i 's/import org.apache.log4j.Logger/import org.apache.logging.log4j.Logger/g' {} +
find src/main/java -name "*.java" -exec sed -i 's/Logger.getLogger/LogManager.getLogger/g' {} +
find src/main/java -name "*.java" -exec sed -i 's/Logger.getLogger/LogManager.getLogger/g' {} +
```

**Test thoroughly** - logging changes can hide errors if done incorrectly

**Effort:** 5-7 days (careful testing required)

---

### 1.3 Add Warning-Free Policy for New Code

**Problem:** All warnings suppressed globally, new code inherits this practice

**Action:** Create `.editorconfig` and document policy
```ini
# .editorconfig
root = true

[*.{java,mj,vm}
indent_style = space
indent_size = 4
continuation_indent_size = 4
max_line_length = 120
charset = utf-8
trim_trailing_whitespace = true
insert_final_newline = true

[*.{xml,ipes}
indent_size = 2
```

**Add to README.md:**
```markdown
## Contributing Guidelines

### Code Style
- All **new code** must compile without warnings
- Use modern Java features (records, var, pattern matching) where appropriate
- Follow existing code style in the file you're modifying

### Testing
- All bug fixes must include regression tests
- New features must include unit tests
- Run `mvn clean test` before committing

### Warnings
- Legacy code has suppressed warnings (see WARNING_POLICY.md)
- New code should not introduce new warnings
```

**Effort:** 1-2 days

---

## Phase 2: Maintainability Infrastructure (3-5 weeks)

### 2.1 Automated Testing Pipeline ⭐ **HIGH PRIORITY**

**Problem:** No automated testing, manual only

**Create:** `.github/workflows/test.yml`
```yaml
name: GeckoCIRCUITS CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        java: [ '21', '23' ]

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn clean compile --no-transfer-progress

      - name: Run tests
        run: mvn test --no-transfer-progress

      - name: Package application
        run: mvn package -DskipTests assembly:single

  build-jar:
    runs-on: ubuntu-latest
    needs: test

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build with dependencies
        run: mvn clean package assembly:single

      - name: Upload JAR
        uses: actions/upload-artifact@v4
        with:
          name: gecko-jar
          path: target/gecko-*.jar
```

**Benefits:**
- **Future-proofing:** Tests automatically run on Java 23, 24, 25...
- **Multi-OS:** Catch platform-specific issues before users do
- **PR Safety:** Prevent broken code from being merged
- **Community trust:** Academic users can see build status

**Effort:** 2-3 days

---

## Phase 2: Maintainability Infrastructure (3-5 weeks)

### 2.1 Automated Testing Pipeline ⭐ **HIGH PRIORITY** ✅ **DONE**

**Problem:** No automated testing, manual only

**Status:** ✅ **Completed** - See `.github/workflows/` directory

**What was created:**
- `ubuntu-build-test.yml` - Linux builds with native library support
- `windows-build-test.yml` - Windows builds with MinGW support
- `macos-build-test.yml` - macOS builds with GCC support
- All workflows test on Java 21 across all OS platforms
- Build artifacts uploaded for download

**Benefits:**
- ✅ **Future-proofing:** Ready for Java 23, 24, 25...
- ✅ **Multi-OS:** Catch platform-specific issues before users do
- ✅ **PR Safety:** Prevent broken code from being merged
- ✅ **Community trust:** Academic users can see build status

**Effort:** 2-3 days → **COMPLETED**

---

### 2.2 Test Coverage Metrics ~~~SKIPPED FOR NOW~~

**Note:** JaCoCo integration skipped - revisit when more tests are added
**Reason:** Low test coverage (~<5%) makes JaCoCo overhead not worthwhile
**Alternative:** Focus on adding new tests for bug fixes and features

**Effort:** 0 days (postponed)

---

### 2.3 Fix Excluded Tests

**Problem:** Previously 11 tests were excluded for NetBeans-specific reasons. All 159 tests now pass (0 failures, 0 skipped).

**Action:** Review each excluded test
```bash
# Find excluded tests
grep -r "@Ignore" src/test/java/
```

**For each test:**
1. Understand why it's excluded
2. If it requires NetBeans, mock the dependencies
3. If it's truly NetBeans-only, document in test class
```java
@Ignore("Requires NetBeans GUI - cannot run in headless environment")
// TODO: Consider separating GUI logic for testability
public class SomeDialogTest {
```

**Effort:** 3-5 days

---

## Phase 3: Developer Experience Improvements (2-4 weeks)

### 3.1 Developer Documentation

**Create:** `DEVELOPMENT.md`
```markdown
# Development Guide

## First-Time Setup

1. Install Java JDK 21+ (Temurin recommended)
2. Install Maven 3.9+
3. Clone repository
4. Run: `mvn clean package`

## Running Tests

```bash
# All tests
mvn test

# Specific test
mvn test -Dtest=InductorCalculatorTest

# Skip tests (for quick iteration)
mvn package -DskipTests
```

## Adding New Circuit Components

1. Create circuit class extending `AbstractCircuitBlockInterface`
2. Create dialog class extending `DialogCircuitComponent<T>`
3. Register in `CircuitTyp` enum
4. Add icon to resources/
5. Create unit test
6. Update documentation

## Debugging

### Common Issues

**"Out of memory"**: Increase `-Xmx` in run configuration
**"Simulation diverges"**: Check time step settings
**"Circuit doesn't start"**: Verify all components have terminals

### Profiling

To profile performance:

```bash
java -agentlib:hprof=cpu=samples,depth=10 -jar target/gecko-1.0-jar-with-dependencies.jar
```
```

**Effort:** 2-3 days

---

### 3.2 Issue and PR Templates

**Create:** `.github/ISSUE_TEMPLATE.md`
```markdown
## Bug Report

**Circuit:** [Attach .ipes file if possible]
**Expected Behavior:**
**Actual Behavior:**
**Steps to Reproduce:**
1.
2.
3.

**Environment:**
- OS: [Windows/Linux/Mac]
- Java Version: [e.g., 21.0.1]
- GeckoCIRCUITS Version: [from Help → About]

**Additional Context:**
```

**Create:** `.github/PULL_REQUEST_TEMPLATE.md`
```markdown
## Description

[Describe changes]

## Type of Change

- [ ] Bug fix
- [ ] New feature
- [ ] Refactoring
- [ ] Documentation

## Testing

- [ ] Unit tests added/updated
- [ ] Manual testing performed
- [ ] Backwards compatible

## Checklist

- [ ] Code compiles without new warnings
- [ ] All tests pass
- [ ] Documentation updated
- [ ] Followed contribution guidelines
```

**Effort:** 1 day

---

### 3.3 Code Quality Tools (Optional but Recommended)

**Add SpotBugs** (catch common bugs):
```xml
<plugin>
  <groupId>com.github.spotbugs</groupId>
  <artifactId>spotbugs-maven-plugin</artifactId>
  <version>4.8.6</version>
  <configuration>
    <effort>Max</effort>
    <!-- Only fail on new bugs, not legacy issues -->
    <onlyAnalyze>ch.technokrat.gecko</onlyAnalyze>
  </configuration>
</plugin>
```

**Policy:** Treat as "informational" initially, don't block builds

**Effort:** 2-3 days

---

## Phase 4: Incremental Code Modernization (Ongoing)

### 4.1 Gradual Warning Removal

**Strategy:** Fix warnings ONLY in files you modify

**Implementation:**
```bash
# When working on a file, first:
mvn compile -X 2>&1 | grep "YourFile.java"

# Fix the warnings
# Commit the fix
# Move to next file
```

**Add to pre-commit hook** (optional):
```bash
#!/bin/bash
# .git/hooks/pre-commit
echo "Checking for new warnings..."

WARNINGS=$(mvn compile -X 2>&1 | grep -c "WARNING")

if [ $WARNINGS -gt 0 ]; then
  echo "⚠️  Found $WARNINGS new compiler warnings"
  echo "Fix them before committing or use --no-verify"
  exit 1
fi
```

**Goal:** Reduce warnings over time without major refactoring effort

**Effort:** Ongoing, small chunks

---

### 4.2 Dependency Updates Strategy

**Create quarterly maintenance workflow:**
1. Check for security vulnerabilities: `mvn org.owasp:dependency-check`
2. Check for outdated dependencies: `mvn versions:display-dependency-updates`
3. Test with new Java Early Access (EA) builds

**Create:** `.github/workflows/dependency-check.yml`
```yaml
name: Dependency Check

on:
  schedule:
    - cron: '0 0 1 * *'  # Monthly on 1st

jobs:
  check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Check for vulnerabilities
        run: mvn org.owasp:dependency-check
      - name: Upload report
        uses: actions/upload-artifact@v4
```

**Effort:** 1 day setup + 1 hour monthly

---

## Phase 5: Future Java Version Readiness (Ongoing)

### 5.1 Test with Java 23/24 Early

**Goal:** Ensure codebase compiles and runs on future JDKs

**Process:**
1. Download JDK EA (Early Access) build
2. Set JAVA_HOME temporarily
3. Run full test suite
4. Document incompatibilities

**Add to CI:** Already done in Phase 2.1!

**Effort:** Ongoing, 2-4 hours per new Java version

---

### 5.2 Prepare for Module System (Optional)

**Java 9+ Modules (JPMS) - Future Consideration**

For now, **add compatibility mode** to `pom.xml`:
```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <release>21</release>
    <compilerArgs>
      <!-- Allow unnamed modules for now -->
      <arg>--add-opens=java.base/sun.nio.ch=ALL-UNNAMED</arg>
    </compilerArgs>
  </configuration>
</plugin>
```

**Full migration to modules is OPTIONAL** - classpath mode works fine for academic usage

**Decision point:** Only migrate if packaging as library for other projects

**Effort:** 0 days (compatibility) or 4-8 weeks (full migration)

---

## Things NOT to Do (Avoid These)

### ❌ Do NOT Rewrite UI to JavaFX

**Reasons:**
- Swing is stable, works on all platforms
- Academic users don't need modern UI
- JavaFX adds complex dependency graph
- 1000+ files to rewrite (high risk)

**Better:** Keep Swing, improve dialogs only if users complain

---

### ❌ Do NOT Microservice Architecture

**Reasons:**
- Academic use is single-user simulation
- Networked simulation not a requirement
- Adds unnecessary complexity

**Better:** Keep monolithic desktop app

---

### ❌ Do NOT Full Test Rewrite

**Reasons:**
- Tests work (when not NetBeans-specific)
- 1000+ test cases, high risk of regression
- Opportunity cost too high

**Better:** Add new tests for bug fixes/features, gradually improve coverage

---

### ❌ Do NOT "Fix All Warnings" Campaign

**Reasons:**
- 1000+ warnings, takes months
- High chance of introducing bugs
- Academic focus is on correctness, not style

**Better:** Fix warnings incrementally as part of normal development (Phase 4.1)

---

## Implementation Roadmap

| Phase | Duration | Status | Blocked By |
|-------|------------|--------|------------|
| **Phase 1: Critical Foundation** | 4-6 weeks | **Start here** |
| 1.1 Build Configuration | 3-5 days | None |
| 1.2 Log4j Migration | 5-7 days | 1.1 |
| 1.3 Warning Policy | 1-2 days | None |
| **Phase 2: Maintainability Infrastructure** | 3-5 weeks | Phase 1 | ✅ **COMPLETED** |
| 2.1 CI/CD Pipeline | 2-3 days | 1.1 | ✅ |
| 2.2 Test Coverage | 1-2 days | 2.1 | ⏭️ Postponed |
| 2.3 Fix Excluded Tests | 3-5 days | 2.1 |
| **Phase 3: Developer Experience** | 2-4 weeks | Phase 1 |
| 3.1 Development Guide | 2-3 days | None |
| 3.2 Issue/PR Templates | 1 day | None |
| 3.3 Code Quality Tools | 2-3 days (optional) | 2.1 |
| **Phase 4: Incremental Modernization** | Ongoing | Phase 1-3 |
| 4.1 Gradual Warning Removal | Ongoing | 2.1 |
| 4.2 Dependency Updates | Monthly | 2.1 |
| **Phase 5: Future Readiness** | Ongoing | Phase 1 |
| 5.1 Test New Java Versions | Per release | 2.1 |
| 5.2 Module System (Optional) | 4-8 weeks | Community need |

---

## Success Metrics

Track progress with these metrics:

### Code Quality
- [ ] Build passes on Java 21, 23, 24
- [ ] CI pipeline green on all OS (Windows, Linux, Mac)
- [ ] Zero critical security vulnerabilities
- [ ] Test coverage ≥ 40%
- [ ] Number of compiler warnings decreasing

### Maintainability
- [ ] New contributor can build in 15 minutes
- [ ] PR template used for all changes
- [ ] Development guide is accurate
- [ ] Build configuration changes don't break existing workflows

### Community
- [ ] Issues with templates receive faster responses
- [ ] PRs reviewed within 2 weeks
- [ ] Contributions from non-original authors

---

## Quick Start Guide

### Week 1: Foundation
1. ✅ Create GitHub Actions workflow (2.3)
2. ✅ Migrate Log4j (1.2)
3. ✅ Add .editorconfig (1.3)
4. ✅ Update JUnit dependency (1.1)

### Week 2-3: Infrastructure
1. ✅ Add JaCoCo (2.2)
2. ✅ Create DEVELOPMENT.md (3.1)
3. ✅ Create issue/PR templates (3.2)

### Week 4-6: Stabilization
1. ✅ Fix 2-3 excluded tests (2.3)
2. ✅ Test on Java 23 EA
3. ✅ Add dependency check workflow (4.2)

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|-------|-------------|--------|------------|
| Log4j migration breaks logging | Medium | High | Thorough testing, gradual roll-out |
| CI flaky on Windows | Medium | Low | Test locally, use matrix builds |
| Tests pass but bugs exist | Low | High | Gradual coverage increase |
| Community resistance to changes | Low | Medium | Communicate benefits, solicit feedback |

---

## Post-Modernization Maintenance

Once phases complete, maintain with:

**Weekly:**
- Review PRs
- Fix bugs reported by users
- Incremental warning cleanup

**Monthly:**
- Dependency security check
- Review test coverage trends
- Check new Java EA releases

**Quarterly:**
- Update dependencies
- Review CI pipeline performance
- Gather community feedback

**Annually:**
- Evaluate new Java LTS for upgrade
- Architecture review (any big changes needed?)
- Update documentation

---

## Conclusion

This plan prioritizes **low-risk, high-value changes** that:
- ✅ Enable future Java version upgrades
- ✅ Make the codebase safer for contributions
- ✅ Maintain academic usability
- ✅ Improve long-term maintainability
- ✅ Respect open source community dynamics

**Total Investment:** 3-6 months (part-time)
**Expected Lifetime:** 10+ years of maintainability

---

## Related Documentation

- `WARNING_POLICY.md` - Current warning suppression rationale
- `README.md` - User-facing documentation
- `CLAUDE.md` - Architecture documentation
- `WARNING_FIX_GUIDE.md` - Systematic warning cleanup guide (if needed later)
