# Pull-based frontend and single-owner session thread

Date: 2026-11-12

## Context

The [local simulation session decision](0007-local-simulation-session.md) requires one backend for desktop and REST.
A first attempt shared the legacy desktop kernel and added locks, execution tickets, readiness futures and queued callback admission.
That kernel calls scope widgets, menus and export progress synchronously while it owns simulation state.
Those callbacks can request lifecycle changes, so each added lock produced another lock cycle.
The attempt is preserved unmerged at `d3657a97` on `codex/consolidate-backend-frontend-20261110`, including its draft queued-callback ADR.

## Decision

1. The backend never calls a frontend. No listener, observer or widget callback crosses the boundary.
2. Frontends pull `status()` and bounded `observe()` results. Swing polls on a timer; REST progress polls the same reads.
3. One thread owns each `SimulationSession`. Run, pause, continue, step, extend, external exchange and close are commands submitted to that thread.
4. Pause and stop are flags that the owning thread checks at committed time-step boundaries.
5. Reads from other threads never pass a published committed-sample count.
6. The core `SimulationSession` is the single engine. The desktop routes a circuit to it when `CircuitReadiness` accepts the snapshot.
7. Circuits that the core does not support yet run on the legacy kernel, unchanged and without new concurrency work. The legacy path is removed when all supported callers and test circuits route to the core.
8. Separation is complete when GUI Start/Pause/Continue, GeckoScript, RMI/MMF and Simulink/External callers all use the core session.

This replaces ADR-0007 decision 8 for presentation: presentation callbacks are replaced by pulled observations.

## Why

The EDT never holds or waits for a backend lock, so it cannot deadlock with the backend.
Synchronous callers can block on a command result because the owning thread never waits on a frontend.
One execution owner removes the need for admission queues, leases and readiness futures.
Automatic fallback keeps every desktop feature available while parity work proceeds.

## Options considered

| Choice | Benefit | Cost |
|---|---|---|
| Lock the shared legacy kernel and queue contended callbacks | Keeps the legacy kernel as the engine. | Did not converge; every fix exposed a new lock cycle. |
| Push events through a nonblocking queue | Sample-accurate live updates. | More code than polling; no current workflow needs it. |
| Pull reads and a single-owner session thread | Removes the callback deadlock class. | Scope updates lag by up to one timer tick. |
| Separate backend process now | Clean process split. | Transport work before parity; added latency for stepping. |
| Hard cutover after full parity | One code path. | No usable progress until every feature is ported. |

## Consequences

Scope repaint latency is bounded by the polling interval.
Each external step is a hand-off to the session thread; its overhead must be measured for long Simulink runs.
Export runs on the session thread and reports progress through pulled status.
The legacy kernel remains in the build until the fallback is unused.
A remote Swing frontend remains future work and can reuse the pull API.
