# PI controller

A proportional-integral (PI) controller adds a proportional term and the integrated input.

## Terminals

One control input and one control output.

## Parameters

| Parameter | Unit | Function |
| --- | --- | --- |
| Gain `r0` | — | Multiplies the input for the proportional term. |
| Integrator coefficient `a1` | 1/s | Multiplies the integrated input. |
| Time constant `T` | s | Displays `r0 / a1` when `a1` is nonzero. |

## Model

The control calculator updates the integral at each control step.
