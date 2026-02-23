# 503 - Heatsink Design

## Overview

This tutorial covers the design and simulation of heatsinks for power electronic converters. You'll learn to model thermal impedance networks, calculate required thermal resistance, and verify junction temperatures under various operating conditions.

**Difficulty:** 3/3 (Advanced)

**Estimated Time:** 45-60 minutes

## Learning Objectives

After completing this tutorial, you will be able to:
- Calculate required heatsink thermal resistance from power loss data
- Model Foster and Cauer thermal networks in GeckoCIRCUITS
- Simulate transient thermal behavior during load steps
- Design cooling systems for target junction temperature

## Prerequisites

- Complete [Tutorial 501: Loss Calculation](../501_loss_calculation/)
- Complete [Tutorial 502: Junction Temperature](../502_junction_temperature/)
- Understanding of conduction and switching losses
- Basic heat transfer concepts (thermal resistance, capacitance)

## Theory

### Thermal Resistance Network

Heat flows from the junction (Tj) through the case (Tc) and heatsink (Ts) to ambient (Ta):

```
    Tj ──[Rth,jc]── Tc ──[Rth,ch]── Ts ──[Rth,ha]── Ta
         │                │                │
        Cth,j           Cth,c            Cth,h
         │                │                │
        GND              GND              GND
```

Where:
- **Rth,jc** = Junction-to-case thermal resistance (from datasheet)
- **Rth,ch** = Case-to-heatsink thermal resistance (thermal interface material)
- **Rth,ha** = Heatsink-to-ambient thermal resistance (heatsink property)
- **Cth** = Thermal capacitances (for transient analysis)

### Steady-State Analysis

At thermal equilibrium:

```
Tj = Ta + Ploss × (Rth,jc + Rth,ch + Rth,ha)
```

Solving for heatsink requirement:

```
Rth,ha = (Tj,max - Ta) / Ploss - Rth,jc - Rth,ch
```

### Key Parameters

| Parameter | Symbol | Typical Range | Unit | Notes |
|-----------|--------|---------------|------|-------|
| Junction-to-Case | Rth,jc | 0.1 - 1.0 | K/W | Datasheet value |
| Case-to-Heatsink | Rth,ch | 0.05 - 0.5 | K/W | Depends on TIM |
| Max Junction Temp | Tj,max | 125 - 175 | °C | Device limit |
| Ambient Temp | Ta | 25 - 55 | °C | Operating environment |
| Power Dissipation | Ploss | varies | W | From loss calculation |

### Foster vs Cauer Networks

**Foster Network (More Common):**
- RC elements in series from junction to ambient
- Directly represents datasheet Zth curve
- NOT physically meaningful at internal nodes

**Cauer Network (Physically Accurate):**
- Each node represents actual physical layer
- Allows coupling multiple devices to one heatsink
- More complex to derive from Zth curves

```
Foster (Series-RC):
Tj ──[R1]──┬──[R2]──┬──[R3]──┬──[R4]── Ta
           │        │        │
          C1       C2       C3
           │        │        │
          GND      GND      GND

Cauer (Ladder):
Tj ──┬──[R1]──┬──[R2]──┬──[R3]── Ta
     │        │        │
    C1       C2       C3
     │        │        │
    GND      GND      GND
```

## Thermal Network in GeckoCIRCUITS

### Using Thermal Components

GeckoCIRCUITS provides thermal analogs:
- **Thermal Resistance:** Use resistor element in thermal domain
- **Thermal Capacitance:** Use capacitor element in thermal domain
- **Heat Source:** Current source (power loss → heat current)
- **Temperature Source:** Voltage source (ambient temperature)

### Example: IGBT Module Thermal Model

For an IGBT module with Rth,jc = 0.2 K/W (4-layer Foster):

| Layer | Ri (K/W) | τi (ms) | Ci = τi/Ri (J/K) |
|-------|----------|---------|------------------|
| 1 | 0.05 | 1 | 0.02 |
| 2 | 0.08 | 10 | 0.125 |
| 3 | 0.04 | 100 | 2.5 |
| 4 | 0.03 | 1000 | 33.3 |

## Design Procedure

### Step 1: Determine Power Losses

From loss calculation (Tutorial 501):
1. Measure/calculate conduction losses: Pcond = I²·Ron + Vf·I
2. Calculate switching losses: Psw = (Eon + Eoff)·fsw
3. Total loss per device: Ploss = Pcond + Psw

**Example:**
- IGBT: Pcond = 50W, Psw = 30W → Total = 80W
- Diode: Pcond = 20W, Prr = 10W → Total = 30W
- Module total: 110W per switch position

### Step 2: Establish Thermal Budget

Given:
- Tj,max = 150°C (datasheet limit, use 125°C for margin)
- Ta = 40°C (worst-case ambient)
- Available temperature rise: ΔT = 125 - 40 = 85°C

### Step 3: Calculate Heatsink Requirement

For a three-phase inverter (6 IGBT+Diode pairs on one heatsink):
- Total power: Ptotal = 6 × 110W = 660W
- Assuming parallel thermal paths:

```
Rth,total = ΔT / Ptotal = 85 / 660 = 0.129 K/W
```

Per device path:
```
Rth,jc + Rth,ch = 0.2 + 0.1 = 0.3 K/W (from datasheet + TIM)
```

For parallel devices, effective heatsink resistance:
```
Rth,ha = (6 × (ΔT/Ploss,device - Rth,jc - Rth,ch))⁻¹
       ≈ (6 × (85/110 - 0.3))⁻¹
       ≈ 0.35 K/W (target heatsink specification)
```

### Step 4: Select Heatsink

