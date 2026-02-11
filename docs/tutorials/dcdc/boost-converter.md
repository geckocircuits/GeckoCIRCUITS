---
title: Boost Converter
description: Step-up DC-DC converter design and simulation
---

# Tutorial: Boost Converter

Design and simulate a boost (step-up) DC-DC converter.

**Difficulty:** Beginner (1/3) | **Duration:** 30 minutes

**Circuit file:** `resources/tutorials/2xx_dcdc_converters/202_boost_converter/boost_simple.ipes`

## Learning Objectives

- [ ] Understand boost converter operation
- [ ] Calculate output voltage and component values
- [ ] Simulate and verify the design
- [ ] Analyze voltage and current waveforms

## Theory

### Topology

The boost converter steps up DC voltage:

```
                    L
    Vin ──────[LLLLL]──●──|>|──●── Vout
                        │   D    │
                      [FET]    [C]  [R]
                        │       │    │
                       GND ─────●────┘
```

### Operating Principle

**Switch ON (0 < t < D·T):**
Inductor charges from V_in, diode is reverse-biased, capacitor supplies load.

$$V_L = V_{in}, \quad \frac{di_L}{dt} = \frac{V_{in}}{L}$$

**Switch OFF (D·T < t < T):**
Inductor discharges through diode to load and capacitor.

$$V_L = V_{in} - V_{out}, \quad \frac{di_L}{dt} = \frac{V_{in} - V_{out}}{L}$$

### Key Equations

**Voltage conversion ratio (CCM):**

$$\frac{V_{out}}{V_{in}} = \frac{1}{1-D}$$

**Minimum inductance for CCM:**

$$L_{min} = \frac{D(1-D)^2 R}{2f_{sw}}$$

**Output voltage ripple:**

$$\frac{\Delta V_{out}}{V_{out}} = \frac{D}{R C f_{sw}}$$

**Inductor current ripple:**

$$\Delta i_L = \frac{V_{in} \cdot D}{L \cdot f_{sw}}$$

## Design Example

### Specifications

| Parameter | Value |
|-----------|-------|
| Input voltage | 12 V |
| Output voltage | 24 V |
| Output power | 24 W |
| Switching frequency | 100 kHz |
| Max voltage ripple | 1% |

### Calculations

**Duty cycle:**

$$D = 1 - \frac{V_{in}}{V_{out}} = 1 - \frac{12}{24} = 0.5$$

**Load resistance:**

$$R = \frac{V_{out}^2}{P_{out}} = \frac{24^2}{24} = 24\ \Omega$$

**Minimum inductance:**

$$L_{min} = \frac{0.5 \times 0.5^2 \times 24}{2 \times 100k} = 15\ \mu H$$

Choose L = 100 uH (well above minimum for low ripple).

**Output capacitor:**

$$C = \frac{D}{R \times \frac{\Delta V}{V} \times f_{sw}} = \frac{0.5}{24 \times 0.01 \times 100k} = 20.8\ \mu F$$

Choose C = 47 uF.

## Simulation

### Setup

1. Open `boost_simple.ipes`
2. Verify parameters match the design
3. Set simulation: T_end = 2 ms, dt = 50 ns

### Run

Press ++f5++ and open the SCOPE to view:

1. **Output voltage** - Should settle to ~24V
2. **Inductor current** - Triangular waveform, always positive (CCM)
3. **Switch voltage** - Pulses to V_out when OFF

### Expected Waveforms

- Output voltage: 24V with < 1% ripple
- Average inductor current: I_out / (1-D) = 2A
- Inductor current ripple: V_in x D / (L x f_sw) = 0.6A

## Exercises

### Exercise 1: Duty Cycle Variation
Change D from 0.3 to 0.7 in steps of 0.1. Plot V_out vs D and compare with theory.

### Exercise 2: CCM/DCM Boundary
Reduce the load power (increase R) until the inductor current touches zero. This is the CCM/DCM boundary.

### Exercise 3: Component Sizing
Reduce L to 15 uH (minimum). Observe increased ripple. Then try 5 uH to see DCM operation.

## Important Notes

!!! warning "Boost Converter Limitations"
    - Output is always higher than input (V_out > V_in)
    - As D approaches 1, efficiency drops due to parasitic resistances
    - Practical limit: V_out < 4-5x V_in
    - Input current is continuous (good for solar/battery applications)

## Next Steps

- [Buck-Boost Topologies](buck-boost.md) - Inverting and non-inverting variants
- [Flyback Converter](flyback.md) - Isolated boost-derived topology
- [PFC Boost](../../tutorials/acdc/pfc-basics.md) - Boost converter for power factor correction
