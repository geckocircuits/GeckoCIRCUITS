---
title: Basic Topologies Examples
---

# Basic Topologies Examples

Fundamental DC-DC converter circuits demonstrating essential power conversion principles.

## Examples

| Example | Description | Difficulty |
|---------|-------------|------------|
| [Buck Converter](buck_converter/index.md) | Step-down DC-DC | Beginner |
| [Boost Converter](boost_converter/index.md) | Step-up DC-DC | Beginner |
| [Buck-Boost](buck_boost_converter/) | Inverting DC-DC | Intermediate |
| [Flyback](flyback_converter/index.md) | Isolated step-up/down | Intermediate |
| [Forward](forward_converter/index.md) | Isolated step-down | Intermediate |
| [Dual Active Bridge](../power-supplies/dab.md) | High-power isolated | Advanced |

## Quick Reference

### Non-Isolated Topologies

| Topology | Conversion | Vout/Vin | Key Characteristic |
|----------|------------|----------|-------------------|
| Buck | Step-down | D | Continuous input current |
| Boost | Step-up | 1/(1-D) | Continuous output current |
| Buck-Boost | Inverting | -D/(1-D) | Inverted polarity |
| Cuk | Inverting | -D/(1-D) | Continuous I/O current |
| SEPIC | Non-inverting | D/(1-D) | Same polarity as boost |

### Isolated Topologies

| Topology | Conversion | Vout/Vin | Power Range |
|----------|------------|----------|-------------|
| Flyback | Up/Down | n×D/(1-D) | 5-150W |
| Forward | Down | n×D | 50-500W |
| Half-Bridge | Up/Down | n×D | 100-500W |
| Full-Bridge | Up/Down | n×D | 500W+ |

## Learning Path

1. Start with **Buck** and **Boost** to understand basic principles
2. Progress to **Buck-Boost** for inverting operation
3. Study **Flyback** for isolation and coupled inductors
4. Explore **Forward** and **Full Bridge** for higher power

## Related Tutorials

- [201 - Buck Converter](../../tutorials/dcdc/buck-converter.md)
- [202 - Boost Converter](../../tutorials/dcdc/boost-converter.md)
- [203 - Buck-Boost Topologies](../../tutorials/dcdc/buck-boost.md)
