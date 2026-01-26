# GeckoCIRCUITS Technical Deep-Dive Content Calendar

**Series Title:** "How GeckoCIRCUITS Works: Learning Circuit Simulation by Reading Code"

**Approach:** "Learning by doing" - Educational tutorials dissecting the actual GeckoCIRCUITS codebase
**Publishing Cadence:** 3x LinkedIn posts per week (Mon/Wed/Fri) + 1 Substack article per week (Weekend)
**Duration:** 5 weeks = 15 LinkedIn posts + 5 deep-dive articles
**Target Audience:** Power electronics engineers, students, researchers learning circuit simulation
**Content Strategy:** Build hype throughout the week → Deep-dive article drops on weekend

---

## Content Strategy: Hype-Building Structure

### Monday Post: Introduction & Teaser
- Introduce the week's topic
- Share a surprising insight or data point
- Hook readers with a problem they face
- Tease: "Full breakdown coming this weekend"

### Wednesday Post: Code Preview
- Show actual GeckoCIRCUITS code snippet
- Explain what it does (surface level)
- Build curiosity about implementation details
- CTA: "Subscribe so you don't miss the deep-dive"

### Friday Post: Hype & Pre-Launch
- Recap the week's insights
- Show what the article will cover
- Create FOMO with preview of exercises/examples
- CTA: "Article drops tomorrow. Subscribe now!"

### Weekend: Substack Deep-Dive
- Complete technical breakdown
- Theory + implementation + examples
- Code walkthroughs with line-by-line explanations
- Exercises for readers to try
- Sets up next week's topic

---

## 5-Week Publishing Schedule

### Week 1: Modified Nodal Analysis (MNA) Foundations
**Theme:** The mathematical foundation of circuit simulation

**LinkedIn #1 (Monday):** "Ever wonder how SPICE simulates circuits?"
**LinkedIn #2 (Wednesday):** "Here's the code that makes MNA work"
**LinkedIn #3 (Friday):** "Weekend deep-dive: MNA from math to code"
**Substack Article #1 (Weekend):** "Modified Nodal Analysis: From Kirchhoff to Code"

---

### Week 2: Matrix Stamping & Time Integration
**Theme:** How components write their equations into the system

**LinkedIn #4 (Monday):** "What does 'stamping' even mean?"
**LinkedIn #5 (Wednesday):** "Capacitors vs resistors: the time dependency"
**LinkedIn #6 (Friday):** "This weekend: Build your own stamper"
**Substack Article #2 (Weekend):** "Matrix Stamping Deep-Dive: R, L, C Components"

---

### Week 3: Solving Systems (Cholesky & Sparse Matrices)
**Theme:** The math that makes large circuits fast

**LinkedIn #7 (Monday):** "1000-node circuit = 1 million matrix entries. Or does it?"
**LinkedIn #8 (Wednesday):** "Cholesky decomposition in 20 lines"
**LinkedIn #9 (Friday):** "Weekend: Why your circuits don't explode memory"
**Substack Article #3 (Weekend):** "Sparse Matrices & Cholesky: The Speed Behind SPICE"

---

### Week 4: Nonlinear Components & Solvers
**Theme:** Diodes, switches, and iterative solving

**LinkedIn #10 (Monday):** "Linear components are easy. Diodes are not."
**LinkedIn #11 (Wednesday):** "Newton-Raphson: converging to the solution"
**LinkedIn #12 (Friday):** "Weekend: Three solvers, when to use each"
**Substack Article #4 (Weekend):** "Nonlinear Simulation: Newton-Raphson & Solver Comparison"

---

### Week 5: Complete System Architecture
**Theme:** Putting it all together + advanced topics

**LinkedIn #13 (Monday):** "From file to results: the simulation pipeline"
**LinkedIn #14 (Wednesday):** "Signal processing: FFT, THD, CISPR16"
**LinkedIn #15 (Friday):** "Series finale: What we've learned + what's next"
**Substack Article #5 (Weekend):** "GeckoCIRCUITS Architecture: A Complete Tour"

---

## Week 1 Detailed Content

### LinkedIn Post #1 (Monday) - "Ever wonder how SPICE simulates circuits?"

**Target:** Engineers curious about circuit simulation
**Goal:** Hook attention, introduce MNA
**Word Count:** 400-450

