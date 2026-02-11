---
title: BLDC Motor Control Example
---

# BLDC Motor Control Example

Brushless DC (BLDC) motor with six-step trapezoidal commutation for simple and robust control.

## Overview

BLDC motors provide:
- High torque density (permanent magnet rotor)
- Trapezoidal back-EMF (easier commutation than sinusoidal)
- Simple six-step (120° conduction) or three-phase commutation
- Hall sensor feedback or sensorless operation
- Excellent speed control and efficiency
- Suitable for appliances, power tools, e-bikes, drones

## Specifications

| Parameter | Value |
|-----------|-------|
| Rated Power | 500W |
| DC Bus Voltage | 48V |
| Rated Speed | 3000 RPM |
| Pole Pairs | 4 |
| Rated Torque | 1.6 Nm |
| Kt (Torque Constant) | 0.1 Nm/A |
| Ke (Back-EMF Constant) | 0.1 V/(rad/s) |
| Phase Resistance (Ra) | 0.2 Ω |
| Phase Inductance (La) | 2 mH |
| Moment of Inertia (J) | 0.001 kg·m² |
| Feedback | Hall sensors (3-sensor configuration) |

## Circuit Files

- `bldc_six_step_basic.ipes` - Basic six-step commutation with PWM speed control
- `bldc_hall_sensors.ipes` - Hall sensor decoding and commutation switching
- `bldc_speed_control.ipes` - Speed loop with PI controller
- `bldc_current_limiting.ipes` - Current limit protection during startup
- `bldc_sensorless.ipes` - Back-EMF zero-crossing detection (advanced)

## BLDC Motor Fundamentals

### Motor Construction

The BLDC motor has:
- **Stator:** Three-phase armature windings (A, B, C) fixed around the bore
- **Rotor:** Permanent magnets creating trapezoidal flux distribution
- **Hall Sensors:** Three digital sensors (H1, H2, H3) mounted 120° apart, detect rotor position

### Trapezoidal Back-EMF

Unlike sinusoidal PMSM, the BLDC has trapezoidal back-EMF:

$$e_{ph}(t) = K_e \cdot \omega \cdot f_{trap}(\theta_e)$$

Where f_trap(θe) is a trapezoidal waveform:
- Constant for 120° (±Ke·ω)
- Transition for 60° (zero crossing)
- Repeats every 360°

This trapezoidal shape allows simple on-off commutation (no sinusoidal modulation).

## Six-Step Commutation

### Hall Sensor Arrangement

Three Hall sensors placed 120° mechanical apart provide 6 distinct states:

| Hall Code | H1 | H2 | H3 | Active Phases | Back-EMF Zero-X |
|-----------|----|----|-----|---------------|-----------------|
| 1 | 1 | 0 | 1 | A+, B- (H1→H3) | Between A-B |
| 2 | 1 | 0 | 0 | A+, C- (H1→H2) | Between A-C |
| 3 | 1 | 1 | 0 | B+, C- (H1→H2) | Between B-C |
| 4 | 0 | 1 | 0 | B+, A- (H2→H3) | Between B-A |
| 5 | 0 | 1 | 1 | C+, A- (H2→H3) | Between C-A |
| 6 | 0 | 0 | 1 | C+, B- (H3→H1) | Between C-B |

**Note:** Each state represents a 60° electrical sector.

### Commutation Switching

At each Hall transition, switch which two phases are active:

```
Sector 1 (H1=1,H2=0,H3=1): Phase A → Phase B return
  Top FET:    Q1 (Phase A) ON    → Q3 (Phase B) ON
  Bottom FET: Q5 (Phase B) OFF   → Q6 (Phase C) OFF

Sector 2 (H1=1,H2=0,H3=0): Phase B → Phase C return
  Top FET:    Q1 (Phase A) ON    → Q5 (Phase C) ON
  Bottom FET: Q4 (Phase C) OFF   → Q6 (Phase B) OFF

[... continue for all 6 sectors]
```

**Hard Switching vs. Soft Switching:**
- **Hard switching:** Switch only at Hall edge (simple, generates noise)
- **Commutation advance:** Switch 30° before Hall edge for smooth transition (requires accurate timing)

## Motor Equations

### Back-EMF Equation

