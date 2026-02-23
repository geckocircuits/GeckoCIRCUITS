---
title: "404 - MMC Converter"
---

# 404 - Modular Multilevel Converter

Scalable multilevel topology for HVDC and FACTS applications.

## Overview

The Modular Multilevel Converter (MMC) is the state-of-the-art topology for:
- HVDC transmission (±320kV to ±525kV)
- STATCOM systems
- High-power motor drives
- Battery energy storage systems (BESS)

## Topology

### Structure

```
        +Vdc/2
           │
    ┌──────┼──────┐
    │      │      │
   SM     SM     SM   ─── Upper Arm
   SM     SM     SM       (N submodules)
   SM     SM     SM
    │      │      │
    ●──────●──────●  ─── Phase outputs (A, B, C)
    │      │      │
   SM     SM     SM   ─── Lower Arm
   SM     SM     SM       (N submodules)
   SM     SM     SM
    │      │      │
    └──────┼──────┘
           │
        -Vdc/2
```

### Submodule Types

**Half-Bridge SM:**
```
    ┬───┤S1├───┬
    │         │
    C        out
    │         │
    ┴───┤S2├───┘
```
- Output: 0 or Vc
- Unipolar capability

**Full-Bridge SM:**
```
    ┬───┤S1├───┬───┤S3├───┬
    │         │         │
    C        out        C
    │         │         │
    ┴───┤S2├───┴───┤S4├───┘
```
- Output: -Vc, 0, or +Vc
- DC fault blocking capability

## Operating Principle

### Voltage Synthesis
- N submodules per arm
- Insert/bypass SMs to create voltage steps
- Output voltage = Vdc/2 - (inserted SMs × Vc)

### Current Path
- Upper + Lower arm currents combine at output
- Circulating current flows within converter
- Arm inductors limit circulating current

## Key Equations

**Submodule Voltage:**
$$V_{SM} = \frac{V_{dc}}{N}$$

**Number of Levels:**
$$N_{levels} = N + 1$$ (per arm)

**Arm Current:**
$$i_{arm} = \frac{i_{dc}}{3} + \frac{i_{ac}}{2} + i_{circ}$$

**Energy Storage:**
$$E_{stored} = 6 \cdot N \cdot \frac{1}{2}CV_{SM}^2$$

## Control Architecture

### Hierarchical Control

1. **System Level**
   - Active/reactive power control
   - DC voltage control

2. **Converter Level**
   - Circulating current control
   - Capacitor voltage balancing (average)

3. **Arm Level**
   - Individual SM capacitor balancing
   - Sorting algorithm (NLM or PWM)

### Modulation Methods

**Nearest Level Modulation (NLM):**
- Round reference to nearest level
- Very low switching frequency
- Used in HVDC

**Phase-Shifted Carrier PWM:**
- Higher effective switching frequency
- Better harmonic performance
- Used in motor drives

## Design Parameters

| Parameter | HVDC | Motor Drive |
|-----------|------|-------------|
| DC Voltage | ±320-525kV | 3-35kV |
| Submodules/Arm | 200-400 | 10-40 |
| SM Capacitance | 8-15mF | 2-5mF |
| Arm Inductance | 50-100mH | 1-10mH |
| SM Type | Half-bridge | Full-bridge |

## Simulation Exercises

1. Build 5-level MMC (N=4 per arm)
2. Implement capacitor balancing
3. Compare NLM vs PS-PWM
4. Analyze circulating currents
5. Simulate DC fault response

## Applications

- HVDC point-to-point links
- Multi-terminal HVDC grids
- Offshore wind connection
- STATCOM for grid support
- Medium-voltage drives
- Railway interties

## Related Resources

- [403 - NPC Inverter](npc-inverter.md)
- [402 - Three-Phase Inverter](three-phase.md)
