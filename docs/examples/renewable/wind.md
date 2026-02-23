---
title: Wind Converter Example
---

# Wind Converter Example

Variable-speed wind turbine power conversion system.

## Overview

Modern wind turbines use variable-speed operation:
- Maximum energy capture (MPPT)
- Full power conversion (Type 4)
- Grid code compliance
- Fault ride-through capability

## Specifications

| Parameter | Value |
|-----------|-------|
| Generator Type | PMSG |
| Rated Power | 2 MW |
| Speed Range | 8-20 RPM |
| Grid Connection | 690V, 50 Hz |
| DC Link | 1100V |

## Circuit Files

- `wind_pmsg_basic.ipes` - PMSG with back-to-back converter
- `wind_dfig.ipes` - Doubly-fed induction generator
- `wind_mppt.ipes` - With tip speed ratio MPPT

## System Architecture (Type 4 - Full Converter)

```
         Machine Side        Grid Side
┌───────┐  ┌─────────┐  ┌─────────┐  ┌──────┐
│ PMSG  │──│Rectifier│──│Inverter │──│ Grid │
└───────┘  └─────────┘  └─────────┘  └──────┘
    │           │            │
    └───────────┴────────────┘
                DC Link
```

## Theory

### Wind Power

$$P_{wind} = \frac{1}{2}\rho A v^3$$

Where:
- ρ = air density (1.225 kg/m³)
- A = swept area
- v = wind speed

### Power Coefficient

$$P_{turbine} = C_p(\lambda, \beta) \cdot P_{wind}$$

Maximum Cp ≈ 0.48 (Betz limit = 0.593)

### Tip Speed Ratio

$$\lambda = \frac{\omega R}{v}$$

Optimal λ ≈ 6-8 for modern turbines

## MPPT Strategy

### Tip Speed Ratio Control

```
Wind Speed ──► λ_opt ──► ω_ref ──► Speed Control ──► Generator
```

### Power Curve Control

```
Generator Speed ──► P_opt Curve ──► Torque Ref ──► Generator
```

$$P_{opt} = K_{opt} \cdot \omega^3$$

## Grid-Side Control

### Power Control

$$P^* = P_{turbine} - P_{losses}$$
$$Q^* = Q_{grid,ref}$$ (from grid operator)

### Low Voltage Ride-Through (LVRT)

During grid faults:
- Inject reactive current for voltage support
- Limit active power
- Maintain DC link voltage

## DFIG Alternative

Doubly-Fed Induction Generator:
- Reduced converter rating (30%)
- Partial speed range (±30% of sync)
- More complex control
- Brush maintenance

## Exercises

1. **MPPT Implementation:** Tip speed ratio tracking
2. **Power Limiting:** Pitch control above rated wind
3. **LVRT:** Simulate grid fault ride-through
4. **Reactive Power:** Grid voltage support

## Related Resources

- [Solar Inverter](solar.md)
- [PMSM FOC](../motor-drives/pmsm-foc.md)
- [Three-Phase Inverter](../../tutorials/dcac/three-phase.md)
