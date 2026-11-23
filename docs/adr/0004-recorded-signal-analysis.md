# Recorded signal analysis

Date: 2026-11-07

## Context

Retained simulation signals have values and a recorded clock.
Value-only lookup forced analysis callers to supply a sample rate and discarded requested time bounds.
Variable time steps require the actual interval lengths.
Fourier selection also skipped its lower boundary and shortened its integration interval.

## Decision

1. Capture the selected values and recorded timestamps under the same Simulation job monitor.
2. Keep an independent timed signal behind the core analysis interface.
3. Use recorded timestamps for stored signals. Ignore any supplied sample rate for that input form.
4. Preserve explicit sample rates and raw-array precedence for raw requests.
5. Default each omitted stored-signal bound to its corresponding recorded endpoint.
6. Reject nonfinite, reversed, empty, or out-of-range windows with a client error.
7. Hold each recorded value until the next timestamp. Insert exact window boundaries using that same convention.
8. Rebase the selected clock to zero for calculation while preserving recorded interval lengths.
9. Use the complete selected interval for Fourier frequency and sampling. Reject harmonics beyond its available resolution.

## Why

The stored clock is the evidence for physical frequency and time-weighted metrics.
A supplied rate cannot safely replace it when time steps vary.
Left-held boundary values preserve the characteristics calculator's existing integration convention.
One selection path makes characteristics, RMS, and Fourier use the same requested duration.
Relative timestamps avoid precision loss from large absolute clock origins.

## Options considered

| Choice | Benefit | Cost |
|---|---|---|
| Reconstruct time from a supplied rate | Preserves the old caller requirement. | Loses recorded intervals and changes physical frequency. |
| Use recorded samples only inside the bounds | Keeps only original points. | Rounds requested bounds and changes the analysis duration. |
| Recorded clock with exact held boundaries | Preserves the requested duration and existing integration convention. | Requires explicit boundary samples and clock validation. |

## Consequences

Stored values require a matching, finite, strictly increasing clock with at least two samples.
Raw input retains its existing sample-rate contract. Its stored-signal window fields remain inapplicable.
The reported characteristics sample count includes inserted boundary samples.
Characteristics can analyze one selected interval. Fourier can report its DC component but rejects unresolved higher harmonics.
Fourier coefficients use the window start as the phase origin.
Read-only analysis does not change retained results.

## References

- [Timed signal](../../src/modules/gecko-simulation-core/src/main/java/gecko/core/signal/TimedSignal.java)
- [Analysis service](../../src/modules/gecko-rest-api/src/main/java/gecko/rest/service/AnalysisService.java)
- [Stored-signal HTTP contracts](../../src/modules/gecko-rest-api/src/test/java/gecko/rest/integration/StoredSignalAnalysisIntegrationTest.java)