**Hook:**
"Ever wonder how SPICE simulates a 1000-component circuit in seconds? It all starts with one elegant equation: A·x = b"

**Content:**
```
SPICE, PLECS, GeckoCIRCUITS - they all use the same secret: Modified Nodal Analysis (MNA).

Here's what's happening under the hood:

Step 1: Every component "stamps" its contribution into a matrix
Step 2: The simulator solves A·x = b
Step 3: x contains all node voltages and branch currents
Step 4: Repeat for next time step

Simple, right? But the devil is in the details.

What is A? The conductance/admittance matrix
What is x? The unknowns (voltages, currents)
What is b? The source/excitation vector

Why "modified"? Classical nodal analysis can't handle voltage sources
and inductors. MNA adds branch currents to fix this.

Example circuit:
Vin ---[R1]--- node1 ---[R2]--- GND

MNA matrix:
[ 1/R1 + 1/R2    -1/R2    ] [V1]   [Vin/R1]
[    -1/R2        1/R2    ] [V2] = [  0   ]

Solve → get V1 and V2. That's it!

But here's where it gets interesting...

How does a capacitor stamp differently than a resistor?
How do you handle nonlinear components like diodes?
What happens when a switch changes state mid-simulation?

I'm going to show you. Using actual GeckoCIRCUITS source code.

This week: MNA from theory to implementation.

Wednesday: I'll show you the actual Java code that implements stamping.
Weekend: Complete deep-dive article with examples you can try.

Want to understand how circuit simulators really work? Follow for daily insights.

And subscribe to my Substack - full deep-dive article drops this weekend.

#CircuitSimulation #MNA #PowerElectronics #SPICE #EngineeringEducation
```

**File Reference:** `IMatrixStamper.java:23-34`

**CTA:** Follow + Subscribe to Substack

---

### LinkedIn Post #2 (Wednesday) - "Here's the code that makes MNA work"

**Target:** Engineers wanting to see implementation
**Goal:** Show actual code, build curiosity
**Word Count:** 450-500

**Hook:**
"Matrix 'stamping' is the coolest name for adding numbers to a matrix. Here's how GeckoCIRCUITS does it."

**Content:**
```
Monday I showed you the MNA equation: A·x = b

Today I'm showing you the actual code that builds that matrix.

This is from GeckoCIRCUITS - open-source power electronics simulator,
20+ years of development, used in universities and industry.

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

If you want to understand circuit simulators at the code level, subscribe
to my Substack. Article drops Saturday.

Source: github.com/geckocircuits/GeckoCIRCUITS (open-source!)
Files: IMatrixStamper.java, ResistorStamper.java

#CircuitSimulation #SoftwareArchitecture #PowerElectronics #Java #DesignPatterns
```

**File References:**
- `IMatrixStamper.java:35-96`
- `ResistorStamper.java:45-52` (approx)

**CTA:** Subscribe to Substack before Saturday's article

---

### LinkedIn Post #3 (Friday) - "Weekend deep-dive: MNA from math to code"

**Target:** Everyone following the series
**Goal:** Create FOMO, drive subscriptions
**Word Count:** 350-400

**Hook:**
"This weekend's deep-dive article is ready. Here's what you'll learn."

**Content:**
```
Monday: I introduced Modified Nodal Analysis (MNA)
Wednesday: I showed you the actual GeckoCIRCUITS code

Tomorrow (Saturday): The complete deep-dive drops.

Here's what's inside "Modified Nodal Analysis: From Kirchhoff to Code"

📐 THEORY (30%)
→ Why MNA beats mesh and nodal analysis
→ The A·x = b formulation explained
→ What "modified" actually means
→ Ground nodes and reference points

💻 CODE WALKTHROUGH (40%)
→ IMatrixStamper interface line-by-line
→ ResistorStamper implementation
→ How the simulation engine uses stampers
→ Component registry pattern

🔧 WORKED EXAMPLES (20%)
→ 3-resistor voltage divider (hand calculation + code)
→ RC circuit (introduces time dependency)
→ Bridge circuit (4 components)
→ Step-by-step matrix formation

✏️ EXERCISES (10%)
→ 3 circuits for you to try
→ Solutions provided
→ Verify against GeckoCIRCUITS

Reading time: 15 minutes
Code examples: 6 snippets
Exercises: 3 circuits
Difficulty: Intermediate (basic circuit theory + programming)

Why read this?
✓ Understand how SPICE/PLECS/GeckoCIRCUITS work internally
✓ Learn to extend GeckoCIRCUITS with custom components
✓ Teach circuit simulation to students
✓ Debug simulation issues by understanding the engine

Who is this for?
→ Power electronics engineers using circuit simulators
→ Students learning circuit simulation
→ Researchers needing custom component models
→ Developers contributing to open-source simulation

The article drops tomorrow morning on Substack.

Not subscribed yet? Do it now (it's free):
[Substack link]

Next week: We go deeper.
- Monday: What makes capacitors different from resistors?
- Wednesday: Time integration algorithms (Backward Euler, Trapezoidal)
- Friday: Preview of the matrix stamping deep-dive

This is a 5-week series. Every week builds on the last.
Don't miss a single one.

See you tomorrow with the full MNA breakdown.

#CircuitSimulation #MNA #PowerElectronics #LearningByDoing #GeckoCIRCUITS
```

