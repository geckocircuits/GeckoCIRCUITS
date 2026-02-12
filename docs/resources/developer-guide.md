---
title: Developer Guide
description: How to extend GeckoCIRCUITS with new components and features
---

# Developer Guide

Welcome to the GeckoCIRCUITS developer guide! This document explains how to extend the simulator with new circuit components, control blocks, and features.

## Overview

GeckoCIRCUITS is a Java 21 power electronics circuit simulator built on Modified Nodal Analysis (MNA) for electrical simulation. The architecture is modular and designed for extension. Before diving into specific extensions, read `/CLAUDE.md` in the repository root for architecture overview and build commands.

**Setup:** Clone the repository, run `mvn clean install`, then open the project in your IDE (NetBeans recommended for GUI development).

---

## Architecture Overview

```
GeckoCIRCUITS
├── Entry Point: GeckoSim (gecko.GeckoSim)
│   └── Operating Modes: STANDALONE (GUI), REMOTE (RMI), MMF, SIMULINK
│
├── Circuit Simulation Engine
│   ├── circuit/matrix/     → MNA matrix stamping (IMatrixStamper, StamperRegistry)
│   ├── circuit/netlist/    → Netlist building & component definitions
│   ├── circuit/simulation/ → Solver & integration (Backward Euler, Trapezoidal, Gear-Shichman)
│   └── circuit/components/ → 50+ circuit component implementations
│
├── Control Blocks
│   ├── control/calculators/ → 80+ control block classes (PI, PID, Gain, etc.)
│   └── calculators/         → Signal processors & transformations
│
├── Supporting Modules
│   ├── datacontainer/  → Signal storage with caching optimization
│   ├── math/           → Matrix operations, LU decomposition, FFT
│   ├── thermal/        → Thermal network simulation (MNA-based)
│   ├── scope/          → Oscilloscope visualization
│   └── i18n/           → Internationalization (968+ translation keys)
│
└── External Integration
    ├── GeckoRemoteInterface (RMI for MATLAB/Octave)
    ├── GeckoCustomMMF       (Memory-mapped files for high-speed data)
    └── GeckoRemoteRegistry  (RMI registry management)
```

### Key Domains

| Domain | Purpose | Extension Point | Difficulty |
|--------|---------|-----------------|------------|
| **Control Blocks** | Signal processing, PID, gains, logic | Extend `AbstractControlCalculatable` | Easy |
| **Circuit Components** | Resistors, capacitors, switches, motors | Implement `IMatrixStamper`/`IStatefulStamper` | Medium |
| **Matrix Operations** | MNA equation solving | Modify `LKMatrices`, solver implementations | Hard |
| **Thermal Network** | Heat dissipation simulation | Similar to circuit domain (MNA-based) | Medium |

---

## Adding a Control Block Calculator

Control blocks are the easiest extension point. They process input signals and produce output signals based on mathematical operations (gains, integrations, PID control, etc.).

### How the Control Block System Works

1. **Base Class**: `AbstractControlCalculatable` (1 input, 1 output) or multi-input variants
2. **Calculation**: Called each time step by `berechneYOUT(deltaT)` method
3. **Inputs/Outputs**: Signal arrays `_inputSignal` and `_outputSignal` (2D arrays for multi-port)
4. **State**: Optional; managed by subclasses for integrators, delays, etc.

### Example: Simple Gain Amplifier

Here's how `GainCalculator` (the simplest calculator) works:

```java
// File: control/calculators/GainCalculator.java
public final class GainCalculator extends AbstractSingleInputSingleOutputCalculator {
    private double _gain;

    public GainCalculator(final double gain) {
        super();  // Initializes 1 input, 1 output signal array
        setGain(gain);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        // Called every simulation time step
        _outputSignal[0][0] = _gain * _inputSignal[0][0];
    }

    public void setGain(final double gain) {
        _gain = gain;
    }
}
```

**Key Points:**
- `_inputSignal[portIndex][0]` = input value
- `_outputSignal[portIndex][0]` = computed output value
- `berechneYOUT()` is called every Δt seconds
- Use `AbstractSingleInputSingleOutputCalculator` base class for 1-in/1-out blocks

### Step-by-Step: Add a Custom Saturation Block

Let's add a saturation limiter block (useful for control feedback).

**Step 1: Create the calculator class**

