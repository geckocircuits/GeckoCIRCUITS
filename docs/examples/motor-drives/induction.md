---
title: Induction Motor Control Example
---

# Induction Motor Control Example

Three-phase induction motor with V/f and vector control.

## Overview

Induction motors are the workhorse of industry:
- Robust and reliable
- Low cost
- Self-starting
- Various control methods available

## Specifications

| Parameter | Value |
|-----------|-------|
| Rated Power | 5.5 kW |
| Rated Voltage | 400V |
| Rated Frequency | 50 Hz |
| Pole Pairs | 2 |
| Rs | 0.5 Ω |
| Rr | 0.4 Ω |
| Ls = Lr | 80 mH |
| Lm | 75 mH |

## Circuit Files

- `im_vf_control.ipes` - V/f (scalar) control
- `im_vector.ipes` - Indirect field-oriented control
- `im_dtc.ipes` - Direct torque control

## Theory

### V/f Control

Maintain constant flux by keeping V/f ratio:
$$\frac{V}{f} = \text{constant}$$

With voltage boost at low frequency for Rs drop.

### Slip Frequency

$$f_{slip} = f_s - f_r = \frac{n_s - n}{n_s} \cdot f_s$$

$$\omega_{slip} = \frac{R_r}{L_r} \cdot \frac{i_q}{i_d}$$

### Torque Equation

$$\tau_e = \frac{3}{2}p\frac{L_m}{L_r}\psi_r i_q$$

## Control Methods

### Scalar (V/f)

```
Frequency Ref ──► V/f Curve ──► PWM ──► Inverter ──► Motor
```

- Simple implementation
- Open-loop (no encoder)
- Limited dynamics

### Indirect Field-Oriented Control (IFOC)

```
Speed Ref ──► Speed PI ──► iq* ───┬──► Current PI ──► PWM
                                  │
         ψr Ref ──► Flux PI ──► id* ──►
                                  │
         ωslip = f(iq*/id*) ──────┘
```

- High dynamic performance
- Requires motor parameters
- Rotor position from encoder + slip calculation

### Direct Torque Control (DTC)

- Direct control of torque and flux
- Hysteresis controllers
- Variable switching frequency
- Very fast torque response

## Design Considerations

### Field Weakening

Above base speed, reduce flux to maintain voltage limit:
$$\psi_r = \psi_{rated} \cdot \frac{f_{base}}{f_s}$$

### Motor Starting

- V/f: Current limited by inverter rating
- Vector: Controlled current during acceleration

## Exercises

1. **V/f Control:** Implement open-loop V/f drive
2. **Slip Calculation:** Measure slip at different loads
3. **IFOC:** Implement indirect vector control
4. **Speed Reversal:** Four-quadrant operation

## Related Resources

- [PMSM FOC](pmsm-foc.md)
- [402 - Three-Phase Inverter](../../tutorials/dcac/three-phase.md)
