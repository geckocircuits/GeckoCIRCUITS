---
title: First Simulation
description: Step-by-step walkthrough of your first GeckoCIRCUITS simulation
---

# Your First Simulation

This tutorial walks you through opening, running, and analyzing a circuit simulation in GeckoCIRCUITS.

**Duration:** 15 minutes

**Prerequisites:** [Installation](installation.md) complete

## Step 1: Launch GeckoCIRCUITS

Start the application using your platform's launcher:

```bash
./scripts/run-gecko-linux.sh    # Linux/WSL
./scripts/run-gecko-macos.sh    # macOS
scripts\run-gecko.bat           # Windows
```

The main window opens with an empty schematic editor.

## Step 2: Open the Example Circuit

Go to **File > Open** and navigate to:

```
resources/tutorials/1xx_getting_started/101_first_simulation/ex_1.ipes
```

This loads a basic power converter circuit. You should see:

- **Power components** (top) - Switches, diodes, passive components
- **Control blocks** (bottom) - PWM generators, signal sources
- **SCOPE blocks** - Oscilloscope measurement points

## Step 3: Understand the Circuit

Before running, take a moment to understand the schematic:

### Components You'll See

| Component | Symbol | Role |
|-----------|--------|------|
| DC voltage source | Circle with +/- | Provides input power |
| MOSFET switch | Triangle with gate | Controlled switching element |
| Diode | Triangle with bar | Freewheeling current path |
| Inductor (L) | Coil symbol | Stores energy, smooths current |
| Capacitor (C) | Parallel plates | Filters output voltage |
| Resistor (R) | Zigzag | Load |
| Ground | Three lines | Reference node |
| SCOPE | Rectangle | Measurement display |

### Viewing Component Parameters

Click on any component to see its dialog with parameters like:

- Resistance, inductance, capacitance values
- Voltage/current source settings
- Switch timing and control signals

## Step 4: Configure Simulation

Open the simulation settings via **Simulation > Settings** or click the settings icon.

Key parameters:

| Parameter | Meaning | Typical Value |
|-----------|---------|---------------|
| **Total time (T)** | How long to simulate | 10 ms |
| **Time step (dt)** | Computation interval | 1 us |
| **Solver** | Numerical method | Backward Euler |

!!! tip "Time Step Rule"
    The time step should be **100x smaller** than the switching period.
    For 100 kHz switching: dt < 0.1 us

## Step 5: Run the Simulation

Click **Run** (play button) or press ++f5++.

Watch the progress bar - the simulation computes all circuit variables at each time step.

Typical simulation time: **1-5 seconds** for simple circuits.

## Step 6: View Results

Double-click any **SCOPE** block to open the oscilloscope window.

### Reading the Oscilloscope

The scope displays voltage and current waveforms over time:

- **X-axis** - Time (seconds)
- **Y-axis** - Voltage (V) or Current (A)
- **Multiple channels** - Different colors for different signals

### Scope Navigation

| Action | Control |
|--------|---------|
| Zoom in | Scroll wheel up |
| Zoom out | Scroll wheel down |
| Pan | Click + drag |
| Auto-scale all axes | Right-click > Auto Scale |
| Add cursor | Right-click > Cursor |

## Step 7: Analyze the Results

For a buck converter, you should observe:

1. **Output voltage** - Settles to V_out = D x V_in
2. **Inductor current** - Triangular ripple waveform
3. **Switch voltage** - Rectangular pulses between V_in and 0

### What to Check

- Does the output reach steady state?
- Is the ripple within acceptable limits?
- Are there any unexpected spikes or oscillations?

## Step 8: Experiment

Try modifying the circuit:

1. **Change duty cycle** - Click the PWM block, adjust D
2. **Change load** - Click the resistor, change R
3. **Change inductance** - Click the inductor, change L
4. Re-run and compare results

!!! info "Undo"
    Use **Edit > Undo** (++ctrl+z++) to revert changes.

## What You've Learned

- How to open and navigate circuit files
- How to configure and run simulations
- How to view and interpret waveforms
- How to modify circuit parameters

## Common Issues

| Problem | Solution |
|---------|----------|
| Simulation doesn't start | Check all nodes are connected, ground exists |
| Output is zero | Check switch control signal, duty cycle > 0 |
| Waveform oscillates wildly | Reduce time step, check component values |
| Scope shows nothing | Double-click scope to open, check channel assignments |

## Next Steps

- [Building Circuits](building-circuits.md) - Create your own circuits from scratch
- [PWM Basics](pwm-basics.md) - Understand pulse-width modulation
- [Buck Converter Tutorial](../tutorials/dcdc/buck-converter.md) - Detailed theory and design
