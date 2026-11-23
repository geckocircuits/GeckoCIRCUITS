# Circuit preparation interface

Date: 2026-11-04

## Decision and purpose

One deep core module owns effective circuit configuration and Headless readiness.
The REST adapter resolves source identity and captures input before job admission.
The engine consumes an immutable Prepared circuit and creates independent state for each run.
This interface implements the compatibility and ownership decisions in [ADR-0001](0001-circuit-preparation-admission.md).

## Core interface

The following Java sketch defines the interface contract. It is not an implementation.

```java
public final class CircuitPreparation {
    public CircuitSnapshot capture(CircuitModel source);
    public PreparationResult prepare(
        CircuitSnapshot base,
        PreparationRequest request);
}

public record PreparationRequest(
    SolverOverrides settings,
    Map<String, Double> parameters) {}

public sealed interface PreparationResult {
    ReadinessReport report();

    record Rejected(ReadinessReport report)
        implements PreparationResult {}

    record Inspectable(
        CircuitSnapshot circuit,
        ReadinessReport report)
        implements PreparationResult {}

    record Ready(
        CircuitSnapshot circuit,
        PreparedCircuit execution,
        ReadinessReport report)
        implements PreparationResult {}
}
```

`SolverOverrides` contains optional solver, time-step, and duration values.
An absent field inherits the captured circuit value.
A supplied invalid field produces a rejection and does not fall back to a stored default.
The request copies its map and has no nullable collection fields.

`capture` copies all circuit data and preserves parser-owned import evidence.
That evidence includes token presence, explicit saved values, and diagnostics for skipped simulation content.
The programmatic-model adapter supplies canonical values and identifies conflicting input representations.
Neither operation exposes writable model maps, arrays, connections, or solver settings.

`prepare` returns `Rejected` when a requested assignment or explicit setting is invalid.
It publishes no changed snapshot and leaves its input unchanged.
It returns `Inspectable` when the requested assignments are accepted but the resulting circuit fails preflight or uses unsupported behavior.
It returns `Ready` only when preflight passes and an execution description exists.
All outcomes carry diagnostics, including independent errors found in the same request.
Checks that depend on unavailable information must not invent a passing result.

Effective snapshots retain normalization findings for malformed saved values, unresolved optimizer bindings, and conflicting input representations.
Preparing that snapshot again preserves those findings. Snapshot inspection and recapture also preserve the evidence.
A valid assignment to the same logical field clears its normalization finding after the complete override set passes validation.
Named and indexed aliases identify that same field. An unrelated assignment does not clear the finding.

A Prepared circuit contains normalized configuration, resolved settings, topology, and the validated CONTROL construction description.
It contains no live matrix solver, mutable netlist, history arrays, or calculator state.
The engine creates those objects independently for each execution.
The implementation can share immutable descriptions across Batch runs.
It must not share writable numerical or CONTROL state.

## Engine seam

```java
SimulationResult runSimulation(
    PreparedCircuit circuit,
    ExecutionOptions options,
    BooleanSupplier cancellation);
```

`ExecutionOptions` contains logging enablement and logging interval only.
Circuit identity, settings, and parameter overrides cannot change at this seam.
Existing progress and trace listeners retain their current engine interface.
The existing `runSimulation(SimulationConfig, ...)` entry points remain compatibility adapters.
They resolve their input through preparation and enter the same numerical implementation.
Preserve their explicit no-file behavior and existing result/error form.
REST requests still require exactly one source selection.

## Source resolution and admission

The REST source adapter accepts `circuitId` or legacy `circuitFile`, never both or neither.
For `circuitId`, it requires an exact registry match.
For `circuitFile`, an exact live registry match selects the stored circuit.
Otherwise the complete string retains its filesystem-path meaning.
UUID-like syntax alone must not select the registry.
A relative path such as `./<uuid>` retains explicit filesystem meaning.

Stored entries hold immutable Circuit snapshots and provenance.
An update replaces one complete entry atomically.
A lookup therefore observes one complete circuit state.
Uploaded filenames remain provenance and do not cause a later filesystem read.
Path input captures its bytes once before acceptance and never reopens the path during execution.
A missing or unreadable source cannot produce an accepted job.

Single-job admission captures input, resolves request options, prepares the circuit, and then publishes the job.
Source inspection and preparation can run outside the scheduling monitor.
Capacity must be checked again when the job becomes visible.
Existing queue limits, cancellation signals, progress delivery, and terminal-state ownership remain in the job module.

Batch admission captures one base snapshot, expands bounded parameter sets, and prepares every child before publication.
An invalid final child rejects the complete Batch.
The failure creates no visible Batch, child jobs, queue tasks, or consumed retained-job slots.
Publication and capacity admission remain one operation.
Execution gives every child independent state.
Later registry edits or removal cannot change already accepted work.

