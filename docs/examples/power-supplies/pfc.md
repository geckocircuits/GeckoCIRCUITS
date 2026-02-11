---
title: PFC Converter Examples
---

# PFC Converter Examples

Power Factor Correction circuits for AC-DC conversion.

## Overview

PFC converters shape input current to be sinusoidal, achieving:
- Near-unity power factor (>0.99)
- Low THD (<5%)
- Regulatory compliance (IEC 61000-3-2)

## Circuit Files

- `pfc_boost_ccm.ipes` - CCM boost PFC
- `pfc_boost_crm.ipes` - Critical conduction mode
- `pfc_interleaved.ipes` - Two-phase interleaved
- `pfc_bridgeless.ipes` - Totem-pole bridgeless

## Specifications

### Single-Phase Boost PFC

| Parameter | Value |
|-----------|-------|
| Input Voltage | 85-265V AC |
| Output Voltage | 400V DC |
| Output Power | 1 kW |
| Switching Frequency | 65 kHz (CCM) |
| Inductor | 330 µH |
| Power Factor | >0.99 |

## Theory

### Boost PFC Operation

Input current follows rectified voltage shape:
$$i_{in}(t) = I_{pk} \cdot |\sin(\omega t)|$$

Duty cycle varies with instantaneous voltage:
$$D(t) = 1 - \frac{|v_{in}(t)|}{V_{out}}$$

### Control Methods

**Average Current Mode:**
- Current loop tracks sinusoidal reference
- Excellent THD performance
- Bandwidth limited by 2× line frequency

**Critical Conduction Mode (CRM):**
- Variable frequency
- Zero current turn-on
- Higher peak currents

## Interleaved PFC

Benefits:
- Ripple cancellation at input
- Reduced capacitor current
- Smaller inductors
- Distributed thermal load

Phase shift between channels: 360°/N

## Bridgeless Topologies

### Totem-Pole PFC

- Eliminates input bridge
- Higher efficiency (~0.5% gain)
- Requires GaN/SiC for CRM operation
- Bidirectional capable

## Exercises

1. **Power Factor Measurement:** Verify PF and THD
2. **CCM vs CRM:** Compare inductor current waveforms
3. **Interleaving Effect:** Observe ripple cancellation
4. **Efficiency:** Compare topologies

## Related Resources

- [302 - PFC Basics Tutorial](../../tutorials/acdc/pfc-basics.md)
- [Vienna Rectifier Tutorial](../../tutorials/acdc/vienna-rectifier.md)