```java
// File: control/calculators/SaturationCalculator.java
package gecko.geckocircuits.control.calculators;

/**
 * Saturation limiter: output = clamp(input, min, max)
 */
public final class SaturationCalculator extends AbstractSingleInputSingleOutputCalculator {
    private double _minValue = -1.0;
    private double _maxValue = 1.0;

    public SaturationCalculator(final double min, final double max) {
        super();
        setLimits(min, max);
    }

    @Override
    public void berechneYOUT(final double deltaT) {
        double input = _inputSignal[0][0];
        _outputSignal[0][0] = Math.max(_minValue, Math.min(_maxValue, input));
    }

    public void setLimits(final double min, final double max) {
        if (min > max) {
            throw new IllegalArgumentException("Min must be <= Max");
        }
        _minValue = min;
        _maxValue = max;
    }
}
```

**Step 2: Register in control block factory (if using GUI)**

If you're adding this to the GUI, register it in the control block dialog/factory classes (typically in `ReglerGainDialog` or similar). Otherwise, instantiate directly in your simulation code.

**Step 3: Test the calculator**

```java
// File: src/test/java/.../SaturationCalculatorTest.java
public class SaturationCalculatorTest {
    @Test
    public void testSaturation() {
        SaturationCalculator calc = new SaturationCalculator(-10, 10);

        // Connect mock input
        calc._inputSignal[0] = new double[]{0};
        calc.checkInputWithoutConnectionAndFill(0);

        // Test clamping
        calc._inputSignal[0][0] = 5.0;
        calc.berechneYOUT(0.001);
        assertEquals(5.0, calc._outputSignal[0][0], 1e-6);

        calc._inputSignal[0][0] = 15.0;
        calc.berechneYOUT(0.001);
        assertEquals(10.0, calc._outputSignal[0][0], 1e-6);
    }
}
```

### Multi-Input/Multi-Output Calculator

For blocks with multiple inputs or outputs, extend the appropriate base class:

```java
// File: control/calculators/MyMultiInputCalculator.java
public final class MyMultiInputCalculator extends AbstractTwoInputsOneOutputCalculator {
    @Override
    public void berechneYOUT(final double deltaT) {
        double input1 = _inputSignal[0][0];
        double input2 = _inputSignal[1][0];
        _outputSignal[0][0] = input1 + input2;  // Simple adder
    }
}
```

**Available Base Classes:**
- `AbstractSingleInputSingleOutputCalculator` - 1 in, 1 out (most common)
- `AbstractTwoInputsOneOutputCalculator` - 2 in, 1 out (adders, multipliers)
- Multi-input variants in `calculators/` package

### Stateful Calculators (Integrators, Delays)

For blocks that maintain state across time steps:

```java
public final class IntegratorCalculator extends AbstractSingleInputSingleOutputCalculator {
    private double _integralSum = 0.0;

    @Override
    public void berechneYOUT(final double deltaT) {
        // Trapezoidal integration rule
        double input = _inputSignal[0][0];
        _integralSum += input * deltaT;
        _outputSignal[0][0] = _integralSum;
    }

    @Override
    public void tearDownOnPause() {
        // Called when simulation pauses; reset state if needed
        _integralSum = 0.0;
    }

    public void setInitialValue(final double value) {
        _integralSum = value;
    }
}
```

**Key Methods:**
- `berechneYOUT(deltaT)` - calculation (called every step)
- `tearDownOnPause()` - cleanup when simulation pauses (override if needed)
- Private fields store state between calls

---

## Adding a Circuit Component

Circuit components use the **Modified Nodal Analysis (MNA)** technique. This is more complex than control blocks but the stamping pattern is reusable.

### How MNA Works

GeckoCIRCUITS solves the equation: **A · x = b**

Where:
- **A** = conductance/admittance matrix (stamped by each component)
- **x** = vector of unknowns (node voltages, branch currents)
- **b** = source/excitation vector (stamped by current/voltage sources)

Each component "stamps" its contribution into these matrices based on its model.

### The IMatrixStamper Interface

Every circuit component implements this interface:

```java
public interface IMatrixStamper {
    /**
     * Stamp component's effect into A matrix (conductance)
     * @param a            A matrix [nodes × nodes]
     * @param nodeX, nodeY component nodes
     * @param nodeZ        auxiliary node (for inductors, voltage sources)
     * @param parameter    component parameters array
     * @param dt           time step size (for implicit integration)
     */
    void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                      double[] parameter, double dt);

    /**
     * Stamp component's effect into b vector (sources)
     */
    void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                      double[] parameter, double dt, double time,
                      double[] previousValues);

    /**
     * Calculate component current after matrix solution
     */
    double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                           double[] parameter, double dt, double previousCurrent);

    /**
     * Weight factor for A-matrix: e.g., 1/R for resistor, C/dt for capacitor
     */
    double getAdmittanceWeight(double parameterValue, double dt);
}
```

