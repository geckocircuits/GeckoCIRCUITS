---
title: Tutorial 706: Python Integration
---

# Tutorial 706: Python Integration

## Overview

Control GeckoCIRCUITS simulations from Python for automated testing, parameter sweeps, optimization, and data analysis. Use Python's scientific computing ecosystem with your power electronics models.

**Level:** Intermediate (2/3)

**Duration:** 40-50 minutes

**Series:** Scripting & Automation

**Status:** Placeholder - Implementation depends on available APIs

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Connect Python to GeckoCIRCUITS
- [ ] Run simulations programmatically
- [ ] Perform parameter sweeps
- [ ] Analyze results with NumPy/Pandas
- [ ] Create optimization scripts

## Prerequisites

- Complete [Tutorial 104: Running Simulations](../../1xx_getting_started/104_running_simulations/)
- Python 3.8+ installed
- Familiarity with NumPy and Matplotlib

## Integration Methods

### Method 1: File-Based Interface

```
Python Script → Parameter File → GeckoCIRCUITS → Results File → Python Analysis
```

**Pros:** Simple, works with any version
**Cons:** Slower, requires file I/O

### Method 2: Command-Line Automation

```bash
# Run simulation from Python using subprocess
python run_gecko.py --circuit buck.ipes --params "L=100e-6,D=0.5"
```

### Method 3: RMI Bridge (via Py4J or JPype)

```
Python → Py4J/JPype → Java RMI → GeckoCIRCUITS
```

**Pros:** Full API access, real-time control
**Cons:** More complex setup

### Method 4: REST API (gecko-rest-api module)

```
Python → HTTP Requests → Spring Boot API → Gecko Simulation Core
```

**Pros:** Language agnostic, modern approach
**Cons:** Requires API server running

## Setup

### Required Python Packages

```bash
pip install numpy pandas matplotlib scipy
pip install py4j  # For RMI bridge (optional)
pip install requests  # For REST API (optional)
```

### Environment Configuration

```python
import os

# Set paths
GECKO_JAR = "path/to/gecko-1.0-jar-with-dependencies.jar"
CIRCUIT_DIR = "path/to/circuits"
RESULTS_DIR = "path/to/results"
```

## Method 1: File-Based Workflow

### Step 1: Create Parameter Template

```python
# generate_params.py
def create_param_file(params, filename):
    with open(filename, 'w') as f:
        for key, value in params.items():
            f.write(f"{key}={value}\n")

# Example
params = {
    'L': 100e-6,
    'C': 100e-6,
    'D': 0.5,
    'Vin': 48,
    'fs': 100e3
}
create_param_file(params, 'buck_params.txt')
```

### Step 2: Run Simulation

```python
import subprocess

def run_simulation(circuit_file, param_file=None):
    cmd = [
        'java', '-Xmx3G',
        '-Dpolyglot.js.nashorn-compat=true',
        '-jar', GECKO_JAR,
        circuit_file,
        '--batch',  # Non-interactive mode
    ]
    if param_file:
        cmd.extend(['--params', param_file])

    result = subprocess.run(cmd, capture_output=True)
    return result.returncode == 0
```

### Step 3: Read Results

```python
import pandas as pd

def read_results(results_file):
    df = pd.read_csv(results_file)
    return df

# Example
results = read_results('simulation_output.csv')
print(f"Vout mean: {results['Vout'].mean():.2f} V")
print(f"Vout ripple: {results['Vout'].max() - results['Vout'].min():.3f} V")
```

## Parameter Sweep Example

### Sweep Duty Cycle

```python
import numpy as np
import matplotlib.pyplot as plt

def sweep_duty_cycle(circuit, d_values):
    results = []

    for d in d_values:
        # Create parameter file
        params = {'D': d, 'L': 100e-6, 'C': 100e-6}
        create_param_file(params, 'temp_params.txt')

        # Run simulation
        run_simulation(circuit, 'temp_params.txt')

        # Read and store results
        df = read_results('output.csv')
        results.append({
            'D': d,
            'Vout_avg': df['Vout'].mean(),
            'Vout_ripple': df['Vout'].max() - df['Vout'].min(),
            'IL_avg': df['IL'].mean()
        })

    return pd.DataFrame(results)

# Run sweep
d_values = np.linspace(0.1, 0.9, 9)
sweep_results = sweep_duty_cycle('buck.ipes', d_values)

# Plot
plt.figure(figsize=(10, 4))
plt.subplot(1, 2, 1)
plt.plot(sweep_results['D'], sweep_results['Vout_avg'], 'o-')
plt.xlabel('Duty Cycle')
plt.ylabel('Output Voltage (V)')
plt.title('Vout vs D')
plt.grid(True)

plt.subplot(1, 2, 2)
plt.plot(sweep_results['D'], sweep_results['Vout_ripple']*1000, 'o-')
plt.xlabel('Duty Cycle')
plt.ylabel('Voltage Ripple (mV)')
plt.title('Ripple vs D')
plt.grid(True)

plt.tight_layout()
plt.savefig('sweep_results.png')
```

## Optimization Example

### Find Optimal L for Minimum Ripple

