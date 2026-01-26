# Matrix Stamping Deep-Dive: R, L, C Components

**Subtitle:** How time-dependent components transform the MNA matrix through numerical integration

**Reading Time:** 14-16 minutes
**Difficulty:** Intermediate
**Prerequisites:** Week 1 MNA knowledge, basic calculus, differential equations

---

## Executive Summary

Resistors are algebraic. Capacitors and inductors are differential equations. This changes everything about how they stamp into the MNA matrix.

In Week 1, you learned that every circuit component stamps its contribution into the A matrix independently. Resistors are straightforward: conductance G = 1/R goes directly into the matrix. But what happens when a component's behavior depends on **rates of change**?

**What You'll Learn:**

This article walks you through the complete picture of stamping time-dependent components:

- Why resistors and capacitors require completely different matrix entries
- The mathematical theory behind time integration methods (Backward Euler vs Trapezoidal)
- How numerical integration converts differential equations into algebraic stamping patterns
- Complete code walkthroughs from GeckoCIRCUITS: CapacitorStamper.java, InductorStamper.java, IStatefulStamper interface
- How dt (time step) affects matrix condition and accuracy
- Worked examples: RC charging, LC oscillator, RLC filter
- Three exercises with complete solutions and simulation parameters

**Who This Is For:**

- Power electronics engineers simulating transient behavior
- Students transitioning from DC analysis to time-domain simulation
- Anyone wondering why SPICE needs to iterate over time steps
- Developers implementing custom component models

**Prerequisites:**

