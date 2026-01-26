# Week 2 Article Outline: Matrix Stamping Deep-Dive: R, L, C Components

**Target:** Power electronics engineers and students learning circuit simulation
**Word Count:** 2,800-3,200 words
**Reading Time:** 14-16 minutes
**Prerequisites:** Week 1 knowledge (MNA fundamentals, resistor stamping)

---

## Structure:

### 1. Executive Summary (250w)

**Hook:**
Resistors are algebraic. Capacitors and inductors are differential equations. This changes everything about how they stamp into the MNA matrix.

**What You'll Learn:**
- Why time-dependent components require numerical integration
- Backward Euler vs Trapezoidal integration methods
- Complete code walkthroughs: CapacitorStamper.java and InductorStamper.java
- How dt (time step) affects matrix entries
- When to use which integration method

**Prerequisites:**
- Week 1 MNA knowledge
- Basic calculus (derivatives, integration)
- Understanding of differential equations (helpful)

**Time Investment:** 15 min reading + 45 min exercises

---

### 2. The Problem: Differential Equations in MNA (350w)

**Why Time Matters:**
- Resistors: I = V/R (instantaneous, algebraic)
- Capacitors: I = C·dV/dt (depends on rate of change)
- Inductors: V = L·dI/dt (depends on rate of change)

**Can't Directly Stamp Derivatives:**
- MNA requires algebraic equations (A·x = b)
- Derivatives are operators, not values
- Need to discretize time

**Common Mistakes:**
- Treating capacitors like resistors with G = C
- Ignoring history (previous time step values)
- Using wrong integration method for problem type

**Solution Preview:**
Time integration methods convert differential equations to algebraic approximations.

---

### 3. Time Integration Theory (800w)

**3.1 Backward Euler (First-Order)**

Approximation: dV/dt ≈ (V_new - V_old) / dt

For capacitor I = C·dV/dt:
- I = C·(V_new - V_old) / dt
- Rearrange: I = (C/dt)·V_new - (C/dt)·V_old
- Two parts:
  - (C/dt)·V_new → stamps into matrix A (like resistor with G = C/dt)
  - (C/dt)·V_old → stamps into vector b (like current source)

Properties:
- First-order accurate: error ~ O(dt)
- A-stable (unconditionally stable)
- Numerically dissipative (damps oscillations)

**3.2 Trapezoidal Rule (Second-Order)**

Approximation: dV/dt ≈ (V_new - V_old) / dt, evaluated at midpoint

For capacitor:
- I_avg = (I_new + I_old) / 2
- I_avg = C·(V_new - V_old) / dt
- More complex history term (involves both V_old and I_old)

Properties:
- Second-order accurate: error ~ O(dt²)
- A-stable
- Can oscillate (not dissipative)

**3.3 Comparison Table**

| Method | Order | Stability | Dissipation | Use Case |
|--------|-------|-----------|-------------|----------|
| Backward Euler | 1st | Stable | Yes | Stiff systems, switches |
| Trapezoidal | 2nd | Stable | No | Accurate transients |
| Gear-Shichman | Variable | Stable | Adaptive | General purpose |

**When to Use Which:**
- Backward Euler: Switching converters, stiff systems, large dt
- Trapezoidal: Oscillatory systems, need accuracy, small dt
- Gear-Shichman: Adaptive time stepping, mixed dynamics

---

### 4. Implementation in GeckoCIRCUITS (1,000w)

**4.1 CapacitorStamper (Backward Euler)**

Complete code walkthrough:

```java
public class CapacitorStamper implements IStatefulStamper {
    // stampMatrixA: G_eq = C/dt
    // stampVectorB: I_history = (C/dt)*V_old
    // calculateCurrent: I = C*(V_new - V_old)/dt
}
```

Line-by-line explanation:
- Why admittance = C/dt
- How previousValues array is used
- Why dt is a parameter
- Difference from resistor

**4.2 InductorStamper (Backward Euler)**

Dual of capacitor:

Inductor equation: V = L·dI/dt
- Rearrange: dI/dt = V/L
- Discretize: (I_new - I_old)/dt = V/L
- I_new = I_old + (dt/L)*V

Stamps like resistor with G = dt/L, but adds branch current to x vector.

Complete code walkthrough with augmented matrix structure.

**4.3 IStatefulStamper Interface**

```java
public interface IStatefulStamper extends IMatrixStamper {
    void updateState(double[] currentValues);
    double[] getPreviousState();
}
```

Why components need memory between time steps.

**4.4 SolverType Enumeration**

```java
public enum SolverType {
    BACKWARD_EULER,
    TRAPEZOIDAL,
    GEAR_SHICHMAN_2,
    GEAR_SHICHMAN_4
}
```

How solver choice affects stamping.

---

### 5. Worked Examples (450w)

**Example 1: RC Circuit Step Response**

Circuit:
```
Vin=10V (step) ----[R=1kΩ]---- node1 ----[C=10µF]---- GND
```

