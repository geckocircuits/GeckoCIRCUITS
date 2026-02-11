---
title: Tutorial 102: Basic Circuits
---

# Tutorial 102: Basic Circuits

## Overview

Learn how to build circuits from scratch in GeckoCIRCUITS. This tutorial covers component selection, placement, wiring, and parameter configuration.

**Level:** Beginner (1/3)

**Duration:** 25-30 minutes

**Series:** Getting Started

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Navigate the component library
- [ ] Place and orient components on the schematic
- [ ] Connect components with wires
- [ ] Configure component parameters
- [ ] Add measurement probes (SCOPE connections)
- [ ] Build a complete RLC circuit

## Prerequisites

- Complete [Tutorial 101: First Simulation](../101_first_simulation/)
- Basic understanding of electrical circuits (R, L, C components)

## Materials

| File | Description |
|------|-------------|
| `ex_2.ipes` | Completed reference circuit |
| `GeckoCIRCUITS_tutorial_basics_EN.pdf` | Detailed PDF guide |

## Component Library Overview

GeckoCIRCUITS organizes components into categories:

### POWER Components (Electrical)
| Component | Symbol | Description |
|-----------|--------|-------------|
| Resistor | R | Ohmic resistance |
| Inductor | L | Inductance with optional resistance |
| Capacitor | C | Capacitance |
| Voltage Source | V | DC or AC voltage |
| Current Source | I | DC or AC current |
| Diode | D | Semiconductor diode |
| Switch/IGBT | S | Controllable switch |

### CONTROL Components (Signal)
| Component | Symbol | Description |
|-----------|--------|-------------|
| Signal Generator | SIG | Sine, square, triangle waves |
| Comparator | CMP | Compare two signals |
| Gain | K | Multiply by constant |
| Sum | SUM | Add/subtract signals |
| PI Controller | PI | Proportional-integral control |

### THERM Components (Thermal)
| Component | Description |
|-----------|-------------|
| Thermal Resistance | Heat path modeling |
| Heat Source | Thermal current input |

### Measurement (SCOPE)
| Component | Description |
|-----------|-------------|
| SCOPE | Multi-channel oscilloscope |
| DISPLAY | Numeric value display |

## Building Your First Circuit

### Step 1: Create a New File

1. **File > New** (or Ctrl+N)
2. A blank canvas appears

### Step 2: Add a Voltage Source

1. In the left panel, expand **POWER** category
2. Click on **Voltage Source (V)**
3. Move cursor to the canvas
4. Click to place the component
5. Right-click to finish placement (or press Escape)

### Step 3: Configure the Voltage Source

1. Double-click on the voltage source
2. Set parameters:
   - **Amplitude:** 10 V
   - **Frequency:** 50 Hz (for AC)
   - **DC Offset:** 0 V
3. Click **OK**

### Step 4: Add a Resistor

1. Select **Resistor (R)** from POWER components
2. Click to place it to the right of the voltage source
3. Double-click to set: **R = 10 Ω**

### Step 5: Add an Inductor

1. Select **Inductor (L)** from POWER components
2. Place it in series with the resistor
3. Double-click to set: **L = 10 mH**

### Step 6: Connect Components with Wires

1. Click on an output terminal of the voltage source
2. Drag to the input terminal of the resistor
3. Click to create connection points
4. Repeat for all connections
5. Create a return path back to the voltage source

> **Tip:** Use Ctrl+click to create corner points in wires

### Step 7: Add Ground Reference

1. One node must be designated as ground (0V reference)
2. Select **Ground** symbol
3. Place it at the bottom node of your circuit

### Step 8: Add Oscilloscope

1. Select **SCOPE** from component library
2. Place it near your circuit
3. Connect scope inputs to nodes you want to measure:
   - Channel 1: Voltage across resistor
   - Channel 2: Current through inductor (use current probe)

## Component Orientation

### Rotating Components
- **Before placing:** Press R to rotate 90°
- **After placing:** Select component, then Edit > Rotate (or R key)

