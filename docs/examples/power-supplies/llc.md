---
title: LLC Resonant Converter Example
---

# LLC Resonant Converter Example

High-efficiency resonant DC-DC converter with ZVS operation.

## Overview

The LLC resonant converter offers:
- Zero Voltage Switching (ZVS) for all switches
- Zero Current Switching (ZCS) for secondary diodes
- High efficiency (>96%)
- Wide input voltage range with frequency modulation

## Specifications

| Parameter | Value |
|-----------|-------|
| Input Voltage | 360-400V DC |
| Output Voltage | 12V DC |
| Output Power | 500W |
| Resonant Frequency | 100 kHz |
| Transformer Ratio | 16:1 |

## Circuit Files

- `llc_basic.ipes` - Basic LLC operation
- `llc_frequency_sweep.ipes` - Gain vs frequency analysis

## Theory

### Resonant Tank

```
    Lr      Cr
 ───⊏⊐───┤├───┬───
              │
             Lm    Transformer
              │
 ─────────────┴───
```

- Lr: Series resonant inductance
- Cr: Series resonant capacitance
- Lm: Magnetizing inductance (parallel)

### Resonant Frequencies

**Series Resonance:**
$$f_r = \frac{1}{2\pi\sqrt{L_r C_r}}$$

**Parallel Resonance:**
$$f_p = \frac{1}{2\pi\sqrt{(L_r + L_m) C_r}}$$

### Operating Regions

| Region | fsw vs fr | ZVS | ZCS |
|--------|-----------|-----|-----|
| Below fr | fsw < fr | Yes | No |
| At fr | fsw = fr | Yes | Yes |
| Above fr | fsw > fr | Yes | Yes |

## Design Parameters

### Quality Factor

$$Q = \frac{\sqrt{L_r/C_r}}{R_{ac}}$$

Where Rac = (8/π²)·n²·Rload

### Inductance Ratio

$$L_n = \frac{L_m}{L_r}$$

Typical range: 3-7

## Key Equations

**Voltage Gain (FHA):**
$$M = \frac{L_n \cdot f_n^2}{\sqrt{(L_n + 1 - L_n \cdot f_n^2)^2 + Q^2(f_n - 1/f_n)^2 \cdot L_n^2}}$$

Where fn = fsw/fr

## Exercises

1. **Frequency Sweep:** Plot gain vs frequency
2. **ZVS Verification:** Observe switch voltage at turn-on
3. **Load Variation:** Analyze efficiency vs load
4. **Resonant Component Tolerance:** Effect on gain curve

## Related Resources

- [DAB Converter](dab.md)
- [502 - Junction Temperature](../../tutorials/thermal/junction-temperature.md)