```python
from scipy.optimize import minimize_scalar

def objective(L):
    """Objective: minimize voltage ripple"""
    params = {'D': 0.5, 'L': L, 'C': 100e-6}
    create_param_file(params, 'temp_params.txt')
    run_simulation('buck.ipes', 'temp_params.txt')

    df = read_results('output.csv')
    ripple = df['Vout'].max() - df['Vout'].min()
    return ripple

# Optimize
result = minimize_scalar(objective, bounds=(10e-6, 1e-3), method='bounded')
print(f"Optimal L: {result.x*1e6:.1f} µH")
print(f"Minimum ripple: {result.fun*1000:.2f} mV")
```

## Data Analysis

### Statistical Analysis

```python
def analyze_waveform(signal, fs):
    """Compute statistics for a waveform"""
    return {
        'mean': np.mean(signal),
        'rms': np.sqrt(np.mean(signal**2)),
        'min': np.min(signal),
        'max': np.max(signal),
        'pp': np.max(signal) - np.min(signal),  # peak-to-peak
        'std': np.std(signal)
    }

# Example
stats = analyze_waveform(results['Vout'].values, fs=1e6)
print(f"Vout: {stats['mean']:.2f} V ± {stats['pp']*1000:.1f} mV")
```

### FFT Analysis

```python
from scipy.fft import fft, fftfreq

def compute_thd(signal, fs, fundamental_freq):
    """Compute Total Harmonic Distortion"""
    N = len(signal)
    yf = fft(signal)
    xf = fftfreq(N, 1/fs)

    # Find fundamental and harmonics
    fund_idx = np.argmin(np.abs(xf - fundamental_freq))
    fund_mag = np.abs(yf[fund_idx])

    # Sum of harmonics (2nd through 10th)
    harm_sum_sq = 0
    for h in range(2, 11):
        harm_idx = np.argmin(np.abs(xf - h*fundamental_freq))
        harm_sum_sq += np.abs(yf[harm_idx])**2

    thd = np.sqrt(harm_sum_sq) / fund_mag * 100
    return thd

# Example
thd = compute_thd(results['Vout'].values, fs=1e6, fundamental_freq=100e3)
print(f"THD: {thd:.2f}%")
```

## Method 3: Py4J Bridge (Advanced)

### Setup Py4J Gateway

```python
from py4j.java_gateway import JavaGateway

# Start GeckoCIRCUITS with Py4J gateway enabled
gateway = JavaGateway()

# Access Gecko API
gecko = gateway.entry_point
```

### Run Simulation via API

```python
# Load circuit
gecko.loadCircuit("buck.ipes")

# Set parameters
gecko.setParameter("L.1", "inductance", 100e-6)
gecko.setParameter("R.1", "resistance", 4.8)

# Run simulation
gecko.runSimulation(simulationTime=0.01)

# Get results
vout = gecko.getSignal("V.out")
iL = gecko.getSignal("I.L")
```

## Exercises

### Exercise 1: Basic Sweep
1. Create script to sweep Vin from 36V to 60V
2. Plot Vout vs Vin
3. Verify Vout = D × Vin relationship

### Exercise 2: 2D Parameter Sweep
1. Sweep both L and C
2. Create contour plot of ripple vs L and C
3. Find minimum ripple combination

### Exercise 3: Efficiency Optimization
1. Define efficiency = Pout / Pin
2. Optimize D for maximum efficiency
3. Plot efficiency curve

### Exercise 4: Monte Carlo Analysis
1. Add random variation to L (±10%)
2. Run 100 simulations
3. Plot histogram of Vout

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Subprocess hangs | GUI blocking | Use --batch flag |
| Can't read results | Wrong file path | Check output directory |
| Slow execution | Too many simulations | Use parallel processing |
| Memory error | Too many results | Process in batches |

## Parallel Processing

```python
from concurrent.futures import ProcessPoolExecutor

def run_single_sim(params):
    # Create unique temp files
    param_file = f"params_{params['id']}.txt"
    create_param_file(params, param_file)
    run_simulation('buck.ipes', param_file)
    return read_results(f"output_{params['id']}.csv")

# Parallel sweep
with ProcessPoolExecutor(max_workers=4) as executor:
    param_list = [{'id': i, 'D': d} for i, d in enumerate(d_values)]
    results = list(executor.map(run_single_sim, param_list))
```

## Related Tutorials

- [702 - MATLAB Integration](../702_matlab_integration/) - Alternative interface
- [701 - GeckoSCRIPT](../701_gecko_script_basics/) - Built-in scripting
- [105 - Analysis Tools](../../1xx_getting_started/105_analysis_tools/) - Frequency analysis

## References

1. NumPy Documentation: https://numpy.org/doc/
2. SciPy Optimization: https://docs.scipy.org/doc/scipy/reference/optimize.html
3. Py4J Documentation: https://www.py4j.org/

## Script Files

> **Status:** Placeholder
> - `gecko_interface.py` - Base interface class
> - `parameter_sweep.py` - Sweep automation
> - `optimization_example.py` - SciPy optimization
> - `analysis_utils.py` - Data analysis functions

---
*Tutorial Version: 1.0 (Placeholder)*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
