# Convert a circuit from the command line

## SPICE netlists

The SPICE importer accepts resistors, inductors, capacitors, diodes, and voltage-controlled switches.
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

## LTspice schematics

Use the same command with a flat `.asc` schematic.

```sh
java -cp src/modules/gecko-simulation-core/target/gecko-simulation-core-1.0-SNAPSHOT.jar gecko.core.io.importer.Main input.asc output.ipes
```

Use stock symbols for the supported elements. The importer uses the saved symbol positions, wires, and net labels.

### Import an alternating current hierarchy

1. Keep the root `.asc`, child `.asc` sheets, and block `.asy` symbols together.
2. Convert the root schematic with the stock symbol directory and a source root.

```sh
java -cp src/modules/gecko-simulation-core/target/gecko-simulation-core-1.0-SNAPSHOT.jar gecko.core.io.importer.Main input.asc output.ipes --symbols path/to/stock-symbols --source-root path/to/project
```

3. Review the exit code and the JSON diagnostic lines.
4. Open the output file in the desktop application.

Use each additional `--source-root` after `--symbols`. Each child sheet and block symbol must resolve to one file across the caller directory and the source roots. Use a relative symbol path. The `--symbols` option verifies the pin geometry and order of the stock symbols used for the analysis. For a flat AC schematic, use the same option without `--source-root`.

A block symbol defines the formal ports with `PinName` and `SpiceOrder`. Use a matching named flag on the child sheet for each formal port. Put default parameter expressions in the child sheet's `.param` directives. Set parameter overrides on each block occurrence in its caller. The analysis evaluates an override in the caller's scope.

The output stores editable native child sheets. Edits to shared definition members apply to linked occurrences. Each occurrence keeps its own parameter overrides and position.

The importer reports unsupported symbol attributes such as `ModelFile`. Review diagnostics for unresolved symbols, invalid port mappings, or layout conflicts before you use the circuit.

### Select an LTspice AC profile

Select the profile for an ASC from LTspice 26.1.1 with the Normal solver and 27 °C temperatures.

1. In the desktop, select **File > Import LTspice ASC**.
2. Select the ASC file and the stock symbol directory.
3. Select **LTspice 26.1.1, Normal solver, 27 °C**.
4. Save the imported circuit as an IPES file.

To select the same profile from the command line, use `--profile` after `--symbols`.

```sh
java -cp src/modules/gecko-simulation-core/target/gecko-simulation-core-1.0-SNAPSHOT.jar gecko.core.io.importer.Main input.asc output.ipes --symbols path/to/stock-symbols --profile ltspice-26.1.1-normal-27c-v1
```

The profile applies to uncoupled linear AC inductors. It uses 0.001 Ω when the ASC omits `Rser`. An explicit expression can evaluate to zero or a positive value. The native file keeps the selected profile and the resistance setting. Open the inductor dialog to see the effective resistance or to select **Use imported default**.

The profile rejects model definitions, coupled or nonlinear devices, simulator options, and inductor attributes such as `Rpar`. A portable CIR or an ASC without a selected profile requires an explicit positive `Rser` for AC execution.

## Linear alternating current analysis

Use one `.ac dec` sweep in a `.cir` netlist or an `.asc` schematic. The sweep specifies points per decade, start frequency, and stop frequency.

Use resistors, capacitors, inductors, independent voltage and current sources, and voltage-controlled voltage sources. The controlled source accepts scalar gain or a Laplace expression. Use these native inductor laws only for AC execution.

Run the saved analysis to write the frequencies and complex results as JSON.

```sh
java -cp src/modules/gecko-simulation-core/target/gecko-simulation-core-1.0-SNAPSHOT.jar gecko.core.ac.AcMain output.ipes
```

Flat circuits use `gecko-ac-result-v1`. Hierarchical circuits use `gecko-ac-result-v2`, which adds member identities, branch currents, and signed currents into child ports. Voltage and current records include units and physical terminal bindings. The bindings include the native component identifier, device kind, occurrence path, and original port order.

This analysis is limited to the supported linear device laws and the decade sweep. Nonlinear models and text SPICE subcircuits produce conversion diagnostics. Frequency response analysis based on transient excitation and DC operating-point analysis are outside this command.

## PLECS Standalone models

Use the same command with a text `.plecs` input file.

```sh
java -cp src/modules/gecko-simulation-core/target/gecko-simulation-core-1.0-SNAPSHOT.jar gecko.core.io.importer.Main input.plecs output.ipes
```

Use scalar values for resistors, inductors, capacitors, DC and AC sources, and diodes.
Use supported constant, pulse, and sine signals to drive controlled sources.
Use a supported signal to drive a PLECS Switch.
The switch uses finite on-state and off-state resistances.
The importer reads `TimeSpan` and the legacy `StopTime` field.
For two-terminal electrical components, it preserves open-circuit and short-circuit comment modes.
It uses saved component positions and terminal connections to place labeled wires.

Review the JSON diagnostic lines if the model contains subsystems, C-scripts, or unsupported parameters or blocks.
Initialization commands and variable expressions that depend on them require manual conversion.
The importer reports unsupported controlled-source options and positions that would join different electrical nets.
