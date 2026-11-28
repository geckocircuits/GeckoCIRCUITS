# Preserve native occurrence identity in transient execution

ADR-0032 · Codex · 2026-11-27 · Version 1.0

**Decision** Keep the native hierarchy in the detached execution model. Use occurrence paths to name solver rows and resolve numeric overrides.

## Context

A native ASC definition can have several occurrences. Their local component names can be the same. Each occurrence has its own parameters and formal terminals. A root voltage probe can refer to a label on an occurrence pin. [ADR-0022](0022-native-asc-hierarchy.md) · [Issue #196](https://github.com/tinix84/GeckoCIRCUITS/issues/196)

## Options

| Option | Advantage | Disadvantage | Effort |
|---|---|---|---|
| Keep one global component name | Reuses flat lookup | Repeated local names collide | Low |
| Rename and flatten the saved model | Gives each solver row a unique name | Changes the authored names and loses formal terminal context | Medium |
| Keep the hierarchy and qualify execution names | Keeps saved identity and supports independent occurrence values | Readiness and execution must use the same scope rules | Medium |

## Decision and rationale

Resolve the saved native wires and formal terminals before assigning solver rows. Transient ASC primitives still need physical terminal geometry when they have terminal labels. Use the probe's sheet to resolve a label to a terminal group, then use the solver node assigned to that group.

Use the occurrence path and local component name as the execution name. Reject ambiguous names. Apply numeric overrides to the detached run after resolving authored bindings.

## Consequences

Saved names, numeric identifiers, formal terminals, and bindings remain intact. A key such as `X1:Xleaf:Rseries.resistance` selects one occurrence. The admission criteria and scope are recorded in the issue. [Issue #196](https://github.com/tinix84/GeckoCIRCUITS/issues/196)
