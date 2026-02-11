---
title: 403 - Neutral-Point-Clamped (NPC) 3-Level Inverter
---

# 403 - Neutral-Point-Clamped (NPC) 3-Level Inverter

## Overview

The Neutral-Point-Clamped (NPC) inverter is a multilevel topology that produces three voltage levels per phase, reducing harmonic content and voltage stress on switching devices compared to conventional two-level inverters.

**Difficulty:** 3/3 (Advanced)

**Estimated Time:** 45-60 minutes

## Learning Objectives

After completing this tutorial, you will be able to:
- Understand the NPC 3-level inverter topology and switching states
- Analyze neutral-point voltage balancing challenges
- Design PWM strategies for multilevel converters
- Compare performance with two-level inverters

## Prerequisites

- Complete [Tutorial 402: Three-Phase Inverter](../402_three_phase_inverter/)
- Understanding of PWM modulation techniques
- Familiarity with power semiconductor devices (IGBTs, diodes)

## Circuit Description

### NPC Topology

The NPC inverter uses four switches and two clamping diodes per phase leg to generate three voltage levels: +Vdc/2, 0, and -Vdc/2.

```
                    +Vdc/2
                       │
                   ┌───┴───┐
                   │  S1   │ IGBT with antiparallel diode
                   └───┬───┘
                       │
                   ┌───┴───┐
                   │  S2   │
                   └───┬───┘
                       ├──────── Phase Output (A, B, C)
               Dc1 ────┤
                       │
           0 (NP) ─────┼───────────────── Neutral Point (Capacitor Midpoint)
                       │
               Dc2 ────┤
                       ├────────
                   ┌───┴───┐
                   │  S3   │
                   └───┬───┘
                       │
                   ┌───┴───┐
                   │  S4   │
                   └───┬───┘
                       │
                    -Vdc/2
```

### Switching States

| State | S1 | S2 | S3 | S4 | Output Voltage |
|-------|----|----|----|----|----------------|
| P (Positive) | ON | ON | OFF | OFF | +Vdc/2 |
| O (Zero) | OFF | ON | ON | OFF | 0 |
| N (Negative) | OFF | OFF | ON | ON | -Vdc/2 |

### Key Parameters

| Parameter | Symbol | Typical Value | Unit | Description |
|-----------|--------|---------------|------|-------------|
| DC Bus Voltage | Vdc | 800 | V | Total DC link voltage |
| Output Power | Pout | 10-250 | kW | Rated power |
| Switching Frequency | fsw | 5-20 | kHz | PWM frequency |
| Modulation Index | m | 0-1.15 | - | With 3rd harmonic injection |
| DC Link Capacitance | Cdc | 1000-5000 | uF | Per capacitor |
| Fundamental Frequency | f1 | 50/60 | Hz | Grid/motor frequency |

## Theory

### Advantages of NPC over Two-Level

1. **Lower dv/dt:** Output voltage steps are Vdc/2 instead of Vdc
2. **Reduced THD:** Better approximation of sinusoidal waveform
3. **Lower switching losses:** Devices switch at half the DC bus voltage
4. **Common-mode voltage reduction:** Smaller CM voltage steps

### Challenges

1. **Neutral-point voltage balancing:** Unequal charge/discharge of DC capacitors
2. **Increased component count:** 4 switches + 2 diodes per phase (vs 2 switches)
3. **Complex modulation:** Requires space vector or carrier-based PWM

### Neutral-Point Current

The neutral-point current determines capacitor voltage balance:

```
i_NP = i_a * f_a(d) + i_b * f_b(d) + i_c * f_c(d)
```

Where f(d) is a function of the switching state (0 for P/N states, ±1 for O state).

### PWM Strategies

1. **Carrier-Based PWM (PD-PWM):**
   - Two triangular carriers, phase-disposed
   - Upper carrier: 0 to 1
   - Lower carrier: -1 to 0

2. **Space Vector PWM (SVPWM):**
   - 27 switching states (3^3)
   - Redundant states for NP balancing

## Building the Circuit in GeckoCIRCUITS

### Step 1: Create DC Link

1. Add two DC voltage sources in series (or capacitors charged by a rectifier)
2. Connect their junction to create the neutral point (NP)
3. Each source/capacitor = Vdc/2 (e.g., 400V each for 800V bus)

### Step 2: Build One Phase Leg

