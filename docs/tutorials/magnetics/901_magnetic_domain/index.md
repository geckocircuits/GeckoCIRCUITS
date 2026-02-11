---
title: Tutorial 901: Magnetic Domain Introduction
---

# Tutorial 901: Magnetic Domain Introduction

## Overview

Learn to model magnetic circuits using the permeance-capacitance analogy. This tutorial introduces magnetic domain simulation for transformers, inductors, and coupled magnetic components.

**Level:** Advanced (3/3)

**Duration:** 45-60 minutes

**Series:** Magnetics & Mechanical

**Status:** Placeholder - Circuit files to be added

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Understand the permeance-capacitance analogy
- [ ] Model simple magnetic circuits
- [ ] Simulate flux and MMF in magnetic components
- [ ] Connect magnetic domain to electrical circuits

## Prerequisites

- Complete [201 - Buck Converter](../../2xx_dcdc_converters/201_buck_converter/)
- Understanding of magnetic circuit fundamentals (reluctance, permeance, MMF)
- Familiarity with transformer operation

## Theory

### Permeance-Capacitance Analogy

Magnetic circuits can be modeled using electrical analogies:

| Magnetic Quantity | Symbol | Electrical Analog | Unit |
|-------------------|--------|-------------------|------|
| Magnetomotive Force (MMF) | F = N×I | Voltage | A-turns |
| Magnetic Flux | Φ | Current | Wb |
| Reluctance | R = l/(μA) | Resistance | A-turns/Wb |
| Permeance | P = 1/R | Conductance | Wb/A-turns |

### Magnetic Circuit Model

```
    MMF Source          Reluctance Path
    (N×I)               (Core + Air Gap)
      │                      │
      ○ ─────────────[R_core]────[R_gap]─────┐
      │                                       │
      └───────────────────────────────────────┘
                         Φ (flux)
```

### Core Material Properties

| Material | μr (relative) | Bsat (T) | Application |
|----------|---------------|----------|-------------|
| Air | 1 | - | Gap, leakage |
| Ferrite (MnZn) | 2000-5000 | 0.3-0.5 | High frequency |
| Ferrite (NiZn) | 100-1000 | 0.3-0.4 | EMI suppression |
| Iron powder | 50-100 | 1.0-1.5 | DC bias |
| Amorphous | 10000+ | 1.5 | Low loss |
| Silicon steel | 3000-5000 | 1.5-2.0 | 50/60 Hz |

### Saturation Modeling

Non-linear B-H characteristic:
```
B = Bsat × tanh(H/Hsat)  (simplified model)
```

Or using piecewise linear approximation:
```
     B
     │      ╱─────── Bsat
     │     ╱
     │    ╱
     │   ╱
     │  ╱
─────┼─╱──────────── H
     │╱
```

## Magnetic Components in GeckoCIRCUITS

### Available Components

| Component | Description | Parameters |
|-----------|-------------|------------|
| Inductor | Basic inductance | L, R (DCR) |
| Coupled Inductor | Mutual inductance | L1, L2, M, k |
| Ideal Transformer | Turns ratio | n1:n2 |
| Non-linear Inductor | With saturation | L(i), Bsat |

### Coupled Inductor Model

```
        ┌─────●─────┐
        │     ║     │
       L1     ║ M   L2
        │     ║     │
        └─────●─────┘

    M = k × √(L1 × L2)
    k = coupling coefficient (0-1)
```

## Building Magnetic Models

### Example 1: Simple Inductor with Core

1. Define core geometry:
   - Cross-section area: Ac = 100 mm²
   - Magnetic path length: lc = 50 mm
   - Air gap: lg = 1 mm

2. Calculate reluctances:
   ```
   Rc = lc / (μ0 × μr × Ac)
   Rg = lg / (μ0 × Ac)
   ```

3. Calculate inductance:
   ```
   L = N² / (Rc + Rg) = N² × P_total
   ```

### Example 2: Flyback Transformer

Model as coupled inductor with:
- Primary inductance Lp (magnetizing)
- Leakage inductance Llk
- Turns ratio n
- Coupling coefficient k < 1

```
    Primary           Secondary
    ┌──[Llk]──●═══════●──┐
    │         ║       ║   │
    │        Lm      Ls   │
    │         ║       ║   │
    └─────────●═══════●───┘
              k = 0.95-0.99
```

### Example 3: Saturable Reactor

For controlled inductance (magnetic amplifier):
- DC bias winding controls saturation level
- AC winding provides variable inductance
- Used in: dimmers, welding power supplies

## Simulation Setup

### Time Step Considerations

Magnetic domain may require smaller time step:
- Fast flux changes during switching
- Core loss modeling needs accurate dB/dt

Recommended: dt < 1/(100 × fs)

### Initial Conditions

- Set initial flux or inductor current
- Avoid starting from zero (long settling)
- For transformers, ensure no DC flux buildup

## Expected Results

### Waveforms to Observe

1. **Flux (Φ):** Should stay below saturation
2. **Flux Density (B):** B = Φ/Ac
3. **Magnetizing Current:** Non-linear at saturation
4. **Core Loss:** Increases with frequency and Bmax

### Saturation Effects

When B → Bsat:
- Inductance drops dramatically
- Current spikes occur
- Losses increase
- Waveform distortion

## Exercises

### Exercise 1: Linear Inductor
1. Model a 100μH inductor with ferrite core
2. Apply 10V at 100kHz
3. Verify: V = L × di/dt

### Exercise 2: Saturation
1. Add saturation (Bsat = 0.3T) to the inductor
2. Increase voltage until saturation occurs
3. **Observe:** Current waveform distortion

### Exercise 3: Coupled Inductor (Flyback)
1. Model flyback transformer: Lp = 500μH, n = 10:1, k = 0.98
2. Simulate energy transfer
3. **Measure:** Leakage inductance effect on voltage spike

### Exercise 4: Core Loss
1. Add core loss model (Steinmetz equation)
2. Compare efficiency at 50kHz vs 200kHz
3. **Calculate:** Core loss contribution to total loss

## Core Loss Modeling

### Steinmetz Equation

```
Pcore = k × f^α × Bmax^β × Volume
```

Typical coefficients for ferrite:
- k ≈ 1.5 (material constant)
- α ≈ 1.5 (frequency exponent)
- β ≈ 2.5 (flux density exponent)

### iGSE (Improved Generalized Steinmetz)

For non-sinusoidal waveforms:
```
Pcore = (1/T) × ∫ ki |dB/dt|^α × (ΔB)^(β-α) dt
```

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Simulation diverges | Rapid saturation | Reduce time step |
| Unrealistic current spike | No saturation model | Add Bsat limit |
| DC flux buildup | Volt-second imbalance | Check transformer reset |
| High losses | Operating near Bsat | Reduce flux density or increase core |

## Related Tutorials

- [902 - Transformer Design](../902_transformer_design/) - Detailed transformer modeling
- [903 - Inductor Saturation](../903_inductor_saturation/) - Saturation effects
- [Flyback Converter](../examples/basic_topologies/flyback_converter/) - Application example

## References

1. McLyman, W.T. "Transformer and Inductor Design Handbook"
2. Kazimierczuk, M.K. "High-Frequency Magnetic Components"
3. Erickson & Maksimovic, Chapter 13: "Basic Magnetics Theory"

## Circuit Files

> **Status:** Placeholder - Circuit files to be created
> - `magnetic_basic.ipes` - Simple magnetic circuit
> - `inductor_saturation.ipes` - Saturable inductor model
> - `coupled_inductor.ipes` - Flyback transformer model

---
*Tutorial Version: 1.0 (Placeholder)*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
