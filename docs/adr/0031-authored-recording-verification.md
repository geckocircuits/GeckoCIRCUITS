# Compare the authored recording with pinned evidence
ADR-0031 · Codex · 2026-11-27 · Status: Accepted · Version 1.0

Audience: M6 verification maintainers and independent reviewers.

**Decision** Use the authored recording interval for transient policy v2 and a shared batch runner for pinned comparison evidence.

## Context

An authored transient can start recording after time zero. Its recording can contain at least one switching period but fewer than ten. Policy v1 requires startup coverage and both full window widths. A shared batch needs reproducible inputs while each original analysis still needs independent qualification.

## Options

Table 1 compares the coverage and audit costs.

| Option | Benefit | Cost |
|---|---|---|
| Keep v1 and write case-specific comparisons | Keeps the historical protocol | Excludes valid shorter recordings or requires separate review of each comparison method |
| Relax metric limits or use the overlap of both traces | Increases the number of comparable traces | Can hide a missing tail, shifted response, or excessive error |
| Use authored coverage and a pinned batch | Preserves the declared recording and applies common metrics | Requires a complete switching period and case evidence prepared before comparison |

## Decision and rationale

Declare the original recording boundaries before comparison. The two traces must cover those boundaries on the same physical time axis. The report identifies whether the comparison includes startup and whether each trace covers time zero.

Use one full switching period `Ts` as the first window. Cap the second window at the smaller of `10 Ts` and the recording duration. This checks shorter authored recordings without extending the data or reducing the first window.

Keep the existing peak, average, root mean square (RMS), waveform error, and absolute tolerance limits. AC keeps the signed complex 3 % criterion. The [policy decision](https://github.com/tinix84/GeckoCIRCUITS/issues/191#recorded-window-policy-v2-and-batch-verification) gives the exact limits.

## Consequences

Keep v1 available for replay and retain the policy attached to each historical result. A batch manifest pins case identities, input files, and evidence receipts with SHA-256 digests. The audit index pins the case reports. Independent review confirms the original source, runtime, reference, and physical checks before qualification.

## Status

Accepted by the user on 2026-11-27. [M6 verification policy](https://github.com/tinix84/GeckoCIRCUITS/issues/191#recorded-window-policy-v2-and-batch-verification)
