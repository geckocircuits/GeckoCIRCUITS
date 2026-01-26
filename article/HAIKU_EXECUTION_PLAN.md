# Haiku Execution Plan: Complete GeckoCIRCUITS Content Series

**Purpose:** Token-efficient content generation for Weeks 1-5
**Model:** Claude Haiku (fast, low-cost)
**Status:** Ready to execute

---

## What's Already Done ✅

1. ✅ Content calendar structure (article/CONTENT_CALENDAR.md)
2. ✅ Week 1 LinkedIn posts #1-3 (article/linkedin/drafts/)
3. ✅ Directory structure
4. ✅ Skills files (content-strategist, linkedin-writer, substack-article-writer)
5. ✅ Tools (metrics_extractor.py, template_renderer.py)

## What Needs to be Created 📝

### Task 1: Week 1 Substack Article (Full Text)
**File:** `article/substack/drafts/week1-article-mna-complete.md`
**Length:** 2,800-3,200 words
**Use:** Template from CONTENT_CALENDAR.md lines 350-1107

### Task 2: Weeks 2-5 LinkedIn Posts
**Files:** `article/linkedin/drafts/week{N}-post{N}-{day}-{topic}.md`
**Count:** 12 posts (3 per week × 4 weeks)
**Pattern:** Monday (teaser) → Wednesday (code) → Friday (hype)

### Task 3: Weeks 2-5 Substack Article Outlines
**Files:** `article/substack/drafts/week{N}-article-{topic}-outline.md`
**Count:** 4 outlines
**Detail:** Section structure, word counts, code examples needed

---

## Task 1: Complete Week 1 Substack Article

### Instructions for Haiku:

**Input:** Read `article/CONTENT_CALENDAR.md` lines 350-1107 (article structure)

**Output:** Create full article with these sections:

1. **Executive Summary (250w)**
   - Hook: Every simulator uses MNA
   - What reader learns: theory + code + examples
   - Prerequisites: circuit theory, linear algebra, programming
   - Time: 15 min read + 30 min exercises

2. **The Problem (400w)**
   - Hand analysis doesn't scale
   - Mesh analysis: too many unknowns
   - Nodal analysis: can't handle voltage sources
   - MNA: the solution

3. **MNA Theory (900w)**
   - Core equation: A·x = b
   - Step-by-step derivation
   - Example: 3-resistor circuit
   - Stamping pattern explained
   - Why "modified"? (voltage sources, inductors)

4. **Implementation in GeckoCIRCUITS (1,100w)**
   - IMatrixStamper interface (full code with comments)
   - ResistorStamper implementation (full code)
   - Simulation engine usage pattern
   - Why this design works (Strategy pattern)

5. **Worked Example (500w)**
   - Circuit: Vin=10V --[R1=10Ω]-- node1 --[R2=20Ω]-- GND, [R3=5Ω] from node1 to GND
   - Hand calculation step-by-step
   - Matrix formation
   - Solution: V1 = 2.857V
   - Current calculations
   - Verify KCL

6. **Advanced Topics Preview (300w)**
   - Capacitors/inductors (time integration)
   - Nonlinear components (Newton-Raphson)
   - Switching (topology changes)
   - Sparse matrices (efficiency)

7. **Exercises (300w)**
   - Exercise 1: Voltage divider (Vin=12V, R1=1kΩ, R2=2kΩ)
   - Exercise 2: Bridge circuit (4 resistors)
   - Exercise 3: RC circuit (challenge for next week)
   - Solutions provided

8. **What's Next (200w)**
   - Preview Week 2: Time-dependent stamping
   - 5-week roadmap
   - Building toward complete understanding

9. **Further Reading (150w)**
   - Books: Vladimirescu, SPICE Book
   - Papers: Hachtel, Ho
   - Source code: GeckoCIRCUITS, ngspice

10. **CTA (100w)**
    - Subscribe for weekly deep-dives
    - Follow on LinkedIn (3x/week)
    - Try exercises
    - Share with colleagues