At rated speed ωr (rad/s):
$$e_{ph}(t) = K_e \cdot \omega_r \cdot f_{trap}(θ_e)$$

**RMS back-EMF (per phase):**
$$E_{rms} = \frac{K_e \cdot \omega_r}{\sqrt{3}}$$

For sinusoidal waveform, Ke = (30/π) × Φm × Z / P, where:
- Φm: Peak flux per pole (Wb)
- Z: Total conductors per phase
- P: Number of poles

### Torque Production

Instantaneous torque:
$$\tau_e(t) = K_t \cdot i_{ph}(t)$$

Where Kt = Ke (electromagnetic constant).

**Average torque (during 120° conduction):**
$$\bar{\tau}_e = K_t \cdot I_{avg}$$

For six-step drive, Iavg is the DC link current (one phase always conducting).

### Motor Equations (Phase Model)

For a single phase:
$$v_{ph} = R_a \cdot i_{ph} + L_a \frac{di_{ph}}{dt} + e_{ph}(t)$$

Where:
- vph: Applied phase voltage (0V or Vdc depending on switch state)
- Ra, La: Phase resistance and inductance
- eph: Back-EMF

## Control Structure

### Speed Control Loop

```
           Speed Ref (ωref)
                │
           ┌────┴─────┐
           │    -     │
           └────┬─────┘
                │ ω_error
                ▼
         ┌────────────┐
         │  Speed PI  │ ◄─ Integral Anti-Windup
         │ Controller │
         └────┬───────┘
              │ I_ref_cmd
              ▼
         ┌─────────────────┐
         │ Current Limiter │ ◄─ Peak Current Limit
         │ (max Iref=50A)  │
         └────┬────────────┘
              │ PWM Duty
              ▼
         ┌──────────────┐
         │    PWM on    │ ◄─ Hall Sensors (commutation)
         │  High-Side   │
         │  or Low-Side │
         └────┬─────────┘
              │
              ▼
         ┌──────────────┐
         │   3-Phase    │
         │   Inverter   │
         └────┬─────────┘
              │
              ▼
         ┌──────────────┐
         │  BLDC Motor  │
         └────┬─────────┘
              │
    ┌─────────┴──────────┐
    │ Hall Sensors       │
    │ Speed Estimation   │
    │ (Hall edges → ω)   │
    └─────────┬──────────┘
              │
              └──► Speed Feedback
```

### Current Control Options

**Option 1: Hysteresis Band Control (Simple)**
```
I_ref = I_max × (PWM_duty / 100)

Actual current feedback:
if i_ph > I_ref + ΔI_hys
    turn OFF PWM
if i_ph < I_ref - ΔI_hys
    turn ON PWM
```

**Option 2: PI Current Loop (Smoother)**
```
I_error = I_ref - I_actual

PWM_duty = K_p × I_error + K_i × ∫I_error dt

Limit: PWM_duty ∈ [0, 100%]
```

**Option 3: Voltage Control (Simplest)**
```
PWM_duty = Duty_ref (directly from speed PI)
No explicit current feedback
```

## Sensorless Operation (Back-EMF Zero-Crossing)

For sensorless control, detect the back-EMF zero-crossing of the floating (non-conducting) phase.

### Detection Principle

During each 120° sector, one phase is floating (not part of the active phase pair).

**Example: Sector 1 (A+, B-)**
```
Phase A: Positive high side → floating (not actively driven)
Phase B: Negative low side → floating
Phase C: Floating (detect back-EMF zero crossing)

Back-EMF of phase C:
e_C increases from 0 to peak (positive half-cycle)

Zero-crossing occurs 60° after sector start
```

### Algorithm

```
At each Hall transition:
1. Determine floating phase (the non-conducting one)
2. Read phase voltage via ADC
3. Compare to neutral point (Vdc/2)
4. Detect when e_phase crosses zero (slope change)
5. Apply time delay ≈ 30° (= Ts/6, where Ts = 6 × Thall)
6. Generate next commutation command

Blanking period: First 30° of sector (wait for current decay)
Detection window: 30-60° (look for zero-crossing)
Time delay offset: Advance next commutation by 30°
```

## Startup Procedure (Open-Loop Ramp)

For sensorless motors, startup requires special handling:

