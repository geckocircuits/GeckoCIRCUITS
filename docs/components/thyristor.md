# Thyristor

A thyristor starts to conduct when it receives a gate command under forward bias.

## Terminals

Anode and cathode. A gate control block supplies the command.

## Parameters

| Parameter | Unit | Function |
| --- | --- | --- |
| On and off resistance | Ω | Set the resistance in each conduction state. |
| Forward voltage drop | V | Sets the on-state voltage offset. |
| Reverse recovery delay | s | Sets the delay in the reverse recovery model. |

## Model

The conduction state depends on the gate command and the electrical conditions.
