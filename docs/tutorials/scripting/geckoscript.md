---
title: "701 - GeckoSCRIPT Basics"
---

# 701 - GeckoSCRIPT Basics

Built-in scripting language for automation and batch simulations.

## Overview

GeckoSCRIPT is JavaScript-based scripting built into GeckoCIRCUITS for:
- Parameter sweeps
- Batch simulations
- Custom calculations
- Data export automation

## Script Structure

### Basic Script

```javascript
// GeckoSCRIPT example
var Vin = 48;        // Input voltage
var Vout = 12;       // Output voltage
var Iout = 10;       // Output current

// Calculate duty cycle
var D = Vout / Vin;
print("Duty cycle: " + D);

// Set component parameter
setParameter("R_load", Vout / Iout);

// Run simulation
runSimulation();

// Get result
var efficiency = getMeasurement("P_out") / getMeasurement("P_in");
print("Efficiency: " + (efficiency * 100) + "%");
```

## Core Functions

### Parameter Control

```javascript
// Set component parameters
setParameter("componentName.parameter", value);

// Examples
setParameter("MOSFET1.RdsOn", 0.01);
setParameter("C1.capacitance", 100e-6);
setParameter("L1.inductance", 50e-6);

// Get current parameter value
var L = getParameter("L1.inductance");
```

### Simulation Control

```javascript
// Configure simulation
setSimulationTime(0.01);        // 10 ms
setTimeStep(1e-7);              // 100 ns
setSolverType("GEAR");          // GEAR, TRZ, or BE

// Run simulation
runSimulation();

// Stop simulation
stopSimulation();

// Wait for completion
waitForSimulation();
```

### Data Access

```javascript
// Get measurement values (after simulation)
var Vavg = getMeasurement("SCOPE.CH1.mean");
var Irms = getMeasurement("SCOPE.CH2.rms");
var Ppk = getMeasurement("SCOPE.CH3.max");

// Get waveform data
var time = getWaveform("SCOPE.time");
var voltage = getWaveform("SCOPE.CH1");
```

### Output and Export

```javascript
// Print to console
print("Result: " + value);

// Export data to CSV
exportCSV("results.csv", ["time", "voltage", "current"]);

// Save circuit
saveCircuit("modified_circuit.ipes");
```

## Parameter Sweeps

### Single Parameter Sweep

```javascript
var results = [];
var duties = [0.3, 0.4, 0.5, 0.6, 0.7];

for (var i = 0; i < duties.length; i++) {
    setParameter("PWM1.dutyCycle", duties[i]);
    runSimulation();
    waitForSimulation();

    var vout = getMeasurement("SCOPE.Vout.mean");
    results.push({duty: duties[i], vout: vout});
    print("D=" + duties[i] + " -> Vout=" + vout);
}

// Export results
exportResults("sweep_results.csv", results);
```

### Multi-Parameter Sweep

```javascript
var fsw_values = [50e3, 100e3, 200e3];
var L_values = [22e-6, 47e-6, 100e-6];

for (var i = 0; i < fsw_values.length; i++) {
    for (var j = 0; j < L_values.length; j++) {
        setParameter("PWM1.frequency", fsw_values[i]);
        setParameter("L1.inductance", L_values[j]);

        runSimulation();
        waitForSimulation();

        var ripple = getMeasurement("SCOPE.IL.pp");
        print("fsw=" + fsw_values[i] + ", L=" + L_values[j] + " -> ripple=" + ripple);
    }
}
```

## Control Flow

### Conditional Logic

```javascript
var Vout = getMeasurement("SCOPE.Vout.mean");

if (Vout < 11.9) {
    print("Output voltage too low!");
} else if (Vout > 12.1) {
    print("Output voltage too high!");
} else {
    print("Output voltage within spec");
}
```

### Loops

```javascript
// For loop
for (var i = 0; i < 10; i++) {
    // ...
}

// While loop
var iteration = 0;
while (efficiency < 0.95 && iteration < 100) {
    // Adjust parameters
    iteration++;
}
```

## Mathematical Functions

```javascript
// Built-in math
var x = Math.sin(angle);
var y = Math.cos(angle);
var z = Math.sqrt(value);
var p = Math.pow(base, exponent);
var l = Math.log10(value);

// Constants
var pi = Math.PI;
var e = Math.E;
```

## Error Handling

```javascript
try {
    runSimulation();
    var result = getMeasurement("SCOPE.CH1.mean");
} catch (error) {
    print("Error: " + error.message);
    // Handle error
}
```

## Running Scripts

### From GUI

1. **Tools → Script Editor**
2. Write or load script
3. Click **Run**

### From Command Line

```bash
java -jar gecko.jar --script script.js circuit.ipes
```

## Simulation Exercises

1. Write parameter sweep for optimal duty cycle
2. Automate efficiency measurement vs load
3. Generate Bode plot data with frequency sweep
4. Create batch simulation for component comparison

## Related Resources

- [702 - MATLAB Integration](matlab.md)
- [706 - Python Integration](python.md)
