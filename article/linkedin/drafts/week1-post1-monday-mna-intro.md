# LinkedIn Post #1 (Monday) - Week 1

**Publishing Date:** Week 1, Monday 8am
**Topic:** MNA Introduction & Teaser
**Target:** Engineers curious about circuit simulation
**Goal:** Hook attention, introduce MNA, build anticipation
**Word Count:** 427 words

---

## Post Content

Ever wonder how SPICE simulates a 1000-component circuit in seconds? It all starts with one elegant equation: A·x = b

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

Why "modified"? Classical nodal analysis can't handle voltage sources and inductors. MNA adds branch currents to fix this.

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

---

**Hashtags:**
#CircuitSimulation #MNA #PowerElectronics #SPICE #EngineeringEducation

**CTA:** Follow + Subscribe to Substack

**Source File Reference:** IMatrixStamper.java:23-34

**Metrics:**
- Word count: 427
- Reading time: 2 min
- Code snippets: 1 (matrix example)
- Questions raised: 3
