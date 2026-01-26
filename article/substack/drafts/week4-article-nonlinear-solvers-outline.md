# Week 4 Article Outline: Nonlinear Simulation: Newton-Raphson & Solver Comparison

**Target:** Engineers dealing with nonlinear circuits and convergence issues
**Word Count:** 2,900-3,300 words
**Reading Time:** 14-17 minutes
**Prerequisites:** Weeks 1-3 (MNA, time integration, Cholesky), calculus (derivatives)

---

## Structure:

### 1. Executive Summary (250w)

**Hook:**
Diodes follow I = Is·e^(V/Vt). MOSFETs have piecewise nonlinear I-V curves. You can't stamp an exponential into a linear matrix. So circuit simulators iterate: linearize, stamp, solve, check, repeat. Welcome to Newton-Raphson.

**What You'll Learn:**
- Why nonlinear components break the linear MNA formulation
- Newton-Raphson algorithm from first principles
- Diode, BJT, MOSFET modeling and linearization
- Convergence criteria and failure modes
- Solver comparison: Backward Euler, Trapezoidal, Gear-Shichman
- How to debug "SPICE failed to converge" errors

**Prerequisites:**
- MNA and stamping (Week 1)
- Time integration (Week 2)
- Cholesky solver (Week 3)
- Calculus: derivatives, Taylor series

**Time Investment:** 16 min reading + 45 min exercises

---

### 2. The Nonlinearity Problem (350w)

**Linear Components Recap:**

Weeks 1-3 covered linear components:
- Resistors: I = V/R
- Capacitors: I = C·dV/dt → linearizable with Backward Euler
- Inductors: V = L·dI/dt → linearizable

All reduce to: **A·x = b** (linear system)

**Nonlinear Components:**

**Diode:** I = Is·(e^(V/Vt) - 1)
- Exponential relationship
- Is ≈ 10^-12 A (saturation current)
- Vt ≈ 0.026 V (thermal voltage at 25°C)

**BJT (Ebers-Moll model):**
- I_C = β·I_B where I_B = Is·(e^(V_BE/Vt) - 1)
- Nonlinear in V_BE

**MOSFET (Level 1 model):**
- Cutoff: I_D = 0 (V_GS < V_th)
- Linear: I_D = k·[(V_GS - V_th)·V_DS - V_DS²/2] (V_DS < V_GS - V_th)
- Saturation: I_D = k·(V_GS - V_th)²/2 (V_DS > V_GS - V_th)
- Piecewise nonlinear!

**Why This Breaks MNA:**

Can't stamp nonlinear function directly:
```java
// This doesn't work - V is unknown!
a[nodeX][nodeX] += dI/dV(V);  // V not yet solved
```

Chicken-and-egg: Need V to compute conductance, need conductance to solve for V.

**Other Nonlinear Examples:**
- Thermal resistors: R(T) where T = P·R_thermal
- Magnetic cores: B-H curve with saturation
- Varistors: V = k·I^α (power law)

---

### 3. Newton-Raphson Theory (900w)

**3.1 Scalar Case (Single Nonlinear Equation)**

Problem: Find x such that f(x) = 0

Newton-Raphson iteration:
1. Start with guess x_0
2. Linearize: f(x) ≈ f(x_k) + f'(x_k)·(x - x_k)
3. Set to zero: 0 = f(x_k) + f'(x_k)·(x_{k+1} - x_k)
4. Solve: x_{k+1} = x_k - f(x_k)/f'(x_k)
5. Repeat until |x_{k+1} - x_k| < tolerance

**Example:** Find voltage across diode with 1mA current

f(V) = Is·(e^(V/Vt) - 1) - 0.001 = 0

f'(V) = (Is/Vt)·e^(V/Vt)

Iteration:
- x_0 = 0.6V (guess)
- x_1 = 0.6 - f(0.6)/f'(0.6) = 0.583V
- x_2 = 0.583 - f(0.583)/f'(0.583) = 0.5829V
- Converged! (|x_2 - x_1| < 1µV)

**3.2 Vector Case (Circuit with Multiple Nonlinear Components)**

Problem: Find x such that F(x) = 0 (x is vector, F is vector function)

For circuits: F(x) = A(x)·x - b(x)

Newton-Raphson iteration:
1. Start with guess x_0
2. Compute Jacobian: J = ∂F/∂x
3. Solve: J·Δx = -F(x_k)
4. Update: x_{k+1} = x_k + Δx
5. Repeat until ||Δx|| < tolerance

**3.3 Linearization: Key Insight**

For diode: I = Is·(e^(V/Vt) - 1)

Linearize around operating point V_k:
I ≈ I_k + G_eq·(V - V_k)

Where:
- I_k = Is·(e^(V_k/Vt) - 1) (current at operating point)
- G_eq = dI/dV|_{V=V_k} = (Is/Vt)·e^(V_k/Vt) (incremental conductance)

Rearrange:
I ≈ G_eq·V + (I_k - G_eq·V_k)

Two parts:
- G_eq·V → stamps into matrix A (like resistor)
- (I_k - G_eq·V_k) → stamps into vector b (history current)