### Example: Resistor Stamper

```java
// Simplified resistor implementation
public class ResistorStamper implements IMatrixStamper {

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt) {
        double resistance = parameter[0];
        double conductance = 1.0 / resistance;

        // Stamp conductance at diagonal entries
        a[nodeX][nodeX] += conductance;
        a[nodeY][nodeY] += conductance;

        // Stamp negative conductance at off-diagonal entries
        a[nodeX][nodeY] -= conductance;
        a[nodeY][nodeX] -= conductance;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt, double time,
                            double[] previousValues) {
        // Resistor is purely passive; no source contribution
    }

    @Override
    public double calculateCurrent(double vx, double vy, double[] parameter,
                                  double dt, double previousCurrent) {
        double resistance = parameter[0];
        return (vx - vy) / resistance;  // Ohm's law
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        return 1.0 / parameterValue;  // 1/R
    }
}
```

### Step-by-Step: Add a Custom Nonlinear Resistor

Let's implement a temperature-dependent resistor: `R(T) = R0 * (1 + α * ΔT)`

**Step 1: Create the stamper**

```java
// File: circuit/matrix/TemperatureResistorStamper.java
package gecko.geckocircuits.circuit.matrix;

public class TemperatureResistorStamper implements IMatrixStamper {

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt) {
        // parameter[0] = base resistance R0
        // parameter[1] = temperature coefficient α
        // parameter[2] = current temperature T
        // parameter[3] = reference temperature T0

        double r0 = parameter[0];
        double alpha = parameter[1];
        double temp = parameter[2];
        double tempRef = parameter[3];

        double resistance = r0 * (1.0 + alpha * (temp - tempRef));
        double conductance = 1.0 / resistance;

        a[nodeX][nodeX] += conductance;
        a[nodeY][nodeY] += conductance;
        a[nodeX][nodeY] -= conductance;
        a[nodeY][nodeX] -= conductance;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt, double time,
                            double[] previousValues) {
        // No source term
    }

    @Override
    public double calculateCurrent(double vx, double vy, double[] parameter,
                                  double dt, double previousCurrent) {
        double r0 = parameter[0];
        double alpha = parameter[1];
        double temp = parameter[2];
        double tempRef = parameter[3];

        double resistance = r0 * (1.0 + alpha * (temp - tempRef));
        return (vx - vy) / resistance;
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        // parameterValue is the effective resistance
        return 1.0 / parameterValue;
    }
}
```

**Step 2: Register in StamperRegistry**

```java
// In your simulation initialization
StamperRegistry registry = StamperRegistry.createDefault();
registry.register(CircuitTyp.LK_R, new TemperatureResistorStamper());
```

### Stateful Components: IStatefulStamper

Components that can change state (diodes, switches, thyristors) use `IStatefulStamper`:

```java
public interface IStatefulStamper extends IMatrixStamper {
    /** Check if component should turn on/off based on voltages/currents */
    void updateState(double vx, double vy, double current, double time);

    /** Returns true if state changed (matrix needs re-factorization) */
    boolean isStateChanged();

    /** Reset state-changed flag after solver convergence */
    void resetStateChange();

    /** Get current ON/OFF state */
    boolean isOn();

    /** Force a specific state */
    void setState(boolean on);

    /** Effective resistance (depends on state) */
    double getCurrentResistance();
}
```

**Example: Ideal Switch (On/Off)**

```java
public class IdealSwitchStamper implements IStatefulStamper {
    private boolean _isOn = false;
    private boolean _stateChanged = false;
    private static final double ON_RESISTANCE = 0.001;    // 1 mΩ
    private static final double OFF_RESISTANCE = 1e9;     // 1 GΩ

    @Override
    public void updateState(double vx, double vy, double current, double time) {
        // Check if switch should toggle based on external signal
        // (would be connected to a control input in practice)
    }

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt) {
        double resistance = _isOn ? ON_RESISTANCE : OFF_RESISTANCE;
        double conductance = 1.0 / resistance;

        a[nodeX][nodeX] += conductance;
        a[nodeY][nodeY] += conductance;
        a[nodeX][nodeY] -= conductance;
        a[nodeY][nodeX] -= conductance;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt, double time,
                            double[] previousValues) {
        // No source
    }

    @Override
    public double calculateCurrent(double vx, double vy, double[] parameter,
                                  double dt, double previousCurrent) {
        double resistance = _isOn ? ON_RESISTANCE : OFF_RESISTANCE;
        return (vx - vy) / resistance;
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        double resistance = _isOn ? ON_RESISTANCE : OFF_RESISTANCE;
        return 1.0 / resistance;
    }

    // IStatefulStamper methods
    @Override
    public boolean isStateChanged() {
        return _stateChanged;
    }

    @Override
    public void resetStateChange() {
        _stateChanged = false;
    }

    @Override
    public boolean isOn() {
        return _isOn;
    }

    @Override
    public void setState(boolean on) {
        boolean old = _isOn;
        _isOn = on;
        _stateChanged = (old != on);
    }

    @Override
    public double getCurrentResistance() {
        return _isOn ? ON_RESISTANCE : OFF_RESISTANCE;
    }
}
```

