# [Example Name]

## Overview

Brief description of what this example demonstrates and its practical applications.

**Difficulty:** [Beginner | Intermediate | Advanced]

**Estimated Time:** [XX minutes]

## Learning Objectives

After completing this example, you will be able to:
- Objective 1
- Objective 2
- Objective 3

## Prerequisites

- Basic understanding of power electronics
- Familiarity with GeckoCIRCUITS interface
- [Other prerequisites]

## Circuit Description

### Topology

[Include schematic description or reference to image]

```
  +Vin ─────[Switch]─────[L]─────┬───── +Vout
                                 │
                [D]              [C]    [R]
                 │                │      │
  GND ───────────┴────────────────┴──────┴───── GND
```

### Key Parameters

| Parameter | Symbol | Value | Unit | Description |
|-----------|--------|-------|------|-------------|
| Input Voltage | Vin | 48 | V | DC supply voltage |
| Output Voltage | Vout | 12 | V | Regulated output |
| Switching Frequency | fs | 100 | kHz | PWM frequency |
| Inductance | L | 100 | uH | Output inductor |
| Capacitance | C | 100 | uF | Output capacitor |

### Operating Principle

Detailed explanation of how the circuit operates, including:
1. Operating modes (e.g., CCM, DCM)
2. Control strategy
3. Key waveforms to observe

## Circuit Files

| File | Description |
|------|-------------|
| `example_basic.ipes` | Basic topology without control |
| `example_controlled.ipes` | Complete circuit with feedback control |

## Simulation Setup

### Step 1: Open the Circuit
1. Launch GeckoCIRCUITS
2. File > Open > Navigate to this example folder
3. Select `example_basic.ipes`

### Step 2: Configure Simulation
- **Simulation time:** 10 ms
- **Time step:** 100 ns (automatic)
- **Solver:** Trapezoidal (default)

### Step 3: Run and Observe
1. Click the Run button (or press F5)
2. Observe the following waveforms:
   - Output voltage ripple
   - Inductor current
   - Switch voltage stress

## Expected Results

### Steady-State Waveforms

| Signal | Expected Value | Ripple |
|--------|----------------|--------|
| Output Voltage | 12 V | ±0.5% |
| Inductor Current | 2.5 A | 1 A p-p |
| Efficiency | 95% | - |

[Reference to screenshot or expected waveform description]

## Key Equations

### Duty Cycle
```
D = Vout / Vin = 12/48 = 0.25
```

### Inductor Current Ripple
```
ΔI_L = (Vin - Vout) * D / (fs * L)
```

### Output Voltage Ripple
```
ΔV_out = ΔI_L / (8 * fs * C)
```

## Exercises

### Exercise 1: Parameter Variation
1. Increase the load resistance from 10Ω to 5Ω
2. Observe the effect on output voltage and inductor current
3. **Question:** Does the converter remain in CCM?

### Exercise 2: Frequency Analysis
1. Change the switching frequency from 100kHz to 50kHz
2. Compare the inductor current ripple
3. **Question:** What is the minimum inductance for CCM at 50kHz?

### Exercise 3: Transient Response
1. Apply a step load change at t=5ms
2. Measure the voltage undershoot and recovery time
3. **Question:** How does the output capacitance affect recovery?

## Troubleshooting

| Issue | Possible Cause | Solution |
|-------|---------------|----------|
| Simulation diverges | Time step too large | Reduce max time step |
| Unexpected waveforms | Initial conditions | Run for longer settling time |
| High losses | Component parameters | Check switch Ron, diode Vf |

## Related Examples

- [Boost Converter](../boost_converter/) - Step-up topology
- [Buck-Boost](../buck_boost_converter/) - Inverting topology
- [Thermal Analysis](../../thermal/loss_calculation/) - Adding loss models

## References

1. Erickson, R.W., Maksimovic, D. "Fundamentals of Power Electronics"
2. Mohan, N., Undeland, T.M., Robbins, W.P. "Power Electronics"
3. [Link to application note or paper]

---
*Last updated: 2026-02*
*GeckoCIRCUITS v1.0*