This is how nonlinear components stamp!

**3.4 Convergence Criteria**

Different criteria:

**Absolute Voltage Error:**
max_i |V_i^{k+1} - V_i^k| < ε_V (e.g., ε_V = 1µV)

**Relative Voltage Error:**
max_i |V_i^{k+1} - V_i^k| / |V_i^k| < ε_rel (e.g., ε_rel = 0.01%)

**Current Residual:**
max_i |I_i^{calculated} - I_i^{stamped}| < ε_I (e.g., ε_I = 1nA)

**Combined:**
Use all three. If any fails, continue iterating.

**Damping (for stability):**
Instead of x_{k+1} = x_k + Δx, use:
x_{k+1} = x_k + α·Δx where 0 < α ≤ 1

Reduces step size if iteration diverges.

---

### 4. Implementation in GeckoCIRCUITS (1,000w)

**4.1 DiodeStamper Class**

Complete implementation:

```java
public class DiodeStamper implements IMatrixStamper {

    private static final double IS = 1e-12;  // Saturation current
    private static final double VT = 0.026;  // Thermal voltage

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY,
                            int nodeZ, double[] parameter, double dt) {
        // Get current voltage estimate from last iteration
        double V = getCurrentVoltage(nodeX, nodeY);

        // Compute incremental conductance: G_eq = dI/dV
        double G_eq = (IS / VT) * Math.exp(V / VT);

        // Stamp like resistor with conductance G_eq
        a[nodeX][nodeX] += G_eq;
        a[nodeY][nodeY] += G_eq;
        a[nodeX][nodeY] -= G_eq;
        a[nodeY][nodeX] -= G_eq;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY,
                            int nodeZ, double[] parameter, double dt,
                            double time, double[] previousValues) {
        // Get current voltage estimate
        double V = previousValues[nodeX] - previousValues[nodeY];

        // Compute current at operating point
        double I_actual = IS * (Math.exp(V / VT) - 1);

        // Compute linearized current
        double G_eq = (IS / VT) * Math.exp(V / VT);
        double I_linear = G_eq * V;

        // History current: difference between actual and linear
        double I_history = I_actual - I_linear;

        // Stamp correction current
        b[nodeX] -= I_history;
        b[nodeY] += I_history;
    }

    @Override
    public double calculateCurrent(double Vx, double Vy,
                                   double[] parameter, double dt,
                                   double previousCurrent) {
        double V = Vx - Vy;
        return IS * (Math.exp(V / VT) - 1);
    }
}
```

**Line-by-line explanation:**
- stampMatrixA: Computes G_eq at current operating point, stamps like resistor
- stampVectorB: Computes correction current for linearization error
- calculateCurrent: After convergence, compute final current

**4.2 SolverContext: Newton-Raphson Loop**

```java
public class SolverContext {

    private static final int MAX_ITERATIONS = 100;
    private static final double TOLERANCE_V = 1e-6;  // 1 microvolt

    public double[] solveNonlinear(Circuit circuit, double[] initialGuess) {
        double[] x = initialGuess.clone();

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            // Build linearized system around current guess
            double[][] A = new double[n][n];
            double[] b = new double[n];

            // Stamp all components (nonlinear ones linearize around x)
            for (Component comp : circuit.getComponents()) {
                comp.getStamper().stampMatrixA(A, ...);
                comp.getStamper().stampVectorB(b, ..., x);
            }

            // Solve linearized system
            double[] x_new = cholesky.solve(A, b);

            // Check convergence
            double maxError = 0.0;
            for (int i = 0; i < n; i++) {
                maxError = Math.max(maxError, Math.abs(x_new[i] - x[i]));
            }

            // Update
            x = x_new;

            // Converged?
            if (maxError < TOLERANCE_V) {
                return x;  // Success!
            }
        }

        throw new ConvergenceException("Failed to converge");
    }
}
```

**Key points:**
- Each iteration solves a linear system (Cholesky from Week 3)
- Nonlinear components re-linearize every iteration
- Convergence checked before next iteration

**4.3 SolverSettings Class**

```java
public class SolverSettings {
    public SolverType solverType;  // BACKWARD_EULER, TRAPEZOIDAL, etc.
    public int maxIterations = 100;
    public double voltageToler ance = 1e-6;
    public double currentTolerance = 1e-9;
    public double dampingFactor = 1.0;  // 1.0 = no damping
}
```

User-configurable settings for convergence tuning.

---

### 5. Solver Comparison (700w)

**5.1 Backward Euler**

Properties:
- Implicit: A(V_{n+1})·V_{n+1} = b(V_{n+1})
- Unconditionally stable (A-stable)
- Numerically dissipative (damps oscillations)
- First-order accurate

Best for:
- Switching circuits (hard discontinuities)
- Stiff systems (widely separated time scales)
- Large time steps (stability over accuracy)

Worst for:
- Oscillatory systems (damps natural oscillations)
- Energy-conserving systems (LC tanks)

**5.2 Trapezoidal Rule**

