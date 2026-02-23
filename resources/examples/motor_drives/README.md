# Motor Drive Examples

Electric motor control systems for industrial, automotive, and renewable energy applications.

## Examples

| Example | Description | Difficulty |
|---------|-------------|------------|
| [BLDC Control](bldc_control/) | Brushless DC with trapezoidal | Intermediate |
| [PMSM FOC](pmsm_foc/) | Field-oriented control | Advanced |
| [Induction Motor](induction_motor/) | V/f and vector control | Advanced |

## Quick Reference

### Motor Types Comparison

| Motor | Control | Sensors | Efficiency | Cost |
|-------|---------|---------|------------|------|
| BLDC | Trapezoidal | Hall | Good | Low |
| PMSM | FOC | Encoder | Excellent | High |
| IM | V/f or FOC | None/Encoder | Good | Low |

### Control Strategies

| Strategy | Description | Complexity | Performance |
|----------|-------------|------------|-------------|
| Trapezoidal | 6-step commutation | Low | Torque ripple |
| SPWM | Sinusoidal PWM | Medium | Good |
| FOC | Field-oriented (dq) | High | Excellent |
| DTC | Direct torque control | High | Fast response |

### FOC Block Diagram

```
Speed Ref → [PI] → Id/Iq Ref → [PI×2] → Vd/Vq → [Inverse Park] → Va,Vb,Vc → [PWM] → Inverter
                      ↑                                               ↑
               [Park Transform] ← [Clarke] ← [Current Sense]          │
                      ↑                                               │
               [Position/Speed] ←────────────────────────────────── Motor
```

## Related Tutorials

- [802 - Motor Drives PMSM](../../tutorials/8xx_advanced_topics/802_motor_drives_pmsm/)
- [402 - Three-Phase Inverter](../../tutorials/4xx_dcac_inverters/402_three_phase_inverter/)
