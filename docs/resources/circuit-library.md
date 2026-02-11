---
title: Circuit Library
description: Browse all available circuit files included with GeckoCIRCUITS
---

# Circuit Library

GeckoCIRCUITS includes 100+ circuit example files (`.ipes`) organized by topic.

## How to Open

**File > Open** and navigate to `resources/`, or from command line:

```bash
java -jar target/gecko-1.0-jar-with-dependencies.jar resources/tutorials/2xx_dcdc_converters/201_buck_converter/buck_simple.ipes
```

## Getting Started

| File | Description |
|------|-------------|
| `1xx.../101.../ex_1.ipes` | First simulation example |
| `1xx.../103.../ex_3_pwm.ipes` | PWM basics |

## DC-DC Converters

| File | Topology |
|------|----------|
| `2xx.../201.../buck_simple.ipes` | Buck converter |
| `2xx.../201.../A_Buck.ipes` | Buck with control |
| `2xx.../202.../boost_simple.ipes` | Boost converter |
| `2xx.../202.../B_Boost.ipes` | Boost with control |
| `2xx.../203.../buckBoost_simple.ipes` | Buck-Boost |
| `2xx.../203.../cuk_simple.ipes` | Cuk converter |
| `2xx.../203.../sepic_simple.ipes` | SEPIC converter |

## AC-DC Rectifiers

| File | Topology |
|------|----------|
| `3xx.../301.../diode_bridge.ipes` | Diode bridge rectifier |
| `3xx.../302.../PFC_boost.ipes` | PFC boost converter |
| `3xx.../303.../Vienna_rectifier.ipes` | Vienna rectifier (250kW) |

## DC-AC Inverters

| File | Topology |
|------|----------|
| `4xx.../401.../singlePhase_PWM_converter.ipes` | Single-phase inverter |
| `4xx.../402.../inverter.ipes` | Three-phase inverter |
| `4xx.../402.../three-phase_VSR_simpleControl_250kW.ipes` | Three-phase VSR (250kW) |

## Thermal Simulation

| File | Description |
|------|-------------|
| `5xx.../501.../BuckBoost_thermal.ipes` | Buck-boost with thermal model |
| `5xx.../502.../ThreePhase_thermal.ipes` | Three-phase thermal analysis |

## EMI Filters

| File | Description |
|------|-------------|
| `6xx.../602.../CM_filter_*.ipes` | Common-mode filter topologies |
| `6xx.../602.../DM_filter_*.ipes` | Differential-mode filters |

## Scripting Examples

| File | Description |
|------|-------------|
| `7xx.../701.../GeckoSCRIPT.ipes` | GeckoSCRIPT demo |
| `7xx.../701.../buck_control.ipes` | Script-controlled buck |
| `7xx.../704.../demo_JAVA_Block.ipes` | Java block demo |
| `7xx.../704.../JavaBlockPMSM.ipes` | PMSM with Java control |

## Advanced Topics

| File | Description |
|------|-------------|
| `8xx.../801.../sparse_matrix_*.ipes` | Matrix converter topologies |
| `8xx.../802.../PMSM_control.ipes` | PMSM motor control |
| `8xx.../803.../SwissRectifier_*.ipes` | Swiss rectifier optimization |
| `8xx.../804.../thyristor_*.ipes` | Thyristor control circuits (11 files) |

## Analog Circuits

| File | Description |
|------|-------------|
| `2xx.../204.../OpAmp_inverting.ipes` | Inverting amplifier |
| `2xx.../204.../OpAmp_noninverting.ipes` | Non-inverting amplifier |
| `2xx.../204.../OpAmp_integrator.ipes` | Op-amp integrator |

## Contributing Circuits

See the [Contributing Guide](contributing.md) for how to add circuits to the library.
