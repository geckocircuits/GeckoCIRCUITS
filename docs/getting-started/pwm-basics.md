---
title: PWM Basics
description: Understanding pulse-width modulation in power electronics
---

# PWM Basics

Pulse-Width Modulation (PWM) is the fundamental control method for power electronics. This tutorial explains how PWM works and how to use it in GeckoCIRCUITS.

**Duration:** 15 minutes

**Prerequisites:** [Building Circuits](building-circuits.md)

## What is PWM?

PWM converts a DC input voltage to a controllable output by rapidly switching a transistor on and off.

```
     ON        OFF       ON        OFF
    ┌───┐            ┌───┐
    │   │            │   │
────┘   └────────────┘   └────────────
    |←D·T→|←(1-D)·T→|

T = switching period = 1/f
D = duty cycle (0 to 1)
```

### Key Parameters

| Parameter | Symbol | Unit | Description |
|-----------|--------|------|-------------|
| Switching frequency | f_sw | Hz | How fast the switch toggles |
| Switching period | T = 1/f_sw | s | Duration of one cycle |
| Duty cycle | D | - | Fraction of period the switch is ON (0-1) |
| ON time | t_on = D x T | s | Time switch conducts |
| OFF time | t_off = (1-D) x T | s | Time switch is off |

### Why PWM?

The average output voltage is proportional to the duty cycle:

$$V_{out,avg} = D \times V_{in}$$

By varying D from 0 to 1, you can control the output from 0V to V_in.

## PWM in GeckoCIRCUITS

### The PWM Block

GeckoCIRCUITS provides a built-in PWM control block:

**Inputs:**
- Carrier frequency (f_sw)
- Duty cycle (D)

**Output:**
- Digital signal (0 or 1) that drives the switch gate

### Setting Up PWM

1. Place a **PWM** block from the control components
2. Double-click to set parameters:
   - **Frequency:** e.g., 100 kHz
   - **Duty Cycle:** e.g., 0.5 (50%)
3. Connect the output to the switch **gate** terminal

### PWM Generation Methods

| Method | Description | Use Case |
|--------|-------------|----------|
| Fixed duty cycle | Constant D value | Open-loop testing |
| Comparator-based | Compare reference to carrier | Closed-loop control |
| Signal-based | External signal sets duty cycle | MATLAB/Python control |

## Duty Cycle and Output Voltage

### Buck Converter

$$V_{out} = D \times V_{in}$$

| D | V_in = 48V | V_out |
|---|-----------|-------|
| 0.25 | 48V | 12V |
| 0.50 | 48V | 24V |
| 0.75 | 48V | 36V |

### Boost Converter

$$V_{out} = \frac{V_{in}}{1 - D}$$

| D | V_in = 12V | V_out |
|---|-----------|-------|
| 0.25 | 12V | 16V |
| 0.50 | 12V | 24V |
| 0.75 | 12V | 48V |

## Frequency Selection

Choosing the right switching frequency involves trade-offs:

| Higher Frequency | Lower Frequency |
|-----------------|-----------------|
| Smaller L and C needed | Larger L and C needed |
| Higher switching losses | Lower switching losses |
| Faster transient response | Slower transient response |
| More EMI | Less EMI |

### Typical Frequencies by Application

| Application | Typical f_sw |
|-------------|-------------|
| Motor drives | 10-20 kHz |
| DC-DC converters | 50-500 kHz |
| LED drivers | 100 kHz - 2 MHz |
| RF power | 1-100 MHz |

## Exercise: PWM Parameter Sweep

Try this experiment with the buck converter:

1. Open `resources/tutorials/1xx_getting_started/103_pwm_basics/ex_3_pwm.ipes`
2. Run with D = 0.25, observe V_out
3. Change D to 0.50, re-run, compare
4. Change D to 0.75, re-run, compare

**Expected results:**

| D | Expected V_out |
|---|---------------|
| 0.25 | ~25% of V_in |
| 0.50 | ~50% of V_in |
| 0.75 | ~75% of V_in |

## Dead Time

In half-bridge and full-bridge circuits, complementary switches must never be ON simultaneously (shoot-through). A **dead time** is inserted between turn-off and turn-on:

```
Switch 1: ─────┐           ┌─────
               └───────────┘
                 |← dead →|
Switch 2: ──────┘time     └──────
                ─────────────
```

GeckoCIRCUITS supports dead-time configuration in the PWM block for bridge topologies.

## Next Steps

- [Running Simulations](running-simulations.md) - Simulation settings and solver options
- [Analysis Tools](analysis-tools.md) - Measuring and analyzing results
- [Buck Converter Tutorial](../tutorials/dcdc/buck-converter.md) - Complete design with theory
