---
title: Flyback Converter Example
---

# Flyback Converter Example

Isolated DC-DC converter with transformer energy storage.

## Overview

The flyback converter provides:
- Galvanic isolation
- Multiple outputs possible
- Buck-boost functionality
- Simple topology

## Specifications

| Parameter | Value |
|-----------|-------|
| Input Voltage | 85-265V AC (rectified) |
| Output Voltage | 12V DC |
| Output Power | 65W |
| Switching Frequency | 65 kHz |
| Turns Ratio | 10:1 |
| Magnetizing Inductance | 500 µH |

## Circuit Files

- `flyback_basic.ipes` - Basic flyback operation
- `flyback_ccm.ipes` - Continuous conduction mode
- `flyback_dcm.ipes` - Discontinuous conduction mode
- `flyback_snubber.ipes` - With RCD snubber

## Theory

### Operating Principle

**Switch ON:**
- Energy stored in transformer primary (magnetizing inductance)
- Secondary diode reverse biased

**Switch OFF:**
- Energy transfers to secondary
- Output diode conducts

### Key Equations

**Voltage Conversion Ratio (CCM):**
$$\frac{V_{out}}{V_{in}} = \frac{D}{1-D} \cdot \frac{N_s}{N_p}$$

**Maximum Duty Cycle:**
$$D_{max} = \frac{V_{out} \cdot N_p/N_s}{V_{out} \cdot N_p/N_s + V_{in,min}}$$

**Peak Primary Current:**
$$I_{pk} = \frac{2 \cdot P_{out}}{V_{in,min} \cdot D_{max} \cdot \eta}$$

## Transformer Design

### Magnetizing Inductance

For CCM operation:
$$L_m > \frac{V_{in,min} \cdot D_{max}^2}{2 \cdot f_s \cdot I_{out,min}}$$

### Air Gap

Required to store energy:
$$l_g = \frac{\mu_0 \cdot N_p^2 \cdot A_e}{L_m}$$

## Snubber Design

### Leakage Inductance Spike

Voltage spike on switch turn-off:
$$V_{spike} = L_{leak} \cdot \frac{dI}{dt}$$

### RCD Snubber

Clamp voltage selection:
$$V_{clamp} \approx 1.5 \times V_{reflected}$$

## Exercises

1. **CCM vs DCM:** Compare operation at different loads
2. **Transformer Saturation:** Increase duty cycle, observe magnetizing current
3. **Snubber Effect:** Measure voltage spike with/without snubber
4. **Voltage Regulation:** Vary input voltage, verify output

## Related Resources

- [Forward Converter](forward.md)
- [Transformer Design Tutorial](../../tutorials/magnetics/transformer-design.md)
