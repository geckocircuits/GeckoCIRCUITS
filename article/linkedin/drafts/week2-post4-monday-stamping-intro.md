# LinkedIn Post #4 (Monday) - Week 2

**Publishing Date:** Week 2, Monday 8am
**Topic:** What Makes Capacitors Different (Time Dependency)
**Target:** Engineers who understand resistors, curious about dynamic components
**Goal:** Tease time-dependent stamping, build curiosity
**Word Count:** 419 words

---

## Post Content

Resistors are algebraic. Capacitors are differential equations. This changes everything about how they stamp into the MNA matrix.

Last week we covered Modified Nodal Analysis (MNA) and resistor stamping. Clean, simple, elegant:

```java
// Resistor stamping (time-independent)
double G = 1.0 / R;
a[nodeX][nodeX] += G;
a[nodeY][nodeY] += G;
a[nodeX][nodeY] -= G;
a[nodeY][nodeX] -= G;
```

Four entries. Symmetric. Constant. Beautiful.

Now here's a capacitor:

**I = C · dV/dt**

That's a derivative. The current depends on the **rate of change** of voltage, not the voltage itself.

You can't directly stamp a derivative into an algebraic matrix.

So what do we do?

**Time Integration**

Circuit simulators use numerical integration to convert differential equations to algebraic equations:

- Backward Euler: First-order accurate, stable
- Trapezoidal: Second-order accurate, can oscillate
- Gear-Shichman: Variable order, adaptive

Each method produces a different stamping pattern!

**Backward Euler Example:**

Using Backward Euler: dV/dt ≈ (V_new - V_old) / dt

Substitute into capacitor equation:
I = C · (V_new - V_old) / dt

Rearrange:
I = (C/dt) · V_new - (C/dt) · V_old

Now we have two parts:
1. (C/dt) · V_new → stamps into matrix A (like a resistor with G = C/dt)
2. (C/dt) · V_old → stamps into vector b (like a current source)

The stamping pattern **changes with time step dt**. And it **depends on history** (V_old).

Resistors don't care about history. Capacitors do.

**Why This Matters:**

- Different time steps → different matrix entries
- Adaptive time stepping → rebuild matrix every step
- Backward Euler vs Trapezoidal → different numerical behavior
- Stiff systems (fast + slow dynamics) → solver choice matters

Wednesday I'll show you the actual GeckoCIRCUITS code:
→ CapacitorStamper.java implementation
→ How admittance = C/dt is computed
→ How previousValues are used
→ Why dt is a parameter to every stamp method

This is where circuit simulation gets interesting. Resistors were the warm-up. Capacitors are where the real engineering happens.

Want to understand how time-dependent components work in SPICE, PLECS, GeckoCIRCUITS? Follow along this week.

Weekend article: Complete deep-dive on stamping for R, L, C components with worked examples.

---

**Hashtags:**
#CircuitSimulation #TimeIntegration #CapacitorStamping #NumericalMethods #PowerElectronics

**CTA:** Follow for Wednesday code preview

**Source Reference:** CapacitorStamper.java:45-80

**Metrics:**
- Word count: 419
- Reading time: 2.2 min
- Code snippet: 1 (resistor stamping for contrast)
- Tease: Capacitor stamping depends on time step and history
