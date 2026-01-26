# Modified Nodal Analysis: From Kirchhoff to Code

**Subtitle:** Learn how SPICE, PLECS, and GeckoCIRCUITS simulate circuits by reading actual source code

**Reading Time:** 15 minutes
**Difficulty:** Intermediate
**Prerequisites:** Basic circuit theory, linear algebra, programming experience

---

## Executive Summary

Every circuit simulator - SPICE, PLECS, GeckoCIRCUITS - uses the same mathematical foundation: Modified Nodal Analysis (MNA). If you've ever wondered how these tools can simulate a 1000-component circuit in seconds, this is where it starts.

**What You'll Learn:**

This article walks you through MNA from first principles to actual implementation. You'll see:
- The mathematical theory behind MNA and why it's superior to mesh or classical nodal analysis
- How GeckoCIRCUITS implements MNA in Java using the Strategy design pattern
- Complete code walkthroughs with line-by-line explanations
- The "stamping" pattern that makes circuit simulation systematic and scalable
- Worked examples you can verify by hand and in code

**Who This Is For:**

- Power electronics engineers wanting to understand their simulation tools
- Students learning circuit simulation algorithms
- Researchers needing to implement custom component models
- Developers contributing to open-source circuit simulators

**Prerequisites:**

- Basic circuit theory (Ohm's law, Kirchhoff's Current Law, Kirchhoff's Voltage Law)
- Linear algebra (matrix-vector operations, solving linear systems)
- Programming experience (helpful but not required)

**Time Investment:** 15 minutes reading + 30 minutes trying exercises

By the end, you'll understand how to build a basic circuit simulator from scratch. You'll be able to read GeckoCIRCUITS source code, implement custom components, and know exactly what happens when you click "Run Simulation."

Let's dive in.

---

## The Problem: Analyzing Circuits at Scale

**Hand Analysis Doesn't Scale**

Analyzing a simple 2-resistor voltage divider by hand is trivial. Write Kirchhoff's Voltage Law (KVL) around the loop, apply Ohm's law, solve for voltages and currents. Takes maybe 30 seconds.

But what about:
- A 100-resistor network with multiple voltage sources?
- A switched-mode power supply with 50 components including capacitors, inductors, diodes, and MOSFETs?
- A motor drive circuit with IGBTs, snubbers, control loops, and parasitic elements?

Hand analysis becomes impossible. Even setting up the equations takes hours. Solving them by hand? Forget it. We need a systematic approach that:

1. Works for any circuit topology (no matter how complex)
2. Handles any number of components (scales to thousands)
3. Can be automated (programmed into software)
4. Extends naturally to time-varying (transient) analysis

**Classical Methods Fall Short**

Circuit analysis textbooks teach two classical methods:

**Mesh Analysis:**
- Write KVL around each independent loop
- Solve for loop currents
- Problem: How do you identify independent loops? Non-planar circuits have many choices.
- Number of equations = number of independent loops (hard to count systematically!)
- Doesn't handle current sources gracefully (requires transformation)

**Classical Nodal Analysis:**
- Apply Kirchhoff's Current Law (KCL) at each node (except ground)
- Use conductance form: I = G·V where G = 1/R
- Number of equations = number of nodes - 1 (easy to count!)
- Problem: Can't handle voltage sources or inductors directly

Voltage sources break classical nodal analysis because you can't write I = G·V for a voltage source - the voltage is known, the current is unknown, and there's no resistance to form a conductance.

**Modified Nodal Analysis (MNA): The Solution**

MNA combines the best of both approaches:
- Start with nodal analysis (KCL at each node) - systematic and easy to set up
- Add branch currents as additional unknowns for voltage sources and inductors
- Result: A systematic method that handles all component types

**Why MNA Won:**

- **Sparse matrices:** Most real circuits have 5-10 connections per node, not N connections for N nodes. Sparse matrix storage saves 100x memory.
- **Easy to automate:** Every component "stamps" its contribution independently. No topology analysis required.
- **Handles all components:** Resistors, capacitors, inductors, voltage sources, current sources, nonlinear devices.
- **Extends naturally:** Time-varying components (C, L) and nonlinear components (diodes, transistors) fit the same framework.
- **Industry standard:** SPICE adopted MNA in the 1970s. Every simulator since has followed.

That's why every circuit simulator uses MNA. It's the right abstraction. Let's see how it works.

---

## MNA Theory: The A·x = b Formulation

**The Core Equation**

Modified Nodal Analysis reduces circuit analysis to solving a linear system:

**A · x = b**

Where:
- **A** = System matrix (conductances, admittances, circuit topology)
- **x** = Unknown vector (node voltages + branch currents for voltage sources/inductors)
- **b** = Source vector (independent current sources and voltage source values)

Let's unpack each piece with a concrete example.

**Step 1: Number the Nodes**

Consider this simple circuit:

```
Example circuit:

Vin=10V ----[R1=10Ω]---- node1 ----[R2=20Ω]---- GND
                            |
                         [R3=5Ω]
                            |
                           GND
```

Nodes:
- **node0** = GND (reference, voltage = 0V by definition)
- **node1** = unknown voltage (this is what we're solving for)
- **Vin** = voltage source node (treated specially)

We have 1 unknown node voltage: V1

**Step 2: Apply KCL at Each Unknown Node**

At node1, current flowing in equals current flowing out (Kirchhoff's Current Law):

**I_from_R1 = I_to_R2 + I_to_R3**

Using conductance form (I = G·ΔV where G = 1/R):

**G1·(Vin - V1) = G2·(V1 - 0) + G3·(V1 - 0)**

Rearranging terms:

```
G1·Vin - G1·V1 = G2·V1 + G3·V1
G1·Vin = G1·V1 + G2·V1 + G3·V1
G1·Vin = (G1 + G2 + G3)·V1
```

This is our MNA equation for this circuit!

**Step 3: Form the Matrix Equation**

For this simple circuit:
- 1 unknown: V1
- 1 equation: KCL at node1

In matrix form:

```
[ G1 + G2 + G3 ] [ V1 ] = [ G1·Vin ]
```

Substituting values (R1=10Ω → G1=0.1S, R2=20Ω → G2=0.05S, R3=5Ω → G3=0.2S):

```
[ 0.1 + 0.05 + 0.2 ] [ V1 ] = [ 0.1 · 10 ]
[      0.35        ] [ V1 ] = [   1.0   ]
```

Solve: **V1 = 1.0 / 0.35 = 2.857V**

That's MNA! Build the matrix A, build the vector b, solve for x.

**Stamping Pattern: The Key Insight**

Notice how each resistor contributed to the matrix **independently**:

**R1** (between Vin and node1):
- Adds G1 to the diagonal element (self-admittance of node1)
- Subtracts G1 from off-diagonal (coupling between Vin and node1)
- Contributes G1·Vin to the source vector

**R2** (between node1 and GND):
- Adds G2 to node1 diagonal (self-admittance)
- GND is reference (doesn't appear in equations)

**R3** (between node1 and GND):
- Adds G3 to node1 diagonal (self-admittance)

This **independent contribution** is called "stamping." Each component stamps its contribution into the system matrices without knowing about other components.

**General Stamping Rule for Resistor R Between Nodes i and j:**

```
A[i][i] += 1/R    (self-admittance at node i)
A[j][j] += 1/R    (self-admittance at node j)
A[i][j] -= 1/R    (coupling i→j)
A[j][i] -= 1/R    (coupling j→i)
```

This is a symmetric pattern. Always 4 entries (unless one node is ground).

**Physical Meaning:**

These 4 entries encode Kirchhoff's Current Law:
- Current into node i from this resistor: I_i = G·(V_i - V_j)
- Current into node j from this resistor: I_j = G·(V_j - V_i)
- Conservation: I_i + I_j = 0 (current in equals current out)

The matrix formulation automatically satisfies KCL!

**Why "Modified"? Handling Voltage Sources**

Classical nodal analysis struggles with voltage sources because:
- We can't write I = G·V (voltage sources have no conductance)
- The voltage is **known**, not unknown!
- But the **current** through the voltage source is unknown

MNA solution: **Add the branch current as an additional unknown.**

Voltage source V_s between nodes i and j:
- **Augment x:** Add variable I_Vs (current through voltage source)
- **Add constraint equation:** V_i - V_j = V_s (the voltage is fixed)
- **Update KCL:** The branch current I_Vs affects KCL at nodes i and j

This extends the matrix from n×n to (n+1)×(n+1):

```
Original circuit with n nodes:
  → n-1 unknowns (excluding ground)
  → n-1 equations (KCL at each node)

Add one voltage source:
  → n unknowns (n-1 voltages + 1 branch current)
  → n equations (n-1 KCL equations + 1 voltage constraint)
```

**Inductors Get Similar Treatment**

Inductor equation: **V = L · dI/dt**

In the time domain, this is a differential equation. But in MNA:
- Add branch current I_L as an unknown
- Constraint equation: V_i - V_j = L·dI_L/dt
- Time integration converts this to algebraic form (we'll cover this next week)

**Final A·x = b Structure**

For a general circuit with n nodes and m voltage sources/inductors:

**A matrix:** (n+m) × (n+m)
- Upper-left block: Nodal conductances (from resistors, capacitors)
- Upper-right block: Voltage source/inductor topology (-1, +1 entries)
- Lower-left block: Transpose of upper-right (symmetry)
- Lower-right block: Zeros (no self-coupling of branch currents)

**x vector:** (n+m) × 1
- First n entries: Node voltages [V1, V2, ..., Vn]
- Last m entries: Branch currents [I_V1, I_L1, ...]

**b vector:** (n+m) × 1
- First n entries: Current source contributions
- Last m entries: Voltage source values [V_s1, V_s2, ...]

Solve **A·x = b** → extract all voltages and branch currents. Done!

That's MNA in a nutshell. Elegant, systematic, automatable.

Now let's see how GeckoCIRCUITS implements this in code.

---

## Implementation in GeckoCIRCUITS

**Component Strategy: The IMatrixStamper Interface**

GeckoCIRCUITS uses the **Strategy design pattern**. Every component (resistor, capacitor, inductor, diode, etc.) implements the same interface. The simulation engine doesn't need to know about specific component types - it just calls the interface methods.

Here's the complete interface:

```java
// From: src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/IMatrixStamper.java

/**
 * Strategy interface for stamping component contributions into MNA matrices.
 *
 * In Modified Nodal Analysis (MNA), each circuit component "stamps" its
 * contribution into the system matrices. This interface defines the
 * contract for different stamping strategies.
 *
 * The MNA equation is: A * x = b
 * where:
 * - A is the admittance/conductance matrix
 * - x is the vector of unknowns (node voltages, branch currents)
 * - b is the source/excitation vector
 */
public interface IMatrixStamper {

    /**
     * Stamps the component's contribution into the A matrix.
     *
     * For a resistor R between nodes x and y:
     * - a[x][x] += 1/R
     * - a[y][y] += 1/R
     * - a[x][y] -= 1/R
     * - a[y][x] -= 1/R
     *
     * @param a the A matrix to stamp into
     * @param nodeX first node index
     * @param nodeY second node index
     * @param nodeZ auxiliary node index (for voltage sources, inductors)
     * @param parameter component parameters array
     * @param dt time step size
     */
    void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                      double[] parameter, double dt);

    /**
     * Stamps the component's contribution into the b vector (sources).
     *
     * For a current source I from node x to y:
     * - b[x] -= I
     * - b[y] += I
     *
     * @param b the b vector to stamp into
     * @param nodeX first node index
     * @param nodeY second node index
     * @param nodeZ auxiliary node index
     * @param parameter component parameters array
     * @param dt time step size
     * @param time current simulation time
     * @param previousValues array of previous state values
     */
    void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                      double[] parameter, double dt, double time,
                      double[] previousValues);

    /**
     * Calculates the component current after matrix solution.
     *
     * @param nodeVoltageX voltage at node X
     * @param nodeVoltageY voltage at node Y
     * @param parameter component parameters array
     * @param dt time step size
     * @param previousCurrent previous current value
     * @return calculated component current
     */
    double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                           double[] parameter, double dt, double previousCurrent);

    /**
     * Gets the admittance weight factor for this component type.
     *
     * @param parameterValue primary parameter (e.g., R, C, L)
     * @param dt time step size
     * @return admittance weight (e.g., 1/R, C/dt, dt/L)
     */
    double getAdmittanceWeight(double parameterValue, double dt);
}
```

**Why This Design Is Brilliant:**

1. **Polymorphism:** Every component implements the same interface → simulation engine treats all components uniformly
2. **Open/Closed Principle:** Adding a new component type doesn't require changing the simulation engine code
3. **Testability:** Each stamper can be unit-tested independently
4. **Maintainability:** Component logic is isolated in its own class

This is textbook software engineering applied to circuit simulation.

**Resistor Implementation: The Simplest Stamper**

Let's see the actual implementation for a resistor:

```java
// From: src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/ResistorStamper.java
// (Simplified for clarity)

public class ResistorStamper implements IMatrixStamper {

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt) {
        // parameter[0] contains the resistance value
        double resistance = parameter[0];
        double conductance = 1.0 / resistance;

        // Stamp the 4-entry pattern for resistor between nodeX and nodeY
        // This implements: I = G * (V_nodeX - V_nodeY)
        a[nodeX][nodeX] += conductance;  // Self-admittance at nodeX
        a[nodeY][nodeY] += conductance;  // Self-admittance at nodeY
        a[nodeX][nodeY] -= conductance;  // Mutual coupling nodeX → nodeY
        a[nodeY][nodeX] -= conductance;  // Mutual coupling nodeY → nodeX
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt, double time,
                            double[] previousValues) {
        // Resistors have no independent sources
        // Nothing to stamp into the b vector
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt,
                                   double previousCurrent) {
        double resistance = parameter[0];
        // Ohm's law: I = (V_X - V_Y) / R
        return (nodeVoltageX - nodeVoltageY) / resistance;
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        // For resistors: admittance = conductance = 1/R
        return 1.0 / parameterValue;
    }
}
```

**Code Walkthrough:**

**stampMatrixA method:**
- Receives the A matrix (passed by reference - Java arrays are mutable)
- Receives node indices (nodeX, nodeY)
- Receives component parameters (resistance value in parameter[0])
- Computes conductance G = 1/R
- Stamps 4 entries into A following the pattern we derived earlier
- These 4 entries encode KCL: current in = current out

**stampVectorB method:**
- Resistors have no independent sources → nothing to add to b vector
- Other components (current sources, voltage sources) use this method

**calculateCurrent method:**
- Called **after** solving A·x = b
- Receives the computed node voltages
- Applies Ohm's law: I = ΔV / R
- Returns the current flowing through the resistor

**getAdmittanceWeight method:**
- Helper method returning the admittance (conductance for resistors)
- Used for matrix conditioning and numerical stability checks

**How the Simulation Engine Uses Stampers**

Here's simplified pseudocode showing how GeckoCIRCUITS assembles and solves the MNA system:

```java
// Simplified simulation loop
// (Actual code is in SimulationsKern.java and related classes)

// Step 1: Initialize A matrix and b vector
double[][] A = new double[numNodes][numNodes];
double[] b = new double[numNodes];

// Step 2: Loop through all components and stamp
for (Component component : circuit.getAllComponents()) {
    // Polymorphism: each component has its own stamper implementation
    IMatrixStamper stamper = component.getStamper();

    // Call stampMatrixA - adds to A matrix
    stamper.stampMatrixA(
        A,
        component.getNodeX(),
        component.getNodeY(),
        component.getNodeZ(),
        component.getParameters(),
        dt
    );

    // Call stampVectorB - adds to b vector
    stamper.stampVectorB(
        b,
        component.getNodeX(),
        component.getNodeY(),
        component.getNodeZ(),
        component.getParameters(),
        dt,
        currentTime,
        previousStateValues
    );
}

// Step 3: Solve the linear system A*x = b
double[] x = matrixSolver.solve(A, b);
// (Uses Cholesky decomposition or other solver - covered Week 3)

// Step 4: Extract node voltages from solution vector
for (int i = 0; i < numNodes; i++) {
    nodeVoltages[i] = x[i];
}

// Step 5: Calculate component currents
for (Component component : circuit.getAllComponents()) {
    IMatrixStamper stamper = component.getStamper();

    double current = stamper.calculateCurrent(
        nodeVoltages[component.getNodeX()],
        nodeVoltages[component.getNodeY()],
        component.getParameters(),
        dt,
        component.getPreviousCurrent()
    );

    component.setCurrent(current);
}

// Step 6: Update scope/waveform data, advance time, repeat
```

**Key Observations:**

1. **Generic loop:** No if-statements checking component types. Works for any component that implements IMatrixStamper.
2. **Independent stamping:** Each component stamps without knowing about others. Order doesn't matter (matrix addition is commutative).
3. **Two-phase process:** First build and solve the system (steps 1-4), then calculate derived quantities like currents (step 5).
4. **Extensibility:** To add a new component type:
   - Create a class implementing IMatrixStamper
   - Register it with the component factory
   - Done! No changes to simulation engine code.

This architecture is what makes GeckoCIRCUITS maintainable after 20+ years of development.

---

## Worked Example: 3-Resistor Circuit

Let's solve a complete example by hand, then verify with the stamping algorithm.

**Circuit:**

```
Vin=10V ----[R1=10Ω]---- node1 ----[R2=20Ω]---- GND
                            |
                         [R3=5Ω]
                            |
                           GND
```

**Given:**
- Vin = 10V (voltage source)
- R1 = 10Ω
- R2 = 20Ω
- R3 = 5Ω

**Find:** V1, I_R1, I_R2, I_R3

### Step 1: Set Up the MNA Equation

Unknowns:
- V1 (voltage at node1)
- V_gnd = 0 (by definition)

Apply KCL at node1:

**I_from_R1 = I_to_R2 + I_to_R3**

Using conductance form:

**G1·(Vin - V1) = G2·(V1 - 0) + G3·(V1 - 0)**

Where:
- G1 = 1/R1 = 1/10 = 0.1 S (siemens)
- G2 = 1/R2 = 1/20 = 0.05 S
- G3 = 1/R3 = 1/5 = 0.2 S

Expand:

```
0.1·(10 - V1) = 0.05·V1 + 0.2·V1
1.0 - 0.1·V1 = 0.25·V1
1.0 = 0.1·V1 + 0.25·V1
1.0 = 0.35·V1
```

### Step 2: Form the Matrix Equation

**A·x = b:**

```
[ 0.35 ] [ V1 ] = [ 1.0 ]
```

(This is a 1×1 system because we have only 1 unknown)

### Step 3: Solve

**V1 = 1.0 / 0.35 = 2.857 V**

### Step 4: Calculate Currents

Using Ohm's law:

**I_R1 = (Vin - V1) / R1 = (10 - 2.857) / 10 = 7.143 / 10 = 0.7143 A**

**I_R2 = (V1 - 0) / R2 = 2.857 / 20 = 0.1429 A**

**I_R3 = (V1 - 0) / R3 = 2.857 / 5 = 0.5714 A**

### Step 5: Verify KCL

Check: I_R1 should equal I_R2 + I_R3

**0.7143 =? 0.1429 + 0.5714**
**0.7143 = 0.7143** ✓

KCL is satisfied! Our solution is correct.

### Stamping Algorithm: Building the Matrix Step-by-Step

Now let's see exactly how the matrix is assembled using stamping:

**Initial state:**
```
A = [ 0 ]
b = [ 0 ]
```

**R1 stamps** (between Vin and node1):

Since Vin is a voltage source, it's not in the A matrix for this simplified example. R1 contributes:
```
A[1][1] += G1 = 0.1
b[1] += G1·Vin = 0.1·10 = 1.0

Result:
A = [ 0.1 ]
b = [ 1.0 ]
```

**R2 stamps** (between node1 and GND):

GND is reference (index 0, not in matrix). Only node1 side stamps:
```
A[1][1] += G2 = 0.05

Result:
A = [ 0.15 ]
b = [ 1.0 ]
```

**R3 stamps** (between node1 and GND):
```
A[1][1] += G3 = 0.2

Result:
A = [ 0.35 ]
b = [ 1.0 ]
```

**Final system:**
```
[ 0.35 ] [ V1 ] = [ 1.0 ]
```

**Solve:** V1 = 1.0 / 0.35 = 2.857 V ✓

Same answer! This is MNA stamping in action. Each component contributes independently, building up the system matrix piece by piece.

**Exercise:** Build this circuit in GeckoCIRCUITS and verify:
- V1 = 2.857 V
- I_R1 = 0.7143 A
- I_R2 = 0.1429 A
- I_R3 = 0.5714 A

---

## Advanced Topics Preview

We've covered the basics of MNA for resistive (DC) circuits. But real circuit simulation involves much more:

**Time-Varying Components (Coming Next Week)**

Capacitors: **I = C · dV/dt**
Inductors: **V = L · dI/dt**

These are differential equations! You can't directly stamp them into an algebraic A matrix. The solution: **time integration methods**.

- Backward Euler: First-order accurate, very stable
- Trapezoidal (second-order): Better accuracy, can oscillate
- Gear-Shichman: Variable order, adaptive

The stamping pattern changes based on the integration method. Capacitors become time-dependent resistors with history terms. We'll dive deep into this next week.

**Nonlinear Components (Week 4)**

Diodes: **I = Is · (e^(V/Vt) - 1)**
MOSFETs: **I = f(Vgs, Vds)** (complex nonlinear function)

You can't directly stamp a nonlinear function into a linear matrix A. The solution: **Newton-Raphson iteration**.

Algorithm:
1. Linearize the nonlinear component around current operating point
2. Stamp the linearized conductance
3. Solve A·x = b
4. Check convergence
5. If not converged, update operating point and repeat

This is why SPICE simulations sometimes fail to converge!

**Switching Components (Week 4)**

Ideal switch:
- R = 0Ω when closed (on)
- R = ∞Ω when open (off)

Problem: Matrix A changes when switch state changes → need to rebuild and re-solve.

Solution: **Event-driven simulation**. Detect switching events, rebuild matrix, continue.

**Sparse Matrices (Week 3)**

Real circuits are sparsely connected:
- 1000-node circuit might have ~5000 non-zero entries
- Dense matrix would have 1,000,000 entries
- Sparse storage: 100x memory savings!

Cholesky decomposition on sparse matrices → fast solve even for large circuits.

**Voltage Sources and Inductors (Week 2)**

Require augmenting the x vector with branch currents. Matrix structure becomes:

```
[ G    B^T ] [ v ]   [ i_s ]
[ B     0  ] [ j ] = [ e_s ]
```

Where:
- G = conductance matrix
- B = incidence matrix for voltage sources/inductors
- v = node voltages
- j = branch currents
- e_s = voltage source values

Still uses stamping, just with a more complex pattern.

All of these advanced topics build on the MNA foundation you learned today. The core idea - stamping component contributions into a system matrix - remains the same.

---

## Exercises

**Exercise 1: Simple Voltage Divider**

Circuit:
```
Vin=12V ----[R1=1kΩ]---- node1 ----[R2=2kΩ]---- GND
```

Tasks:
a) Write the MNA matrix equation
b) Solve for V1 by hand
c) Calculate I_R1 and I_R2
d) Verify in GeckoCIRCUITS or by inspection using voltage divider formula

**Exercise 2: Bridge Circuit**

Circuit:
```
        Vin=10V
          |
      +---+---+
      |       |
     R1      R2
     10Ω     20Ω
      |       |
    node1   node2
      |       |
     R3      R4
     30Ω     40Ω
      |       |
      +---+---+
          |
         GND
```

Tasks:
a) Set up MNA equations for node1 and node2 (apply KCL at each node)
b) Form the 2×2 matrix system
c) Solve for V1 and V2
d) Is the bridge balanced? (A bridge is balanced when R1/R2 = R3/R4)