Hand calculation:
- Analytical solution: V1(t) = 10·(1 - e^(-t/RC))
- Time constant: τ = RC = 10ms

Simulation with Backward Euler:
- dt = 1ms: Error ~5% at τ
- dt = 0.1ms: Error <1% at τ

Simulation with Trapezoidal:
- dt = 1ms: Error ~1% at τ
- dt = 0.1ms: Error <0.1% at τ

**Example 2: LC Oscillator**

Circuit:
```
Vin=10V (initial) ----[L=1mH]---- node1 ----[C=10µF]---- GND
```

Natural frequency: ω = 1/√(LC) = 10 krad/s, f = 1.59 kHz

Backward Euler:
- Damps oscillation (numerical dissipation)
- Amplitude decreases over time (non-physical)

Trapezoidal:
- Preserves amplitude (energy conserving)
- Slight frequency error with large dt

**Example 3: RLC Damped Response**

Critical damping condition: R = 2√(L/C)

Compare solver behavior at critical damping, overdamping, underdamping.

---

### 6. Advanced Topics (250w)

**Variable Time Stepping:**
- Why adaptive dt is needed
- How it affects matrix rebuilding
- Computational cost

**Higher-Order Methods:**
- Gear-Shichman 2, 4
- Variable order adaptive
- When the complexity is worth it

**Implicit vs Explicit:**
- Backward Euler: implicit (A matrix depends on V_new)
- Forward Euler: explicit (avoided in SPICE - unstable!)

**Preview Week 3:**
Sparse matrices and Cholesky make large RLC circuits fast.

---

### 7. Exercises (300w)

**Exercise 1: RC Charging Circuit**

Circuit: Vin=5V, R=2kΩ, C=100µF
- Hand: Calculate time constant τ
- Simulate: Compare Backward Euler vs Trapezoidal
- Analyze: Plot error vs analytical solution

**Exercise 2: LC Tank Circuit**

Circuit: L=100µH, C=100nF, initial V_C = 10V
- Hand: Calculate resonant frequency
- Simulate: Backward Euler with dt=1µs (observe damping)
- Simulate: Trapezoidal with dt=1µs (energy conservation)

**Exercise 3: RLC Filter Design**

Design low-pass filter: f_cutoff = 1kHz, damping ζ = 0.707
- Calculate R, L, C values
- Simulate step response
- Verify no overshoot (critically damped)

**Solutions:** Provided at end of article with code snippets.

---

### 8. What's Next (150w)

**Next Week (Week 3):** Sparse Matrices & Cholesky
- Why 1000-node circuits don't use 8MB of RAM
- Cholesky decomposition for symmetric matrices
- 200x performance improvement

**Series Roadmap:**
- Week 3: Performance (sparse matrices)
- Week 4: Nonlinear (Newton-Raphson)
- Week 5: Complete architecture

---

### 9. Further Reading (100w)

**Books:**
- Gear, "Numerical Initial Value Problems in Ordinary Differential Equations"
- Hairer & Wanner, "Solving Ordinary Differential Equations II: Stiff and Differential-Algebraic Problems"

**Papers:**
- Gear, "The Automatic Integration of ODEs" (1971)

---

### 10. CTA (100w)

- Subscribe for Week 3
- Follow on LinkedIn (3x/week)
- Try exercises
- Share with colleagues
- Comment with questions

---

## Source Files:

**Primary:**
- `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/CapacitorStamper.java`
- `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/InductorStamper.java`
- `src/main/java/ch/technokrat/gecko/geckocircuits/circuit/matrix/IStatefulStamper.java`

**Secondary:**
- `src/main/java/ch/technokrat/gecko/geckocircuits/allg/SolverType.java`
- `src/main/java/ch/technokrat/gecko/geckocircuits/math/BackwardEuler.java`
- `src/main/java/ch/technokrat/gecko/geckocircuits/math/TrapezoidalIntegrator.java`

---

## Code Examples Needed:

1. CapacitorStamper.java complete implementation (stampMatrixA, stampVectorB, calculateCurrent)
2. InductorStamper.java complete implementation
3. IStatefulStamper interface definition
4. SolverType enum
5. Simulation loop with time stepping
6. RC circuit example with both solvers
7. Energy calculation for LC circuit

---

## Diagrams/ASCII Art:

1. RC circuit schematic
2. LC tank circuit schematic
3. Backward Euler vs Trapezoidal step response comparison (table/ASCII plot)
4. Time discretization illustration (continuous vs discrete)
5. Augmented matrix structure for inductor (showing branch current row/column)

---

## SEO Keywords:

**Primary:**
- Time integration circuit simulation
- Backward Euler method
- Trapezoidal rule circuit analysis
- Capacitor stamping MNA
- Transient analysis SPICE

**Secondary:**
- Numerical integration electrical circuits
- Differential equations circuit simulation
- GeckoCIRCUITS time stepping
- SPICE integration methods
