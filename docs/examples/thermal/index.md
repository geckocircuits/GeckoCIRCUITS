---
title: Thermal Analysis Examples
---

# Thermal Analysis Examples

Power semiconductor thermal modeling and heatsink design for reliable operation.

## Examples

| Example | Description | Difficulty |
|---------|-------------|------------|
| [Loss Calculation](loss_calculation/) | Conduction and switching losses | Intermediate |
| [Junction Temperature](junction_temperature/) | Thermal network modeling | Intermediate |
| [Thermal Networks](thermal_networks/) | Foster/Cauer models | Advanced |

## Quick Reference

### Thermal Resistance Chain

```
Tj ──[Rth,jc]── Tc ──[Rth,ch]── Ts ──[Rth,ha]── Ta
     Junction    Case    Interface   Heatsink   Ambient
```

### Typical Values

| Component | Rth (K/W) | Notes |
|-----------|-----------|-------|
| Rth,jc (IGBT) | 0.1-0.5 | From datasheet |
| Rth,jc (MOSFET) | 0.5-2.0 | Depends on package |
| Rth,ch (thermal grease) | 0.05-0.2 | With good contact |
| Rth,ch (thermal pad) | 0.2-0.5 | Electrical isolation |
| Rth,ha (natural convection) | 1-5 | Small heatsink |
| Rth,ha (forced air) | 0.1-1 | With fan |

### Power Loss Equations

**Conduction Loss (IGBT):**
```
Pcond = Vce(sat) × Ic × D + Ic² × Ron × D
```

**Switching Loss:**
```
Psw = (Eon + Eoff) × fs
```

## Design Flow

1. Calculate power losses (conduction + switching)
2. Determine maximum allowable Tj (with margin)
3. Calculate required total Rth
4. Select heatsink with Rth,ha meeting requirement
5. Verify with thermal simulation

## Related Tutorials

- [501 - Loss Calculation](../tutorials/5xx_thermal_simulation/501_loss_calculation/)
- [502 - Junction Temperature](../tutorials/5xx_thermal_simulation/502_junction_temperature/)
- [503 - Heatsink Design](../tutorials/5xx_thermal_simulation/503_heatsink_design/)
