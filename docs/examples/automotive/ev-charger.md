---
title: Level 2 EV Charger Example
---

# Level 2 EV Charger Example

Single-phase Power Factor Correction (PFC) frontend with isolated DC-DC converter for electric vehicle charging.

## Overview

A Level 2 EV charger provides:
- Power Factor Correction (PFC) for grid compliance
- Galvanic isolation via transformer
- Constant Power, Constant Voltage (CPCV) charging profile
- Grid synchronization and harmonics reduction
- Thermal management and safety monitoring

## Specifications

| Parameter | Value |
|-----------|-------|
| Input Grid Voltage | 230V AC, 50 Hz |
| Input Current (RMS) | 32 A |
| Output Voltage | 400V DC (target) |
| Output Power | 7.4 kW |
| PFC Topology | Boost converter |
| PFC Switching Frequency | 100 kHz |
| Isolation DC-DC | Full-bridge isolated converter |
| DC-DC Switching Frequency | 50 kHz |
| Power Factor Target | >0.99 |
| THD Target | <5% |

## Circuit Files

- `ev_charger_pfc_boost.ipes` - Single-phase PFC boost stage
- `ev_charger_isolated_dcdc.ipes` - Full-bridge isolated converter
- `ev_charger_complete.ipes` - Complete system with control
- `ev_charger_cpcv_control.ipes` - Charging profile implementation

## System Architecture

```
Grid (230V/50Hz)
     │
     ▼
  ┌─────┐
  │ EMI │
  │Filter│
  └──┬──┘
     │ (AC)
     ▼
  ┌─────────────┐
  │  PFC Boost  │ ──► 400V DC (controlled)
  │  Converter  │
  └──────┬──────┘
         │ (400V DC)
         ▼
  ┌──────────────────┐
  │ Full-Bridge ISO  │
  │   DC-DC Conv.    │
  └────────┬─────────┘
           │ (Output: 400V to Batt)
           ▼
      ┌────────┐
      │Battery │
      └────────┘
```

## Theory

### Power Factor Correction

The PFC boost converter corrects the grid current to be in phase with the sinusoidal voltage.

**Grid Voltage:**
$$v_g(t) = V_m \sin(\omega t)$$

**Desired Input Current (unity PF):**
$$i_g(t) = I_m \sin(\omega t)$$

**Instantaneous Power:**
$$p(t) = v_g(t) \cdot i_g(t) = \frac{V_m I_m}{2}[1 - \cos(2\omega t)]$$

Where the average power is P = (1/2)·Vm·Im = Vrms·Irms.

### PFC Boost Converter Operating Equations

Assuming continuous conduction mode (CCM):

**On-state (S1 closed):**
$$v_L = v_g - L\frac{di_L}{dt}$$

**Off-state (S1 open):**
$$v_L = v_g - V_{out} - L\frac{di_L}{dt}$$

**Boost voltage gain:**
$$M = \frac{V_{out}}{V_g} = \frac{1}{1-D}$$

Where D is the duty cycle.

### Isolated DC-DC Converter

Full-bridge converter with transformer isolation:

**Transformer Voltage Ratio:**
$$n = \frac{N_s}{N_p} = \frac{V_{out}}{V_{iso}}$$

**Forward and Reverse Power Transfer:**
- Forward: Primary switches drive secondary rectifier
- Reverse: Can support bidirectional power flow

## Control Structure

```
              Grid Voltage
              Sensing (Vgrid)
                    │
                    ▼
          ┌──────────────────┐
          │  Phase Detector  │
          │   (PLL/Zero-X)   │
          └────────┬─────────┘
                   │ Phase θ
       ┌───────────┴──────────┐
       ▼                      ▼
┌─────────────┐      ┌──────────────┐
│ Sine Gen    │      │Current Limit │
│ (Reference) │      │  Modulator   │
└────┬────────┘      └──────┬───────┘
     │                      │
     ▼                      ▼
┌───────────────────────────────┐
│   Average Current Control     │
│  (Averaging Feedback)         │
│  Iref = Igrid_target * sin(θ) │
└──────────────┬────────────────┘
               │
               ▼
         ┌──────────┐
         │   PI    │
         │ Control │
         └────┬────┘
              ▼
       ┌─────────────┐
       │ PWM for PFC │
       │   Boost     │
       └─────┬───────┘
             ▼
        ┌──────────────┐
        │ Gate Drivers │
        └────┬─────────┘
             ▼
        ┌──────────────┐
        │  Power Stage │
        └──────────────┘
```

