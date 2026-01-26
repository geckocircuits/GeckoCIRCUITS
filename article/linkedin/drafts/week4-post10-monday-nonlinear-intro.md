# LinkedIn Post #10 (Monday) - Week 4

**Publishing Date:** Week 4, Monday 8am
**Topic:** Nonlinear Components (Why Diodes Break MNA)
**Target:** Engineers understanding linear MNA, ready for complexity
**Goal:** Introduce nonlinearity problem, tease Newton-Raphson solution
**Word Count:** 441 words

---

## Post Content

Linear components are easy. Diodes are not. You can't stamp an exponential function into a linear matrix. So how do simulators handle it?

**Linear Components: We've Got This**

Weeks 1-3 covered linear components:
- Resistors: I = V/R (algebraic)
- Capacitors: I = C·dV/dt (differential, but linearizable)
- Inductors: V = L·dI/dt (differential, but linearizable)

MNA reduces everything to: **A·x = b** (linear system)

Build matrix → solve → done.

**Nonlinear Components: Problem**

Diode I-V characteristic:

**I = Is · (e^(V/Vt) - 1)**

Where:
- Is = saturation current (~10^-12 A)
- Vt = thermal voltage (~26mV at room temp)
- e = Euler's number (2.718...)

This is **exponential**. Not linear. Can't directly stamp into A matrix.

Try it:
```java
// This doesn't work!
a[nodeX][nodeX] += Is * exp(V / Vt);  // V is unknown!
```

The problem: We're building A matrix **before** solving for V. But the diode's contribution **depends on** V. Chicken and egg.

**Other Nonlinear Components:**

MOSFETs: I_ds = f(V_gs, V_ds) (complex piecewise function)
BJTs: I_c = f(V_be, V_bc) (Ebers-Moll model)
Thermal resistors: R = R0·(1 + α·T) where T depends on power dissipation
Magnetic cores: B-H curves (hysteresis, saturation)

All nonlinear. All break the linear A·x = b formulation.

**The Solution: Newton-Raphson Iteration**

Idea: **Linearize locally, solve, update, repeat.**

Algorithm:
1. Guess initial voltage V_guess (start with 0V)
2. Linearize diode around V_guess: I ≈ I0 + G·(V - V_guess)
3. Stamp linearized G into matrix A (like a resistor!)
4. Stamp I0 into vector b (like a current source!)
5. Solve A·x = b → get V_new
6. Check convergence: |V_new - V_guess| < tolerance?
7. If converged: done. If not: V_guess = V_new, go to step 2.

This is **iterative**. Each iteration solves a linear system. Loop until convergence.

**Why SPICE Simulations Sometimes Fail:**

"SPICE failed to converge"

Translation: Newton-Raphson iteration didn't converge.

Reasons:
- Bad initial guess
- Poorly scaled circuit
- Truly chaotic behavior (oscillation, bistability)
- Too-tight tolerance

Understanding Newton-Raphson helps you fix convergence issues!

**Wednesday Preview:**

I'll show you actual Newton-Raphson code from GeckoCIRCUITS:
→ DiodeStamper.java (how diodes linearize)
→ SolverContext.java (iteration loop)
→ Convergence criteria
→ How to handle failure

And Friday: Preview of the weekend article comparing solvers (Backward Euler, Trapezoidal, Gear-Shichman) for nonlinear circuits.

Linear components were the foundation. Nonlinear components are where the real simulation challenges begin.

---

**Hashtags:**
#NonlinearCircuits #NewtonRaphson #DiodeModeling #CircuitSimulation #SPICE

**CTA:** Follow for Wednesday Newton-Raphson code

**Source Reference:** DiodeStamper.java:60-120

**Metrics:**
- Word count: 441
- Reading time: 2.3 min
- Diode equation: I = Is·(e^(V/Vt) - 1)
- Tease: Iterative solution via linearization
