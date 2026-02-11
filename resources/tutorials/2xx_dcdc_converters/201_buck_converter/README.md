# Tutorial 201: Buck Converter

## Overview

The buck converter is the most fundamental step-down DC-DC topology. It efficiently converts a higher DC voltage to a lower DC voltage using PWM control. This tutorial covers buck converter operation, design equations, and simulation analysis.

**Level:** Intermediate (2/3)

**Duration:** 30-40 minutes

**Series:** DC-DC Converters

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Understand buck converter operation in CCM and DCM
- [ ] Apply the volt-second balance principle
- [ ] Calculate output voltage, inductor current ripple, and capacitor voltage ripple
- [ ] Design a buck converter for given specifications
- [ ] Simulate and verify converter operation

## Prerequisites

- Complete [Tutorial 103: PWM Basics](../../1xx_getting_started/103_pwm_basics/)
- Understanding of inductor and capacitor behavior
- Basic circuit analysis skills

## Materials

| File | Description |
|------|-------------|
| `buck_simple.ipes` | Simple open-loop buck converter |
| `A_Buck.ipes` | Complete buck converter example |
| `Buck_Exercise.pdf` | Exercise problems |
| `Buck_Solution.pdf` | Solutions with detailed explanations |

## Circuit Description

### Buck Converter Topology

```
        +Vin ──────┬──[S]──┬──[L]──┬── +Vout
                   │       │       │
                   │      [D]     [C]     [R] Load
                   │       │       │       │
        GND ───────┴───────┴───────┴───────┴── GND
```

**Components:**
- **S:** High-side switch (MOSFET/IGBT), controlled by PWM
- **D:** Freewheeling diode (Schottky recommended)
- **L:** Output inductor (energy storage)
- **C:** Output capacitor (voltage smoothing)
- **R:** Load resistance

### Operating Modes

**Switch ON (0 < t < D·Ts):**
- Current path: Vin → S → L → C/R → GND
- Inductor voltage: VL = Vin - Vout (positive, current increases)
- Diode: Reverse biased (OFF)

**Switch OFF (D·Ts < t < Ts):**
- Current path: L → C/R → GND → D → L (freewheeling)
- Inductor voltage: VL = -Vout (negative, current decreases)
- Diode: Forward biased (ON)

## Key Equations

### Output Voltage (Volt-Second Balance)

In steady state, the average inductor voltage is zero:
```
(Vin - Vout)·D·Ts + (-Vout)·(1-D)·Ts = 0
```

Solving:
```
Vout = D × Vin
```

**Key insight:** Output voltage is directly proportional to duty cycle!

### Inductor Current Ripple

```
ΔIL = (Vin - Vout) × D / (fs × L)
    = Vout × (1 - D) / (fs × L)
```

### Output Voltage Ripple

Assuming ESR = 0:
```
ΔVout = ΔIL / (8 × fs × C)
```

Including ESR:
```
ΔVout ≈ ΔIL × ESR + ΔIL / (8 × fs × C)
```

### Boundary Condition (CCM/DCM)

CCM is maintained when average inductor current > ripple/2:
```
IL,avg > ΔIL/2

Iout > (Vin × D × (1-D)) / (2 × fs × L)
```

Critical inductance for CCM:
```
Lcrit = (Vin × (1-D) × D) / (2 × fs × Iout,min)
```

## Design Parameters

### Example Design Specifications

| Parameter | Value | Unit |
|-----------|-------|------|
| Input Voltage (Vin) | 48 | V |
| Output Voltage (Vout) | 12 | V |
| Output Current (Iout) | 5 | A |
| Output Power (Pout) | 60 | W |
| Switching Frequency (fs) | 100 | kHz |
| Max Voltage Ripple | 1% | of Vout |
| Max Current Ripple | 30% | of IL,avg |

### Step-by-Step Design

**1. Calculate Duty Cycle:**
```
D = Vout / Vin = 12/48 = 0.25 (25%)
```

**2. Calculate Load Resistance:**
```
R = Vout / Iout = 12/5 = 2.4 Ω
```

**3. Calculate Inductance:**
For 30% ripple:
```
ΔIL = 0.3 × Iout = 0.3 × 5 = 1.5 A
L = Vout × (1-D) / (fs × ΔIL)
L = 12 × 0.75 / (100k × 1.5) = 60 μH
```
Choose: L = 68 μH (standard value)

