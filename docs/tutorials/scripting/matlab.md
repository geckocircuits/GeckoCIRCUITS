---
title: "702 - MATLAB Integration"
---

# 702 - MATLAB Integration

Remote Method Invocation (RMI) interface for MATLAB/Octave.

## Overview

GeckoCIRCUITS provides MATLAB integration through:
- **RMI Interface** - Remote procedure calls from MATLAB
- **Simulink S-Function** - Co-simulation block
- **Data Exchange** - Import/export simulation data

## RMI Setup

### Starting GeckoCIRCUITS in Remote Mode

```bash
java -jar gecko.jar --remote
```

Or from MATLAB:
```matlab
system('java -jar gecko.jar --remote &');
pause(5);  % Wait for startup
```

### Connecting from MATLAB

```matlab
% Add GeckoCIRCUITS to Java path
javaaddpath('path/to/gecko.jar');

% Import remote interface
import ch.technokrat.gecko.*;

% Connect to running instance
gecko = GeckoRemoteInterface.getInstance();
```

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

1. **Always check simulation completion** before accessing results
2. **Use appropriate time step** for accuracy vs speed
3. **Close connection** when done:
   ```matlab
   gecko.disconnect();
   ```
4. **Batch operations** for efficiency (minimize RMI calls)

## Related Resources

- [701 - GeckoSCRIPT](geckoscript.md)
- [706 - Python Integration](python.md)
- [706 - Python Integration](python.md)
