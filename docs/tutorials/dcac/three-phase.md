---
title: "402 - Three-Phase Inverter"
---

# 402 - Three-Phase Inverter

Three-phase voltage source inverter (VSI) for motor drives and grid-tied applications.

## Overview

Three-phase VSIs are the backbone of:
- AC motor drives (industrial, EV)
- Grid-tied solar/wind inverters
- Active power filters
- STATCOM/FACTS devices

## Topology

### Two-Level VSI

```
      +Vdc ───┬────────┬────────┬───
              │        │        │
             S1       S3       S5
              │        │        │
              A        B        C ─── Three-phase
              │        │        │     output
             S2       S4       S6
              │        │        │
      GND ───┴────────┴────────┴───
```

## Space Vector PWM (SVPWM)

### Switching States

| State | S1 | S3 | S5 | Vector |
|-------|----|----|----| -------|
| V0 | 0 | 0 | 0 | Zero |
| V1 | 1 | 0 | 0 | Active |
| V2 | 1 | 1 | 0 | Active |
| V3 | 0 | 1 | 0 | Active |
| V4 | 0 | 1 | 1 | Active |
| V5 | 0 | 0 | 1 | Active |
| V6 | 1 | 0 | 1 | Active |
| V7 | 1 | 1 | 1 | Zero |

### SVPWM Algorithm

1. Calculate reference vector in α-β frame
2. Identify sector (1-6)
3. Calculate dwell times for adjacent vectors
4. Generate switching sequence

## Key Equations

**DC Bus Utilization:**
- Sine PWM: $$V_{LL,max} = \frac{\sqrt{3}}{2} V_{dc} \approx 0.866 \cdot V_{dc}$$
- SVPWM: $$V_{LL,max} = \frac{V_{dc}}{\sqrt{3}} \cdot \frac{2}{\sqrt{3}} = \frac{2}{\sqrt{3}} \cdot 0.5 \cdot V_{dc}$$

**15% more DC bus utilization with SVPWM**

**Clarke Transform:**
$$\begin{bmatrix} v_\alpha \\ v_\beta \end{bmatrix} = \frac{2}{3}\begin{bmatrix} 1 & -\frac{1}{2} & -\frac{1}{2} \\ 0 & \frac{\sqrt{3}}{2} & -\frac{\sqrt{3}}{2} \end{bmatrix} \begin{bmatrix} v_a \\ v_b \\ v_c \end{bmatrix}$$

## Design Parameters

| Parameter | Motor Drive | Grid-Tied |
|-----------|-------------|-----------|
| DC Bus | 300-800V | 700-1500V |
| Switching Freq | 5-20 kHz | 10-50 kHz |
| Dead Time | 1-3 µs | 0.5-2 µs |
| Output Filter | Motor inductance | LCL filter |

## Control Architecture

```
                    ┌─────────────┐
Speed ref ─────────►│   Speed     │
                    │   Control   │
Speed fb ──────────►│   (PI)      │
                    └──────┬──────┘
                           │ iq*
                    ┌──────▼──────┐
                    │   Current   │
id*, iq* ──────────►│   Control   │───► PWM
                    │   (PI)      │
id, iq fb ─────────►│             │
                    └─────────────┘
```

## Simulation Exercises

1. Implement SVPWM modulator
2. Compare with sinusoidal PWM
3. Observe common-mode voltage
4. Implement dead-time compensation

## Related Resources

- [401 - Single-Phase Inverter](single-phase.md)
- [403 - NPC Multilevel](npc-inverter.md)
- [MMC Converter](mmc-converter.md)
