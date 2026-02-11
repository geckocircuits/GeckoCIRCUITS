---
title: Java Blocks
description: Create custom control blocks and circuit components using Java
---

# 704 - Java Blocks

Extend GeckoCIRCUITS with custom control blocks and circuit components written in Java. Java Blocks allow implementation of complex algorithms that compile and execute at runtime within the simulator.

## What are Java Blocks?

Java Blocks are custom components built from Java classes that integrate directly into the GeckoCIRCUITS schematic editor:

- **Control Blocks** - Pure signal processing (PID, filters, transforms, lookup tables)
- **Circuit Components** - Custom power elements with terminal definitions
- **Runtime Compilation** - Compiled on load, full performance of native Java
- **Full API Access** - Direct integration with the GeckoCIRCUITS simulation engine

!!! tip "When to Use Java Blocks"
    Java Blocks are ideal for complex algorithms that would be slow in GeckoSCRIPT, or for reusable components you want to encapsulate and distribute.

## Block Types

### Control Block

Pure signal processing - processes control-domain signals:

- **Inputs** - Scalar or vector control signals
- **Outputs** - Calculated control outputs
- **Applications** - PID controllers, filters, signal transforms, limiters, dead-bands
- **Performance** - Fast, no power circuit interaction
- **Example** - Custom PI controller, dq0 transform, lookup table

### Circuit Component

Custom power circuit element - connects to electrical network:

- **Terminals** - Electrical connection points to circuit
- **Behavior** - Defined by voltage/current relationships
- **Matrix Stamping** - Integrates into MNA matrix for solver
- **Applications** - Custom semiconductor models, nonlinear devices
- **Advanced** - Requires deeper API knowledge

This tutorial focuses on Control Blocks, which are more commonly used.

## Creating a Custom PI Controller

### Complete Example

Here's a custom PI (Proportional-Integral) controller block that can be added to any schematic:

```java
package ch.technokrat.gecko.custom;

import ch.technokrat.gecko.geckocircuits.control.AbstractControlBlock;

/**
 * Custom PI Controller Block
 * Inputs: error signal (reference - feedback)
 * Output: control signal
 * Parameters: Kp (proportional gain), Ki (integral gain)
 */
public class CustomPIController extends AbstractControlBlock {

    // Tuning parameters
    private double Kp = 1.0;      // Proportional gain
    private double Ki = 0.1;      // Integral gain

    // Internal state
    private double integralState = 0.0;
    private double saturationMin = -1.0;
    private double saturationMax = 1.0;

    @Override
    public void init() {
        // Reset integral state at simulation start
        integralState = 0.0;
    }

    @Override
    public double calculate(double[] inputs, double time) {
        double error = inputs[0];
        double dt = getTimeStep();

        // Proportional term
        double proportional = Kp * error;

        // Integral term (accumulate error over time)
        integralState += Ki * error * dt;

        // Anti-windup: clamp integral state
        if (integralState > saturationMax) {
            integralState = saturationMax;
        } else if (integralState < saturationMin) {
            integralState = saturationMin;
        }

        // Calculate output
        double output = proportional + integralState;

        // Output saturation
        if (output > saturationMax) output = saturationMax;
        if (output < saturationMin) output = saturationMin;

        return output;
    }

    @Override
    public String[] getParameterNames() {
        return new String[]{"Kp", "Ki", "satMax", "satMin"};
    }

    @Override
    public void setParameter(String name, double value) {
        switch (name) {
            case "Kp": Kp = value; break;
            case "Ki": Ki = value; break;
            case "satMax": saturationMax = value; break;
            case "satMin": saturationMin = value; break;
        }
    }

    @Override
    public String getBlockName() {
        return "Custom PI Controller";
    }
}
```

### Key Concepts

| Method | Purpose |
|--------|---------|
| `init()` | Called once at simulation start - initialize state variables |
| `calculate()` | Called every timestep - compute output from inputs |
| `getParameterNames()` | List editable parameters shown in GUI |
| `setParameter()` | Update parameters from GUI or scripts |
| `getTimeStep()` | Get current simulation timestep (use for integration) |