### Control Loop Design

**Current Loop (PFC Boost):**

Reference current:
$$i_g^* = I_{max} \sin(\omega t + \phi)$$

Current error:
$$e_i = i_g^* - i_g$$

PI controller output:
$$d(t) = K_p \cdot e_i + K_i \int e_i \, dt$$

**Output Voltage Regulation:**

Outer loop controls the DC bus voltage:
$$K_p = 0.001, \quad K_i = 0.0001$$

Adjust Imax based on Vout regulation.

### CPCV Charging Profile

Typical EV charging sequence:

**Constant Power Phase:**
- Limited by maximum current (32A typical)
- Voltage ramps from 0 to Vbatt_max
- Power = 7.4 kW (constant)

**Constant Voltage Phase:**
- Voltage held at Vbatt_max (e.g., 400V)
- Current decreases as battery charges
- Power decreases toward end of charge

**Transition point:**
$$P = V_{batt} \cdot I_{max}$$

## Harmonic Mitigation

**THD Reduction Techniques:**

1. **Current Ripple Reduction:**
   - Increase inductor value (but limits response)
   - Use current-mode control with slope compensation

2. **Frequency Selection:**
   - fsw >> fgrid × N (where N is desired harmonic number)
   - Typical: 100 kHz >> 50 Hz

3. **EMI Filtering:**
   - Input LC filter: L = 3-5 μH, C = 100-330 μF
   - Prevents high-frequency switching ripple

4. **Deadtime Compensation:**
   - Account for gate driver delays
   - Typically 200-500 ns deadtime

## Design Steps

### Step 1: PFC Boost Inductor Design

Ripple current target: 20% of output current
$$L = \frac{V_g(1-D)}{f_{sw} \Delta I_L}$$

Minimum inductance for CCM:
$$L_{min} = \frac{(1-D)V_g}{2f_{sw} I_{out}}$$

### Step 2: Boost Output Capacitor

Voltage ripple limit (e.g., 2%):
$$C = \frac{I_{out} D}{f_{sw} \cdot V_r}$$

Select ceramic or film capacitor, 400V rated.

### Step 3: Transformer Design

- Core: EI or EF core, ferrite (e.g., N67)
- Turns ratio determined by desired isolation voltage
- Leakage inductance: 5-10% of primary inductance

### Step 4: Secondary Rectifier

- Schottky or SiC diodes for low voltage drop
- PRV ≥ 2× peak secondary voltage
- Thermal management for forward loss

## Exercises

1. **PFC Fundamentals:** Measure grid current and voltage, verify power factor >0.98
2. **Input Filter Design:** Add L-C EMI filter and observe THD improvement
3. **CPCV Profile:** Implement charging algorithm, simulate battery charging cycle
4. **Efficiency Mapping:** Measure efficiency vs. load (25%, 50%, 75%, 100%)
5. **Thermal Analysis:** Simulate device losses and estimate heatsink requirements
6. **Grid Compliance:** Verify compliance with EN 61000-3-2 harmonic limits

## Related Resources

- [PFC Converters](../power-supplies/pfc.md)
- [Isolated DC-DC Converters](../power-supplies/llc.md)
- [DAB Converter](../power-supplies/dab.md)
- [502 - Junction Temperature](../../tutorials/thermal/junction-temperature.md)
- [401 - Two-Pulse Bridge Rectifier](../../tutorials/rectifiers/basic-rectifiers.md)
