# Tutorial 202: Boost Converter

## Overview

The boost converter is a step-up DC-DC topology that produces an output voltage higher than its input. It's widely used in battery-powered applications, solar MPPT, and power factor correction. This tutorial covers boost converter operation, design, and the unique challenges of step-up conversion.

**Level:** Intermediate (2/3)

**Duration:** 30-40 minutes

**Series:** DC-DC Converters

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Understand boost converter operation and volt-second balance
- [ ] Calculate output voltage as a function of duty cycle
- [ ] Design for CCM operation and calculate component values
- [ ] Understand right-half-plane zero and control challenges
- [ ] Simulate and analyze boost converter performance

## Prerequisites

- Complete [Tutorial 201: Buck Converter](../201_buck_converter/)
- Understanding of inductor energy storage
- Basic feedback control concepts (helpful)

## Materials

| File | Description |
|------|-------------|
| `boost_simple.ipes` | Simple open-loop boost converter |
| `B_Boost.ipes` | Complete boost converter example |

## Circuit Description

### Boost Converter Topology

```
        +Vin ──[L]──┬──[D]──┬── +Vout
                    │       │
                   [S]     [C]     [R] Load
                    │       │       │
        GND ────────┴───────┴───────┴── GND
```

**Components:**
- **L:** Input inductor (energy storage)
- **S:** Low-side switch, controlled by PWM
- **D:** Boost diode (high-side)
- **C:** Output capacitor
- **R:** Load resistance

### Operating Modes

**Switch ON (0 < t < D·Ts):**
- Current path: Vin → L → S → GND
- Inductor voltage: VL = Vin (positive, current increases)
- Diode: Reverse biased (OFF)
- Capacitor supplies load current

**Switch OFF (D·Ts < t < Ts):**
- Current path: Vin → L → D → C/R → GND
- Inductor voltage: VL = Vin - Vout (negative, current decreases)
- Diode: Forward biased (ON)
- Inductor transfers energy to output

## Key Equations

### Output Voltage (Volt-Second Balance)

In steady state:
```
Vin·D·Ts + (Vin - Vout)·(1-D)·Ts = 0
```

Solving:
```
Vout = Vin / (1 - D)
```

**Voltage Gain:**
```
M = Vout/Vin = 1 / (1 - D)
```

| D | M = Vout/Vin |
|---|--------------|
| 0 | 1.00 |
| 0.25 | 1.33 |
| 0.50 | 2.00 |
| 0.75 | 4.00 |
| 0.90 | 10.00 |

> **Warning:** As D → 1, gain → ∞, but efficiency drops rapidly!

### Inductor Current

Average inductor current:
```
IL,avg = Iout / (1 - D) = Pin / Vin
```

Inductor current ripple:
```
ΔIL = (Vin × D) / (fs × L)
```

### Output Voltage Ripple

```
ΔVout = (Iout × D) / (fs × C)
```

Note: Ripple is higher than buck because current is pulsating!

### CCM Boundary

Critical inductance:
```
Lcrit = (Vin × D × (1-D)²) / (2 × fs × Iout)
```

## Design Parameters

### Example Design Specifications

| Parameter | Value | Unit |
|-----------|-------|------|
| Input Voltage (Vin) | 12 | V |
| Output Voltage (Vout) | 48 | V |
| Output Current (Iout) | 2 | A |
| Output Power (Pout) | 96 | W |
| Switching Frequency (fs) | 100 | kHz |
| Max Voltage Ripple | 2% | of Vout |
| Max Current Ripple | 40% | of IL,avg |

### Step-by-Step Design

**1. Calculate Duty Cycle:**
```
D = 1 - Vin/Vout = 1 - 12/48 = 0.75 (75%)
```

**2. Calculate Input (Inductor) Current:**
```
IL,avg = Iout / (1-D) = 2 / 0.25 = 8 A
```

**3. Calculate Inductance:**
For 40% ripple:
```
ΔIL = 0.4 × IL,avg = 0.4 × 8 = 3.2 A
L = (Vin × D) / (fs × ΔIL)
L = (12 × 0.75) / (100k × 3.2) = 28 μH
```
Choose: L = 33 μH (standard value)

**4. Calculate Capacitance:**
For 2% ripple (0.96V):
```
C = (Iout × D) / (fs × ΔVout)
C = (2 × 0.75) / (100k × 0.96) = 15.6 μF
```
Choose: C = 22 μF (low ESR)

**5. Component Stress:**
```
Switch current: IL,peak = IL,avg + ΔIL/2 = 9.6 A
Switch voltage: Vout = 48 V → use 80V+ MOSFET
Diode current: IL,avg = 8 A (pulsed)
Diode voltage: Vout = 48 V → use 60V+ Schottky
```

## Control Challenges

### Right-Half-Plane Zero (RHPZ)

The boost converter has an inherent RHPZ in its transfer function:
```
fz,RHP = (1-D)² × R / (2π × L)
```

**Effects:**
- Limits control bandwidth
- Causes initial wrong-way response
- Requires slower feedback loop

**Mitigation:**
- Keep D < 0.7-0.8 if possible
- Use current-mode control
- Design for lower bandwidth

