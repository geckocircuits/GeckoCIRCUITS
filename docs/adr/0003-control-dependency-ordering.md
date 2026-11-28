# CONTROL dependency ordering

Date: 2026-11-07

## Context

CONTROL connections define dependencies between calculators.
File order must not introduce an implicit sampling delay.
A buffered feedback delay needs historical output and a settled current input from the same time step.
Preparation must reject an instantaneous cycle before job admission.

## Decision

1. Order factory calculators by their actual wired signal dependencies.
2. Use input order to choose between independent ready calculators.
3. Reject instantaneous cycles with a typed factory exception and a structured preparation diagnostic.
4. Publish all buffered delay outputs before evaluating combinational calculators.
5. Capture all buffered delay inputs after combinational evaluation.
6. Keep zero and substep delays as direct feedthrough in the supplied execution order.
7. Preserve caller responsibility for combinational order in the explicit ordered-netlist API.
8. Keep the serialized CONTROL capability boundary from the preparation interface.

## Why

Shared signal references identify the dependencies that execution actually reads.
A topological order makes acyclic results independent of file order.
Rejecting a cycle avoids treating an arbitrary block position as a delay.
Separate publication and capture prevent an extra sampling delay in buffered feedback.
They also prevent one delay from capturing another delay's unpublished output.

## Options considered

| Choice | Benefit | Cost |
|---|---|---|
| File order | Requires no dependency analysis. | Reordering changes first-step results and apparent latency. |
| Dependency order only | Resolves acyclic chains and identifies instantaneous cycles. | A buffered feedback loop still needs separate publication and capture. |
| Dependency order plus delay phases | Preserves combinational dependencies and declared buffered delay. | The netlist needs two delay phases around combinational evaluation. |
| Add serialized delay types | Extends saved-circuit execution. | Expands the preparation capability contract beyond this ordering decision. |

## Consequences

Factory construction and preparation use the same cycle check.
Preparation honors existing CONTROL configuration and reference diagnostics before constructing calculators.
Buffered delay phases require simulation-start initialization.
Direct calls to a delay calculator retain their existing combined publication and capture behavior.
Coverage gates protect the factory, netlist, and delay calculator.

## References

- [Preparation interface](0002-circuit-preparation-interface.md)
- [CONTROL factory](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ControlCalculatorFactory.java)
- [Ordered netlist](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/simulation/ControlNetlist.java)
- [Delay calculator](https://github.com/tinix84/GeckoCIRCUITS/blob/main/src/modules/gecko-simulation-core/src/main/java/gecko/core/control/calculators/DelayCalculator.java)
