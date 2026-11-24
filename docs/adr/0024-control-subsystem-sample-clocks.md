# Run CONTROL subsystems on their own sample clocks

ADR-0024 · Claude Opus 5.5 for tinix84 · 2026-11-24 · Status: Proposed · Version 1.3
Audience: maintainers of the CONTROL runtime in `gecko-simulation-core`.

**Decision:** A run request gives a sample period per subcircuit sheet path. The runtime runs the due blocks of all CONTROL subsystems in one dependency order. Each CONTROL subsystem has its own controller step and absolute sample clock.

## Context

Issue [#111](https://github.com/tinix84/GeckoCIRCUITS/issues/111) asks for separate sample periods for CONTROL subsystems, such as a fast current loop and a slow voltage loop.
Issue #61 added one global period, which blocks outside every CONTROL subsystem must keep.
CONTROL blocks run in one dependency order ([ADR-0003](0003-control-dependency-ordering.md)), and delay histories follow the controller step ([ADR-0005](0005-shared-control-delay-history.md)).
Subcircuit sheets already group blocks in saved circuits, but legacy sheet names are not unique [C, High].
Gates and signal-controlled sources read producer arrays directly, so they bypass a calculator hold [C, High].

## Options

| Option | Pros | Cons | Cost / effort |
|---|---|---|---|
| **Sheet path in the run request, one filtered order** | Existing identity. File meaning unchanged. One order for all rates. | Users must place a loop on its own sheet. | Medium |
| Block-name lists in the request | Works on flat circuits | New grouping concept. Arbitrary rate boundaries. | Medium |
| Period token on the sheet block | Saved with the circuit | The legacy fallback ignores it, so backends differ | Medium |
| One netlist per subsystem | Simple per-subsystem state | A subsystem order may not exist for cross-subsystem signals | High |

## Decision and rationale

`SolverFeatures.controlSubsystemPeriods` maps a sheet path to a period in seconds, finite and not negative.
A path joins the segments from the root with `/`. A native sheet contributes its occurrence instance and a legacy sheet its saved name. Segments compare without regard to case ([ADR-0022](0022-native-asc-hierarchy.md)).
A block takes the period of its nearest configured enclosing sheet, found by walking sheet identifiers. Otherwise it takes the global period.
Nested keys are allowed. With `outer` and `outer/inner` configured, a block below `outer/inner` takes the second period.
Each CONTROL subsystem runs at the first grid step at or after each multiple of its period. It uses the absolute clock and the step `max(period, dt)`. A period of zero runs it at every grid step.
Each CONTROL subsystem keeps the #61 clock arithmetic, including its summed grid time and its due-sample slack. This ADR adds no clock algorithm.
At a grid step the runtime runs the due blocks in the global dependency order, with the ADR-0003 publication and capture phases. Delays and dependency planning use the step of their own subsystem.
A block output changes only when its subsystem runs, so faster consumers read a held value.
A gate publishes at a sample of its own subsystem. A signal-controlled source, electrical or thermal ([ADR-0023](0023-core-thermal-network.md)), publishes after a sample of its producer's subsystem. Probes stay live.
A gate applies its published value to the switch, not its live input, in two cases. The gate belongs to a configured subsystem. Or subsystems are configured and the default group has a positive period. Without configured subsystems, a gate keeps the #61 behaviour. A default group with period zero reads its live input.
A step change restages only subsystems whose controller step changes. Adaptive and event sub-steps never run CONTROL ([ADR-0018](0018-adaptive-electrical-step.md)).
The periodic steady-state search needs a period that is a whole multiple of every positive period. It also compares each subsystem's sample phase, flag and held gate values.
Preparation rejects these cases at `controlSubsystemPeriods.<path>`:

- an invalid key or period
- an unknown path
- a path that matches more than one sheet
- two spellings of one sheet
- a sheet without CONTROL blocks

It rejects these cases at `controlSubsystemPeriods`:

- a parent sheet chain that is missing or circular
- an instantaneous cycle at the controller steps

An empty map keeps the single-clock path of #61 unchanged, including its checks.

## Consequences

- Fast and slow loops run in one circuit without extra blocks [C, High].
- Users must place each loop on its own sheet. No tracked example circuit uses sheets [M, High].
- Each CONTROL subsystem exports its sample phase and flag with the #61 arithmetic. A test compares them bit for bit with a global run of the same period [C, High].
- On one flat probe with a global period, an empty map gives the same rows and steady-state report as the #61 baseline [M, High].
- The roundoff of the summed grid time, inherited from #61, makes samples fire one grid step late after about 10⁴ steps. On the baseline probe at `dt` of 1 µs and a period of 10 µs, the first late sample is at step 10151 [M, High].
- The periodic steady-state search is verified on a decaying circuit that two CONTROL subsystems drive [C, High].
- Where nothing decays, the phase residual is this drift of a few ulps of time per period, and it does not contract. A pure CONTROL circuit may not converge within `maxPeriods`, and the search promises nothing there [M, Medium].
- A rate boundary adds no delay at a common sample instant. Algebraic loops across rates stay rejected [C, High].
- A native sheet cannot hold CONTROL blocks, and a transient run rejects every native hierarchy document. A native path resolves in the plan only [C, High].
- REST, the desktop and the circuit file do not carry the periods yet [C, High].
- The #61 global period alone still checks cycles at the electrical step only [C, High].
- Upgrades: a fix for the inherited clock roundoff, REST fields for the periods, rate-transition delays, and a definition-wide key once native sheets can hold CONTROL [E, Low].

## Status

Proposed by Claude Opus 5.5 (design pass for tinix84) on 2026-11-24. No human approval is claimed.

Links: [ControlRuntime](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ControlRuntime.java) · [ControlSubsystems](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ControlSubsystems.java) · [ADR-0017](0017-periodic-steady-state.md)