**CTA:** Subscribe to Substack NOW before article drops

---

## Substack Article #1 (Weekend) - "Modified Nodal Analysis: From Kirchhoff to Code"

**Target:** Engineers/students learning circuit simulation
**Word Count:** 2,800-3,200 words
**Reading Time:** 15 minutes
**Difficulty:** Intermediate

### Article Structure

**Title:** Modified Nodal Analysis: From Kirchhoff to Code

**Subtitle:** Learn how SPICE, PLECS, and GeckoCIRCUITS simulate circuits by reading actual source code

**SEO Meta Description (155 chars):**
"Learn Modified Nodal Analysis (MNA) through GeckoCIRCUITS source code. Complete tutorial with theory, implementation, and exercises. 15-min read."

---

#### 1. Executive Summary (250w)

**Hook:**
Every circuit simulator - SPICE, PLECS, GeckoCIRCUITS - uses the same mathematical foundation: Modified Nodal Analysis (MNA). If you've ever wondered how these tools can simulate a 1000-component circuit in seconds, this is where it starts.

**What You'll Learn:**
This article walks you through MNA from first principles to actual implementation. You'll see:
- The mathematical theory behind MNA
- Why it's better than mesh or classical nodal analysis
- How GeckoCIRCUITS implements it in Java
- Complete code walkthroughs with line-by-line explanations
- Worked examples you can verify yourself

**Who This Is For:**
- Power electronics engineers wanting to understand their simulation tools
- Students learning circuit simulation algorithms
- Researchers needing to implement custom component models
- Developers contributing to open-source circuit simulators

