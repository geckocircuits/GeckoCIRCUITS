# 5xx - Thermal Simulation

Power loss calculation and thermal analysis for reliable converter design.

| Tutorial | Title | Difficulty | Materials |
|----------|-------|------------|-----------|
| [501](501_loss_calculation/) | Loss Calculation | 2/3 | circuit |
| [502](502_junction_temperature/) | Junction Temperature | 3/3 | circuits |
| [503](503_heatsink_design/) | Heatsink Design | 3/3 | README |

## Learning Objectives

- Calculate conduction and switching losses in semiconductors
- Model thermal impedance networks (Foster/Cauer)
- Estimate junction temperatures during operation
- Design cooling systems for target reliability
- Analyze transient thermal behavior

## Contents

### 501 - Loss Calculation
- `BuckBoost_thermal.ipes` - Thermal model of buck-boost converter
- Conduction loss: I²R, Vce×Ic
- Switching loss: Eon + Eoff at fsw

### 502 - Junction Temperature
- `BuckBoost_thermal_with_java.ipes` - Advanced thermal with Java blocks
- `ThreePhase-VSR_10kW_thermal.ipes` - 10kW VSR thermal analysis
- `ThreePhase-VSR_10kW_thermal_with_java.ipes` - Enhanced thermal model

### 503 - Heatsink Design
- Comprehensive README with design methodology
- Thermal resistance calculations
- Heatsink selection guidelines
- Transient thermal analysis

## Quick Reference

### Thermal Resistance Chain

```
Junction → [Rth,jc] → Case → [Rth,ch] → Heatsink → [Rth,ha] → Ambient
```

Tj = Ta + Ploss × (Rth,jc + Rth,ch + Rth,ha)

### Typical Thermal Resistances

| Component | Rth (K/W) | Notes |
|-----------|-----------|-------|
| IGBT module | 0.1-0.5 | Junction to case |
| Thermal grease | 0.05-0.2 | Case to heatsink |
| Small heatsink | 2-5 | Natural convection |
| Large heatsink | 0.2-1 | Forced air |

### Loss Equations

**Conduction:**
```
IGBT: Pcond = Vce0×Ic + Ron×Ic²
Diode: Pcond = Vf×Id + Rd×Id²
```

**Switching:**
```
Psw = (Eon + Eoff) × fsw × (Vdc/Vref) × (Ic/Iref)
```

## Prerequisites

- Complete 2xx DC-DC Converters (base circuits)
- Understanding of power semiconductor operation
- Basic heat transfer concepts

## Related Examples

- [Thermal Examples](../examples/thermal/)
- [Loss Calculation](501_loss_calculation/)
