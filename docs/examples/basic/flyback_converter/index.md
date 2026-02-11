---
title: Flyback Converter
---

# Flyback Converter

## Overview

The flyback converter is an isolated DC-DC topology derived from the buck-boost converter. It uses a coupled inductor (flyback transformer) to provide galvanic isolation while enabling step-up or step-down voltage conversion. Flyback converters are widely used in low-power applications (5-150W) such as phone chargers, LED drivers, and auxiliary power supplies.

**Difficulty:** Intermediate

**Estimated Time:** 30-45 minutes

## Learning Objectives

After completing this example, you will be able to:
- Understand flyback converter operation in CCM and DCM
- Design the coupled inductor (flyback transformer)
- Analyze voltage stresses on switch and diode
- Calculate output voltage as a function of duty cycle and turns ratio

## Prerequisites

- Basic understanding of DC-DC converter operation
- Familiarity with [Buck-Boost Converter](../tutorials/2xx_dcdc_converters/203_buck_boost/)
- Understanding of transformer/coupled inductor behavior

## Circuit Description

### Topology

```
                   Flyback Transformer
                         n:1
    +Vin ──┬──[S]──┬────●  ●────[D]──┬── +Vout
           │       │    ║  ║         │
           │      ┌┴┐   ║  ║        ┌┴┐
           │      │ │ Lm ║  ║        │ │ C
          ┌┴┐     │ │   ║  ║        │ │
          │ │ Cin └┬┘   ║  ║        └┬┘
          │ │      │    ●  ●         │
          └┬┘      │               R │ Load
           │       │                 │
    GND ───┴───────┴─────────────────┴── GND (isolated)

    Primary Side         Secondary Side
```

**Key Components:**
- **S:** Primary-side switch (MOSFET)
- **Lm:** Magnetizing inductance (energy storage)
- **n:1:** Turns ratio (primary:secondary)
- **D:** Secondary-side rectifier diode
- **C:** Output capacitor

### Operating Principle

**Switch ON (Energy Storage Phase):**
1. Primary current ramps up through magnetizing inductance Lm
2. Energy stored in magnetic field: E = ½ Lm Ip²
3. Secondary diode is reverse-biased (due to transformer polarity)
4. Output capacitor supplies load current

**Switch OFF (Energy Transfer Phase):**
1. Primary current interrupted
2. Transformer polarity reverses (flyback action)
3. Secondary diode conducts, transferring energy to output
4. Magnetizing current decreases, reflected to secondary: Is = Ip × n

### Key Parameters

| Parameter | Symbol | Example Value | Unit | Description |
|-----------|--------|---------------|------|-------------|
| Input Voltage | Vin | 85-265 (rectified AC) | V | Wide input range |
| Output Voltage | Vout | 12 | V | Regulated output |
| Output Power | Pout | 30 | W | Rated power |
| Switching Frequency | fs | 100 | kHz | Fixed frequency |
| Turns Ratio | n | 10:1 | - | Primary:Secondary |
| Magnetizing Inductance | Lm | 500 | μH | Primary referred |
| Output Capacitance | C | 470 | μF | Low ESR type |
| Max Duty Cycle | Dmax | 0.5 | - | For DCM/CCM boundary |

### Voltage Conversion Ratio

**CCM (Continuous Conduction Mode):**
```
Vout/Vin = D/(n(1-D))
```

**DCM (Discontinuous Conduction Mode):**
```
Vout/Vin = D/(n × √(2Lm fs/RL))  (for D < Dcrit)
```

Where:
- D = duty cycle
- n = turns ratio (Np/Ns)
- Lm = magnetizing inductance (primary referred)
- RL = load resistance

## Design Equations

### Turns Ratio Selection

For Vin = 150V (typical rectified line), Vout = 12V, D = 0.4:
```
n = (Vin × D) / (Vout × (1-D))
n = (150 × 0.4) / (12 × 0.6) = 8.33 ≈ 8:1
```

### Magnetizing Inductance

For CCM operation at minimum load:
```
Lm > (Vin,min × Dmax)² / (2 × Pout,min × fs)
```

For boundary/DCM operation (common in low-power):
```
Lm = (Vin × D × (1-D)) / (2 × Iout × n × fs)
```

### Switch Voltage Stress

The switch must withstand input voltage plus reflected output voltage plus leakage spike:
```
Vds,max = Vin + n × Vout + Vspike
```

Example: Vin=400V, n=8, Vout=12V, Vspike=100V
```
Vds,max = 400 + 8×12 + 100 = 596V → Use 800V MOSFET
```

### Diode Voltage Stress

```
Vr,diode = Vout + Vin/n
```

Example: Vout=12V, Vin=400V, n=8
```
Vr,diode = 12 + 400/8 = 62V → Use 100V Schottky
```

## Building in GeckoCIRCUITS

### Step 1: Create Primary Side

