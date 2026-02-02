# GeckoCIRCUITS Tutorials

This directory contains organized tutorials for learning GeckoCIRCUITS, structured by topic and difficulty level.

## Tutorial Series

| Series | Topic | Description |
|--------|-------|-------------|
| **1xx** | [Getting Started](1xx_getting_started/) | First steps with GeckoCIRCUITS |
| **2xx** | [DC-DC Converters](2xx_dcdc_converters/) | Buck, boost, and buck-boost topologies |
| **3xx** | [AC-DC Rectifiers](3xx_acdc_rectifiers/) | Diode rectifiers, PFC, Vienna rectifier |
| **4xx** | [DC-AC Inverters](4xx_dcac_inverters/) | Single and three-phase inverters |
| **5xx** | [Thermal Simulation](5xx_thermal_simulation/) | Loss calculation and junction temperature |
| **6xx** | [EMI/EMC](6xx_emi_emc/) | EMI filter design and optimization |
| **7xx** | [Scripting & Automation](7xx_scripting_automation/) | GeckoSCRIPT, MATLAB, Simulink, Java |
| **8xx** | [Advanced Topics](8xx_advanced_topics/) | Matrix converters, PMSM, optimization |

## How to Use

1. Start with the **1xx series** if you're new to GeckoCIRCUITS
2. Each tutorial folder contains:
   - `.ipes` circuit files - Open in GeckoCIRCUITS
   - `.pdf` documentation - Exercise instructions and solutions
   - `README.md` - Tutorial overview and learning objectives

## Prerequisites

- GeckoCIRCUITS 1.0 or later
- Java 21 runtime
- For scripting tutorials: MATLAB/Simulink (optional)

## Running Simulations

```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar gecko-1.0-jar-with-dependencies.jar path/to/file.ipes
```
