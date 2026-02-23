# Tutorial 103: PWM Basics

## Overview

Pulse-Width Modulation (PWM) is the fundamental control technique in power electronics. This tutorial introduces PWM generation, duty cycle control, and how to apply PWM to switch power semiconductors.

**Level:** Beginner/Intermediate (2/3)

**Duration:** 30-35 minutes

**Series:** Getting Started

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Understand PWM principles and terminology
- [ ] Generate PWM signals using control blocks
- [ ] Control switch duty cycle
- [ ] Observe the relationship between duty cycle and average output
- [ ] Build a simple PWM-controlled circuit

## Prerequisites

- Complete [Tutorial 101: First Simulation](../101_first_simulation/)
- Complete [Tutorial 102: Basic Circuits](../102_basic_circuits/)
- Basic understanding of switching converters (helpful)

## Materials

| File | Description |
|------|-------------|
| `ex_3_pwm.ipes` | PWM example circuit |

## PWM Fundamentals

### What is PWM?

PWM converts a DC voltage into a pulsed waveform by switching between ON and OFF states at high frequency. The average output is controlled by the **duty cycle**.

```
        │ ON │OFF│ ON │OFF│
        ├────┼───┼────┼───┤
    Vin ┤████│   │████│   │  PWM Waveform
        │    │   │    │   │
    0   └────┴───┴────┴───┴────────── Time
        │<──>│   │<──>│
          Ton      Ton
        │<──────>│
           Ts (Period)
```

### Key Parameters

| Parameter | Symbol | Definition |
|-----------|--------|------------|
| Switching Period | Ts | Time for one complete cycle |
| Switching Frequency | fs | 1/Ts (Hz or kHz) |
| ON Time | Ton | Duration switch is ON |
| OFF Time | Toff | Duration switch is OFF |
| Duty Cycle | D | Ton/Ts (0 to 1 or 0% to 100%) |

### Duty Cycle Calculation

```
D = Ton / Ts = Ton × fs
```

### Average Output Voltage

For an ideal switch with input Vin:
```
Vout,avg = D × Vin
```

Example: Vin = 100V, D = 0.4 → Vout,avg = 40V

## PWM Generation Methods

### Method 1: Comparator-Based PWM

Compare a reference (DC or slow-varying) signal with a carrier (triangle or sawtooth):

```
  Reference (Vref) ──────────────────────────
                     ╱╲    ╱╲    ╱╲    Carrier (Triangle)
                    ╱  ╲  ╱  ╲  ╱  ╲
                   ╱    ╲╱    ╲╱    ╲

  PWM Output:      HIGH when Vref > Carrier
                   LOW when Vref < Carrier
```

### Method 2: Digital Counter

1. Counter counts from 0 to N (period)
2. Compare counter with threshold value
3. Output HIGH when counter < threshold

### GeckoCIRCUITS Implementation

Use CONTROL components:
1. **Triangle Generator (SIG):** Creates carrier waveform
2. **Comparator (CMP):** Compares reference with carrier
3. **Signal Source:** Provides DC reference (duty cycle setpoint)

## Building a PWM Generator

### Step 1: Create Triangle Carrier

1. Add **Signal Generator (SIG)** from CONTROL
2. Configure:
   - **Waveform:** Triangle
   - **Amplitude:** 1 (0 to 1 range)
   - **Frequency:** 10 kHz (switching frequency)
   - **Offset:** 0.5 (center at 0.5)

### Step 2: Add Reference Signal

1. Add **Constant Source** or **Signal Generator**
2. Set value = 0.5 (for 50% duty cycle)
3. This represents the duty cycle command (0-1 = 0-100%)

### Step 3: Create Comparator

1. Add **Comparator (CMP)** from CONTROL
2. Connect:
   - **Input A:** Reference signal
   - **Input B:** Triangle carrier
3. Output: HIGH when A > B, LOW when A < B

### Step 4: View PWM Output

1. Connect comparator output to SCOPE
2. Also connect carrier and reference for comparison
3. Run simulation

## PWM Control Circuit

### Simple Switch with PWM

```
    CONTROL DOMAIN                    POWER DOMAIN
    ┌─────────────────┐              ┌─────────────────┐
    │                 │              │                 │
    │  [Carrier]      │              │  +Vin ──[S]──┬──│
    │      │          │              │         │    │  │
    │     [CMP]───────│──── Gate ────│─────────●    │  │
    │      │          │              │              R  │
    │  [Ref/D]        │              │               │ │
    │                 │              │  GND ─────────┴─│
    └─────────────────┘              └─────────────────┘
```

### Control-Power Interface

