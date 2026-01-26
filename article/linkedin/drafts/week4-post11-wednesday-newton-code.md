# LinkedIn Post #11 (Wednesday) - Week 4

**Publishing Date:** Week 4, Wednesday 8am
**Topic:** Newton-Raphson Implementation Code
**Target:** Engineers ready for iterative solver details
**Goal:** Show iteration loop, explain convergence, drive to weekend article
**Word Count:** 501 words

---

## Post Content

Newton-Raphson in action: Linearize. Stamp. Solve. Check. Repeat. Here's how GeckoCIRCUITS converges to the solution.

Monday I explained why diodes break MNA (exponential I-V curve can't be directly stamped). Today: the actual iteration code.

**Newton-Raphson Algorithm:**

```java
// From: SolverContext.java (simplified)
public class SolverContext {

    private static final int MAX_ITERATIONS = 100;
    private static final double TOLERANCE = 1e-6;  // 1 microvolt

    public double[] solveNonlinear(Circuit circuit, double[] initialGuess) {
        double[] x = initialGuess.clone();
        int iteration = 0;

        while (iteration < MAX_ITERATIONS) {
            // Step 1: Build linearized system around current guess
            double[][] A = new double[numNodes][numNodes];
            double[] b = new double[numNodes];

            for (Component component : circuit.getAllComponents()) {
                IMatrixStamper stamper = component.getStamper();

                // For nonlinear components, stamper linearizes around x
                stamper.stampMatrixA(A, nodeX, nodeY, nodeZ, params, dt);
                stamper.stampVectorB(b, nodeX, nodeY, nodeZ, params,
                                    dt, time, x);  // x = current guess
            }

            // Step 2: Solve linearized system
            double[] x_new = choleskySolver.solve(A, b);

            // Step 3: Check convergence
            double maxError = 0.0;
            for (int i = 0; i < numNodes; i++) {
                double error = Math.abs(x_new[i] - x[i]);
                if (error > maxError) {
                    maxError = error;
                }
            }

            // Step 4: Update guess
            x = x_new;
            iteration++;

            // Step 5: Converged?
            if (maxError < TOLERANCE) {
                return x;  // Success!
            }
        }

        // Failed to converge
        throw new ConvergenceException(
            "Newton-Raphson did not converge after " + MAX_ITERATIONS + " iterations"
        );
    }
}
```

**Diode Stamper (Linearization):**

```java
// From: DiodeStamper.java (simplified)
public class DiodeStamper implements IMatrixStamper {

    @Override
    public void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt) {
        // Linearize diode: I = Is*(e^(V/Vt) - 1)
        // Derivative: dI/dV = (Is/Vt)*e^(V/Vt) = G_eq

        double V_current = getCurrentVoltage(nodeX, nodeY);  // From last iteration
        double G_eq = (IS / VT) * Math.exp(V_current / VT);  // Incremental conductance

        // Stamp like a resistor with G = G_eq
        a[nodeX][nodeX] += G_eq;
        a[nodeY][nodeY] += G_eq;
        a[nodeX][nodeY] -= G_eq;
        a[nodeY][nodeX] -= G_eq;
    }

    @Override
    public void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                            double[] parameter, double dt, double time,
                            double[] previousValues) {
        // History current: difference between full equation and linearization
        double V_current = previousValues[nodeX] - previousValues[nodeY];
        double I_actual = IS * (Math.exp(V_current / VT) - 1);
        double G_eq = (IS / VT) * Math.exp(V_current / VT);
        double I_linearized = G_eq * V_current;

        // Stamp correction term
        double I_history = I_actual - I_linearized;
        b[nodeX] -= I_history;
        b[nodeY] += I_history;
    }
}
```

**What's Happening:**

**SolverContext (iteration loop):**
1. Build A and b using current voltage guess
2. Solve linear system → get new voltage estimate
3. Check if |V_new - V_old| < tolerance
4. If yes: converged! If no: repeat with V_new

**DiodeStamper (linearization):**
- Computes G_eq = dI/dV at current operating point
- Stamps G_eq like a resistor
- Adds history current to correct for linearization error

**Convergence Criteria:**

Different simulators use different criteria:
- Absolute voltage error: |V_new - V_old| < 1µV
- Relative voltage error: |V_new - V_old| / |V_old| < 0.01%
- Current residual: |I_calculated - I_stamped| < 1µA

GeckoCIRCUITS uses a combination.

**Why Convergence Fails:**

- Initial guess too far from solution
- Timestep too large (nonlinearity too strong)
- Multiple stable solutions (bistable circuit)
- Numerical precision issues

**Friday Preview:**

Weekend article: "Nonlinear Simulation: Newton-Raphson & Solver Comparison"
→ Complete Newton-Raphson derivation
→ Diode, BJT, MOSFET models
→ Backward Euler vs Trapezoidal vs Gear-Shichman
→ When to use each solver

Subscribe now for Saturday's deep-dive.

Source: github.com/geckocircuits/GeckoCIRCUITS
Files: SolverContext.java:100-200, DiodeStamper.java:60-140

---

**Hashtags:**
#NewtonRaphson #NonlinearSolvers #DiodeModeling #NumericalMethods #CircuitSimulation

**CTA:** Subscribe for weekend solver comparison

**Source File References:**
- SolverContext.java:100-200
- DiodeStamper.java:60-140
- SolverSettings.java:25-50

**Metrics:**
- Word count: 501
- Reading time: 2.6 min
- Code snippets: 2 (iteration loop + diode linearization)
- Max iterations: 100, tolerance: 1µV
