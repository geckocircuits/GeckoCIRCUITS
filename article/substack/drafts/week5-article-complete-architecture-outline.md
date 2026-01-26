# Week 5 Article Outline: GeckoCIRCUITS Architecture: A Complete Tour

**Target:** Engineers and developers wanting system-level understanding
**Word Count:** 2,800-3,200 words
**Reading Time:** 14-16 minutes
**Prerequisites:** Weeks 1-4 (all concepts), software engineering (design patterns helpful)

---

## Structure:

### 1. Executive Summary (250w)

**Hook:**
5 weeks, 5 articles. We've gone from A·x = b to complete circuit simulation. Now let's see how it all fits together: from .ipes file to waveforms on your scope, with REST APIs, WebSockets, and signal processing along the way.

**What You'll Learn:**
- Complete simulation pipeline (10 steps from file to scope)
- Key interfaces and their roles
- Design patterns used (Strategy, Factory, Observer)
- Signal processing: FFT, THD, CISPR16
- REST API integration (from Phase 1)
- WebSocket real-time updates
- What makes GeckoCIRCUITS different from SPICE/PLECS

**Prerequisites:**
- Weeks 1-4 content
- Basic software engineering concepts
- Java experience (helpful)

**Time Investment:** 15 min reading + 30 min exploring source code

---

### 2. The Big Picture (300w)

**What We've Learned:**

- **Week 1:** MNA foundations, matrix stamping, IMatrixStamper interface
- **Week 2:** Time integration, Backward Euler, Trapezoidal
- **Week 3:** Sparse matrices, Cholesky decomposition, performance
- **Week 4:** Nonlinear components, Newton-Raphson, solver comparison

**What's Missing:**

How does a circuit description become simulation results?

**The 10-Step Pipeline:**

1. **File Loading:** Read .ipes circuit file (XML)
2. **Parsing:** Convert XML → Java objects
3. **Component Registration:** Map types → stamper implementations
4. **Topology Analysis:** Assign node numbers, detect ground
5. **Memory Allocation:** Size matrices for N nodes + M branches
6. **Time Loop Initialization:** t = 0, set initial conditions
7. **Matrix Assembly:** Components stamp into A and b
8. **System Solve:** Cholesky (or LU) solves A·x = b (with Newton-Raphson if nonlinear)
9. **Current Calculation:** Components compute currents from voltages
10. **Scope Update:** Write to waveform buffers, update GUI/REST API

This article walks through each step with actual GeckoCIRCUITS code.

---

### 3. File Format & Parsing (500w)

**3.1 IPES File Format**

GeckoCIRCUITS uses .ipes files (XML-based):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ipes>
  <circuit>
    <component type="RESISTOR" id="R1">
      <nodes>
        <node>1</node>
        <node>0</node>
      </nodes>
      <parameters>
        <resistance>1000.0</resistance>
      </parameters>
    </component>
    <component type="CAPACITOR" id="C1">
      <nodes>
        <node>1</node>
        <node>0</node>
      </nodes>
      <parameters>
        <capacitance>1e-6</capacitance>
      </parameters>
    </component>
  </circuit>
  <simulation>
    <solver>BACKWARD_EULER</solver>
    <timestep>1e-6</timestep>
    <duration>0.1</duration>
  </simulation>
</ipes>
```

**3.2 IpesFileable Interface**

```java
public interface IpesFileable {
    /**
     * Export component to IPES XML format
     */
    Element exportToXML(Document doc);

    /**
     * Import component from IPES XML element
     */
    void importFromXML(Element element);

    /**
     * Get component type identifier
     */
    String getTypeIdentifier();
}
```

Every component implements this for serialization.

**3.3 Parsing Pipeline**

```java
// From: IpesFileParser.java
public class IpesFileParser {
    public Circuit parseFile(String filePath) throws IOException {
        // 1. Load XML
        Document doc = loadXML(filePath);

        // 2. Parse components
        List<Component> components = new ArrayList<>();
        NodeList compNodes = doc.getElementsByTagName("component");
        for (int i = 0; i < compNodes.getLength(); i++) {
            Element elem = (Element) compNodes.item(i);
            String type = elem.getAttribute("type");

            // Factory pattern: create component by type
            Component comp = ComponentFactory.create(type);
            comp.importFromXML(elem);
            components.add(comp);
        }

        // 3. Parse simulation settings
        SolverSettings settings = parseSimulationSettings(doc);

        // 4. Build circuit
        return new Circuit(components, settings);
    }
}
```

**Key Design Patterns:**
- Factory: ComponentFactory creates instances by type string
- Strategy: Each component knows how to parse itself (IpesFileable)

---

### 4. Component Registration & Stamper Assignment (400w)

**4.1 StamperRegistry Class**

```java
public class StamperRegistry {
    private static Map<String, Class<? extends IMatrixStamper>> registry
        = new HashMap<>();

