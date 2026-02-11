---
title: Tutorial 903: Inductor Saturation
---

# Tutorial 903: Inductor Saturation

## Overview

Model and simulate inductor saturation effects in power converters. Understand how saturation impacts converter operation and learn design techniques to avoid or utilize saturation.

**Level:** Advanced (3/3)

**Duration:** 30-45 minutes

**Series:** Magnetics & Mechanical

**Status:** Placeholder - Circuit files to be added

## Learning Objectives

- Model non-linear inductance L(i)
- Simulate saturation effects on converter waveforms
- Design inductors with appropriate saturation margins
- Understand saturable reactor applications

## Saturation Theory

### B-H Curve

```
    B (Tesla)
        │      ╱────── Bsat
        │     ╱
        │    ╱  Linear region
        │   ╱   (μ = constant)
        │  ╱
    ────┼─╱──────────── H (A/m)
        │╱
        │    Saturation region
              (μ → μ0)
```

### Inductance vs Current

```
    L
    │ L0 ────┐
    │        │
    │        └───────┐
    │                └────── L_sat ≈ L0 × (μ0/μr)
    └──────────────────────── I
              Isat
```

## Non-Linear Inductor Model

### Arctangent Model
```
L(i) = L0 × [1 - tanh²(i/Isat)]
```

### Piecewise Linear
```
L = L0           for |i| < Isat
L = L0/100       for |i| ≥ Isat
```

## Impact on Converters

| Effect | Consequence | Mitigation |
|--------|-------------|------------|
| Current spike | Device stress | Overcurrent protection |
| Increased ripple | Higher losses | Design margin |
| Waveform distortion | EMI, control issues | Air gap |

## Exercises

### Exercise 1: Linear vs Saturating
1. Compare buck with linear L vs saturating L
2. Apply 120% rated load
3. Observe current waveform differences

### Exercise 2: Saturable Reactor
1. Model magnetically controlled inductor
2. Vary DC bias current
3. Observe variable AC impedance

## Circuit Files

> **Status:** Placeholder
> - `inductor_linear.ipes`
> - `inductor_saturating.ipes`
> - `saturable_reactor.ipes`

---
*Tutorial Version: 1.0 (Placeholder)*
