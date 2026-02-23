---
title: Buck Converter Example
---

# Buck Converter Example

Step-down DC-DC converter with voltage mode control.

## Overview

The buck converter is the fundamental step-down topology, converting a higher DC voltage to a lower DC voltage with high efficiency.

## Specifications

| Parameter | Value |
|-----------|-------|
| Input Voltage | 48V DC |
| Output Voltage | 12V DC |
| Output Power | 100W |
| Switching Frequency | 100 kHz |
| Inductor | 47 µH |
| Output Capacitor | 220 µF |

## Circuit Files

- `buck_basic.ipes` - Open-loop buck converter
- `buck_voltage_mode.ipes` - Voltage mode control
- `buck_current_mode.ipes` - Peak current mode control

## Theory

### Operating Principle

**Switch ON (0 < t < DTs):**
- Inductor current ramps up
- Energy stored in inductor

**Switch OFF (DTs < t < Ts):**
- Inductor current ramps down through diode
- Energy transferred to output

### Key Equations

**Voltage Conversion Ratio:**
$$\frac{V_{out}}{V_{in}} = D$$

**Inductor Current Ripple:**
$$\Delta I_L = \frac{V_{in} - V_{out}}{L} \cdot D \cdot T_s$$

**Output Voltage Ripple:**
$$\Delta V_{out} = \frac{\Delta I_L}{8 \cdot f_s \cdot C}$$

## Simulation Results

### Steady-State Waveforms

Expected measurements:
- Output voltage: 12.0V ± 0.1V
- Inductor current ripple: ~2A peak-to-peak
- Output voltage ripple: <50mV

### Startup Transient

Soft-start implementation limits inrush current.

## Exercises

1. **Duty Cycle Sweep:** Vary D from 0.2 to 0.8, measure Vout
2. **Load Step Response:** Apply 50% load step, measure recovery time
3. **CCM/DCM Boundary:** Find minimum load for CCM operation
4. **Efficiency Analysis:** Enable loss models, measure efficiency vs load

## Related Resources

- [201 - Buck Converter Tutorial](../../tutorials/dcdc/buck-converter.md)
- [Boost Converter](boost.md)
