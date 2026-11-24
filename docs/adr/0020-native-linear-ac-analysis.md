# Solve linear AC on the native circuit graph

ADR-0020 · Codex · 2026-11-24 · Version 1.0

Audience — Core and desktop maintainers

## Decision

We use complex modified nodal analysis (MNA) in the Java core to solve linear alternating current (AC) circuits from native, editable components.

## Context

The imported proportional-integral-derivative circuit contains resistors, capacitors, and independent voltage sources. Its controlled voltage sources use scalar or Laplace gain. Its ideal derivative has an improper transfer function. The native circuit file must retain the topology and laws through desktop edits and save and reopen. [Issue #48](https://github.com/tinix84/GeckoCIRCUITS/issues/48) owns the requirements and acceptance criteria.

## Options

| Option | Pros | Cons | Effort |
|---|---|---|---|
| Native four-terminal source with frequency-domain stamping | Preserves editable gain, pin polarity, and branch current identity | Requires AC preparation and a native source component | Medium |
| Expand transfer functions into state-space components | Can represent proper causal dynamics in time | The ideal derivative needs additional equations. Expansion obscures imported branch identity. | High |
| Delegate execution to LTspice | Reuses its solver | Requires an external executable and a translated circuit at runtime | Medium |

## Decision and rationale

The following paragraphs specify the implementation required by this decision.

The editor document in `SchematicEditor2` owns the analysis settings and scalar parameter definitions. Each live component owns its active value law and source instance identity. A voltage source owns its AC phasor independently of its time-domain values. The desktop uses `ProjectData.exportASCII` and `ProjectData.importASCII` through `MainWindow`. These methods share a versioned codec with the core `CircuitFileWriter` and `CircuitFileParser`. `EditorCircuitCapture` takes the current document settings, component laws, and live terminal geometry into the circuit snapshot.

An edit replaces the active law, and compatibility values derive from that law.

One terminal descriptor shares original `SpiceOrder` across import placement and desktop geometry. The controlled source has output positive, output negative, sense positive, and sense negative terminals in that order. `CircuitTopology` derives electrical groups from live wires, pins, junctions, intentional electrical labels, and ground. Source instance identity and terminal order identify group membership for comparison. Trace aliases serve display only.

`CircuitPreparation` dispatches by analysis before transient normalization and validation. The `SpiceValue` grammar uses one abstract syntax tree for real scalar evaluation and complex gain evaluation. Only transfer expressions reserve the Laplace variable `s`, evaluated at `jω` for each nonzero frequency.

Complex MNA keeps signed voltage-source branch currents as unknowns. The core solves the equivalent real block with double-precision, pivoted lower–upper (LU) factorization. This reuses the core `Matrix` and `LUDecomposition`. It rejects singular or nonfinite solutions and checks the scaled backward residual against the original equations.

## Consequences

- Legacy files without an analysis declaration use the transient default. Unsupported explicit analyses and reachable component laws fail with a diagnostic. Invalid versioned fields and transient-only overrides on an AC run also fail.
- AC admits improper gains where they are finite. The Laplace gain is AC-only and its time-domain execution is explicitly unsupported.
- The candidate identifier `(LK, 53)` requires factory and enum collision checks, plus a mixed-sheet round trip with the existing CONTROL counter at `(CONTROL, 53)`.
- Execution uses a standalone native IPES circuit file and the Gecko runtime as its only inputs. Native LTspice netlists and RAW results belong only to the external comparator. The solve derives its results from live components. Embedded source netlists and recorded results cannot supply execution.
- Nonlinear bias linearization, full archive dependency resolution, and qualification of alternatives for opaque vendor models require separate decisions and evidence.

## Status

Accepted by the project maintainer on 2026-11-24.