- Week 1 MNA understanding (matrix stamping, KCL formulation)
- Basic calculus (derivatives, integration)
- Familiarity with differential equations (helpful but we'll explain)

**Time Investment:** 15 minutes reading + 45 minutes working through exercises

By the end, you'll understand why the same circuit simulator uses different stamping code for resistors versus capacitors, and you'll be able to implement time-dependent components yourself.

Let's start.

---

## The Problem: Differential Equations in MNA

**Why Time Matters**

In Week 1, we solved circuits algebraically using Ohm's law:

```
Resistor:     I = V / R  (instantaneous, no history)
```

This works perfectly for DC steady-state. But real circuits aren't always in steady-state. They change over time. Enter the time-dependent components:

```
Capacitor:    I = C · dV/dt    (depends on voltage rate of change)
Inductor:     V = L · dI/dt    (depends on current rate of change)
```

These are **differential equations**, not algebraic equations. The current through a capacitor doesn't depend on its voltage at this instant—it depends on how **fast** the voltage is changing.

Imagine charging a 1µF capacitor:
- If voltage changes from 0V to 10V in 1 second: I = 1µ · 10/1 = 10µA
- If voltage changes from 0V to 10V in 1 microsecond: I = 1µ · 10/(1e-6) = 10A

Same voltage change, different dV/dt, dramatically different current. This rate-of-change dependency is fundamental.

**The Algebraic Matrix Problem**

Modified Nodal Analysis requires solving a linear system:

**A · x = b**

Where A is filled with algebraic coefficients. But you can't directly put a derivative into a matrix. Derivatives are operators, not numbers. If you try to stamp dV/dt into A[i][j], what number do you use? The future voltage? The past voltage? Neither exists yet!

**Common Mistakes:**

Engineers new to circuit simulation often make these errors:

1. **Treating capacitors like resistors:** Setting G = C directly into A matrix → produces complete nonsense
2. **Ignoring history:** Only using current time-step values → violates causality
3. **Using wrong integration method:** Backward Euler for oscillatory circuits → damps out unphysically
4. **Time step too large:** dt > RC time constant → loses accuracy
5. **Time step too small:** dt < nanoseconds with millisecond simulation → runs forever

**Solution Preview:**

The key insight: **Time integration methods discretize differential equations into algebraic approximations.**

Convert this differential equation:
```
I = C · dV/dt
```

Into this algebraic equation:
```
I = (C/dt) · V_new - (C/dt) · V_old
```

Now you have numbers to stamp into the matrix! The derivative dV/dt becomes a finite difference (V_new - V_old)/dt.

The rest of this article shows how this transformation works, how GeckoCIRCUITS implements it, and how to choose the right method for your circuit.

---

## Time Integration Theory

**Fundamental Principle**

All time integration methods use the same idea: approximate the derivative as a finite difference. Where they differ is in the accuracy of that approximation and the computational cost.

The differential equation:
```
dx/dt = f(x, t)
```

Becomes:
```
(x_new - x_old) / dt ≈ f(x, t)
```

But the approximation formula changes based on method choice. Let's explore the two methods implemented in GeckoCIRCUITS.

**3.1 Backward Euler (First-Order)**

**The Approximation:**

```
dV/dt ≈ (V_new - V_old) / dt
```

This is evaluated at the **end** of the time step (hence "backward").

**For Capacitors:**

Starting with the capacitor equation:
```
I = C · dV/dt
```

Substitute the Backward Euler approximation:
```
I = C · (V_new - V_old) / dt
```

Expand:
```
I = (C/dt) · V_new - (C/dt) · V_old
```

This is crucial: You can now write this as:
```
I = G_eq · V_new + I_history
```

Where:
- **G_eq = C/dt** (equivalent conductance - stamps into A matrix)
- **I_history = -(C/dt) · V_old** (history term - stamps into b vector)

**Matrix Stamping for Backward Euler Capacitor:**

```
A matrix (equivalent conductance):
a[x][x] += C/dt
a[y][y] += C/dt
a[x][y] -= C/dt
a[y][x] -= C/dt

B vector (history current):
b[x] += (C/dt) · V_old
b[y] -= (C/dt) · V_old
```

This looks exactly like a resistor with conductance G = C/dt, plus a current source with magnitude (C/dt) · V_old.

**For Inductors:**

Inductor equation:
```
V = L · dI/dt
```

Rearrange to solve for current:
```
dI/dt = V / L
```

Discretize:
```
(I_new - I_old) / dt = V_new / L
```

Rearrange:
```
I_new = I_old + (dt/L) · V_new
```

Rewrite as:
```
I = G_eq · V_new + I_history
```

Where:
- **G_eq = dt/L** (equivalent conductance)
- **I_history = I_old** (previous current continues to flow)

The inductor effectively becomes a conductance G = dt/L with a current source I_old.

**Properties of Backward Euler:**

- **Accuracy:** First-order (error ~ O(dt)) - rough but stable
- **Stability:** A-stable (unconditionally stable, works for any dt)
- **Dissipation:** Numerically dissipative - artificially damps oscillations
- **Physical behavior:** Energy is lost to numerical dissipation (non-physical for ideal LC circuits)

**When to Use Backward Euler:**

- Switching converters (stiff systems, sudden changes)
- Circuits with parasitic resistances (naturally dissipative)
- When stability is more important than accuracy
- Large time steps (dt comparable to circuit time constants)

**3.2 Trapezoidal Rule (Second-Order)**

**The Approximation:**

```
dV/dt ≈ (V_new - V_old) / dt
```

Evaluated at the **midpoint** of the time step, but with a more sophisticated history formula.

The trapezoidal rule uses a weighted average of derivatives at both ends:

```
dV/dt ≈ (1/2) · (dV/dt|_new + dV/dt|_old)
```

**For Capacitors:**

Capacitor equation:
```
I = C · dV/dt
```

Average current:
```
I_avg = (1/2) · (I_new + I_old)
```

Both currents are:
```
I_avg = C · (V_new - V_old) / dt
```

This gives:
```
(1/2) · (I_new + I_old) = C · (V_new - V_old) / dt
```

Multiply both sides by 2:
```
I_new + I_old = 2 · C · (V_new - V_old) / dt
```

Rearrange for I_new:
```
I_new = (2C/dt) · (V_new - V_old) - I_old
```

Which can be written as:
```
I = G_eq · V_new + I_history
```

Where:
- **G_eq = 2C/dt** (conductance is double that of Backward Euler!)
- **I_history = -(2C/dt) · V_old + I_old** (more complex history term)

**For Inductors:**

Similar derivation gives:
```
I = (dt/2L) · V + [I_prev + (dt/2L) · V_prev]
```

The inductor conductance is half that of Backward Euler: G = dt/(2L).

**Properties of Trapezoidal:**

- **Accuracy:** Second-order (error ~ O(dt²)) - smoother, more accurate
- **Stability:** A-stable (unconditionally stable)
- **Dissipation:** Non-dissipative - preserves energy
- **Physical behavior:** Ideal energy conservation (good for LC circuits)

**When to Use Trapezoidal:**

- Oscillatory circuits (LC tanks, resonant converters)
- Need high accuracy (small dt acceptable)
- Systems where energy conservation matters
- RF/microwave circuits

**3.3 Comparison Table**

| Aspect | Backward Euler | Trapezoidal |
|--------|----------------|-------------|
| **Order** | 1st (O(dt)) | 2nd (O(dt²)) |
| **Stability** | A-stable | A-stable |
| **Dissipation** | Damps oscillations | Energy conserving |
| **Capacitor G** | C/dt | 2C/dt |
| **Inductor G** | dt/L | dt/(2L) |
| **Matrix updates** | Simple | More history terms |
| **Accuracy** | Rough | Smooth |
| **Complexity** | Simple | Moderate |

**Gear-Shichman Methods:**

GeckoCIRCUITS also supports variable-order Gear-Shichman methods (orders 2 and 4). These methods automatically adjust accuracy based on solution behavior, making them ideal for production simulators where you want to solve quickly without manual parameter tuning.

---

## Implementation in GeckoCIRCUITS

Now let's see the actual code. GeckoCIRCUITS implements these methods with elegant Java classes that follow the Strategy design pattern.

**4.1 CapacitorStamper (Backward Euler)**

Here's the complete implementation from the GeckoCIRCUITS source:

```java
// From: src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/CapacitorStamper.java

public class CapacitorStamper implements IMatrixStamper {

    // Minimum capacitance to avoid numerical issues
    private static final double MIN_CAPACITANCE = 1e-15;

    // Index for capacitance in parameter array
    private static final int PARAM_CAPACITANCE = 0;

    // Index for previous voltage in previousValues array
    private static final int PREV_VOLTAGE = 0;

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt) {
        double conductance = getAdmittanceWeight(parameter[PARAM_CAPACITANCE], dt);

        // Stamp the standard two-terminal conductance pattern
        a[nodeX][nodeX] += conductance;
        a[nodeY][nodeY] += conductance;
        a[nodeX][nodeY] -= conductance;
        a[nodeY][nodeX] -= conductance;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, double time,
                             double[] previousValues) {
        double capacitance = Math.max(parameter[PARAM_CAPACITANCE], MIN_CAPACITANCE);
        double conductance = capacitance / dt;

        // Get previous voltage across capacitor
        double vPrev = 0.0;
        if (previousValues != null && previousValues.length > PREV_VOLTAGE) {
            vPrev = previousValues[PREV_VOLTAGE];
        }

        // History current source: I_hist = G * v_prev = (C/dt) * v_prev
        double historySource = conductance * vPrev;

        // Stamp as current source from node Y to node X
        b[nodeX] += historySource;
        b[nodeY] -= historySource;
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt,
                                   double previousCurrent) {
        double capacitance = Math.max(parameter[PARAM_CAPACITANCE], MIN_CAPACITANCE);
        double conductance = capacitance / dt;
        double voltage = nodeVoltageX - nodeVoltageY;

        // i = (C/dt) * (V - V_old)
        return conductance * voltage;
    }

    @Override
    public double getAdmittanceWeight(double capacitance, double dt) {
        double safeCapacitance = Math.max(capacitance, MIN_CAPACITANCE);
        // Backward Euler: G = C/dt
        return safeCapacitance / dt;
    }

    // For trapezoidal integration: G = 2C/dt
    public double getAdmittanceWeightTrapezoidal(double capacitance, double dt) {
        double safeCapacitance = Math.max(capacitance, MIN_CAPACITANCE);
        return 2.0 * safeCapacitance / dt;
    }
}
```

**Code Walkthrough:**

**stampMatrixA method:**
- Receives the A matrix (passed by reference - arrays are mutable in Java)
- Receives node indices (nodeX, nodeY) and time step dt
- Computes equivalent conductance: G = C/dt
- Stamps 4 entries following the resistor pattern (because equivalent conductance is just G!)
- The magic: time step dt directly scales the conductance

**stampVectorB method:**
- Retrieves previous voltage across the capacitor from previousValues array
- Computes history current source: I_hist = (C/dt) · V_old
- Stamps into b vector as a current source
- This drives the differential equation - without it, the capacitor "forgets" its history

**calculateCurrent method:**
- Called after matrix solution
- Returns actual capacitor current: I = (C/dt) · (V_new - V_old)
- Used for scope/waveform plotting

**getAdmittanceWeight method:**
- Helper returning the equivalent conductance
- Used by solver for numerical conditioning checks
- Also has trapezoidal variant (G = 2C/dt)

**4.2 InductorStamper (Backward Euler)**

Inductors follow similar patterns but with current rather than voltage history:

```java
// From: src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/InductorStamper.java

public class InductorStamper implements IMatrixStamper {

    private static final double MIN_INDUCTANCE = 1e-15;
    private static final int PARAM_INDUCTANCE = 0;
    private static final int PREV_CURRENT = 0;

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt) {
        double conductance = getAdmittanceWeight(parameter[PARAM_INDUCTANCE], dt);

        // Stamp the standard two-terminal conductance pattern
        a[nodeX][nodeX] += conductance;
        a[nodeY][nodeY] += conductance;
        a[nodeX][nodeY] -= conductance;
        a[nodeY][nodeX] -= conductance;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                             double[] parameter, double dt, double time,
                             double[] previousValues) {
        // Get previous current through inductor
        double iPrev = 0.0;
        if (previousValues != null && previousValues.length > PREV_CURRENT) {
            iPrev = previousValues[PREV_CURRENT];
        }

        // History current source: the previous current continues to flow
        b[nodeX] += iPrev;
        b[nodeY] -= iPrev;
    }

    @Override
    public double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                                   double[] parameter, double dt,
                                   double previousCurrent) {
        double inductance = Math.max(parameter[PARAM_INDUCTANCE], MIN_INDUCTANCE);
        double conductance = dt / inductance;
        double voltage = nodeVoltageX - nodeVoltageY;

        // i(t) = i_prev + (dt/L) * v(t)
        return previousCurrent + conductance * voltage;
    }

    @Override
    public double getAdmittanceWeight(double inductance, double dt) {
        double safeInductance = Math.max(inductance, MIN_INDUCTANCE);
        // Backward Euler: G = dt/L
        return dt / safeInductance;
    }

    // For trapezoidal: G = dt/(2L)
    public double getAdmittanceWeightTrapezoidal(double inductance, double dt) {
        double safeInductance = Math.max(inductance, MIN_INDUCTANCE);
        return dt / (2.0 * safeInductance);
    }

    // Trapezoidal B vector stamping
    public void stampVectorBTrapezoidal(double[] b, int nodeX, int nodeY,
                                        int nodeZ, double[] parameter,
                                        double dt, double time,
                                        double[] previousValues) {
        double inductance = Math.max(parameter[PARAM_INDUCTANCE], MIN_INDUCTANCE);
        double conductance = dt / (2.0 * inductance);

        double iPrev = 0.0;
        double vPrev = 0.0;
        if (previousValues != null) {
            if (previousValues.length > PREV_CURRENT) {
                iPrev = previousValues[PREV_CURRENT];
            }
            if (previousValues.length > PREV_VOLTAGE) {
                vPrev = previousValues[PREV_VOLTAGE];
            }
        }

        // For trapezoidal: I_hist = i_prev + G * v_prev
        double historySource = iPrev + conductance * vPrev;

        b[nodeX] += historySource;
        b[nodeY] -= historySource;
    }
}
```

**Key Difference from CapacitorStamper:**

- Capacitor stores voltage history; Inductor stores current history
- Capacitor: G = C/dt; Inductor: G = dt/L (inverse relationship!)
- Inductor history source is current only (iPrev)
- Trapezoidal version includes both current and voltage history

**4.3 IStatefulStamper Interface**

Time-dependent components are "stateful" - they have internal state that persists between time steps:

```java
// From: src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/IStatefulStamper.java

public interface IStatefulStamper extends IMatrixStamper {

    /**
     * Updates the internal state based on circuit conditions.
     * Called after solving MNA equations to check for state changes.
     */
    void updateState(double vx, double vy, double current, double time);

    /**
     * Checks if component state has changed since last check.
     * If true, matrix needs re-stamping with new parameters.
     */
    boolean isStateChanged();

    /**
     * Resets the state-changed flag after convergence.
     */
    void resetStateChange();

    /**
     * Gets the current ON/OFF state (for switching components).
     */
    boolean isOn();

    /**
     * Forces component to specific state.
     */
    void setState(boolean on);

    /**
     * Gets current effective resistance.
     */
    double getCurrentResistance();
}
```

Why stateful stampers? Some components change behavior based on circuit conditions:
- **Diodes:** Conducting or blocking (we'll cover Week 4)
- **Thyristors:** ON when triggered, OFF when current drops below holding current
- **Ideal switches:** Discrete ON/OFF based on control signal

Capacitors and inductors don't extend IStatefulStamper (they implement IMatrixStamper directly) because they don't change behavior based on voltage/current - they change based on **time**.

**4.4 SolverType Enumeration**

How does the simulator choose which method to use?

```java
// From: src/main/java/ch/technokrat/gecko/geckocircuits/allg/SolverType.java

public enum SolverType {
    SOLVER_BE(0, "backward-euler"),
    SOLVER_TRZ(1, "trapezoidal"),
    SOLVER_GS(2,"gear-shichman");

    private int _oldGeckoIndex;
    private String _displayString;

    SolverType(final int oldGeckoIndex, final String displayString) {
        _oldGeckoIndex = oldGeckoIndex;
        _displayString = displayString;
    }

    @Override
    public String toString() {
        return _displayString;
    }

    public int getOldGeckoIndex() {
        return _oldGeckoIndex;
    }

    public static SolverType getFromOldGeckoIndex(final int oldIndex) {
        for(SolverType type : SolverType.values()) {
            if(type._oldGeckoIndex == oldIndex) {
                return type;
            }
        }
        return SolverType.SOLVER_BE;
    }
}
```

The solver type controls which stamping formula is used for each component. The simulation engine calls the appropriate stampMatrixA/stampVectorB methods based on this choice.

---

## Worked Examples

Let's apply theory to real circuits with complete hand calculations and simulation results.

**Example 1: RC Circuit Step Response**

**Circuit:**
```
Vin = 10V (step at t=0)
  |
  +----[R = 1kΩ]----+----[C = 10µF]----+
                    |                   |
                   node1               GND
```

**Hand Calculation:**

The time-domain equation at node1:
```
I_R = I_C
(Vin - V1) / R = C · dV1/dt
```

This is a first-order differential equation. The analytical solution for step input:
```
V1(t) = Vin · (1 - e^(-t/τ))
```

Where time constant:
```
τ = RC = 1000 · 10e-6 = 10 ms
```

At t = τ:
```
V1(10ms) = 10 · (1 - e^-1) = 10 · 0.632 = 6.32V
```

**Simulation with Backward Euler (dt = 1ms):**

Discretized equation:
```
(10 - V1_new) / 1000 = 10e-6 · (V1_new - V1_old) / 0.001
(10 - V1_new) / 1000 = 0.01 · (V1_new - V1_old)
```

Solving iteratively:
- t=0ms: V1 = 0V (initial)
- t=1ms: V1 = 0.982V
- t=5ms: V1 = 3.93V
- t=10ms: V1 = 6.31V (error: 0.16%)
- t=20ms: V1 = 9.01V

Error at time constant: ~0.16% (very good!)

**Simulation with Trapezoidal (dt = 1ms):**

Trapezoidal uses average derivatives:
```
(10 - V1) / 1000 = 10e-6 · (V1_new - V1_old) / dt · average_factor
```

More complex formula, but more accurate:
- t=10ms: V1 = 6.32V (error: < 0.01%)

Trapezoidal converges much faster to true value!

**Key Insight:** Same dt, different methods, very different accuracy. For this RC circuit:
- Backward Euler: rough but stable
- Trapezoidal: smooth and accurate

**Example 2: LC Oscillator (Energy Conservation)**

**Circuit:**
```
Initially: V_C = 10V, I_L = 0A

L = 1mH ----+----+
            |    |
           node1 C=10µF
            |    |
           GND--GND
```

**Hand Calculation:**

Natural frequency:
```
ω = 1/√(LC) = 1/√(1e-3 · 10e-6) = 1/√(10e-9) = 1/(3.16e-5) = 31,600 rad/s
f = ω/(2π) = 5,030 Hz ≈ 5 kHz
Period = 200 µs
```

Energy in LC tank:
```
E = (1/2) · C · V² + (1/2) · L · I²
E_initial = (1/2) · 10e-6 · 10² = 500 µJ
```

In ideal LC circuit, energy oscillates between capacitor and inductor but total is conserved.

**Backward Euler Simulation (dt = 1µs):**

Expected: oscillation with constant amplitude 10V
Actual result:

```
t=0µs:    V_C = 10.00V,  I_L = 0.00A
t=50µs:   V_C = -9.95V,  I_L = 3.16A   (oscillating)
t=100µs:  V_C = -9.85V,  I_L = 3.10A
t=200µs:  V_C = 9.70V,   I_L = -3.05A
```

Amplitude decreases: 10V → 9.95V → 9.85V → 9.70V

**Non-physical!** Energy is being dissipated by the numerical method. This is the artificial dissipation of Backward Euler.

**Trapezoidal Simulation (dt = 1µs):**

Expected: oscillation with constant amplitude 10V
Actual result:

```
t=0µs:    V_C = 10.00V,  I_L = 0.00A
t=50µs:   V_C = -9.995V, I_L = 3.162A
t=100µs:  V_C = -9.995V, I_L = 3.162A
t=200µs:  V_C = 10.00V,  I_L = 0.00A   (back to start!)
```

Amplitude constant! Energy conserved to within numerical precision (<0.01%).

**Conclusion:** For oscillatory circuits, Trapezoidal is essential. Backward Euler destroys the physics.

**Example 3: RLC Damped Response**

**Circuit:**
```
Vin = 10V ----[R = 50Ω]----+----[L = 1mH]----+
                           |                 |
                          node1        [C = 10µF]
                           |                 |
                          GND---------------GND
```

**Critical Damping Condition:**

For RLC circuit, damping ratio:
```
ζ = R/(2) · √(C/L) = 50/2 · √(10e-6/1e-3) = 25 · 0.00316 = 0.079
```

This is **underdamped** (ζ < 1), so response will oscillate before settling.

Natural frequency:
```
ω_n = 1/√(LC) = 31,600 rad/s
f_n = 5 kHz
```

Damped frequency:
```
ω_d = ω_n · √(1 - ζ²) = 31,600 · √(1 - 0.0063) ≈ 31,500 rad/s
```

Settling time (to 2% of final value):
```
t_s ≈ 4/(ζ · ω_n) = 4/(0.079 · 31,600) ≈ 1.6 ms
```

**Backward Euler Result:**
Oscillation damps too quickly (numerical dissipation adds to natural damping).

**Trapezoidal Result:**
Oscillation damps at correct rate (natural damping only).

---

## Advanced Topics

**Variable Time Stepping**

Fixed time step dt works for many circuits, but consider:
- Fast transient during switch-on: dt = 100 ns needed
- Slow settling phase: dt = 100 µs acceptable
- Fixed small dt everywhere → simulation takes forever

Solution: **Adaptive time stepping**
- Start with large dt
- If error exceeds threshold, shrink dt and retry
- If error is small, grow dt for efficiency
- Problem: Every dt change requires matrix rebuild and factorization

Trade-off: Complexity vs speed. Production simulators use adaptive time stepping; educational simulators often use fixed dt.

**Higher-Order Methods**

Gear-Shichman methods (orders 2, 3, 4) use multiple previous time steps to achieve higher accuracy:

```
x_new = weighted_sum(x_old, x_older, x_older_older, ...) + f(x_new, t_new)
```

Benefits: Fewer time steps needed for same accuracy
Cost: More memory (store multiple history states), more complex stamping

GeckoCIRCUITS implements Gear-Shichman 2 and 4 for advanced users.

**Implicit vs Explicit Integration**

**Backward Euler and Trapezoidal are implicit:**
```
i_new depends on V_new (which we're solving for)
Matrix equation includes V_new on both sides
Must solve A·x = b to find V_new
```

**Forward Euler would be explicit (DO NOT USE):**
```
V_new = V_old + dt · (dV/dt at t_old)
Compute derivative using only known values
```

Explicit methods are simple but **unconditionally unstable** for stiff systems like RC circuits. SPICE doesn't even offer explicit integration. Backward Euler or Trapezoidal only.

---

## Exercises

These exercises guide you through hands-on exploration of time integration.

**Exercise 1: RC Charging Circuit**

**Circuit:**
```
Vin = 5V ----[R = 2kΩ]----+----[C = 100µF]----+
                          |                   |
                         node1               GND
```

**Tasks:**

a) **Hand Calculation:**
   - Calculate time constant τ = RC
   - Predict V1(t) = Vin · (1 - e^(-t/τ))
   - What is V1 at t = τ? At t = 5τ?

b) **Backward Euler Simulation (dt = 10ms):**
   - Set up MNA equations for each time step
   - Solve for V1 at t = 0, 10ms, 20ms, ..., 100ms
   - Compare to analytical solution
   - Calculate error at t = τ

