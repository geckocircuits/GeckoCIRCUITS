---
title: PMSM Field-Oriented Control Example
---

# PMSM Field-Oriented Control Example

Permanent magnet synchronous motor with vector control.

## Overview

Field-Oriented Control (FOC) provides:
- Torque and flux decoupling
- Maximum torque per ampere
- Smooth low-speed operation
- Four-quadrant operation

## Specifications

| Parameter | Value |
|-----------|-------|
| Rated Power | 2.2 kW |
| Rated Speed | 3000 RPM |
| Pole Pairs | 4 |
| Rs (Stator Resistance) | 1.5 Ω |
| Ld (d-axis Inductance) | 8 mH |
| Lq (q-axis Inductance) | 12 mH |
| ψm (PM Flux Linkage) | 0.175 Wb |

## Circuit Files

- `pmsm_foc_basic.ipes` - Basic FOC implementation
- `pmsm_foc_mtpa.ipes` - Maximum torque per ampere
- `pmsm_sensorless.ipes` - Sensorless FOC

## Theory

### dq Reference Frame

Transform abc quantities to rotating dq frame:

**Clarke Transform (abc → αβ):**
$$\begin{bmatrix} i_\alpha \\ i_\beta \end{bmatrix} = \frac{2}{3}\begin{bmatrix} 1 & -1/2 & -1/2 \\ 0 & \sqrt{3}/2 & -\sqrt{3}/2 \end{bmatrix}\begin{bmatrix} i_a \\ i_b \\ i_c \end{bmatrix}$$

**Park Transform (αβ → dq):**
$$\begin{bmatrix} i_d \\ i_q \end{bmatrix} = \begin{bmatrix} \cos\theta & \sin\theta \\ -\sin\theta & \cos\theta \end{bmatrix}\begin{bmatrix} i_\alpha \\ i_\beta \end{bmatrix}$$

### Motor Equations (dq Frame)

$$v_d = R_s i_d + L_d \frac{di_d}{dt} - \omega_e L_q i_q$$
$$v_q = R_s i_q + L_q \frac{di_q}{dt} + \omega_e L_d i_d + \omega_e \psi_m$$

### Torque Equation

$$\tau_e = \frac{3}{2}p[\psi_m i_q + (L_d - L_q)i_d i_q]$$

For surface PM (Ld = Lq): τe = (3/2)p·ψm·iq

## Control Structure

```
                    ┌──────────────┐
Speed Ref ─────────►│   Speed PI   │
                    │   Controller │
Speed fb ──────────►│              │
                    └──────┬───────┘
                           │ iq*
              id*=0        ▼
                ├─────────────────────┐
                ▼                     ▼
         ┌──────────┐          ┌──────────┐
         │ id PI    │          │ iq PI    │
         │ Control  │          │ Control  │
         └────┬─────┘          └────┬─────┘
              │ vd*                 │ vq*
              └──────────┬─────────┘
                         ▼
                   ┌──────────┐
                   │ Inverse  │
                   │ Park/SVM │
                   └────┬─────┘
                        ▼
                   ┌──────────┐
                   │ Inverter │
                   └────┬─────┘
                        ▼
                   ┌──────────┐
                   │  PMSM    │
                   └──────────┘
```

## Current Controller Tuning

### PI Gains

**d-axis:**
$$K_{p,d} = L_d \cdot \omega_c, \quad K_{i,d} = R_s \cdot \omega_c$$

**q-axis:**
$$K_{p,q} = L_q \cdot \omega_c, \quad K_{i,q} = R_s \cdot \omega_c$$

Where ωc is desired current loop bandwidth.

### Decoupling

Add feedforward terms:
- ωe·Lq·iq to d-axis
- ωe·Ld·id + ωe·ψm to q-axis

## Exercises

1. **Basic FOC:** Implement current loops, verify iq tracking
2. **Speed Loop:** Add speed controller, tune response
3. **MTPA:** Implement id* curve for IPM motor
4. **Field Weakening:** Extend speed range above base speed

## Related Resources

- [BLDC Control](bldc.md)
- [904 - Mechanical Systems](../../tutorials/magnetics/mechanical-systems.md)
