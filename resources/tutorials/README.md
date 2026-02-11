# GeckoCIRCUITS Tutorials

Comprehensive tutorials for learning power electronics simulation with GeckoCIRCUITS, organized by topic and difficulty level.

## Quick Start

**New to GeckoCIRCUITS?** Start with the [Getting Started series (1xx)](1xx_getting_started/).

## Tutorial Series Overview

| Series | Topic | Tutorials | Difficulty |
|--------|-------|-----------|------------|
| **[1xx](1xx_getting_started/)** | Getting Started | 7 tutorials | Beginner-Intermediate |
| **[2xx](2xx_dcdc_converters/)** | DC-DC Converters | 4 tutorials | Intermediate |
| **[3xx](3xx_acdc_rectifiers/)** | AC-DC Rectifiers | 3 tutorials | Intermediate |
| **[4xx](4xx_dcac_inverters/)** | DC-AC Inverters | 3 tutorials | Intermediate-Advanced |
| **[5xx](5xx_thermal_simulation/)** | Thermal Simulation | 3 tutorials | Advanced |
| **[6xx](6xx_emi_emc/)** | EMI/EMC | 2 tutorials | Advanced |
| **[7xx](7xx_scripting_automation/)** | Scripting & Automation | 6 tutorials | Intermediate-Advanced |
| **[8xx](8xx_advanced_topics/)** | Advanced Topics | 4 tutorials | Advanced |
| **[9xx](9xx_magnetics_mechanical/)** | Magnetics & Mechanical | 4 tutorials | Advanced |

## Learning Paths

### Path 1: Power Electronics Fundamentals
For students and engineers new to power electronics simulation:

```
101 → 102 → 103 → 104 → 201 → 202 → 203
```

1. [101 - First Simulation](1xx_getting_started/101_first_simulation/)
2. [102 - Basic Circuits](1xx_getting_started/102_basic_circuits/)
3. [103 - PWM Basics](1xx_getting_started/103_pwm_basics/)
4. [104 - Running Simulations](1xx_getting_started/104_running_simulations/)
5. [201 - Buck Converter](2xx_dcdc_converters/201_buck_converter/)
6. [202 - Boost Converter](2xx_dcdc_converters/202_boost_converter/)
7. [203 - Buck-Boost](2xx_dcdc_converters/203_buck_boost/)

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
| [101](1xx_getting_started/101_first_simulation/) | First Simulation | Launch, open, run, view results |
| [102](1xx_getting_started/102_basic_circuits/) | Basic Circuits | Component library, wiring |
| [103](1xx_getting_started/103_pwm_basics/) | PWM Basics | Duty cycle, control signals |
| [104](1xx_getting_started/104_running_simulations/) | Running Simulations | Solvers, time step, export |
| [105](1xx_getting_started/105_analysis_tools/) | Analysis Tools | Steady-state, Bode plots |
| [106](1xx_getting_started/106_state_machines/) | State Machines | Control sequencing |
| [107](1xx_getting_started/107_component_libraries/) | Component Libraries | Custom components |

### 2xx - DC-DC Converters
| # | Title | Description |
|---|-------|-------------|
| [201](2xx_dcdc_converters/201_buck_converter/) | Buck Converter | Step-down, CCM/DCM |
| [202](2xx_dcdc_converters/202_boost_converter/) | Boost Converter | Step-up, RHPZ |
| [203](2xx_dcdc_converters/203_buck_boost/) | Buck-Boost | SEPIC, Cuk, inverting |
| [204](2xx_dcdc_converters/204_analog_circuits/) | Analog Circuits | Op-amp controllers |

### 3xx - AC-DC Rectifiers
| # | Title | Description |
|---|-------|-------------|
| [301](3xx_acdc_rectifiers/301_diode_rectifier/) | Diode Rectifier | Single/three-phase bridges |
| [302](3xx_acdc_rectifiers/302_pfc_basics/) | PFC Basics | Boost PFC, current control |
| [303](3xx_acdc_rectifiers/303_vienna_rectifier/) | Vienna Rectifier | Three-level PFC |

### 4xx - DC-AC Inverters
| # | Title | Description |
|---|-------|-------------|
| [401](4xx_dcac_inverters/401_single_phase_inverter/) | Single-Phase Inverter | PWM inverter basics |
| [402](4xx_dcac_inverters/402_three_phase_inverter/) | Three-Phase Inverter | VSI, VSR control |
| [403](4xx_dcac_inverters/403_npc_inverter/) | NPC Inverter | 3-level multilevel |

### 5xx - Thermal Simulation
| # | Title | Description |
|---|-------|-------------|
| [501](5xx_thermal_simulation/501_loss_calculation/) | Loss Calculation | Conduction, switching |
| [502](5xx_thermal_simulation/502_junction_temperature/) | Junction Temperature | Thermal networks |
| [503](5xx_thermal_simulation/503_heatsink_design/) | Heatsink Design | Thermal resistance |

### 6xx - EMI/EMC
| # | Title | Description |
|---|-------|-------------|
| [601](6xx_emi_emc/601_emi_filter_basics/) | EMI Filter Basics | Filter design |
| [602](6xx_emi_emc/602_cm_dm_filters/) | CM/DM Filters | Common/differential mode |

### 7xx - Scripting & Automation
| # | Title | Description |
|---|-------|-------------|
| [701](7xx_scripting_automation/701_gecko_script_basics/) | GeckoSCRIPT Basics | Scripting language |
| [702](7xx_scripting_automation/702_matlab_integration/) | MATLAB Integration | RMI interface |
| [703](7xx_scripting_automation/703_simulink_cosimulation/) | Simulink Co-simulation | S-Function block |
| [704](7xx_scripting_automation/704_java_blocks/) | Java Blocks | Custom components |
| [705](7xx_scripting_automation/705_api_integration/) | API Integration | External signals |
| [706](7xx_scripting_automation/706_python_integration/) | Python Integration | NumPy, SciPy automation |

### 8xx - Advanced Topics
| # | Title | Description |
|---|-------|-------------|
| [801](8xx_advanced_topics/801_matrix_converters/) | Matrix Converters | Direct AC-AC |
| [802](8xx_advanced_topics/802_motor_drives_pmsm/) | Motor Drives (PMSM) | FOC control |
| [803](8xx_advanced_topics/803_optimization/) | Optimization | Swiss rectifier |
| [804](8xx_advanced_topics/804_thyristor_control/) | Thyristor Control | Phase-controlled |

### 9xx - Magnetics & Mechanical (New)
| # | Title | Description |
|---|-------|-------------|
| [901](9xx_magnetics_mechanical/901_magnetic_domain/) | Magnetic Domain | Permeance modeling |
| [902](9xx_magnetics_mechanical/902_transformer_design/) | Transformer Design | HF transformer |
| [903](9xx_magnetics_mechanical/903_inductor_saturation/) | Inductor Saturation | Non-linear inductance |
| [904](9xx_magnetics_mechanical/904_mechanical_systems/) | Mechanical Systems | Motor-load dynamics |

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

- [**Examples**](../examples/) - Complete application examples
- [**Application Examples**](../application_examples/) - Industry-specific circuits
- [**Articles**](../articles/) - Technical papers and notes

---
*GeckoCIRCUITS Tutorials v1.0*
*Last updated: 2026-02*
