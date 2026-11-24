# Run the thermal network in the core once per grid step

ADR-0023 · Claude Opus 5.5 for tinix84 · 2026-11-24 · Status: Proposed · Version 1.0
Audience: maintainers of `gecko-simulation-core` and reviewers of desktop backend routing.

**Decision:** The core solves the saved THERMAL network as a second MNA network with the electrical step code, once per grid step between the electrical commit and CONTROL, with legacy loss algorithms and lags.

## Context

Issue [#95](https://github.com/tinix84/GeckoCIRCUITS/issues/95) asks the core to run saved thermal circuits, with a junction temperature within 1 % of the legacy kernel.
Thermal circuits run on the legacy kernel when the core rejects their elements ([ADR-0010](0010-pull-based-frontend-single-owner-session.md) decision 7).
The legacy kernel stamps thermal elements as aliases of R, C, I and U in a second matrix and solves it after the electrical step and before CONTROL (`SimulationsKern.simulateOneTimeStep`, `LKMatrices`) [C, High].
A loss source computes its heat from the coupled component's current and voltage of this step and the junction temperature of the previous thermal step. The next thermal step stamps that heat (`ThermPvChip.doCalculation`) [C, High].
The core adds electrical sub-steps ([ADR-0016](0016-zero-crossing-event-location.md), [ADR-0018](0018-adaptive-electrical-step.md)) that the legacy kernel does not have.

## Options

| Option | Pros | Cons | Cost / effort |
|---|---|---|---|
| Separate thermal solver | Independent of electrical code | Duplicates stamps, initial conditions and currents | High |
| One merged electrical and thermal matrix | One solve | Changes electrical numerics and default results. Couples thermal to sub-steps. | High |
| **Second network on the electrical step code, once per grid step** | Reuses stamps, initial conditions, step changes and currents. Trials never touch thermal state. | Loss sampling has grid resolution when sub-steps are on | Medium |
| Second network per accepted sub-step | Loss sampling at sub-step resolution | Trial rollback must cover thermal and loss state. No legacy reference. | High |

## Decision and rationale

The thermal netlist maps `TH_RTH` and `TH_AMBIENT` to a resistor, `TH_CTH` to a capacitor, `TH_FLOW` to a current source and `TH_TEMP` to a voltage source, like `LKMatrices` [C, High].
A loss source expands to a signal-controlled heat source and a 10 kΩ resistor from the thermal zero, like `ThermPvChip` [C, High]. All thermal-zero terminals form one node.
Thermal uses the circuit solver type, the dense LU and the saved initial conditions, also when the electrical network starts from its DC operating point.

Each grid step solves the thermal network once, with the grid step width, after the electrical results publish. It then updates every loss source for the next step. Temperature and heat-flow probes read this step's values before CONTROL runs.
Electrical sub-steps, trials and rejections stay inside the electrical step, so thermal state changes only at a committed step [C, High].
A step change rebuilds the thermal matrix, like the legacy continuation [C, High].

Loss sources port the legacy SIMPLE, DETAILED and resistor algorithms unchanged, including the initial history of -1 and the current threshold of 0.01 A. A contract test compares them with the legacy classes.

Readiness accepts thermal circuits that the core can execute. It reports a capability diagnostic, and the desktop runs the legacy kernel, for a power module element, a loss source on a MOSFET, a DETAILED model without its loss file, an unresolved loss coupling and unsupported source modes.
The periodic steady-state search and AC analysis reject thermal elements: the thermal time constant spans many thousand periods, so the period-drift test cannot accept in a practical run time [E, Medium].

## Consequences

- Thermal circuits that pass readiness run in the core on desktop, REST and scripts [C, High].
- With event location or the adaptive step on, losses sample the grid, not the sub-steps. The electrical results still improve [C, Medium].
- Heat lags the electrical step by one grid step, as in legacy [C, High].
- Upgrades: per-sub-step loss integration, a shared legacy and core loss class, MOSFET loss coupling with its anti-parallel diode, periodic steady state with a slow thermal state [E, Low].

## Status

Proposed by Claude Opus 5.5 (design pass for tinix84) on 2026-11-24. No human approval is claimed.

Links: [ElectricalTimeStep](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ElectricalTimeStep.java) · [SimulationSession](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/SimulationSession.java) · [ADR-0011](0011-numerical-reference-compatibility.md) · [ADR-0017](0017-periodic-steady-state.md)