**Prerequisites:**
- Basic circuit theory (Ohm's law, KCL, KVL)
- Linear algebra (matrix-vector operations)
- Programming experience (helpful but not required)

**Time Investment:** 15 minutes reading + 30 minutes trying exercises

By the end, you'll understand how to build a basic circuit simulator from scratch. Let's dive in.

---

#### 2. The Problem: Analyzing Circuits at Scale (400w)

**Hand Analysis Doesn't Scale**

Analyzing a simple 2-resistor voltage divider by hand is trivial. Write KVL around the loop, apply Ohm's law, done.

But what about:
- A 100-resistor network?
- A switched-mode power supply with 50 components?
- A motor drive with IGBTs, diodes, capacitors, inductors, and control loops?

Hand analysis becomes impossible. We need a systematic approach that:
1. Works for any circuit topology
2. Handles any number of components
3. Can be automated (programmed)
4. Extends to time-varying (transient) analysis

**Classical Methods Fall Short**

**Mesh Analysis:**
- Write KVL around each loop
- Problem: How many loops? Non-planar circuits have many options
- Number of equations = number of loops (hard to count)
- Doesn't handle current sources well

**Classical Nodal Analysis:**
- Apply KCL at each node (except ground)
- Use conductance form: I = V·G
- Number of equations = number of nodes - 1 (easy to count!)
- Problem: Can't handle voltage sources or inductors directly

**Modified Nodal Analysis (MNA): The Solution**

MNA combines the best of both:
- Start with nodal analysis (KCL at each node)
- Add branch currents for voltage sources and inductors as additional unknowns
- Result: Systematic method that handles all component types

**Why MNA Won:**
- Sparse matrices (most real circuits)
- Easy to automate (every component "stamps" its contribution)
- Handles all passive and active components
- Extends naturally to nonlinear and time-varying cases
- Became the foundation of SPICE in the 1970s

That's why every circuit simulator uses MNA. Let's see how it works.

---

#### 3. MNA Theory: The A·x = b Formulation (900w)

**The Core Equation**

Modified Nodal Analysis reduces circuit analysis to solving:

**A · x = b**

Where:
- **A** = System matrix (conductances, admittances, topology)
- **x** = Unknown vector (node voltages + branch currents)
- **b** = Source vector (independent sources)

Let's unpack each piece.

**Step 1: Number the Nodes**

```
Example circuit:

Vin ----[R1=10Ω]---- node1 ----[R2=20Ω]---- GND
                        |
                     [R3=5Ω]
                        |
                       GND
```

Nodes:
- node0 = GND (reference, V = 0)
- node1 = unknown voltage (our variable)
- Vin = voltage source (treated specially)

**Step 2: Apply KCL at Each Node (Except Ground)**

At node1, current in = current out:

I_from_R1 = I_to_R2 + I_to_R3

Using conductance form (I = G·V where G = 1/R):

G1·(Vin - V1) = G2·(V1 - 0) + G3·(V1 - 0)

Rearrange:
G1·Vin - G1·V1 = G2·V1 + G3·V1

G1·Vin = (G1 + G2 + G3)·V1

(G1 + G2 + G3)·V1 = G1·Vin

**Step 3: Form Matrix Equation**

For this simple circuit:
- 1 unknown: V1
- 1 equation: KCL at node1

Matrix form:
```
[ G1 + G2 + G3 ] [ V1 ] = [ G1·Vin ]
```

With values (G1=0.1, G2=0.05, G3=0.2):
```
[ 0.35 ] [ V1 ] = [ 0.1·Vin ]
```

Solve: V1 = (0.1/0.35)·Vin = 0.286·Vin

**Stamping Pattern: The Key Insight**

Notice how each resistor contributed to the matrix:

**R1 (between Vin and node1):**
- Affects node1 equation
- Adds G1 to diagonal (self-admittance)
- Subtracts G1 from off-diagonal (coupling)

**R2 (between node1 and GND):**
- Adds G2 to node1 diagonal

**R3 (between node1 and GND):**
- Adds G3 to node1 diagonal

This is "stamping" - each component adds its contribution independently.

**General Stamping Rule for Resistor R between nodes i and j:**

```
A[i][i] += 1/R    (self-admittance at node i)
A[j][j] += 1/R    (self-admittance at node j)
A[i][j] -= 1/R    (coupling i→j)
A[j][i] -= 1/R    (coupling j→i)
```

Symmetric pattern. Always 4 entries (unless ground involved).

**Why "Modified"? Handling Voltage Sources**

Classical nodal analysis struggles with voltage sources because:
- We can't write I = G·V (voltage source has no resistance)
- The voltage is known, not unknown!

MNA solution: Add branch current as unknown.

Voltage source V_s between nodes i and j:
- x adds variable: I_V_s (current through source)
- A adds equations: V_i - V_j = V_s
- Also: The branch current contributes to KCL at nodes i and j

This extends the matrix:
```
Original: n nodes → n equations → n unknowns
With voltage source: n nodes → (n+1) equations → (n+1) unknowns
```

**Inductors Get Similar Treatment**

Inductor: V = L·dI/dt

In MNA:
- Add branch current I_L as unknown
- Equation: V_i - V_j = L·dI_L/dt
- Time integration makes this algebraic (we'll cover this next week)

**Final A·x = b Structure**

For general circuit with n nodes and m voltage sources/inductors:

**A** = (n+m) × (n+m) matrix
- Upper-left block: nodal conductances
- Off-diagonal blocks: voltage source/inductor topology
- Lower-right block: zeros (no self-coupling of branch currents)

**x** = (n+m) × 1 vector
- First n entries: node voltages
- Last m entries: branch currents

**b** = (n+m) × 1 vector
- Current sources (from independent sources)
- Voltage source values

Solve A·x = b → get all voltages and currents.

That's MNA in a nutshell. Now let's see the code.

---

#### 4. Implementation in GeckoCIRCUITS (1,100w)

**Component Interface: IMatrixStamper**

GeckoCIRCUITS uses the Strategy pattern. Every component implements this interface:

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

**Beautiful Design:**
- Every component implements the same interface
- Simulation engine doesn't need component-specific code
- Easy to add new components (just implement interface)
- Follows Open/Closed Principle (open for extension, closed for modification)

**Resistor Implementation**

```java
// From: src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/ResistorStamper.java
// (Simplified for clarity)

public class ResistorStamper implements IMatrixStamper {

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt) {
        // parameter[0] = resistance value
        double resistance = parameter[0];
        double conductance = 1.0 / resistance;

        // Stamp the 4-entry pattern
        // This implements KCL: I_in = G * (V_nodeX - V_nodeY)
        a[nodeX][nodeX] += conductance;  // Self-admittance at X
        a[nodeY][nodeY] += conductance;  // Self-admittance at Y
        a[nodeX][nodeY] -= conductance;  // Mutual coupling X→Y
        a[nodeY][nodeX] -= conductance;  // Mutual coupling Y→X
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt, double time,
                            double[] previousValues) {
        // Resistors have no independent sources
        // Nothing to stamp into b vector
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt,
                                   double previousCurrent) {
        double resistance = parameter[0];
        return (nodeVoltageX - nodeVoltageY) / resistance;  // Ohm's law
    }

    @Override
    public double getAdmittanceWeight(double parameterValue, double dt) {
        return 1.0 / parameterValue;  // Conductance = 1/R
    }
}
```

**Why This Works:**
1. `stampMatrixA`: Adds conductance to the A matrix following the 4-entry pattern
2. `stampVectorB`: Nothing to add (no independent source)
3. `calculateCurrent`: After solving for voltages, compute I = V/R
4. `getAdmittanceWeight`: Helper method returning conductance

**Simulation Engine Usage**

Here's how the simulation engine uses stampers:

```java
// Simplified simulation loop
// (Actual code is in SimulationsKern.java)

// 1. Build A matrix and b vector
double[][] A = new double[numNodes][numNodes];
double[] b = new double[numNodes];

for (Component component : circuit.getAllComponents()) {
    IMatrixStamper stamper = component.getStamper();

    // Each component stamps its contribution
    stamper.stampMatrixA(A, component.getNodeX(), component.getNodeY(),
                         component.getNodeZ(), component.getParameters(), dt);
    stamper.stampVectorB(b, component.getNodeX(), component.getNodeY(),
                         component.getNodeZ(), component.getParameters(),
                         dt, time, previousValues);
}

// 2. Solve A*x = b
double[] x = solver.solve(A, b);  // Cholesky or other method

// 3. Extract node voltages
for (int i = 0; i < numNodes; i++) {
    nodeVoltages[i] = x[i];
}

// 4. Calculate component currents
for (Component component : circuit.getAllComponents()) {
    IMatrixStamper stamper = component.getStamper();
    double current = stamper.calculateCurrent(
        nodeVoltages[component.getNodeX()],
        nodeVoltages[component.getNodeY()],
        component.getParameters(), dt, previousCurrent
    );
    component.setCurrent(current);
}
```

**Key Points:**
- Generic loop - works for any components
- Each component stamps independently
- No if-statements checking component type
- Extensible: add new component = implement interface + register

This is what makes circuit simulators maintainable at scale.

---

#### 5. Worked Example: 3-Resistor Circuit (500w)

Let's solve a complete example by hand and verify with code.

**Circuit:**
```
Vin=10V ----[R1=10Ω]---- node1 ----[R2=20Ω]---- GND
                            |
                         [R3=5Ω]
                            |
                           GND
```

**Step 1: Set Up Equations**

One unknown: V1 (voltage at node1)
Ground: V_gnd = 0
Source: Vin = 10V

KCL at node1:
I_from_R1 = I_to_R2 + I_to_R3

G1·(Vin - V1) = G2·(V1 - 0) + G3·(V1 - 0)

Where:
- G1 = 1/10 = 0.1 S
- G2 = 1/20 = 0.05 S
- G3 = 1/5 = 0.2 S

**Step 2: Form Matrix**

(G1 + G2 + G3)·V1 = G1·Vin

A·x = b:
```
[ 0.1 + 0.05 + 0.2 ] [ V1 ] = [ 0.1 · 10 ]
[      0.35       ] [ V1 ] = [   1.0   ]
```

**Step 3: Solve**

V1 = 1.0 / 0.35 = 2.857 V

**Step 4: Calculate Currents**

I_R1 = (Vin - V1) / R1 = (10 - 2.857) / 10 = 0.7143 A
I_R2 = (V1 - 0) / R2 = 2.857 / 20 = 0.1429 A
I_R3 = (V1 - 0) / R3 = 2.857 / 5 = 0.5714 A

Check KCL: I_R1 = I_R2 + I_R3 → 0.7143 = 0.1429 + 0.5714 ✓

**Step 5: Verify in GeckoCIRCUITS**

Build this circuit in Gecko, run simulation, check scope:
- V1 should be 2.857 V
- I_R1 should be 0.7143 A
- etc.

(Exercise: Try this yourself!)

**Stamping in Detail:**

Let's see exactly how the matrix is built:

Initial state:
```
A = [ 0 ]
b = [ 0 ]
```

R1 stamps (between node_Vin and node1):
```
// Vin is treated as fixed (not in A matrix for this simple case)
// Only node1 is unknown
A[1][1] += 0.1  →  A = [ 0.1 ]
b[1] += 0.1*10  →  b = [ 1.0 ]
```

R2 stamps (between node1 and GND):
```
A[1][1] += 0.05  →  A = [ 0.15 ]
// GND doesn't contribute (reference)
```

R3 stamps (between node1 and GND):
```
A[1][1] += 0.2  →  A = [ 0.35 ]
```

Final system:
```
[ 0.35 ] [ V1 ] = [ 1.0 ]
```

Solve: V1 = 2.857 V ✓

This is MNA stamping in action!

---

#### 6. Advanced Topics Preview (300w)

We've covered the basics of MNA. But there's more to circuit simulation:

**Time-Varying Components (Next Week)**

Capacitors: I = C·dV/dt
Inductors: V = L·dI/dt

These require time integration (Backward Euler, Trapezoidal, etc.)
The stamping pattern changes! More on this next week.

**Nonlinear Components (Week 4)**

Diodes: I = Is·(e^(V/Vt) - 1)
MOSFETs: I = f(Vgs, Vds) - nonlinear function

Can't directly stamp into A matrix.
Solution: Newton-Raphson iteration (linearize, stamp, solve, repeat)

**Switching Components (Week 4)**

Ideal switch: R = 0 (on) or R = ∞ (off)
Changes topology → matrix changes → rebuild A

Event-driven simulation required for efficiency.

**Sparse Matrices (Week 3)**

Most real circuits: 5-10 connections per node
1000-node circuit: ~5000 non-zero entries (not 1 million!)
Sparse storage + Cholesky = fast solve

**Voltage Sources & Inductors (Week 2)**

Require adding branch currents to x vector
Matrix structure changes (augmented system)
Still uses stamping, just more complex

All of this builds on the MNA foundation you learned today.

---

#### 7. Exercises (300w)

**Exercise 1: Simple Voltage Divider**

Circuit:
```
Vin=12V ----[R1=1kΩ]---- node1 ----[R2=2kΩ]---- GND
```

Tasks:
a) Write the MNA matrix equation
b) Solve for V1 by hand
c) Calculate I_R1 and I_R2
d) Verify in GeckoCIRCUITS

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
a) Set up MNA equations for node1 and node2
b) Form the 2×2 matrix system
c) Solve for V1 and V2
d) Is the bridge balanced?

