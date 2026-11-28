# Solve exponential diodes inside each electrical trial
ADR-0029 · Codex · 2026-11-27 · Status: Proposed · Version 0.2
Audience: Simulation Core maintainers and independent reviewers.

**Decision:** Use an opt-in Shockley junction with a Newton solve and commit its history with the electrical sub-step.

## Context

Issue #163 requires the exponential model in Simulation Core. The default diode uses a piecewise-linear (PWL) law. Archive case 103 needs direct current (DC) bias and alternating current (AC) linearization. Its clamp currents are approximately 1 fA in AC [S, High]. A calculation omitting both clamps also passes the 3 % test [C, High]. See the issue and the event and adaptive step decisions. [#163](https://github.com/tinix84/GeckoCIRCUITS/issues/163) · [ADR-0016](https://github.com/tinix84/GeckoCIRCUITS/blob/main/docs/adr/0016-zero-crossing-event-location.md) · [ADR-0018](https://github.com/tinix84/GeckoCIRCUITS/blob/main/docs/adr/0018-adaptive-electrical-step.md)

## Options

| Option | Pros | Cons | Effort |
|---|---|---|---|
| Keep the PWL diode | Uses current stamps and event handling | Omits the voltage-dependent slope and diffusion charge | Low |
| Use a static Shockley stamp | Solves the forward current law | Omits diffusion charge when transit time is set | Medium |
| Use a Shockley junction with series resistance and diffusion charge | Gives DC current, AC slope, and transient branch current | Needs nonlinear solves and private trial state | High |

Table 1. The Shockley junction with charge uses the parameters in case 103.

## Decision and rationale

For the initial model, use saturation current, emission coefficient, series resistance, and transit time at nominal temperature. Reject unsupported model parameters at import. Expose the opt-in through the solver settings. Enable the existing PWL path by default.

Linearize current and charge inside each electrical trial. Use junction-voltage limiting and damped Newton iterations. For transient trials, start with a 50-iteration limit and at most 48 line-search halvings per iteration. Accept a result only when the physical Kirchhoff current residual and the voltage correction meet their tolerances. Use an absolute and a relative tolerance for each quantity. The initial values are 1 pA, 10 nV, and a relative tolerance of 10⁻⁶ [A].

Keep each trial’s junction voltage and charge separate from committed state. Independent adaptive companion solves start from the same committed state. An accepted sub-step advances the history for the next sub-step. Event-location and refinement trials reconverge at their candidate width and discard tentative history. On failure, reduce the adaptive step if possible. Report nonconvergence at the minimum width or in fixed-step mode. Never publish an unconverged trial. The continuous diode produces no PWL switching event.

## AC bias

Start with flat AC circuits. The import flag explicitly selects the exponential law. Keep that law in the saved document. Use the diode's converged DC current and tangent conductance to derive AC admittance. The AC bias permits up to 48 line-search halvings to reach the junction-voltage range from a high-impedance initial state. Include series resistance and diffusion charge. Reject incompatible laws and unsupported parameters. Save the AC and transient selections with distinct law tags. An AC-only document retains its analysis boundary.

## Transient integration

For backward Euler, use the difference between the trial charge and the accepted charge divided by the step. For trapezoidal integration, use twice that difference divided by the step and subtract the last accepted displacement current.

Compare the adaptive trial and its companion in junction voltage and displacement current. Check the complete matrix solution and evaluate the physical residual before accepting a trial. Use the active matrix representation for the residual.

Gear–Shichman, DC start, UIC, and periodic steady-state search require a separate history contract. Reject those combinations with this transient law.

## Consequences

Publish terminal current from conduction and charge. The initial acceptance combines an unchanged archive case with a forward sweep. The forward sweep checks voltage and differential resistance over at least three current decades. It detects omitted junctions and fixed-threshold replacements.

## Status

Proposed by Codex on 2026-11-27. Independent review covers case 103 and the proposed forward sensitivity check. The issue and its pull requests retain the verification evidence.
