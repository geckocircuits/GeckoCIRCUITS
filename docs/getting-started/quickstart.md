---
title: Quick Start
description: Run your first simulation in 5 minutes
---

# Quick Start

Get up and running with GeckoCIRCUITS in 5 minutes.

## 1. Launch the Application

=== "Linux / WSL"

    ```bash
    ./scripts/run-gecko-linux.sh
    ```

=== "Windows"

    ```bat
    scripts\run-gecko.bat
    ```

=== "macOS"

    ```bash
    ./scripts/run-gecko-macos.sh
    ```

Or run directly with Java:

```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar
```

## 2. Open an Example Circuit

Go to **File > Open** and navigate to:

```
resources/tutorials/2xx_dcdc_converters/201_buck_converter/buck_simple.ipes
```

This opens a basic **buck converter** circuit - one of the most common DC-DC power topologies.

!!! tip "Quick Open"
    You can also pass a circuit file as a command-line argument:
    ```bash
    java -jar target/gecko-1.0-jar-with-dependencies.jar \
      resources/tutorials/2xx_dcdc_converters/201_buck_converter/buck_simple.ipes
    ```

## 3. Explore the Schematic

The circuit editor shows the buck converter topology:

- **MOSFET switch** - controlled by a PWM signal
- **Diode** - freewheeling path for inductor current
- **Inductor (L)** - energy storage element
- **Capacitor (C)** - output voltage filter
- **Resistive load (R)** - the load being powered
- **SCOPE** - oscilloscope to view waveforms

You can click on any component to see and edit its parameters.

## 4. Run the Simulation

Click the **Run** button in the toolbar or press ++f5++.

The simulation will execute for the configured duration. You'll see the progress in the status bar.

## 5. View Results

Double-click the **SCOPE** component to open the oscilloscope window. You should see:

- **Output voltage** settling to the target value
- **Inductor current** with triangular ripple waveform
- **Switch node voltage** toggling between input voltage and ground

### Oscilloscope Controls

| Action | How |
|--------|-----|
| Zoom in/out | Mouse scroll wheel |
| Pan | Click and drag |
| Auto-scale | Right-click > Auto Scale |
| Measure | Right-click > Cursor |

## 6. Modify Parameters

Try changing the duty cycle to see how it affects the output:

1. Click the **PWM** component
2. Change the duty cycle parameter
3. Re-run the simulation (++f5++)
4. Compare the new output voltage

The buck converter relationship is: **V_out = D x V_in**, where D is the duty cycle (0-1).

## What's in a Circuit File?

GeckoCIRCUITS uses `.ipes` files (gzip-compressed) that contain:

- Component placement and connections
- Parameter values for each component
- Scope/measurement configuration
- Simulation settings (time step, duration)

## Try More Examples

| Example | File | What You'll Learn |
|---------|------|-------------------|
| First steps | `1xx_getting_started/101_first_simulation/ex_1.ipes` | Basic simulation flow |
| Buck converter | `2xx_dcdc_converters/201_buck_converter/buck_simple.ipes` | DC-DC conversion |
| Boost converter | `2xx_dcdc_converters/202_boost_converter/boost_simple.ipes` | Step-up conversion |
| Three-phase inverter | `4xx_dcac_inverters/402_three_phase_inverter/inverter.ipes` | AC power generation |

All examples are in `resources/tutorials/`.

## Next Steps

- [First Simulation Tutorial](first-simulation.md) - Detailed walkthrough with explanations
- [Building Circuits](building-circuits.md) - Create your own circuits from scratch
- [User Interface Guide](interface.md) - Learn the editor and toolbar
- [Tutorials](../tutorials/index.md) - Complete tutorial library
- [Examples](../examples/index.md) - Browse 100+ application examples
