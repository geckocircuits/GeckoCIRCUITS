---
title: "901 - Magnetic Domain"
---

# 901 - Magnetic Domain

Permeance-capacitance modeling for magnetic circuits.

## Overview

GeckoCIRCUITS uses the **permeance-capacitance analogy** for magnetic circuit simulation, enabling:
- Core saturation modeling
- Leakage inductance effects
- Coupled inductors and transformers
- Non-linear magnetic behavior

## Magnetic-Electrical Analogy

### Permeance-Capacitance Model

| Magnetic | Electrical | Unit |
|----------|------------|------|
| MMF (F) | Voltage (V) | A-turns / V |
| Flux (Φ) | Charge (Q) | Wb / C |
| dΦ/dt | Current (I) | V / A |
| Permeance (P) | Capacitance (C) | H / F |
| Reluctance (R) | 1/C | 1/H / 1/F |

### Why This Analogy?

The permeance-capacitance model:
- Allows integration with electrical circuit
- Handles non-linear permeability naturally
- Supports multiple windings on single core
- Models leakage paths explicitly

## Basic Components

### Permeance (Magnetic Capacitor)

$$P = \frac{\mu_0 \mu_r A}{l}$$

Where:
- μ₀ = 4π × 10⁻⁷ H/m
- μᵣ = relative permeability
- A = cross-sectional area
- l = magnetic path length

### Winding (Gyrator)

Converts electrical current to MMF:
$$F = N \cdot i$$
$$v = N \cdot \frac{d\Phi}{dt}$$

### Air Gap

$$P_{gap} = \frac{\mu_0 A_{eff}}{l_g}$$

With fringing factor for accurate modeling.

## Building Magnetic Circuits

### Simple Inductor

```
Electrical:          Magnetic:

  ●──[winding]──●    MMF ──[P_core]── GND
       │  │               │
      ~~~        Leakage:[P_leak]
       N turns            │
                         GND
```

### Transformer (Two Windings)

```
    Primary           Secondary
       │                 │
   [winding]         [winding]
       │                 │
       └──[P_core]──────┘
              │
          [P_leak]
              │
             GND
```

## Saturation Modeling

### B-H Curve

Non-linear permeability:
$$B = f(H) = \mu_0 \mu_r(H) \cdot H$$

### Jiles-Atherton Model

Physics-based hysteresis model with parameters:
- Ms = saturation magnetization
- a = domain wall density
- k = pinning coefficient
- c = reversibility
- α = domain coupling

### Piecewise Linear

Simpler approach with:
- Initial permeability μᵢ
- Saturation flux Bsat
- Saturated permeability μsat ≈ μ₀

## Core Loss Modeling

### Steinmetz Equation

$$P_v = k \cdot f^\alpha \cdot B^\beta$$

Typical values:
- Ferrite: α ≈ 1.3, β ≈ 2.5
- Iron powder: α ≈ 1.5, β ≈ 2.0

### Improved GSE (iGSE)

For non-sinusoidal waveforms:
$$P_v = \frac{1}{T}\int_0^T k_i \left|\frac{dB}{dt}\right|^\alpha (\Delta B)^{\beta-\alpha} dt$$

## GeckoCIRCUITS Setup

### Creating Magnetic Components

1. **Add permeance element** from magnetic component library
2. **Set core parameters:**
   - Material (ferrite, iron powder)
   - Dimensions (A, l)
   - Saturation characteristics
3. **Add windings** and connect to electrical circuit
4. **Add leakage permeance** if needed

### Material Database

GeckoCIRCUITS includes common materials:
- N87, N97, N49 (ferrite)
- Kool Mμ, MPP (powder core)
- Nanocrystalline

## Simulation Exercises

1. Model inductor with saturation
2. Compare linear vs non-linear core
3. Observe flux vs current (B-H loop)
4. Calculate core losses at different frequencies

## Related Resources

- [902 - Transformer Design](transformer-design.md)
- [903 - Inductor Saturation](inductor-saturation.md)
