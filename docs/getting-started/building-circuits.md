---
title: Building Circuits
description: Learn to create circuits from scratch in GeckoCIRCUITS
---

# Building Circuits

Learn how to create power electronics circuits from scratch in the GeckoCIRCUITS editor.

**Duration:** 20 minutes

**Prerequisites:** [First Simulation](first-simulation.md)

## Overview

Building a circuit involves four steps:

1. **Place components** on the schematic
2. **Wire them together**
3. **Set parameters** for each component
4. **Add measurement** points (SCOPE)

## Step 1: Start a New Circuit

Go to **File > New** (++ctrl+n++) to create an empty schematic.

## Step 2: Place Components

### Adding Components

1. Select a component from the component palette (left panel or menu)
2. Click on the schematic to place it
3. The component appears at the clicked location

### Component Categories

Power electronics circuits typically need:

| Category | What to Place | Why |
|----------|--------------|-----|
| Sources | Voltage source | Provides input power |
| Switches | MOSFET, IGBT, Diode | Switching elements |
| Passive | L, C, R | Energy storage and filtering |
| Ground | Ground symbol | **Required** - voltage reference |
| Control | PWM, Constant | Switch gate signals |
| Display | SCOPE | View simulation results |

### Rotating and Flipping

- **Right-click** a component for rotation options
- Rotate to align terminals for clean wiring

## Step 3: Wire Components

### Making Connections

1. Click on a component **terminal** (small square)
2. Click to add wire waypoints (corners)
3. Click the destination terminal to complete the connection

### Wiring Tips

!!! tip "Clean Schematics"
    - Use horizontal and vertical wires only
    - Align components on the grid
    - Keep signal flow left-to-right, top-to-bottom

### Common Wiring Mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Missing ground | Simulation fails | Add ground to return path |
| Open node | "Floating node" warning | Connect all terminals |
| Shorted source | Infinite current | Add series resistance/inductance |

## Step 4: Set Parameters

Double-click each component to set its values.

### Example: Buck Converter Parameters

| Component | Parameter | Example Value |
|-----------|-----------|---------------|
| V_source | Voltage | 48 V |
| MOSFET | (default) | - |
| Diode | (default) | - |
| Inductor | L | 100 uH |
| Capacitor | C | 47 uF |
| Resistor | R | 10 Ohm |
| PWM | Frequency | 100 kHz |
| PWM | Duty Cycle | 0.25 |

### Unit Prefixes

GeckoCIRCUITS recognizes standard SI prefixes:

| Prefix | Symbol | Value |
|--------|--------|-------|
| mega | M | 10^6 |
| kilo | k | 10^3 |
| milli | m | 10^-3 |
| micro | u | 10^-6 |
| nano | n | 10^-9 |
| pico | p | 10^-12 |

## Step 5: Add Measurements

### Placing a SCOPE

1. Add a **SCOPE** block from the control components
2. Connect signals you want to measure to the scope inputs
3. Each input becomes a channel in the oscilloscope

### What to Measure

For a typical power converter, measure:

- **Output voltage** across the load
- **Inductor current** through the inductor
- **Switch node voltage** at the switching point

### Using Voltage and Current Probes

- **Voltage probe** - Measures voltage between two nodes
- **Current probe** - Measures current through a component (place in series)

## Example: Build a Buck Converter

Follow these steps to build a basic buck converter from scratch:

### Circuit Topology

```
    Vin ──[MOSFET]──●──[L]──●── Vout
                    │        │
                 [Diode]   [C]  [R]
                    │        │    │
                   GND ──────●────┘
```

### Step-by-Step

1. **Place Vin** - DC voltage source, set to 48V
2. **Place MOSFET** - Connect drain to Vin positive
3. **Place Diode** - Cathode to MOSFET source, anode to ground
4. **Place Inductor** - From switch node to output
5. **Place Capacitor** - From output to ground
6. **Place Resistor** - From output to ground (load)
7. **Connect Ground** - To source negative, diode anode, capacitor, resistor
8. **Add PWM** - Connect to MOSFET gate (100 kHz, D=0.25)
9. **Add SCOPE** - Connect to output voltage node

### Verify Before Running

Checklist:

- [ ] All nodes connected (no floating nodes)
- [ ] Ground present
- [ ] Source has a return path
- [ ] Switch has a control signal
- [ ] SCOPE connected to measurement points
- [ ] Simulation time step appropriate (< 0.1 us for 100 kHz)

## Simulation Settings

Before running, set appropriate simulation parameters:

| Parameter | Value | Why |
|-----------|-------|-----|
| Total time | 1 ms | ~100 switching periods |
| Time step | 50 ns | 200 steps per period |
| Solver | Backward Euler | Stable for switching circuits |

## Next Steps

- [PWM Basics](pwm-basics.md) - Understand PWM signals for switch control
- [Running Simulations](running-simulations.md) - Simulation settings and modes
- [Buck Converter Tutorial](../tutorials/dcdc/buck-converter.md) - Full design with theory
