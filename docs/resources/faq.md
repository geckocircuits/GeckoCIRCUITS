---
title: FAQ
---

# Frequently Asked Questions

## General

### What is GeckoCIRCUITS?

GeckoCIRCUITS is an open-source power electronics circuit simulator written in Java. It supports multi-domain simulation including electrical, thermal, and EMI analysis. Originally developed at ETH Zurich, it's used in education and research.

### What Java version do I need?

**Java 21** or later. We recommend [Eclipse Temurin](https://adoptium.net/).

### Which operating systems are supported?

GeckoCIRCUITS runs on **Windows**, **Linux**, **macOS**, and **WSL** (Windows Subsystem for Linux). Platform-specific launcher scripts are provided in `scripts/`.

### Is it really free?

Yes. GeckoCIRCUITS is dual-licensed under GPL v3 (free for open-source/academic use) and a commercial license for proprietary applications.

## Simulation

### What solver does GeckoCIRCUITS use?

GeckoCIRCUITS uses **Modified Nodal Analysis (MNA)** with fixed time-step solvers:

- **Backward Euler (BE)** - Default, most stable
- **Trapezoidal (TRZ)** - Better accuracy, can oscillate
- **Gear-Shichman (GS)** - Multi-step method

### How do I choose the right time step?

Rule of thumb: the time step should be at least **100x smaller** than the switching period.

| Switching Frequency | Recommended Time Step |
|--------------------|-----------------------|
| 10 kHz | < 1 us |
| 100 kHz | < 100 ns |
| 1 MHz | < 10 ns |

### Why is my simulation unstable?

Common causes:

1. **Time step too large** - Reduce it (see table above)
2. **Algebraic loops** - Add a small series resistance or inductance
3. **Ideal switches** - Add snubber circuits or use realistic switch models
4. **Stiff system** - Try the Backward Euler solver instead of Trapezoidal

### Can I simulate thermal effects?

Yes. GeckoCIRCUITS supports coupled electro-thermal simulation:

1. Enable thermal nodes on semiconductor components
2. Define thermal resistance networks (Rth, Cth)
3. The simulator solves electrical and thermal equations simultaneously

See [Thermal Simulation Tutorials](../tutorials/thermal/index.md).

### What's the maximum circuit size?

There's no hard limit, but practical limits depend on your system RAM:

- **1000+ components** - Typical desktop (8GB RAM)
- **Complex thermal networks** - May need `-Xmx3G` or more
- **Very fast switching (>1MHz)** - Long simulations will be slow with small time steps

## Integration

### Can I use GeckoCIRCUITS with MATLAB?

Yes, via the **RMI (Remote Method Invocation)** interface:

```matlab
gecko = GeckoRemoteInterface('localhost', 1099);
gecko.setParameter('PWM.1', 'dutyCycle', 0.5);
gecko.runSimulation();
```

See [MATLAB Integration Tutorial](../tutorials/scripting/matlab.md).

### Can I use it with Python?

Yes, through:

- **File-based** - Run simulations via subprocess, parse output files
- **REST API** - The `gecko-rest-api` module provides HTTP endpoints
- **GeckoSCRIPT** - Built-in scripting (JavaScript-like syntax)

See [Python Integration Tutorial](../tutorials/scripting/python.md).

### Can I use it with Simulink?

Yes. GeckoCIRCUITS provides an S-function for Simulink co-simulation. See the tutorial in `resources/tutorials/7xx_scripting_automation/703_simulink_cosimulation/`.

## File Formats

### What is an .ipes file?

The native GeckoCIRCUITS circuit file format. It's **gzip-compressed** text containing component data, connections, and simulation parameters.

### Can I import SPICE netlists?

Not directly. GeckoCIRCUITS uses its own circuit format. You'll need to rebuild the circuit in the GeckoCIRCUITS editor.

### Can I export simulation data?

Yes. Use the SCOPE component's export function to save waveform data as CSV files for analysis in MATLAB, Python, or Excel.

## Troubleshooting

### The application won't start

See [Troubleshooting](troubleshooting.md) for common startup issues.

### Where do I report bugs?

Open an issue on [GitHub](https://github.com/tinix84/GeckoCIRCUITS/issues) using the bug report template.

### How do I get help?

- [GitHub Issues](https://github.com/tinix84/GeckoCIRCUITS/issues) - Bug reports and feature requests
- [GitHub Discussions](https://github.com/tinix84/GeckoCIRCUITS/discussions) - Questions and community help
- [Tutorials](../tutorials/index.md) - Step-by-step learning guides