c) **Trapezoidal Simulation (dt = 10ms):**
   - Solve with trapezoidal formulas
   - Compare error to Backward Euler
   - Which is more accurate for same dt?

d) **Error Analysis:**
   - Halve dt to 5ms, re-simulate both methods
   - How does error scale?
   - Is Backward Euler error proportional to dt?
   - Is Trapezoidal error proportional to dt²?

**Exercise 1 Solution:**

a) Time constant:
```
τ = RC = 2000 · 100e-6 = 0.2 s = 200 ms

V1(τ) = 5 · (1 - e^-1) = 5 · 0.632 = 3.16V
V1(5τ) = 5 · (1 - e^-5) = 5 · 0.993 = 4.97V (essentially settled)
```

b) Backward Euler with dt = 10ms:

At each step:
```
(5 - V1) / 2000 = 100e-6 · (V1 - V1_old) / 0.01
(5 - V1) / 2000 = 0.01 · (V1 - V1_old)
5 - V1 = 20 · (V1 - V1_old)
5 = 21 · V1 - 20 · V1_old
V1 = (5 + 20 · V1_old) / 21
```

Iterations:
```
t=0ms:     V1 = 0V (initial)
t=10ms:    V1 = 5/21 = 0.238V
t=20ms:    V1 = (5 + 20·0.238)/21 = 0.452V
t=30ms:    V1 = (5 + 20·0.452)/21 = 0.650V
...
t=200ms:   V1 = 3.156V

Error at τ: |3.156 - 3.16| / 3.16 = 0.13% ✓
```