For stored IDs, omitted settings inherit the captured values.
For path requests, retain current single-request timing requirements and existing Batch timing defaults.
Both explicit `circuitId` and legacy `circuitFile: <registered-id>` use stored-ID semantics.

## Parameter interpretation

One internal descriptor table owns each supported numeric configuration field, its aliases, saved layout, constraints, and derived values.
Parser display aliases, overrides, and netlist construction use this interpretation.
Remove the zero-dependent semantic fallback from the execution path.
Zero is a value and cannot indicate that a field was omitted.

Resolve component names as opaque names before binding a parameter.
`R.1.resistance` identifies the complete name `R.1`.
Reject an ambiguous name across domains or multiple valid name/field matches.
Do not select the first domain or split blindly at the first dot.

Resolve all requested paths to logical fields before assigning any values.
Equal named/indexed assignments to one logical field coalesce.
Different assignments to that field reject the whole request, regardless of map iteration order.
Reject null, nonfinite, unknown, unsupported, and read-only values with path-specific diagnostics.
Apply existing type-specific constraints instead of a universal positive-number rule.

The existing numeric request shape remains `Map<String, Double>`.
Configured numeric fields are eligible, including CONTROL CONST, GAIN, and SIGNAL fields.
Wiring, probe references, string settings, identifiers, and runtime state are copied and validated.
They are not writable through this numeric override map.
Expose supported configuration names from the descriptor table, rather than treating raw model-map keys as writable fields.
Retain indexed paths only for verified configuration slots.

### Saved-source normalization

Match the desktop import precedence: indexed values, legacy optimization names, explicit named tokens, per-type migration, and runtime-state reset.
Preserve the distinction between derived display aliases and explicitly saved fields.
Modern explicit named values can override stale indexed values, including explicit zero.

For independent sources, `sourceType` is slot 0 and `dcValue` is slot 1.
Modern `amplitude` is slot 20, frequency is slot 2, offset is slot 3, and phase in degrees is slot 4.
When the amplitude token is absent, use the legacy amplitude migration from slot 1.
When the phase-degrees token is absent, convert the legacy radian phase.
Token presence controls migration. File version or a zero-value test cannot replace it.

Resolve saved optimizer bindings after saved-value precedence and per-type migration.
A named numeric token does not clear a legacy `nameOpt[]` binding.
Read `<field>$ParameterName` only when the corresponding named numeric token exists.
An explicit binding token replaces or clears the legacy binding. The final nonempty binding selects the optimizer value.
The bare `nameOpt[] null` value represents an absent array. A delimited `/null` entry remains an optimizer name.
Legacy amplitude migration carries its slot-1 binding into amplitude. Legacy phase conversion changes the backing value, not a retained binding value.
Explicit request assignments take precedence over resolved bindings and remain effective when the resulting snapshot is prepared again.

For saved CONTROL SIGNAL arrays, accept GUI subtype IDs 402, 403, and 404 for sine, triangle, and rectangle.
Also accept the corresponding legacy IDs 0, 1, and 2.
Normalize these IDs to factory IDs 400, 401, and 404.
Saved slot 3 contains the offset. Saved slot 4 contains the phase.
Version 170 stores that phase in degrees. Other supported saved versions store it in radians.
The canonical factory layout uses phase in radians at slot 3 and offset at slot 4.
Programmatic canonical arrays retain those units and positions.
The named `phaseDegrees` field and its `phase` alias use degrees. Their `param3` alias uses radians.
Compare named and indexed phase assignments in physical units before conflict detection.

For linear capacitors, derive effective slots 6 and 7 from the configured capacitance.
Preserve configured initial voltage and reset current/history fields independently.
Do not silently convert an unsupported nonlinear layout into a linear capacitor.
CONTROL numeric arrays and their visible named/indexed values come from the same normalized configuration.

## Executable coverage contract

Coverage is a matrix of input kind, domain, type, mode, and saved layout.
A registry entry alone does not prove file-to-engine execution support.
Every matrix row requires either a passing behavioral contract or an explicit unsupported diagnostic.
This scope covers the complete current executable subset and adds no new executable types.

| Family | Preparation obligation |
|---|---|
| Resistor, inductor, capacitor | Configured values, initial conditions, derived values, and supported saved layouts. |
| Independent voltage/current sources | DC and sinusoidal modes, old/new identifiers, named-token precedence, legacy amplitude and phase. |
| Switch, MOSFET, IGBT, thyristor, diode | Verified configuration fields and preserved gate/turn-off parameter layouts. Runtime resistance/current/history remain read-only. |
| Coupled winding and LK_M coupling metadata | Inductance, signed coupling configuration, winding association, and identifier preservation. |
| Registered thermal/reluctance types | Cover the input paths that actually reach the current engine. Do not claim support for ignored saved thermal element blocks. |
| CONTROL VOLT, AMP, CONST, SIGNAL, SCOPE, GATE, GAIN | Numeric configuration plus required-reference and layout validation. SIGNAL includes sine, triangle, and rectangle. |

