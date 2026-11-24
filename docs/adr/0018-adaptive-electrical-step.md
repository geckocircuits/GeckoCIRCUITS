# Adapt the electrical step between fixed CONTROL grid points

ADR-0018 · Claude Opus 5.5 for tinix84 · 2026-11-24 · Status: Accepted · Version 1.1
Audience: maintainers of `gecko-simulation-core`.

**Decision:** We adapt only the electrical sub-step width between fixed grid points, with a Backward Euler (BE) and trapezoidal (TRZ) companion error estimate, while CONTROL and recordings stay on the configured grid.

## Context

Issue [#59](https://github.com/tinix84/GeckoCIRCUITS/issues/59) asks for steps controlled by the local truncation error (LTE), with the original controller and clamps. It is off by default.
CONTROL depends on its step: an unstaged change resamples delay histories, turns a delay into a feedthrough when the step exceeds its delay time, and changes carrier, derivative (PD) and random outputs [C, High].
The recording capacity comes from the fixed step and duration, and [ADR-0016](0016-zero-crossing-event-location.md) rolls back only uncommitted trial scratch.
The `buck_simple` acceptance test needs its PWM CONTROL blocks and the sampled CONTROL of #61.

## Options

| Option | Pros | Cons | Cost / effort |
|---|---|---|---|
| CONTROL at each accepted step, variable-length recording | Largest step growth | Delay resampling and feedthrough changes. Carrier and random output changes. Unbounded recording. | High |
| Steps across grid points, with interpolated rows | Speed-up with a fixed output grid | CONTROL inside a step needs a rollback | High |
| **Grid-anchored electrical sub-steps** | All CONTROL blocks and #61 work unchanged. Fixed recording. Small rollback. | The step never exceeds the grid. Speed-up only against a fixed minimum step. | Medium |
| Reject circuits with CONTROL blocks | Simplest | Fails the `buck_simple` acceptance test | Low |

## Decision and rationale

CONTROL runs only at grid points, with the grid width or the #61 sample period as its step. Adaptation never calls `prepareTimeStepChange` or `initWithNewDt` [C, High].
An explicit `extend` with a new step keeps the staged contract and fails before any change if it cannot stage.
Recordings keep one row per grid step, and the ADR-0016 event log stays bounded.

Each adaptive interval spans the last published grid time to the new one.
After an `extend` or the warmup-to-main change, the session keeps its next timestamp, so the gap can differ from the step.
The new step stays nominal for CONTROL, the settings and the width clamps. Histories and CONTROL are not reset [C, High].

The companion solve uses the other one-step method, from the same committed history and with the converged topology. It runs no CONTROL and draws no random numbers.
`|x_TRZ - x_BE|` estimates the BE local error, so the estimate is not divided by 3. For TRZ it is an upper bound while `h |x'''| < 6 |x''|` [C, Medium].
The controller keeps `h (tol / lte)^(1/2) 0.9`, clamped to `[dt_min, dt_max]` then to the next grid point.

A rejected trial rolls back with the ADR-0016 set. A forced step at `dt_min` is counted and reported.
The rejected count holds LTE rejections only, not discarded event or refinement trials.
The first sub-step after a topology change uses BE. Setting validation rejects GS.
Periodic steady state checks the physical electrical state, not the numerical controller step, restart flag, margin trend or statistics [C, Medium].

## Consequences

- Accuracy improves against the analytic RC solution [C, High].
- The step count falls only against a fixed `dt_min` [C, Medium].
- Each new `h` costs two LU factorizations. No speed-up is claimed [E, Medium].
- With BE, `tol` bounds the local error, not the global error [C, High].
- Upgrades: an order-consistent TRZ estimator, steps across grid points after a CONTROL snapshot contract [E, Low].

## Status

Proposed by Claude Opus 5.5 (design pass for tinix84) on 2026-11-24. Accepted by root review on 2026-11-24. Amended 2026-11-24 by Claude Sonnet 5.5 for the elapsed interval, pending root review. No human approval is claimed. Depends on [#58](https://github.com/tinix84/GeckoCIRCUITS/issues/58).

Links: [ElectricalTimeStep](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ElectricalTimeStep.java) · [ControlNetlist](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ControlNetlist.java) · [ControlRuntime](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ControlRuntime.java) · [ADR-0005](0005-shared-control-delay-history.md) · [ADR-0011](0011-numerical-reference-compatibility.md)