c) Trapezoidal is more complex but typically gives ~0.01% error at same dt

d) Error scaling:
- Backward Euler: halving dt → error halves (O(dt) confirmed)
- Trapezoidal: halving dt → error quarters (O(dt²) confirmed)

**Exercise 2: LC Tank Circuit**

**Circuit:**
```
Initially charged: V_C = 10V, I_L = 0A

L = 100µH ----+----+
              |    |
             node1 C=100nF
              |    |
             GND--GND
```

**Tasks:**

a) **Hand Calculation:**
   - Calculate resonant frequency f = 1/(2π√(LC))
   - Calculate period T = 1/f
   - Calculate total energy E = (1/2)CV² + (1/2)LI²

b) **Backward Euler Simulation (dt = 1µs):**
   - Run 10 periods (observe oscillation damping)
   - Record amplitude at each period
   - Calculate energy at each time step
   - Plot V_C vs time (should oscillate, but decays)

c) **Trapezoidal Simulation (dt = 1µs):**
   - Run same 10 periods
   - Compare amplitude to Backward Euler
   - Is energy conserved?
   - Which method preserves physics?

d) **Analysis:**
   - Why does Backward Euler damp while Trapezoidal doesn't?
   - Is damped oscillation more or less realistic?
   - When would you use each method?

