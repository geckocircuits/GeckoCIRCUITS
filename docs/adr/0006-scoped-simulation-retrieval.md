# Scoped simulation retrieval

Date: 2026-11-09

## Context

Progress polling and single-signal retrieval need a job's status before they can form a response.
The general simulation snapshot also copies every retained waveform twice to preserve ownership.
Using that snapshot for metadata makes polling allocation grow with the entire retained result set.

## Decision

1. Capture status metadata without reading result arrays under the existing Simulation job monitor.
2. Return a detached SimulationResponse with empty results from the metadata-only service read.
3. Use that read for progress polling and single-signal readiness checks.
4. Copy only the requested array through the existing defensive single-signal accessor.
5. Preserve full results for endpoints that explicitly return simulation results, including the general simulation endpoint.
6. Preserve existing response fields, completion checks, and missing-job or missing-signal responses.

## Why

Metadata allocation must not depend on waveform size or signal count.
A single-signal request needs allocation proportional to that signal, regardless of other retained signals.
Detached metadata and copied selected arrays preserve the existing ownership boundary.
The job monitor continues to protect each read without adding a second result store.

## Options considered

| Choice | Benefit | Cost |
|---|---|---|
| Use the general snapshot for every read | Reuses one response path. | Copies unrelated waveform arrays for polling and selected reads. |
| Return shared result arrays | Avoids copying samples. | Allows callers to change retained results. |
| Add a metadata-only snapshot | Preserves ownership and current HTTP contracts. | Requires callers to select the appropriate read. |

## Consequences

The general simulation and listing endpoints retain their full-result contracts.
This decision does not change progress units or the existing sequence of progress and signal reads.
Allocation regressions use thread allocation accounting where the JVM supports it, with budgets independent of unrelated waveform data.
HTTP and lifecycle regressions verify response compatibility and detached ownership.

## References

- Service API: `SimulationService.getSimulationStatus(String)`.
- Retrieval HTTP contracts: `SimulationRetrievalIntegrationTest`.
