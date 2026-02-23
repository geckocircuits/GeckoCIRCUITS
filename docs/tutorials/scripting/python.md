---
title: Python Integration
description: Automate GeckoCIRCUITS from Python using multiple integration methods
---

# 706 - Python Integration

Automate GeckoCIRCUITS simulations from Python and integrate with the scientific Python ecosystem. Multiple integration methods support different use cases—from simple subprocess automation to high-performance API communication.

## Overview

Python integration enables:

- **Subprocess Control** - Run GeckoCIRCUITS from Python scripts, parse output
- **Parameter Sweeps** - Use NumPy/SciPy for design space exploration
- **Data Analysis** - Pandas, NumPy, Matplotlib for post-processing
- **Optimization** - SciPy.optimize for automated design optimization
- **REST API** (planned) - Future high-performance server-based integration

!!! info "Real-World Example"
    See `resources/validate_circuits.py` for a production example that runs batch circuit simulations and validates results.

## Integration Methods

### Method 1: Subprocess Control (Recommended for Simple Use)

Run GeckoCIRCUITS as an external process and communicate via files or stdout:

```python
import subprocess
import json
import tempfile

def run_simulation(circuit_file, parameters):
    """Run GeckoCIRCUITS simulation from Python."""

    # Build command with parameters
    cmd = [
        'java', '-Xmx3G', '-jar', 'gecko-1.0-jar-with-dependencies.jar',
        '--headless',
        '--circuit', circuit_file,
        '--output', 'results.json'
    ]

    # Add parameters
    for name, value in parameters.items():
        cmd.extend(['--param', f'{name}={value}'])

    # Run simulation
    result = subprocess.run(cmd, capture_output=True, text=True)

    if result.returncode != 0:
        raise RuntimeError(f"Simulation failed: {result.stderr}")

    # Parse results
    with open('results.json') as f:
        return json.load(f)

# Usage
results = run_simulation('buck_converter.ipes', {
    'R_load': 10.0,
    'PWM1.frequency': 100e3
})
```

!!! tip "Headless Mode"
    Use `--headless` flag to run without GUI overhead, or `--remote` mode for persistent sessions across multiple simulations.

### Method 2: File-Based Parameter Modification

Directly modify circuit .ipes files (gzip-compressed XML):

```python
import zipfile
import xml.etree.ElementTree as ET
import gzip
import shutil

def modify_circuit_param(ipes_file, component, param, value):
    """Modify parameter in .ipes file and save as new circuit."""

    # Extract gzip
    with gzip.open(ipes_file, 'rb') as f_in:
        xml_content = f_in.read()

    # Parse XML
    root = ET.fromstring(xml_content)

    # Find and modify component parameter
    for comp in root.findall('.//component[@name="' + component + '"]'):
        param_elem = comp.find(f".//parameter[@name='{param}']")
        if param_elem is not None:
            param_elem.set('value', str(value))

    # Save modified circuit
    output_file = f'modified_{ipes_file}'
    new_xml = ET.tostring(root)

    with gzip.open(output_file, 'wb') as f_out:
        f_out.write(new_xml)

    return output_file
```

### Method 3: REST API (Future)

Planned high-performance integration via Spring Boot REST server:

```python
import requests

# Future API endpoint
BASE_URL = "http://localhost:8080/api"

response = requests.post(f"{BASE_URL}/simulation/run", json={
    "circuit": "buck_converter.ipes",
    "parameters": {"R_load": 10.0},
    "duration": 0.01,
    "timestep": 1e-7
})

results = response.json()
```

!!! note "REST API Status"
    The REST API is under development in the `gecko-rest-api` module. Currently, use subprocess or file-based methods.

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

## Real-World Example: validate_circuits.py

The GeckoCIRCUITS repository includes a complete example of Python integration:

```python
# From: resources/validate_circuits.py
# Validates all .ipes circuit files in a directory

import subprocess
import glob
from pathlib import Path

def validate_circuit(circuit_file):
    """Run simulation and check for errors."""
    result = subprocess.run([
        'java', '-jar', 'gecko.jar',
        '--headless',
        '--circuit', circuit_file,
        '--validate'
    ], capture_output=True, text=True)

    return result.returncode == 0

# Validate all circuits
circuits = glob.glob('resources/examples/**/*.ipes', recursive=True)
passed = sum(1 for c in circuits if validate_circuit(c))

print(f"Passed: {passed}/{len(circuits)}")
```

This script demonstrates:
- Subprocess invocation with multiple options
- Batch processing of multiple files
- Error detection and reporting
- Integration with standard Python tools

## Best Practices

| Best Practice | Why | Example |
|---------------|-----|---------|
| Use subprocess for isolation | Avoid JVM overhead in Python | One JAR invocation per simulation |
| Cache heavy simulations | Avoid redundant computation | Store results in SQLite or HDF5 |
| Batch parameters efficiently | Reduce process startup overhead | Group related parameter sweeps |
| Use parallel processing | Exploit multiple CPU cores | `ProcessPoolExecutor` for independent sweeps |

```python
# Parallel sweeps with ProcessPoolExecutor
from concurrent.futures import ProcessPoolExecutor
import numpy as np

def sweep_single_value(load_value):
    """Run single simulation."""
    return run_simulation('buck.ipes', {'R_load': load_value})

# Parallel execution
with ProcessPoolExecutor(max_workers=4) as executor:
    loads = np.linspace(5, 50, 10)
    results = list(executor.map(sweep_single_value, loads))
```

## Resources

- Tutorial directory: `resources/tutorials/7xx_scripting_automation/706_python_integration/`
- Validation script: `resources/validate_circuits.py`
- Example circuits: `resources/examples/`

## Related Tutorials

- [701 - GeckoSCRIPT Basics](geckoscript.md)
- [702 - MATLAB Integration](matlab.md)
- [704 - Java Blocks](java-blocks.md)