1. Add 4 IGBTs (or ideal switches with antiparallel diodes) in series
2. Add 2 clamping diodes:
   - Dc1: Anode to midpoint between S1-S2, Cathode to NP
   - Dc2: Anode to NP, Cathode to midpoint between S3-S4
3. Phase output is the junction between S2 and S3

### Step 3: Replicate for Three Phases

1. Copy the phase leg structure for phases B and C
2. Connect all to the same DC bus and neutral point
3. Connect phase outputs to a balanced 3-phase load (R-L or motor)

### Step 4: Add PWM Modulation

1. Create three sinusoidal reference signals (120° phase shift)
2. Add two triangular carrier signals (PD-PWM):
   - Carrier 1: offset +0.5, amplitude 0.5, frequency = fsw
   - Carrier 2: offset -0.5, amplitude 0.5, frequency = fsw
3. Use comparators:
   - S1: ref > carrier1
   - S2: ref > carrier2
   - S3: ref < carrier2 (inverted logic)
   - S4: ref < carrier1 (inverted logic)

### Step 5: Configure Simulation

- **Simulation time:** 60-100 ms (3-5 fundamental cycles)
- **Time step:** 0.1-1 us (depends on fsw)
- **Solver:** Trapezoidal

## Expected Results

### Waveforms to Observe

1. **Phase-to-Neutral Voltage:** Three distinct levels (+Vdc/2, 0, -Vdc/2)
2. **Line-to-Line Voltage:** Five levels (±Vdc, ±Vdc/2, 0)
3. **Neutral Point Voltage:** Should remain at Vdc/2 (balanced)
4. **Output Current:** Near-sinusoidal with load inductance

### Performance Metrics

| Metric | Two-Level | NPC 3-Level |
|--------|-----------|-------------|
| Voltage THD | 80-90% | 40-50% |
| Current THD | 5-10% | 2-5% |
| Max device voltage | Vdc | Vdc/2 |
| Switching loss | Higher | Lower |

## Exercises

### Exercise 1: Basic Operation

1. Build a single-phase NPC leg with resistive load
2. Apply a 50% duty cycle to observe the three voltage levels
3. Measure the output voltage and verify the switching states

### Exercise 2: PWM Modulation

1. Implement PD-PWM with m = 0.8
2. Compare output THD at fsw = 5 kHz vs 10 kHz
3. **Question:** What is the dominant harmonic frequency?

### Exercise 3: Neutral Point Balancing

1. Connect unbalanced loads to phases A and B
2. Observe neutral point voltage drift
3. **Challenge:** Implement a simple balancing controller using redundant switching states

### Exercise 4: Comparison Study

1. Build an equivalent two-level inverter (same power rating)
2. Compare:
   - Output voltage THD
   - Semiconductor losses
   - Common-mode voltage
3. Document your findings

## Common Issues and Troubleshooting

| Issue | Possible Cause | Solution |
|-------|---------------|----------|
| Simulation diverges | Improper switching complementarity | Ensure dead-time or mutual exclusion of S1-S4 and S2-S3 |
| NP voltage drifts | Unbalanced modulation | Add NP voltage feedback to PWM |
| High current ripple | Low switching frequency | Increase fsw or add output filter |
| Clamping diode stress | High di/dt | Check diode ratings and snubbers |

## Related Examples

- [Two-Level Three-Phase Inverter](../402_three_phase_inverter/) - Compare with simpler topology
- [Thermal Analysis](../../5xx_thermal_simulation/502_junction_temperature/) - Add loss models
- [Matrix Converters](../../8xx_advanced_topics/801_matrix_converters/) - Alternative AC-AC topology

## References

1. Rodriguez, J., Lai, J.S., Peng, F.Z. "Multilevel Inverters: A Survey of Topologies, Controls, and Applications." IEEE Trans. Ind. Electronics, 2002.
2. Nabae, A., Takahashi, I., Akagi, H. "A New Neutral-Point-Clamped PWM Inverter." IEEE Trans. Ind. Appl., 1981.
3. Holmes, D.G., Lipo, T.A. "Pulse Width Modulation for Power Converters." Wiley-IEEE Press, 2003.

## Circuit Files

> **Note:** This tutorial folder will contain example circuits once created in GeckoCIRCUITS.
> To contribute a circuit, follow the documentation and save as:
> - `npc_single_phase.ipes` - Single-phase NPC leg
> - `npc_three_phase.ipes` - Complete three-phase NPC inverter
> - `npc_three_phase_balanced.ipes` - With NP voltage balancing control

---
*Tutorial Version: 1.0*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
