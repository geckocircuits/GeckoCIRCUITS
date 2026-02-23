---
title: Buck-Boost Topologies
description: Inverting and non-inverting step-up/down converters
---

# Tutorial: Buck-Boost Topologies

Explore converter topologies that can both step up and step down voltage.

**Difficulty:** Intermediate (2/3) | **Duration:** 30 minutes

**Circuit files:**
- `resources/tutorials/2xx_dcdc_converters/203_buck_boost/buckBoost_simple.ipes`
- `resources/tutorials/2xx_dcdc_converters/203_buck_boost/cuk_simple.ipes`
- `resources/tutorials/2xx_dcdc_converters/203_buck_boost/sepic_simple.ipes`

## Topologies Covered

| Topology | Output Polarity | Output Range | Continuous Input Current |
|----------|----------------|--------------|------------------------|
| Buck-Boost | Inverted | 0 to -inf | No |
| Cuk | Inverted | 0 to -inf | Yes |
| SEPIC | Non-inverted | 0 to +inf | Yes |

## Inverting Buck-Boost

### Topology

```
    Vin ──[FET]──●──[LLLLL]──●── Vout (negative)
                 │            │
               [D]          [C]  [R]
                 │            │    │
                GND ──────────●────┘
```

### Voltage Ratio

$$V_{out} = -\frac{D}{1-D} \times V_{in}$$

| D | V_out/V_in | Mode |
|---|-----------|------|
| 0.25 | -0.33 | Step-down |
| 0.50 | -1.00 | Unity |
| 0.75 | -3.00 | Step-up |

!!! note "Inverted Output"
    The basic buck-boost produces a negative output voltage relative to input ground.

## SEPIC Converter

### Topology

The SEPIC (Single-Ended Primary-Inductor Converter) provides non-inverted output:

```
         L1            C_coupling     D
    Vin──[LLLLL]──●────||────●──|>|──●── Vout
                  │          │       │
                [FET]      [L2]    [C]  [R]
                  │          │       │    │
                 GND────────●───────●────┘
```

### Voltage Ratio

Same as buck-boost magnitude, but non-inverted:

$$V_{out} = \frac{D}{1-D} \times V_{in}$$

### Advantages

- Non-inverted output
- Continuous input current (good for battery/solar)
- Can step up or step down

### Disadvantages

- Two inductors and coupling capacitor
- Higher component count than buck-boost
- Pulsating output current

## Cuk Converter

### Topology

```
         L1        C_coupling        L2
    Vin──[LLLLL]──●──||──●──[LLLLL]──●── Vout (negative)
                  │       │           │
                [FET]   [D]         [C]  [R]
                  │       │           │    │
                 GND─────●───────────●────┘
```

### Voltage Ratio

$$V_{out} = -\frac{D}{1-D} \times V_{in}$$

### Advantages

- Continuous input AND output current
- Low ripple at both ports
- Can couple L1 and L2 on same core

## Design Guidelines

### Coupling Capacitor (SEPIC/Cuk)

$$C_{coupling} = \frac{I_{out}}{f_{sw} \times \Delta V_C}$$

Typically 1-10 uF ceramic. Must handle RMS ripple current.

### Inductors

$$L_1 \geq \frac{V_{in} \times D}{f_{sw} \times \Delta i_{L1}}$$

$$L_2 \geq \frac{V_{out} \times (1-D)}{f_{sw} \times \Delta i_{L2}}$$

## Simulation

### Comparing Topologies

1. Open each circuit file
2. Set identical specs: V_in = 24V, D = 0.5, f_sw = 100 kHz
3. Run and compare output voltage, ripple, and waveform shapes

### What to Observe

| Measurement | Buck-Boost | SEPIC | Cuk |
|-------------|-----------|-------|-----|
| Output polarity | Negative | Positive | Negative |
| Input current | Pulsating | Continuous | Continuous |
| Output current | Continuous | Pulsating | Continuous |

## Exercises

### Exercise 1: Step-Up/Step-Down Transition
Sweep D from 0.2 to 0.8. Identify the crossover point where |V_out| = V_in.

### Exercise 2: SEPIC vs Buck-Boost
Compare efficiency of SEPIC and buck-boost at D = 0.5, 0.3, and 0.7.

### Exercise 3: Coupled Inductors
For the Cuk converter, try coupling L1 and L2 (k = 0.95). Observe the effect on ripple.

## Next Steps

- [Flyback Converter](flyback.md) - Isolated topology derived from buck-boost
- [Forward Converter](forward.md) - Isolated buck-derived topology
- [Thermal Simulation](../../tutorials/thermal/index.md) - Add loss and thermal analysis
