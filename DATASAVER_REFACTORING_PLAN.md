# DataSaver Refactoring Plan

## Overview
**Package**: `ch.technokrat.gecko.geckocircuits.control`  
**File**: `DataSaver.java` (516 lines)  
**Test Coverage**: 0%  
**Target Coverage**: 85%+

## Problem Analysis

### Current Issues
1. **Monolithic `SaveRunnable.run()` method** (~75 lines)
   - Mixes simulation-end and during-simulation logic
   - Handles multiple concerns: initialization, saving, cleanup
   - Difficult to test independently

2. **Complex `doFullSave()` method** (~50 lines)
   - Mixed responsibilities: progress tracking, data saving, thread coordination
   - Hard-coded magic numbers
   - Nested conditions

3. **`AbstractLinePrinter.compareAndCorrectSignalNamesIndices()` method** (~35 lines)
   - Complex nested loops
   - Side effects buried in validation logic
   - Poor separation of concerns

4. **Tightly coupled dependencies**
   - Direct Observable/Observer pattern usage
   - Thread management mixed with business logic
   - File I/O mixed with data formatting

## Refactoring Strategy

### Phase 1: Extract Service Classes (New Files)

#### 1.1 Create `FileNameGenerator.java`
Extract file naming logic from `findFreeFile()`:
```java
public class FileNameGenerator {
    private static final int MAX_FILE_COUNTER = 1000;
    
    public String findFreeFileName(String origFile);
    String generateNumberedFileName(String origFile, int counter);
    FileNameParts parseFileName(String fileName);
}
```

#### 1.2 Create `SignalValidator.java`
Extract signal validation and correction:
```java
public class SignalValidator {
    public ValidationResult validateSignals(
        List<String> originalNames,
        List<Integer> indices,
        AbstractDataContainer data);
        
    public int findSignalIndexByName(String name, AbstractDataContainer data);
}
```

#### 1.3 Create `SaveProgressTracker.java`
Extract progress tracking logic:
```java
public class SaveProgressTracker extends Observable {
    public void updateProgress(int current, int total);
    public int getPercentage();
    public void reset();
}
```

#### 1.4 Create `SaveStrategy` interface + implementations
Extract save mode logic:
```java
public interface SaveStrategy {
    void execute(DataSaver saver, AbstractDataContainer data);
}

public class SimulationEndSaveStrategy implements SaveStrategy { }
public class DuringSimulationSaveStrategy implements SaveStrategy { }
public class ManualSaveStrategy implements SaveStrategy { }
```

### Phase 2: Refactor Existing Methods

#### 2.1 Refactor `SaveRunnable.run()`
**Before**: 75 lines of mixed logic  
**After**: Delegate to strategy pattern

```java
@Override
public void run() {
    SaveStrategy strategy = createStrategy(_regler._saveModus);
    try {
        strategy.execute(DataSaver.this, _data);
    } catch (SignalMissingException exc) {
        handleSaveError(exc);
    } finally {
        cleanup();
    }
}
```

#### 2.2 Refactor `doFullSave()`
**Before**: 50 lines with mixed concerns  
**After**: Extract smaller methods

```java
private void doFullSave(AbstractDataContainer data) {
    if (shouldSaveTransposed()) {
        saveTransposedData();
        return;
    }
    saveNormalData(data);
}

private void saveNormalData(AbstractDataContainer data) {
    int maxIndex = data.getMaximumTimeIndex(0);
    int skipPoints = _regler._skipDataPoints.getValue();
    
    DataRange range = createSaveRange(_lastSavedDataIndex + 1, maxIndex, skipPoints);
    for (int line : range) {
        if (shouldAbort()) return;
        saveLine(line);
        updateProgressIfNeeded(line, maxIndex);
    }
    _lastSavedDataIndex = maxIndex;
}

private void saveLine(int lineNumber) throws IOException {
    _linePrinter.printLine(lineNumber);
}

private void updateProgressIfNeeded(int current, int total) {
    _progressTracker.updateProgress(current, total);
}
```

#### 2.3 Refactor Signal Validation
Move to `SignalValidator` class with clear method names:

