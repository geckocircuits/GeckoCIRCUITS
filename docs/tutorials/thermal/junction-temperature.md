---
title: "502 - Junction Temperature"
---

# 502 - Junction Temperature

Thermal network modeling and transient analysis.

## Overview

Junction temperature directly affects:
- Semiconductor reliability (Arrhenius law)
- Conduction losses (RDS(on) increases)
- Switching characteristics
- Safe Operating Area (SOA)

## Thermal Equivalent Circuit

### Electrical-Thermal Analogy

| Thermal | Electrical | Unit |
|---------|------------|------|
| Temperature T | Voltage V | °C / V |
| Heat Flow P | Current I | W / A |
| Thermal Resistance Rth | Resistance R | K/W / Ω |
| Thermal Capacitance Cth | Capacitance C | J/K / F |

### Basic Thermal Network

```
  Ploss      Rth,jc     Rth,ch     Rth,ha
   (I)   ●───/\/\/───●───/\/\/───●───/\/\/───●
         │           │           │           │
        Tj          Tc          Th          Ta
                   (Cth)       (Cth)        ⏚
```

## Thermal Network Models

### Foster Network

- Parameters from datasheet curve fitting
- **Cannot be cascaded** (nodes have no physical meaning)
- Faster simulation

```
        R1         R2         R3
   ●───/\/\/───●───/\/\/───●───/\/\/───●
       │           │           │
      C1          C2          C3
       │           │           │
       ⏚           ⏚           ⏚
```

### Cauer Network

- Physically meaningful nodes (material layers)
- **Can be cascaded** (junction→case→heatsink→ambient)
- Derived from Foster via mathematical transformation

```
        R1         R2         R3
   ●───/\/\/───●───/\/\/───●───/\/\/───●
               │           │           │
              C1          C2          C3
               │           │           │
               ⏚           ⏚           ⏚
```

## Setting Up Thermal Models

### Datasheet Parameters

From semiconductor datasheet:
- Rth,j-c (junction to case)
- Rth,c-h (case to heatsink) - depends on mounting
- Transient thermal impedance curve Zth(t)

### Extracting Foster Parameters

From Zth curve fitting:
$$Z_{th}(t) = \sum_{i=1}^{n} R_i \cdot (1 - e^{-t/\tau_i})$$

Where τᵢ = Rᵢ × Cᵢ

## Temperature Calculations

### Steady-State

$$T_j = T_a + P_{loss} \cdot (R_{th,jc} + R_{th,ch} + R_{th,ha})$$

### Transient Response

For step power change:
$$T_j(t) = T_a + P \cdot Z_{th}(t)$$

### Periodic Power

Average temperature + ripple:
$$T_{j,avg} = T_a + P_{avg} \cdot R_{th,total}$$
$$\Delta T_{j,ripple} = P_{peak} \cdot Z_{th}(t_{on})$$

## Design Guidelines

### Maximum Junction Temperature

| Device | Tj,max | Recommended Max |
|--------|--------|-----------------|
| Si MOSFET | 150-175°C | 125°C |
| Si IGBT | 150-175°C | 125°C |
| SiC MOSFET | 175-200°C | 150°C |
| GaN HEMT | 150°C | 125°C |

### Derating

Temperature margin for reliability:
$$T_{j,design} = T_{j,max} - \Delta T_{margin}$$

Typical margin: 25-50°C

## Simulation Exercises

1. Build Cauer thermal network from datasheet
2. Simulate startup thermal transient
3. Compare Foster vs Cauer response
4. Analyze temperature at different switching frequencies

## Related Resources

- [501 - Loss Calculation](loss-calculation.md)
- [503 - Heatsink Design](heatsink-design.md)
