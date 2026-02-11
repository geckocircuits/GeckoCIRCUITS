---
title: "704 - Java Blocks"
---

# 704 - Java Blocks

Custom component creation using Java.

## Overview

GeckoCIRCUITS allows creating custom components using Java, enabling:
- Complex control algorithms
- Custom mathematical functions
- Interface to external systems
- Proprietary models

## Java Block Types

### Control Block

Custom signal processing in control domain:
- Inputs: Control signals
- Outputs: Control signals
- No direct power circuit connection

### Circuit Component

Custom power circuit element:
- Electrical terminals
- Current/voltage relationships
- Matrix stamping for solver

## Creating a Control Block

### Basic Structure

```java
package ch.technokrat.gecko.custom;

import ch.technokrat.gecko.geckocircuits.control.AbstractControlBlock;

public class MyCustomBlock extends AbstractControlBlock {

    // Parameters (user-editable)
    private double gain = 1.0;
    private double offset = 0.0;

    // Internal state
    private double previousValue = 0.0;

    @Override
    public void init() {
        // Called once at simulation start
        previousValue = 0.0;
    }

    @Override
    public double calculate(double[] inputs, double time) {
        // Called every time step
        double input = inputs[0];
        double output = gain * input + offset;

        // Optional: use previous value for filtering
        output = 0.9 * previousValue + 0.1 * output;
        previousValue = output;

        return output;
    }

    @Override
    public String[] getParameterNames() {
        return new String[]{"gain", "offset"};
    }

    @Override
    public void setParameter(String name, double value) {
        switch (name) {
            case "gain": gain = value; break;
            case "offset": offset = value; break;
        }
    }
}
```

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

## Compiling and Using

### Compilation

```bash
# Compile with GeckoCIRCUITS on classpath
javac -cp gecko.jar MyCustomBlock.java

# Create JAR
jar cf mycustom.jar MyCustomBlock.class
```

### Loading in GeckoCIRCUITS

1. Place JAR in `plugins/` directory
2. Restart GeckoCIRCUITS
3. Block appears in control component library
4. Drag to schematic and configure

### Direct Loading

```java
// In GeckoSCRIPT
loadJavaBlock("path/to/mycustom.jar", "MyCustomBlock");
```

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

## Best Practices

1. **Initialize all state** in `init()` method
2. **Avoid allocations** in `calculate()` - pre-allocate arrays
3. **Use `getTimeStep()`** for integration accuracy
4. **Handle edge cases** (NaN, Inf, division by zero)
5. **Document parameters** with clear names and units

## Related Resources

- [701 - GeckoSCRIPT](geckoscript.md)
- [API Reference](../../api/index.md)
