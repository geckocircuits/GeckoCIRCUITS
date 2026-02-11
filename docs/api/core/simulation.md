---
title: Simulation Engine API
---

# Simulation Engine API

Package: `ch.technokrat.gecko.core.simulation`

The simulation engine provides headless circuit simulation without any GUI dependencies.

## HeadlessSimulationEngine

Headless simulation engine for running GeckoCIRCUITS simulations without GUI. Suitable for REST APIs, CLI tools, batch processing, and cloud deployment.

```java
HeadlessSimulationEngine engine = new HeadlessSimulationEngine();
SimulationConfig config = SimulationConfig.builder()
    .circuitFile("path/to/circuit.ipes")
    .stepWidth(1e-6)
    .simulationDuration(20e-3)
    .build();

SimulationResult result = engine.runSimulation(config);
if (result.isSuccess()) {
    double[] times = result.getTimeArray();
    float[] voltages = result.getSignalData(0);
}
```

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `runSimulation(SimulationConfig)` | `SimulationResult` | Runs a simulation (blocking) |
| `cancel()` | `void` | Requests cancellation of running simulation |
| `getState()` | `EngineState` | Gets current engine state |
| `getCurrentTime()` | `double` | Current simulation time (seconds) |
| `getEndTime()` | `double` | End time of simulation (seconds) |
| `getProgress()` | `double` | Progress from 0.0 to 1.0 |
| `getCurrentStep()` | `int` | Current time step number |
| `setProgressListener(listener)` | `void` | Sets progress callback |

### EngineState Enum

| Value | Description |
|-------|-------------|
| `IDLE` | Engine is idle and ready to run |
| `RUNNING` | Simulation is in progress |
| `PAUSED` | Simulation is paused |
| `CANCELLED` | Simulation has been cancelled |

### SimulationProgressListener

Functional interface for progress callbacks:

```java
engine.setProgressListener((currentTime, endTime, step) -> {
    System.out.printf("Progress: %.1f%%\n", currentTime / endTime * 100);
});
```

---

## SimulationConfig

Immutable configuration for a headless simulation run. Built using the builder pattern.

### Builder Methods

| Method | Type | Description |
|--------|------|-------------|
| `circuitFile(path)` | `String` | Path to `.ipes` circuit file |
| `stepWidth(dt)` | `double` | Simulation time step (seconds) |
| `simulationDuration(t)` | `double` | Total simulation time (seconds) |
| `solverType(type)` | `SolverType` | Solver algorithm (BE, TRZ, GS) |
| `parameterOverride(name, value)` | `String, double` | Override circuit parameter |
| `enableDataLogging(flag)` | `boolean` | Enable/disable data recording |
| `dataLoggingInterval(n)` | `int` | Record every N-th step |

### Getter Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getSolverSettings()` | `SolverSettingsCore` | Copy of solver settings |
| `getCircuitFilePath()` | `String` | Circuit file path |
| `getParameterOverrides()` | `Map<String, Double>` | Unmodifiable parameter map |
| `isDataLoggingEnabled()` | `boolean` | Whether logging is on |
| `getDataLoggingInterval()` | `int` | Logging step interval |

---

## SimulationResult

Immutable result of a simulation run.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `isSuccess()` | `boolean` | True if simulation completed normally |
| `getStatus()` | `Status` | Result status enum |
| `getErrorMessage()` | `String` | Error message (if failed) |
| `getDataContainer()` | `DataContainerGlobal` | Simulation data |
| `getTimeArray()` | `double[]` | Array of time values |
| `getSignalData(index)` | `float[]` | Signal data by index |
| `getExecutionTimeMs()` | `long` | Wall-clock execution time |
| `getTotalTimeSteps()` | `int` | Total steps simulated |
| `getSimulatedTime()` | `double` | Simulated time reached |
| `getMetadata()` | `Map<String, Object>` | Additional metadata |

### Status Enum

| Value | Description |
|-------|-------------|
| `SUCCESS` | Simulation completed successfully |
| `FAILED` | Simulation encountered an error |
| `CANCELLED` | Simulation was cancelled |

---

## ZeroCrossingDetector

Detects zero-crossing events in simulation signals and locates the exact crossing time using bisection. Critical for power electronics where switch events (MOSFET/IGBT transitions, diode changes, thyristor firing) create discontinuities.

```java
ZeroCrossingDetector detector = new ZeroCrossingDetector(1e-12, 50);

// After each simulation step, check signals for crossings
List<ZeroCrossingEvent> events = detector.detectCrossings(
    signalsPrev, signalsCurr, 0.5, tPrev, tCurr);

if (!events.isEmpty()) {
    double tCross = events.get(0).getCrossingTime();
    // Re-stamp MNA matrix with new switch states at tCross
}
```

### Constructors

| Constructor | Description |
|-------------|-------------|
| `ZeroCrossingDetector()` | Default: tolerance=1e-12, maxIterations=50 |
| `ZeroCrossingDetector(tolerance, maxIterations)` | Custom parameters |

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `DEFAULT_TOLERANCE` | `1e-12` | Default bisection tolerance (1 ps) |
| `DEFAULT_MAX_ITERATIONS` | `50` | Default max bisection iterations |

### ZeroCrossingEvent

| Method | Returns | Description |
|--------|---------|-------------|
| `getSignalIndex()` | `int` | Index of the signal that crossed |
| `getCrossingTime()` | `double` | Exact time of zero crossing |
| `getDirection()` | `Direction` | `RISING` or `FALLING` |
| `getThreshold()` | `double` | Threshold value crossed |

---

## SolverType

Available solver algorithms for circuit simulation.

| Value | Description |
|-------|-------------|
| `SOLVER_BE` | Backward Euler - simple, stable, first-order |
| `SOLVER_TRZ` | Trapezoidal - second-order, potential oscillation |
| `SOLVER_GS` | Gear-Shichman - multi-step, good for stiff systems |
