# Use one headless writer for circuit imports
ADR-0019 · Codex · 2026-11-24 · Version 1.0
Audience: Core and desktop maintainers

## Decision
We serialize owned circuit descriptions through one headless writer shared by all circuit importers.

## Context
The core parser reads circuit files, but desktop saving depends on frontend objects.
SPICE, LTspice, and PLECS imports need the same output contract.
Both the desktop and the core require component markers, numeric arrays, terminal labels, and simulation settings.
Waveform resources must travel with the circuit.

## Options
| Option | Pros | Cons | Effort |
|---|---|---|---|
| Call the desktop saver | Uses existing serialization | Requires frontend objects and a display | High |
| Write files in each importer | Small initial implementation | Repeats serialization and compatibility fixes | Medium |
| Share a core writer | One contract and no frontend dependencies | Requires explicit compatibility tests | Medium |

## Decision and rationale
`CircuitFileWriter` accepts `CircuitModel` and emits gzip-compressed UTF-8.
It writes desktop markers (`e`, `c`, `eTH`, `sp`, and `verbindung` plus domain) before their blocks.
Each component includes its type, identifier, position, orientation, numeric parameters, parameter strings, and ordered terminal labels.
Each component also has a text offset, an enabled state, and a parent identifier.
Global settings include duration, time step, solver, file version, display settings, path metadata, and optimizer bindings.
The `GeckoFileManager` marker precedes the file-manager block. Each embedded resource retains its bytes and owner identifiers.
Importers supply the numeric slots and named tokens that the desktop format stores.
The writer rejects programmatic parameter maps that lack a saved representation.
It also rejects saved component blocks that lack a supported writer representation.
Generated coordinates use the desktop grid. The writer sets worksheet bounds that include component and wire positions.
Wire crossings alone do not define junctions.
PULSE waveforms use embedded time/value tables through the existing SIGNAL import block.
These tables retain the existing two-step CONTROL coupling delay. Numerical acceptance must account for this delay explicitly.
Importers use `CircuitModel.addConversionIssue` to record source diagnostics.
The writer stores these diagnostics in a project section. The core recalculates native parser diagnostics when it reads the output.

## Consequences
Round-trip tests compare topology, parameters, resource bytes, and settings.
Physics tests run written files through the headless engine.
Desktop checks verify that generated circuits open and simulate.
The writer creates new files and refuses existing output paths.
Import success and numerical equivalence remain separate results for each archive model.

## Status
Accepted by the implementation contract for #42 on 2026-11-24.
