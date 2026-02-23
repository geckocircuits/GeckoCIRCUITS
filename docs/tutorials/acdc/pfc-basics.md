---
title: "302 - PFC Basics"
---

# 302 - PFC Basics

Power Factor Correction fundamentals using boost converter topology.

## Overview

PFC circuits shape the input current to be sinusoidal and in phase with the voltage, achieving near-unity power factor and reducing harmonic distortion.

## Why PFC?

Without PFC, rectifiers draw peaky current that:
- Causes harmonic pollution on the grid
- Reduces effective power delivery
- Fails regulatory standards (IEC 61000-3-2)

## Boost PFC Topology

### Circuit Configuration

```
        L           D
AC+ ──┬─⊐⊏──┬──────┤►├─┬── DC+
      │     │           │
     ⌇      │    SW     C    Load
Cin  ⌇      └────┤      │
      │          │      │
AC- ──┴──────────┴──────┴── DC-
```

### Operating Principle

1. **Switch ON:** Inductor charges, current rises
2. **Switch OFF:** Inductor discharges through diode to output
3. **Current Shaping:** PWM duty cycle modulated to make current follow voltage

## Control Methods

### Average Current Mode Control

- Current loop tracks reference derived from voltage
- Excellent current waveform
- Complex implementation

### Critical Conduction Mode (CRM)

- Variable frequency operation
- Zero-current switching
- Simpler implementation
- Higher peak currents

## Key Equations

**Duty Cycle (CCM):**
$$D = 1 - \frac{V_{in}(t)}{V_{out}}$$

**Inductor Current Ripple:**
$$\Delta I_L = \frac{V_{in} \cdot D}{L \cdot f_{sw}}$$

**Power Factor:**
$$PF = \frac{P}{S} = \frac{V_{rms} \cdot I_{rms} \cdot \cos\phi}{V_{rms} \cdot I_{rms}}$$

## Design Parameters

| Parameter | Typical Value | Notes |
|-----------|---------------|-------|
| Output Voltage | 385-400V DC | Above peak AC |
| Switching Freq | 65-130 kHz | EMI tradeoffs |
| Inductor | 200-500µH | Ripple control |
| Output Cap | 200-400µF | Holdup time |

## Simulation Exercises

1. Compare PF with and without correction
2. Observe inductor current shaping
3. Vary load and check regulation
4. Analyze THD of input current

## Related Resources

- [301 - Diode Rectifier](diode-rectifier.md) - Basic rectification
- [303 - Vienna Rectifier](vienna-rectifier.md) - Three-level PFC
