---
title: Forward Converter
---

# Forward Converter

## Overview

The forward converter is an isolated DC-DC topology derived from the buck converter. Unlike the flyback, it transfers energy to the output during the switch-on time and requires a transformer reset mechanism. Forward converters are commonly used in medium-power applications (50-500W) such as telecom power supplies, industrial equipment, and server power systems.

**Difficulty:** Intermediate

**Estimated Time:** 30-45 minutes

## Learning Objectives

After completing this example, you will be able to:
- Understand forward converter operation and energy transfer mechanism
- Design transformer reset circuits (third winding, RCD clamp, active clamp)
- Analyze the relationship between turns ratio and duty cycle limits
- Compare forward and flyback converter characteristics

## Prerequisites

- Complete [Buck Converter Tutorial](../tutorials/2xx_dcdc_converters/201_buck_converter/)
- Understanding of transformer operation
- Familiarity with [Flyback Converter](../flyback_converter/) for comparison

## Circuit Description

### Topology (Third-Winding Reset)

```
                     Transformer
    Reset             n1:n3:n2
    Winding  Dr        │  │
      ┌──────|<|───────●  ●
      │                ║  ║
      │     n1:n2      ║  ║
+Vin ─┼──┬──[S]──┬─────●  ●─────[D1]──┬──[L]──┬── +Vout
      │  │       │     ║  ║           │       │
      │ ┌┴┐     ┌┴┐    ║  ║          ┌┴┐     ┌┴┐
      │ │ │ Cin │ │Lm  ║  ║          │ │ D2  │ │ C
      │ │ │     │ │    ║  ║          │ │ FW  │ │
      │ └┬┘     └┬┘    ║  ║          └┬┘     └┬┘
      │  │       │     ●  ●           │       │
      └──┴───────┴─────────────────────┴───────┴── GND

    Primary Side                    Secondary Side
```

**Key Components:**
- **S:** Primary switch (MOSFET)
- **n1:n2:** Main turns ratio (primary:secondary)
- **n3:** Reset winding (typically n3 = n1)
- **Dr:** Reset diode
- **D1:** Rectifier diode
- **D2:** Freewheeling diode (continuous current through L)
- **L:** Output inductor (energy storage)
- **C:** Output capacitor

### Operating Principle

**Switch ON (Energy Transfer Phase):**
1. Current flows through primary, transformer transfers energy to secondary
2. D1 conducts, current flows through L to output
3. Inductor current ramps up: diL/dt = (Vout/n - Vout)/L
4. Core magnetizes (flux increases)

**Switch OFF (Reset + Freewheeling Phase):**
1. Primary current stops, transformer must reset
2. Reset winding conducts through Dr, core demagnetizes
3. D1 blocks, D2 (freewheeling) conducts
4. Inductor current ramps down: diL/dt = -Vout/L
5. Reset time: Treset = Ton × (n1/n3)

### Key Parameters

| Parameter | Symbol | Example Value | Unit | Description |
|-----------|--------|---------------|------|-------------|
| Input Voltage | Vin | 48 | V | Telecom bus |
| Output Voltage | Vout | 5 | V | Logic supply |
| Output Power | Pout | 100 | W | Rated power |
| Switching Frequency | fs | 200 | kHz | Fixed frequency |
| Turns Ratio | n | 4:1 | - | Primary:Secondary |
| Reset Ratio | n1:n3 | 1:1 | - | Primary:Reset |
| Output Inductance | L | 10 | μH | For CCM operation |
| Output Capacitance | C | 470 | μF | Low ESR |

### Voltage Conversion Ratio

```
Vout = (Vin × D) / n
```

Where:
- D = duty cycle (limited by reset requirement)
- n = turns ratio (n1/n2)

### Maximum Duty Cycle

For third-winding reset with n1 = n3:
```
Dmax = n3 / (n1 + n3) = 0.5 (for equal windings)
```

For n3 < n1 (faster reset, higher Dmax):
```
Dmax = n3 / (n1 + n3)
```

## Design Equations

### Turns Ratio Selection

For Vin = 48V, Vout = 5V, D = 0.4:
```
n = (Vin × D) / Vout = (48 × 0.4) / 5 = 3.84 ≈ 4:1
```

### Output Inductor

For CCM operation with 30% ripple at minimum load:
```
L = (Vin/n - Vout) × D / (ΔIL × fs)
L = (48/4 - 5) × 0.4 / (0.3 × Iout × 200k)
```

For Iout = 20A, ΔIL = 6A:
```
L = (12 - 5) × 0.4 / (6 × 200k) = 2.3 μH
```

Use 5-10 μH for margin.

### Switch Voltage Stress

With third-winding reset (n1 = n3):
```
Vds = Vin + Vin × (n1/n3) = 2 × Vin
```

For Vin = 48V: Vds = 96V → Use 150V MOSFET

With RCD clamp, voltage can be higher due to leakage.

### Diode Voltage Stress

Rectifier diode D1:
```
Vr,D1 = Vin/n + Vout × (n3/n2)  ≈ 2 × Vin/n (worst case)
```

For Vin = 48V, n = 4: Vr,D1 ≈ 24V → Use 40V Schottky

## Reset Methods Comparison

