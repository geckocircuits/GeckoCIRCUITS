---
title: GeckoCIRCUITS Examples Library
---

# GeckoCIRCUITS Examples Library

A comprehensive collection of power electronics circuit examples demonstrating real-world applications and design techniques.

## Quick Start

1. **Browse by Category** - Navigate to the topic folder below
2. **Open Circuit** - Load the `.ipes` file in GeckoCIRCUITS
3. **Read Documentation** - Each example has a `README.md` with theory and exercises
4. **Run & Experiment** - Modify parameters and observe results

## Example Categories

| Category | Description | Examples |
|----------|-------------|----------|
| [**Basic Topologies**](basic_topologies/) | Fundamental DC-DC converter circuits | Buck, Boost, Flyback, Forward |
| [**Power Supplies**](power_supplies/) | Isolated and regulated power supplies | LLC, DAB, PFC, Isolated DC-DC |
| [**Motor Drives**](motor_drives/) | Electric motor control systems | BLDC, PMSM FOC, Induction |
| [**Rectifiers**](rectifiers/) | AC-DC conversion circuits | Diode Bridge, Vienna, Swiss |
| [**Inverters**](inverters/) | DC-AC conversion circuits | Single/Three-Phase, NPC, MMC |
| [**Thermal**](thermal/) | Thermal analysis and design | Loss Calculation, Heatsink |
| [**Automotive**](automotive/) | EV and charging applications | OBC, DC Fast Charger, Traction |
| [**Renewable Energy**](renewable_energy/) | Solar and wind applications | PV Inverter, Wind Converter |

## Examples by Difficulty

### Beginner
| Example | Category | Description |
|---------|----------|-------------|
| [Buck Converter](basic_topologies/buck_converter/) | Basic | Step-down DC-DC |
| [Boost Converter](basic_topologies/boost_converter/) | Basic | Step-up DC-DC |
| [Diode Bridges](rectifiers/diode_bridges/) | Rectifiers | Basic AC-DC |

### Intermediate
| Example | Category | Description |
|---------|----------|-------------|
| [Flyback Converter](basic_topologies/flyback_converter/) | Basic | Isolated DC-DC |
| [Forward Converter](basic_topologies/forward_converter/) | Basic | Isolated step-down |
| [PFC Converters](power_supplies/pfc_converters/) | Power Supplies | Power factor correction |
| [Three-Phase VSI](inverters/three_phase_vsi/) | Inverters | Grid-tied inverter |
| [Loss Calculation](thermal/loss_calculation/) | Thermal | Semiconductor losses |
| [EV Charger](automotive/ev_charger/) | Automotive | Level 2 AC charging |

### Advanced
| Example | Category | Description |
|---------|----------|-------------|
| [LLC Resonant](power_supplies/llc_resonant/) | Power Supplies | Resonant converter |
| [DAB Converter](power_supplies/dab_converter/) | Power Supplies | Bidirectional isolated |
| [NPC Multilevel](inverters/multilevel_npc/) | Inverters | 3-level inverter |
| [MMC Converter](inverters/mmc_converter/) | Inverters | Modular multilevel |
| [PMSM FOC](motor_drives/pmsm_foc/) | Motor Drives | Field-oriented control |
| [Thermal Networks](thermal/thermal_networks/) | Thermal | Transient thermal |
| [Onboard Charger](automotive/onboard_charger/) | Automotive | Bidirectional OBC |
| [Traction Inverter](automotive/traction_inverter/) | Automotive | EV motor drive |

## Example Structure

Each example folder contains:
```
example_name/
├── README.md           # Documentation with theory and exercises
├── example_basic.ipes  # Basic circuit file
├── example_ctrl.ipes   # Circuit with control (if applicable)
└── img/                # Screenshots and diagrams (optional)
```

## PLECS Equivalent Examples

| PLECS Example | GeckoCIRCUITS Equivalent |
|---------------|-------------------------|
| Basic Topologies | [basic_topologies/](basic_topologies/) |
| Power Supplies | [power_supplies/](power_supplies/) |
| Motor Drives | [motor_drives/](motor_drives/) |
| Thermal | [thermal/](thermal/) |
| Automotive | [automotive/](automotive/) |
| Renewable Energy | [renewable_energy/](renewable_energy/) |

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

## Documentation Templates

For contributors, documentation templates are available in [`_templates/`](_templates/):
- `README_example.md` - Template for example documentation
- `README_tutorial.md` - Template for tutorial documentation

## Related Resources

- [**Tutorials**](../tutorials/) - Step-by-step learning guides
- [**Application Examples**](../application_examples/) - Industry-specific applications
- [**Articles**](../articles/) - Technical papers and application notes

## Status Legend

| Status | Meaning |
|--------|---------|
| ✅ Complete | Circuit files and full documentation |
| ⚠️ Partial | README available, circuit files pending |
| 📝 Placeholder | Documentation only, circuits to be added |

## Contributing

To contribute new examples:
1. Follow the structure in `_templates/README_example.md`
2. Include complete documentation with theory and exercises
3. Test circuits in GeckoCIRCUITS before submitting
4. Ensure all referenced files are included

---
*GeckoCIRCUITS Examples Library v1.0*
*Last updated: 2026-02*