### Input vs. Output Current

Unlike buck, the boost draws **more current from input than output:**
```
Iin = Iout / (1-D) > Iout
```

This affects:
- Input capacitor sizing
- Wire gauge selection
- EMI filter design

## Building the Circuit

### Step 1: Power Stage

1. Add voltage source (Vin = 12V DC)
2. Add inductor (L = 33 μH) in series with input
3. Add ideal switch (low-side, to ground)
4. Add diode (cathode to output)
5. Add capacitor (C = 22 μF) at output
6. Add resistor (R = 24 Ω for 2A at 48V)

### Step 2: PWM Control

1. Add PWM signal generator:
   - Frequency: 100 kHz
   - Duty cycle: 0.75
2. Connect PWM output to switch gate

### Step 3: Measurements

1. Add SCOPE
2. Connect to:
   - Output voltage (Vout)
   - Inductor current (IL)
   - Switch voltage (Vds)
   - Diode current (Id)

### Step 4: Simulation Settings

- Simulation time: 5-10 ms (allow settling)
- Time step: 50 ns (or automatic)
- Solver: TRZ

## Expected Results

### Steady-State Waveforms

| Signal | Expected Value |
|--------|----------------|
| Vout (average) | 48 V |
| Vout (ripple) | ~1 V p-p |
| IL (average) | 8 A |
| IL (ripple) | ~3 A p-p |
| Switch Vds (OFF) | 48 V |

### Waveform Characteristics

**Inductor Current:**
- Triangular waveform
- Never goes negative (CCM)
- Average = Iin = Iout/(1-D)

**Switch Voltage:**
- 0 during ON
- Vout during OFF
- May show ringing at transitions

**Output Voltage:**
- Higher ripple than buck (discontinuous diode current)
- DC level at Vout = Vin/(1-D)

## Exercises

### Exercise 1: Voltage Gain
1. Open `boost_simple.ipes`
2. Set Vin = 12V, vary D from 0.2 to 0.8
3. Record Vout for each D
4. **Verify:** Vout = Vin/(1-D)
5. **Note:** What happens at high D?

### Exercise 2: Efficiency vs. Duty Cycle
1. Add component losses (Ron, Vf, DCR)
2. Measure efficiency at D = 0.5, 0.67, 0.75, 0.85
3. **Plot:** Efficiency vs D
4. **Explain:** Why does efficiency drop at high D?

### Exercise 3: CCM vs DCM
1. With L = 33 μH, set light load (R = 240Ω)
2. **Observe:** Does IL reach zero?
3. **Calculate:** Critical load for CCM boundary
4. **Measure:** How does DCM affect Vout regulation?

### Exercise 4: Transient Response
1. Apply step load change (R: 24Ω → 12Ω)
2. Measure: Vout undershoot, recovery time
3. **Compare:** Response with different C values
4. **Challenge:** Add simple voltage feedback

### Exercise 5: Current Mode Control
1. Add current sense on inductor
2. Implement peak current-mode PWM
3. **Compare:** Transient response vs. voltage-mode

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Output lower than expected | High diode drop, losses | Use Schottky, lower D |
| Excessive ripple | C too small, high ESR | Increase C, use MLCC |
| DCM at light load | L too small | Increase L |
| Switch failure | Voltage spike | Add snubber, check ratings |
| Unstable control | RHPZ, high bandwidth | Slow down controller |

## Efficiency Analysis

### Loss Breakdown

| Component | Loss Mechanism | Typical % |
|-----------|---------------|-----------|
| Switch | Conduction (I²R) | 1-3% |
| Switch | Switching (CV²f) | 2-5% |
| Diode | Conduction (Vf×I) | 2-5% |
| Diode | Reverse recovery | 1-3% |
| Inductor | Copper (I²R) | 1-2% |
| Inductor | Core loss | 0.5-1% |
| Capacitor | ESR loss | 0.2-0.5% |

### Improving Efficiency

1. **Synchronous rectification:** Replace diode with MOSFET
2. **Interleaving:** Multiple parallel phases
3. **Lower frequency:** Reduce switching losses (trade-off: larger L, C)
4. **Lower D:** Limit step-up ratio (cascade if needed)

## Boost vs. Buck Comparison

| Aspect | Buck | Boost |
|--------|------|-------|
| Voltage ratio | Step-down | Step-up |
| Input current | Pulsating | Continuous |
| Output current | Continuous | Pulsating |
| RHPZ | No | Yes |
| Control | Easier | Harder |
| Efficiency at extreme D | Good | Poor |

## Related Tutorials

- [201 - Buck Converter](../201_buck_converter/) - Step-down topology
- [203 - Buck-Boost](../203_buck_boost/) - Inverting topologies
- [302 - PFC Basics](../../3xx_acdc_rectifiers/302_pfc_basics/) - Boost PFC application

## References

1. Erickson, R.W., Maksimovic, D. "Fundamentals of Power Electronics" - Chapter 7
2. Vorpérian, V. "Fast Analytical Techniques for Electrical and Electronic Circuits"
3. Texas Instruments SLVA372: "Boost Converter Design"

---
*Tutorial Version: 1.0*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