**Exercise 3: RC Circuit (Challenge - Preview of Next Week)**

Circuit:
```
Vin=5V ----[R=1kΩ]---- node1 ----[C=10µF]---- GND
```

Tasks:
a) Write the time-domain differential equation (hint: I_R = I_C, and I_C = C·dV/dt)
b) What makes this different from pure resistor circuits?
c) Predict: What happens to V1 over time if Vin is a step input?
d) This requires time integration to solve - we'll tackle this next week!

### Solutions

**Exercise 1 Solution:**

a) MNA equation:
```
G1·(Vin - V1) = G2·V1
G1·Vin = (G1 + G2)·V1

Matrix form:
[ G1 + G2 ] [ V1 ] = [ G1·Vin ]
```

b) With G1 = 1/1000 = 0.001 S, G2 = 1/2000 = 0.0005 S:
```
[ 0.0015 ] [ V1 ] = [ 0.001 · 12 ] = [ 0.012 ]
V1 = 0.012 / 0.0015 = 8V
```

Or use voltage divider formula: V1 = Vin · R2/(R1+R2) = 12 · 2000/3000 = 8V ✓

c) Currents:
```
I_R1 = (Vin - V1) / R1 = (12 - 8) / 1000 = 4mA
I_R2 = V1 / R2 = 8 / 2000 = 4mA
Check: I_R1 = I_R2 ✓ (KCL satisfied)
```

