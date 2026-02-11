---
title: Buck Converter
description: Step-down DC-DC converter tutorial
---

# Tutorial 201: Buck Converter

The buck converter is the fundamental step-down DC-DC topology, efficiently converting a higher voltage to a lower voltage using PWM control.

## Overview

| | |
|---|---|
| **Difficulty** | :material-star::material-star: Intermediate |
| **Duration** | 30-40 minutes |
| **Prerequisites** | [PWM Basics](../../getting-started/pwm-basics.md) |

## Learning Objectives

By the end of this tutorial, you will:

- [x] Understand buck converter operation in CCM and DCM
- [x] Apply the volt-second balance principle
- [x] Calculate output voltage, inductor current ripple, and capacitor voltage ripple
- [x] Design a buck converter for given specifications

## Circuit Topology

```
    +Vin ──────┬──[S]──┬──[L]──┬── +Vout
               │       │       │
               │      [D]     [C]     [R] Load
               │       │       │       │
    GND ───────┴───────┴───────┴───────┴── GND
```

**Components:**

- **S** - High-side switch (MOSFET/IGBT)
- **D** - Freewheeling diode (Schottky recommended)
- **L** - Output inductor
- **C** - Output capacitor
- **R** - Load resistance

## Operating Principle

=== "Switch ON"

    - Current path: Vin → S → L → C/R → GND
    - Inductor voltage: \\(V_L = V_{in} - V_{out}\\) (positive)
    - Current increases: \\(\frac{di_L}{dt} = \frac{V_{in} - V_{out}}{L}\\)

=== "Switch OFF"

    - Current path: L → C/R → GND → D → L
    - Inductor voltage: \\(V_L = -V_{out}\\) (negative)
    - Current decreases: \\(\frac{di_L}{dt} = \frac{-V_{out}}{L}\\)

## Key Equations

### Output Voltage (Volt-Second Balance)

In steady state, the average inductor voltage is zero:

\\[
V_{out} = D \times V_{in}
\\]

!!! info "Key Insight"
    Output voltage is directly proportional to duty cycle!

### Inductor Current Ripple

\\[
\Delta I_L = \frac{V_{out} \times (1 - D)}{f_s \times L}
\\]

### Output Voltage Ripple

\\[
\Delta V_{out} = \frac{\Delta I_L}{8 \times f_s \times C}
\\]

### Critical Inductance (CCM Boundary)

\\[
L_{crit} = \frac{V_{in} \times (1-D) \times D}{2 \times f_s \times I_{out,min}}
\\]

## Design Example

### Specifications

| Parameter | Value | Unit |
|-----------|-------|------|
| Input Voltage | 48 | V |
| Output Voltage | 12 | V |
| Output Current | 5 | A |
| Switching Frequency | 100 | kHz |
| Max Voltage Ripple | 1% | |
| Max Current Ripple | 30% | |

### Step-by-Step Design

**1. Calculate Duty Cycle:**
\\[
D = \frac{V_{out}}{V_{in}} = \frac{12}{48} = 0.25
\\]

**2. Calculate Load Resistance:**
\\[
R = \frac{V_{out}}{I_{out}} = \frac{12}{5} = 2.4\,\Omega
\\]

**3. Calculate Inductance (for 30% ripple):**
\\[
\Delta I_L = 0.3 \times 5 = 1.5\,\text{A}
\\]
\\[
L = \frac{V_{out} \times (1-D)}{f_s \times \Delta I_L} = \frac{12 \times 0.75}{100k \times 1.5} = 60\,\mu\text{H}
\\]

**4. Calculate Capacitance (for 1% ripple):**
\\[
C = \frac{\Delta I_L}{8 \times f_s \times \Delta V_{out}} = \frac{1.5}{8 \times 100k \times 0.12} = 15.6\,\mu\text{F}
\\]

## Simulation

### Building the Circuit

1. Add voltage source (Vin = 48V)
2. Add switch with PWM gate signal (D = 0.25, fs = 100kHz)
3. Add freewheeling diode
4. Add inductor (L = 68µH)
5. Add capacitor (C = 22µF)
6. Add load resistor (R = 2.4Ω)
7. Connect SCOPE to measure Vout and IL

### Expected Results

| Signal | Expected Value |
|--------|----------------|
| Vout (average) | 12 V |
| Vout (ripple) | ~100 mV p-p |
| IL (average) | 5 A |
| IL (ripple) | ~1.5 A p-p |

## Exercises

!!! example "Exercise 1: Duty Cycle Variation"
    1. Vary D from 0.1 to 0.5 in steps of 0.1
    2. Record Vout for each D
    3. Verify: \\(V_{out} = D \times V_{in}\\)

!!! example "Exercise 2: CCM to DCM Transition"
    1. Increase load resistance to 24Ω
    2. Observe inductor current waveform
    3. Does IL reach zero?

!!! example "Exercise 3: Component Sizing"
    1. Double the inductance to 136µH
    2. Measure the new current ripple
    3. Compare with calculated value

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Output too low | Wrong duty cycle | Check D = Vout/Vin |
| High ripple | L or C too small | Increase component values |
| Simulation diverges | Time step too large | Reduce dt |

## Download

[:material-download: Buck Converter Circuit (buck_simple.ipes)](https://github.com/geckocircuits/geckocircuits/blob/main/resources/tutorials/2xx_dcdc_converters/201_buck_converter/buck_simple.ipes){ .md-button }

## Next Steps

- [Boost Converter](boost-converter.md) - Step-up topology
- [Buck-Boost](buck-boost.md) - Inverting topologies
- [Thermal Analysis](../thermal/loss-calculation.md) - Add loss models
