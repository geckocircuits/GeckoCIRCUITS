---
title: Solar Inverter Example
---

# Solar Inverter Example

Grid-tied photovoltaic inverter system.

## Overview

Solar (PV) inverters convert DC from solar panels to grid-synchronized AC:
- Maximum Power Point Tracking (MPPT)
- Grid synchronization
- Anti-islanding protection
- Reactive power support

## Specifications

| Parameter | Value |
|-----------|-------|
| PV Input | 200-450V DC |
| Output | 230V AC / 50 Hz |
| Rated Power | 5 kW |
| MPPT Range | 200-400V |
| THD | <3% |
| Efficiency | >97% |

## Circuit Files

- `pv_inverter_single.ipes` - Single-phase grid-tied
- `pv_inverter_mppt.ipes` - With P&O MPPT
- `pv_inverter_three.ipes` - Three-phase system

## System Architecture

```
┌──────────┐    ┌────────┐    ┌─────────┐    ┌──────┐
│ PV Array │───►│  MPPT  │───►│Inverter │───►│ Grid │
└──────────┘    │ DC-DC  │    │  DC-AC  │    └──────┘
                └────────┘    └─────────┘
```

## Theory

### PV Cell Model

$$I = I_{ph} - I_0(e^{V/nV_t} - 1) - \frac{V + IR_s}{R_{sh}}$$

### Maximum Power Point

$$P_{mpp} = V_{mpp} \cdot I_{mpp}$$

MPP voltage varies with:
- Irradiance
- Temperature
- Partial shading

### MPPT Algorithms

**Perturb & Observe (P&O):**
```
if (dP/dV > 0)
    V_ref = V_ref + ΔV
else
    V_ref = V_ref - ΔV
```

**Incremental Conductance:**
```
if (dI/dV = -I/V)
    MPP reached
else if (dI/dV > -I/V)
    V_ref = V_ref + ΔV  // Left of MPP
else
    V_ref = V_ref - ΔV  // Right of MPP
```

## Grid Synchronization

### Phase-Locked Loop (PLL)

```
        ┌───────────────────────────┐
vgrid ──► abc/dq ──► PI ──► VCO ──► θ
        └───────────────────────────┘
```

### Current Control

Grid current reference from power commands:
$$i_d^* = \frac{P^*}{v_d}, \quad i_q^* = \frac{Q^*}{v_d}$$

## Anti-Islanding

Detection methods:
- Frequency drift
- Voltage drift
- Active: frequency shift injection
- Passive: over/under voltage/frequency

## Exercises

1. **P&O MPPT:** Implement and tune tracking algorithm
2. **PLL Design:** Achieve fast grid synchronization
3. **Reactive Power:** Inject Q for grid support
4. **Partial Shading:** Observe MPPT behavior

## Related Resources

- [Wind Converter](wind.md)
- [PFC Converters](../power-supplies/pfc.md)
- [Three-Phase Inverter](../../tutorials/dcac/three-phase.md)
