# LinkedIn Post #6 (Friday) - Week 2

**Publishing Date:** Week 2, Friday 8am
**Topic:** Weekend Article Hype - Matrix Stamping Deep-Dive
**Target:** Everyone following the series
**Goal:** Create urgency, drive Substack subscriptions, preview content
**Word Count:** 381 words

---

## Post Content

This weekend: The complete guide to stamping time-dependent components. Resistors, capacitors, inductors. Theory + code + examples.

**What We Covered This Week:**

Monday: Why capacitors are different from resistors (differential equations vs algebra)
Wednesday: Actual CapacitorStamper.java code (admittance = C/dt, history terms)

Tomorrow: The full deep-dive drops.

**What's Inside "Matrix Stamping Deep-Dive: R, L, C Components"**

📐 **Theory (35%)**
→ Why I = C·dV/dt can't be directly stamped
→ Time integration methods: Backward Euler, Trapezoidal, Gear-Shichman
→ Numerical stability and accuracy trade-offs
→ When to use which method

💻 **Code Walkthroughs (40%)**
→ ResistorStamper.java (review from Week 1)
→ CapacitorStamper.java (time-dependent, history terms)
→ InductorStamper.java (dual of capacitor)
→ IStatefulStamper interface (components with memory)

🔧 **Worked Examples (20%)**
→ RC circuit step response (hand calculation + code)
→ LC oscillator (energy conservation check)
→ RLC damped response (critically damped vs overdamped)
→ Step-by-step with Backward Euler and Trapezoidal

✏️ **Exercises (5%)**
→ 3 transient circuits for you to solve
→ Compare Backward Euler vs Trapezoidal results
→ Solutions provided

Reading time: 18 minutes
Code examples: 7 snippets
Exercises: 3 circuits with solutions
Difficulty: Intermediate (requires Week 1 knowledge)

**Why Read This?**

✓ Understand why adaptive time stepping requires matrix rebuilds
✓ Learn when Backward Euler fails (and why Trapezoidal helps)
✓ Implement your own time-dependent stampers
✓ Debug transient simulation issues (oscillations, instability)

**Who's This For?**

→ Power electronics engineers using PLECS/SPICE
→ Students learning circuit simulation algorithms
→ Researchers implementing custom dynamic components
→ Anyone who wondered "Why does my RC circuit simulation oscillate?"

**The Article Drops Tomorrow Morning**

Not subscribed yet? Do it now (it's free):
[Substack link]

**Next Week Preview:**

Week 3 is about efficiency. How do you solve a 1000×1000 matrix fast?
- Sparse matrix storage (100x memory savings)
- Cholesky decomposition (faster than LU)
- SymmetricSparseMatrix.java code walkthrough

Don't miss it. Subscribe now.

See you tomorrow with the complete matrix stamping guide.

---

**Hashtags:**
#CircuitSimulation #MatrixStamping #TimeIntegration #LearningByDoing #GeckoCIRCUITS

**CTA:** Subscribe to Substack NOW before article drops

**Metrics:**
- Word count: 381
- Reading time: 2 min
- Article preview: Complete breakdown with percentages
- FOMO elements: 3 (tomorrow, not subscribed?, free)
