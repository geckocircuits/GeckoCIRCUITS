# Run the thermal network in the core once per grid step

ADR-0023 · Claude Opus 5.5 for tinix84 · 2026-11-24 · Status: Proposed · Version 1.0
Audience: maintainers of `gecko-simulation-core` and reviewers of desktop backend routing.

**Decision:** The core solves the saved THERMAL network as a second MNA network. It runs once per grid step, between the electrical commit and CONTROL.

## Context

Issue [#95](https://github.com/tinix84/GeckoCIRCUITS/issues/95) asks the core to run a circuit with a thermal network. The junction temperature must stay within 1 % of the legacy kernel.
Until now the core rejected thermal elements, so these circuits ran on the legacy kernel ([ADR-0010](0010-pull-based-frontend-single-owner-session.md) decision 7).
The legacy kernel solves a second thermal matrix after the electrical step and before CONTROL (`SimulationsKern.simulateOneTimeStep`) [C, High].
A loss source heats the next thermal step, not the current one (`ThermPvChip.doCalculation`) [C, High].
The core adds electrical sub-steps ([ADR-0016](0016-zero-crossing-event-location.md), [ADR-0018](0018-adaptive-electrical-step.md)). The legacy kernel has none.

## Options

| Option | Pros | Cons | Cost / effort |
|---|---|---|---|
| Separate thermal solver | Independent of electrical code | Duplicates stamps, initial conditions and currents | High |
| One merged electrical and thermal matrix | One solve | Changes electrical numerics and default results. Couples thermal to sub-steps. | High |
| **Second network on the electrical step code, once per grid step** | Reuses stamps, initial conditions, step changes and currents. Trials never touch thermal state. | Loss sampling has grid resolution when sub-steps are on | Medium |
| Second network per accepted sub-step | Loss sampling at sub-step resolution | Trial rollback must cover thermal and loss state. No legacy reference. | High |

## Decision and rationale

The legacy kernel stamps thermal elements as aliases of R, C, I and U (`LKMatrices`) [C, High].
The thermal netlist uses the native `TH_RTH`, `TH_AMBIENT`, `TH_CTH`, `TH_FLOW` and `TH_TEMP` stamps for the same elements.
A loss source expands to a signal-controlled heat flow and a 10 kΩ resistor from the thermal zero, like `ThermPvChip` [C, High].
It computes its heat from the current and voltage of this step. It uses the junction temperature of the previous thermal step [C, High].
All thermal-zero terminals form one node.
Thermal uses the circuit solver type, the dense LU and the saved initial conditions. This holds also when the electrical network starts from its DC operating point.

Each grid step solves the thermal network once, with the grid step width, after the electrical results publish.
It then updates every loss source for the next step.
Electrical sub-steps, trials and rejections stay inside the electrical step. Thermal state changes only at a committed step [C, High].
A step change rebuilds the thermal matrix, like the legacy continuation [C, High].

`DomainCoupler` mirrors the LK↔CONTROL exchange for THERMAL:

- `transferThermalToControlProbes` writes TEMP and FLOW probe outputs after the thermal solve. This happens before CONTROL runs.
- Probes stay live: they update at every committed grid step.
- A TEMP probe reads a node potential difference. A FLOW probe reads a heat flow or one loss component.
- `transferControlToThermalSources` writes signal-controlled `TH_FLOW` and `TH_TEMP` sources from the previous CONTROL outputs.
- Only this source publication follows the sample gate of the LK sources.

Loss sources port the legacy SIMPLE, DETAILED and resistor algorithms. This includes the initial history of -1 and the current threshold of 0.01 A.
A contract test compares them with the legacy classes.

Readiness accepts a circuit with a thermal network that the core can execute.
For the rest it reports a capability diagnostic. The desktop then runs the legacy kernel.
These cases fall back:

- a power module element
- a loss source on a MOSFET
- a DETAILED model without its loss file
- an unresolved or ambiguous loss coupling
- an unsupported source mode
- a TEMP or FLOW probe in a circuit without an active thermal network

The periodic steady-state search and AC analysis reject thermal elements.
The search exports and compares only electrical and CONTROL state. It has no thermal state or loss history to converge.

## Consequences

- Circuits with a thermal network that pass readiness run in the core on desktop, REST and scripts [C, High].
- With event location or the adaptive step on, losses sample the grid, not the sub-steps. The electrical results still improve [C, Medium].
- Heat lags the electrical step by one grid step, as in legacy [C, High].
- Upgrades: per-sub-step loss integration, a shared legacy and core loss class, MOSFET loss coupling with its anti-parallel diode, periodic steady state with a slow thermal state [E, Low].

## Status

Proposed by Claude Opus 5.5 (design pass for tinix84) on 2026-11-24. No human approval is claimed.

Links: [ElectricalTimeStep](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ElectricalTimeStep.java) · [SimulationSession](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/SimulationSession.java) · [ADR-0011](0011-numerical-reference-compatibility.md) · [ADR-0017](0017-periodic-steady-state.md)
