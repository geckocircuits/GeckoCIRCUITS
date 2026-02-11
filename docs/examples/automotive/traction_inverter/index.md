---
title: Traction Inverter
---

# Traction Inverter

## Overview

The traction inverter converts DC from the battery to three-phase AC to drive the electric motor. High efficiency, power density, and reliability are critical for vehicle performance and range.

**Difficulty:** Advanced

**Status:** Placeholder

## Specifications

| Parameter | Value | Unit |
|-----------|-------|------|
| DC Voltage | 400 or 800 | VDC |
| Peak Power | 150-350 | kW |
| Continuous Power | 100-250 | kW |
| Peak Efficiency | >97% | - |
| Switching Frequency | 8-20 | kHz |
| Power Density | >30 | kW/L |

## Topology

### Standard 2-Level VSI

```
         Battery
      +Vdc    -Vdc
        │      │
    ┌───┴──────┴───┐
    │   Inverter   │
    │  ┌──┬──┬──┐  │
    │  │  │  │  │  │
    │  A  B  C  │  │
    └──┼──┼──┼──┴──┘
       │  │  │
    ┌──┴──┴──┴──┐
    │   PMSM    │
    │  Motor    │
    └───────────┘
```

### 800V Systems

Benefits of 800V vs 400V:
- Half the current for same power
- Smaller cables, connectors
- Faster DC charging
- Higher efficiency

Challenges:
- Component availability
- Increased insulation requirements

## SiC vs Si-IGBT

| Parameter | Si-IGBT | SiC MOSFET |
|-----------|---------|------------|
| Switching loss | Higher | 70% lower |
| Switching freq | 8-12 kHz | 15-25 kHz |
| Efficiency | 95-96% | 97-98% |
| Cost | Lower | Higher (decreasing) |
| Thermal | 175°C Tj | 200°C+ Tj |

## Control: Field-Oriented Control (FOC)

```
Speed Ref ─►[PI]─► Iq_ref ─►[PI]─► Vq ─►[Inverse]─► Va,Vb,Vc ─►[SVPWM]─► PWM
                      │              │     Park                    │
              Id_ref=0│              │                             │
                ─────►[PI]─► Vd ─────┘                             │
                      │                                            │
                      └◄─────[Park]◄─────[Clarke]◄─────[Current]◄──┘
                              dq            αβ          Measure
                               │
                          θe ──┘ (from resolver/encoder)
```

### Key Control Features

| Feature | Purpose |
|---------|---------|
| MTPA | Maximum torque per ampere |
| Field weakening | Extend speed range |
| Dead-time compensation | Reduce distortion |
| Active damping | Suppress resonance |

## Thermal Design

### Cooling Requirements

```
P_loss = P_cond + P_sw ≈ 3-5% of P_out

For 200kW inverter:
  P_loss ≈ 6-10 kW
  Requires liquid cooling with ~50°C ΔT
```

### Cooling Options

| Method | Heat Dissipation | Use Case |
|--------|------------------|----------|
| Air cooled | <5 kW | Low power |
| Liquid (glycol) | 5-15 kW | Standard EV |
| Direct oil cooling | >15 kW | High performance |
| Two-phase | Highest | Racing/aerospace |

## PMSM Motor Parameters

| Parameter | Symbol | Typical Value |
|-----------|--------|---------------|
| Pole pairs | p | 4-8 |
| Stator resistance | Rs | 5-20 mΩ |
| d-axis inductance | Ld | 100-500 μH |
| q-axis inductance | Lq | 100-500 μH |
| PM flux linkage | λm | 50-150 mWb |
| Max speed | nmax | 12000-18000 RPM |

## Exercises

### Exercise 1: Basic Inverter
1. Build 400V, 100kW 2-level VSI
2. Apply SPWM modulation
3. Measure output THD

### Exercise 2: FOC Implementation
1. Add PMSM model
2. Implement d-q current control
3. Test torque step response

### Exercise 3: Efficiency Map
1. Sweep speed and torque
2. Calculate losses at each point
3. Generate efficiency map

## Circuit Files

> **Status:** Placeholder
> - `traction_inverter_basic.ipes` - Open-loop SPWM
> - `traction_inverter_foc.ipes` - With FOC control
> - `traction_pmsm_system.ipes` - Complete drivetrain

---
*Placeholder - Details to be added*
