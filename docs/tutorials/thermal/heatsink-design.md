---
title: "503 - Heatsink Design"
---

# 503 - Heatsink Design

Thermal resistance calculation and heatsink selection.

## Overview

Heatsink design ensures junction temperature stays within safe limits under all operating conditions.

## Thermal Budget

### Required Heatsink Thermal Resistance

$$R_{th,ha} = \frac{T_{j,max} - T_a}{P_{loss}} - R_{th,jc} - R_{th,ch}$$

### Example Calculation

Given:
- Tj,max = 125°C (design limit)
- Ta,max = 50°C (ambient)
- Ploss = 50W
- Rth,jc = 0.5 K/W
- Rth,ch = 0.2 K/W (with thermal paste)

Required:
$$R_{th,ha} = \frac{125 - 50}{50} - 0.5 - 0.2 = 0.8 \text{ K/W}$$

## Heatsink Types

### Natural Convection

| Type | Rth Range | Application |
|------|-----------|-------------|
| TO-220 clip | 10-30 K/W | <5W |
| Extruded profile | 1-10 K/W | 5-50W |
| Large finned | 0.3-2 K/W | 50-200W |

### Forced Air Cooling

Thermal resistance with airflow:
$$R_{th}(v) = R_{th,natural} \cdot \sqrt{\frac{v_0}{v}}$$

Where v₀ ≈ 0.5 m/s (natural convection equivalent)

### Liquid Cooling

| Type | Rth Range |
|------|-----------|
| Cold plate | 0.05-0.2 K/W |
| Microchannel | 0.02-0.1 K/W |
| Jet impingement | 0.01-0.05 K/W |

## Thermal Interface Materials

### TIM Thermal Resistance

$$R_{th,ch} = \frac{t_{TIM}}{k_{TIM} \cdot A} + R_{contact}$$

### Material Comparison

| TIM Type | k (W/m·K) | Rth (typical) |
|----------|-----------|---------------|
| Thermal grease | 1-5 | 0.1-0.3 K/W |
| Phase change | 3-5 | 0.1-0.2 K/W |
| Thermal pad | 1-6 | 0.2-0.5 K/W |
| Solder | 50 | 0.01-0.05 K/W |

## Multi-Device Heatsinks

### Thermal Coupling

Devices share heatsink → thermal interaction:

$$\begin{bmatrix} T_{j1} \\ T_{j2} \end{bmatrix} = \begin{bmatrix} Z_{11} & Z_{12} \\ Z_{21} & Z_{22} \end{bmatrix} \begin{bmatrix} P_1 \\ P_2 \end{bmatrix} + \begin{bmatrix} T_a \\ T_a \end{bmatrix}$$

Self-heating: Z₁₁, Z₂₂
Cross-coupling: Z₁₂ = Z₂₁

## Design Procedure

### Step 1: Loss Budget
- Calculate losses at worst-case operating point
- Include all devices on heatsink

### Step 2: Thermal Budget
- Determine maximum ambient temperature
- Set junction temperature target
- Calculate required Rth,ha

### Step 3: Heatsink Selection
- Select heatsink meeting Rth requirement
- Add margin for:
  - Manufacturing variation (10-20%)
  - TIM degradation over life
  - Altitude derating

### Step 4: Verification
- Simulate thermal transient
- Check at startup (maximum losses)
- Check at overload conditions

## GeckoCIRCUITS Thermal Setup

### Single Device

1. Enable thermal on device
2. Set Rth,jc from datasheet
3. Set Rth,ch (interface)
4. Add thermal node for heatsink
5. Set Rth,ha and Cth,ha

### Multiple Devices

1. Connect devices to common thermal node
2. Set individual Rth,jc
3. Single heatsink Rth,ha
4. Enable thermal coupling if needed

## Simulation Exercises

1. Size heatsink for 100W dissipation
2. Compare natural vs forced convection
3. Analyze thermal coupling effects
4. Design for transient overload

## Related Resources

- [501 - Loss Calculation](loss-calculation.md)
- [502 - Junction Temperature](junction-temperature.md)