**Exercise 2 Solution:**

a) KCL at node1: G1·(Vin - V1) = G3·V1
   KCL at node2: G2·(Vin - V2) = G4·V2

b) Matrix form (2×2 system):
```
[ G1+G3   0   ] [ V1 ]   [ G1·Vin ]
[  0    G2+G4 ] [ V2 ] = [ G2·Vin ]
```

Notice: No coupling between node1 and node2 (diagonal matrix) because they're not directly connected.

c) Solve:
```
V1 = G1·Vin / (G1+G3) = (0.1·10) / (0.1 + 0.0333) = 1.0 / 0.1333 = 7.5V
V2 = G2·Vin / (G2+G4) = (0.05·10) / (0.05 + 0.025) = 0.5 / 0.075 = 6.67V
```

d) Bridge balance check: R1/R2 =? R3/R4
   10/20 =? 30/40
   0.5 ≠ 0.75
   **Not balanced!** (Voltage difference V1 - V2 = 0.83V appears across the bridge)

**Exercise 3 Solution:**

a) Time-domain equation:
```
At node1: I_R = I_C
(Vin - V1) / R = C · dV1/dt

This is a first-order differential equation!
```

b) Difference from resistive circuits:
- Resistive: Algebraic equations → solve directly
- RC: Differential equation → requires time integration

