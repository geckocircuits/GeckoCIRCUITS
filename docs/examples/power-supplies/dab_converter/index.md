---
title: DAB - Dual Active Bridge Converter
---

# DAB - Dual Active Bridge Converter

## Overview

The Dual Active Bridge (DAB) is a bidirectional isolated DC-DC converter using two active full-bridges connected through a high-frequency transformer. It's widely used in energy storage systems, EV chargers, and solid-state transformers.

**Difficulty:** Advanced

**Estimated Time:** 45-60 minutes

**Status:** Placeholder

## Learning Objectives

- Understand DAB topology and operating principles
- Implement phase-shift modulation
- Analyze ZVS conditions
- Design for bidirectional power flow

## Topology

```
    Primary Bridge               Secondary Bridge
         ┌───┐         1:n           ┌───┐
  Vdc1 ──┤S1 ├──┬──────●═══●──────┬──┤S5 ├── Vdc2
         └───┘  │      ║   ║      │  └───┘
                │      ║   ║      │
         ┌───┐  │  Lk  ║   ║      │  ┌───┐
         │S2 ├──┴──────╫───╫──────┴──┤S6 │
         └───┘         ║   ║         └───┘
                       ║   ║
         ┌───┐         ║   ║         ┌───┐
         │S3 ├──┬──────╫───╫──────┬──┤S7 │
         └───┘  │      ║   ║      │  └───┘
                │      ║   ║      │
         ┌───┐  │      ●═══●      │  ┌───┐
  GND ───┤S4 ├──┴─────────────────┴──┤S8 ├── GND
         └───┘                       └───┘
```

**Key Components:**
- Two full-bridge converters (H-bridges)
- High-frequency transformer (n:1)
- Series inductance Lk (leakage or external)

## Operating Principle

### Phase-Shift Modulation

Power transfer controlled by phase shift φ between bridges:

```
Primary Bridge:  ┌───┐   ┌───┐   ┌───┐
                 │   │   │   │   │   │
              ───┘   └───┘   └───┘   └───
                 ←──────→
                    φ (phase shift)
Secondary Bridge:    ┌───┐   ┌───┐   ┌───┐
                     │   │   │   │   │   │
                 ────┘   └───┘   └───┘   └
```

### Power Transfer Equation

```
P = (n × Vdc1 × Vdc2 × φ × (π - |φ|)) / (2 × π² × fs × Lk)
```

Where:
- φ = phase shift (radians, -π/2 to π/2)
- fs = switching frequency
- Lk = series inductance
- n = turns ratio

### Voltage Gain

At φ = π/2 (maximum power):
```
Vdc2/Vdc1 = n (voltage matching condition)
```

## Key Parameters

| Parameter | Symbol | Typical Value | Unit |
|-----------|--------|---------------|------|
| DC Voltage 1 | Vdc1 | 400 | V |
| DC Voltage 2 | Vdc2 | 48-400 | V |
| Power | P | 1-50 | kW |
| Switching Frequency | fs | 50-200 | kHz |
| Series Inductance | Lk | 10-100 | μH |
| Turns Ratio | n | 1:1 to 10:1 | - |

## ZVS (Zero Voltage Switching)

### ZVS Condition

For ZVS turn-on, the device must have current flowing through its body diode:

```
ZVS achieved when: I_Lk(t_switch) > I_min

Where: I_min = (2 × Coss × Vdc) / t_dead
```

### ZVS Region

```
Power
  │          ╱╲
  │         ╱  ╲  ZVS region
  │        ╱    ╲
  │       ╱      ╲
  │──────╱────────╲────────
  │     ╱ Hard     ╲
  │    ╱  switching ╲
  └────────────────────────► φ (phase shift)
      -π/2    0    π/2
```

## Modulation Strategies

### Single Phase Shift (SPS)

- Both bridges operate at 50% duty cycle
- Only phase shift varies
- Simple control
- Limited ZVS range

### Extended Phase Shift (EPS)

- Primary bridge: variable duty cycle D1
- Secondary bridge: 50% duty cycle + phase shift
- Extended ZVS range

### Triple Phase Shift (TPS)

- Both bridges: variable duty cycles D1, D2
- Phase shift φ between bridges
- Widest ZVS range
- Most complex control

## Design Procedure

### Step 1: Determine Power and Voltages

Given: Vdc1 = 400V, Vdc2 = 48V, P = 3.3kW

### Step 2: Select Turns Ratio

For voltage matching:
```
n = Vdc1 / Vdc2 = 400/48 ≈ 8:1
```

### Step 3: Calculate Inductance

For desired phase shift at rated power (e.g., φ = 30° = π/6):
```
Lk = (n × Vdc1 × Vdc2 × φ × (π - φ)) / (2 × π² × fs × P)
```

### Step 4: Verify ZVS

Check that ZVS is maintained over operating range.

## Control Architecture

```
                ┌─────────────────────────────────┐
Vdc2_ref ──────►│                                 │
                │   Voltage     Current    Phase  │
                │   Controller → Controller → Shift │──► PWM
Vdc2_meas ─────►│     (PI)        (PI)    Modulator│
                │                                 │
Idc2_meas ─────►│                                 │
                └─────────────────────────────────┘
```

## Bidirectional Operation

| Mode | Phase Shift | Power Flow |
|------|-------------|------------|
| Forward | φ > 0 | Vdc1 → Vdc2 |
| Reverse | φ < 0 | Vdc2 → Vdc1 |
| Zero | φ = 0 | No power transfer |

## Exercises

### Exercise 1: Basic DAB
1. Build DAB with Vdc1 = 400V, Vdc2 = 48V, n = 8:1
2. Apply φ = 30° phase shift
3. Measure power transfer

### Exercise 2: Phase Shift Sweep
1. Sweep φ from -60° to +60°
2. Plot P vs φ
3. Verify theoretical curve

### Exercise 3: ZVS Analysis
1. Add output capacitance to switches
2. Observe switch waveforms
3. Identify ZVS/hard-switching transitions

### Exercise 4: Bidirectional Operation
1. Implement voltage control loop
2. Apply step change in Vdc2_ref
3. Observe automatic direction change

## Applications

| Application | Power | Voltage Range |
|-------------|-------|---------------|
| EV OBC | 3-22 kW | 400V ↔ 48-450V |
| Energy Storage | 10-100 kW | 400V ↔ 200-400V |
| Solid-State Transformer | 1-10 MW | MV ↔ LV |
| DC Microgrid | 5-50 kW | 380V ↔ 48V |

## Related Examples

- [LLC Resonant](../llc_resonant/) - Unidirectional alternative
- [Full Bridge](../../basic_topologies/full_bridge/) - Single active bridge
- [Onboard Charger](../../automotive/onboard_charger/) - Application

## References

1. De Doncker, R.W. "A Three-Phase Soft-Switched High-Power-Density DC/DC Converter"
2. Zhao, B. "Overview of Dual-Active-Bridge Isolated Bidirectional DC-DC Converter"
3. Krismer, F. "Accurate Power Loss Model Derivation of a DAB Converter"

## Circuit Files

> **Status:** Placeholder
> - `dab_basic.ipes` - Basic DAB with SPS
> - `dab_zvs.ipes` - With ZVS analysis
> - `dab_bidirectional.ipes` - Closed-loop bidirectional

---
*Example Version: 1.0 (Placeholder)*
*Last updated: 2026-02*
*GeckoCIRCUITS v1.0*
