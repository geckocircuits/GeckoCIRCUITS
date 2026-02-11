---
title: "903 - Inductor Saturation"
---

# 903 - Inductor Saturation

Non-linear inductance modeling and saturation effects.

## Overview

Inductor saturation occurs when the core's magnetic flux density approaches Bsat, causing:
- Dramatic inductance drop
- Current spike
- Potential device damage
- Control instability

## Saturation Physics

### B-H Relationship

$$B = \mu_0 \mu_r(H) \cdot H$$

At saturation:
$$\mu_r \rightarrow 1$$ (core behaves like air)

### Inductance vs Current

$$L(i) = \frac{N^2 \cdot \mu_0 \mu_r(i) \cdot A_e}{l_e}$$

As i increases → H increases → μr decreases → L decreases

## Saturation Models

### Piecewise Linear

Simple three-region model:
```
L(i)
  │
Lnom├────────┐
  │         │
  │         └────────
Lsat├──────────────────
  │
  └─────────────────── i
       Isat
```

$$L(i) = \begin{cases} L_{nom} & |i| < I_{sat} \\ L_{sat} & |i| \geq I_{sat} \end{cases}$$

### Smooth Saturation

More realistic model:
$$L(i) = \frac{L_0}{1 + (i/I_{sat})^n}$$

Where n controls transition sharpness (typically 2-6)

### Jiles-Atherton

Physics-based model including:
- Anhysteretic magnetization
- Domain wall motion
- Energy loss (hysteresis)

## Impact on Converter Operation

### DC-DC Converter

**Normal operation (CCM):**
- Triangle current waveform
- Predictable ripple

**Saturated operation:**
- Current spike during on-time
- Potential switch damage
- EMI increase

### Current at Saturation

$$I_{sat} = \frac{B_{sat} \cdot l_e}{\mu_0 \mu_r \cdot N}$$

With air gap:
$$I_{sat} = \frac{B_{sat} \cdot l_g}{\mu_0 \cdot N}$$

## Design for Saturation Avoidance

### Method 1: Air Gap

Add gap to reduce effective permeability:
$$\mu_{eff} = \frac{\mu_r}{1 + \mu_r \cdot l_g/l_e}$$

Benefits:
- Stores energy in gap
- Soft saturation characteristic
- Stable inductance vs temperature

### Method 2: Larger Core

Increase Ae to reduce B for given flux:
$$B = \frac{L \cdot I_{peak}}{N \cdot A_e}$$

### Method 3: Distributed Gap Materials

Powder cores (Kool Mμ, MPP, etc.):
- Inherent distributed gap
- Soft saturation
- Higher core losses

## Saturation Effects Analysis

### Buck Converter Example

| Parameter | Before Sat | After Sat |
|-----------|------------|-----------|
| Inductance | 100 µH | 10 µH |
| di/dt | 50 A/µs | 500 A/µs |
| Peak current | 10 A | unlimited |

### Transient Response

During load step:
1. Current increases toward new operating point
2. If current exceeds Isat, L drops suddenly
3. di/dt increases dramatically
4. Current overshoots

## GeckoCIRCUITS Saturation Modeling

### Using Non-linear Inductor

1. Select **saturating inductor** component
2. Enter parameters:
   - Nominal inductance L0
   - Saturation current Isat
   - Saturated inductance Lsat
3. Or use flux-linkage characteristic

### Using Magnetic Domain

1. Create permeance with saturation
2. Enter B-H curve data points
3. Connect winding to electrical circuit
4. Observe flux and inductance vs current

## Simulation Exercises

1. Compare linear vs saturating inductor
2. Observe current waveform at saturation boundary
3. Design air gap to prevent saturation
4. Analyze startup transient with saturation

## Design Guidelines

### Maximum Flux Density

| Core Material | Bsat (T) | Typical Use |
|---------------|----------|-------------|
| Ferrite (N87) | 0.39 | High freq |
| Ferrite (N49) | 0.49 | High Bsat |
| Kool Mμ | 1.0 | DC bias |
| Iron powder | 1.0-1.4 | Low cost |
| Nanocrystalline | 1.2 | High perf |

### Safety Margin

Design for:
$$I_{peak} \leq 0.8 \cdot I_{sat}$$

Account for:
- Load transients
- Temperature variation (Bsat decreases with temp)
- Component tolerance

## Related Resources

- [901 - Magnetic Domain](magnetic-domain.md)
- [902 - Transformer Design](transformer-design.md)