### Component Registration: CircuitTyp Enum

New circuit components must be registered in `CircuitTyp` enum:

```java
// File: circuit/circuitcomponents/CircuitTyp.java
public enum CircuitTyp implements AbstractComponentTyp {
    // ... existing types ...
    LK_R(1, ResistorCircuit.TYPE_INFO),          // Resistor
    LK_CUSTOM_NLRES(99, MyCustomNonlinearResistor.TYPE_INFO),  // Your component
}
```

---

## Testing Your Changes

GeckoCIRCUITS enforces test coverage and code quality standards.

### Running Tests

```bash
# Run all tests
mvn test

# Run single test class
mvn test -Dtest=GainCalculatorTest

# Generate JaCoCo coverage report (output: target/site/jacoco/index.html)
mvn clean test jacoco:report

# Check coverage meets 60% minimum for core packages
mvn verify
```

### Writing Calculator Tests

```java
// File: src/test/java/.../GainCalculatorTest.java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GainCalculatorTest {

    @Test
    public void testBasicGain() {
        GainCalculator calc = new GainCalculator(2.5);

        // Initialize input signal array
        calc._inputSignal[0] = new double[1];
        calc._inputSignal[0][0] = 4.0;

        calc.berechneYOUT(0.001);  // deltaT doesn't matter for gain

        assertEquals(10.0, calc._outputSignal[0][0], 1e-9);
    }

    @Test
    public void testZeroGain() {
        GainCalculator calc = new GainCalculator(0.0);
        calc._inputSignal[0] = new double[1];
        calc._inputSignal[0][0] = 100.0;

        calc.berechneYOUT(0.001);
        assertEquals(0.0, calc._outputSignal[0][0], 1e-9);
    }

    @Test
    public void testNegativeGain() {
        GainCalculator calc = new GainCalculator(-2.0);
        calc._inputSignal[0] = new double[1];
        calc._inputSignal[0][0] = 3.0;

        calc.berechneYOUT(0.001);
        assertEquals(-6.0, calc._outputSignal[0][0], 1e-9);
    }
}
```

### Writing Circuit Component Tests

```java
// File: src/test/java/.../ResistorStamperTest.java
public class ResistorStamperTest {

    @Test
    public void testStamping() {
        ResistorStamper stamper = new ResistorStamper();
        double[][] a = new double[3][3];
        double[] parameter = {100.0};  // 100 Ω

        stamper.stampMatrixA(a, 0, 1, 2, parameter, 0.001);

        // Verify conductance stamping (1/100 = 0.01)
        assertEquals(0.01, a[0][0], 1e-6);
        assertEquals(0.01, a[1][1], 1e-6);
        assertEquals(-0.01, a[0][1], 1e-6);
        assertEquals(-0.01, a[1][0], 1e-6);
    }

    @Test
    public void testCurrentCalculation() {
        ResistorStamper stamper = new ResistorStamper();
        double[] parameter = {50.0};  // 50 Ω

        double current = stamper.calculateCurrent(10.0, 5.0, parameter, 0.001, 0.0);

        assertEquals(0.1, current, 1e-6);  // (10-5)/50 = 0.1 A
    }
}
```

### Coverage Thresholds

Core packages must maintain **60%+ instruction coverage**:
- `circuit.matrix`
- `circuit.netlist`
- `circuit.simulation`
- `control.calculators`
- `math`
- `datacontainer`

Check `CorePackageValidationTest` for enforced boundaries—any Swing/AWT imports in these packages will fail the build.

---

## Code Quality

### Running Code Quality Checks

```bash
# SpotBugs (bug detection)
mvn spotbugs:check

# Checkstyle (coding standards)
mvn checkstyle:check

# PMD (code smell detection)
mvn pmd:check

# All checks
mvn verify
```

### Common Issues

| Issue | Fix |
|-------|-----|
| Public mutable fields | Use `@SuppressFBWarnings` with justification if performance-critical |
| GUI imports in core | Move to separate `*UI` class or GUI package |
| Missing unit tests | Minimum 60% coverage on core packages |
| Unused parameters | Prefix with `@SuppressWarnings("unused")` or remove |