```
1. Initial Alignment (500 ms)
   - Apply positive current to phase A
   - Aligns rotor to known position

2. Ramp-Up (1000 ms)
   - Commutate at fixed frequency (e.g., 50 Hz starting)
   - Gradually increase frequency toward expected speed
   - Keep current below limit

3. Transition to Closed-Loop (when speed sufficient)
   - Once back-EMF amplitude exceeds threshold
   - Switch to zero-crossing detection
   - Monitor for successful lock

4. Speed Loop Control
   - Closed-loop frequency modulation
   - Adjust commutation frequency to match motor speed
```

## Design Steps

### Step 1: Inverter Bridge Selection

For 48V DC, 500W:
- Peak phase current: Ipk = 500W / (48V × 0.85) ≈ 12A
- Average current: Iavg = 10A (at rated power)
- Select switches rated for Vce ≥ 60V, Ic ≥ 20A (margin)
- Options: low-voltage MOSFET (IRF540, etc.) or IGBT

**Deadtime:** For 48V low-power, deadtime ≈ 200 ns (minimize shoot-through loss).

### Step 2: PWM Frequency Selection

Higher frequency → lower torque ripple, higher switching loss
Lower frequency → lower loss, more torque ripple

**Recommendation:** fsw = 20-40 kHz (balance between noise and efficiency)

For commutation, update only on Hall transitions (no PWM within sector).

### Step 3: Current Limiting

Set maximum phase current to protect switches and battery:

$$I_{max} = \frac{V_{dc}}{R_a} \times 0.8 = \frac{48V}{0.2Ω} \times 0.8 = 192A \text{ (theoretical)}$$

**Practical limit:** Imax = 50A (power module thermal limit).

Duty cycle limit:
$$D_{max} = \frac{I_{max} \times R_a}{V_{dc}} = \frac{50A \times 0.2Ω}{48V} = 0.21 = 21\%$$

### Step 4: Speed Measurement

**From Hall Edges:**
```
Hall pulse period = Time between edges
Sector time = Hall period × 6
Motor speed (RPM) = (60 / Sector_time_seconds) × (360 / 360°_mech)

For 6-pole motor (4 pole pairs):
ω_e = 2π × RPM / 60 = π × RPM / 30
```

**Low-Pass Filter Hall speed:**
Apply soft filter to reduce noise:
$$\omega_{filtered}(k) = 0.9 \times \omega_{filtered}(k-1) + 0.1 \times \omega_{measured}(k)$$

### Step 5: PI Speed Controller Tuning

For motor with J = 0.001 kg·m²:

$$K_p = 0.01, \quad K_i = 0.001$$

Adjust for stable response (avoid oscillation).

**Anti-windup:** Limit integral accumulation when duty cycle saturates.

## Losses and Efficiency

**Copper Loss (Joule Heating):**
$$P_{cu} = 3 \times I_{rms}^2 \times R_a$$

For 10A RMS phase current:
$$P_{cu} = 3 \times 10^2 \times 0.2 = 60W$$

**Iron Loss (core saturation):**
$$P_{iron} ≈ 20-40W \text{ (at rated speed)}$$

**Mechanical Loss (friction):**
$$P_{mech} ≈ 10-20W$$

**Total Loss:** Ptotal ≈ 90-120W
**Efficiency:** η ≈ 500W / (500 + 100)W ≈ **83%** (typical for this size)

Higher power units achieve 90-95% efficiency.

## Exercises

1. **Hall Sensor Decoding:** Verify Hall code to phase-mapping; toggle each phase manually
2. **Commutation Sequence:** Capture motor phase currents; verify six clean current steps
3. **Speed Control:** Implement PI speed loop, measure step response (ramp from 0 to 3000 RPM)
4. **Current Limiting:** Apply load step, observe current limiting and thermal protection
5. **Torque Ripple:** Measure instantaneous torque (via current × Kt); quantify ripple percentage
6. **Load Variation:** Test motor under light, medium, and full load; plot efficiency vs. load
7. **Sensorless Transition:** (Advanced) Implement back-EMF zero-crossing, test startup-to-closed-loop handoff
8. **Thermal Simulation:** Estimate winding temperature rise from copper loss; design cooling strategy

## Related Resources

- [PMSM Field-Oriented Control](pmsm-foc.md)
- [402 - Three-Phase Inverter](../../tutorials/dcac/three-phase.md)
