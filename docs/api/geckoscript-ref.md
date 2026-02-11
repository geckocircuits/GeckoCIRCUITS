---
title: GeckoSCRIPT Reference
description: Built-in scripting language reference
---

# GeckoSCRIPT Reference

GeckoSCRIPT is the built-in scripting language for automating simulations within GeckoCIRCUITS. It uses JavaScript syntax powered by GraalVM.

## Overview

GeckoSCRIPT lets you:

- Automate parameter sweeps
- Run batch simulations
- Extract and process measurement data
- Implement custom control algorithms

## Getting Started

1. Open a circuit in GeckoCIRCUITS
2. Go to **Tools > GeckoSCRIPT**
3. Write or load your script
4. Click **Run Script**

```javascript
print("Hello from GeckoSCRIPT!");
```

## Core Functions

### Parameter Control

```javascript
setParameter("R.1", "resistance", 10.0);
setParameter("PWM.1", "dutyCycle", 0.5);

var D = getParameter("PWM.1", "dutyCycle");
print("Current duty cycle: " + D);
```

### Simulation Control

```javascript
setSimulationTime(0.01);    // 10 ms
setTimeStep(1e-7);          // 100 ns
runSimulation();             // blocks until complete
```

### Measurements

```javascript
var vAvg = getMeasurement("SCOPE.1", "ch1_avg");
var vRms = getMeasurement("SCOPE.1", "ch1_rms");
var vMax = getMeasurement("SCOPE.1", "ch1_max");
var vMin = getMeasurement("SCOPE.1", "ch1_min");
print("Vout: " + vAvg + " V");
```

## Examples

### Parameter Sweep

```javascript
for (var D = 0.1; D <= 0.9; D += 0.1) {
    setParameter("PWM.1", "dutyCycle", D);
    runSimulation();
    var Vout = getMeasurement("SCOPE.1", "ch1_avg");
    print("D=" + D.toFixed(2) + ", Vout=" + Vout.toFixed(2) + " V");
}
```

### Component Optimization

```javascript
var L_values = [10e-6, 22e-6, 47e-6, 100e-6, 220e-6];

for (var i = 0; i < L_values.length; i++) {
    setParameter("L.1", "inductance", L_values[i]);
    runSimulation();
    var iMax = getMeasurement("SCOPE.1", "ch2_max");
    var iMin = getMeasurement("SCOPE.1", "ch2_min");
    var ripple = iMax - iMin;
    print("L=" + (L_values[i]*1e6).toFixed(0) + "uH, ripple=" + ripple.toFixed(3) + "A");
}
```

### 2D Sweep

```javascript
var D_values = [0.25, 0.5, 0.75];
var f_values = [50e3, 100e3, 200e3];

print("D, f_sw, Vout, Vripple");
for (var i = 0; i < D_values.length; i++) {
    for (var j = 0; j < f_values.length; j++) {
        setParameter("PWM.1", "dutyCycle", D_values[i]);
        setParameter("PWM.1", "frequency", f_values[j]);
        runSimulation();
        var vAvg = getMeasurement("SCOPE.1", "ch1_avg");
        var vMax = getMeasurement("SCOPE.1", "ch1_max");
        var vMin = getMeasurement("SCOPE.1", "ch1_min");
        print(D_values[i] + ", " + f_values[j] + ", " +
              vAvg.toFixed(2) + ", " + (vMax-vMin).toFixed(4));
    }
}
```

## Function Reference

### Simulation

| Function | Description |
|----------|-------------|
| `runSimulation()` | Run and wait for completion |
| `stopSimulation()` | Abort running simulation |
| `setSimulationTime(t)` | Set duration (seconds) |
| `setTimeStep(dt)` | Set time step (seconds) |

### Parameters

| Function | Description |
|----------|-------------|
| `setParameter(comp, param, val)` | Set component value |
| `getParameter(comp, param)` | Get component value |

### Measurements

| Function | Description |
|----------|-------------|
| `getMeasurement(scope, key)` | Get scalar measurement |

### File I/O

| Function | Description |
|----------|-------------|
| `openFile(path)` | Open circuit file |
| `saveFile(path)` | Save circuit file |
| `print(msg)` | Print to console |

## Language Features

GeckoSCRIPT supports JavaScript (ES5+) via GraalVM:

- Variables, functions, arrays, objects
- `Math.sin()`, `Math.PI`, `Math.sqrt()`
- String operations, `toFixed()`, template literals

## Tutorial Files

- `resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.ipes`
- `resources/tutorials/7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.pdf`

## See Also

- [GeckoSCRIPT Tutorial](../tutorials/scripting/geckoscript.md)
- [MATLAB Integration](remote-interface.md)
- [REST API](rest-api.md)