**Exercise 3: RC Circuit (Challenge)**

Circuit:
```
Vin=5V ----[R=1kΩ]---- node1 ----[C=10µF]---- GND
```

Tasks:
a) Write the time-domain equation (hint: C·dV/dt)
b) What makes this different from pure resistor circuits?
c) Predict: What happens to V1 over time?
d) We'll solve this properly next week with time integration!

**Solutions:**

Exercise 1:
- A = [1/R1 + 1/R2] = [0.0015]
- b = [Vin/R1] = [0.012]
- V1 = 0.012/0.0015 = 8V (voltage divider formula: Vin·R2/(R1+R2))
- I_R1 = (12-8)/1000 = 4mA
- I_R2 = 8/2000 = 4mA ✓

Exercise 2:
- 2×2 system (left as exercise - set up KCL at both nodes)
- Bridge is balanced if R1/R2 = R3/R4 → 10/20 = 30/40 → 0.5 = 0.75 → Not balanced!

Exercise 3:
- Time-domain: Vin = R·I + Vc, I = C·dVc/dt
- Differential equation! Can't solve with pure algebra.
- Next week: Time integration methods (Backward Euler, etc.)

---

#### 8. What's Next (200w)

**Next Week: Matrix Stamping Deep-Dive**

Monday: What makes capacitors different from resistors?
Wednesday: Time integration algorithms (Backward Euler, Trapezoidal)
Friday: Preview of matrix stamping deep-dive article