    static {
        // Register stampers for each component type
        register("RESISTOR", ResistorStamper.class);
        register("CAPACITOR", CapacitorStamper.class);
        register("INDUCTOR", InductorStamper.class);
        register("DIODE", DiodeStamper.class);
        register("VOLTAGE_SOURCE", VoltageSourceStamper.class);
        // ... etc
    }

    public static void register(String type,
                                Class<? extends IMatrixStamper> stamperClass) {
        registry.put(type, stamperClass);
    }

    public static IMatrixStamper getStamper(String type) {
        Class<? extends IMatrixStamper> stamperClass = registry.get(type);
        return stamperClass.newInstance();  // Reflection
    }
}
```

**Why This Works:**
- Decouples component type from implementation
- Adding new component: Implement IMatrixStamper + register
- No if-statements in simulation engine

**4.2 Component Class**

```java
public class Component implements IpesFileable {
    private String type;
    private int[] nodes;
    private double[] parameters;
    private IMatrixStamper stamper;

    public void initialize() {
        // Assign stamper based on type
        this.stamper = StamperRegistry.getStamper(type);
    }

    public IMatrixStamper getStamper() {
        return stamper;
    }
}
```

---

### 5. Topology Analysis (350w)

**5.1 Node Numbering**

Algorithm:
1. Scan all components, collect unique node IDs
2. Find ground node (ID = 0 or labeled "GND")
3. Assign indices: ground = 0, others = 1, 2, 3, ...
4. Count unknowns: N_nodes + N_branches (voltage sources, inductors)

**5.2 Ground Detection**

```java
public class TopologyAnalyzer {
    public int findGround(List<Component> components) {
        // Strategy 1: Look for node ID 0
        if (nodeExists(components, 0)) return 0;

        // Strategy 2: Look for node labeled "GND"
        int gndNode = findNodeByLabel(components, "GND");
        if (gndNode != -1) return gndNode;

        // Strategy 3: Use most-connected node
        return findMostConnectedNode(components);
    }
}
```

**5.3 Matrix Sizing**

```java
int n_nodes = uniqueNodes.size() - 1;  // Exclude ground
int n_branches = countVoltageSources() + countInductors();
int matrixSize = n_nodes + n_branches;

double[][] A = new double[matrixSize][matrixSize];
double[] b = new double[matrixSize];
double[] x = new double[matrixSize];
```

---

### 6. Simulation Engine (800w)

**6.1 Main Time-Stepping Loop**

```java
// From: SimulationsKern.java (simplified)
public class SimulationsKern {

    public SimulationResults run(Circuit circuit, SolverSettings settings) {
        int n = circuit.getMatrixSize();
        double dt = settings.getTimeStep();
        double tmax = settings.getDuration();

        // Storage for results
        List<double[]> voltageHistory = new ArrayList<>();
        List<double[]> currentHistory = new ArrayList<>();

        // Initial conditions
        double[] x = new double[n];  // Start at 0V

        // Time loop
        for (double t = 0; t < tmax; t += dt) {
            // Build system matrices
            double[][] A = new double[n][n];
            double[] b = new double[n];

            // Stamp all components
            for (Component comp : circuit.getComponents()) {
                IMatrixStamper stamper = comp.getStamper();
                stamper.stampMatrixA(A, comp.getNodes(), comp.getParams(), dt);
                stamper.stampVectorB(b, comp.getNodes(), comp.getParams(),
                                    dt, t, x);
            }

            // Solve (with Newton-Raphson if nonlinear)
            if (circuit.hasNonlinearComponents()) {
                x = solveNonlinear(A, b, x, settings);
            } else {
                x = cholesky.solve(A, b);
            }

            // Calculate component currents
            for (Component comp : circuit.getComponents()) {
                double current = comp.getStamper().calculateCurrent(
                    x[comp.getNodeX()], x[comp.getNodeY()],
                    comp.getParams(), dt, comp.getPreviousCurrent()
                );
                comp.setCurrent(current);
            }

            // Store results
            voltageHistory.add(x.clone());
            currentHistory.add(getCurrents(circuit));

            // Update scope (GUI or WebSocket)
            scopeUpdate(t, x, circuit);
        }

        return new SimulationResults(voltageHistory, currentHistory);
    }

