---
title: Flyback Converter
description: Isolated DC-DC converter for multi-output power supplies
---

# Tutorial: Flyback Converter

Design and simulate an isolated flyback converter.

**Difficulty:** Intermediate (2/3) | **Duration:** 30 minutes

## Learning Objectives

- [ ] Understand flyback operation and energy transfer
- [ ] Design the coupled inductor (flyback transformer)
- [ ] Handle leakage inductance and voltage spikes
- [ ] Simulate transient and steady-state behavior

## Theory

### Topology

The flyback is an isolated version of the buck-boost converter:

```
              Np : Ns
    Vin ──[FET]──●══════●──|>|──●── Vout
                 ║      ║       │
                 ║  Lm  ║     [C]  [R]
                 ║      ║       │    │
                GND─────●──────●────┘
```

### Operating Principle

**Switch ON:** Energy stores in magnetizing inductance Lm. Secondary diode is reverse-biased.

$$V_{Lm} = V_{in}, \quad \Delta i_{Lm} = \frac{V_{in} \cdot D}{L_m \cdot f_{sw}}$$

**Switch OFF:** Stored energy transfers to secondary through the coupled inductor. Diode conducts.

$$V_{Lm} = -\frac{V_{out}}{n}, \quad n = \frac{N_s}{N_p}$$

### Voltage Conversion

$$V_{out} = n \cdot \frac{D}{1-D} \cdot V_{in}$$

Where n = Ns/Np is the turns ratio.

### Key Design Equations

**Magnetizing inductance:**

$$L_m = \frac{V_{in}^2 \cdot D^2}{2 \cdot P_{out} \cdot f_{sw}}$$

**Turns ratio:**

$$n = \frac{V_{out}}{V_{in}} \cdot \frac{1-D}{D}$$

**Switch voltage stress:**

$$V_{DS,max} = V_{in} + \frac{V_{out}}{n} + V_{spike}$$

## Design Example

### Specifications

| Parameter | Value |
|-----------|-------|
| Input voltage | 48V DC |
| Output voltage | 5V DC |
| Output power | 25W |
| Switching frequency | 100 kHz |
| Max duty cycle | 0.45 |

### Calculations

**Turns ratio:**

$$n = \frac{5}{48} \times \frac{1-0.45}{0.45} = 0.127 \approx 1:8 \text{ (Np:Ns)}$$

**Magnetizing inductance (for CCM):**

$$L_m = \frac{48^2 \times 0.45^2}{2 \times 25 \times 100k} = 93.3\ \mu H$$

**Output capacitor:**

$$C = \frac{I_{out} \cdot D}{f_{sw} \cdot \Delta V_{out}} = \frac{5 \times 0.45}{100k \times 0.05} = 450\ \mu F$$

## Leakage Inductance

Real transformers have imperfect coupling (k < 1). The leakage inductance causes:

- Voltage spikes on the switch at turn-off
- Energy loss if not recovered
- EMI concerns

### Snubber Circuit

An RCD snubber clamps the voltage spike:

$$R_{snub} = \frac{V_{clamp}^2}{P_{leak}}, \quad C_{snub} = \frac{V_{clamp}}{R_{snub} \cdot f_{sw} \cdot \Delta V_{clamp}}$$

## Simulation Notes

### Coupled Inductor Parameters

In GeckoCIRCUITS, set:

| Parameter | Value |
|-----------|-------|
| Primary inductance (Lp) | 93 uH |
| Turns ratio (n) | 1:8 |
| Coupling coefficient (k) | 0.95-0.99 |

### What to Observe

1. **Primary current** - Ramp up during ON, reset during OFF
2. **Secondary current** - Pulse during OFF period
3. **Switch voltage** - V_in + reflected voltage + spike
4. **Output voltage** - Should regulate to 5V

## Exercises

### Exercise 1: CCM vs DCM
Reduce load until the magnetizing current reaches zero. Compare waveforms.

### Exercise 2: Coupling Coefficient
Vary k from 0.90 to 0.99. Observe the voltage spike on the primary switch.

### Exercise 3: Snubber Design
Add an RCD snubber. Size it to clamp the spike below 120V.

## Next Steps

- [Forward Converter](forward.md) - Another isolated topology
- [Thermal Simulation](../../tutorials/thermal/loss-calculation.md) - Add switch and diode losses
- [Magnetics](../../tutorials/magnetics/transformer-design.md) - Detailed transformer design