!!! warning "Important"
    Always reset state variables in `init()`. Use `getTimeStep()` for accurate integration. Never allocate new objects in `calculate()` - pre-allocate in constructor.

### Multiple Outputs

```java
public class MultiOutputBlock extends AbstractControlBlock {

    @Override
    public int getOutputCount() {
        return 3;  // Three outputs
    }

    @Override
    public double[] calculateMultiple(double[] inputs, double time) {
        double[] outputs = new double[3];

        // Example: dq0 transform
        double a = inputs[0];
        double b = inputs[1];
        double c = inputs[2];
        double theta = inputs[3];

        outputs[0] = 2.0/3.0 * (a * Math.cos(theta) +
                               b * Math.cos(theta - 2*Math.PI/3) +
                               c * Math.cos(theta + 2*Math.PI/3));
        outputs[1] = 2.0/3.0 * (-a * Math.sin(theta) -
                                b * Math.sin(theta - 2*Math.PI/3) -
                                c * Math.sin(theta + 2*Math.PI/3));
        outputs[2] = 1.0/3.0 * (a + b + c);

        return outputs;
    }
}
```

## State Variables

### Integrator Example

```java
public class CustomIntegrator extends AbstractControlBlock {

    private double state = 0.0;
    private double previousInput = 0.0;

    @Override
    public void init() {
        state = 0.0;
        previousInput = 0.0;
    }

    @Override
    public double calculate(double[] inputs, double time) {
        double dt = getTimeStep();
        double input = inputs[0];

        // Trapezoidal integration
        state += 0.5 * dt * (input + previousInput);
        previousInput = input;

        return state;
    }
}
```

### Discrete Filter

```java
public class DiscreteFilter extends AbstractControlBlock {

    private double[] xHistory = new double[3];
    private double[] yHistory = new double[3];

    // Filter coefficients (example: 2nd order Butterworth)
    private double[] b = {0.0675, 0.1349, 0.0675};
    private double[] a = {1.0, -1.1430, 0.4128};

    @Override
    public double calculate(double[] inputs, double time) {
        // Shift history
        xHistory[2] = xHistory[1];
        xHistory[1] = xHistory[0];
        xHistory[0] = inputs[0];

        yHistory[2] = yHistory[1];
        yHistory[1] = yHistory[0];

        // Difference equation
        yHistory[0] = b[0]*xHistory[0] + b[1]*xHistory[1] + b[2]*xHistory[2]
                    - a[1]*yHistory[1] - a[2]*yHistory[2];

        return yHistory[0];
    }
}
```

## Lookup Tables

```java
public class LookupTable extends AbstractControlBlock {

    private double[] xData;
    private double[] yData;

    @Override
    public void init() {
        // Load lookup table data
        xData = new double[]{0, 0.2, 0.4, 0.6, 0.8, 1.0};
        yData = new double[]{0, 0.1, 0.35, 0.7, 0.9, 1.0};
    }

    @Override
    public double calculate(double[] inputs, double time) {
        double x = inputs[0];

        // Linear interpolation
        return interpolate(xData, yData, x);
    }

    private double interpolate(double[] xArr, double[] yArr, double x) {
        // Clamp to range
        if (x <= xArr[0]) return yArr[0];
        if (x >= xArr[xArr.length-1]) return yArr[yArr.length-1];

        // Find interval
        int i = 0;
        while (i < xArr.length-1 && xArr[i+1] < x) i++;

        // Linear interpolation
        double t = (x - xArr[i]) / (xArr[i+1] - xArr[i]);
        return yArr[i] + t * (yArr[i+1] - yArr[i]);
    }
}
```

## Compiling and Installing

### Step 1: Compile

Compile your Java block class with the GeckoCIRCUITS JAR on the classpath:

```bash
# Navigate to your project directory
cd my_java_blocks/

# Compile (adjust path to gecko JAR as needed)
javac -cp /path/to/gecko-1.0-jar-with-dependencies.jar CustomPIController.java

# Verify compilation succeeded
ls CustomPIController.class
```

### Step 2: Package as JAR

Create a JAR file containing your compiled classes:

```bash
# Package single class
jar cf CustomPIController.jar CustomPIController.class

# Package entire directory with multiple blocks
jar cf myblocks.jar ch/technokrat/gecko/custom/*.class
```

