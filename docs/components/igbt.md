# IGBT

The insulated-gate bipolar transistor switches current in a power branch.

## Terminals

Collector and emitter. A gate control block supplies the command.

## Parameters

| Parameter | Unit | Function |
| --- | --- | --- |
| On and off resistance | Ω | Set the switch resistance in each state. |
| Forward voltage drop | V | Sets the on-state voltage offset. |
| Saturation current | A | Sets the current for the saturation model when enabled. |

## Model

The conduction characteristic uses the forward drop and the configured on-state resistance.
