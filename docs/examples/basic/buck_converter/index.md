---
title: Buck Converter Example
---

# Buck Converter Example

## Overview

The buck converter is the fundamental step-down DC-DC topology. This example demonstrates basic operation with open-loop PWM control.

**Difficulty:** Beginner

**Estimated Time:** 15-20 minutes

## Learning Objectives

- Understand buck converter operation
- Observe CCM waveforms
- Verify Vout = D × Vin relationship

## Circuit Description

```
    +Vin ──┬──[S]──┬──[L]──┬── +Vout
           │       │       │
           │      [D]     [C]     [R]
           │       │       │       │
    GND ───┴───────┴───────┴───────┴── GND
```

## Parameters

| Parameter | Value | Unit |
|-----------|-------|------|
| Vin | 48 | V |
| Vout | 12 | V |
| D (duty cycle) | 0.25 | - |
| fs | 100 | kHz |
| L | 100 | μH |
| C | 100 | μF |
| R (load) | 4.8 | Ω |

## Expected Results

| Signal | Value |
|--------|-------|
| Vout (avg) | 12 V |
| IL (avg) | 2.5 A |
| ΔIL | 0.9 A p-p |
| ΔVout | 22 mV p-p |

## Exercises

1. Vary D from 0.1 to 0.5, verify Vout = D × Vin
2. Change load to light (48Ω), observe DCM
3. Reduce L to 10μH, observe increased ripple

## Related

- [Tutorial 201: Buck Converter](../../tutorials/2xx_dcdc_converters/201_buck_converter/)
- [Boost Converter](../boost_converter/)
