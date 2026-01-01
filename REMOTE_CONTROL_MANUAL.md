# GeckoCIRCUITS Remote Control Manual

## Table of Contents
- [Overview](#overview)
- [Operating Modes](#operating-modes)
- [Quick Start](#quick-start)
- [Remote Mode (RMI)](#remote-mode-rmi)
- [Memory-Mapped File Mode (MMF)](#memory-mapped-file-mode-mmf)
- [API Reference](#api-reference)
- [Examples](#examples)
- [Troubleshooting](#troubleshooting)

---

## Overview

GeckoCIRCUITS provides **remote control capabilities** that allow you to:
- Run simulations programmatically
- Set and modify parameters
- Retrieve simulation results
- Perform signal analysis (RMS, THD, FFT, etc.)
- Integrate with MATLAB, Python, or other external tools

This is particularly useful for:
- **Batch processing** - Running multiple simulations automatically
- **Parameter sweeps** - Testing different parameter combinations
- **MATLAB/Octave integration** - Controlling simulations from MATLAB scripts
- **CI/CD pipelines** - Automated testing
- **Optimization** - Running optimization algorithms that require many simulations

---

## Operating Modes

### Remote Mode (RMI) - Network-Based
- Uses Java RMI (Remote Method Invocation)
- Works over network (localhost or remote machine)
- Easier to use from any JVM language
- Slightly slower than MMF due to network overhead
- **Recommended for**: MATLAB/Octave, Python (with Jython/JPype), general use

### Memory-Mapped File Mode (MMF) - High-Performance
- Uses shared memory file for communication
- Extremely fast (no network overhead)
- Local machine only
- Requires custom client implementation
- **Recommended for**: High-frequency data exchange, C/C++ integration, local batch processing

---

## Quick Start

### 1. Build the Application
```bash
cd /path/to/gecko
mvn clean package assembly:single
```

This creates `target/gecko-1.0-jar-with-dependencies.jar`

### 2. Start GeckoCIRCUITS in Remote Mode
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar -p 43035
```

### 3. Connect from External Program
```java
// In your Java program
GeckoRemote.connectToGecko("localhost", 43035);
GeckoRemote.openFile("path/to/circuit.ipes");
GeckoRemote.runSimulation();
double[][] results = GeckoRemote.getGlobalDoubleMatrix();
GeckoRemote.disconnectFromGecko();
```

---

## Remote Mode (RMI)

### Starting GeckoCIRCUITS

#### Basic Remote Mode
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar -p <PORT>
```

#### With Circuit File
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar <circuit-file.ipes> -p <PORT>
```

#### With Custom Java Path
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar -p <PORT> -j /path/to/java
```

**Parameters:**
- `-p <PORT>` - Enable remote access on specified port (required)
- `-j <java_path>` - Path to Java executable (optional)
- `<circuit-file.ipes>` - Load circuit file on startup (optional)

**Default Port:** 43035 (can be changed)

### Connecting to GeckoCIRCUITS

#### From Java
```java
import ch.technokrat.gecko.GeckoRemote;

// Connect to running instance
GeckoRemote.connectToGecko("localhost", 43035);

// Or start a new instance and connect
GeckoRemote.startGui(43035);
```

#### From MATLAB/Octave
```matlab
% Add GeckoCIRCUITS JAR to MATLAB classpath
javaaddpath('/path/to/gecko-1.0-jar-with-dependencies.jar');

% Create GeckoRemote object
gesim = javaObject('gecko.GeckoRemote');

% Connect to instance on port 43035
gesim.connectToGecko(43035);
```

#### From Python (using JPype)
```python
import jpype
import jpype.imports

# Start JVM with GeckoCIRCUITS JAR
jpype.startJVM(classpath=['target/gecko-1.0-jar-with-dependencies.jar'])

# Import and connect
GeckoRemote = jpype.JClass('gecko.GeckoRemote')
GeckoRemote.connectToGecko('localhost', 43035)
```

### Common Workflow

```java
// 1. Connect
GeckoRemote.connectToGecko("localhost", 43035);

try {
    // 2. Load circuit file
    GeckoRemote.openFile("path/to/circuit.ipes");

    // 3. Set parameters
    GeckoRemote.setGlobalParameterValue("duty_cycle", 0.5);
    GeckoRemote.setParameter("R1", "resistance", 100.0);

    // 4. Configure simulation
    GeckoRemote.set_dt(1e-6);           // Time step
    GeckoRemote.set_Tend(0.01);         // End time

    // 5. Run simulation
    GeckoRemote.initSimulation();
    GeckoRemote.runSimulation();

    // 6. Get results
    double simTime = GeckoRemote.getSimulationTime();
    double[][] results = GeckoRemote.getGlobalDoubleMatrix();

    // Or get specific signal data
    float[] signalData = GeckoRemote.getSignalData("Scope1.Out1", 0, simTime, 1);

    // 7. Perform analysis
    double rms = GeckoRemote.getSignalRMS("Scope1.Out1", 0, simTime);
    double thd = GeckoRemote.getSignalTHD("Scope1.Out1", 0, simTime);
    double[][] fft = GeckoRemote.getSignalFourier("Scope1.Out1", 0, simTime, 20);

} finally {
    // 8. Disconnect
    GeckoRemote.disconnectFromGecko();
}
```

---

## Memory-Mapped File Mode (MMF)

### Starting GeckoCIRCUITS in MMF Mode

```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar -mm <filename> <filesize>
```

**Parameters:**
- `-mm <filename>` - Name of memory-mapped file
- `<filesize>` - Size of file in bytes (optional, default: 10485760)

**Example:**
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar \
  -mm /tmp/gecko_mmf.bin 10485760
```

### Using MMF

Memory-mapped files provide the same API as RMI but with much higher performance. The implementation requires creating a `GeckoCustomMMF` object on the server side and a corresponding client that reads/writes to the shared file.

**Note:** MMF mode is advanced and typically requires custom client implementation. See `GeckoCustomMMF.java` for details.

---

## API Reference

### Simulation Control

| Method | Description | Parameters |
|---------|-------------|-------------|
| `runSimulation()` | Run the entire simulation | - |
| `initSimulation()` | Initialize with default time step and end time | - |
| `initSimulation(deltaT, endTime)` | Initialize with specific time settings | `deltaT` (double), `endTime` (double) |
| `continueSimulation()` | Continue a paused simulation | - |
| `simulateTime(time)` | Simulate until specific time | `time` (double) |
| `endSimulation()` | End the simulation | - |
| `getSimulationTime()` | Get current simulation time | Returns `double` |
| `get_dt()` | Get time step | Returns `double` |
| `set_dt(value)` | Set time step | `value` (double) |
| `get_Tend()` | Get end time | Returns `double` |
| `set_Tend(value)` | Set end time | `value` (double) |

### Parameter Management

| Method | Description | Parameters |
|---------|-------------|-------------|
| `setParameter(elementName, parameterName, value)` | Set a single parameter | `elementName` (String), `parameterName` (String), `value` (double) |
| `setParameters(elementName, parameterNames, values)` | Set multiple parameters at once | `elementName` (String), `parameterNames` (String[]), `values` (double[]) |
| `getParameter(elementName, parameterName)` | Get parameter value | Returns `double` |
| `setGlobalParameterValue(name, value)` | Set global parameter | `name` (String), `value` (double) |
| `getGlobalParameterValue(name)` | Get global parameter value | Returns `double` |

### Component Information

| Method | Description | Returns |
|---------|-------------|---------|
| `getControlElements()` | List all control elements | `String[]` |
| `getCircuitElements()` | List all circuit components | `String[]` |
| `getIGBTs()` | List all IGBTs | `String[]` |
| `getDiodes()` | List all diodes | `String[]` |
| `getThyristors()` | List all thyristors | `String[]` |
| `getResistors()` | List all resistors | `String[]` |
| `getInductors()` | List all inductors | `String[]` |
| `getCapacitors()` | List all capacitors | `String[]` |

### Data Retrieval

| Method | Description | Parameters | Returns |
|---------|-------------|-------------|----------|
| `getOutput(elementName)` | Get output value | `elementName` (String) | `double` |
| `getOutput(elementName, outputName)` | Get named output | `elementName` (String), `outputName` (String) | `double` |
| `getSignalData(signalName, tStart, tEnd, skipPoints)` | Get waveform data | `signalName` (String), `tStart` (double), `tEnd` (double), `skipPoints` (int) | `float[]` |
| `getTimeArray(signalName, tStart, tEnd, skipPoints)` | Get time array | `signalName` (String), `tStart` (double), `tEnd` (double), `skipPoints` (int) | `double[]` |
| `getGlobalDoubleMatrix()` | Get all simulation results | - | `double[][]` |
| `getGlobalFloatMatrix()` | Get all results as float | - | `float[][]` |

### Signal Analysis

| Method | Description | Parameters | Returns |
|---------|-------------|-------------|----------|
| `getSignalAvg(signalName, startTime, endTime)` | Average value | `signalName` (String), `startTime` (double), `endTime` (double) | `double` |
| `getSignalRMS(signalName, startTime, endTime)` | RMS value | `signalName` (String), `startTime` (double), `endTime` (double) | `double` |
| `getSignalMax(signalName, startTime, endTime)` | Maximum value | `signalName` (String), `startTime` (double), `endTime` (double) | `double` |
| `getSignalMin(signalName, startTime, endTime)` | Minimum value | `signalName` (String), `startTime` (double), `endTime` (double) | `double` |
| `getSignalTHD(signalName, startTime, endTime)` | Total Harmonic Distortion | `signalName` (String), `startTime` (double), `endTime` (double) | `double` |
| `getSignalShape(signalName, startTime, endTime)` | Signal shape factor | `signalName` (String), `startTime` (double), `endTime` (double) | `double` |
| `getSignalRipple(signalName, startTime, endTime)` | Ripple percentage | `signalName` (String), `startTime` (double), `endTime` (double) | `double` |
| `getSignalFourier(signalName, startTime, endTime, harmonics)` | FFT analysis | `signalName` (String), `startTime` (double), `endTime` (double), `harmonics` (int) | `double[][]` |

### File Operations

| Method | Description | Parameters |
|---------|-------------|-------------|
| `openFile(fileName)` | Load circuit file | `fileName` (String) - absolute path |
| `saveFileAs(fileName)` | Save circuit file | `fileName` (String) - absolute path |

### Component Manipulation

| Method | Description | Parameters |
|---------|-------------|-------------|
| `createComponent(elementType, elementName, x, y)` | Create new component | `elementType` (String), `elementName` (String), `x` (int), `y` (int) |
| `deleteComponent(elementName)` | Delete a component | `elementName` (String) |
| `setPosition(elementName, x, y)` | Set component position | `elementName` (String), `x` (int), `y` (int) |
| `rotate(elementName)` | Rotate component 90 degrees | `elementName` (String) |
| `setOrientation(elementName, direction)` | Set component orientation | `elementName` (String), `direction` (String) |

---

## Examples

### Example 1: Basic Parameter Sweep in Java

```java
import ch.technokrat.gecko.GeckoRemote;

public class ParameterSweep {
    public static void main(String[] args) throws Exception {
        // Connect to GeckoCIRCUITS
        GeckoRemote.connectToGecko("localhost", 43035);

        try {
            // Load circuit
            GeckoRemote.openFile("resources/Topologies/BuckBoost_thermal.ipes");

            // Sweep duty cycle from 0.1 to 0.9
            double[] dutyCycles = {0.1, 0.3, 0.5, 0.7, 0.9};

            for (double duty : dutyCycles) {
                System.out.println("Running simulation with duty cycle = " + duty);

                // Set parameter
                GeckoRemote.setGlobalParameterValue("duty_cycle", duty);

                // Run simulation
                GeckoRemote.runSimulation();

                // Get output voltage
                double outputVoltage = GeckoRemote.getSignalAvg("Scope1.Out1", 0.005, 0.01);

                System.out.println("Output voltage: " + outputVoltage + " V");
            }
        } finally {
            GeckoRemote.disconnectFromGecko();
        }
    }
}
```

### Example 2: MATLAB/Octave Integration

```matlab
%% GeckoCIRCUITS Remote Control Example
clc; clear all;

% Add GeckoCIRCUITS to MATLAB classpath
gecko_jar = '/path/to/gecko-1.0-jar-with-dependencies.jar';
javaaddpath(gecko_jar);

% Create and connect
gesim = javaObject('gecko.GeckoRemote');
gesim.connectToGecko(43035);

try
    % Load circuit
    gesim.openFile('resources/Topologies/ThreePhase-VSR_10kW_thermal.ipes');

    % Set parameters
    gesim.setParameter('L1', 'inductance', 100e-6);
    gesim.setParameter('R1', 'resistance', 0.1);

    % Run simulation
    gesim.runSimulation();

    % Get simulation time
    Tsim = gesim.getSimulationTime();
    fprintf('Simulation completed in %.6f seconds\n', Tsim);

    % Get output data
    voltage_data = gesim.getSignalData('Scope1.Out1', 0, Tsim, 1);
    time_array = gesim.getTimeArray('Scope1.Out1', 0, Tsim, 1);

    % Perform analysis
    rms_voltage = gesim.getSignalRMS('Scope1.Out1', 0.001, Tsim);
    thd_voltage = gesim.getSignalTHD('Scope1.Out1', 0.001, Tsim);

    fprintf('RMS Voltage: %.2f V\n', rms_voltage);
    fprintf('THD: %.2f%%\n', thd_voltage * 100);

    % Get FFT
    fft_data = gesim.getSignalFourier('Scope1.Out1', 0.001, Tsim, 20);

    % Plot results
    figure;
    subplot(2,1,1);
    plot(time_array, voltage_data);
    title('Output Voltage');
    xlabel('Time (s)');
    ylabel('Voltage (V)');
    grid on;

    subplot(2,1,2);
    stem(fft_data(1,:), fft_data(2,:));
    title('FFT Spectrum');
    xlabel('Harmonic Number');
    ylabel('Magnitude');
    grid on;

catch ex
    disp('Error:');
    disp(ex.message);
end

% Cleanup
gesim.disconnectFromGecko();
```

### Example 3: Batch Processing Multiple Files

```java
import ch.technokrat.gecko.GeckoRemote;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class BatchSimulator {
    public static void main(String[] args) throws Exception {
        GeckoRemote.connectToGecko("localhost", 43035);

        try {
            // Process all .ipes files in a directory
            File dir = new File("resources/Topologies/");
            List<File> files = Files.walk(dir.toPath())
                .filter(p -> p.toString().endsWith(".ipes"))
                .map(p -> p.toFile())
                .toList();

            for (File file : files) {
                System.out.println("Processing: " + file.getName());

                // Load and run
                GeckoRemote.openFile(file.getAbsolutePath());
                GeckoRemote.runSimulation();

                // Save results
                double[][] results = GeckoRemote.getGlobalDoubleMatrix();
                saveResults(file.getName(), results);
            }
        } finally {
            GeckoRemote.disconnectFromGecko();
        }
    }

    private static void saveResults(String filename, double[][] results) throws Exception {
        // Save results to CSV or other format
        String outputFile = "results/" + filename + ".csv";
        // Implementation depends on your needs
    }
}
```

### Example 4: Using the Provided gecko2octave Function

The repository includes a ready-to-use function in `gecko2octave/gecko2octave.m`:

```matlab
% Define parameters
parameter_names = {'duty_cycle', 'inductance'};
parameter_values = [0.5, 100e-6];
filename = 'resources/Topologies/buck_simple.ipes';
gecko_path = '/path/to/gecko-1.0-jar-with-dependencies.jar';

% Run simulation
[t, waveforms] = gecko2octave(filename, parameter_names, parameter_values, gecko_path);

% Plot results
plot(t, waveforms');
xlabel('Time (s)');
ylabel('Amplitude');
legend('Signal 1', 'Signal 2');
grid on;
```

---

## Troubleshooting

### Connection Issues

**Problem:** `RuntimeException: There is no GeckoCIRCUITS instance at port 43035`

**Solutions:**
1. Make sure GeckoCIRCUITS is running with `-p 43035` flag
2. Check that the port number matches
3. Verify no firewall is blocking the connection
4. Use `"localhost"` instead of IP if running on same machine

### Port Already in Use

**Problem:** `Port 43035 is already occupied`

**Solutions:**
1. Use a different port: `-p 43036`
2. Close the existing GeckoCIRCUITS instance
3. Check if another program is using the port

### Classpath Issues (MATLAB/Python)

**Problem:** `java.lang.ClassNotFoundException: gecko.GeckoRemote`

**Solutions:**
1. Ensure JAR path is correct and absolute
2. In MATLAB: `javaaddpath('/absolute/path/to/gecko.jar')`
3. In Python: Ensure JAR is in classpath when starting JVM

### Memory Issues

**Problem:** `java.lang.OutOfMemoryError`

**Solutions:**
1. Increase JVM heap size: `-Xmx4G` instead of `-Xmx3G`
2. Reduce circuit complexity or simulation time
3. Use `getSignalData()` for specific signals instead of `getGlobalDoubleMatrix()`

### Simulation Not Completing

**Problem:** Simulation hangs or runs indefinitely

**Solutions:**
1. Check circuit file loads correctly in GUI mode first
2. Verify simulation parameters (time step, end time) are reasonable
3. Add timeout logic in your control script
4. Check for numerical instabilities in the circuit

### Data Retrieval Issues

**Problem:** `getSignalData()` returns empty array

**Solutions:**
1. Ensure signal name matches scope input terminal label
2. Verify simulation has run and reached the requested time
3. Use `getSimulationTime()` to check actual simulation time
4. Check `skipPoints` parameter (use 1 to get all points)

### MATLAB Integration Tips

1. **Always use `try-catch-finally`**:
   ```matlab
   try
       gesim.connectToGecko(43035);
       % your code
   catch ex
       disp(ex.message);
   end
   gesim.disconnectFromGecko();  % Always call this
   ```

2. **Convert Java arrays to MATLAB arrays**:
   ```matlab
   java_array = gesim.getSignalData(...);
   matlab_array = java_array';  % Transpose for MATLAB column-major
   ```

3. **Handle data types carefully**:
   ```matlab
   % Use .doubleValue() for conversion
   value = gesim.getParameter('R1', 'resistance').doubleValue();
   ```

---

## Best Practices

1. **Always disconnect** - Always call `disconnectFromGecko()` in a finally block
2. **Use try-catch** - Wrap remote calls in exception handling
3. **Verify in GUI first** - Test circuit in GUI mode before remote control
4. **Start with simple cases** - Test basic operations before complex workflows
5. **Monitor simulation time** - Use `getSimulationTime()` to track progress
6. **Handle data types** - Be careful with Java vs. MATLAB array conventions
7. **Set appropriate timeouts** - Don't let scripts hang indefinitely
8. **Use logging** - Log operations for debugging
9. **Close connections** - Ensure proper cleanup to avoid resource leaks
10. **Document parameters** - Keep track of what parameters you're changing

---

## Additional Resources

- **GeckoSCRIPT Tutorial**: `resources/GeckoSCRIPT/GeckoSCRIPT.pdf`
- **MATLAB Integration**: `resources/GeckoSCRIPT_ example_matlab/`
- **Octave Example**: `gecko2octave/gecko2octave.m`
- **API Source Code**: `src/main/java/ch/technokrat/gecko/GeckoRemote.java`
- **Remote Interface**: `src/main/java/ch/technokrat/gecko/GeckoRemoteIntWithoutExc.java`

---

## License

GeckoCIRCUITS is licensed under GPLv3 for open source use. Commercial licenses are available from Gecko-Research GmbH.

---

## Support

For issues or questions:
- GitHub Issues: https://github.com/your-repo/gecko/issues
- Gecko-Research GmbH: https://www.gecko-research.com/
