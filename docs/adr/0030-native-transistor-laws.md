# Evaluate native transistor models for the archive
ADR-0030 · Codex · 2026-11-27 · Status: Proposed · Version 0.2
Audience: Simulation Core maintainers and independent reviewers.

**Decision:** Develop opt-in transistor laws for bias-dependent circuits. Treat ideal-switch substitutions as case-specific approximations.

## Context

Issue #164 covers MOSFET and bipolar junction transistor (BJT) instances and vendor models. Current imports reject the test devices. A fixed-state switch has zero incremental base-to-collector gain. The archived compensated regulator uses transistor gain across a 10 Hz to 100 kHz sweep [C, High]. [#164](https://github.com/tinix84/GeckoCIRCUITS/issues/164) · [Archive requirements](https://github.com/tinix84/GeckoCIRCUITS/issues/48)

## Options

| Option | Pros | Cons | Effort |
|---|---|---|---|
| Use an ideal switch with a body diode where applicable | Reuses existing Gecko switching elements | Omits charge, interelectrode capacitance, and incremental gain | Medium |
| Add native transistor laws | Supports terminal current and small-signal derivatives | Needs nonlinear convergence and model-specific limits | High |

Table 1. An ideal switch can approximate selected circuit outputs. A device law preserves bias dependence.

## Decision and rationale

The archived LLC converter compares two SPA11N60C3 MOSFETs with two voltage-controlled switches and body diodes in LTspice. The comparison keeps the transformer, gate-drive circuits, and other parts fixed. The switches use 4.08 V thresholds, 0.34 Ω on-resistance, and 1 GΩ off-resistance. Each body diode uses a 0.7 V threshold, 0.34 Ω on-resistance, and 1 GΩ off-resistance. Output voltage stayed within 3 % at all 1,357 samples. The other five author traces exceeded 3 % on 87 to 1,156 samples per trace [S, High]. The interval is 7.95 ms to 8.00 ms. The denominator is the greater of the absolute reference value and a 1 nV or 1 nA floor. An independent RAW decoder reproduced all counts. The input files and comparison script are in the local evidence folder for #164.

The TL431 comparator trial replaces its external 2N2907 with a switch. The switch uses a 0.675 V threshold, 0.5 Ω on-resistance, and 1 GΩ off-resistance. It removes the original base current. At inspection, the trial remained near 15.293 ms of the 20 ms interval and reported repeated tolerance relaxations [S, High]. The comparison has no complete switch trace.

## Consequences

Start with terminal-current and derivative tests for BJT and MOSFET laws. The BJT pilot is the archive's TL431 discrete AC test. Its child uses bipolar transistors, diodes, resistors, and capacitors. Preserve the transistor area multipliers and its hierarchical terminal connections. The unchanged archive case 443 gives a 401-point LTspice reference. The generated deck contains all 11 BJTs. LTspice uses Gmin stepping to establish its bias [S, High]. Retain the authored model cards and gate-drive circuits. Qualify each import on the original analysis and its physical traces. Record an approximation on the archive issue only after its affected traces and limits are known.

## Transient MOSFET scope

Use the Level 1 channel law with explicitly selected width and length and a source-tied bulk. Include the body diode and the reverse channel. Use the drain and gate derivatives in the transient Newton solve. Keep the legacy switch model for circuits that do not select a device law.

Supply the supported NMOS or PMOS parameters in a local model card. Use separate circuit elements for external capacitance and resistance. Vendor models, intrinsic charge, and an independent bulk terminal require separate device laws. [SPICE Level 1 reference](https://github.com/ngspice/ngspice/blob/master/src/spicelib/devices/mos1/mos1load.c)

## Initial AC scope

Use a nominal-temperature bipolar law with collector, base, and emitter terminals. Preserve local model cards and device area. Accept an implicit substrate or an explicit grounded substrate. Reject unsupported substrate and model effects.

Solve the nonlinear bias before the AC sweep. Include Early effect, gain reduction at high current, terminal resistance, depletion capacitance, and diffusion charge. Use source stepping for feedback circuits with a difficult bias. Keep the opt-in off by default.

For direct ASC hierarchy import, use opt-in and an explicit local model-card file. Check the stock symbol pin order and polarity. Record the model file digest with the source dependencies. Sum signed transistor terminal currents at each hierarchy boundary. Keep the same local circuit laws for flat and hierarchical AC analysis. Transient bipolar operation needs a separate model. The acceptance evidence belongs to the issue and its pull request. [#164](https://github.com/tinix84/GeckoCIRCUITS/issues/164)