Choose a heatsink with:
- Rth,ha ≤ 0.35 K/W (with forced air)
- Consider: profile dimensions, mounting, airflow

Typical heatsink specifications:
| Type | Rth,ha (K/W) | Airflow |
|------|--------------|---------|
| Small extruded | 2-5 | Natural |
| Medium finned | 0.5-2 | 1-2 m/s |
| Large with fan | 0.1-0.5 | 3-5 m/s |
| Liquid cooled | 0.02-0.1 | N/A |

## Building the Model in GeckoCIRCUITS

### Step 1: Create Thermal Domain

1. Open the thermal domain (use THERM elements)
2. Add a voltage source for ambient temperature (Ta = 40°C = 313K)
3. This represents the thermal ground reference

### Step 2: Add Device Thermal Network

1. Add resistors for each Foster/Cauer layer
2. Add capacitors for transient response
3. Connect in series from junction to heatsink node

### Step 3: Add Heatsink Model

1. Add Rth,ch (case-to-heatsink) resistor
2. Add Rth,ha (heatsink-to-ambient) resistor
3. Optional: Add Cth,h for heatsink thermal mass

### Step 4: Connect to Power Circuit

1. Enable loss calculation in semiconductor components
2. Route power loss output to thermal current source
3. Monitor junction temperature from thermal node voltage

### Step 5: Simulate

1. Run steady-state simulation to verify Tj < Tj,max
2. Apply load steps to observe transient thermal response
3. Measure thermal time constant (τ ≈ 63% of final ΔT)

## Expected Results

### Steady-State

For the example above (660W total loss, 40°C ambient):

| Node | Expected Temperature |
|------|---------------------|
| Ta (Ambient) | 40°C |
| Ts (Heatsink) | 40 + 660×0.35 = 271°C |

**Wait!** This exceeds limits! Let's recalculate properly:

With proper parallel thermal modeling:
- Ts = 40 + 660×0.1 = 106°C (with better heatsink)
- Tj = 106 + 110×0.3 = 139°C (per device)

This is within the 150°C limit but has low margin.

### Transient Response

For a load step (0 to 100%):
- Fast response (ms): Junction heats up through Rth,jc
- Medium response (100ms-1s): Case temperature rises
- Slow response (1-10s): Heatsink reaches equilibrium

## Exercises

### Exercise 1: Steady-State Design

1. Use the `BuckBoost_thermal.ipes` circuit from Tutorial 501
2. Add a heatsink thermal resistance (Rth,ha = 2 K/W)
3. Add ambient temperature source (Ta = 40°C)
4. Verify Tj stays below 125°C at full load
5. **Question:** What Rth,ha is needed for Tj < 100°C?

### Exercise 2: Transient Analysis

1. Start with the circuit from Exercise 1
2. Add thermal capacitances (Foster model, 4 layers)
3. Apply a 0-100% load step at t = 1s
4. Measure: time to reach 90% of final Tj
5. **Question:** How does Cth,h affect settling time?

### Exercise 3: Multi-Device Heatsink

1. Model a three-phase inverter (6 devices)
2. All devices share one heatsink
3. Apply unbalanced power (phase A: 150W, B: 100W, C: 50W)
4. Observe individual junction temperatures
5. **Question:** Which device is hottest? Why?

### Exercise 4: Cooling System Comparison

1. Design for natural convection (Rth,ha = 3 K/W)
2. Design for forced air (Rth,ha = 0.5 K/W)
3. Compare: maximum power rating, heatsink size
4. **Challenge:** At what power level does forced air become necessary?

## Component Sizing Tables

### Thermal Interface Materials (TIM)

| Material | Rth (K·cm²/W) | Thickness | Rth,ch (K/W) for 10cm² |
|----------|---------------|-----------|------------------------|
| Thermal grease | 0.5-2 | 50-100 μm | 0.05-0.2 |
| Thermal pad | 2-5 | 0.5-2 mm | 0.2-0.5 |
| Phase-change | 0.3-1 | 25-50 μm | 0.03-0.1 |

### Typical Heatsink Thermal Resistance

| Size (mm) | Natural (K/W) | 2m/s Air (K/W) |
|-----------|---------------|----------------|
| 50×50×20 | 8-12 | 3-5 |
| 100×100×40 | 2-4 | 0.8-1.5 |
| 150×150×60 | 1-2 | 0.3-0.6 |
| 200×200×80 | 0.5-1 | 0.15-0.3 |

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Tj exceeds limit | Insufficient cooling | Reduce Rth,ha or increase heatsink |
| Slow thermal response | Large Cth values | Use finer time step for accuracy |
| Oscillating temperature | Numerical instability | Increase simulation time step |
| Uneven device temps | Thermal coupling ignored | Use Cauer network with coupling |

## Related Examples

- [Loss Calculation](../501_loss_calculation/) - Determine power losses
- [Junction Temperature](../502_junction_temperature/) - Basic thermal modeling
- [Three-Phase VSR Thermal](../502_junction_temperature/ThreePhase-VSR_10kW_thermal.ipes) - Complete example

## References

1. Infineon Application Note AN2015-10: "Thermal Design and Simulation of Power Electronics"
2. SEMIKRON Application Manual: "Power Semiconductor Handbook"
3. Drofenik, U., Kolar, J.W. "A General Scheme for Calculating Switching- and Conduction-Losses of Power Semiconductors"

## Circuit Files

> **Note:** Example circuits for heatsink design will be added.
> To contribute, save circuits as:
> - `heatsink_single_device.ipes` - Single device with heatsink
> - `heatsink_multichip.ipes` - Multiple devices on shared heatsink
> - `heatsink_transient.ipes` - Transient thermal analysis

---
*Tutorial Version: 1.0*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
