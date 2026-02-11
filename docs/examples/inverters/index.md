---
title: Inverter Examples
---

# Inverter Examples

DC to AC power conversion circuits for motor drives, grid-tied systems, and HVDC applications.

## Examples

| Example | Description | Difficulty | Status |
|---------|-------------|------------|--------|
| [Single-Phase](single_phase/) | H-bridge inverter, PWM | Intermediate | Placeholder |
| [Three-Phase VSI](three_phase_vsi/) | Voltage source inverter | Intermediate | Placeholder |
| [Multilevel NPC](multilevel_npc/) | 3-level neutral-point-clamped | Advanced | Placeholder |
| [MMC Converter](mmc_converter/) | Modular multilevel converter | Advanced | Placeholder |

## Quick Reference

### Topologies Comparison

| Topology | Levels | THD | Efficiency | Applications |
|----------|--------|-----|------------|--------------|
| 2-Level VSI | 2 | High | Good | Motor drives |
| 3-Level NPC | 3 | Medium | Better | Medium voltage |
| 5-Level NPC | 5 | Low | High | High power |
| MMC | N+1 | Very Low | Excellent | HVDC, STATCOM |

### Voltage & Power Ratings

| Topology | Voltage Range | Power Range |
|----------|---------------|-------------|
| 2-Level | <1 kV | <1 MW |
| 3-Level NPC | 1-4 kV | 1-10 MW |
| 5-Level | 3-10 kV | 5-50 MW |
| MMC | 10-500 kV | 100-2000 MW |

### Key Concepts

- **Modulation Index (m):** Controls output voltage magnitude (0-1.15 with SVPWM)
- **Dead-time:** Prevents shoot-through, causes distortion
- **THD:** Total harmonic distortion, quality measure
- **Neutral-Point Balancing:** Critical for NPC operation

## PWM Strategies

| Strategy | Description | Best For |
|----------|-------------|----------|
| SPWM | Sinusoidal PWM, simplest | General purpose |
| SVPWM | Space vector, +15% voltage | High performance |
| DPWM | Discontinuous, lower losses | Efficiency |
| SHE-PWM | Selective harmonic elimination | Grid connection |
| NLM | Nearest level modulation | Multilevel |

## Design Equations

### 2-Level Inverter

Output voltage (line-to-line, fundamental):
```
V_LL = m × (√3/2) × Vdc × √2 = 0.612 × m × Vdc (RMS)
```

Maximum with SVPWM:
```
V_LL,max = 0.612 × 1.15 × Vdc = 0.704 × Vdc
```

### 3-Level NPC

Output voltage:
```
V_LL = m × (√3/2) × Vdc × √2 (same equation)
```

Device voltage stress:
```
V_device = Vdc / 2 (half of 2-level)
```

### MMC

Output voltage:
```
V_AC = m × Vdc / 2
```

Number of submodules:
```
N = Vdc / (2 × V_SM)
```

## Applications Overview

| Application | Typical Topology | Power |
|-------------|-----------------|-------|
| Industrial drives | 2L VSI | 1-500 kW |
| Medium voltage drives | 3L NPC | 1-10 MW |
| Wind turbines | 3L NPC, MMC | 2-15 MW |
| Solar inverters | 2L/3L | 1-100 kW |
| HVDC transmission | MMC | 100-2000 MW |
| STATCOM | MMC | 10-400 MVAr |

## Related Tutorials

- [401 - Single-Phase Inverter](../tutorials/4xx_dcac_inverters/401_single_phase_inverter/)
- [402 - Three-Phase Inverter](../tutorials/4xx_dcac_inverters/402_three_phase_inverter/)
- [403 - NPC Inverter](../tutorials/4xx_dcac_inverters/403_npc_inverter/)
- [Motor Drives](../motor_drives/)