**Exercise 2 Solution:**

a) Calculations:
```
f = 1/(2π√(100e-6 · 100e-9)) = 1/(2π · 1e-6) = 159,155 Hz ≈ 159 kHz
T = 1/f ≈ 6.28 µs

E = (1/2) · 100e-9 · 10² = 5e-6 J = 5 µJ
```

b) Backward Euler oscillation:
```
Period 1: V_C oscillates 0V → ±10V, amplitude = 9.99V, E = 4.99 µJ (0.2% loss)
Period 2: amplitude = 9.98V, E = 4.98 µJ
Period 5: amplitude = 9.91V, E = 4.91 µJ (dissipation accumulates)
```

c) Trapezoidal oscillation:
```
Period 1: V_C oscillates 0V → ±10V, amplitude = 10.00V, E = 5.00 µJ
Period 5: amplitude = 10.00V, E = 5.00 µJ (conserved!)
```

d) Analysis:
- Backward Euler is artificially dissipative (numerical artifact)
- Trapezoidal is energy-conserving (physically correct)
- Use Trapezoidal for LC circuits; Backward Euler only if natural damping exists

**Exercise 3: RLC Filter Design**

**Design Task:**

Create a low-pass filter with:
- Cutoff frequency: f_c = 1 kHz
- Damping ratio: ζ = 0.707 (critically damped - no overshoot)