c) Step response prediction:
- At t=0: V1 = 0 (capacitor initially uncharged)
- At t=∞: V1 = Vin (capacitor fully charged, no current)
- Exponential transition: V1(t) = Vin · (1 - e^(-t/RC))
- Time constant: τ = RC = 1000 · 10×10^-6 = 10ms

d) Next week, we'll learn how to stamp capacitors using Backward Euler or Trapezoidal integration!

---

## What's Next: The 5-Week Roadmap

**Next Week (Week 2): Matrix Stamping Deep-Dive**

- **Monday:** What makes capacitors different from resistors?
- **Wednesday:** Capacitor stamping with time integration (Backward Euler code walkthrough)
- **Friday:** Article preview + hype for weekend deep-dive

**Weekend Article #2:** "Matrix Stamping Deep-Dive: R, L, C Components"
- Capacitor stamping (time-dependent!)
- Inductor stamping (also time-dependent!)
- Backward Euler vs Trapezoidal integration
- Complete CapacitorStamper.java code walkthrough
- Exercises with transient analysis

**Week 3:** Sparse Matrices & Cholesky Decomposition
- Why sparse matrices matter (memory + speed)
- Cholesky decomposition algorithm
- SymmetricSparseMatrix.java implementation
- Performance analysis

**Week 4:** Nonlinear Components & Newton-Raphson
- Diode modeling (exponential I-V curve)
- Newton-Raphson iteration for convergence
- Solver comparison: Backward Euler vs Trapezoidal vs Gear-Shichman
- DiodeStamper.java code walkthrough

