---
title: "902 - Transformer Design"
---

# 902 - Transformer Design

High-frequency transformer modeling with leakage and parasitics.

## Overview

Transformer modeling in power electronics requires:
- Magnetizing inductance
- Leakage inductance
- Winding capacitance
- Core losses

## Transformer Equivalent Circuits

### Ideal Transformer

$$\frac{V_1}{V_2} = \frac{N_1}{N_2} = n$$
$$\frac{I_1}{I_2} = \frac{N_2}{N_1} = \frac{1}{n}$$

### Practical Model

```
  Primary Side          Ideal TX         Secondary Side
      │                   │ │                  │
     Rp    Llk,p         ┌┴─┴┐        Llk,s   Rs
  ●──/\/\──⊏⊐──┬────────┤ n:1├────────⊏⊐──/\/\──●
               │        └┬─┬┘
              Lm         │ │
               │
              ═╧═
```

Where:
- Lm = magnetizing inductance
- Llk,p, Llk,s = leakage inductances
- Rp, Rs = winding resistances

## Magnetic Circuit Model

### Permeance Network

```
         P_leak,p      P_leak,s
   MMF1 ────┬──────┬──────┬──── MMF2
            │      │      │
         P_gap   P_core   │
            │      │      │
   ─────────┴──────┴──────┴─────
```

### Parameter Extraction

**Magnetizing Inductance:**
$$L_m = N_1^2 \cdot P_{core}$$

**Leakage Inductance:**
$$L_{lk} = N^2 \cdot P_{leak}$$

## Design Procedure

### Step 1: Core Selection

Area product method:
$$A_p = A_e \cdot A_w = \frac{P_{out}}{K_f \cdot K_u \cdot J \cdot B_{max} \cdot f}$$

Where:
- Ae = core cross-section
- Aw = window area
- Kf = waveform factor
- Ku = window utilization
- J = current density
- Bmax = peak flux density

### Step 2: Turns Calculation

**Primary Turns:**
$$N_1 = \frac{V_1}{4 \cdot f \cdot B_{max} \cdot A_e}$$

**Secondary Turns:**
$$N_2 = \frac{N_1}{n}$$

### Step 3: Wire Selection

Current density (typical 3-5 A/mm² for natural cooling):
$$A_{wire} = \frac{I_{rms}}{J}$$

Skin depth at frequency f:
$$\delta = \sqrt{\frac{\rho}{\pi f \mu}}$$

### Step 4: Leakage Inductance

Depends on winding arrangement:
- Interleaved: lower leakage
- Separated: higher leakage

$$L_{lk} \approx \frac{\mu_0 N^2 MLT}{3 h_w} \cdot \left(n_p \cdot b_p + \frac{n_i \cdot b_i}{3}\right)$$

## Winding Arrangements

### Primary-Secondary (P-S)
- Simple construction
- High leakage inductance
- Suitable for flyback

### Interleaved (P-S-P-S)
- Reduced leakage
- Lower proximity effect
- More complex

### Sandwich (P-S-P)
- Moderate leakage
- Good balance

## Parasitic Capacitances

### Inter-winding Capacitance

$$C_{ps} = \frac{\epsilon_0 \epsilon_r A_{overlap}}{d_{insulation}}$$

Important for:
- Common-mode noise
- High dv/dt applications

### Intra-winding Capacitance

Affects self-resonance frequency:
$$f_{res} = \frac{1}{2\pi\sqrt{L_{lk} C_{self}}}$$

## GeckoCIRCUITS Transformer Model

### Setting Up

1. Use **coupled inductor** component
2. Set:
   - Turns ratio
   - Magnetizing inductance
   - Leakage inductance (referred to primary)
   - Winding resistances
3. For detailed analysis:
   - Use permeance network
   - Add saturation to core permeance

## Simulation Exercises

1. Compare ideal vs practical transformer
2. Measure leakage inductance effect on regulation
3. Model flyback transformer with gap
4. Analyze resonance with capacitance

## Related Resources

- [901 - Magnetic Domain](magnetic-domain.md)
- [903 - Inductor Saturation](inductor-saturation.md)
- [Flyback Example](../../examples/basic/flyback.md)