Properties:
- Implicit: Uses average of t_n and t_{n+1}
- Unconditionally stable
- NOT dissipative (energy conserving)
- Second-order accurate

Best for:
- Oscillatory circuits (LC, RLC resonators)
- High-accuracy requirements
- Small time steps

Worst for:
- Switching circuits (can oscillate at discontinuities)
- Very stiff systems (requires tiny dt)

**5.3 Gear-Shichman Methods**

Variable-order backward differentiation formulas (BDF):
- Order 1: Same as Backward Euler
- Order 2: Better accuracy than BE, same stability
- Order 4: Even better accuracy

Adaptive:
- Increase order when smooth
- Decrease order at discontinuities
- Adjust time step based on local error estimate

Best for:
- General-purpose simulation
- Unknown system characteristics
- Long transients with mixed dynamics

Worst for:
- Simplicity (complex implementation)
- Debugging (hard to predict behavior)

**5.4 Comparison Table**

| Solver | Order | Stable | Dissipative | Nonlinear Convergence | Use Case |
|--------|-------|--------|-------------|----------------------|----------|
| BE | 1st | Yes | Yes | Good | Switches, stiff |
| TRZ | 2nd | Yes | No | Moderate | Oscillators, accuracy |
| GS-2 | 2nd | Yes | Some | Good | General purpose |
| GS-4 | 4th | Yes | Some | Moderate | Long transients |

---

### 6. Worked Examples (450w)

**Example 1: Diode Rectifier**

Circuit:
```
Vin=10V (sine) ----[D1]---- node1 ----[R=1kΩ]---- GND
                              |
                           [C=10µF]
                              |
                             GND
```

Newton-Raphson convergence:
- Iteration 0: V1 = 0V (initial guess)
- Iteration 1: V1 = 9.3V
- Iteration 2: V1 = 9.29V
- Converged after 2 iterations

Compare solvers:
- Backward Euler: Smooth output, slight distortion
- Trapezoidal: Oscillations at switching instants

**Example 2: BJT Common Emitter Amplifier**

Nonlinear: I_B = Is·(e^(V_BE/Vt) - 1), I_C = β·I_B

Newton-Raphson typically converges in 3-5 iterations.

Solver comparison:
- Backward Euler: Stable, slight gain error
- Trapezoidal: Accurate gain, can oscillate with large signals

**Example 3: Convergence Failure**

Circuit with bad initial guess:
- LED (V_f ≈ 3V) with guess V = 0V
- Large exponential gradient → large Newton step
- Overshoots → negative voltage → exp(-∞) = 0 → stuck

Solution: Better initial guess (DC operating point analysis first)

---

### 7. Debugging Convergence Failures (300w)

**Common Causes:**

1. **Bad initial guess:** Start with DC analysis, use previous time step
2. **Time step too large:** Reduce dt
3. **Poorly scaled circuit:** Normalize voltages/currents
4. **Truly chaotic:** Bistable circuits, no unique solution
5. **Numerical precision:** Use double precision, check for NaN/Inf

**Debugging Strategies:**

- Enable verbose logging (iteration count, max error)
- Plot Newton-Raphson trajectory
- Try damping (α = 0.5)
- Reduce time step
- Check component models (negative resistance?)
- Verify topology (floating nodes?)

**GeckoCIRCUITS Convergence Helpers:**

- Automatic damping when diverging
- DC operating point finder
- Adjustable tolerances per component type

---

### 8. Advanced Topics (200w)

**Continuation Methods:**
- Gradually ramp nonlinear components
- Start with linear approximation, increase nonlinearity

**Quasi-Newton Methods:**
- Approximate Jacobian (avoid recomputation)
- BFGS, Broyden updates

**Preview Week 5:**
Full simulation pipeline: file → parser → stampers → Newton-Raphson → scope

---

### 9. Exercises (250w)

**Exercise 1:** Diode with 1kΩ resistor, compute Newton-Raphson by hand (3 iterations)
**Exercise 2:** Compare BE vs TRZ for RC circuit with diode
**Exercise 3:** Debug convergence failure in provided circuit

Solutions provided.

---

### 10. What's Next (150w)

Week 5: Complete architecture (simulation pipeline, signal processing)

---

### 11. CTA (100w)

Subscribe, follow, exercises, share.

---

## Source Files:

- `DiodeStamper.java`
- `SolverContext.java`
- `SolverSettings.java`
- `SolverType.java`
- `ConvergenceException.java`

---

## Code Examples:

1. DiodeStamper complete implementation
2. Newton-Raphson loop
3. BJT stamper (Ebers-Moll)
4. MOSFET stamper (Level 1, piecewise)
5. Convergence checking
6. Damping implementation

---

## Diagrams:

1. Diode I-V curve (exponential)
2. Newton-Raphson geometric interpretation
3. Convergence trajectory plot
4. Rectifier circuit
5. BJT amplifier circuit
6. Solver comparison (BE vs TRZ waveforms)

---

## SEO Keywords:

- Newton-Raphson circuit simulation
- Nonlinear circuit solver
- SPICE convergence failure
- Diode modeling SPICE
- Backward Euler vs Trapezoidal
