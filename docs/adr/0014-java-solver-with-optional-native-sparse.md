# Java solver with optional native sparse backend

Date: 2026-02-05

The circuit solver stays in Java.
A sparse matrix path is added first with a pure-Java library (EJML), and a native solver such as KLU may be added later behind JNA/FFM as an option.
The comparison with SIMBA showed that its main advantages are algorithms: adaptive time steps, zero-crossing location, DC initialization and sampled control.
Changing language does not provide any of them.

## Considered options

- **Rewrite the solver in C++ or Rust.** Rejected: it means rewriting a large body of working simulation code, and the desktop is tightly coupled to it. Most circuits are small enough that pure-Java sparse LU is sufficient.
- **Native solver only.** Rejected: a native dependency would break the single cross-platform JAR. A native backend makes sense only for very large circuits (about 1,000+ nodes) or real-time hardware-in-the-loop simulation.

## Consequences

The solver features are tracked under #56 and are built in `gecko-simulation-core`.
Each one is opt-in, so results are unchanged when it is off.
A native backend must stay optional, and every build must still work without it.
