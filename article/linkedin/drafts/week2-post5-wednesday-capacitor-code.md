# LinkedIn Post #5 (Wednesday) - Week 2

**Publishing Date:** Week 2, Wednesday 8am
**Topic:** Capacitor Stamping Code Preview
**Target:** Engineers wanting to see time-dependent stamping implementation
**Goal:** Show actual code, explain time dependency, drive to weekend article
**Word Count:** 487 words

---

## Post Content

Here's how capacitors stamp into the MNA matrix. Notice what's different from resistors: the time step dt is everywhere.

Monday I explained why capacitors are tricky: I = C·dV/dt is a differential equation, not algebraic.

Today I'm showing you the actual GeckoCIRCUITS code that handles it.

**Capacitor Stamping with Backward Euler:**

```java
// From: CapacitorStamper.java
public class CapacitorStamper implements IMatrixStamper {

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt) {
        // parameter[0] = capacitance value
        double capacitance = parameter[0];

        // Key difference: admittance depends on time step!
        // Backward Euler: G_eq = C / dt
        double admittance = capacitance / dt;

        // Stamp like a resistor, but with time-dependent conductance
        a[nodeX][nodeX] += admittance;
        a[nodeY][nodeY] += admittance;
        a[nodeX][nodeY] -= admittance;
        a[nodeY][nodeX] -= admittance;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt, double time,
                            double[] previousValues) {
        double capacitance = parameter[0];
        double admittance = capacitance / dt;

        // History term: current source from previous time step
        // I_history = (C/dt) * V_old
        double V_old_X = previousValues[nodeX];
        double V_old_Y = previousValues[nodeY];
        double historyCurrent = admittance * (V_old_X - V_old_Y);

        // Stamp as current source into b vector
        b[nodeX] += historyCurrent;
        b[nodeY] -= historyCurrent;
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt,
                                   double previousCurrent) {
        double capacitance = parameter[0];
        // I = C * dV/dt ≈ C * (V_new - V_old) / dt
        double V_new = nodeVoltageX - nodeVoltageY;
        double V_old = previousCurrent; // stored as "current" for convenience
        return capacitance * (V_new - V_old) / dt;
    }
}
```

**What's Happening:**

**stampMatrixA:**
- Admittance = C/dt (not constant like resistors!)
- Smaller time step → larger admittance → "stiffer" component
- Larger time step → smaller admittance → more like open circuit

**stampVectorB:**
- Uses previousValues from last time step
- History term represents "inertia" of the capacitor
- This is why circuit simulators need to store state between time steps

**Why dt Matters:**

Resistor: G = 1/R (constant, doesn't depend on dt)
Capacitor: G_eq = C/dt (changes every time dt changes!)

If you use adaptive time stepping:
- dt changes → admittance changes → matrix A changes → must rebuild and re-solve

This is the computational cost of time-dependent components.

**Trapezoidal Rule (Alternative):**

Backward Euler is first-order accurate. Trapezoidal rule is second-order:

G_eq = 2·C/dt

History term is more complex (involves both V_old and I_old).

We'll cover this in the weekend article.

**Friday Preview:**

I'll hype the weekend deep-dive where we'll cover:
→ Resistor, capacitor, and inductor stamping
→ Backward Euler vs Trapezoidal comparison
→ When to use which integration method
→ Complete worked examples
→ Exercises you can try

If you want to understand time integration in circuit simulation, subscribe to my Substack. Article drops Saturday.

Source: github.com/geckocircuits/GeckoCIRCUITS
File: CapacitorStamper.java:45-95

---

**Hashtags:**
#CircuitSimulation #BackwardEuler #TimeIntegration #Java #NumericalAnalysis

**CTA:** Subscribe to Substack for weekend deep-dive

**Source File References:**
- CapacitorStamper.java:45-95
- IStatefulStamper.java:12-30

**Metrics:**
- Word count: 487
- Reading time: 2.5 min
- Code snippets: 1 (complete CapacitorStamper class)
- Numerical method: Backward Euler
