# Tutorial 104: Running Simulations

## Overview

Master the simulation settings in GeckoCIRCUITS. Learn about solver options, time step configuration, convergence issues, and how to optimize simulations for accuracy and speed.

**Level:** Beginner/Intermediate (2/3)

**Duration:** 25-30 minutes

**Series:** Getting Started

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Configure simulation time and time step
- [ ] Understand different solver algorithms
- [ ] Diagnose and fix convergence problems
- [ ] Optimize simulations for speed vs. accuracy
- [ ] Use the oscilloscope (SCOPE) effectively
- [ ] Export simulation data

## Prerequisites

- Complete [Tutorial 101-103](../101_first_simulation/)
- Basic understanding of differential equations (helpful but not required)

## Simulation Settings Overview

Access settings via **Simulation > Settings** or press **F9**.

### Main Parameters

| Parameter | Description | Typical Values |
|-----------|-------------|----------------|
| Simulation Time (Tend) | Total duration to simulate | 1ms - 1s |
| Time Step (dt) | Integration step size | 0.1μs - 10μs |
| Output Interval | Data storage frequency | 1-100 points/period |
| Solver | Numerical integration method | TRZ, BE, GS |

## Time Step Configuration

### Automatic vs. Fixed Time Step

**Automatic (Recommended for beginners):**
- GeckoCIRCUITS adjusts dt based on circuit dynamics
- Smaller dt during fast transients, larger during steady-state
- Good balance of speed and accuracy

**Fixed Time Step:**
- You specify exact dt value
- Useful for: deterministic timing, FFT analysis, comparison studies
- Risk: too large → inaccuracy, too small → slow simulation

### Choosing Time Step

Rule of thumb:
```
dt < Ts / 100    (switching period / 100)
```

Example: fs = 100 kHz → Ts = 10 μs → dt < 100 ns

| Circuit Type | Recommended dt |
|--------------|----------------|
| 50/60 Hz AC circuits | 10-100 μs |
| 10 kHz switching | 100 ns - 1 μs |
| 100 kHz switching | 10-100 ns |
| 1 MHz switching | 1-10 ns |

### Time Step Too Large

Symptoms:
- Waveforms look choppy/aliased
- Incorrect average values
- Missing fast transients
- Simulation completes but results are wrong

### Time Step Too Small

Symptoms:
- Simulation runs very slowly
- Excessive data storage
- No improvement in accuracy (diminishing returns)

## Solver Selection

GeckoCIRCUITS offers multiple numerical solvers:

### Trapezoidal (TRZ) - Default

```
x(t+dt) = x(t) + dt/2 × [f(t) + f(t+dt)]
```

- **Accuracy:** Second-order, no numerical damping
- **Stability:** A-stable for linear circuits
- **Best for:** Most power electronics circuits
- **Issue:** May show ringing on discontinuities

### Backward Euler (BE)

```
x(t+dt) = x(t) + dt × f(t+dt)
```

- **Accuracy:** First-order, introduces numerical damping
- **Stability:** Very stable, L-stable
- **Best for:** Stiff circuits, initial debugging
- **Issue:** Over-damps high-frequency oscillations

### Gear-Shichman (GS)

- **Accuracy:** Higher-order, multi-step method
- **Best for:** Circuits with multiple time scales
- **Issue:** More complex, may have startup transients

### Solver Comparison

| Characteristic | TRZ | BE | GS |
|---------------|-----|----|----|
| Accuracy | High | Medium | High |
| Damping | None | High | Low |
| Stability | Good | Excellent | Good |
| Speed | Fast | Fast | Medium |
| Memory | Low | Low | Higher |

## Simulation Workflow

### Step 1: Initial Setup

1. Set simulation time: 5-10 fundamental periods
2. Start with automatic time step
3. Use TRZ solver
4. Run simulation

### Step 2: Check Results

1. View waveforms in SCOPE
2. Verify expected behavior
3. Look for: oscillations, convergence issues, unrealistic values

### Step 3: Refine Settings

If results look wrong:
- Try smaller time step
- Switch to BE solver temporarily
- Check circuit for errors

If simulation is too slow:
- Increase time step (if accuracy allows)
- Reduce simulation time
- Simplify circuit model

## Oscilloscope (SCOPE) Usage

### Adding Signals

1. Connect circuit nodes to SCOPE inputs
2. Use current probes for current measurement
3. Multiple channels available (different colors)

### Measurement Tools

| Tool | Function | Access |
|------|----------|--------|
| Cursors | Measure values at specific times | Click + drag on waveform |
| Auto-scale | Fit all signals in view | Toolbar button |
| Zoom | Magnify time/amplitude | Scroll wheel, toolbar |
| Pan | Move view | Middle-click + drag |
| FFT | Frequency spectrum | View > FFT |

