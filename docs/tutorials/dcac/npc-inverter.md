---
title: "403 - NPC Multilevel Inverter"
---

# 403 - NPC Multilevel Inverter

Neutral-Point-Clamped three-level inverter topology.

## Overview

The NPC inverter produces three voltage levels per phase, offering:
- Reduced harmonic distortion
- Lower dv/dt stress
- Smaller output filters
- Higher efficiency at medium voltage

## Topology

### Single-Phase Leg

```
      +Vdc/2 ───┬───┤S1├───┬───────────┐
                │         │           │
               C1        D1►          │
                │         │           │
      Neutral ──┼─────────┼───┤S2├────┼─── Output
                │         │           │
               C2        ◄D2          │
                │         │           │
      -Vdc/2 ───┴───┤S3├───┴───┤S4├───┘
```

### Switching States

| State | S1 | S2 | S3 | S4 | Output |
|-------|----|----|----|----|--------|
| P | ON | ON | OFF | OFF | +Vdc/2 |
| O | OFF | ON | ON | OFF | 0 |
| N | OFF | OFF | ON | ON | -Vdc/2 |

## Key Advantages

### Voltage Stress
- Each switch blocks only Vdc/2
- Enables use of lower voltage devices
- Higher reliability

### Output Quality
- 3-level output waveform
- Lower dv/dt (smaller steps)
- Reduced EMI emissions
- Smaller output filter

## Neutral Point Balancing

### The Challenge
- Capacitor voltage imbalance causes distortion
- Imbalance occurs with non-unity power factor
- Requires active control strategy

### Balancing Methods

1. **Carrier-based PWM with offset**
   - Add zero-sequence component
   - Simple implementation

2. **Space Vector PWM**
   - Select redundant vectors strategically
   - Better utilization

3. **Direct voltage control**
   - Measure capacitor voltages
   - Adjust modulation in real-time

## Key Equations

**Output Voltage Levels:**
$$V_{out} \in \{-V_{dc}/2, 0, +V_{dc}/2\}$$

**Modulation Index:**
$$m_a = \frac{V_{ref,peak}}{V_{dc}/2}$$

**Switching Frequency per Device:**
- Lower than 2-level (each switch operates part of cycle)
- Reduces switching losses

## Design Parameters

| Parameter | Typical Value |
|-----------|---------------|
| DC Bus | 600-1200V |
| Split Capacitors | 2× matched |
| Clamping Diodes | Fast recovery |
| Switching Freq | 2-10 kHz |

## Applications

- Medium voltage drives (2.3-6.6 kV)
- Grid-tied inverters (MW scale)
- HVDC converters
- Railway traction

## Simulation Exercises

1. Compare 2-level vs 3-level output spectrum
2. Implement neutral point balancing
3. Analyze capacitor voltage ripple
4. Design for different power factors

## Related Resources

- [402 - Three-Phase Inverter](three-phase.md)
- [MMC Converter](mmc-converter.md) - Higher level counts
