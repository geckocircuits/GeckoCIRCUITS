---
title: MATLAB and Octave Integration
description: Remote control of GeckoCIRCUITS from MATLAB/Octave via RMI
---

# 702 - MATLAB Integration

Control GeckoCIRCUITS remotely from MATLAB or Octave using the Remote Method Invocation (RMI) interface. This enables seamless integration of circuit simulation into MATLAB workflows, Simulink models, and automated analysis pipelines.

## Overview

MATLAB integration provides:

- **RMI Interface** - Full remote control from MATLAB command line or scripts
- **Parameter Control** - Set and get component parameters programmatically
- **Simulation Execution** - Run simulations and wait for completion from MATLAB
- **Data Extraction** - Retrieve measurements and waveforms directly into MATLAB variables
- **Simulink Co-Simulation** - Embed GeckoCIRCUITS in Simulink models via S-Function blocks

!!! warning "Prerequisites"
    - MATLAB R2018b or later (with Java support)
    - GeckoCIRCUITS 1.0+ (`gecko-1.0-jar-with-dependencies.jar`)
    - Both tools must run on the same machine for RMI

## RMI Setup

### Step 1: Start GeckoCIRCUITS in Remote Mode

First, launch GeckoCIRCUITS with the remote flag:

```bash
java -Xmx3G -jar gecko-1.0-jar-with-dependencies.jar --remote
```

The application will start in RMI server mode, listening for MATLAB connections on the default RMI port (1099).

Alternatively, start it from MATLAB:

```matlab
% Start GeckoCIRCUITS in background (Windows/Linux/macOS)
system('java -Xmx3G -jar gecko-1.0-jar-with-dependencies.jar --remote &');
pause(5);  % Wait for startup and RMI registration
```

### Step 2: Connect from MATLAB

Once GeckoCIRCUITS is running, add the JAR to MATLAB's classpath and connect:

```matlab
% Add GeckoCIRCUITS JAR to Java path
javaaddpath('path/to/gecko-1.0-jar-with-dependencies.jar');

% Import the remote interface class
import ch.technokrat.gecko.GeckoRemoteInterface;

% Get singleton instance (connects to running GeckoCIRCUITS)
gecko = GeckoRemoteInterface.getInstance();

% Verify connection
if gecko.isConnected()
    disp('Connected to GeckoCIRCUITS');
else
    error('Failed to connect to GeckoCIRCUITS');
end
```

!!! tip "Port Configuration"
    By default, GeckoCIRCUITS uses RMI port 1099. If this port is in use, start with:
    ```bash
    java -jar gecko.jar --remote --rmi-port 2099
    ```
    Then connect from MATLAB using the GeckoRemoteInterface constructor with the custom port.

## Basic Operations

### Opening Circuits

```matlab
% Open circuit file
gecko.openFile('buck_converter.ipes');

% Save circuit
gecko.saveFile('modified_buck.ipes');
```

### Setting Parameters

```matlab
% Set component parameter
gecko.setParameter('R_load', 10);
gecko.setParameter('L1.inductance', 47e-6);
gecko.setParameter('PWM1.frequency', 100e3);
gecko.setParameter('PWM1.dutyCycle', 0.5);
```

### Running Simulations

```matlab
% Configure simulation
gecko.setSimulationTime(0.01);  % 10 ms
gecko.setTimeStep(1e-7);        % 100 ns

% Run simulation
gecko.startSimulation();

% Wait for completion
while gecko.isSimulationRunning()
    pause(0.1);
end
```

### Getting Results

```matlab
% Get scope measurements
Vout_mean = gecko.getMean('SCOPE', 'CH1');
Iout_rms = gecko.getRMS('SCOPE', 'CH2');
ripple_pp = gecko.getPeakToPeak('SCOPE', 'CH3');

% Get waveform data
time = gecko.getSignalTime('SCOPE');
voltage = gecko.getSignalData('SCOPE', 'CH1');
current = gecko.getSignalData('SCOPE', 'CH2');

% Plot in MATLAB
figure;
plot(time*1e3, voltage);
xlabel('Time (ms)');
ylabel('Voltage (V)');
```

## Parameter Sweeps

### Duty Cycle Sweep Example

```matlab
duties = 0.2:0.05:0.8;
Vout = zeros(size(duties));
efficiency = zeros(size(duties));

for i = 1:length(duties)
    gecko.setParameter('PWM1.dutyCycle', duties(i));
    gecko.startSimulation();

    while gecko.isSimulationRunning()
        pause(0.1);
    end

    Vout(i) = gecko.getMean('SCOPE', 'Vout');
    Pin = gecko.getMean('SCOPE', 'Pin');
    Pout = gecko.getMean('SCOPE', 'Pout');
    efficiency(i) = Pout / Pin;
end

% Plot results
figure;
subplot(2,1,1);
plot(duties, Vout);
xlabel('Duty Cycle'); ylabel('Output Voltage (V)');

subplot(2,1,2);
plot(duties, efficiency*100);
xlabel('Duty Cycle'); ylabel('Efficiency (%)');
```

