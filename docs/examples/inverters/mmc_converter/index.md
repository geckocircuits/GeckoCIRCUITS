---
title: MMC - Modular Multilevel Converter
---

# MMC - Modular Multilevel Converter

## Overview

The Modular Multilevel Converter (MMC) is a scalable multilevel topology used in high-voltage direct current (HVDC) transmission, STATCOM, and large motor drives. Each arm contains multiple submodules (SM) that can be independently controlled to synthesize high-quality voltage waveforms.

**Difficulty:** Advanced

**Estimated Time:** 60+ minutes

**Status:** Placeholder - Circuit files to be added

## Learning Objectives

- Understand MMC topology and operating principles
- Model half-bridge and full-bridge submodules
- Implement arm balancing and circulating current control
- Simulate HVDC point-to-point transmission

## Topology

### Single-Phase MMC

```
         ┌─────────┐
    Vdc+ │   SM    │
    ─────┤   SM    │ Upper Arm
         │   SM    │
         │   Larm  │
         └────┬────┘
              │
              ├────────── AC Output
              │
         ┌────┴────┐
         │   Larm  │
         │   SM    │
    ─────┤   SM    │ Lower Arm
    Vdc- │   SM    │
         └─────────┘
```

### Half-Bridge Submodule

```
       ┌────[S1]────┐
       │            │
  + ───┤           ─┼─── +
       │    Csm     │
  - ───┼────[S2]────┼─── -
       │            │
       └────────────┘

States:
  S1=ON, S2=OFF → Vsm = Vcap (inserted)
  S1=OFF, S2=ON → Vsm = 0 (bypassed)
```

### Three-Phase MMC

```
                      Vdc+
                        │
         ┌──────────────┼──────────────┐
         │              │              │
      ┌──┴──┐        ┌──┴──┐        ┌──┴──┐
      │ SMs │        │ SMs │        │ SMs │
      │ N   │        │ N   │        │ N   │
      │ Larm│        │ Larm│        │ Larm│
      └──┬──┘        └──┬──┘        └──┬──┘
         │              │              │
    A ───┼──────  B ────┼──────  C ────┤
         │              │              │
      ┌──┴──┐        ┌──┴──┐        ┌──┴──┐
      │ Larm│        │ Larm│        │ Larm│
      │ N   │        │ N   │        │ N   │
      │ SMs │        │ SMs │        │ SMs │
      └──┬──┘        └──┬──┘        └──┬──┘
         │              │              │
         └──────────────┼──────────────┘
                        │
                      Vdc-
```

## Key Parameters

| Parameter | Symbol | Typical Value | Unit |
|-----------|--------|---------------|------|
| DC Voltage | Vdc | 320-640 | kV (HVDC) |
| Submodules per arm | N | 20-400 | - |
| SM Capacitance | Csm | 5-20 | mF |
| Arm Inductance | Larm | 30-100 | mH |
| Switching Frequency | fsw | 100-200 | Hz (per SM) |
| Power Rating | P | 100-2000 | MW |

## MMC Advantages

| Feature | Benefit |
|---------|---------|
| Modularity | Easy scaling, redundancy |
| Low THD | Near-sinusoidal output |
| Low dv/dt | Small voltage steps |
| Fault tolerance | Bypass failed submodules |
| Efficiency | Low switching losses |

## Control Architecture

### Hierarchy

```
┌─────────────────────────────────────────────┐
│  System Level Control                        │
│  (Power, DC voltage, AC voltage)             │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  Arm Level Control                           │
│  (Circulating current, arm energy)           │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  Submodule Level Control                     │
│  (Capacitor voltage balancing, PWM)          │
└─────────────────────────────────────────────┘
```

### Capacitor Voltage Balancing

1. Measure all SM capacitor voltages
2. Sort by voltage (ascending/descending)
3. Insert SMs based on arm current direction:
   - Current charging caps → insert lowest voltage SMs
   - Current discharging caps → insert highest voltage SMs

### Circulating Current Control

The circulating current flows within the converter:
```
icirc = (iupper + ilower) / 2
```

Control objectives:
- Suppress 2nd harmonic component
- Reduce losses
- Prevent capacitor voltage ripple

## Modulation Strategies

### Nearest Level Modulation (NLM)

1. Calculate reference voltage
2. Round to nearest achievable level
3. Select which SMs to insert

### Phase-Shifted PWM (PS-PWM)

- Each SM has dedicated carrier
- Carriers phase-shifted by 360°/N
- Equivalent switching frequency = N × fsw

### Sorting-Based Modulation

- At each switching instant:
  1. Determine number of SMs to insert
  2. Sort SMs by capacitor voltage
  3. Select based on current direction

## Simulation Considerations

### Model Complexity

| Level | SM Model | Use Case |
|-------|----------|----------|
| Detailed | Full switching model | Design, control tuning |
| Averaged | Average voltage source | System-level studies |
| Reduced | Equivalent Thévenin | Large grid simulations |

### Time Step Requirements

For detailed model:
- dt < 1/(10 × fsw × N)
- May need μs time step

For averaged model:
- dt can be larger (ms range)

## Exercises

### Exercise 1: Single Submodule
1. Build half-bridge SM model
2. Apply switching signals
3. Observe Vcap and Vsm

### Exercise 2: Single-Arm MMC
1. Build arm with 4 SMs
2. Implement NLM modulation
3. Verify voltage levels

### Exercise 3: Capacitor Balancing
1. Implement sorting algorithm
2. Observe Vcap convergence
3. Test with unbalanced initial conditions

### Exercise 4: Three-Phase MMC
1. Build complete 3-phase MMC
2. Implement circulating current control
3. Simulate active power transfer

## Applications

### HVDC Transmission

```
AC Grid 1 ─[MMC1]─ DC Cable ─[MMC2]─ AC Grid 2
```

### STATCOM

```
Grid ─── Transformer ─── MMC (no DC link)
         (reactive power compensation)
```

### Medium-Voltage Drives

```
MV Grid ─[Rectifier MMC]─ DC Link ─[Inverter MMC]─ Motor
```

## Related Examples

- [NPC Inverter](../multilevel_npc/) - 3-level comparison
- [Three-Phase VSI](../three_phase_vsi/) - 2-level baseline
- [Motor Drives](../../motor_drives/) - Drive applications

## References

1. Lesnicar, A., Marquardt, R. "An Innovative Modular Multilevel Converter Topology"
2. CIGRE TB 492: "VSC Transmission"
3. Hagiwara, M., Akagi, H. "Control and Experiment of Pulsewidth-Modulated MMC"

## Circuit Files

> **Status:** Placeholder
> - `mmc_submodule.ipes` - Single half-bridge SM
> - `mmc_single_arm.ipes` - One arm with 4 SMs
> - `mmc_single_phase.ipes` - Full single-phase MMC
> - `mmc_three_phase.ipes` - Three-phase MMC
> - `mmc_hvdc.ipes` - Point-to-point HVDC

---
*Example Version: 1.0 (Placeholder)*
*Last updated: 2026-02*
*GeckoCIRCUITS v1.0*
