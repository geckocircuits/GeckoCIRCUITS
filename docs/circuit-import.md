# Convert a circuit from the command line

The importer accepts resistors, inductors, capacitors, diodes, and voltage-controlled switches.
Independent voltage and current sources accept DC, PULSE, and SIN values.
The importer uses the transient directive for the time step and duration.
It reads scalar parameter definitions from `.param`.

1. Build the core module.

```sh
mvn -f pom-reactor.xml package -pl src/modules/gecko-simulation-core
```

2. Select an output path that does not exist.
3. Convert the source file.

```sh
java -cp src/modules/gecko-simulation-core/target/gecko-simulation-core-1.0-SNAPSHOT.jar gecko.core.io.importer.Main input.cir output.ipes
```

4. Inspect the exit code and the JSON diagnostic lines.
5. Open the output file in the desktop application.

| Exit code | Meaning |
|---|---|
| 0 | The importer reports no unsupported or malformed input. |
| 1 | The command cannot read the input or create the output. |
| 2 | The output contains a partial conversion and retained diagnostics. |

The command creates a new file at the selected path.
Review the diagnostic lines for a partial conversion before you use the circuit.
The core retains these diagnostics when it opens the generated circuit.
Waveform tables are embedded in the circuit. The output does not depend on temporary waveform files.

The RC fixture uses a time step of 1 µs and a time constant of 1 ms.
Its voltage check allows an absolute error of 0.05 V, which is 1% of the 5 V pulse amplitude.
SIGNAL-controlled sources use the existing CONTROL coupling sequence, which introduces a two-step delay.
Use a time step that keeps this delay below the required physical tolerance.

Subcircuits, unsupported directives, and exponential diode behavior produce conversion diagnostics.
The importer uses the native piecewise-linear diode model.
