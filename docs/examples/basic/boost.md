---
title: Boost Converter Example
---

# Boost Converter Example

Step-up DC-DC converter with current mode control.

## Overview

The boost converter steps up DC voltage, commonly used in:
- Battery-powered devices
- Solar MPPT front-end
- PFC rectifiers
- LED drivers

## Specifications

| Parameter | Value |
|-----------|-------|
| Input Voltage | 12V DC |
| Output Voltage | 48V DC |
| Output Power | 100W |
| Switching Frequency | 100 kHz |
| Inductor | 68 µH |
| Output Capacitor | 100 µF |

## Circuit Files

- `boost_basic.ipes` - Open-loop boost converter
- `boost_current_mode.ipes` - Average current mode control
- `boost_pfc.ipes` - PFC application

## Theory

### Operating Principle

**Switch ON:**
- Inductor charges from input
- Output capacitor supplies load

**Switch OFF:**
- Inductor energy transfers to output
- Voltage steps up

### Key Equations

**Voltage Conversion Ratio:**
$$\frac{V_{out}}{V_{in}} = \frac{1}{1-D}$$

**Right-Half-Plane Zero:**
$$f_{RHPZ} = \frac{(1-D)^2 R_{load}}{2\pi L}$$

**Minimum Inductance (CCM):**
$$L_{min} = \frac{D(1-D)^2 R_{load}}{2 f_s}$$

## Design Considerations

### Right-Half-Plane Zero (RHPZ)

The RHPZ limits control bandwidth:
- Cannot cross over above fRHPZ
- Typically limits bandwidth to fRHPZ/5

### Input Current

Continuous input current (advantage for battery/solar):
$$I_{in,avg} = \frac{I_{out}}{1-D}$$

## Exercises

1. **Voltage Gain:** Verify Vout vs D relationship
2. **RHPZ Effect:** Measure loop gain, observe phase drop
3. **Current Ripple:** Compare with buck converter
4. **Soft-Start:** Implement duty cycle ramping

## Related Resources

- [202 - Boost Converter Tutorial](../../tutorials/dcdc/boost-converter.md)
- [Buck Converter](buck.md)
- [PFC Converters](../power-supplies/pfc.md)