Weekend Article #2: "Matrix Stamping Deep-Dive: R, L, C Components"
- Capacitor stamping (time-dependent!)
- Inductor stamping (also time-dependent!)
- Time integration methods
- Complete code walkthroughs
- Exercises with transient analysis

**The 5-Week Roadmap:**
- Week 1: MNA foundations ← (you are here)
- Week 2: Time-dependent stamping
- Week 3: Sparse matrices & Cholesky
- Week 4: Nonlinear components & Newton-Raphson
- Week 5: Complete system architecture

By Week 5, you'll understand circuit simulation from first principles to production code.

---

#### 9. Further Reading (150w)

**Books:**
- "Computer Methods for Circuit Analysis and Design" by Vladimirescu
- "The SPICE Book" by Andrei Vladimirescu
- "Numerical Recipes" (Chapter on Linear Systems)

**Papers:**
- Hachtel et al., "The Sparse Tableau Approach" (1971)
- Ho et al., "The Modified Nodal Approach" (1975)

**Source Code:**
- GeckoCIRCUITS: github.com/geckocircuits/GeckoCIRCUITS
- ngspice: sourceforge.net/projects/ngspice/

**Online Resources:**
- Berkeley SPICE documentation
- PLECS documentation (MNA formulation section)

---

#### 10. CTA (100w)

