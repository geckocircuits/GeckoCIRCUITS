---
title: Remote Interface (RMI)
description: Control GeckoCIRCUITS from MATLAB, Octave, or Java via RMI
---

# Remote Interface (RMI)

GeckoCIRCUITS can be controlled remotely via Java RMI (Remote Method Invocation). This enables integration with MATLAB, Octave, and Java applications.

## Architecture

```
┌──────────────┐     RMI      ┌──────────────────┐
│   MATLAB     │◄────────────►│  GeckoCIRCUITS   │
│   Octave     │  port 1099   │  (REMOTE mode)   │
│   Java app   │              │                  │
└──────────────┘              └──────────────────┘
```

## Starting in Remote Mode

```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar gecko.jar --remote
```

This starts the RMI registry on port 1099 and waits for connections.

## MATLAB Integration

### Connecting

```matlab
% Add GeckoCIRCUITS JAR to MATLAB classpath
javaaddpath('path/to/gecko-1.0-jar-with-dependencies.jar');

% Connect to running GeckoCIRCUITS instance
gecko = GeckoRemoteInterface('localhost', 1099);
```

### Basic Operations

```matlab
% Open a circuit file
gecko.openFile('path/to/circuit.ipes');

% Set component parameters
gecko.setParameter('R.1', 'resistance', 10.0);
gecko.setParameter('PWM.1', 'dutyCycle', 0.5);

% Run simulation
gecko.runSimulation();

% Get measurement results
vout = gecko.getMeasurement('SCOPE.1', 'ch1_avg');
```

### Parameter Sweep Example

```matlab
D_values = 0.1:0.05:0.9;
Vout = zeros(size(D_values));

for i = 1:length(D_values)
    gecko.setParameter('PWM.1', 'dutyCycle', D_values(i));
    gecko.runSimulation();
    Vout(i) = gecko.getMeasurement('SCOPE.1', 'ch1_avg');
end

plot(D_values, Vout);
xlabel('Duty Cycle'); ylabel('Output Voltage (V)');
```

## Available Methods

### File Operations

| Method | Description |
|--------|-------------|
| `openFile(path)` | Open an .ipes circuit file |
| `saveFile(path)` | Save current circuit |

### Parameter Control

| Method | Description |
|--------|-------------|
| `setParameter(component, param, value)` | Set a component parameter |
| `getParameter(component, param)` | Get current parameter value |

### Simulation Control

| Method | Description |
|--------|-------------|
| `runSimulation()` | Run and wait for completion |
| `stopSimulation()` | Stop a running simulation |
| `setSimulationTime(tEnd)` | Set simulation duration |
| `setTimeStep(dt)` | Set computation time step |

### Measurements

| Method | Description |
|--------|-------------|
| `getMeasurement(scope, channel)` | Get scalar measurement |
| `getSignalData(scope, channel)` | Get full waveform data |

## Component Naming

| Component | Format | Example |
|-----------|--------|---------|
| Resistor | `R.N` | `R.1` |
| Inductor | `L.N` | `L.1` |
| Capacitor | `C.N` | `C.1` |
| MOSFET | `MOSFET.N` | `MOSFET.1` |
| Diode | `DIODE.N` | `DIODE.1` |
| PWM | `PWM.N` | `PWM.1` |
| SCOPE | `SCOPE.N` | `SCOPE.1` |

## Memory-Mapped File Interface

For high-performance data exchange:

```java
GeckoCustomMMF mmf = new GeckoCustomMMF();
mmf.connect();
mmf.writeParameter("PWM.1", "dutyCycle", 0.5);
mmf.runSimulation();
double[] data = mmf.readSignal("SCOPE.1", "ch1");
```

Faster than RMI for large data transfers.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Connection refused | Ensure GeckoCIRCUITS runs in REMOTE mode |
| Port in use | Change RMI port or kill existing process |
| ClassNotFoundException | Add gecko JAR to MATLAB classpath |

## See Also

- [MATLAB Tutorial](../tutorials/scripting/matlab.md)
- [GeckoSCRIPT](geckoscript-ref.md) - Built-in scripting
- [REST API](rest-api.md) - HTTP-based alternative
