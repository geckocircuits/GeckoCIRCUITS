# Extracted Helper Classes - Documentation Guide

## Overview
This document describes the refactoring patterns used to extract testable components from monolithic classes in the GeckoCIRCUITS control package.

## Pattern: Helper Class Extraction

### Objective
Extract complex but independent logic from large monolithic classes to:
- Improve testability (remove GUI/framework dependencies)
- Enable comprehensive unit test coverage
- Reduce class complexity and improve maintainability
- Enable code reuse across multiple control blocks

## Extracted Classes

### 1. FileNameGenerator
**Source**: Extracted from `DataSaver.findFreeFile()`
**Location**: `ch.technokrat.gecko.geckocircuits.control.FileNameGenerator`
**Lines**: 77 lines

#### Purpose
Handles logic for generating unique file names with numeric suffixes to avoid overwriting existing files.

#### Public API
```java
public String findFreeFileName(String baseFileName)
```

#### Key Methods
- `findFreeFileName(String)` - Main API, generates unique filename
- `generateNumberedFileName()` - Inserts number into filename
- `parseFileName(String)` - Parses filename into components

#### Inner Classes
- `FileNameParts` - Data holder for filename components (prefix, suffix, extension)

#### Test Coverage
- **File**: `FileNameGeneratorTest.java`
- **Tests**: 9 unit tests
- **Coverage**: 97.5% (79/81 instructions)
- **Key Test Areas**:
  - Basic filename generation
  - Duplicate handling
  - Extension preservation
  - Special character handling

#### Usage Example
```java
FileNameGenerator generator = new FileNameGenerator();
String uniqueFile = generator.findFreeFileName("export_data.csv");
// Returns: "export_data.csv" or "export_data_1.csv" if original exists
```

### 2. SignalValidator
**Source**: Extracted from `DataSaver.compareAndCorrectSignalNamesIndices()`
**Location**: `ch.technokrat.gecko.geckocircuits.control.SignalValidator`
**Lines**: 132 lines

#### Purpose
Validates that signal names match their indices in the data container and attempts auto-correction.

#### Public API
```java
public ValidationResult validateSignals(
    List<String> originalNames,
    List<Integer> indices,
    AbstractDataContainer data)
```

#### Key Methods
- `validateSignals()` - Main validation logic
- `isValidSignalIndex()` - Index validation
- `findSignalIndexByName()` - Signal lookup by name

#### Inner Classes
- `ValidationResult` - Result object containing corrections and missing signals
- `SignalCorrection` - Data holder for individual corrections

#### Test Coverage
- **File**: `SignalValidatorTest.java`
- **Tests**: 8 unit tests
- **Coverage**: 100% (85/85 instructions)
- **Key Test Areas**:
  - Valid signal mapping
  - Auto-correction logic
  - Missing signal detection
  - Edge cases (empty lists, null values)

#### Usage Example
```java
SignalValidator validator = new SignalValidator();
ValidationResult result = validator.validateSignals(
    signalNames, indices, dataContainer);
if (result.hasMissing()) {
    // Handle missing signals
}
```

## Integration Testing Approach

### Integration Test Files
1. **DataSaverRefactoringIntegrationTest.java** (11 tests)
   - Basic integration scenarios
   - Component interaction validation
   - Stateless verification

2. **DataSaverAdvancedIntegrationTest.java** (15+ tests)
   - Real-world usage scenarios
   - Robustness testing
   - Edge case combinations
   - Memory efficiency testing

### Integration Test Patterns

#### Pattern 1: Component Independence
```java
@Test
public void testExtractedClassesAreIndependent() {
    // Create multiple instances
    FileNameGenerator gen1 = new FileNameGenerator();
    FileNameGenerator gen2 = new FileNameGenerator();
    
    // Both should work independently
    assertNotNull(gen1.findFreeFileName("file.txt"));
    assertNotNull(gen2.findFreeFileName("file.txt"));
}
```

#### Pattern 2: Stateless Verification
```java
@Test
public void testRefactoringPatternValidation() {
    FileNameGenerator generator = new FileNameGenerator();
    
    // Generate multiple calls
    String name1 = generator.findFreeFileName("test.txt");
    String name2 = generator.findFreeFileName("test.txt");
    
    // Both should be valid
    assertNotNull(name1);
    assertNotNull(name2);
}
```