    private double[] solveNonlinear(double[][] A, double[] b,
                                    double[] x_guess, SolverSettings settings) {
        // Newton-Raphson iteration (Week 4)
        SolverContext context = new SolverContext(settings);
        return context.solveNonlinear(circuit, x_guess);
    }
}
```

**Key Points:**
- Generic loop: works for any circuit
- Stamping phase: components contribute independently
- Solve phase: Cholesky (Week 3) or Newton-Raphson (Week 4)
- Current calculation: post-processing after voltage solve
- Scope update: real-time feedback

**6.2 SolverContext Integration**

Nonlinear circuits call SolverContext (from Week 4):
- Outer loop: time stepping
- Inner loop: Newton-Raphson iteration
- Each NR iteration solves linear system with Cholesky

---

### 7. Scope & Data Output (500w)

**7.1 IScopeData Interface**

```java
public interface IScopeData {
    /**
     * Add data point to waveform
     */
    void addDataPoint(double time, double value);

    /**
     * Get waveform data for plotting
     */
    double[][] getWaveformData();

    /**
     * Get signal statistics (min, max, avg, RMS)
     */
    SignalStatistics getStatistics();

    /**
     * Export to CSV
     */
    void exportToCSV(String filePath);
}
```

**7.2 ScopeChannel Class**

```java
public class ScopeChannel implements IScopeData {
    private List<Double> timePoints = new ArrayList<>();
    private List<Double> valuePoints = new ArrayList<>();
    private String label;

    @Override
    public void addDataPoint(double time, double value) {
        timePoints.add(time);
        valuePoints.add(value);

        // Real-time WebSocket update (if enabled)
        if (webSocketEnabled) {
            messagingTemplate.convertAndSend(
                "/topic/scope/" + label,
                new DataPoint(time, value)
            );
        }
    }

    @Override
    public SignalStatistics getStatistics() {
        double min = Collections.min(valuePoints);
        double max = Collections.max(valuePoints);
        double avg = valuePoints.stream().average().orElse(0.0);
        double rms = Math.sqrt(
            valuePoints.stream()
                      .mapToDouble(v -> v*v)
                      .average()
                      .orElse(0.0)
        );
        return new SignalStatistics(min, max, avg, rms);
    }
}
```

**7.3 WebSocket Integration (Phase 1)**

From strategic roadmap - REST API with WebSocket updates:

```java
@Controller
public class SimulationWebSocketController {

    @MessageMapping("/simulation/start")
    @SendTo("/topic/simulation/status")
    public SimulationStatus startSimulation(SimulationRequest request) {
        // Start simulation in background thread
        simulationEngine.run(request.getCircuit(), this::onScopeUpdate);
        return new SimulationStatus("RUNNING");
    }

    private void onScopeUpdate(double time, double[] voltages) {
        // Throttle updates to 30 fps
        if (shouldThrottle(time)) return;

        // Send to all connected clients
        messagingTemplate.convertAndSend(
            "/topic/scope/voltages",
            new ScopeUpdate(time, voltages)
        );
    }
}
```

Real-time updates while simulation runs. PLECS can't do this!

---

### 8. Signal Processing (600w)

**8.1 FFT (Fast Fourier Transform)**

From Week 5 LinkedIn preview:

```java
// From: Cispr16Fft.java
public class Cispr16Fft {

    public double[] computeSpectrum(double[] timeSeries, double sampleRate) {
        // Apply Hanning window
        double[] windowed = applyHanningWindow(timeSeries);

        // Zero-pad to power of 2
        double[] padded = zeroPad(windowed, nextPowerOfTwo(timeSeries.length));

        // Cooley-Tukey FFT
        Complex[] spectrum = fft(padded);

        // Convert to magnitude (dBµV for CISPR16)
        return toMagnitude(spectrum);
    }