**Formatting:**
- Use markdown headers (##, ###)
- Code blocks with ```java
- Math equations in plain text with spacing
- Circuit diagrams in ASCII art
- Bold for emphasis
- Bullets for lists

**Save to:** `article/substack/drafts/week1-article-mna-complete.md`

---

## Task 2: Weeks 2-5 LinkedIn Posts (12 posts)

### Pattern for All Posts:

**Monday (Teaser):**
- Word count: 400-450
- Structure: Hook → Problem → Teaser → CTA
- Questions: 2-3 to build curiosity
- CTA: "Wednesday I'll show you...", "Weekend deep-dive coming"

**Wednesday (Code Preview):**
- Word count: 450-500
- Structure: Hook → Code snippet → Explanation → Teaser → CTA
- Code: 10-20 lines from actual GeckoCIRCUITS files
- CTA: "Subscribe for weekend article"

**Friday (Hype):**
- Word count: 350-400
- Structure: Recap → Article preview → Value props → Urgency → CTA
- Use emojis: 📐💻🔧✏️
- CTA: "Article drops tomorrow. Subscribe now!"

### Week 2: Matrix Stamping & Time Integration

**LinkedIn #4 (Monday):**
- Topic: "What does 'stamping' even mean?"
- Hook: Resistors are algebraic, capacitors are differential equations
- Tease: Capacitor stamping changes with time step
- Source: CapacitorStamper.java

**LinkedIn #5 (Wednesday):**
- Topic: "Capacitors vs resistors: the time dependency"
- Code: CapacitorStamper.java stampMatrixA method
- Show: admittance = C/dt (time-step dependent!)
- Also: stampVectorB uses previousValues
- Source: CapacitorStamper.java:50-75 (approx)

**LinkedIn #6 (Friday):**
- Topic: "This weekend: Build your own stamper"
- Article preview: "Matrix Stamping Deep-Dive: R, L, C Components"
- Sections: Capacitor theory, Inductor theory, Time integration, Exercises
- CTA: Subscribe, article drops Saturday

**Article #2 (Weekend):** "Matrix Stamping Deep-Dive: R, L, C Components"

---

### Week 3: Solving Systems (Cholesky & Sparse Matrices)

**LinkedIn #7 (Monday):**
- Topic: "1000-node circuit = 1 million matrix entries. Or does it?"
- Hook: Sparse matrices save 100x memory
- Tease: Most entries are zero
- Source: SymmetricSparseMatrix.java

**LinkedIn #8 (Wednesday):**
- Topic: "Cholesky decomposition in 20 lines"
- Code: CholeskyDecomposition.java core algorithm
- Show: L·L^T factorization
- Why: Faster than LU, more stable
- Source: CholeskyDecomposition.java:50-90 (approx)

**LinkedIn #9 (Friday):**
- Topic: "Weekend: Why your circuits don't explode memory"
- Article preview: "Sparse Matrices & Cholesky: The Speed Behind SPICE"
- Sections: Sparsity patterns, Storage formats, Cholesky algorithm, Performance
- CTA: Subscribe now

**Article #3 (Weekend):** "Sparse Matrices & Cholesky: The Speed Behind SPICE"

---

### Week 4: Nonlinear Components & Solvers

**LinkedIn #10 (Monday):**
- Topic: "Linear components are easy. Diodes are not."
- Hook: Diode I-V curve is exponential
- Tease: Can't directly stamp nonlinear equations
- Solution: Newton-Raphson iteration

**LinkedIn #11 (Wednesday):**
- Topic: "Newton-Raphson: converging to the solution"
- Code: DiodeStamper.java (Newton-Raphson iteration loop)
- Show: Linearize, stamp, solve, check convergence, repeat
- Source: DiodeStamper.java or SolverContext.java

**LinkedIn #12 (Friday):**
- Topic: "Weekend: Three solvers, when to use each"
- Article preview: "Nonlinear Simulation: Newton-Raphson & Solver Comparison"
- Sections: Diode modeling, Newton-Raphson, BE vs TRZ vs GS solvers
- CTA: Subscribe for solver guide

**Article #4 (Weekend):** "Nonlinear Simulation: Newton-Raphson & Solver Comparison"

---

### Week 5: Complete System Architecture

**LinkedIn #13 (Monday):**
- Topic: "From file to results: the simulation pipeline"
- Hook: .ipes file → parser → simulation → scope
- Tease: 10-step simulation loop
- Source: SimulationsKern.java, IpesFileable.java

**LinkedIn #14 (Wednesday):**
- Topic: "Signal processing: FFT, THD, CISPR16"
- Code: Cispr16Fft.java or THD calculation
- Show: Real-time frequency analysis
- Applications: EMC testing, power quality
- Source: Cispr16Fft.java

**LinkedIn #15 (Friday):**
- Topic: "Series finale: What we've learned + what's next"
- Recap: 5 weeks from MNA to complete system
- Poll: What topic next? (AC analysis, optimization, etc.)
- Thank you + CTA: Stay subscribed

**Article #5 (Weekend):** "GeckoCIRCUITS Architecture: A Complete Tour"

---

## Task 3: Create Article Outlines (Weeks 2-5)

For each article, create detailed outline with:

### Outline Template:

```markdown
# Week {N} Article Outline: {Title}

**Target:** {Audience}
**Word Count:** 2,500-3,000
**Reading Time:** 12-15 minutes

## Structure:

### 1. Executive Summary (200w)
- Hook: [1-2 sentences]
- What you'll learn: [bullet points]
- Prerequisites: [list]
- Time investment: [X min]

### 2. The Problem (300w)
- Why this matters
- Common challenges
- What existing approaches miss

### 3. Theory (800w)
[Main topic - equations, concepts, math]
- Subsection 1
- Subsection 2
- Subsection 3

### 4. Implementation in GeckoCIRCUITS (900w)
- Code walkthrough: [File.java]
- Key methods: [list]
- Code snippets: [count]
- Design patterns: [which]

### 5. Worked Example (400w)
- Circuit description
- Step-by-step solution
- Results
- Verification

### 6. Advanced Topics (200w)
- Extension 1
- Extension 2
- Preview next week

### 7. Exercises (300w)
- Exercise 1: [description]
- Exercise 2: [description]
- Solutions: [brief]

### 8. What's Next (100w)
- Next week preview
- Series progress

### 9. CTA (100w)
- Subscribe
- Follow LinkedIn
- Share

## Source Files:
- [File1.java]
- [File2.java]

## Code Examples Needed:
1. [Description]
2. [Description]

## Diagrams/ASCII Art:
1. [Description]
```

---

## Execution Steps for Haiku

### Step 1: Generate Week 1 Substack Article
```bash
# Command:
claude --model haiku "Read article/CONTENT_CALENDAR.md lines 350-1107.
Write complete Substack article following that structure.
Save to article/substack/drafts/week1-article-mna-complete.md"
```

### Step 2: Generate Week 2 LinkedIn Posts
```bash
# Command:
claude --model haiku "Read article/CONTENT_CALENDAR.md Week 2 section.
Create 3 LinkedIn posts (Monday/Wednesday/Friday) following the pattern.
Use CapacitorStamper.java as code source.
Save to article/linkedin/drafts/week2-post{N}-{day}-{topic}.md"
```

### Step 3: Generate Week 2 Article Outline
```bash
# Command:
claude --model haiku "Create detailed outline for Week 2 Substack article:
'Matrix Stamping Deep-Dive: R, L, C Components'
Include sections, word counts, code examples needed.
Save to article/substack/drafts/week2-article-outline.md"
```

### Step 4: Repeat for Weeks 3-5
(Same pattern as Steps 2-3)

---

## File Naming Convention

**LinkedIn Posts:**
- Format: `week{N}-post{N}-{day}-{topic-slug}.md`
- Examples:
  - `week2-post4-monday-stamping-intro.md`
  - `week2-post5-wednesday-capacitor-code.md`
  - `week2-post6-friday-article-hype.md`

**Substack Articles:**
- Outlines: `week{N}-article-{topic-slug}-outline.md`
- Full text: `week{N}-article-{topic-slug}-complete.md`
- Examples:
  - `week2-article-matrix-stamping-outline.md`
  - `week2-article-matrix-stamping-complete.md`

---

## Quality Checks

For each piece of content, verify:

**LinkedIn Posts:**
- [ ] Word count: 350-500 words
- [ ] Hook in first sentence
- [ ] Code snippet (if Wednesday)
- [ ] CTA at end
- [ ] 5-7 hashtags
- [ ] File references included
- [ ] Builds hype for weekend article

**Substack Articles:**
- [ ] Word count: 2,500-3,000 words
- [ ] All 10 sections present
- [ ] Code examples: 5-8 snippets
- [ ] Exercises: 3 circuits with solutions
- [ ] SEO metadata included
- [ ] Reading time: 12-15 min
- [ ] CTA: Subscribe + follow

**Outlines:**
- [ ] Section structure clear
- [ ] Word counts specified
- [ ] Source files listed
- [ ] Code examples identified
- [ ] Diagrams described

---

## Source File Quick Reference

**Week 1 (MNA):**
- IMatrixStamper.java
- ResistorStamper.java

**Week 2 (Time Integration):**
- CapacitorStamper.java
- InductorStamper.java
- IStatefulStamper.java
- SolverType.java

**Week 3 (Solving):**
- CholeskyDecomposition.java
- SymmetricSparseMatrix.java
- Matrix.java
- GeckoMatrix.java

**Week 4 (Nonlinear):**
- DiodeStamper.java
- SolverContext.java
- SolverSettings.java
- SolverType.java

**Week 5 (Architecture):**
- SimulationsKern.java
- IpesFileable.java
- Cispr16Fft.java
- StamperRegistry.java

---

## Token Efficiency Tips for Haiku

1. **Use templates:** Reference CONTENT_CALENDAR.md structure
2. **Batch operations:** Generate all Week N posts in one prompt
3. **Reuse patterns:** Week 2-5 follow same Monday/Wednesday/Friday structure
4. **Focus on differences:** Only specify what changes between weeks
5. **Short prompts:** "Create Week 2 Monday post. Topic: Capacitor stamping. Follow Week 1 Monday pattern."

---

## Success Criteria

**Complete when:**
- ✅ 1 full Substack article (Week 1)
- ✅ 12 LinkedIn posts (Weeks 2-5, 3 each)
- ✅ 4 Substack article outlines (Weeks 2-5)

**Total content:**
- LinkedIn: ~5,500 words (12 posts × ~450 words)
- Substack article: ~3,000 words (Week 1 full text)
- Outlines: ~2,000 words (4 outlines × ~500 words)
- **Grand total: ~10,500 words**

**Estimated Haiku cost:** <$1 USD (vs. $5-10 with Sonnet)

---

## Next Steps

1. Copy this plan to execution environment
2. Run Haiku with prompts from "Execution Steps"
3. Review generated content for quality
4. Make minor edits if needed
5. Move to `published/` when ready to post

---

**Status:** Ready for Haiku execution
**Created:** 2026-01-26
**Model:** Claude Sonnet 4.5 (plan creation)
**Execution Model:** Claude Haiku (content generation)