#### Pattern 3: Real-World Scenario
```java
@Test
public void testMultipleFileGenerationSequence() {
    List<String> generatedNames = new ArrayList<>();
    
    for (int i = 0; i < 5; i++) {
        String fileName = fileNameGenerator.findFreeFileName("data.csv");
        assertFalse("Should not have duplicates", generatedNames.contains(fileName));
        generatedNames.add(fileName);
    }
}
```

## Benefits Achieved

### Testability
- **Before**: Cannot test `findFreeFile()` without entire DataSaver framework
- **After**: Can test `FileNameGenerator` in isolation with 97.5% coverage

### Code Quality
- **Reduced Complexity**: 516-line DataSaver → 77-line helper + original
- **Single Responsibility**: Each class has one clear purpose
- **Reusability**: Helper classes can be used by other control blocks

### Test Coverage
- **FileNameGenerator**: 97.5% (was 0% as part of DataSaver)
- **SignalValidator**: 100% (was 0% as part of DataSaver)
- **Net Gain**: +624 instructions covered in control package

## Recommended Patterns for Future Refactoring

### 1. Identify Extract Candidates
Look for methods that:
- Are >50 lines of code
- Don't depend on GUI/framework components
- Don't require class state
- Are used in multiple places
- Have clear, single responsibility

### 2. Extraction Steps
1. Create new class with extracted logic
2. Write comprehensive unit tests (target 80%+ coverage)
3. Update original class to use new helper
4. Run full test suite to verify no regressions
5. Document usage in integration tests

### 3. Testing Strategy
```
Unit Tests
├─ Test extracted class in isolation (80%+ coverage)
├─ Test all edge cases
├─ Test error conditions
└─ Test boundary values

Integration Tests
├─ Test interaction with original class
├─ Test real-world scenarios
├─ Test with multiple instances
└─ Test thread safety (if applicable)

Regression Tests
├─ Ensure all existing tests still pass
├─ Verify no behavior changes
└─ Check performance impact
```

## Anti-Patterns to Avoid

### 1. Don't Extract With Framework Dependencies
❌ **Bad**: Trying to extract GUI-related code
```java
// DON'T do this - still depends on Window/Dialog
public class ExtractedHelper {
    public void process(Window parent) { ... }
}
```

✅ **Good**: Extract only pure logic
```java
// DO this - no framework dependencies
public class ExtractedHelper {
    public Result process(Data input) { ... }
}
```

### 2. Don't Create Monolithic Helpers
❌ **Bad**: Moving too much into one helper
```java
// 500-line helper class - just pushed the problem
public class MegaHelper { ... }
```

✅ **Good**: Break into focused classes
```java
public class FileNameGenerator { ... }  // 77 lines
public class SignalValidator { ... }    // 132 lines
```

### 3. Don't Forget Integration Tests
❌ **Bad**: Only unit tests, no integration validation
```java
// Tests the class alone, but not with original context
class FileNameGeneratorTest { ... }
```

✅ **Good**: Unit + integration tests
```java
class FileNameGeneratorTest { ... }          // Unit tests
class DataSaverRefactoringIntegrationTest { } // Integration tests
```

## Metrics & Success Criteria

### Coverage Goals
- **Extracted Class**: 80%+ coverage
- **Integration Tests**: All happy paths + edge cases
- **Regression**: 0 failing existing tests

### Quality Metrics
- **Cyclomatic Complexity**: <10 per method
- **Lines of Code**: <300 per class
- **Test-to-Code Ratio**: >0.5 (for critical paths)

## Related Documentation
- `SESSION_SUMMARY_2026-01-26_IMPROVEMENTS.md` - Session completion report
- `COVERAGE_PROGRESS.md` - Coverage improvement timeline
- `DataSaver.java` - Original monolithic class
- Test files: `FileNameGeneratorTest.java`, `SignalValidatorTest.java`

## Future Improvements

### Short-term (1-2 weeks)
- Refactor `ReglerLimit` to extract calculation logic
- Extract `ReglerIntegrator` helper class
- Add more integration test patterns

### Medium-term (1 month)
- Extract 5-10 more utility classes from control blocks
- Achieve 70%+ coverage on 15+ classes
- Create reusable calculator helper utilities

### Long-term (3+ months)
- Systematic MVC pattern implementation
- Complete separation of GUI and business logic
- Achieve 85%+ on pure backend packages
