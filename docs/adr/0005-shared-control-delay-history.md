# Shared CONTROL delay history

Date: 2026-11-08

## Context

Desktop and core delay calculators expose different calculator types and lifecycle interfaces.
Their copied ring-buffer implementations can give different results after restart or a time-step change.
Physical buffer slots do not identify chronological samples after the write position rotates.
Interpolation must distinguish recorded samples from unavailable history.

## Decision

1. Give one GUI-free core component ownership of delay history, configuration activation, and resampling.
2. Keep the existing desktop and core calculator classes as signal and lifecycle adapters.
3. Clear the history and published output on a fresh start, preserving the output array identity.
4. Preserve history during pause and continuation. Activate pending parameter changes on explicit lifecycle callbacks.
5. Resample chronological samples onto the new grid during a time-step callback without advancing the simulation.
6. Retain the sample preceding the oldest ring entry for interpolation.
7. Return zero before the oldest available sample. Do not interpolate unavailable history into the beginning of a run.
8. Record recent inputs during pass-through operation so a later buffered delay can use them.
9. Reject invalid configuration, invalid time steps, and unrepresentable array sizes before changing existing state.
10. Preserve the core runtime's separate publication and capture phases for buffered feedback.

## Why

One history owner makes corrections apply to both runtime adapters.
Chronological resampling preserves known signal values across buffer rotation and explicit time-step changes.
Availability tracking prevents a nonzero value from appearing before the first recorded input.
Restart and continuation need different state transitions.
Keeping the adapters preserves existing callers and the desktop parameter listener.

## Options considered

| Choice | Benefit | Cost |
|---|---|---|
| Correct both copied implementations | Keeps each calculator self-contained. | Leaves two owners of chronology and lifecycle rules. |
| Delegate desktop behavior to the entire core calculator | Reuses core behavior. | Couples separate signal-array and calculator interfaces. |
| Share a core history component | Gives chronology and resampling one owner. | Keeps small adapter methods for each runtime. |

## Consequences

Callers initialize a delay through a start or time-step callback before executing it.
Callers invoke the time-step callback before continuing on a changed sample grid.
Existing whole-sample delay quantization remains. Delays shorter than the step use direct feedthrough.
Increasing the delay cannot recover samples that were not retained. Those older positions are zero.
Runtime scheduling and signal wiring remain adapter responsibilities.
The core buffered-feedback phases retain their existing public methods.
This decision does not add serialized DELAY support to the headless factory.

## References

- [Shared history](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/control/DelayHistory.java)
- [Paired desktop/core contracts](../../src/test/java/gecko/geckocircuits/control/calculators/DelayParityContractTest.java)
- [Core lifecycle contracts](../../src/modules/gecko-simulation-core/src/test/java/gecko/core/control/DelayHistoryLifecycleContractTest.java)
- [CONTROL ordering](0003-control-dependency-ordering.md)