**Week 5:** Complete System Architecture
- Full simulation pipeline: file → parser → stampers → solver → scope
- WebSocket real-time updates
- Signal processing: FFT, THD, CISPR16
- The big picture: how everything fits together

By Week 5, you'll have a complete understanding of circuit simulation from mathematical foundations to production-quality code.

---

## Further Reading

**Books:**
- **"Computer Methods for Circuit Analysis and Design"** by Andrei Vladimirescu - The definitive SPICE reference
- **"The SPICE Book"** by Andrei Vladimirescu - Comprehensive treatment of MNA and numerical methods
- **"Numerical Recipes"** by Press et al. - Chapter on solving linear systems (Cholesky, LU, sparse methods)
- **"Circuit Simulation"** by Farid N. Najm - Modern treatment with MATLAB examples

**Foundational Papers:**
- Hachtel et al., "The Sparse Tableau Approach to Network Analysis and Design" (1971) - Early MNA formulation
- Ho, Ruehli, Brennan, "The Modified Nodal Approach to Network Analysis" (1975) - The definitive MNA paper

**Source Code:**
- **GeckoCIRCUITS:** github.com/geckocircuits/GeckoCIRCUITS - Open source, 20+ years of development
- **ngspice:** sourceforge.net/projects/ngspice/ - Open-source SPICE implementation
- **Qucs:** qucs.sourceforge.net - Quite Universal Circuit Simulator