### Step 3: Load into GeckoCIRCUITS

**Option A: Plugin Directory (Recommended)**

1. Place JAR in `<GeckoCIRCUITS_HOME>/plugins/` directory
2. Restart GeckoCIRCUITS
3. Your block appears in the control component library
4. Drag to schematic, right-click to configure parameters

**Option B: Runtime Loading**

Load dynamically from a GeckoSCRIPT:

```javascript
// In GeckoSCRIPT editor
loadJavaBlock("path/to/CustomPIController.jar", "CustomPIController");

// Now you can use it programmatically
// (block will appear in control palette)
```

!!! tip "Example Circuits"
    See these example circuits demonstrating Java Blocks:
    - `demo_JAVA_Block.ipes` - Simple Java Block demo
    - `JavaBlockPMSM.ipes` - PMSM control with Java Block PI controller

## Debugging

### Print Statements

```java
@Override
public double calculate(double[] inputs, double time) {
    if (time > 0.001 && time < 0.00101) {
        System.out.println("Input: " + inputs[0] + " at t=" + time);
    }
    // ...
}
```

### Logging

```java
import java.util.logging.*;

public class MyBlock extends AbstractControlBlock {
    private static final Logger log = Logger.getLogger(MyBlock.class.getName());

    @Override
    public double calculate(double[] inputs, double time) {
        log.fine("Processing at t=" + time);
        // ...
    }
}
```

## Performance Optimization

### Memory Allocation Pattern

Pre-allocate arrays in constructor, reuse in `calculate()`:

```java
public class FastVectorBlock extends AbstractControlBlock {
    private double[] state;    // Pre-allocated
    private double[] tempVec;  // Working array

    public FastVectorBlock() {
        state = new double[3];
        tempVec = new double[3];
    }

    @Override
    public double calculate(double[] inputs, double time) {
        // Reuse arrays - no new allocations per timestep
        System.arraycopy(inputs, 0, tempVec, 0, 3);
        // ... process ...
        return result;
    }
}
```

### Integration Best Practices

| Method | Accuracy | Stability | Use Case |
|--------|----------|-----------|----------|
| Forward Euler | Low | Poor | Testing |
| Backward Euler | Low | Good | Stiff systems |
| Trapezoidal | Medium | Good | General purpose |

```java
// Trapezoidal integration (best general purpose)
public double calculate(double[] inputs, double time) {
    double dt = getTimeStep();
    double input = inputs[0];

    // Trapezoidal rule
    state += 0.5 * dt * (input + previousInput);
    previousInput = input;

    return state;
}
```

## Best Practices

- Initialize all state in `init()` - don't assume zero state
- Avoid object allocation in `calculate()` - pre-allocate in constructor
- Use `getTimeStep()` for accurate integration
- Handle edge cases (NaN, Inf, division by zero)
- Document parameters with clear units (V, A, s, Hz, etc.)
- Implement anti-windup in integral controllers to prevent saturation issues
- Test with short simulations first to verify correctness

## Debugging Java Blocks

### Print Statements

```java
@Override
public double calculate(double[] inputs, double time) {
    if (time > 0.001 && time < 0.00101) {
        System.out.println("Input: " + inputs[0] + " State: " + state);
    }
    // ...
}
```

### Logging

```java
import java.util.logging.*;

public class MyBlock extends AbstractControlBlock {
    private static final Logger log = Logger.getLogger(MyBlock.class.getName());

    @Override
    public double calculate(double[] inputs, double time) {
        log.fine("Processing input: " + inputs[0]);
        // ...
    }
}
```

Output appears in the GeckoCIRCUITS console/logs.

## Resources

- Tutorial files: `resources/tutorials/7xx_scripting_automation/704_java_blocks/`
- Example circuits: `demo_JAVA_Block.ipes`, `JavaBlockPMSM.ipes`
- API Javadoc: See `src/main/java/ch/technokrat/gecko/geckocircuits/control/AbstractControlBlock.java`

## Related Tutorials

- [701 - GeckoSCRIPT Basics](geckoscript.md)
- [702 - MATLAB Integration](matlab.md)
- [706 - Python Integration](python.md)