**4. Calculate Capacitance:**
For 1% ripple (120 mV):
```
C = ΔIL / (8 × fs × ΔVout)
C = 1.5 / (8 × 100k × 0.12) = 15.6 μF
```
Choose: C = 22 μF (standard value)

## Building the Circuit

### Step 1: Power Stage

1. Add voltage source (Vin = 48V DC)
2. Add ideal switch (or MOSFET) - high-side position
3. Add diode - cathode to switch node, anode to ground
4. Add inductor (L = 68 μH)
5. Add capacitor (C = 22 μF) in parallel with load
6. Add resistor (R = 2.4 Ω)

### Step 2: PWM Control

1. Add PWM signal generator:
   - Frequency: 100 kHz
   - Duty cycle: 0.25
2. Connect PWM output to switch gate

### Step 3: Measurements

1. Add SCOPE
2. Connect channels to:
   - Output voltage (Vout)
   - Inductor current (IL)
   - Switch voltage (Vds)

### Step 4: Simulation Settings

- Simulation time: 2 ms (200 switching cycles)
- Time step: 50 ns (or automatic)
- Solver: TRZ

## Expected Results

### Steady-State Waveforms

| Signal | Expected Value |
|--------|----------------|
| Vout (average) | 12 V |
| Vout (ripple) | ~100 mV p-p |
| IL (average) | 5 A |
| IL (ripple) | ~1.5 A p-p |
| Switch Vds | 0/48 V |

### Waveform Characteristics

**Output Voltage:**
- DC level at 12V
- Small triangular ripple
- Frequency = 2×fs (double switching frequency)

**Inductor Current:**
- Triangular waveform
- Average = Iout
- Ramps up during ON, down during OFF

## Exercises

### Exercise 1: Vary Duty Cycle
1. Open `buck_simple.ipes`
2. Change duty cycle from 0.1 to 0.5 in steps of 0.1
3. Record Vout for each D
4. **Verify:** Vout = D × Vin

### Exercise 2: CCM to DCM Transition
1. Set D = 0.25, L = 68 μH
2. Increase R from 2.4Ω to 24Ω (light load)
3. **Observe:** Does inductor current reach zero?
4. **Calculate:** At what load does DCM begin?

### Exercise 3: Ripple Analysis
1. With L = 68 μH, C = 22 μF, measure ripple
2. Double L to 136 μH, measure ripple
3. Double C to 44 μF, measure ripple
4. **Compare:** Which has more effect on voltage ripple?

### Exercise 4: Component Stress
1. Measure peak switch current (= IL,max)
2. Measure peak diode voltage (= Vin)
3. **Design:** Select components with 2× margin

### Exercise 5: Efficiency Estimation
1. Add realistic component losses:
   - Switch: Ron = 10 mΩ
   - Diode: Vf = 0.5V
   - Inductor: DCR = 20 mΩ
2. Calculate: Pin, Pout, efficiency

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Output too low | Wrong duty cycle | Check D = Vout/Vin |
| High ripple | L or C too small | Increase L or C |
| DCM operation | Light load | Increase L or add min load |
| Ringing | Parasitic inductance | Add snubber |

## Efficiency Considerations

Typical loss breakdown:
| Loss Type | Formula | Example |
|-----------|---------|---------|
| Switch conduction | Irms² × Ron | 25mW |
| Switch switching | Vds × Id × (trise + tfall) × fs | 1W |
| Diode conduction | Iavg × Vf + Irms² × Rd | 2.5W |
| Inductor copper | Irms² × DCR | 0.5W |
| Inductor core | From datasheet | 0.2W |

## Related Tutorials

- [202 - Boost Converter](../202_boost_converter/) - Step-up topology
- [203 - Buck-Boost](../203_buck_boost/) - Inverting topologies
- [501 - Loss Calculation](../../5xx_thermal_simulation/501_loss_calculation/) - Thermal analysis

## References

1. Erickson, R.W., Maksimovic, D. "Fundamentals of Power Electronics" - Chapter 7
2. Mohan, N. "Power Electronics" - DC-DC Converters
3. Texas Instruments SLVA477: "Buck Converter Design"

---
*Tutorial Version: 1.0*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
