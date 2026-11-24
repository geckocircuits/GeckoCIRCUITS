# Locate diode switching inside a step with electrical sub-steps

ADR-0016 · Claude Opus 5.5 for tinix84 · 2026-11-24 · Status: Accepted · Version 1.0
Audience: maintainers who implement or review solver features in `gecko-simulation-core`.

**Decision:** We locate diode-family switching events inside a fixed step by sub-stepping the uncommitted electrical interval to the crossing time. CONTROL and recordings stay on the fixed grid.

## Context

Issue [#58](https://github.com/tinix84/GeckoCIRCUITS/issues/58) asks to locate switching events inside a step, with the feature off by default and bit-identical when off.
A session step applies held gates, solves and commits the electrical step, then runs CONTROL and records one row.
The diode iteration selects the end-of-step state, so a transition time is known only to one step.
The core has no CONTROL state snapshot, `SignalCalculatorRandom` uses the global `Math.random()` stream, and the caller supplies external inputs.
Gear-Shichman (GS) stamps read two history points and assume a uniform step.

## Options

| Option | Pros | Cons | Cost / effort |
|---|---|---|---|
| Snapshot and restore electrical state, CONTROL and recordings | Can relocate any event, also CONTROL thresholds | No calculator state contract. The random stream cannot be restored. External inputs cannot be repeated. | High |
| Replay a trial session from time zero | Uses existing code | Runtime grows with the square of the run length. The random stream differs. | Medium effort, high runtime |
| **Electrical sub-step before the commit** | Rollback is trial scratch only. Reuses `ZeroCrossingDetector`. CONTROL and recordings stay unchanged. | Locates diode-family events only. Gate edges stay on the grid. | Medium |
| Extra recording row at the crossing time | The event shows in the waveform | Variable recording length. Scope rows mix two times. Changes the probe values that sources read. | Medium |

## Decision and rationale

Only `LK_D`, `LK_THYR` and `LK_IGBT` conduction changes emit events. A sign change of `g = vX - vY - Uf` detects them. The diode iteration compares the same quantity [C, High].

CONTROL blocks, `LK_S`, `LK_MOSFET`, source-limit changes and gate edges emit no events. A gate edge is a rising gate of a thyristor or IGBT, or a removed IGBT gate. CONTROL is discrete-time and holds its outputs, so a gate edge has no time inside a step [C, High].
A thyristor or IGBT with a gate that stays high switches by voltage, so the solver locates it.

The split happens before the electrical commit and before CONTROL runs. A rollback therefore restores only trial scratch: diode-family parameter 0, `LK_U` parameter 14, and a forced matrix rebuild [C, High, from the step order in the code].
Each sub-step commit owns the solver histories. Only the grid commit publishes results, step traces and probe values.

Every diode-family state change that a committed sub-step holds is either a located event or an unlocated count. A gate edge is in neither group.
A located event that the next sub-step reverses leaves the log and counts as unlocated. Devices that cross together, such as two series devices of a bridge, switch at the same sub-step.

Recordings keep one row per grid step. Located events go to a bounded log of `ZeroCrossingEvent` records. The result reports the located count, the unlocated count, and any log overflow.
The opt-in setting is `zeroCrossing` in `SolverFeatures`. The setting validation rejects GS. When the setting is off, the existing step runs unchanged.

## Consequences

- Diode turn-on and turn-off times resolve inside the step [C, High].
- Gate-driven edges keep the resolution of the grid. The goal of the original request to match the waveforms of an LLC (inductor-inductor-capacitor) resonant converter is not claimed. A CONTROL state snapshot contract would be a design constraint for locating CONTROL crossings [C, High].
- A split step rebuilds the matrix at each sub-step. The cost grows with the number of events [C, Medium].
- Trapezoidal ringing after an event does not change. The ringing switches a device back and forth, and the solver reports those changes as unlocated [C, Medium].

## Status

Proposed by Claude Opus 5.5 (design pass for tinix84) on 2026-11-24. Decided by: Codex implementation review under tinix84 task authorization, 2026-11-24. No human approval is claimed.

Links: [ElectricalTimeStep](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ElectricalTimeStep.java) · [SimulationSession](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/SimulationSession.java) · [ZeroCrossingDetector](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ZeroCrossingDetector.java) · [ADR-0011](0011-numerical-reference-compatibility.md)