### Frequency Sweep (Bode Plot)

```matlab
frequencies = logspace(1, 5, 50);  % 10 Hz to 100 kHz
magnitude = zeros(size(frequencies));
phase = zeros(size(frequencies));

for i = 1:length(frequencies)
    gecko.setParameter('AC_SOURCE.frequency', frequencies(i));
    gecko.startSimulation();

    while gecko.isSimulationRunning()
        pause(0.1);
    end

    Vin = gecko.getFFTMagnitude('SCOPE', 'Vin', frequencies(i));
    Vout = gecko.getFFTMagnitude('SCOPE', 'Vout', frequencies(i));
    magnitude(i) = 20*log10(Vout/Vin);

    phase(i) = gecko.getFFTPhase('SCOPE', 'Vout', frequencies(i)) - ...
               gecko.getFFTPhase('SCOPE', 'Vin', frequencies(i));
end

% Bode plot
figure;
subplot(2,1,1);
semilogx(frequencies, magnitude);
xlabel('Frequency (Hz)'); ylabel('Magnitude (dB)');
grid on;

subplot(2,1,2);
semilogx(frequencies, phase);
xlabel('Frequency (Hz)'); ylabel('Phase (deg)');
grid on;
```

## Simulink Integration

### S-Function Block

GeckoCIRCUITS can run as Simulink S-Function:

1. Add `GeckoSFunction` block to model
2. Configure:
   - Circuit file path
   - Input signals mapping
   - Output signals mapping
3. Set sample time to match simulation step

### Co-simulation Setup

```matlab
% In MATLAB, before running Simulink model
gecko = GeckoRemoteInterface.getInstance();
gecko.openFile('converter.ipes');
gecko.setSimulinkMode(true);
```

## Memory-Mapped Files (MMF)

For high-speed data exchange:

```matlab
% Start with MMF mode
system('java -jar gecko.jar --mmf &');

% In MATLAB
mmf = memmapfile('gecko_data.bin', 'Format', 'double');
data = mmf.Data;
```

## Error Handling

```matlab
try
    gecko.openFile('circuit.ipes');
    gecko.startSimulation();
catch ME
    fprintf('Error: %s\n', ME.message);
    % Handle error
end
```

## Best Practices

| Practice | Why | Example |
|----------|-----|---------|
| Check simulation completion | Avoid reading stale results | Use `while gecko.isSimulationRunning()` |
| Batch parameter updates | Minimize RMI overhead | Set all parameters before `startSimulation()` |
| Use appropriate timestep | Balance accuracy vs speed | Use 1-10 ns for high-frequency switching |
| Reuse gecko instance | Avoid reconnection delay | Create once, use throughout script |

!!! warning "Error Handling"
    Always wrap RMI calls in try-catch blocks:
    ```matlab
    try
        gecko.openFile('circuit.ipes');
        gecko.startSimulation();
    catch ME
        fprintf('Error: %s\n', ME.message);
        % Handle cleanup
    end
    ```

## Example: Complete Efficiency vs Load Sweep

```matlab
% Full end-to-end example: measure efficiency across load range
gecko = GeckoRemoteInterface.getInstance();
gecko.openFile('buck_converter.ipes');

loads = [2, 5, 10, 20, 50];  % Ohms
efficiency = [];

for R = loads
    gecko.setParameter('R_load', R);
    gecko.setSimulationTime(0.01);
    gecko.setTimeStep(1e-7);

    gecko.startSimulation();
    while gecko.isSimulationRunning()
        pause(0.05);
    end

    Pin = gecko.getMean('SCOPE', 'Pin');
    Pout = gecko.getMean('SCOPE', 'Pout');
    eta = Pout / Pin * 100;

    efficiency = [efficiency; eta];
    fprintf('Load: %g Ohm -> Efficiency: %.1f%%\n', R, eta);
end

% Plot results
figure;
plot(loads, efficiency, 'b-o', 'LineWidth', 2);
xlabel('Load (Ohms)');
ylabel('Efficiency (%)');
grid on;
title('Buck Converter: Efficiency vs Load');
```

## References

- Tutorial guide: `resources/tutorials/7xx_scripting_automation/702_matlab_integration/`
- Example circuits in `resources/examples/`
- MATLAB documentation: [Calling Java from MATLAB](https://www.mathworks.com/help/matlab/java.html)

## Related Tutorials

- [701 - GeckoSCRIPT Basics](geckoscript.md)
- [704 - Java Blocks](java-blocks.md)
- [706 - Python Integration](python.md)