    private Complex[] fft(double[] x) {
        // Recursive Cooley-Tukey algorithm
        // ... (standard FFT implementation)
    }
}
```

**8.2 THD (Total Harmonic Distortion)**

```java
public double computeTHD(double[] spectrum, double fundamentalFreq) {
    int fundamentalBin = (int)(fundamentalFreq / binWidth);
    double fundamental = spectrum[fundamentalBin];

    double harmonicsPower = 0.0;
    for (int n = 2; n <= MAX_HARMONIC; n++) {
        int bin = n * fundamentalBin;
        if (bin < spectrum.length) {
            harmonicsPower += spectrum[bin] * spectrum[bin];
        }
    }

    return Math.sqrt(harmonicsPower) / fundamental * 100;  // Percent
}
```

**8.3 CISPR16 Compliance**

EMC standard for conducted emissions (150kHz - 30MHz):

```java
public boolean checkCISPR16Compliance(double[] spectrum, double[] freqs) {
    // CISPR16 limits: 66 dBµV at 150kHz, 56 dBµV at 30MHz
    for (int i = 0; i < spectrum.length; i++) {
        double freq = freqs[i];
        double limit = interpolateCISPR16Limit(freq);

        if (spectrum[i] > limit) {
            return false;  // Exceeds limit
        }
    }
    return true;  // Passes
}
```

**Why This Is Powerful:**
- Automated pre-compliance testing
- No hardware needed for initial design
- Parametric sweeps to optimize filters

---

### 9. Design Patterns Summary (300w)

**Patterns Used:**

**Strategy Pattern:**
- Interface: IMatrixStamper
- Implementations: ResistorStamper, CapacitorStamper, DiodeStamper, ...
- Benefit: Simulation engine treats all components uniformly

**Factory Pattern:**
- StamperRegistry creates stampers by type
- ComponentFactory creates components from XML
- Benefit: Decouples type strings from classes

**Observer Pattern:**
- IScopeData interface
- ScopeChannels notify GUI/WebSocket on updates
- Benefit: Real-time visualization without tight coupling

**Dependency Inversion:**
- Simulation depends on IMatrixStamper (abstraction)
- Not on concrete ResistorStamper (implementation)
- Benefit: Can swap solver implementations without changing components

**Why These Patterns Matter:**
- 20 years of development → needs maintainability
- Open source → easy for contributors to extend
- Research tool → must support custom components

---

### 10. What Makes GeckoCIRCUITS Different (350w)

**vs SPICE:**
- Open source (SPICE also open, but GeckoCIRCUITS more modern)
- Signal processing built-in (FFT, THD, CISPR16)
- REST API (SPICE is command-line only)
- Object-oriented (SPICE is procedural C)

**vs PLECS:**
- Free (PLECS is €1000+)
- Extensible (source code available)
- Educational (designed for teaching)
- REST API + WebSocket (PLECS has GUI only)

**vs MATLAB Simulink:**
- Faster for power electronics (optimized for circuits)
- Better signal analysis (CISPR16)
- No licensing hassles

**Unique Features:**
- IMatrixStamper interface (clean extension mechanism)
- Dual-track strategy (desktop + REST API)
- EMC analysis tools (CISPR16Fft)
- 20 years of power electronics domain knowledge

---

### 11. Exercises (200w)

**Exercise 1:** Trace simulation pipeline for RC circuit (all 10 steps)
**Exercise 2:** Implement custom component (e.g., varistor V = k·I^α)
**Exercise 3:** Add REST endpoint for FFT spectrum

---

### 12. Series Recap (250w)

**5 Weeks of Learning:**

Week 1: MNA foundations, A·x = b, matrix stamping
Week 2: Time integration, Backward Euler, capacitors/inductors
Week 3: Sparse matrices, Cholesky, 100x performance
Week 4: Newton-Raphson, nonlinear components, convergence
Week 5: Complete architecture, signal processing, REST API

**What You Can Now Do:**
- Understand how SPICE/PLECS work internally
- Implement custom component models
- Debug convergence failures
- Optimize simulation performance
- Build circuit simulation tools

**What's Next:**
- Explore GeckoCIRCUITS source code
- Contribute to open source
- Build your own simulation tools
- Teach circuit simulation to students

---

### 13. Future Topics (Poll Results) (150w)

Based on Week 5 Friday poll:
- AC analysis (small-signal, Bode, stability)
- Optimization (parameter sweeps, gradient descent)
- Multi-physics (thermal-electrical)
- Advanced (state-space, model order reduction)

Next series starts in 2 weeks!

---

### 14. CTA (100w)

Thank you for following the 5-week journey!

- Subscribe for future series
- Star GeckoCIRCUITS on GitHub
- Share with colleagues
- Try the exercises
- Comment with questions

See you in the next series!

---

## Source Files:

**Core:**
- `SimulationsKern.java` (main loop)
- `IpesFileable.java` (file I/O)
- `StamperRegistry.java` (component factory)

**Scope:**
- `IScopeData.java` (interface)
- `ScopeChannel.java` (implementation)

**Signal Processing:**
- `Cispr16Fft.java` (FFT + CISPR16)
- `THD.java` (harmonics analysis)

**REST API:**
- `SimulationController.java` (Spring Boot endpoints)
- `SimulationWebSocketController.java` (real-time updates)

---

## Code Examples:

1. Complete simulation loop (SimulationsKern)
2. IPES XML parsing
3. StamperRegistry
4. IScopeData implementation
5. WebSocket updates
6. FFT + THD computation
7. CISPR16 compliance check

---

## Diagrams:

1. 10-step pipeline flowchart
2. Class diagram (key interfaces)
3. Design patterns illustration
4. WebSocket architecture
5. Signal processing flow (time → FFT → THD)

---

## SEO Keywords:

- Circuit simulator architecture
- GeckoCIRCUITS tutorial
- SPICE vs PLECS comparison
- Circuit simulation REST API
- Power electronics simulation
