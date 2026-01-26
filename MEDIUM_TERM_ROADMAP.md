# Medium-Term Roadmap (1-2 Weeks)

**Target**: Continue test coverage improvements in control package toward 70%+ across 15+ classes

## Strategic Overview

The URGENT and SHORT-TERM phases have established:
1. ✅ Proven refactoring patterns (FileNameGenerator, SignalValidator)
2. ✅ Integration test approaches (11 + 13 comprehensive tests)
3. ✅ Documentation and guidance (patterns guide)
4. ✅ Zero regressions with 696 passing tests

This roadmap outlines the next phase to extend these patterns to additional control block classes.

## Priority 1: Pure Calculation Class Testing

### 1.1 DivCalculator Enhancement
**Current Status**: Basic coverage exists
**Goal**: Expand to edge case coverage
**Scope**:
- Extend existing tests to 80%+ coverage
- Add division-by-zero edge cases
- Test boundary conditions (max/min values)
- Test overflow scenarios

**Estimated Effort**: 4-6 hours
**Success Criteria**: 80%+ coverage, 20+ tests

### 1.2 IntegratorCalculation Enhancement  
**Current Status**: Basic coverage exists
**Goal**: Expand to real-world scenarios
**Scope**:
- Test numerical stability
- Add reset/state transition tests
- Test with varying time steps
- Test accumulation edge cases

**Estimated Effort**: 4-6 hours
**Success Criteria**: 80%+ coverage, 20+ tests

## Priority 2: New Helper Class Extraction

### 2.1 Extract ReglerLimit Calculation Logic
**Source Class**: `ReglerLimit.java`
**Size**: ~150 lines
**Target**: Extract ~60-line utility class

**Steps**:
1. Analyze current implementation
2. Identify pure calculation logic
3. Extract to new `LimitCalculator` class
4. Create 15+ unit tests
5. Create integration tests
6. Update documentation

**Expected Outcome**:
- New class: `LimitCalculator` (60 lines, 80%+ coverage)
- Tests: 15 unit + 3 integration
- Coverage increase: ~150 instructions

**Estimated Effort**: 6-8 hours

### 2.2 Extract ReglerIntegrator Calculation Logic
**Source Class**: `ReglerIntegrator.java`
**Size**: ~119 lines
**Target**: Extract ~70-line utility class

**Steps**:
1. Analyze current implementation
2. Identify integration logic separate from UI
3. Extract to new `IntegrationCalculator` class
4. Create 15+ unit tests
5. Create integration tests
6. Update documentation

**Expected Outcome**:
- New class: `IntegrationCalculator` (70 lines, 80%+ coverage)
- Tests: 15 unit + 3 integration
- Coverage increase: ~200 instructions

**Estimated Effort**: 6-8 hours

## Priority 3: Integration Test Patterns

### 3.1 Extended Integration Testing
**Goal**: Create comprehensive integration tests for all extracted classes
**Scope**:
- Multiple instance consistency tests
- Cross-component interaction tests
- Real-world scenario simulations
- Robustness and edge case combinations

**Estimated Effort**: 4-6 hours
**Success Criteria**: 20+ integration tests, 100% pass rate

### 3.2 Refactoring Safety Validation
**Goal**: Ensure all extractions maintain backward compatibility
**Scope**:
- Original class behavior unchanged
- API compatibility verification
- Performance regression tests
- State migration tests (if applicable)

**Estimated Effort**: 3-4 hours
**Success Criteria**: Zero regressions, backward compat verified

## Priority 4: Documentation & Guidance

### 4.1 Pattern Application Guide
**Goal**: Document lessons learned from extractions
**Content**:
- Common challenges and solutions
- Testing strategies for extracted classes
- Integration test best practices
- Anti-patterns and pitfalls
- Quick reference checklist

**Estimated Effort**: 2-3 hours

### 4.2 Coverage Milestone Documentation
**Goal**: Track progress toward 85% goal
**Content**:
- Class-by-class coverage metrics
- Progress timeline
- Remaining work estimates
- Priority ranking for next extractions

**Estimated Effort**: 1-2 hours

## Detailed Work Breakdown

### Week 1: Core Extraction Work
```
Day 1: DivCalculator & IntegratorCalculation Enhancement
├─ DivCalculator: 4 hours (edge case tests)
├─ IntegratorCalculation: 4 hours (scenario tests)
└─ Testing & validation: 2 hours

Day 2: ReglerLimit Extraction
├─ Analysis & design: 2 hours
├─ Implementation: 3 hours
├─ Unit tests: 2 hours
├─ Integration tests: 1 hour
└─ Validation: 1 hour

Day 3: ReglerIntegrator Extraction
├─ Analysis & design: 2 hours
├─ Implementation: 3 hours
├─ Unit tests: 2 hours
├─ Integration tests: 1 hour
└─ Validation: 1 hour
```

