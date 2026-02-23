---
title: "303 - Vienna Rectifier"
---

# 303 - Vienna Rectifier

Three-level unidirectional PFC rectifier with excellent input current quality.

## Overview

The Vienna rectifier is a three-level, three-phase PFC topology offering:
- High power factor (>0.99)
- Low THD (<5%)
- Reduced voltage stress on switches
- Unidirectional power flow

## Topology

### Circuit Configuration

Three-phase input with bidirectional switches connected to DC bus midpoint:

```
         D1a    D1b
    ┌────┤►├─●─┤►├────┐
    │        │        │
A ──┤     ═══╪═══     │
    │        │        ├─ +Vdc/2
B ──┤    SW(bidir)    ●─ Neutral
    │        │        ├─ -Vdc/2
C ──┤     ═══╪═══     │
    │        │        │
    └────┤◄├─●─┤◄├────┘
         D2a    D2b
```

## Operating Principle

### Switching States

| Input Current | Upper Diodes | Switch | Lower Diodes | Output |
|--------------|--------------|--------|--------------|--------|
| Positive | Conducting | OFF | Blocked | +Vdc/2 |
| Positive | Blocked | ON | Blocked | Neutral |
| Negative | Blocked | ON | Blocked | Neutral |
| Negative | Blocked | OFF | Conducting | -Vdc/2 |

### Three-Level Operation

- Voltage stress = Vdc/2 (not full Vdc)
- Reduced dv/dt stress
- Lower EMI emissions

## Key Equations

**DC Bus Voltage:**
$$V_{dc} = \sqrt{2} \cdot V_{LL} \cdot \frac{\pi}{3} \approx 1.1 \cdot V_{LL,rms}$$

**Typical for 400V input:** Vdc ≈ 700V

## Design Parameters

| Parameter | Value | Notes |
|-----------|-------|-------|
| Input Voltage | 400V L-L | Three-phase |
| DC Bus | 700V (±350V) | Split capacitor |
| Power | 10-100 kW | High power applications |
| Switching Freq | 20-50 kHz | Lower than boost PFC |
| Input Inductors | 100-500µH | Per phase |

## Advantages

- Lower switch voltage stress (Vdc/2)
- Excellent input current quality
- Three-level output reduces filter requirements
- No shoot-through risk (unidirectional switches)

## Disadvantages

- Unidirectional power flow only
- More complex than 2-level boost PFC
- Requires neutral point balancing

## Applications

- Server power supplies (>3kW)
- Telecom rectifiers
- EV battery chargers
- Industrial drives front-end

## Related Resources

- [301 - Diode Rectifier](diode-rectifier.md) - Basic rectification
- [302 - PFC Basics](pfc-basics.md) - Boost PFC fundamentals
