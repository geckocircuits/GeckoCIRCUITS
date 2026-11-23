# Numerical reference compatibility

Date: 2026-11-13

The shared backend preserves the desktop numerical reference and saved wire order inside electrical execution.
Public node indexes and published voltages retain the existing ground-relative convention.
The internal netlist view shares run-owned parameters and maps numerical results back before publication.

Different reference nodes are equivalent in exact arithmetic.
They can produce different small currents when matrix conductances span many orders of magnitude.
The PFC current-control example exposes this difference in its near-zero inductor-current minimum.
Preserving its existing golden reference requires the desktop reference convention and incremental matrix updates.
The backend rebuilds the matrix when the timestep or matrix inputs change.
It retains converged diode updates between unchanged steps.

Changing public node indexes would break existing topology and voltage-output contracts.
Relaxing the golden tolerance would hide the execution difference.
The internal numerical view preserves both contracts, with an additional node permutation and result mapping.

The regression tests compare the PFC minimum with legacy execution and the unchanged golden reference.
Core contracts cover public voltages, resistance changes, and timestep changes.
