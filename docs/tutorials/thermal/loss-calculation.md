---
title: "501 - Loss Calculation"
---

# 501 - Loss Calculation

Semiconductor conduction and switching loss modeling.

## Overview

Accurate loss calculation is essential for:
- Thermal design and heatsink sizing
- Efficiency optimization
- Component selection
- Reliability prediction

## Loss Components

### Total Semiconductor Losses

$$P_{total} = P_{cond} + P_{sw}$$

### Conduction Losses

**MOSFET:**
$$P_{cond,MOS} = I_{rms}^2 \cdot R_{DS(on)}$$

**IGBT:**
$$P_{cond,IGBT} = V_{CE(sat)} \cdot I_{avg} + r_{CE} \cdot I_{rms}^2$$

**Diode:**
$$P_{cond,D} = V_F \cdot I_{avg} + r_D \cdot I_{rms}^2$$

### Switching Losses

$$P_{sw} = (E_{on} + E_{off}) \cdot f_{sw}$$

Where switching energies depend on:
- Current at switching instant
- Voltage being switched
- Temperature (increases with temp)

## Temperature Dependence

### RDS(on) vs Temperature

$$R_{DS(on)}(T) = R_{DS(on)}(25°C) \cdot \left[1 + \alpha(T - 25)\right]$$

Typical α ≈ 0.004 to 0.006 /°C (doubles at 150°C)

### Switching Energy vs Temperature

$$E_{sw}(T) = E_{sw}(25°C) \cdot \left[1 + \beta(T - 25)\right]$$

## GeckoCIRCUITS Loss Models

### Setting Up Loss Parameters

1. **Open semiconductor properties**
2. **Enable thermal model**
3. **Enter datasheet values:**
   - RDS(on) or VCE(sat)
   - Eon, Eoff at reference conditions
   - Temperature coefficients

### Data Entry Table

| Parameter | MOSFET | IGBT | Diode |
|-----------|--------|------|-------|
| V_on | - | VCE(sat) | VF |
| R_on | RDS(on) | rCE | rD |
| E_on | Eon | Eon | Qrr×V |
| E_off | Eoff | Eoff | - |
| E_rr | - | - | Err |

## Example: Buck Converter Losses

### High-Side MOSFET

**Conduction:**
$$P_{cond,HS} = D \cdot I_L^2 \cdot R_{DS(on)}$$

**Switching:**
$$P_{sw,HS} = \frac{1}{2}V_{in} \cdot I_L \cdot (t_r + t_f) \cdot f_{sw}$$

### Low-Side Diode/MOSFET

**Conduction:**
$$P_{cond,LS} = (1-D) \cdot I_L \cdot V_F$$

### Inductor Losses

**DC Copper Loss:**
$$P_{Cu,DC} = I_{DC}^2 \cdot R_{DC}$$

**AC Copper Loss (skin effect):**
$$P_{Cu,AC} = I_{AC,rms}^2 \cdot R_{AC}$$

**Core Loss (Steinmetz):**
$$P_{core} = k \cdot f^{\alpha} \cdot B^{\beta} \cdot V_{core}$$

## Simulation Exercises

1. Measure losses at different load currents
2. Compare MOSFET vs IGBT at different frequencies
3. Analyze loss breakdown pie chart
4. Correlate losses with junction temperature rise

## Related Resources

- [502 - Junction Temperature](junction-temperature.md)
- [503 - Heatsink Design](heatsink-design.md)