The current default stamper registry contains 17 types.
The CONTROL type set contains seven types.
Treat those inventories as drift checks, not sufficient capability tests.
Controlled electrical-source modes lack a complete saved-input-to-engine controlling-reference path.
Report them as unsupported rather than adding dependent-source execution in this refactor.
Unsupported CONTROL subtypes, malformed simulation blocks, and skipped executable domains cannot pass readiness through fallback defaults.
Preserve semiconductor loss metadata, including `kON` and `kOFF`, as read-only inspection data without a capability error.
Do not treat ignored loss metadata as active executable behavior.
Report a nonzero thyristor reverse-recovery delay as unsupported because the headless engine does not execute that delay.
Also report active nonlinear characteristics, parallel-device counts above one, and enabled IGBT saturation-current behavior as unsupported.
Reject numeric requests that try to change those unsupported fields.
Decorative metadata and nonexecuting SPECIAL content can remain ignorable when classified as such.

## Diagnostics and REST behavior

A diagnostic contains a stable code, category, severity, and optional domain, component, parameter path, and source location.
Categories distinguish parsing, configuration, overrides, references, topology, and capability.

Examples include `SOURCE_NOT_FOUND`, `AMBIGUOUS_PARAMETER`, `OVERRIDE_CONFLICT`, `INVALID_PARAMETER`, `UNCONNECTED_GROUND`, and `UNSUPPORTED_SOURCE_MODE`.
Messages are explanatory. Clients use codes for decisions.
Preserve all known diagnostics when an unsupported capability prevents later checks.
Preflight does not run time steps or promise numerical convergence.

`/validate` uses the same preparation path and returns `READY`, `INVALID`, or `UNSUPPORTED` with typed diagnostics.
Keep legacy `valid`, `warnings`, and `errors` fields.
Use `valid` as a conservative readiness summary and add explicit `readyForHeadless` and `outcome` fields.
An unsupported outcome is not a declaration that the circuit is invalid in the desktop application.
If both invalid and unsupported findings exist, use `INVALID` and retain both categories.

| Operation outcome | REST result |
|---|---|
| Both/neither source fields or malformed override request | 400 with typed diagnostics. |
| Readable path source whose captured bytes cannot be parsed | 400 with `MALFORMED_SOURCE` diagnostics before job publication. |
| Missing explicit ID or missing or unreadable path source | 404 with `SOURCE_NOT_FOUND` diagnostics before job publication. |
| Parseable resolved input with invalid configuration/topology or unsupported execution | 422 for job/Batch submission. |
| Validation of an existing stored circuit | 200 with the readiness report. |
| Parseable upload or clone with accepted overrides but failed readiness | Keep the inspectable stored circuit and return its ID. |
| Capacity rejection | Preserve the existing 429 contract and retained results. |
| Failure after an accepted numerical run starts | Preserve the existing failed-job result contract. |

Clone creation stores a returned effective snapshot only for `Inspectable` or `Ready`.
A rejected override set creates no clone and changes no stored circuit.
A Batch rejection does not publish partial work.

## Depth and compatibility

This seam gives callers leverage: one preparation result covers effective values, diagnosis, and execution input.
It gives locality to saved-format and override rules that currently span the parser, REST store, and engine.
Two real adapters justify source resolution: stored circuits and filesystem input.
Keep registry identity outside the core module.

Rejected alternative: synchronize aliases inside each caller.
That retains duplicate interpretation and cannot align validation with execution.
Rejected alternative: put the REST store in the core.
That couples numerical callers to server identity and lifecycle.
Rejected alternative: keep a live solver inside the prepared result.
That shares mutable state across validation, repeated runs, or Batch children.

## References

- [Domain language](../../CONTEXT.md)
- [Admission contract](0001-circuit-preparation-admission.md)
- [Core parser](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/io/CircuitFileParser.java)
- [Parameter overrides](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/io/ParameterOverrideApplicator.java)
- [Netlist construction](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/circuit/netlist/NetlistBuilder.java)
- [Engine](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/HeadlessSimulationEngine.java)
- [Desktop source import](../../src/main/java/gecko/geckocircuits/circuit/circuitcomponents/AbstractCircuitSource.java)
- [CONTROL factory](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ControlCalculatorFactory.java)