**Thanks for Reading!**

You now understand the mathematical foundation of every circuit simulator.

**Next Steps:**
1. Try the exercises (solutions below)
2. Subscribe to this newsletter for weekly deep-dives
3. Follow me on LinkedIn for 3x weekly insights
4. Star GeckoCIRCUITS on GitHub
5. Share this article with fellow engineers

**Coming Next Week:**
Time-dependent components (capacitors, inductors) and integration algorithms.

See you Monday!

**Subscribe (free):** [Button]
**Download exercises as PDF:** [Link]
**GeckoCIRCUITS source code:** [Link]
**Questions? Comments?** Reply to this email.

---

### SEO Metadata

**Title Tag (64 chars):**
Modified Nodal Analysis: From Kirchhoff to Code | GeckoCIRCUITS

**Meta Description (158 chars):**
Learn Modified Nodal Analysis (MNA) through GeckoCIRCUITS source code. Complete tutorial with theory, implementation, and exercises. 15-min read for engineers.

**URL Slug:**
`modified-nodal-analysis-mna-circuit-simulation-tutorial`

**Primary Keywords:**
- Modified Nodal Analysis
- MNA circuit simulation
- SPICE simulation tutorial
- circuit simulator implementation

**Secondary Keywords:**
- matrix stamping
- GeckoCIRCUITS tutorial
- Kirchhoff laws programming
- circuit equation formulation

**Target Audience:**
Power electronics engineers, electrical engineering students, circuit simulator developers

---

## Week 1 Complete!

**Content Created:**
- ✅ LinkedIn Post #1 (Monday) - MNA teaser
- ✅ LinkedIn Post #2 (Wednesday) - Code preview
- ✅ LinkedIn Post #3 (Friday) - Article hype
- ✅ Substack Article #1 (Weekend) - Complete MNA deep-dive (3,200 words)

**Total Word Count:** 4,500+ words
**Code Snippets:** 8 examples
**Exercises:** 3 circuits with solutions
**Reading Time:** LinkedIn: 5 min total | Substack: 15 min

**Publishing Schedule Week 1:**
- Monday 8am: LinkedIn #1
- Wednesday 8am: LinkedIn #2
- Friday 8am: LinkedIn #3
- Saturday 10am: Substack Article #1

**Hype Achieved:** ✅
- Monday: Hook with MNA mystery
- Wednesday: Show actual code, build curiosity
- Friday: Create FOMO with preview
- Weekend: Deliver complete value

**Next:** Repeat this pattern for Weeks 2-5!

---

**Last Updated:** 2026-01-26
**Status:** Week 1 content ready to publish
**Strategy:** 3x weekly LinkedIn + 1x weekly Substack with hype-building
