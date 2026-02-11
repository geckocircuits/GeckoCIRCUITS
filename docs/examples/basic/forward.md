---
title: Forward Converter Example
---

# Forward Converter Example

Isolated buck-derived DC-DC converter.

## Overview

The forward converter is an isolated version of the buck converter:
- Single-ended topology
- Transformer provides isolation (not energy storage)
- Requires reset mechanism for transformer

## Specifications

| Parameter | Value |
|-----------|-------|
| Input Voltage | 48V DC |
| Output Voltage | 5V DC |
| Output Power | 50W |
| Switching Frequency | 200 kHz |
| Turns Ratio | 4:1 |
| Output Inductor | 22 µH |

## Circuit Files

- `forward_basic.ipes` - Basic forward converter
- `forward_rcd_reset.ipes` - RCD clamp reset
- `forward_active_clamp.ipes` - Active clamp reset

## Theory

### Operating Principle

**Switch ON:**
- Power transfers directly through transformer
- Output inductor charges
- Similar to buck "on" state

**Switch OFF:**
- Transformer must reset
- Output inductor freewheels
- Similar to buck "off" state

### Key Equations

**Voltage Conversion Ratio:**
$$V_{out} = D \cdot V_{in} \cdot \frac{N_s}{N_p}$$

**Maximum Duty Cycle (with tertiary reset):**
$$D_{max} = \frac{N_p}{N_p + N_r}$$

For 1:1 reset winding: Dmax = 50%

## Reset Methods

### Tertiary Winding Reset

```
    ┌──┤N_p├──┤SW├──┐
    │              │
Vin─┤──┤N_r├──┤D├──┤
    │              │
    └──────────────┘
```

- Simple, reliable
- Energy returned to input
- Dmax limited by turns ratio

### RCD Clamp Reset

- Higher Dmax possible (up to 70%)
- Energy dissipated in resistor
- Voltage stress on switch higher

### Active Clamp Reset

- Zero voltage switching possible
- Energy recycled
- More complex drive circuit

## Design Procedure

1. **Select turns ratio** for desired Dmax margin
2. **Calculate output inductor** for ripple requirement
3. **Select reset method** based on efficiency/complexity tradeoff
4. **Design transformer** for saturation avoidance

## Exercises

1. **Compare Reset Methods:** Efficiency and component stress
2. **Vary Duty Cycle:** Observe transformer magnetizing current
3. **Transient Response:** Load step performance
4. **Increase Dmax:** What happens near reset limit?

## Related Resources

- [Flyback Converter](flyback.md)
- [Buck Converter](buck.md)
- [Transformer Design Tutorial](../../tutorials/magnetics/transformer-design.md)