| Method | Dmax | Advantages | Disadvantages |
|--------|------|------------|---------------|
| Third Winding | 0.5 | Simple, lossless | Extra winding, limited D |
| RCD Clamp | 0.5-0.6 | No extra winding | Power loss in clamp |
| Active Clamp | 0.6-0.8 | Higher efficiency, ZVS | Complex, extra switch |
| Resonant Reset | 0.7+ | High D, soft switching | Complex, variable freq |

## Building in GeckoCIRCUITS

### Step 1: Create Primary Circuit

1. Add DC voltage source (Vin = 48V)
2. Add input capacitor (Cin = 100μF)
3. Add MOSFET switch

### Step 2: Add Transformer

1. Add ideal transformer with n1:n2 ratio
2. For reset winding: Add second transformer or coupled inductor
3. Connect reset diode (Dr) from reset winding to Vin+
4. Observe dot convention: reset winding dots opposite to primary

### Step 3: Create Secondary Circuit

1. Add rectifier diode D1 (in series with secondary winding)
2. Add freewheeling diode D2 (cathode to D1 output)
3. Add output inductor L
4. Add output capacitor C
5. Add load resistor (R = Vout²/Pout = 0.25Ω)

### Step 4: Add PWM Control

1. Add PWM signal generator (fs = 200kHz)
2. Set duty cycle D = 0.4 (below 0.5 limit)
3. Connect to switch gate

### Step 5: Configure Simulation

- **Simulation time:** 1-5 ms
- **Time step:** 5-25 ns (1/200 of switching period)
- **Solver:** Trapezoidal

## Expected Results

### Steady-State Waveforms

| Signal | Expected Behavior |
|--------|-------------------|
| Switch voltage | 0 during ON, 2×Vin during reset, Vin after reset |
| Primary current | Triangular ramp during ON, zero during OFF |
| Transformer flux | Triangular wave, symmetric reset |
| Inductor current | Triangular ripple around DC value |
| Output voltage | DC = Vin×D/n with small ripple |

### Design Verification

For Vin=48V, D=0.4, n=4:
```
Vout = Vin × D / n = 48 × 0.4 / 4 = 4.8V
```

### Efficiency Estimate

| Loss Component | Typical Value |
|----------------|---------------|
| MOSFET conduction | 1-2% |
| MOSFET switching | 1-2% |
| Transformer core | 0.5-1% |
| Transformer winding | 1-2% |
| Diode conduction | 2-3% |
| Inductor core/winding | 1-2% |
| **Total** | **7-12%** |

## Exercises

### Exercise 1: Duty Cycle Limit

1. Build forward converter with n1 = n3 (1:1 reset)
2. Start with D = 0.3, increase gradually
3. **Question:** What happens when D > 0.5? (flux doesn't reset!)

### Exercise 2: Compare with Buck

1. Build equivalent non-isolated buck: Vin = 12V, Vout = 5V
2. Compare waveforms: inductor current, output ripple
3. **Question:** How do the topologies relate?

### Exercise 3: Reset Winding Ratio

1. Test n1:n3 = 1:1 (Dmax = 0.5)
2. Test n1:n3 = 2:1 (Dmax = 0.33)
3. Test n1:n3 = 1:2 (Dmax = 0.67)
4. **Question:** What are the trade-offs?

### Exercise 4: Current-Mode Control

1. Add current sense resistor in primary
2. Implement peak current-mode control
3. Apply load step (50% to 100%)
4. Compare transient response to voltage-mode

## Forward vs Flyback Comparison

| Characteristic | Forward | Flyback |
|---------------|---------|---------|
| Power range | 50-500W | 5-150W |
| Output inductor | Required | Not needed |
| Transformer utilization | Single quadrant | Full B-H loop |
| Core gap | No (minimal) | Yes (energy storage) |
| Output current ripple | Lower | Higher |
| Component count | Higher | Lower |
| Efficiency | Higher (at higher power) | Moderate |
| Cross-regulation | Better | Worse |

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Core saturation | Incomplete reset | Reduce D or adjust n3 |
| High switch voltage | Leakage spike | Add snubber or clamp |
| D2 doesn't conduct | CCM not achieved | Reduce L or increase load |
| Output oscillation | Poor damping | Add ESR or feedback |
| Low efficiency | Diode losses | Use synchronous rectification |

## Related Examples

- [Flyback Converter](../flyback_converter/) - Alternative isolated topology
- [Buck Converter](../buck_converter/) - Non-isolated equivalent
- [Full Bridge](../full_bridge/) - Higher power isolated

## References

1. Pressman, A. "Switching Power Supply Design" - Forward Converter Chapter
2. Texas Instruments SLUP126: "Forward Converter Design"
3. Infineon AN-SMPS-ICE1PCS01: "Forward Converter Design Guide"
4. Erickson & Maksimovic: "Fundamentals of Power Electronics" - Chapter 6

## Circuit Files

> **Note:** Example circuits to be added:
> - `forward_basic.ipes` - Basic forward with third-winding reset
> - `forward_rcd.ipes` - With RCD clamp reset
> - `forward_active_clamp.ipes` - Active clamp topology
> - `forward_two_switch.ipes` - Two-switch forward (inherent reset)

---
*Example Version: 1.0*
*Last updated: 2026-02*
*GeckoCIRCUITS v1.0*
