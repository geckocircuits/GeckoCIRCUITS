---
title: Tutorial 904: Mechanical Systems
---

# Tutorial 904: Mechanical Systems

## Overview

Model mechanical systems and their coupling with electrical domains. Learn to simulate motor loads, gearboxes, and electromechanical dynamics.

**Level:** Advanced (3/3)

**Duration:** 45-60 minutes

**Series:** Magnetics & Mechanical

**Status:** Placeholder - Circuit files to be added

## Learning Objectives

- Understand mechanical-electrical analogies
- Model rotational inertia and friction
- Simulate motor-load dynamics
- Design for mechanical resonance avoidance

## Mechanical-Electrical Analogy

| Mechanical (Rotational) | Symbol | Electrical | Symbol |
|-------------------------|--------|------------|--------|
| Torque | τ | Current | I |
| Angular velocity | ω | Voltage | V |
| Inertia | J | Capacitance | C |
| Friction (damping) | B | Conductance | G |
| Stiffness (spring) | K | Inductance⁻¹ | 1/L |

## Mechanical Model

```
    Motor Torque (τm)
         │
         ▼
    ┌────┴────┐
    │    J    │  Inertia (motor + load)
    │  (rotor)│
    └────┬────┘
         │
    ┌────┴────┐
    │    B    │  Friction/Damping
    │         │
    └────┬────┘
         │
    ┌────┴────┐
    │    K    │  Shaft stiffness (if flexible)
    │ (shaft) │
    └────┬────┘
         │
         ▼
    Load Torque (τL)
```

## Equations of Motion

### Simple Inertial Load
```
J × dω/dt = τm - τL - B×ω
```

### Two-Mass System (Flexible Shaft)
```
J1 × dω1/dt = τm - K×(θ1-θ2) - B1×ω1
J2 × dω2/dt = K×(θ1-θ2) - τL - B2×ω2
```

## Gearbox Modeling

### Ideal Gear
```
ω2 = ω1 / n  (speed reduction)
τ2 = τ1 × n  (torque multiplication)
```

### With Efficiency
```
τ2 = τ1 × n × η
```

### Reflected Inertia
```
J_reflected = J_load / n²
```

## Common Mechanical Loads

| Load Type | Torque Characteristic |
|-----------|----------------------|
| Constant torque | τL = constant (conveyors, hoists) |
| Linear (friction) | τL = B × ω (viscous loads) |
| Quadratic (fan) | τL = k × ω² (fans, pumps) |
| Constant power | τL = P / ω (machine tools) |

## Exercises

### Exercise 1: Motor Startup
1. Model PMSM with inertial load
2. Simulate acceleration from standstill
3. Calculate acceleration time: t = J×Δω / (τm - τL)

### Exercise 2: Gearbox
1. Add 10:1 gearbox between motor and load
2. Compare motor torque with direct drive
3. Observe speed reduction

### Exercise 3: Resonance
1. Model two-mass system with flexible shaft
2. Find mechanical resonance frequency: fr = √(K/J)/(2π)
3. Avoid exciting resonance with control

## Circuit Files

> **Status:** Placeholder
> - `motor_inertial_load.ipes`
> - `two_mass_system.ipes`
> - `gearbox_model.ipes`

---
*Tutorial Version: 1.0 (Placeholder)*