### Week 2: Testing & Documentation
```
Day 1: Extended Integration Testing
├─ Comprehensive integration tests: 4 hours
├─ Cross-component validation: 2 hours
└─ Regression testing: 2 hours

Day 2: Documentation
├─ Pattern application guide: 3 hours
├─ Coverage metrics: 1 hour
├─ Session summary: 1 hour
└─ Review & cleanup: 1 hour
```

## Success Metrics

### Coverage Goals
- **DivCalculator**: 85%+ coverage
- **IntegratorCalculation**: 85%+ coverage
- **LimitCalculator** (new): 80%+ coverage
- **IntegrationCalculator** (new): 80%+ coverage
- **Control Package**: 10-15% (up from 3%)

### Test Count Goals
- Additional unit tests: 60+ tests
- Additional integration tests: 15+ tests
- **Total Project Tests**: 750+ (from 696)
- **Pass Rate**: 100%

### Quality Goals
- **Regressions**: 0
- **Code Review**: All extractions reviewed
- **Documentation**: Complete and reviewed
- **Patterns**: Validated and documented

## Dependencies & Blockers

### Prerequisites
- ✅ Existing test infrastructure (DONE)
- ✅ Refactoring patterns established (DONE)
- ✅ Documentation templates created (DONE)

### Known Risks
- **Complexity**: Some classes may have unexpected dependencies
- **Mitigation**: Thorough analysis before extraction
- **Testing**: Complex integration may require more test scenarios
- **Mitigation**: Use proven patterns from FileNameGenerator

### Assumptions
1. ReglerLimit and ReglerIntegrator can be cleanly extracted
2. Pure calculation logic is separable from UI concerns
3. Existing test infrastructure supports new patterns
4. No changes to public APIs required

## Milestones & Checkpoints

### Milestone 1: Enhanced Calculator Classes (Days 1-2)
- ✅ Objective: 80%+ coverage on existing calculator classes
- ✅ Validation: 40+ new tests passing
- ✅ Time: 10 hours

### Milestone 2: New Helper Classes (Days 2-3)
- ✅ Objective: 2 new extracted classes with 80%+ coverage
- ✅ Validation: 30+ new tests passing, zero regressions
- ✅ Time: 12 hours

### Milestone 3: Integration & Documentation (Week 2)
- ✅ Objective: Comprehensive integration tests, complete documentation
- ✅ Validation: 750+ total tests, zero regressions
- ✅ Time: 10 hours

## Decision Points & Alternatives

### Decision 1: Extraction Order
**Options**:
1. (Selected) Extract both ReglerLimit and ReglerIntegrator
2. Extract ReglerLimit only, defer ReglerIntegrator
3. Extract different classes

**Rationale**: Both classes have similar patterns; parallel extraction reduces risk

### Decision 2: Test Coverage Target
**Options**:
1. (Selected) 80%+ coverage for all extractions
2. 70%+ minimum, 85%+ ideal
3. 100% coverage target

**Rationale**: 80% is practical sweet spot for control classes; 100% unnecessary

### Decision 3: Integration Test Scope
**Options**:
1. (Selected) Comprehensive real-world scenarios
2. Basic happy path + critical edge cases
3. Minimal integration coverage

**Rationale**: Real-world scenarios ensure robustness; lessons from DataSaver

## Review Criteria for Completion

### Code Quality
- [ ] No code duplication (DRY principle)
- [ ] Single responsibility per class
- [ ] Clear, documented APIs
- [ ] Consistent with existing patterns

### Test Quality
- [ ] 80%+ coverage for new classes
- [ ] 100% pass rate
- [ ] Zero regressions in existing tests
- [ ] Edge cases covered
- [ ] Integration scenarios tested

### Documentation Quality
- [ ] Design rationale documented
- [ ] Usage examples provided
- [ ] Integration patterns explained
- [ ] Metrics tracked and reported

## Post-Completion Tasks

1. **Coverage Analysis**
   - Generate JaCoCo report
   - Compare with baseline
   - Update progress document

2. **Metrics Update**
   - Update coverage dashboard
   - Track test growth trend
   - Analyze code quality metrics

3. **Planning for Next Phase**
   - Identify additional extraction candidates
   - Plan advanced refactoring patterns
   - Estimate 85% coverage timeline

4. **Archive & Documentation**
   - Create session summary
   - Update project roadmap
   - Document lessons learned

## Related Documentation

- `REFACTORING_PATTERNS_GUIDE.md` - Extraction patterns
- `SESSION_SHORTTERM_SUMMARY_2026-01-26.md` - Current session status
- `COVERAGE_PROGRESS.md` - Coverage timeline
- Test files in `src/test/java/ch/technokrat/gecko/geckocircuits/control/`

---

**Status**: Ready for execution
**Estimated Total Time**: 40-45 hours (5-6 working days)
**Target Completion**: End of week 2
**Expected Outcome**: 750+ tests, 70%+ coverage on 15+ classes
