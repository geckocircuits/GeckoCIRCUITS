---
title: "706 - Python Integration"
---

# 706 - Python Integration

Python automation for GeckoCIRCUITS simulations.

## Overview

Python integration enables:
- NumPy/SciPy for data analysis
- Pandas for results management
- Matplotlib for visualization
- Optimization algorithms (scipy.optimize)
- Machine learning workflows

## Integration Methods

### Method 1: REST API

GeckoCIRCUITS REST API (gecko-rest-api module):

```python
import requests

BASE_URL = "http://localhost:8080/api"

# Load circuit
response = requests.post(f"{BASE_URL}/circuit/load",
                        json={"path": "buck_converter.ipes"})

# Set parameter
requests.post(f"{BASE_URL}/circuit/parameter",
             json={"name": "R_load", "value": 10.0})

# Run simulation
requests.post(f"{BASE_URL}/simulation/run",
             json={"duration": 0.01, "timestep": 1e-7})

# Get results
results = requests.get(f"{BASE_URL}/simulation/results").json()
```

### Method 2: JPype (Java Bridge)

Direct Java interface:

```python
import jpype
import jpype.imports

# Start JVM with GeckoCIRCUITS
jpype.startJVM(classpath=['gecko.jar'])

from ch.technokrat.gecko import GeckoRemoteInterface

# Connect to running instance
gecko = GeckoRemoteInterface.getInstance()
```

### Method 3: Subprocess Control

For simpler use cases:

```python
import subprocess
import json

# Run simulation with parameters
result = subprocess.run([
    'java', '-jar', 'gecko.jar',
    '--batch', 'circuit.ipes',
    '--param', 'R_load=10',
    '--output', 'results.json'
], capture_output=True)

# Load results
with open('results.json') as f:
    data = json.load(f)
```

## Python Wrapper Class

```python
import requests
import numpy as np
import pandas as pd

class GeckoCIRCUITS:
    def __init__(self, base_url="http://localhost:8080/api"):
        self.base_url = base_url

    def load_circuit(self, path):
        """Load circuit file."""
        response = requests.post(
            f"{self.base_url}/circuit/load",
            json={"path": path}
        )
        return response.json()

    def set_parameter(self, name, value):
        """Set component parameter."""
        requests.post(
            f"{self.base_url}/circuit/parameter",
            json={"name": name, "value": value}
        )

    def run_simulation(self, duration, timestep=1e-7):
        """Run simulation and wait for completion."""
        requests.post(
            f"{self.base_url}/simulation/run",
            json={"duration": duration, "timestep": timestep}
        )

    def get_measurement(self, scope, channel, measurement):
        """Get scope measurement (mean, rms, max, min, pp)."""
        response = requests.get(
            f"{self.base_url}/simulation/measurement",
            params={"scope": scope, "channel": channel, "type": measurement}
        )
        return response.json()["value"]

    def get_waveform(self, scope, channel):
        """Get waveform data as numpy arrays."""
        response = requests.get(
            f"{self.base_url}/simulation/waveform",
            params={"scope": scope, "channel": channel}
        )
        data = response.json()
        return np.array(data["time"]), np.array(data["values"])
```

## Parameter Sweeps

### Single Sweep

```python
import matplotlib.pyplot as plt

gecko = GeckoCIRCUITS()
gecko.load_circuit("buck_converter.ipes")

duties = np.linspace(0.2, 0.8, 13)
vout = []
efficiency = []

for d in duties:
    gecko.set_parameter("PWM1.dutyCycle", d)
    gecko.run_simulation(0.01)

    vout.append(gecko.get_measurement("SCOPE", "Vout", "mean"))
    pin = gecko.get_measurement("SCOPE", "Pin", "mean")
    pout = gecko.get_measurement("SCOPE", "Pout", "mean")
    efficiency.append(pout / pin * 100)

# Plot results
fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(10, 8))

ax1.plot(duties, vout, 'b-o')
ax1.set_xlabel('Duty Cycle')
ax1.set_ylabel('Output Voltage (V)')
ax1.grid(True)

ax2.plot(duties, efficiency, 'r-o')
ax2.set_xlabel('Duty Cycle')
ax2.set_ylabel('Efficiency (%)')
ax2.grid(True)

plt.tight_layout()
plt.savefig('sweep_results.png')
```

### Multi-Parameter Sweep

```python
from itertools import product
import pandas as pd

# Parameter ranges
fsw_values = [50e3, 100e3, 200e3]
L_values = [22e-6, 47e-6, 100e-6]
load_values = [5, 10, 20]

results = []

for fsw, L, load in product(fsw_values, L_values, load_values):
    gecko.set_parameter("PWM1.frequency", fsw)
    gecko.set_parameter("L1.inductance", L)
    gecko.set_parameter("R_load", load)

    gecko.run_simulation(0.01)

    results.append({
        'fsw': fsw,
        'L': L,
        'load': load,
        'vout': gecko.get_measurement("SCOPE", "Vout", "mean"),
        'ripple': gecko.get_measurement("SCOPE", "IL", "pp"),
        'efficiency': gecko.get_measurement("SCOPE", "efficiency", "mean")
    })

# Create DataFrame
df = pd.DataFrame(results)
df.to_csv('parameter_sweep.csv', index=False)

# Analyze results
print(df.groupby('fsw')['efficiency'].mean())
```

## Optimization

### SciPy Optimization

```python
from scipy.optimize import minimize

def efficiency_objective(params):
    """Objective function to maximize efficiency."""
    L, C, fsw = params

    gecko.set_parameter("L1.inductance", L)
    gecko.set_parameter("C1.capacitance", C)
    gecko.set_parameter("PWM1.frequency", fsw)

    gecko.run_simulation(0.01)

    efficiency = gecko.get_measurement("SCOPE", "efficiency", "mean")

    # Minimize negative efficiency (maximize efficiency)
    return -efficiency

# Initial guess
x0 = [47e-6, 100e-6, 100e3]

# Bounds
bounds = [
    (10e-6, 200e-6),   # L: 10-200 µH
    (22e-6, 470e-6),   # C: 22-470 µF
    (50e3, 500e3)      # fsw: 50-500 kHz
]

# Optimize
result = minimize(efficiency_objective, x0, bounds=bounds, method='L-BFGS-B')

print(f"Optimal L: {result.x[0]*1e6:.1f} µH")
print(f"Optimal C: {result.x[1]*1e6:.1f} µF")
print(f"Optimal fsw: {result.x[2]/1e3:.0f} kHz")
print(f"Max efficiency: {-result.fun*100:.2f}%")
```

## Data Analysis

### FFT Analysis

```python
from scipy.fft import fft, fftfreq

time, voltage = gecko.get_waveform("SCOPE", "Vout")

# Compute FFT
N = len(time)
dt = time[1] - time[0]
yf = fft(voltage)
xf = fftfreq(N, dt)

# Plot spectrum
plt.figure(figsize=(10, 6))
plt.semilogy(xf[:N//2]/1e3, np.abs(yf[:N//2]))
plt.xlabel('Frequency (kHz)')
plt.ylabel('Magnitude')
plt.title('Output Voltage Spectrum')
plt.grid(True)
plt.savefig('spectrum.png')
```

## Best Practices

1. **Use context managers** for connection handling
2. **Batch API calls** to reduce latency
3. **Cache results** to avoid re-running simulations
4. **Use parallel processing** for independent sweeps:
   ```python
   from concurrent.futures import ProcessPoolExecutor
   ```

## Related Resources

- [701 - GeckoSCRIPT](geckoscript.md)
- [702 - MATLAB Integration](matlab.md)