```java
private void compareAndCorrectSignalNamesIndices() {
    ValidationResult result = _signalValidator.validateSignals(
        _settings.getSelectedNames(),
        _settings.getSelectedSignalIndices(),
        _data
    );
    
    if (!result.isValid()) {
        throw new SignalMissingException(result.getErrorMessage());
    }
}
```

### Phase 3: Improve Testability

#### 3.1 Extract Interfaces
- `DataPrinter` interface for line printers
- `FileManager` interface for file operations
- `ProgressListener` interface for progress updates

#### 3.2 Dependency Injection
Add constructor injection for better testability:
```java
public DataSaver(
    AbstractDataContainer data, 
    ReglerSaveData regler,
    FileNameGenerator fileNameGenerator,
    SignalValidator signalValidator,
    SaveProgressTracker progressTracker) {
    // ...
}
```

### Phase 4: Constants and Configuration

Extract magic numbers to named constants:
```java
private static final class Constants {
    static final int SLEEP_TIMER_MS = 200;
    static final double PERCENT_MULTIPLIER = 100.0;
    static final int MAX_FILE_COUNTER = 1000;
    static final int PROGRESS_SLEEP_MS = 1;
}
```

## Test Strategy

### Unit Tests (85%+ coverage target)

#### Test File Structure
```
src/test/java/ch/technokrat/gecko/geckocircuits/control/
├── DataSaverTest.java (main class)
├── FileNameGeneratorTest.java
├── SignalValidatorTest.java  
├── SaveProgressTrackerTest.java
├── TxtLinePrinterTest.java
├── BinaryLinePrinterTest.java
└── strategies/
    ├── SimulationEndSaveStrategyTest.java
    ├── DuringSimulationSaveStrategyTest.java
    └── ManualSaveStrategyTest.java
```

### Test Cases

#### DataSaverTest.java
- [x] testDoManualSave_success
- [x] testDoManualSaveBlocking_whenRunning_throwsException
- [x] testDoManualSaveBlocking_whenManualMode_saves
- [x] testAbortSave_setsAbortSignal
- [x] testUpdate_simulationEnd_startsSave
- [x] testUpdate_simulationEnd_incrementsCounter
- [x] testInitSettings_simulationEnd_addsObserver
- [x] testInitSettings_duringSimulation_startsThread
- [x] testInitSettings_manual_doesNothing

#### FileNameGeneratorTest.java
- [x] testFindFreeFileName_fileDoesNotExist_returnsSame
- [x] testFindFreeFileName_fileExists_addsNumber
- [x] testFindFreeFileName_multipleExist_findsNextFree
- [x] testFindFreeFileName_noExtension_handlesCorrectly
- [x] testFindFreeFileName_maxCounter_returnsOriginal
- [x] testGenerateNumberedFileName_correctFormat
- [x] testParseFileName_withExtension
- [x] testParseFileName_withoutExtension

#### SignalValidatorTest.java
- [x] testValidateSignals_allValid_success
- [x] testValidateSignals_wrongIndex_corrects
- [x] testValidateSignals_signalMissing_throwsException
- [x] testValidateSignals_multipleSignals_handlesCorrectly
- [x] testFindSignalIndexByName_found
- [x] testFindSignalIndexByName_notFound_returnsNegative

#### SaveProgressTrackerTest.java
- [x] testUpdateProgress_calculatesPercentage
- [x] testUpdateProgress_notifiesObservers
- [x] testGetPercentage_returnsCorrectValue
- [x] testReset_clearsProgress

#### TxtLinePrinterTest.java
- [x] testPrintLine_correctFormat
- [x] testPrintLine_multipleSignals
- [x] testPrintHeader_correctFormat
- [x] testPrintTransposedData_correctFormat
- [x] testGetFormatter_smallNumber_usesSci
- [x] testGetFormatter_largeNumber_usesSci
- [x] testGetFormatter_normalNumber_usesDec
- [x] testCloseStream_closesWriter

#### BinaryLinePrinterTest.java
- [x] testPrintLine_writesCorrectBytes
- [x] testPrintTransposedData_correctFormat
- [x] testCloseStream_closesStream

