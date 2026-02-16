---
title: GeckoCIRCUITS Tutorials
---

# GeckoCIRCUITS Tutorials

Comprehensive tutorials for learning power electronics simulation with GeckoCIRCUITS, organized by topic and difficulty level.

## Quick Start

**New to GeckoCIRCUITS?** Start with the [Getting Started series](../getting-started/index.md).

## Tutorial Series Overview

| Series | Topic | Tutorials | Difficulty |
|--------|-------|-----------|------------|
| **[1xx](../getting-started/index.md)** | Getting Started | 7 tutorials | Beginner-Intermediate |
| **[2xx](dcdc/)** | DC-DC Converters | 4 tutorials | Intermediate |
| **[3xx](acdc/)** | AC-DC Rectifiers | 3 tutorials | Intermediate |
| **[4xx](dcac/)** | DC-AC Inverters | 3 tutorials | Intermediate-Advanced |
| **[5xx](thermal/)** | Thermal Simulation | 3 tutorials | Advanced |
| **[6xx](emi/)** | EMI/EMC | 2 tutorials | Advanced |
| **[7xx](scripting/)** | Scripting & Automation | 6 tutorials | Intermediate-Advanced |
| **[8xx](advanced/)** | Advanced Topics | 4 tutorials | Advanced |
| **[9xx](magnetics/)** | Magnetics & Mechanical | 4 tutorials | Advanced |

## Learning Paths

### Path 1: Power Electronics Fundamentals
For students and engineers new to power electronics simulation:

```
101 → 102 → 103 → 104 → 201 → 202 → 203
```

1. [101 - First Simulation](../getting-started/first-simulation.md)
2. [102 - Basic Circuits](../getting-started/building-circuits.md)
3. [103 - PWM Basics](../getting-started/pwm-basics.md)
4. [104 - Running Simulations](../getting-started/running-simulations.md)
5. [201 - Buck Converter](dcdc/buck-converter.md)
6. [202 - Boost Converter](dcdc/boost-converter.md)
7. [203 - Buck-Boost](dcdc/buck-boost.md)

### Path 2: Grid-Tied Power Conversion
For inverter and rectifier applications:

```
201 → 301 → 302 → 401 → 402 → 403
```

### Path 3: Thermal & Reliability Design
For thermal management focus:

```
201 → 501 → 502 → 503 → 901
```

### Path 4: Advanced Analysis
For control design and optimization:

```
104 → 105 → 106 → 701 → 706
```

### Path 5: Automation & Integration
For batch simulations and external tool integration:

```
104 → 701 → 702 → 706 → 705
```

## Complete Tutorial Index

### 1xx - Getting Started
| # | Title | Description |
|---|-------|-------------|
| [101](../getting-started/first-simulation.md) | First Simulation | Launch, open, run, view results |
| [102](../getting-started/building-circuits.md) | Basic Circuits | Component library, wiring |
| [103](../getting-started/pwm-basics.md) | PWM Basics | Duty cycle, control signals |
| [104](../getting-started/running-simulations.md) | Running Simulations | Solvers, time step, export |
| [105](../getting-started/analysis-tools.md) | Analysis Tools | Steady-state, Bode plots |
| 106 | State Machines | Control sequencing |
| 107 | Component Libraries | Custom components |

### 2xx - DC-DC Converters
| # | Title | Description |
|---|-------|-------------|
| [201](dcdc/buck-converter.md) | Buck Converter | Step-down, CCM/DCM |
| [202](dcdc/boost-converter.md) | Boost Converter | Step-up, RHPZ |
| [203](dcdc/buck-boost.md) | Buck-Boost | SEPIC, Cuk, inverting |
| [204](dcdc/) | Analog Circuits | Op-amp controllers |

### 3xx - AC-DC Rectifiers
| # | Title | Description |
|---|-------|-------------|
| [301](acdc/diode-rectifier.md) | Diode Rectifier | Single/three-phase bridges |
| [302](acdc/pfc-basics.md) | PFC Basics | Boost PFC, current control |
| [303](acdc/vienna-rectifier.md) | Vienna Rectifier | Three-level PFC |

### 4xx - DC-AC Inverters
| # | Title | Description |
|---|-------|-------------|
| [401](dcac/single-phase.md) | Single-Phase Inverter | PWM inverter basics |
| [402](dcac/three-phase.md) | Three-Phase Inverter | VSI, VSR control |
| [403](dcac/npc-inverter.md) | NPC Inverter | 3-level multilevel |

### 5xx - Thermal Simulation
| # | Title | Description |
|---|-------|-------------|
| [501](thermal/loss-calculation.md) | Loss Calculation | Conduction, switching |
| [502](thermal/junction-temperature.md) | Junction Temperature | Thermal networks |
| [503](thermal/heatsink-design.md) | Heatsink Design | Thermal resistance |

### 6xx - EMI/EMC
| # | Title | Description |
|---|-------|-------------|
| [601](emi/) | EMI Filter Basics | Filter design |
| [602](emi/) | CM/DM Filters | Common/differential mode |

### 7xx - Scripting & Automation
| # | Title | Description |
|---|-------|-------------|
| [701](scripting/geckoscript.md) | GeckoSCRIPT Basics | Scripting language |
| [702](scripting/matlab.md) | MATLAB Integration | RMI interface |
| 703 | Simulink Co-simulation | S-Function block |
| [704](scripting/java-blocks.md) | Java Blocks | Custom components |
| 705 | API Integration | External signals |
| [706](scripting/python.md) | Python Integration | NumPy, SciPy automation |

### 8xx - Advanced Topics
| # | Title | Description |
|---|-------|-------------|
| [801](advanced/) | Matrix Converters | Direct AC-AC |
| [802](advanced/) | Motor Drives (PMSM) | FOC control |
| [803](advanced/) | Optimization | Swiss rectifier |
| [804](advanced/) | Thyristor Control | Phase-controlled |

### 9xx - Magnetics & Mechanical
| # | Title | Description |
|---|-------|-------------|
| [901](magnetics/magnetic-domain.md) | Magnetic Domain | Permeance modeling |
| [902](magnetics/transformer-design.md) | Transformer Design | HF transformer |
| [903](magnetics/inductor-saturation.md) | Inductor Saturation | Non-linear inductance |
| [904](magnetics/mechanical-systems.md) | Mechanical Systems | Motor-load dynamics |

## PLECS Equivalent Mapping

| PLECS Tutorial | GeckoCIRCUITS Equivalent |
|----------------|-------------------------|
| 101 Introduction | 101-102 First Simulation + Basic Circuits |
| 103 SMPS | Flyback, Forward examples |
| 104 Thermal | 501-503 Thermal series |
| 105 Magnetic | 901-903 Magnetics series |
| 110 PWM | 103 PWM Basics |
| 112 State Machine | 106 State Machines |
| 113 Python | 706 Python Integration |
| 114 Analysis Tools | 105 Analysis Tools |

## Prerequisites

- GeckoCIRCUITS 1.0 or later
- Java 21 runtime
- For scripting tutorials: MATLAB/Simulink or Python (optional)

## Related Resources

- [**Examples**](../examples/index.md) - Complete application examples
- [**Articles**](../articles/index.md) - Technical papers and notes

---
*GeckoCIRCUITS Tutorials v1.0*
*Last updated: 2026-02*
