# Tutorial 105: Analysis Tools

## Overview

Learn to use GeckoCIRCUITS analysis tools for steady-state analysis, frequency response (Bode plots), and small-signal characterization of power converters.

**Level:** Intermediate (2/3)

**Duration:** 40-50 minutes

**Series:** Getting Started

**Status:** Placeholder - Features may vary by GeckoCIRCUITS version

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Perform steady-state (periodic) analysis
- [ ] Generate Bode plots for control loop design
- [ ] Measure transfer functions (input-to-output, control-to-output)
- [ ] Analyze stability margins (gain margin, phase margin)

## Prerequisites

- Complete [Tutorial 104: Running Simulations](../104_running_simulations/)
- Understanding of frequency response concepts
- Basic control theory knowledge

## Analysis Types

### 1. Transient Analysis (Standard)

- Time-domain simulation
- Shows startup, transients, and steady-state
- Most common analysis type

### 2. Steady-State Analysis

- Finds periodic operating point directly
- Skips startup transient
- Faster for efficiency calculations

### 3. AC (Small-Signal) Analysis

- Linearizes circuit around operating point
- Generates frequency response (Bode plots)
- Essential for control loop design

## Steady-State Analysis

### Purpose

Find the periodic steady-state without simulating startup:
- Efficiency calculations at operating point
- Waveform analysis without transients
- Faster than long transient simulation

### Procedure

1. **Setup:** Define fundamental frequency (switching frequency or line frequency)
2. **Run:** Solver iterates to find periodic solution
3. **Verify:** Check that waveforms repeat exactly each period

### Example: Buck Converter Efficiency

1. Build buck converter circuit
2. Enable steady-state analysis
3. Measure: Pin, Pout, losses
4. Calculate: η = Pout / Pin

## AC (Frequency Response) Analysis

### Theory

Linearize non-linear switching converter around operating point:

```
     ┌─────────────────────┐
d̂ ──►│  Converter          │──► v̂out
     │  (linearized model) │
     └─────────────────────┘

Transfer function: Gvd(s) = v̂out(s) / d̂(s)
```

### Key Transfer Functions

| Transfer Function | Symbol | Description |
|-------------------|--------|-------------|
| Control-to-output | Gvd(s) | Duty cycle to output voltage |
| Line-to-output | Gvg(s) | Input voltage to output voltage |
| Input impedance | Zin(s) | Small-signal input impedance |
| Output impedance | Zout(s) | Small-signal output impedance |
| Loop gain | T(s) | Open-loop transfer function |

### Bode Plot Interpretation

```
    Gain (dB)                    Phase (deg)
    │                            │ 0°
 40 ┤                            ├─────────────────
    │ ──────┐                    │         ╲
 20 ┤       │                    │          ╲
    │       ╲                    │           ╲
  0 ┤        ╲─────              ├────────────╲────
    │                            │ -180°      ╲
-20 ┤                            │             ╲
    └────────────────── f        └──────────────── f
         fc (crossover)               PM (phase margin)
```

### Stability Margins

| Margin | Definition | Typical Target |
|--------|------------|----------------|
| Gain Margin (GM) | Gain at -180° phase | > 10 dB |
| Phase Margin (PM) | Phase at 0 dB gain | > 45° |
| Crossover Frequency (fc) | Frequency where gain = 0 dB | fs/5 to fs/10 |

## Analysis Workflow

### Step 1: Establish Operating Point

1. Run transient simulation to steady state
2. Or use steady-state analysis directly
3. Record DC values: Vout, IL, D

### Step 2: Inject Small-Signal Perturbation

1. Add small AC component to control signal
2. Frequency: sweep from low to high (e.g., 10 Hz to fs/2)
3. Amplitude: small enough for linear response (1-5%)

### Step 3: Measure Response

1. Record output AC magnitude and phase
2. At each frequency, calculate:
   - Gain: |Vout_ac| / |d_ac|
   - Phase: ∠Vout - ∠d

### Step 4: Plot Results

1. Magnitude in dB: 20×log10(|G|)
2. Phase in degrees
3. Identify: crossover, margins, resonances

## Buck Converter Example

### Open-Loop Transfer Function

Control-to-output for buck:
```
Gvd(s) = Vin × (1 + s×RC×C) / [1 + s×L/R + s²×L×C]
```

Characteristic:
- DC gain: Vin
- LC resonance: fr = 1/(2π√LC)
- ESR zero: fz = 1/(2π×RC×C)

### Closed-Loop Design

1. **Measure open-loop Gvd:** Run AC analysis
2. **Design compensator:** PI, Type II, or Type III
3. **Verify loop gain T(s):** Gc × Gvd × H
4. **Check margins:** PM > 45°, GM > 10 dB

## Exercises

### Exercise 1: Steady-State Efficiency
1. Build 48V→12V buck converter
2. Run steady-state analysis at full load
3. Calculate efficiency from average powers

### Exercise 2: Open-Loop Bode Plot
1. Use the buck converter from Exercise 1
2. Perform AC analysis (no feedback)
3. Identify: DC gain, resonance frequency, phase

### Exercise 3: Loop Gain with PI Controller
1. Add PI compensator to the buck
2. Measure loop gain T(s) = Gc × Gvd
3. Determine phase margin and crossover frequency

### Exercise 4: Design for 45° Phase Margin
1. Adjust PI gains to achieve PM = 45°
2. Verify with Bode plot
3. Test step response in time domain

## Manual AC Analysis Method

If automated AC analysis is not available:

### Frequency Sweep Procedure

1. Set up transient simulation
2. Add sinusoidal perturbation to duty cycle:
   ```
   d(t) = D0 + Δd × sin(2πf×t)
   ```
3. Run simulation for several cycles at frequency f
4. Measure output amplitude and phase
5. Repeat for multiple frequencies (log spacing)
6. Plot magnitude and phase vs. frequency

### MATLAB Post-Processing

```matlab
% Load exported data
data = readmatrix('sweep_results.csv');
f = data(:,1);
gain_dB = data(:,2);
phase_deg = data(:,3);

% Bode plot
figure;
subplot(2,1,1);
semilogx(f, gain_dB);
ylabel('Gain (dB)');
grid on;

subplot(2,1,2);
semilogx(f, phase_deg);
ylabel('Phase (deg)');
xlabel('Frequency (Hz)');
grid on;
```

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Noisy Bode plot | Perturbation too small | Increase AC amplitude |
| Non-linear response | Perturbation too large | Decrease AC amplitude |
| Aliasing at high freq | Not enough samples | Reduce time step |
| Can't find steady state | Circuit doesn't converge | Check for stability |

## Related Tutorials

- [104 - Running Simulations](../104_running_simulations/) - Basic simulation setup
- [201 - Buck Converter](../../2xx_dcdc_converters/201_buck_converter/) - Test circuit
- [702 - MATLAB Integration](../../7xx_scripting_automation/702_matlab_integration/) - Data export/analysis

## References

1. Erickson & Maksimovic, Chapter 8: "Converter Transfer Functions"
2. Basso, C. "Switch-Mode Power Supply SPICE Cookbook"
3. Venable, D. "The K Factor: A New Mathematical Tool for Stability Analysis"

## Circuit Files

> **Status:** Placeholder
> - `buck_ac_analysis.ipes` - Buck for frequency analysis
> - `boost_loop_gain.ipes` - Boost with compensator
> - `frequency_sweep.ipes` - Manual sweep setup

---
*Tutorial Version: 1.0 (Placeholder)*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
