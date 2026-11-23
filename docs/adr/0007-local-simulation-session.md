# Local simulation session

Date: 2026-11-09

## Context

The desktop supports simulation domains and execution modes that the current headless engine does not accept.
The desktop kernel also reads global frontend settings and changes scope widgets.
Continue preserves numerical history after a segment ends and can change the time step.
External callers can supply simulation timestamps and exchange vector signals.
These behaviors must remain available when the backend owns execution.

## Decision

1. Use one headless Java backend for desktop and REST execution.
2. Give each simulation session independent configuration, numerical state, CONTROL state, recordings, and extension instances.
3. Capture unsaved editor state and required resources in a circuit snapshot before preparation.
4. Expose session operations for run, pause, continuation, step, external exchange, bounded observation, and resource release.
5. Keep segment completion separate from session closure so Continue can retain history.
6. Keep numerical work in process. Route Swing through a local adapter.
7. Keep widgets, display scheduling, menus, and file selection in frontend adapters.
8. Pass configuration and presentation callbacks explicitly during migration.
9. Compare desktop and backend behavior before routing each feature through the shared implementation.
10. Require all desktop simulation features to have backend implementations and parity evidence before declaring separation complete.

## Why

A stateful session preserves continuation without exposing solver matrices to callers.
One execution owner makes numerical and lifecycle corrections apply to both frontends.
Bounded observations support plotting and analysis without copying every recorded waveform for each status read.
Explicit resource ownership prevents concurrent sessions from sharing mutable execution state.
The local adapter preserves desktop use without requiring a network service.

## Options considered

| Choice | Benefit | Cost |
|---|---|---|
| Minimal open, command, and read operations | Gives callers a small method surface. | Places lifecycle complexity in a broad command vocabulary. |
| Extension-oriented command interface | Covers specialized blocks and external execution. | Makes common desktop operations depend on more protocol rules. |
| Stateful simulation session | Maps directly to Start, Pause, Continue, and Step. | Requires explicit state transitions and recording lifetimes. |
| Remote desktop client now | Separates processes. | Adds transport work before numerical parity. |

## Consequences

Preparation, execution, results, and analysis need shared ownership beyond the session interface.
Backend readiness checks cannot replace implementations of desktop features.
Migration preserves the existing desktop behavior as a comparison reference.
The backend must acknowledge pauses at committed time-step boundaries.
Continuation must distinguish a paused segment from an extension after its end.
Frontend adapters own UI thread scheduling and presentation errors.
A remote Swing adapter is future work and does not define this migration's completion criteria.