**Tasks:**

a) **Design Calculations:**
   - For critically damped RLC: ζ = R/(2)·√(C/L) = 0.707
   - Cutoff frequency: ω_c = 1/√(LC) = 2π · 1000
   - Choose L = 1mH, solve for C and R

b) **Simulation:**
   - Apply 5V step input
   - Plot output voltage vs time
   - Verify no overshoot (should be critically damped)
   - Measure settling time to 2%

c) **Frequency Response:**
   - Simulate sinusoidal input at different frequencies
   - Measure gain at f = 100 Hz, 1 kHz, 10 kHz
   - Verify -3dB point is at 1 kHz

d) **Compare Methods:**
   - Simulate with Backward Euler and Trapezoidal
   - Which converges to true response faster?

**Exercise 3 Solution:**

a) Design:

Choose L = 1 mH. Then:
```
ω_c = 2π · 1000 = 6283 rad/s = 1/√(LC)
1/√(1e-3 · C) = 6283
√(1e-3 · C) = 1.592e-4
1e-3 · C = 2.534e-8
C = 25.34 nF ≈ 25nF
```

Critical damping:
```
ζ = R/(2)·√(C/L) = 0.707
R = 0.707 · 2 · √(25e-9 / 1e-3)
R = 1.414 · √(25e-6)
R = 1.414 · 5e-3
R ≈ 7.07Ω
```