### Style Guide

- **Naming**: `camelCase` for variables, `PascalCase` for classes
- **Constants**: `UPPER_SNAKE_CASE`
- **Methods**: Prefer descriptive names (`calculateCurrent` not `calc`)
- **Javadoc**: Required for public classes/methods
- **Access**: Package-private by default, public only if necessary

---

## Common Patterns

### 1. The Stamper Pattern

Used throughout the circuit domain. Each component type has a stamper that knows how to contribute to MNA matrices.

**Pattern:**
```java
public class XyzStamper implements IMatrixStamper {
    public void stampMatrixA(...) { /* Fill A */ }
    public void stampVectorB(...) { /* Fill b */ }
    public double calculateCurrent(...) { /* Compute I */ }
    public double getAdmittanceWeight(...) { /* Return weight */ }
}
```

**Benefits:** Decouples component logic from solver

### 2. The Calculator Pattern

Used for control blocks. Calculators transform input signals to output signals via `berechneYOUT()`.

**Pattern:**
```java
public class XyzCalculator extends AbstractSingleInputSingleOutputCalculator {
    public void berechneYOUT(final double deltaT) {
        _outputSignal[0][0] = f(_inputSignal[0][0]);
    }
}
```

**Benefits:** Simple state machine; easy to chain/compose

### 3. Signal Array Architecture

Signals are 2D arrays `double[port][value]`. Port 0 = first output, Port 1 = second, etc.

```java
// Single output (most calculators)
_outputSignal[0][0] = result;

// Multiple outputs (e.g., dq to abc transformer)
_outputSignal[0][0] = phase_a;  // Port 0
_outputSignal[1][0] = phase_b;  // Port 1
_outputSignal[2][0] = phase_c;  // Port 2
```

### 4. Registry Pattern

Components and stampers are registered dynamically:

```java
StamperRegistry registry = StamperRegistry.createDefault();
registry.register(CircuitTyp.LK_CUSTOM, new MyStamper());
IMatrixStamper stamper = registry.getStamper(CircuitTyp.LK_CUSTOM);
```

**Benefits:** Easy to mock, test, and swap implementations

### 5. State Management Pattern

Stateful components track internal state and signal changes:

```java
public class XyzCalculator extends AbstractSingleInputSingleOutputCalculator {
    private double _state = 0.0;
    private boolean _stateChanged = false;

    @Override
    public void berechneYOUT(final double deltaT) {
        _state += /* update */;
    }

    public void updateState(double newValue) {
        if (newValue != _state) {
            _state = newValue;
            _stateChanged = true;
        }
    }

    public void resetStateChange() {
        _stateChanged = false;
    }
}
```

---

## Troubleshooting

### Build Fails: "No GUI imports in core packages"

**Cause:** You imported `java.awt` or `javax.swing` in a core package

**Fix:** Move GUI code to separate `*UI` class in `allg/` or `gui/` package
```java
// Wrong
public class CalculatorUI extends JPanel { ... }  // in calculators/

// Right
public class CalculatorUIPanel extends JPanel { ... }  // in allg/ui/
```

### Test Coverage Below 60%

**Cause:** New code not tested

**Fix:** Add unit tests
```bash
mvn clean test jacoco:report  # View report in target/site/jacoco/index.html
```

### Stateful Component Not Converging

**Cause:** State changes not signaled properly

**Fix:** Ensure `isStateChanged()` returns true after state update
```java
@Override
public void updateState(double vx, double vy, double current, double time) {
    boolean oldState = _isOn;
    _isOn = (current > THRESHOLD);
    _stateChanged = (oldState != _isOn);  // Signal change!
}
```

### Calculator Output Always Zero

**Cause:** Input signal not connected

**Fix:** Initialize input signal array
```java
calc._inputSignal[0] = new double[1];
calc._inputSignal[0][0] = inputValue;
calc.berechneYOUT(dt);
```

---

## Next Steps

1. **Read the source code:** Browse `circuit/` and `control/calculators/` packages to understand patterns
2. **Try a simple extension:** Add a custom calculator (follow the gain/saturation examples)
3. **Run tests:** Verify your code meets coverage and style standards
4. **Contribute:** Submit a pull request with your changes

For architectural deep-dives, see:
- `.claude/journals/STRATEGIC_ROADMAP_DUAL_TRACK.md` - Long-term vision
- `.claude/journals/CORE_API_BOUNDARY.md` - GUI-free package documentation
- `.claude/journals/OPCODE_GUIDE.md` - Detailed development reference
