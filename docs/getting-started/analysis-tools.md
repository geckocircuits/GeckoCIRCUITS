---
title: Analysis Tools
description: Measuring, analyzing, and exporting simulation results
---

# Analysis Tools

How to measure, analyze, and export simulation results in GeckoCIRCUITS.

**Duration:** 15 minutes

**Prerequisites:** [Running Simulations](running-simulations.md)

## The SCOPE (Oscilloscope)

The SCOPE block is the primary tool for viewing simulation results.

### Opening the Scope

Double-click any SCOPE block in the schematic to open the waveform viewer.

### Scope Channels

Each SCOPE input terminal corresponds to a channel. Channels display:

- **Voltage signals** from voltage probes
- **Current signals** from current probes
- **Control signals** from control blocks

### Navigation

| Action | How |
|--------|-----|
| Zoom in | Scroll wheel up |
| Zoom out | Scroll wheel down |
| Pan | Click + drag |
| Auto-scale | Right-click > Auto Scale |
| Reset view | Right-click > Reset |

## Measurement Tools

### Cursors

Use cursors for precise measurements:

1. Right-click in the scope > **Add Cursor**
2. Drag cursor to the point of interest
3. Read the time and value display

### Using Two Cursors

With two cursors, you can measure:

- **Time difference** (delta t) between two points
- **Value difference** (delta V or delta I)
- **Frequency** = 1 / delta t

### Average and RMS

For periodic signals, the scope can compute:

| Measurement | Formula | Use For |
|-------------|---------|---------|
| **Average** | (1/T) x integral of v(t) dt | DC component |
| **RMS** | sqrt((1/T) x integral of v^2 dt) | Power calculations |
| **Peak** | max(v(t)) | Component stress |
| **Peak-to-peak** | max - min | Ripple measurement |

## Key Measurements in Power Electronics

### Ripple

Voltage or current ripple is the AC component on top of the DC:

$$\Delta V = V_{max} - V_{min}$$

$$\text{Ripple (\%)} = \frac{\Delta V}{V_{avg}} \times 100$$

### Efficiency

$$\eta = \frac{P_{out}}{P_{in}} = \frac{V_{out} \times I_{out}}{V_{in} \times I_{in}}$$

Measure input and output power using voltage and current probes.

### Power Factor

For AC circuits:

$$PF = \frac{P_{real}}{P_{apparent}} = \frac{P}{V_{rms} \times I_{rms}}$$

### THD (Total Harmonic Distortion)

$$THD = \frac{\sqrt{V_2^2 + V_3^2 + V_4^2 + ...}}{V_1}$$

Use the FFT view in the scope for harmonic analysis.

## FFT Analysis

The scope includes a frequency-domain view using Fast Fourier Transform:

### Viewing the Spectrum

1. Run simulation to steady state
2. Open scope
3. Switch to FFT view (if available)
4. Observe harmonic content

### What to Look For

| Feature | Indicates |
|---------|-----------|
| Fundamental peak | Switching frequency |
| Harmonics | Integer multiples of f_sw |
| Low-frequency content | Control loop dynamics |
| Broadband noise | EMI concerns |

## Exporting Data

### Export to CSV

From the scope:

1. Right-click > **Export Data**
2. Choose CSV format
3. Select channels to export
4. Save file

### CSV Format

```csv
Time,Channel1,Channel2,Channel3
0.000000,0.0,0.0,0.0
0.000001,12.1,0.5,48.0
0.000002,12.2,0.6,0.0
```

### Post-Processing

Exported data can be analyzed in:

- **MATLAB/Octave** - `data = readmatrix('output.csv');`
- **Python** - `data = numpy.loadtxt('output.csv', delimiter=',')`
- **Excel** - Open directly as CSV

## Thermal Measurements

When thermal simulation is enabled:

| Measurement | Unit | Description |
|-------------|------|-------------|
| Junction temperature | C | Semiconductor die temperature |
| Switching losses | W | Energy lost during transitions |
| Conduction losses | W | I^2 x R losses during ON state |
| Total losses | W | Switching + conduction |

See [Thermal Simulation Tutorials](../tutorials/thermal/index.md) for details.

## Tips for Good Analysis

1. **Wait for steady state** - Don't measure during startup transients
2. **Use enough cycles** - Average over multiple switching periods
3. **Check time step** - Too large a step hides high-frequency content
4. **Compare with theory** - Verify results match analytical predictions
5. **Document settings** - Record simulation parameters with results

## Next Steps

- [Tutorials](../tutorials/index.md) - Apply analysis to specific topologies
- [GeckoSCRIPT](../tutorials/scripting/geckoscript.md) - Automate measurements
- [Examples](../examples/index.md) - Browse analyzed example circuits
