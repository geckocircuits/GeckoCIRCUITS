---
title: GeckoCIRCUITS Examples Library
---

# GeckoCIRCUITS Examples Library

A comprehensive collection of power electronics circuit examples demonstrating real-world applications and design techniques.

## Quick Start

1. **Browse by Category** - Navigate to the topic folder below
2. **Open Circuit** - Load the `.ipes` file in GeckoCIRCUITS
3. **Read Documentation** - Each example has documentation with theory and exercises
4. **Run & Experiment** - Modify parameters and observe results

## Example Categories

| Category | Description | Examples |
|----------|-------------|----------|
| [**Basic Topologies**](basic/) | Fundamental DC-DC converter circuits | Buck, Boost, Flyback, Forward |
| [**Power Supplies**](power-supplies/) | Isolated and regulated power supplies | LLC, DAB, PFC |
| [**Motor Drives**](motor-drives/) | Electric motor control systems | BLDC, PMSM FOC, Induction |
| [**Thermal**](thermal/) | Thermal analysis and design | Loss Calculation, Heatsink |
| [**Automotive**](automotive/) | EV and charging applications | OBC, DC Fast Charger, Traction |
| [**Renewable Energy**](renewable/) | Solar and wind applications | PV Inverter, Wind Converter |

## Examples by Difficulty

### Beginner
| Example | Category | Description |
|---------|----------|-------------|
| [Buck Converter](basic/buck.md) | Basic | Step-down DC-DC |
| [Boost Converter](basic/boost.md) | Basic | Step-up DC-DC |

### Intermediate
| Example | Category | Description |
|---------|----------|-------------|
| [Flyback Converter](basic/flyback.md) | Basic | Isolated DC-DC |
| [Forward Converter](basic/forward.md) | Basic | Isolated step-down |
| [PFC Converters](power-supplies/pfc.md) | Power Supplies | Power factor correction |
| [EV Charger](automotive/ev-charger.md) | Automotive | Level 2 AC charging |

### Advanced
| Example | Category | Description |
|---------|----------|-------------|
| [LLC Resonant](power-supplies/llc.md) | Power Supplies | Resonant converter |
| [DAB Converter](power-supplies/dab.md) | Power Supplies | Bidirectional isolated |
| [PMSM FOC](motor-drives/pmsm-foc.md) | Motor Drives | Field-oriented control |
| [BLDC Control](motor-drives/bldc.md) | Motor Drives | Trapezoidal commutation |
| [Onboard Charger](automotive/obc.md) | Automotive | Bidirectional OBC |
| [Traction Inverter](automotive/traction.md) | Automotive | EV motor drive |
| [Solar Inverter](renewable/solar.md) | Renewable | Grid-tied PV inverter |

## Example Structure

Each example folder contains:
```
example_name/
├── README.md           # Documentation with theory and exercises
├── example_basic.ipes  # Basic circuit file
├── example_ctrl.ipes   # Circuit with control (if applicable)
└── img/                # Screenshots and diagrams (optional)
```

## Running Examples

### Using Launcher Scripts
```bash
# Linux
./scripts/run-gecko-linux.sh resources/examples/basic_topologies/buck_converter/buck_basic.ipes

# Windows
scripts\run-gecko.bat resources\examples\basic_topologies\buck_converter\buck_basic.ipes

# macOS
./scripts/run-gecko-macos.sh resources/examples/basic_topologies/buck_converter/buck_basic.ipes
```

### Direct Java Execution
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar \
  resources/examples/basic_topologies/buck_converter/buck_basic.ipes
```

## Related Resources

- [**Tutorials**](../tutorials/index.md) - Step-by-step learning guides
- [**Articles**](../articles/index.md) - Technical papers and application notes

## Contributing

To contribute new examples:
1. Include complete documentation with theory and exercises
2. Test circuits in GeckoCIRCUITS before submitting
3. Ensure all referenced files are included

---
*GeckoCIRCUITS Examples Library v1.0*
*Last updated: 2026-02*