1. Add DC voltage source (Vin = 150V for testing)
2. Add input capacitor (Cin = 100μF, optional)
3. Add switch (MOSFET or ideal switch) in series with primary winding

### Step 2: Add Coupled Inductor/Transformer

1. **Option A - Ideal Transformer + Inductor:**
   - Add ideal transformer with turns ratio n:1
   - Add inductor Lm in parallel with primary winding

2. **Option B - Coupled Inductors (if available):**
   - Use coupled inductor component
   - Set primary inductance Lp = Lm
   - Set coupling coefficient k ≈ 0.95-0.99
   - Secondary inductance Ls = Lm/n²

3. Note transformer dot convention (primary and secondary dots on opposite sides for flyback operation)

### Step 3: Create Secondary Side

1. Add rectifier diode (cathode to output positive)
2. Add output capacitor
3. Add load resistor (R = Vout²/Pout)

### Step 4: Add PWM Control

1. Add PWM signal generator (fs = 100kHz)
2. Set duty cycle D = 0.4 (for initial test)
3. Connect to switch gate

### Step 5: Configure Simulation

- **Simulation time:** 5-10 ms (settling time)
- **Time step:** 10-50 ns (1/100 of switching period)
- **Solver:** Trapezoidal

## Expected Results

### Steady-State Waveforms

| Signal | Expected Behavior |
|--------|-------------------|
| Switch voltage (Vds) | Square wave: 0 during ON, Vin + n×Vout during OFF |
| Primary current (Ip) | Triangular ramp during ON, zero during OFF |
| Secondary current (Is) | Zero during ON, decaying ramp during OFF |
| Output voltage | DC with ripple: Vout ± ΔVout |

### Design Verification

For Vin=150V, D=0.4, n=8:1:
```
Vout = Vin × D / (n × (1-D)) = 150 × 0.4 / (8 × 0.6) = 12.5V
```

### Output Voltage Ripple

```
ΔVout = (Iout × D) / (fs × C)
```

For Iout=2.5A, D=0.4, fs=100kHz, C=470μF:
```
ΔVout = (2.5 × 0.4) / (100k × 470μ) = 21mV (0.17%)
```

## Exercises

### Exercise 1: CCM vs DCM Operation

1. Set Lm = 500μH, load R = 10Ω (heavy load)
2. Run simulation, observe secondary current waveform
3. Increase R to 100Ω (light load)
4. **Question:** Does the converter enter DCM? How can you tell?

### Exercise 2: Input Voltage Variation

1. Fix D = 0.4, vary Vin from 100V to 200V
2. Record Vout for each Vin
3. Plot Vout vs Vin
4. **Question:** Why does a fixed duty cycle not regulate output?

### Exercise 3: Turns Ratio Trade-off

1. Test n = 5:1, 10:1, and 15:1 with fixed Vin = 150V
2. Adjust D to achieve Vout = 12V in each case
3. Measure: switch voltage stress, primary current magnitude
4. **Question:** What are the trade-offs in selecting turns ratio?

### Exercise 4: Leakage Inductance Effects

1. If using coupled inductors, reduce k from 0.99 to 0.90
2. Observe switch voltage during turn-off
3. **Advanced:** Add an RCD snubber to clamp the voltage spike
4. **Question:** How much energy is lost in the snubber?

## Practical Considerations

### Transformer Design

- Core selection: Ferrite (EE, ETD, PQ cores common)
- Air gap: Required for energy storage (Lm), typically 0.5-2mm
- Wire gauge: Primary handles high peak current, secondary handles DC

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| No output | Wrong dot convention | Reverse secondary winding |
| Low output | DCM operation | Increase Lm or reduce load |
| Voltage spike | Leakage inductance | Add snubber circuit |
| High ripple | Small capacitor | Increase C or use low-ESR type |
| Transformer saturation | Duty cycle too high | Limit Dmax, add reset mechanism |

## Related Examples

- [Buck Converter](../buck_converter/) - Non-isolated step-down
- [Forward Converter](../forward_converter/) - Alternative isolated topology
- [Full Bridge](../full_bridge/) - Higher power isolated

## References

1. Pressman, A., Billings, K., Morey, T. "Switching Power Supply Design" - Chapter on Flyback Converters
2. Basso, C. "Switch-Mode Power Supplies" - SPICE Simulation
3. Texas Instruments SLUP127: "Flyback Transformer Design"
4. ON Semiconductor AND9124: "Flyback Design Guidelines"

## Circuit Files

> **Note:** Example circuits to be added:
> - `flyback_basic.ipes` - Basic flyback without feedback
> - `flyback_dcm.ipes` - DCM operation example
> - `flyback_ccm.ipes` - CCM operation with larger Lm
> - `flyback_snubber.ipes` - With RCD snubber for leakage

---
*Example Version: 1.0*
*Last updated: 2026-02*
*GeckoCIRCUITS v1.0*
