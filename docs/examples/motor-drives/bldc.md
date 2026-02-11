---
title: BLDC Motor Control Example
---

# BLDC Motor Control Example

Brushless DC motor with six-step commutation.

## Overview

BLDC motors feature:
- Trapezoidal back-EMF
- Six-step commutation
- Hall sensor or sensorless feedback
- High torque density

## Specifications

| Parameter | Value |
|-----------|-------|
| DC Bus Voltage | 48V |
| Rated Power | 500W |
| Rated Speed | 3000 RPM |
| Pole Pairs | 4 |
| Kt (Torque Constant) | 0.1 Nm/A |
| Ke (Back-EMF Constant) | 0.1 V/(rad/s) |

## Circuit Files

- `bldc_six_step.ipes` - Basic six-step commutation
- `bldc_hall_sensors.ipes` - Hall sensor feedback
- `bldc_sensorless.ipes` - Back-EMF zero crossing detection

## Theory

### Six-Step Commutation

| Hall | H1 | H2 | H3 | Active Phases |
|------|----|----|-----|---------------|
| 1 | 1 | 0 | 1 | A+, B- |
| 2 | 1 | 0 | 0 | A+, C- |
| 3 | 1 | 1 | 0 | B+, C- |
| 4 | 0 | 1 | 0 | B+, A- |
| 5 | 0 | 1 | 1 | C+, A- |
| 6 | 0 | 0 | 1 | C+, B- |

### Back-EMF Equation

$$e_{ph} = K_e \cdot \omega \cdot f(\theta_e)$$

Where f(θe) is trapezoidal function.

### Torque Production

$$\tau_e = K_t \cdot i_{ph}$$

During 120° electrical conduction period.

## Control Structure

```
Speed Ref ──► Speed PI ──► Current Limit ──► PWM ──► Inverter
    ▲                                                    │
    │                                                    ▼
Speed fb ◄────────────── Hall Decode ◄────────────── Motor
```

### Current Control

- Hysteresis band control (simple)
- PI current loop (smoother)
- PWM on high-side or low-side

## Sensorless Operation

### Back-EMF Zero Crossing

Detect zero crossing on floating phase:
- Wait for commutation blanking
- Filter and compare to neutral
- Time delay = 30° electrical

### Starting

Open-loop ramp-up required:
1. Apply initial current
2. Step through commutation sequence
3. Gradually increase frequency
4. Switch to closed-loop when reliable detection

## Exercises

1. **Commutation Sequence:** Verify phase current waveforms
2. **Speed Control:** Implement PI speed loop
3. **Load Step:** Apply torque step, observe response
4. **Sensorless Transition:** Start to closed-loop handoff

## Related Resources

- [PMSM FOC](pmsm-foc.md)
- [402 - Three-Phase Inverter](../../tutorials/dcac/three-phase.md)