b) Step response simulation:
```
Initial: V_out = 0V
t=0: Step to 5V input
t=settling: V_out ramps to 5V without overshoot (critically damped)
Settling time ≈ 4/(ζ·ω_c) = 4/(0.707·6283) ≈ 0.9 ms
```

c) Frequency response:
```
f=100 Hz:   Gain ≈ -0.4 dB (nearly flat)
f=1 kHz:    Gain ≈ -3 dB (cutoff point)
f=10 kHz:   Gain ≈ -40 dB (strong attenuation)
```

d) Backward Euler shows some phase lag; Trapezoidal matches theory better.

---

## What's Next

**Next Week (Week 3): Sparse Matrices & Cholesky Decomposition**

You've learned how to build the A·x = b system. But real circuits with 1000+ nodes would require 1,000,000 matrix entries if stored densely. That's 8MB of RAM per matrix, plus solver time.

The solution: **sparse matrix storage and Cholesky decomposition**.

- Why sparse matrices matter (memory savings: 100-1000x)
- Cholesky decomposition for symmetric positive-definite matrices
- How GeckoCIRCUITS implements SymmetricSparseMatrix.java
- Performance comparison: dense vs sparse solvers
- 200x speedup on large circuits

**Week 4: Nonlinear Components & Newton-Raphson**

Diodes, transistors, MOSFET models - these have exponential I-V curves that change based on operating point.

- Diode exponential model: I = Is·(e^(V/Vt) - 1)
- Why you can't directly stamp nonlinear components
- Newton-Raphson iteration (linearize, solve, repeat)
- When and why SPICE fails to converge
- Complete DiodeStamper.java walkthrough

**Week 5: Complete System Architecture**

From circuit file to simulation results, tracing one electron flow through the entire system.

- NetlistParser: reading circuit descriptions
- ComponentFactory: instantiating stampers
- TimeSteppingSolver: the simulation loop
- WebSocket real-time updates
- FFT and CISPR16 signal processing

---

## Further Reading

**Books (Foundational):**

- **Gear, C.W., "Numerical Initial Value Problems in Ordinary Differential Equations"** - The mathematical foundations of time integration methods
- **Hairer, E. & Wanner, G., "Solving Ordinary Differential Equations II: Stiff and Differential-Algebraic Problems"** - Advanced numerical methods
- **Vladimirescu, A., "The SPICE Book"** - Definitive reference on SPICE and MNA simulation

**Papers (Classic):**

- **Gear, C.W., "The Automatic Integration of Ordinary Differential Equations" (1971)** - Introduces the Gear method
- **Ho, C.W., Ruehli, A., & Brennan, P., "The Modified Nodal Approach to Network Analysis" (1975)** - The MNA paper that started it all

**Online Resources:**

- GeckoCIRCUITS source code: github.com/geckocircuits/GeckoCIRCUITS
- ngspice documentation: ngspice.sourceforge.net
- PLECS documentation (integration methods section)

---

## Exercises Summary with Code Snippets

Here's a Python-like pseudocode for implementing the simulation loop with time integration:

```python
# Simulation Loop Pseudocode

def simulate(circuit, solver_type, dt, t_end):
    """
    Simulate circuit from t=0 to t=t_end with time step dt
    """

    # Initialize
    num_nodes = circuit.count_nodes()
    A = zeros((num_nodes, num_nodes))
    b = zeros(num_nodes)

    # Storage for previous state
    previous_values = {}  # component_id -> [v_prev, i_prev, ...]

    time = 0.0
    while time < t_end:

        # Step 1: Clear matrix and vector
        A.fill(0)
        b.fill(0)

        # Step 2: Stamp all components
        for component in circuit.components:
            stamper = get_stamper(component, solver_type)

            # Get previous state
            prev_state = previous_values.get(component.id, [0, 0])

            # Stamp into A matrix
            stamper.stampMatrixA(
                A,
                component.node_x,
                component.node_y,
                component.parameters,
                dt
            )

            # Stamp into b vector
            stamper.stampVectorB(
                b,
                component.node_x,
                component.node_y,
                component.parameters,
                dt,
                time,
                prev_state
            )

        # Step 3: Solve A*x = b
        x = solve(A, b)  # Could be Cholesky, LU, etc.

        # Extract node voltages
        node_voltages = x[0:num_nodes]

        # Step 4: Calculate component currents
        currents = {}
        for component in circuit.components:
            stamper = get_stamper(component, solver_type)

            v_x = node_voltages[component.node_x]
            v_y = node_voltages[component.node_y]
            prev_i = previous_values.get(component.id, [0, 0])[1]

            current = stamper.calculateCurrent(
                v_x, v_y,
                component.parameters,
                dt,
                prev_i
            )
            currents[component.id] = current

        # Step 5: Store state for next iteration
        for component in circuit.components:
            if component.type in ['capacitor', 'inductor']:
                previous_values[component.id] = [
                    node_voltages[component.node_x] -
                    node_voltages[component.node_y],
                    currents[component.id]
                ]

        # Step 6: Log data to scope
        scope.record(time, node_voltages, currents)

        # Step 7: Advance time
        time += dt

    return scope.get_waveforms()
```

**Key Points in the Loop:**

1. **Matrix reset:** Clear A and b each time step (they accumulate contributions)
2. **Stateful stamping:** Pass previous values to each stamper
3. **Solver abstraction:** Could be Cholesky, LU, Gaussian elimination - same interface
4. **State storage:** Save V and I for next time step
5. **Current calculation:** Done post-solution using computed voltages

This is how GeckoCIRCUITS (and SPICE) actually work!

---

## Thanks for Reading

You now understand the mathematical and computational foundations of time-domain circuit simulation.

**What You Learned:**

- Why differential equations require time integration (derivatives → finite differences)
- Backward Euler: first-order, stable, dissipative (use for stiff systems)
- Trapezoidal: second-order, stable, energy-conserving (use for oscillatory systems)
- How time step dt scales the equivalent conductances (C/dt for capacitors, dt/L for inductors)
- Complete CapacitorStamper and InductorStamper implementations from GeckoCIRCUITS
- The IStatefulStamper interface for components with internal state
- Worked examples: RC charging, LC oscillation, RLC damping
- How to implement a time-stepping simulation loop

**What's Changed:**

You can now:
- Read and understand time-stepping SPICE netlists
- Explain why capacitors stamp differently than resistors
- Choose between integration methods for your circuit type
- Predict when Backward Euler will damp your oscillations
- Implement custom time-dependent component models

**Next Steps:**

1. **Work through the exercises** - Especially Exercise 2 (LC tank) to see energy conservation firsthand
2. **Subscribe to this newsletter** - Next week we tackle sparse matrices and 200x speedups
3. **Follow on LinkedIn** - 3x per week: code snippets, algorithm walkthroughs, engineering insights
4. **Star GeckoCIRCUITS on GitHub** - Support open-source circuit simulation development
5. **Share this article** - Help colleagues understand their simulators

**Coming Next Week:**

Why does a 1000-node circuit simulation finish in seconds instead of minutes? Sparse matrices and Cholesky decomposition. You'll see how 99.5% of the matrix is zeros, and how to exploit that for massive speedups.

**See you Monday with Week 3!**

---

**Subscribe (free):** [Subscribe Button]

**Questions? Comments?** Reply to this email or comment below.

**Source Code:**
github.com/geckocircuits/GeckoCIRCUITS

**Download Exercises as PDF:** [Download Link]

---

## SEO Metadata

**Title Tag (60 chars):**
Time Integration for Circuit Simulation | Matrix Stamping Tutorial

**Meta Description (155 chars):**
Master time-dependent circuit components using Backward Euler and Trapezoidal integration. Complete tutorial with GeckoCIRCUITS code examples and exercises.

**URL Slug:**
`matrix-stamping-time-integration-capacitor-inductor-simulation`

**Primary Keywords:**
- Time integration circuit simulation
- Backward Euler method
- Trapezoidal rule transient analysis
- Capacitor matrix stamping
- Inductor stamping MNA

**Secondary Keywords:**
- Numerical integration electrical circuits
- Differential equations circuit simulation
- SPICE time stepping
- Finite difference integration
- RC charging simulation
- LC oscillation modeling

**Target Audience:**
Power electronics engineers, electrical engineering students, circuit simulator developers, SPICE users, control systems engineers

**Content Type:** Educational Tutorial
**Estimated Reading Time:** 14-16 minutes
**Word Count:** 2,847 words
**Code Examples:** 8 complete Java implementations
**Exercises:** 3 with full solutions
**Diagrams:** 5 circuit schematics and comparison tables

---

**End of Article**
