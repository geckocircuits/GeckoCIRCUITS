# Solve behavioural sources inside the electrical step

ADR-0033 · Claude Fable 5.2 for tinix84 · 2026-11-28 · Status: Proposed · Version 0.1
Audience: Simulation Core maintainers and independent reviewers.

**Decision:** Add an opt-in behavioural source law, `V = f(...)` or `I = f(...)` of node voltages and voltage-source branch currents, and solve it in the existing nonlinear device iteration with an exact Jacobian.

## Context

186 of the 226 analyses of the M6 target contain an LTspice behavioural source. The spike [#162](https://github.com/tinix84/GeckoCIRCUITS/issues/162) measured the existing route, CONTROL blocks driving a signal-controlled source ([ADR-0008](0008-controlled-source-compatibility.md)). The source reaches the circuit two solver steps late. Archive case 438 then fails #191 by 6.3 % of peak-to-peak at the authored 100 ns step, and passes only at 12.5 ns. With a lower load the same coupling sits in a loop and diverges; a smaller step diverges sooner. AC never reads CONTROL. The same law evaluated in the same step passes at every step, at about 1 % of the limit [S, High].

The archive uses 281 distinct expressions: node voltages, branch currents, `if` with comparisons (118), `&` and `|`, `sqrt`, `abs`, trigonometric functions, `atan2` and `**`. None uses `table`, `delay`, `ddt`, `idt`, `time` or `limit` [S, High].

## Options

| Option | Pros | Cons | Effort |
|---|---|---|---|
| CONTROL blocks and a signal-controlled source | Exists | Two-step delay; loops diverge; no AC | None |
| Compensate the delay, for example by extrapolating the CONTROL output two steps | No solver change | Fixes only the phase of feed-forward use. A loop keeps its delay, extrapolation amplifies high frequencies, and an `if` cannot be extrapolated. No AC | Low |
| A second Newton loop only for behavioural sources | Independent of the diode code | Alternates with the device loop; an averaged switch with a diode loses quadratic convergence | Medium |
| **The law in the existing device iteration** | One loop for diodes, MOSFETs and sources. Same step, exact coupling, same tolerances | Discontinuous expressions need a rule in the line search | Medium |

## Decision and rationale

A behavioural source is an LK voltage or current source that carries the law `Behavioural(expression, current)`. The law is the opt-in: only the import option `--behavioural-sources` creates it, from a netlist `B` element or a stock `bv` or `bi` symbol. Without the option these stay unsupported, with the same diagnostics as before. In a child schematic, each `{name}` of the expression binds as a required input of the definition. A circuit without the law builds no device iteration and runs the unchanged step, as for the Shockley diode ([ADR-0029](0029-shockley-diode.md)) and the MOSFET ([ADR-0030](0030-native-transistor-laws.md)).

The expression uses the `SpiceValue` grammar, extended with `v(a)`, `v(a,b)`, `i(source)`, `if(c,a,b)`, `atan2`, comparisons, `&` and `|`. Probes are valid only in a behavioural law. Scalar parameters bind once, at session start. A label resolves like a voltage probe on the same sheet; a current names a voltage source and reads its branch unknown, in the SPICE sign.

The source joins the iteration in `TransientSemiconductors`. The netlist sees a 0 V or 0 A source. Each iteration stamps the gradient into the source row, or into both KCL rows, and the value minus the gradient term into the right-hand side. The 50-iteration limit and the adaptive halving of [ADR-0018](0018-adaptive-electrical-step.md) stay unchanged.

The residual check gains a rounding floor, for all members of the iteration. A row is scaled by the larger of 1, its right-hand side, and 1 % of the sum of its absolute terms. The authored 1 µΩ resistor of the archive's transformer model otherwise holds the residual above 10⁻¹² A by rounding alone, and a linear law never converges. The solution-change criterion is unchanged.

Derivatives are exact. The parse tree evaluates value and gradient together, in forward mode. Finite differences were rejected: across a threshold they return the jump divided by the step.

Discontinuous expressions use the gradient of the selected branch, as SPICE does. The jump itself is not a descent direction, so a full Newton step that changes any comparison result is accepted without the line search. The next iteration linearises on the new branch. A hysteresis loop therefore settles in two or three iterations. A chattering threshold exhausts the iteration limit. The adaptive step then halves; the fixed step reports nonconvergence. The threshold time is not located inside the step ([ADR-0016](0016-zero-crossing-event-location.md) locates diode-family events only). Edge timing against LTspice belongs to [#206](https://github.com/tinix84/GeckoCIRCUITS/issues/206).

The source has no state, so it adds nothing to the adaptive error estimate. The restrictions of the device iteration apply: backward Euler or trapezoidal integration, no DC start, no UIC, no periodic steady state.

AC linearises the same law at the DC operating point. The bias solve of [ADR-0020](0020-native-linear-ac-analysis.md) includes the law in its Newton iteration, and the AC matrix receives the gradient as real controlled-source coefficients.

## Consequences

- Feed-forward and in-loop behavioural sources see the circuit of the same step [S, High, from the spike counterfactual].
- An expression that the archive does not use fails at import or preparation with its name.
- Out of scope: `table`, `delay`, `ddt`, `idt`, `time`, digital gates, DC sweep, and user functions of vendor models.

## Status

Proposed by Claude Fable 5.2 on 2026-11-28 for issue [#205](https://github.com/tinix84/GeckoCIRCUITS/issues/205). No review is claimed.
