# 7xx - Scripting & Automation

Programmatic control and external tool integration.

| Tutorial | Title | Difficulty | Status |
|----------|-------|------------|--------|
| [701](701_gecko_script_basics/) | GeckoSCRIPT Basics | 2/3 | Available |
| [702](702_matlab_integration/) | MATLAB Integration | 2/3 | Available |
| [703](703_simulink_cosimulation/) | Simulink Co-Simulation | 3/3 | Available |
| [704](704_java_blocks/) | Java Blocks | 3/3 | Available |
| [705](705_api_integration/) | API Integration | 3/3 | Available |
| [706](706_python_integration/) | Python Integration | 2/3 | Placeholder |

## Learning Objectives

- Automate simulations with GeckoSCRIPT
- Integrate with MATLAB for parameter sweeps
- Co-simulate with Simulink models
- Create custom Java control blocks
- Use the REST API for remote control
- Control simulations from Python scripts

## Integration Options Overview

| Method | Best For | Complexity |
|--------|----------|------------|
| GeckoSCRIPT | Built-in automation | Low |
| MATLAB | Control design, data analysis | Medium |
| Simulink | Real-time HIL, system models | Medium |
| Java Blocks | Custom components | Medium-High |
| Python | Optimization, machine learning | Medium |
| REST API | Web integration, remote control | High |

## Contents

### 701 - GeckoSCRIPT Basics
- `GeckoSCRIPT.pdf` - Complete scripting guide
- `GeckoSCRIPT.ipes` - Example script-controlled circuit
- `buck_control.ipes` / `buck_control_tuning.m` - Control tuning example

### 702 - MATLAB Integration
- `GeckoSCRIPT_ example_matlab/` - MATLAB integration examples
- Loss calculation scripts (`.scl`)
- RMI interface for bidirectional communication

### 703 - Simulink Co-Simulation
- `GeckoCIRCUITS_simulink_tutorial.pdf` - Tutorial guide
- `Gecko_VR1.mdl` - Simulink model
- `s_GeckoCIRCUITS.c` - S-function source
- MEX files for various platforms

### 704 - Java Blocks
- `demo_JAVA_Block.ipes` - Java block introduction
- `JavaBlockPMSM.ipes` - PMSM control with Java
- Custom component development guide

### 705 - API Integration
- `SIGNAL_ANALYSIS_API.md` - API documentation
- `websocket-client.html` - WebSocket test client
- External signal source examples

### 706 - Python Integration (New)
- Parameter sweep automation
- Optimization with SciPy
- Data analysis with NumPy/Pandas
- Parallel simulation execution

## Quick Start: Automation

### GeckoSCRIPT (Built-in)
```javascript
// Simple parameter sweep
for (D = 0.2; D <= 0.8; D += 0.1) {
    setParameter("PWM.1", "dutyCycle", D);
    runSimulation();
    Vout = getMeasurement("SCOPE.1", "ch1_avg");
    print("D=" + D + ", Vout=" + Vout);
}
```

### MATLAB (RMI Interface)
```matlab
% Connect to GeckoCIRCUITS
gecko = GeckoRemoteInterface('localhost', 1099);

% Run parameter sweep
D_values = 0.2:0.1:0.8;
for i = 1:length(D_values)
    gecko.setParameter('PWM.1', 'dutyCycle', D_values(i));
    gecko.runSimulation();
    Vout(i) = gecko.getMeasurement('SCOPE.1', 'ch1_avg');
end
```

### Python (File-based or API)
```python
import subprocess
import numpy as np

# Parameter sweep
D_values = np.linspace(0.2, 0.8, 7)
for D in D_values:
    # Run simulation
    subprocess.run(['java', '-jar', 'gecko.jar',
                   '--param', f'D={D}', 'buck.ipes'])
    # Analyze results
    results = np.loadtxt('output.csv')
    print(f"D={D:.1f}, Vout={results[:,1].mean():.2f}")
```

## Use Case Selection Guide

| Task | Recommended Tool |
|------|------------------|
| Quick parameter check | GeckoSCRIPT |
| Control loop design | MATLAB |
| System-level simulation | Simulink |
| Custom algorithms | Java Blocks |
| Optimization/ML | Python |
| Web dashboard | REST API |

## Related Tutorials

- [104 - Running Simulations](../1xx_getting_started/104_running_simulations/) - Batch mode basics
- [105 - Analysis Tools](../1xx_getting_started/105_analysis_tools/) - Automated analysis
