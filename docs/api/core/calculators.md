---
title: Control Calculators API
---

# Control Calculators API

Package: `ch.technokrat.gecko.core.control.calculators`

72 calculator classes implementing the computation logic for GeckoCIRCUITS control blocks. These are GUI-free and suitable for headless simulation.

## Base Classes

### AbstractControlCalculatable

Base class for all control block calculators. Provides the framework for:

- Input/output signal management
- Time step handling
- State initialization and reset

### AbstractSingleInputSingleOutputCalculator

Specialized base for blocks with one input and one output (most common pattern).

### AbstractTwoInputsOneOutputCalculator

Base for blocks with two inputs and one output (e.g., `Add`, `Multiply`, `Compare`).

### AbstractSignalCalculator

Base for signal generators (sine, square, triangle, etc.).

### AbstractPTCalculator

Base for transfer function blocks (PT1, PT2, etc.).

---

## Calculator Categories

### Math Operations

| Calculator | Inputs | Output | Description |
|------------|--------|--------|-------------|
| `AddCalculator` | 2+ | 1 | Sum of inputs |
| `SubtractCalculator` | 2 | 1 | Difference |
| `MultCalculator` | 2 | 1 | Product |
| `DivCalculator` | 2 | 1 | Division |
| `AbsCalculator` | 1 | 1 | Absolute value |
| `SqrtCalculator` | 1 | 1 | Square root |
| `PowCalculator` | 2 | 1 | Power function |
| `ExpCalculator` | 1 | 1 | Exponential |
| `LogCalculator` | 1 | 1 | Natural logarithm |
| `MinCalculator` | 2+ | 1 | Minimum |
| `MaxCalculator` | 2+ | 1 | Maximum |
| `SignumCalculator` | 1 | 1 | Sign function |
| `ConstantCalculator` | 0 | 1 | Constant value |
| `GainCalculator` | 1 | 1 | Multiply by constant |

### Trigonometric Functions

| Calculator | Function |
|------------|----------|
| `SinCalculator` | sin(x) |
| `CosCalculator` | cos(x) |
| `TanCalculator` | tan(x) |
| `ASinCalculator` | arcsin(x) |
| `ACosCalculator` | arccos(x) |
| `ATanCalculator` | arctan(x) |

### Control System Blocks

| Calculator | Description |
|------------|-------------|
| `IntegrationCalculator` | Discrete-time integrator |
| `LimitedIntegrationCalculator` | Integrator with saturation limits |
| `PICalculator` | Proportional-integral controller |
| `PIDCalculator` | Proportional-integral-derivative controller |
| `PT1Calculator` | First-order low-pass filter |
| `LimiterCalculator` | Output saturation/clamping |
| `HysteresisCalculator` | Hysteresis comparator |
| `DelayCalculator` | Time delay block |
| `SampleHoldCalculator` | Sample-and-hold |
| `RateLimiterCalculator` | Slew rate limiter |
| `CounterCalculatable` | Event counter |

### Signal Generators

| Calculator | Waveform |
|------------|----------|
| `AbstractSignalCalculatorPeriodic` | Base for periodic signals |
| `SignalCalculatorSine` | Sine wave |
| `SignalCalculatorSquare` | Square wave |
| `SignalCalculatorTriangle` | Triangle wave |
| `SignalCalculatorSawtooth` | Sawtooth wave |

### Logic Operations

| Calculator | Description |
|------------|-------------|
| `AndTwoPortCalculator` | 2-input AND gate |
| `AndMultiInputCalculator` | Multi-input AND gate |
| `OrCalculator` | OR gate |
| `NotCalculator` | NOT gate (inverter) |
| `XorCalculator` | XOR gate |
| `EqualCalculatorMultiInput` | Equality comparator |
| `GreaterThanCalculator` | Greater-than comparator |
| `LessThanCalculator` | Less-than comparator |

### Coordinate Transforms

| Calculator | Description |
|------------|-------------|
| `ABCDQCalculator` | ABC to DQ transformation (Park transform) |
| `DQABCDCalculator` | DQ to ABC inverse transformation |

### Multiplexing

| Calculator | Description |
|------------|-------------|
| `MuxCalculator` | Multiplex multiple signals into one |
| `DemuxCalculator` | Demultiplex one signal into multiple |

---

## Usage Pattern

All calculators follow a common pattern during simulation:

```java
// 1. Initialize
calculator.init(dt);          // Set time step

// 2. Set inputs (each simulation step)
calculator.setInputValue(0, inputSignal);

// 3. Calculate
calculator.bepiRechenRechts(dt);  // Compute output

// 4. Read output
double output = calculator.getOutputValue(0);
```

## Related

- [Simulation Engine](simulation.md)
- [Matrix Stampers](matrix-stampers.md)
- [GeckoSCRIPT Reference](../geckoscript-ref.md)
