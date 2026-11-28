# Circuit preparation admission contract

Date: 2026-11-04

## Context

The release guide submits a cloned circuit ID through `circuitFile`.
The current job module forwards that value as a filesystem path.
Stored models and parameter aliases also differ from the values used during execution.
These choices define compatibility and ownership for the circuit preparation refactor.

## Decision

1. Add `circuitId` to single-job and Batch requests. Reject requests that supply both source fields.
2. Keep file paths and exact registered IDs in the legacy `circuitFile` field.
3. Capture a Circuit snapshot when a Simulation job is accepted. A Batch captures one base snapshot and isolates each run's overrides.
4. Reject an entire override set for an unknown path, invalid value, or conflicting named and indexed assignments.
5. Retain parseable uploads and clones for inspection when headless behavior is unsupported. Report Headless readiness separately from parse success.
6. Distinguish unsupported behavior from invalid topology. Passing preflight does not guarantee numerical convergence.
7. For stored IDs, inherit omitted time step, duration, and solver from the captured circuit. Explicit request values take precedence.
8. Preserve existing path-based timing requirements and Batch defaults.
9. Cover verified configuration fields for the complete current executable subset, including legacy sources and CONTROL configuration.

## Why

The documented clone workflow requires ID compatibility.
Accepted jobs need stable inputs when users later edit or remove a stored circuit.
Atomic overrides prevent a partial parameter set from producing an unintended experiment.
Separate readiness diagnostics keep inspection available without claiming that unsupported circuits can run.

## Options considered

| Choice | Benefit | Cost |
|---|---|---|
| Explicit ID plus legacy compatibility | Fixes the documented workflow and preserves current path clients. | Source resolution keeps a compatibility rule. |
| Legacy field only | Keeps the request shape small. | New callers still cannot state the source kind explicitly. |
| Snapshot at acceptance | Queued jobs and Batch runs keep the selected circuit state. | Admission must capture input state before dispatch. |
| Resolve at execution | Defers source capture. | Later edits can change an accepted experiment. |
| Atomic overrides | Every accepted assignment set has one meaning. | Clients must correct previously ignored invalid paths. |
| Partial overrides | Applies recognized entries despite other errors. | A run can use only part of the requested experiment. |

## Consequences

The core preparation module must remain independent of the REST circuit store.
Uploaded filenames identify provenance and must not replace captured circuit data during execution.
Stored-model updates and snapshot capture require a consistent operation across all model fields.
The [preparation interface](0002-circuit-preparation-interface.md) defines the module contract and executable-coverage obligations.
These decisions authorize the design contract. They do not authorize production implementation or publication.

## References

- [Domain language](https://github.com/tinix84/GeckoCIRCUITS/blob/main/CONTEXT.md)
- [Documented cloned-ID workflow](../releases/2200.md)
- [Current circuit store](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-rest-api/src/main/java/gecko/rest/service/CircuitFileService.java)
- [Current job admission](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-rest-api/src/main/java/gecko/rest/service/SimulationService.java)
- [Settings precedence](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/SimulationConfig.java)
