# LinkedIn Post #12 (Friday) - Week 4

**Publishing Date:** Week 4, Friday 8am
**Topic:** Weekend Article Hype - Nonlinear Solvers
**Target:** Everyone following the series
**Goal:** Create urgency, drive subscriptions, preview solver comparison
**Word Count:** 375 words

---

## Post Content

This weekend: Three solvers, when to use each. The complete guide to nonlinear circuit simulation and solver trade-offs.

**This Week:**

Monday: Why diodes break linear MNA (exponential I-V curve)
Wednesday: Newton-Raphson iteration code (linearize → solve → repeat)

Tomorrow: The full nonlinear solver deep-dive.

**What's Inside "Nonlinear Simulation: Newton-Raphson & Solver Comparison"**

🔬 **Diode Modeling (25%)**
→ I-V characteristic: I = Is·(e^(V/Vt) - 1)
→ Linearization: G_eq = dI/dV
→ History current correction
→ DiodeStamper.java walkthrough

🔄 **Newton-Raphson Iteration (30%)**
→ Why iteration is necessary
→ Complete algorithm derivation
→ Convergence criteria (absolute, relative, residual)
→ Damping for stability
→ SolverContext.java implementation

⚙️ **Solver Comparison (35%)**
→ Backward Euler: Stable, first-order accurate, dissipative
→ Trapezoidal: Second-order accurate, can oscillate
→ Gear-Shichman: Variable order, adaptive, best for stiff systems
→ When to use which (with examples)

✏️ **Worked Examples (10%)**
→ Diode rectifier circuit
→ BJT amplifier (Common Emitter)
→ Solver comparison on same circuit
→ Convergence debugging

Reading time: 17 minutes
Code examples: 6 snippets
Solver comparisons: 3 case studies
Difficulty: Advanced (requires Weeks 1-3 knowledge)

**Why This Matters:**

✓ Understand why "SPICE failed to converge" happens
✓ Learn to choose the right solver for your circuit
✓ Debug nonlinear simulation issues
✓ Implement custom nonlinear component models

**Who's This For?**

→ Power electronics engineers debugging convergence
→ Researchers modeling nonlinear components
→ Students learning numerical circuit simulation
→ Anyone frustrated by SPICE convergence failures

**The Article Drops Tomorrow**

Subscribe now:
[Substack link]

**Next Week (Final Week 5):**

The complete system. From .ipes file to waveforms on scope.

Monday: The simulation pipeline (parser → stampers → solver → scope)
Wednesday: Signal processing (FFT, THD, CISPR16)
Friday: Series wrap-up + poll on what to cover next

This is the finale. 5 weeks from MNA foundations to complete understanding.

Don't miss it. Subscribe now.

See you tomorrow with the nonlinear solver guide!

---

**Hashtags:**
#NonlinearSimulation #SolverComparison #NewtonRaphson #CircuitSimulation #SPICE

**CTA:** Subscribe NOW for solver comparison

**Metrics:**
- Word count: 375
- Reading time: 2 min
- Article preview: Three solvers compared
- FOMO: Tomorrow, finale next week, subscribe now
