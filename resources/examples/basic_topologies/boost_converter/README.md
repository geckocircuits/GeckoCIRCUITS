# Boost Converter Example

## Overview

The boost converter is the fundamental step-up DC-DC topology, producing output voltage higher than input.

**Difficulty:** Beginner

**Estimated Time:** 15-20 minutes

## Learning Objectives

- Understand boost converter operation
- Observe energy storage in inductor
- Verify Vout = Vin/(1-D) relationship

## Circuit Description

```
    +Vin ──[L]──┬──[D]──┬── +Vout
                │       │
               [S]     [C]     [R]
                │       │       │
    GND ────────┴───────┴───────┴── GND
```

## Parameters

| Parameter | Value | Unit |
|-----------|-------|------|
| Vin | 12 | V |
| Vout | 24 | V |
| D (duty cycle) | 0.5 | - |
| fs | 100 | kHz |
| L | 50 | μH |
| C | 100 | μF |
| R (load) | 24 | Ω |

## Expected Results

| Signal | Value |
|--------|-------|
| Vout (avg) | 24 V |
| IL (avg) | 2 A |
| Iout (avg) | 1 A |
| ΔVout | ~0.5 V p-p |

## Key Insight

Input current (IL) = Output current / (1-D)

At D=0.5: IL = Iout / 0.5 = 2 × Iout

## Exercises

1. Vary D from 0.3 to 0.7, verify Vout = Vin/(1-D)
2. Observe efficiency drop at high D (>0.8)
3. Compare input vs output current

## Related

- [Tutorial 202: Boost Converter](../../../tutorials/2xx_dcdc_converters/202_boost_converter/)
- [Buck Converter](../buck_converter/)
