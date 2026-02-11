---
title: "904 - Mechanical Systems"
---

# 904 - Mechanical Systems

Motor-load dynamics and electromechanical simulation.

## Overview

Power electronics often drive mechanical loads through electric machines. GeckoCIRCUITS supports:
- Rotating machine models (PMSM, BLDC, IM)
- Mechanical load models
- Multi-mass systems
- Gear boxes and couplings

## Mechanical-Electrical Analogy

### Force/Torque Analogy

| Mechanical (Rotational) | Electrical | Unit |
|------------------------|------------|------|
| Torque τ | Voltage V | Nm / V |
| Angular velocity ω | Current I | rad/s / A |
| Inertia J | Capacitance C | kg·m² / F |
| Friction b | Conductance G | Nm·s / S |
| Stiffness k | 1/L | Nm/rad / 1/H |

## Basic Mechanical Models

### Single Inertia

$$J \frac{d\omega}{dt} = \tau_{motor} - \tau_{load} - b\omega$$

In GeckoCIRCUITS: Capacitor with value J

### Two-Mass System

Models shaft compliance:
```
    Motor        Shaft         Load
   ┌─────┐    ┌────────┐    ┌─────┐
   │  J1 │────│ k, b   │────│  J2 │
   └─────┘    └────────┘    └─────┘
```

Equations:
$$J_1 \frac{d\omega_1}{dt} = \tau_m - \tau_s$$
$$J_2 \frac{d\omega_2}{dt} = \tau_s - \tau_L$$
$$\tau_s = k(\theta_1 - \theta_2) + b(\omega_1 - \omega_2)$$

## Load Types

### Constant Torque

$$\tau_L = \tau_0$$

Applications: Conveyors, hoists, cranes

### Quadratic (Fan/Pump)

$$\tau_L = k \cdot \omega^2$$

Applications: Fans, pumps, blowers

### Linear (Friction)

$$\tau_L = b \cdot \omega$$

Viscous friction model

### Constant Power

$$\tau_L = \frac{P_0}{\omega}$$

Applications: Winders, machine tools

## Motor Models

### PMSM (Permanent Magnet Synchronous Motor)

**Electrical equations (dq frame):**
$$v_d = R_s i_d + L_d \frac{di_d}{dt} - \omega_e L_q i_q$$
$$v_q = R_s i_q + L_q \frac{di_q}{dt} + \omega_e L_d i_d + \omega_e \psi_m$$

**Torque:**
$$\tau_e = \frac{3}{2} p [\psi_m i_q + (L_d - L_q)i_d i_q]$$

### BLDC (Brushless DC)

Trapezoidal back-EMF model:
$$e_{ph} = k_e \cdot \omega \cdot f(\theta_e)$$

Where f(θe) is trapezoidal function of electrical angle.

### Induction Motor

**Rotor flux model:**
$$\frac{d\psi_r}{dt} = \frac{L_m}{\tau_r}i_s - \frac{1}{\tau_r}\psi_r + j\omega_{slip}\psi_r$$

**Torque:**
$$\tau_e = \frac{3}{2}p\frac{L_m}{L_r}Im\{\psi_r^* i_s\}$$

## Gearbox Modeling

### Ideal Gear Ratio

$$\omega_2 = \frac{\omega_1}{n}$$
$$\tau_2 = n \cdot \tau_1$$

### Reflected Inertia

$$J_{eq} = J_1 + \frac{J_2}{n^2}$$

Inertia seen from motor side

### Gear Efficiency

$$\tau_2 = \eta \cdot n \cdot \tau_1$$ (motoring)
$$\tau_2 = \frac{n \cdot \tau_1}{\eta}$$ (regenerating)

## GeckoCIRCUITS Setup

### Adding Mechanical Domain

1. **Insert motor model** from component library
2. **Set parameters:**
   - Electrical: Rs, Ld, Lq, ψm
   - Mechanical: J (motor inertia), b (friction)
   - Pole pairs p
3. **Add load model:**
   - Connect mechanical port to load
   - Set load type and parameters

### Multi-Mass Systems

1. **Use mechanical nodes** (angular velocity)
2. **Connect:**
   - Inertia elements (capacitors)
   - Shaft compliance (spring-damper)
   - Gearbox (transformer)

## Simulation Exercises

1. Startup transient of PMSM with fan load
2. Compare single vs two-mass system response
3. Analyze gear ratio selection
4. Simulate regenerative braking

## Design Considerations

### Inertia Ratio

$$\sigma = \frac{J_{load}}{J_{motor}}$$

| Ratio | Application | Response |
|-------|-------------|----------|
| σ < 3 | Servo | Fast |
| 3 < σ < 10 | Industrial | Moderate |
| σ > 10 | High inertia | Slow, oscillation risk |

### Resonance Frequency

For two-mass system:
$$f_{res} = \frac{1}{2\pi}\sqrt{\frac{k(J_1 + J_2)}{J_1 J_2}}$$

Control bandwidth should be below resonance.

## Related Resources

- [PMSM FOC Example](../../examples/motor-drives/pmsm-foc.md)
- [BLDC Control Example](../../examples/motor-drives/bldc.md)
