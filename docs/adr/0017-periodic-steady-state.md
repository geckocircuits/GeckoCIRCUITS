# Find periodic steady state by brute-force period iteration

ADR-0017 · Claude Opus 5.5 for tinix84 · 2026-11-24 · Status: Accepted · Version 1.1
Audience: maintainers who implement or review solver features in `gecko-simulation-core` and the REST API.

**Decision:** We find the periodic steady state (PSS) with a brute-force period search before the main phase, seeded from the DC operating point and tested on the full periodic state.

## Context

Issue [#53](https://github.com/tinix84/GeckoCIRCUITS/issues/53) asks for PSS for a period T, with the periods used and a converged flag.
The warmup phase already passes all histories to the main phase without a reset.
CONTROL carriers advance per step and clamp, so their simulated period is a whole number of steps: 30 µs, not 28.57 µs, for 35 kHz at 1 µs [C, High].
The full state holds solver histories, discrete switch states, and private CONTROL state such as delay buffers and carrier phase.

## Options

| Option | Pros | Cons |
|---|---|---|
| Brute force, residual on outputs only | Simplest | Misses hidden CONTROL state. Accepts a slow drift. |
| **Brute force, full-state residual** | A real periodic check. Uses only the session step. | Stateful calculators need a read-only export. |
| Shooting with Newton iteration | Few periods when lightly damped | Needs sensitivities and a CONTROL state restore. |
| Extrapolation of the period sequence | Faster without sensitivities | Unstable with switching. |

## Decision and rationale

The search runs the main step as a warmup variant and records nothing. The main phase starts at the converged boundary with the same clock and state.
The residual covers solver histories, switch and source-limit states (exact), held CONTROL values, exported CONTROL state and the #61 sample phase.
A stateful calculator without an export is rejected, and so is the random source, whose generator state is hidden [C, High].
Each value is scaled inside its own unit class (volt, ampere, CONTROL) with a floor. The estimate `D r / (1 - r)` rejects slow drift, and two consecutive periods must pass [C, High].
At `maxPeriods` the session ends with `converged = false` and no main phase, and the engine reports a failed result. A cancellation stops at the next step.

The residual holds no absolute time, so each absolute-time forcing must repeat in T, which is a whole number of steps and of #61 sample periods [C, High].
- A fixed nonzero sine completes one or more whole cycles in T. A fixed zero frequency is constant.
- An imported table starts at time zero or earlier, and T is a whole multiple of its duration. A constant table repeats in any T.
- PSS rejects a sine with an input frequency, the matrix converter block, and a time block that feeds control logic. A time block that feeds only a scope is valid.

Preparation reports each violation, so REST rejects the request before it accepts a job [C, High].

## Consequences

- Users give the simulated period, not a nominal frequency [C, High].
- Carriers or counters that do not repeat in T fail with a residual report [C, Medium].
- v1 rejects time in control logic, modulated sines and matrix converter blocks, and the issue names the block [C, High].
- Main-phase timestamps and logging start after the search [C, High].
- REST gets `periodicSteadyState` and `steadyState` fields. Batch requests are a follow-up [C, High].
- Period boundaries stay on the grid, so [ADR-0016](0016-zero-crossing-event-location.md) stays compatible [C, Medium].

## Status

Proposed by Claude Opus 5.5 (design pass for tinix84) on 2026-11-24. Accepted by root review on 2026-11-24. No human approval is claimed.
Amended to Version 1.1 by Claude Opus 5.5 on 2026-11-24 after the root audit found a false convergence with an imported table. Root accepted it, with the clarification that a fixed zero-frequency sine is constant.
Depends on [#57](https://github.com/tinix84/GeckoCIRCUITS/issues/57).