In GeckoCIRCUITS:
- **Control signals** (SIG, CMP, etc.) connect to **gate inputs** of power switches
- The switch turns ON when gate signal is HIGH (typically > 0.5)
- The switch turns OFF when gate signal is LOW

## PWM Parameters Effect

### Varying Duty Cycle (D)

| D | Ton | Vout,avg (Vin=100V) |
|---|-----|---------------------|
| 0.1 | 10% of Ts | 10V |
| 0.3 | 30% of Ts | 30V |
| 0.5 | 50% of Ts | 50V |
| 0.7 | 70% of Ts | 70V |
| 0.9 | 90% of Ts | 90V |

### Varying Switching Frequency (fs)

Higher fs:
- Smaller output ripple
- Higher switching losses
- Smaller filter components

Lower fs:
- Larger output ripple
- Lower switching losses
- Larger filter components

Typical fs: 20 kHz - 200 kHz for power converters

## Circuit Example: PWM-Controlled Resistor

Build this circuit to see PWM in action:

```
         Control              Power
    ┌───────────────┐    ┌──────────────────┐
    │               │    │                  │
    │ Vref=0.5  ──┐ │    │  Vin    S.1      │
    │             │ │    │  ┌─┐   ┌───┐     │
    │ Triangle ─┬─┤ │    │  │ │───┤   ├───┐ │
    │ fs=10kHz  │ │ │    │  └─┘   └─┬─┘   │ │
    │           v │ │    │          │   ┌─┴─┐
    │        [CMP]─│─│──gate──      │   │ R │
    │              │ │    │         │   └─┬─┘
    │              │ │    │         └─────┤ │
    └──────────────┘ │    │      GND ─────┘ │
                     │    └─────────────────┘
```

### Expected Results

1. **Switch Voltage (Vds):**
   - 0V when ON
   - Vin when OFF

2. **Resistor Voltage (Vr):**
   - Vin when switch ON
   - 0V when switch OFF
   - Average = D × Vin

3. **Resistor Current:**
   - Vin/R when switch ON
   - 0 when switch OFF

## Advanced: Modulated PWM

Instead of a fixed reference, use a varying reference:

```
    Sine Wave Reference (Vref)
    ────╱╲────╱╲────╱╲────
       ╱  ╲  ╱  ╲  ╱  ╲

    PWM Output (variable D)
    │██│ │█││██│ │█││██│ │█│
```

This is the basis for:
- **Inverters:** Create AC from DC
- **Motor drives:** Variable speed control
- **Audio amplifiers:** Class D amplification

## Checkpoint

At this point, you should:
- [ ] Understand duty cycle and its effect on average voltage
- [ ] Be able to create a triangle carrier waveform
- [ ] Use a comparator to generate PWM
- [ ] Connect PWM to a power switch gate
- [ ] Observe PWM waveforms in the SCOPE

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| No switching | Gate not connected | Check control-power connection |
| Always ON or OFF | Reference outside carrier range | Adjust reference (0-1 range) |
| Wrong frequency | Carrier frequency setting | Check SIG parameters |
| Choppy waveform | Simulation time step too large | Reduce time step |

## Exercises

### Exercise 1: Duty Cycle Variation
1. Open `ex_3_pwm.ipes`
2. Vary the reference from 0.2 to 0.8
3. Measure average output voltage at each setting
4. **Verify:** Vout,avg = D × Vin

### Exercise 2: Frequency Effects
1. Change fs from 5 kHz to 20 kHz
2. Keep D = 0.5
3. **Observe:** Output waveform changes

### Exercise 3: PWM with LC Filter
1. Add an inductor in series with the switch output
2. Add a capacitor to ground
3. **Observe:** The output becomes nearly DC (ripple removed)

### Exercise 4: Sinusoidal PWM
1. Replace DC reference with a sine wave (50 Hz, amplitude 0.4, offset 0.5)
2. **Observe:** PWM width varies sinusoidally
3. This is the basis for inverters!

## Summary

In this tutorial, you learned:
1. PWM principles: duty cycle, frequency, carrier
2. Comparator-based PWM generation
3. Connecting control signals to power switches
4. The relationship between D and average output
5. Foundation for power converter control

## Key Formulas

| Formula | Description |
|---------|-------------|
| D = Ton/Ts | Duty cycle definition |
| Vout,avg = D × Vin | Average output (ideal switch) |
| fs = 1/Ts | Switching frequency |

## Next Steps

Continue your learning with:
- **Tutorial:** [201 - Buck Converter](../../2xx_dcdc_converters/201_buck_converter/) - Apply PWM to real converter
- **Example:** [Basic Topologies](../../examples/basic_topologies/) - Complete converter examples

---
*Tutorial Version: 1.0*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
