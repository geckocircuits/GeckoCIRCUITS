# LinkedIn Post #2 (Wednesday) - Week 1

**Publishing Date:** Week 1, Wednesday 8am
**Topic:** Code Preview - IMatrixStamper Interface
**Target:** Engineers wanting to see implementation
**Goal:** Show actual code, build curiosity for weekend article
**Word Count:** 476 words

---

## Post Content

Matrix 'stamping' is the coolest name for adding numbers to a matrix. Here's how GeckoCIRCUITS does it.

Monday I showed you the MNA equation: A·x = b

Today I'm showing you the actual code that builds that matrix.

This is from GeckoCIRCUITS - open-source power electronics simulator, 20+ years of development, used in universities and industry.

Here's the interface every component must implement:

```java
// From IMatrixStamper.java
public interface IMatrixStamper {

    // Stamp conductance/admittance into A matrix
    void stampMatrixA(double[][] a,
                     int nodeX, int nodeY, int nodeZ,
                     double[] parameter, double dt);

    // Stamp sources into b vector
    void stampVectorB(double[] b,
                     int nodeX, int nodeY, int nodeZ,
                     double[] parameter, double dt,
                     double time, double[] previousValues);

    // Calculate current after solving
    double calculateCurrent(double nodeVoltageX,
                          double nodeVoltageY,
                          double[] parameter, double dt,
                          double previousCurrent);
}
```

Beautiful, right?

Every component (resistor, capacitor, diode, switch) implements this interface.

The simulation engine doesn't know about component details. It just calls:
- component.stampMatrixA(A, ...)
- component.stampVectorB(b, ...)
- Solve A·x = b
- component.calculateCurrent(...)

This is the Strategy pattern in action.
Clean separation. Easy to extend. Add new component? Implement the interface.

Let me show you a resistor:

```java
// From ResistorStamper.java (simplified)
public void stampMatrixA(double[][] a, int nodeX, int nodeY, ...) {
    double conductance = 1.0 / resistance;

    // The 4-entry stamp pattern:
    a[nodeX][nodeX] += conductance;  // Node X self-admittance
    a[nodeY][nodeY] += conductance;  // Node Y self-admittance
    a[nodeX][nodeY] -= conductance;  // Coupling X→Y
    a[nodeY][nodeX] -= conductance;  // Coupling Y→X
}
```

That's it. 4 lines. That's how a resistor stamps into the MNA matrix.

Why this pattern? It represents Kirchhoff's Current Law:
- Current into X = conductance · (Vx - Vy)
- Current into Y = conductance · (Vy - Vx)
- Conservation: I_X + I_Y = 0

Symmetric. Elegant. Correct.

But wait... what about capacitors? They're time-dependent!
And diodes? They're nonlinear!
And switches? They change topology!

That's what makes circuit simulation interesting.

Friday: I'll preview the weekend deep-dive article where we walk through:
→ Complete MNA theory
→ Resistor, capacitor, inductor stamping
→ Code walkthroughs line-by-line
→ Example circuit you can build
→ Exercises to test your understanding

If you want to understand circuit simulators at the code level, subscribe to my Substack. Article drops Saturday.

Source: github.com/geckocircuits/GeckoCIRCUITS (open-source!)
Files: IMatrixStamper.java, ResistorStamper.java

---

**Hashtags:**
#CircuitSimulation #SoftwareArchitecture #PowerElectronics #Java #DesignPatterns

**CTA:** Subscribe to Substack before Saturday's article

**Source File References:**
- IMatrixStamper.java:35-96
- ResistorStamper.java:45-52

**Metrics:**
- Word count: 476
- Reading time: 2.5 min
- Code snippets: 2 (interface + resistor implementation)
- Design pattern: Strategy