### SCOPE Settings

1. Double-click SCOPE component
2. Configure:
   - **Channels:** Enable/disable, colors
   - **Scale:** Manual or auto
   - **Trigger:** Level, edge, mode
   - **Time base:** Samples per division

## Data Export

### Exporting Waveforms

1. **File > Export Data** after simulation
2. Choose format:
   - **CSV:** For Excel, MATLAB, Python
   - **Text:** Tab-separated values
3. Select signals to export

### Export Format

```csv
Time,Channel1,Channel2,Channel3
0.0000,0.00,10.00,0.00
0.0001,5.23,10.00,0.52
0.0002,9.87,10.00,0.99
...
```

### Using Exported Data

**MATLAB:**
```matlab
data = readmatrix('simulation_data.csv');
t = data(:,1);
v = data(:,2);
plot(t, v);
```

**Python:**
```python
import pandas as pd
import matplotlib.pyplot as plt

data = pd.read_csv('simulation_data.csv')
plt.plot(data['Time'], data['Channel1'])
plt.show()
```

## Troubleshooting

### Simulation Won't Start

| Cause | Solution |
|-------|----------|
| Floating node | Add ground reference |
| Voltage source loop | Add small resistance |
| Current source open | Add parallel resistance |
| Invalid parameters | Check component values |

### Simulation Diverges

| Cause | Solution |
|-------|----------|
| Time step too large | Reduce dt |
| Stiff circuit | Use BE solver |
| Unrealistic parameters | Check component values |
| Algebraic loop | Restructure circuit |

### Slow Simulation

| Cause | Solution |
|-------|----------|
| Time step too small | Increase dt |
| Long simulation time | Reduce Tend |
| Complex circuit | Simplify model |
| Many SCOPE points | Reduce output interval |

### Oscillations in Results

| Type | Cause | Solution |
|------|-------|----------|
| Physical | LC resonance | Expected behavior |
| Numerical | TRZ ringing | Use BE solver or smaller dt |
| Control | Unstable feedback | Check controller gains |

## Advanced Settings

### Initial Conditions

For faster convergence:
1. Set initial capacitor voltages
2. Set initial inductor currents
3. Avoids long startup transients

### Steady-State Detection

GeckoCIRCUITS can detect when circuit reaches steady state:
1. Enable in Simulation > Settings
2. Simulation stops when periodic steady state achieved
3. Useful for efficiency calculations

### Parametric Sweeps

Run multiple simulations with varying parameters:
1. Use GeckoSCRIPT or MATLAB integration
2. Sweep: duty cycle, load, frequency
3. Automatically collect results

## Checkpoint

At this point, you should be able to:
- [ ] Configure simulation time and time step appropriately
- [ ] Choose the right solver for your circuit
- [ ] Diagnose and fix common simulation issues
- [ ] Use SCOPE measurement tools effectively
- [ ] Export data for external analysis

## Exercises

### Exercise 1: Time Step Sensitivity

1. Open a buck converter circuit
2. Run with dt = 1 μs, record output voltage
3. Run with dt = 100 ns, record output voltage
4. Run with dt = 10 ns, record output voltage
5. **Compare:** At what dt does accuracy stabilize?

### Exercise 2: Solver Comparison

1. Create an LC circuit (L=1mH, C=1μF)
2. Run with TRZ solver, observe ringing
3. Run with BE solver, observe damping
4. **Question:** Which is more physically accurate?

### Exercise 3: Data Export

1. Run any simulation
2. Export data as CSV
3. Plot in Excel/MATLAB/Python
4. Calculate: average, RMS, ripple

### Exercise 4: Convergence Debugging

1. Create a circuit that diverges (e.g., very large L, very small C)
2. Try to fix it using:
   - Smaller time step
   - BE solver
   - Adding damping resistor
3. **Document:** What worked?

## Summary

In this tutorial, you learned:
1. How simulation time and time step affect results
2. Different solver algorithms and when to use them
3. Troubleshooting common simulation problems
4. Using SCOPE for measurements
5. Exporting data for external analysis

## Quick Reference

| Setting | Rule of Thumb |
|---------|---------------|
| Simulation time | 5-10 × longest time constant |
| Time step | < Switching period / 100 |
| Solver | TRZ default, BE for stiff circuits |
| Output points | 100-1000 points per period |

## Next Steps

You've completed the Getting Started series! Continue with:
- **DC-DC Converters:** [2xx Series](../../2xx_dcdc_converters/)
- **Rectifiers:** [3xx Series](../../3xx_acdc_rectifiers/)
- **Examples:** [Basic Topologies](../../examples/basic_topologies/)

---
*Tutorial Version: 1.0*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
