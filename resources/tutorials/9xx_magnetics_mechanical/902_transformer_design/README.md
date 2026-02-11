# Tutorial 902: Transformer Design

## Overview

Design and simulate power transformers for switched-mode power supplies. Learn to model ideal and non-ideal transformer behavior including leakage inductance, winding capacitance, and core saturation.

**Level:** Advanced (3/3)

**Duration:** 45-60 minutes

**Series:** Magnetics & Mechanical

**Status:** Placeholder - Circuit files to be added

## Learning Objectives

- Design transformers for flyback, forward, and full-bridge converters
- Model leakage inductance and its effects
- Simulate winding capacitance and high-frequency behavior
- Understand transformer core selection

## Transformer Equivalent Circuit

### Complete Model

```
    Primary                                    Secondary
    ───┬──[Rp]──[Llkp]──┬────n:1────┬──[Llks]──[Rs]──┬───
       │                │           │                │
      Cp               Lm          Rc               Cs
       │                │           │                │
    ───┴────────────────┴───────────┴────────────────┴───
```

| Element | Description | Typical Value |
|---------|-------------|---------------|
| Rp, Rs | Winding resistance | mΩ to Ω |
| Llkp, Llks | Leakage inductance | 1-5% of Lm |
| Lm | Magnetizing inductance | μH to mH |
| Rc | Core loss resistance | kΩ |
| Cp, Cs | Winding capacitance | pF to nF |

## Design Procedure

### Step 1: Determine Requirements

| Parameter | Symbol | Example |
|-----------|--------|---------|
| Input voltage | Vin | 400V DC |
| Output voltage | Vout | 12V |
| Output power | Pout | 100W |
| Switching frequency | fs | 100kHz |
| Efficiency target | η | 95% |

### Step 2: Select Turns Ratio

```
n = Np/Ns = (Vin × D) / (Vout × (1-D))  [Flyback]
n = Np/Ns = Vin × D / Vout              [Forward]
```

### Step 3: Calculate Magnetizing Inductance

For flyback (energy storage):
```
Lm = (Vin × Dmax)² / (2 × Pout × fs)
```

For forward (no energy storage):
```
Lm > (Vin × Dmax) / (ΔIm × fs)
```

### Step 4: Select Core

Area product method:
```
Ap = Aw × Ac = (Pout × 10⁴) / (Kw × Bmax × fs × J)
```

| Core | Ap (cm⁴) | Power Range |
|------|----------|-------------|
| EE16 | 0.05 | 5-15W |
| EE25 | 0.2 | 15-50W |
| EE35 | 0.8 | 50-150W |
| EE42 | 2.0 | 150-300W |
| ETD49 | 5.0 | 300-500W |

### Step 5: Calculate Turns

```
Np = (Vin × Dmax) / (Bmax × Ac × fs)
Ns = Np / n
```

## Exercises

### Exercise 1: Ideal Transformer
1. Model ideal transformer with n = 10:1
2. Apply 100V AC at 100kHz
3. Verify Vs = Vp/n

### Exercise 2: Leakage Inductance
1. Add 2% leakage to flyback transformer
2. Observe voltage spike at turn-off
3. Design snubber to limit spike

### Exercise 3: Core Saturation
1. Model transformer with Bsat = 0.3T
2. Apply excessive volt-seconds
3. Observe saturation effects

## Circuit Files

> **Status:** Placeholder
> - `transformer_ideal.ipes`
> - `transformer_leakage.ipes`
> - `transformer_fullmodel.ipes`

---
*Tutorial Version: 1.0 (Placeholder)*
