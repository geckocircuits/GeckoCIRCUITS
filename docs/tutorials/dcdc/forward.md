---
title: Forward Converter
description: Isolated buck-derived DC-DC converter
---

# Tutorial: Forward Converter

Design and simulate an isolated forward converter.

**Difficulty:** Intermediate (2/3) | **Duration:** 30 minutes

## Theory

### Topology

The forward converter is an isolated version of the buck converter:

```
              Np : Ns
    Vin ──[FET]──●══════●──|>|──[L]──●── Vout
                 ║      ║       │      │
                 ║  Lm  ║     [D2]   [C]  [R]
                 ║      ║       │      │    │
                GND─────●──────●──────●────┘
```

Unlike the flyback, energy transfers during the switch ON time.

### Operating Principle

**Switch ON:** Power transfers through transformer to secondary. Output diode D1 conducts.

$$V_{sec} = n \cdot V_{in}, \quad V_L = n \cdot V_{in} - V_{out}$$

**Switch OFF:** Transformer resets. Freewheeling diode D2 conducts. Inductor supplies load.

$$V_L = -V_{out}$$

### Voltage Conversion

$$V_{out} = n \cdot D \cdot V_{in}$$

Where n = Ns/Np.

### Key Difference from Flyback

| Feature | Forward | Flyback |
|---------|---------|---------|
| Energy transfer | During ON time | During OFF time |
| Output filter | LC filter needed | Capacitor only |
| Output ripple | Lower (LC filtered) | Higher |
| Core utilization | Unipolar (needs reset) | Unipolar |
| Power level | Medium (50-500W) | Low-Medium (5-150W) |

### Core Reset

The forward converter needs a mechanism to reset the transformer core each cycle. With third winding:

$$D_{max} = \frac{N_p}{N_p + N_r}$$

For Np = Nr: D_max = 0.5

## Design Example

### Specifications

| Parameter | Value |
|-----------|-------|
| Input voltage | 48V DC |
| Output voltage | 12V DC |
| Output power | 100W |
| Switching frequency | 200 kHz |
| Max duty cycle | 0.45 |

### Calculations

**Turns ratio:**

$$n = \frac{V_{out}}{D \cdot V_{in}} = \frac{12}{0.45 \times 48} = 0.556 \approx 1:1.8$$

**Output inductor:**

$$L = \frac{(n \cdot V_{in} - V_{out}) \cdot D}{f_{sw} \cdot \Delta i_L} = 16\ \mu H$$

**Output capacitor:**

$$C = \frac{\Delta i_L}{8 \cdot f_{sw} \cdot \Delta V_{out}} = 8.7\ \mu F$$

## Exercises

### Exercise 1: Duty Cycle Sweep
Vary D from 0.1 to 0.45. Plot V_out and compare with theory.

### Exercise 2: Forward vs Flyback
Build both topologies for the same specs. Compare output ripple and switch stress.

### Exercise 3: Core Reset
Observe the magnetizing current waveform. Verify the core resets before the next ON period.

## Next Steps

- [Buck Converter](buck-converter.md) - The non-isolated equivalent
- [Flyback Converter](flyback.md) - Alternative isolated topology
- [Thermal Simulation](../../tutorials/thermal/loss-calculation.md) - Add losses
