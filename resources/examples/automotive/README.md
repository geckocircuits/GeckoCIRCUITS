# Automotive Examples

Power electronics for electric vehicles, hybrid vehicles, and charging infrastructure.

## Examples

| Example | Description | Difficulty |
|---------|-------------|------------|
| [EV Charger](ev_charger/) | Level 2 AC charging | Intermediate |
| [Onboard Charger](onboard_charger/) | OBC with PFC and isolation | Advanced |
| [DC Fast Charger](dc_fast_charger/) | Level 3 DC charging station | Advanced |
| [Traction Inverter](traction_inverter/) | EV motor drive inverter | Advanced |

## Quick Reference

### EV Charging Levels

| Level | Voltage | Power | Connector | Time (0-80%) |
|-------|---------|-------|-----------|--------------|
| Level 1 | 120V AC | 1.4 kW | J1772 | 20+ hours |
| Level 2 | 240V AC | 7-19 kW | J1772 | 4-8 hours |
| Level 3 DC | 200-1000V DC | 50-350 kW | CCS/CHAdeMO | 20-40 min |

### Typical EV Power Architecture

```
                                    ┌─────────────┐
Grid ───[OBC]───┐                   │   Motor     │
                │   ┌─────────┐     │  (PMSM)     │
                ├──►│ Battery ├────►│ ┌─────────┐ │
DC Charger ─────┘   │ 400-800V│     │ │Inverter │ │
                    └────┬────┘     │ └─────────┘ │
                         │          └─────────────┘
                    ┌────▼────┐
                    │ DC-DC   │──► 12V Auxiliary
                    │ (LV)    │
                    └─────────┘
```

### Key Specifications

| System | Voltage | Power | Efficiency |
|--------|---------|-------|------------|
| Onboard Charger | 400V/800V | 11-22 kW | >94% |
| Traction Inverter | 400V/800V | 100-300 kW | >97% |
| DC-DC Converter | 400V→12V | 2-3 kW | >95% |
| DC Fast Charger | 200-1000V | 50-350 kW | >95% |

## Design Considerations

### Automotive Requirements

| Requirement | Specification |
|-------------|---------------|
| Operating temp | -40°C to +85°C (ambient) |
| EMC | CISPR 25 Class 5 |
| Safety | ISO 26262 (ASIL B-D) |
| Efficiency | DoE Level VI minimum |
| Power density | >3 kW/L target |

### Isolation Requirements

| Application | Isolation | Standard |
|-------------|-----------|----------|
| OBC (AC-DC) | Required | IEC 61851 |
| Traction inverter | Not required | - |
| DC-DC (HV-LV) | Required | ISO 6469 |
| Charging coupler | Required | IEC 62196 |

## Related Tutorials

- [302 - PFC Basics](../../tutorials/3xx_acdc_rectifiers/302_pfc_basics/)
- [Flyback Converter](../basic_topologies/flyback_converter/)
- [LLC Resonant](../power_supplies/llc_resonant/)
- [Motor Drives](../motor_drives/)
