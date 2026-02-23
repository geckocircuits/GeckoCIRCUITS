---
title: "401 - Single-Phase Inverter"
---

# 401 - Single-Phase Inverter

PWM inverter fundamentals for DC-AC conversion.

## Overview

Single-phase inverters convert DC to AC using PWM switching strategies, essential for:
- UPS systems
- Solar inverters (residential)
- Motor drives (single-phase)
- AC power supplies

## Topologies

### Half-Bridge Inverter

```
       +Vdc/2
         │
         ├───┤S1├───┬─── AC out
         │          │
        Cdc        Load
         │          │
         ├───┤S2├───┘
         │
       -Vdc/2
```

**Output:** ±Vdc/2

### Full-Bridge (H-Bridge) Inverter

```
      +Vdc ───┬────────┬───
              │        │
             S1       S3
              │        │
              ├─ Load ─┤
              │        │
             S2       S4
              │        │
      GND ───┴────────┴───
```

**Output:** ±Vdc

## PWM Strategies

### Bipolar PWM

- Compare sine reference to triangle carrier
- S1-S4 switch together, S2-S3 switch together
- Output levels: +Vdc, -Vdc

### Unipolar PWM

- Two reference signals (positive and negative)
- Each leg switched independently
- Output levels: +Vdc, 0, -Vdc
- Lower harmonic content, reduced filter size

## Key Equations

**Modulation Index:**
$$m_a = \frac{V_{ref,peak}}{V_{carrier,peak}}$$

**Fundamental Output Voltage (linear region):**
$$V_{out,1} = m_a \cdot V_{dc}$$ (full-bridge)

**THD Calculation:**
$$THD = \frac{\sqrt{V_2^2 + V_3^2 + ... + V_n^2}}{V_1}$$

## Design Parameters

| Parameter | Typical Value |
|-----------|---------------|
| DC Bus | 400V (from PFC) |
| Switching Freq | 10-50 kHz |
| Output Filter L | 1-5 mH |
| Output Filter C | 2-10 µF |
| Modulation Index | 0.8-0.95 |

## Simulation Exercises

1. Compare bipolar vs unipolar PWM
2. Observe dead-time effects
3. Analyze output spectrum (FFT)
4. Design LC output filter

## Related Resources

- [402 - Three-Phase Inverter](three-phase.md)
- [403 - NPC Multilevel](npc-inverter.md)
