# Rectifier Examples

AC to DC power conversion circuits for power supplies and motor drives.

## Examples

| Example | Description | Difficulty |
|---------|-------------|------------|
| [Diode Bridges](diode_bridges/) | Single and three-phase | Beginner |
| [Vienna Rectifier](vienna_rectifier/) | 3-level PFC rectifier | Advanced |
| [Swiss Rectifier](swiss_rectifier/) | Ultra-compact topology | Advanced |

## Quick Reference

### Topology Comparison

| Rectifier | Phases | PF | THD | Efficiency | Complexity |
|-----------|--------|-----|-----|------------|------------|
| Diode Bridge | 1/3 | 0.6-0.8 | 80%+ | 98% | Low |
| Boost PFC | 1 | >0.99 | <5% | 95% | Medium |
| Vienna | 3 | >0.99 | <5% | 97% | High |
| Swiss | 3 | >0.99 | <5% | 98% | Very High |

### Power Factor Correction (PFC)

**Why PFC?**
- Regulatory requirements (IEC 61000-3-2)
- Reduced utility current, smaller conductors
- Higher efficiency, less harmonic heating

**Boost PFC Principle:**
- Shape input current to follow voltage
- Unity power factor (sinusoidal, in-phase)
- Typically 85-265VAC universal input

## Related Tutorials

- [301 - Diode Rectifier](../../tutorials/3xx_acdc_rectifiers/301_diode_rectifier/)
- [302 - PFC Basics](../../tutorials/3xx_acdc_rectifiers/302_pfc_basics/)
- [303 - Vienna Rectifier](../../tutorials/3xx_acdc_rectifiers/303_vienna_rectifier/)