### Integration Tests

#### DataSaverIntegrationTest.java
- [x] testFullSaveWorkflow_textFormat
- [x] testFullSaveWorkflow_binaryFormat
- [x] testDuringSimulationSave_multipleWrites
- [x] testSimulationEndSave_completeSave
- [x] testFileNumbering_multipleFiles
- [x] testTransposedData_correctOutput
- [x] testSignalRename_autoCorrects
- [x] testAbortDuringSave_stopsProperly

## Implementation Order

### Sprint 1: Infrastructure (Days 1-2)
1. Create test infrastructure and mock objects
2. Create `FileNameGeneratorTest.java` and `FileNameGenerator.java`
3. Create `SignalValidatorTest.java` and `SignalValidator.java`
4. Create `SaveProgressTrackerTest.java` and `SaveProgressTracker.java`

### Sprint 2: Strategy Pattern (Days 3-4)
1. Create `SaveStrategy` interface
2. Implement strategy classes with tests
3. Refactor `SaveRunnable.run()` to use strategies
4. Update tests for `DataSaver`

### Sprint 3: Method Extraction (Days 5-6)
1. Refactor `doFullSave()` into smaller methods
2. Add tests for new methods
3. Refactor `AbstractLinePrinter` validation logic
4. Update integration tests

### Sprint 4: Final Cleanup (Day 7)
1. Extract constants and configuration
2. Add dependency injection support
3. Run full test suite
4. Verify 85%+ coverage
5. Documentation updates

## Success Criteria

- [ ] All tests passing (100%)
- [ ] Test coverage ≥ 85%
- [ ] No methods > 30 lines
- [ ] No cyclomatic complexity > 10
- [ ] All magic numbers extracted to constants
- [ ] Clear separation of concerns
- [ ] Thread-safe implementation verified

## Risk Mitigation

1. **Backward Compatibility**: Keep original public API intact
2. **Thread Safety**: Careful review of concurrent operations
3. **File I/O**: Comprehensive error handling tests
4. **Performance**: Benchmark before/after to ensure no regression

## Files to Create

### New Production Files
1. `/src/main/java/ch/technokrat/gecko/geckocircuits/control/FileNameGenerator.java`
2. `/src/main/java/ch/technokrat/gecko/geckocircuits/control/SignalValidator.java`
3. `/src/main/java/ch/technokrat/gecko/geckocircuits/control/SaveProgressTracker.java`
4. `/src/main/java/ch/technokrat/gecko/geckocircuits/control/SaveStrategy.java`
5. `/src/main/java/ch/technokrat/gecko/geckocircuits/control/SimulationEndSaveStrategy.java`
6. `/src/main/java/ch/technokrat/gecko/geckocircuits/control/DuringSimulationSaveStrategy.java`
7. `/src/main/java/ch/technokrat/gecko/geckocircuits/control/ManualSaveStrategy.java`

### New Test Files
1. `/src/test/java/ch/technokrat/gecko/geckocircuits/control/DataSaverTest.java`
2. `/src/test/java/ch/technokrat/gecko/geckocircuits/control/FileNameGeneratorTest.java`
3. `/src/test/java/ch/technokrat/gecko/geckocircuits/control/SignalValidatorTest.java`
4. `/src/test/java/ch/technokrat/gecko/geckocircuits/control/SaveProgressTrackerTest.java`
5. `/src/test/java/ch/technokrat/gecko/geckocircuits/control/TxtLinePrinterTest.java`
6. `/src/test/java/ch/technokrat/gecko/geckocircuits/control/BinaryLinePrinterTest.java`
7. `/src/test/java/ch/technokrat/gecko/geckocircuits/control/DataSaverIntegrationTest.java`

## Metrics

| Metric | Before | After |
|--------|--------|-------|
| Lines in longest method | 75 | <30 |
| Cyclomatic complexity (max) | ~15 | <10 |
| Test coverage | 0% | 85%+ |
| Number of classes | 1 (+inner) | 8 |
| Number of test classes | 0 | 7 |
| Total test count | 0 | 60+ |
