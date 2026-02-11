---
title: 4xx - DC-AC Inverters
---

# 4xx - DC-AC Inverters

DC to AC power conversion and PWM control.

| Tutorial | Title | Difficulty | Materials |
|----------|-------|------------|-----------|
| [401](401_single_phase_inverter/) | Single-Phase Inverter | 2/3 | circuit |
| [402](402_three_phase_inverter/) | Three-Phase Inverter | 2/3 | PDF + circuits |
| [403](403_npc_inverter/) | NPC 3-Level Inverter | 3/3 | README |

## Learning Objectives

- Design single-phase PWM converters
- Understand three-phase inverter control
- Analyze voltage source rectifier (VSR) operation
- Study neutral-point-clamped (NPC) multilevel topology
- Compare two-level and multilevel inverters

## Contents

### 401 - Single-Phase Inverter
- `singlePhase_PWM_converter.ipes` - Basic PWM inverter with H-bridge

### 402 - Three-Phase Inverter
- `inverter.ipes` - Three-phase inverter model
- `three-phase_VSR_simpleControl_250kW.ipes` - 250kW VSR with control
- `three_phase_inverter.pdf` / `three_phase_inverter_solution.pdf` - Exercises

### 403 - NPC 3-Level Inverter
- Comprehensive README with theory and design guide
- Neutral-point balancing concepts
- PWM strategies for multilevel

## Quick Reference

### Inverter Types

| Type | Levels | Applications |
|------|--------|-------------|
| H-Bridge | 3 | Single-phase, low power |
| 3-Phase 2-Level | 2 | Motor drives, general |
| NPC 3-Level | 3 | High power, grid-tie |
| 5-Level | 5 | HVDC, large drives |

### Modulation Techniques

| Method | Max Modulation | THD | Notes |
|--------|----------------|-----|-------|
| SPWM | 1.0 | Higher | Simple |
| Third harmonic | 1.15 | Medium | +15% voltage |
| SVPWM | 1.15 | Lower | Best performance |

## Prerequisites

- Complete 2xx DC-DC Converters (basics of switching)
- Understanding of three-phase systems
- PWM fundamentals ([103 - PWM Basics](../1xx_getting_started/103_pwm_basics/))

## Related Examples

- [Inverters](../examples/inverters/)
- [Motor Drives](../examples/motor_drives/)