**Online Resources:**
- Berkeley SPICE documentation (classic references)
- PLECS documentation - MNA formulation section
- CircuitLab blog - Circuit simulation tutorials

---

## Thanks for Reading!

You now understand the mathematical foundation of every circuit simulator: SPICE, PLECS, GeckoCIRCUITS, and beyond.

**What You Learned:**
- Modified Nodal Analysis (MNA) theory and A·x = b formulation
- Why MNA beats mesh and classical nodal analysis (automation, extensibility)
- The "stamping" pattern that makes circuit simulation systematic
- GeckoCIRCUITS IMatrixStamper interface (Strategy pattern in action)
- ResistorStamper implementation line-by-line
- How to solve circuits by hand using MNA
- What makes capacitors, inductors, and nonlinear components more complex

**Next Steps:**

1. **Try the exercises** - Especially Exercise 1 and 2. Solve by hand, verify in code.
2. **Subscribe to this newsletter** - Weekly deep-dives on circuit simulation (it's free!)
3. **Follow me on LinkedIn** - 3x per week: teasers, code previews, article announcements
4. **Star GeckoCIRCUITS on GitHub** - Support open-source circuit simulation
5. **Share this article** - Help fellow engineers understand their simulation tools

**Coming Monday:**
Why do capacitors stamp differently than resistors? Preview: Because capacitance is **time-dependent**. The stamping pattern changes based on the time integration method (Backward Euler, Trapezoidal). We'll walk through CapacitorStamper.java line-by-line.

**See you Monday with Week 2!**

---

**Subscribe (free):** [Subscribe Button]

**Questions? Comments?** Reply to this email or comment below.

**Source Code:**
github.com/geckocircuits/GeckoCIRCUITS

**Exercises as PDF:** [Download Link]

---

## SEO Metadata

**Title Tag (60 chars):**
Modified Nodal Analysis Tutorial | GeckoCIRCUITS Code

**Meta Description (155 chars):**
Learn Modified Nodal Analysis (MNA) through GeckoCIRCUITS source code. Complete tutorial with theory, implementation, and exercises. 15-min read for engineers.

**URL Slug:**
`modified-nodal-analysis-mna-circuit-simulation-tutorial`

**Primary Keywords:**
- Modified Nodal Analysis
- MNA circuit simulation
- SPICE simulation tutorial
- circuit simulator implementation
- matrix stamping pattern

**Secondary Keywords:**
- GeckoCIRCUITS tutorial
- Kirchhoff laws programming
- circuit equation formulation
- Java circuit simulation
- electrical engineering algorithms

**Target Audience:**
Power electronics engineers, electrical engineering students, circuit simulator developers, open-source contributors

**Estimated Reading Time:** 15 minutes
**Word Count:** 3,185 words
**Code Examples:** 6 snippets
**Exercises:** 3 circuits with complete solutions

---

**End of Article**