### Mirroring Components
- **Edit > Mirror** or press M key

### Grid Alignment
- Components snap to grid automatically
- Use **View > Grid** to show/hide grid
- **View > Snap to Grid** toggles snapping

## Wiring Best Practices

### Good Wiring
```
    ┌──[R]──[L]──┐
    │            │
   [V]          [C]
    │            │
    └────────────┘
        GND
```

### Avoid
- Overlapping wires (hard to follow)
- Very long wire runs (use labels instead)
- Wires crossing components

### Using Labels

For complex circuits, use labels instead of long wires:
1. Right-click on a wire
2. Select **Add Label**
3. Enter label name (e.g., "Vout")
4. Any wires with the same label are electrically connected

## Circuit: RLC Series Example

Build this circuit step by step:

```
         R=10Ω        L=10mH
    +───[===]───────[oooo]───┐
    │                        │
   [ ] V = 10V               │
   [ ] f = 50Hz         C=100μF
    │                    │ │
    └────────────────────┴─┘
                        GND
```

### Parameters

| Component | Parameter | Value | Unit |
|-----------|-----------|-------|------|
| V.1 | Amplitude | 10 | V |
| V.1 | Frequency | 50 | Hz |
| R.1 | Resistance | 10 | Ω |
| L.1 | Inductance | 10 | mH |
| C.1 | Capacitance | 100 | μF |

### Expected Behavior

At 50 Hz:
- XL = 2πfL = 2π × 50 × 0.01 = 3.14 Ω
- XC = 1/(2πfC) = 1/(2π × 50 × 100μ) = 31.8 Ω
- Z = √(R² + (XL-XC)²) = √(100 + 821) ≈ 30 Ω
- I = V/Z = 10/30 ≈ 0.33 A

## Simulation Settings

1. **Simulation > Settings** (or F9)
2. Configure:
   - **Simulation time:** 0.1 s (5 cycles at 50 Hz)
   - **Time step:** automatic or 10 μs
   - **Solver:** Trapezoidal (default)
3. Click **OK**
4. Run simulation (F5)

## Checkpoint

At this point, your circuit should:
- [ ] Have all 4 components placed (V, R, L, C)
- [ ] Be fully wired in a closed loop
- [ ] Have a ground reference
- [ ] Show waveforms in the SCOPE after simulation

## Common Mistakes

| Mistake | Symptom | Fix |
|---------|---------|-----|
| Open circuit | Simulation error | Check all connections |
| No ground | Simulation error | Add ground symbol |
| Wrong polarity | Unexpected results | Check component orientation |
| Missing scope connection | No waveform | Wire to SCOPE input |

## Exercises

### Exercise 1: Modify Component Values
1. Change R from 10Ω to 100Ω
2. Re-run the simulation
3. **Question:** How does the current change?

### Exercise 2: Resonance Frequency
1. Calculate the resonant frequency: fr = 1/(2π√LC)
2. Change the source frequency to fr
3. **Question:** What happens to the impedance?

### Exercise 3: Add Another Component
1. Add a second resistor in parallel with C
2. Experiment with its value
3. **Question:** How does this affect the circuit behavior?

### Exercise 4: Build from Scratch
1. Create a new file
2. Build the circuit without looking at `ex_2.ipes`
3. Compare your results with the reference

## Summary

In this tutorial, you learned:
1. Navigating the component library
2. Placing and configuring components
3. Wiring circuits correctly
4. Setting up measurements with SCOPE
5. Running simulations and checking results

## Next Steps

Continue your learning with:
- **Next Tutorial:** [103 - PWM Basics](../103_pwm_basics/) - Learn pulse-width modulation
- **Example:** [Buck Converter](../examples/basic_topologies/buck_converter/) - Apply your skills

## Additional Resources

- `GeckoCIRCUITS_tutorial_basics_EN.pdf` - Complete PDF tutorial
- Help > Component Reference - Detailed component documentation

---
*Tutorial Version: 1.0*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
