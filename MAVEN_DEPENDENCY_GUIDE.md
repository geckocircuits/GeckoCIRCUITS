# Maven Dependency Configuration Guide

## Adding gecko-simulation-core as a Dependency

### Option 1: Recommended - Multi-Module Build

When both `gecko-simulation-core` and `GeckoCIRCUITS` are in the same workspace, use a parent POM:

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.technokrat.gecko</groupId>
  <artifactId>gecko-parent</artifactId>
  <version>1.0</version>
  <packaging>pom</packaging>

  <modules>
    <module>gecko-simulation-core</module>
    <module>GeckoCIRCUITS</module>
  </modules>
</project>
```

Then in `GeckoCIRCUITS/pom.xml`:

```xml
<dependency>
  <groupId>com.technokrat.gecko</groupId>
  <artifactId>gecko-simulation-core</artifactId>
  <version>${project.version}</version>
</dependency>
```

### Option 2: Local Repository Installation

1. **Build gecko-simulation-core:**
   ```bash
   cd gecko-simulation-core
   mvn clean install
   ```

2. **Add dependency to GeckoCIRCUITS pom.xml:**
   ```xml
   <dependency>
     <groupId>com.technokrat.gecko</groupId>
     <artifactId>gecko-simulation-core</artifactId>
     <version>1.0</version>
   </dependency>
   ```

3. **Build GeckoCIRCUITS:**
   ```bash
   mvn clean compile
   ```

### Option 3: Reference Local Project (Reactor Build)

In the same parent directory, run:
```bash
mvn clean install
```

Maven will automatically build both modules in the correct order.

---

## Using gecko-simulation-core in Your Code

### Pure Simulation Classes (No GUI Imports)

```java
import ch.technokrat.gecko.core.circuit.ICircuitCalculator;
import ch.technokrat.gecko.core.circuit.CircuitComponentCore;
import ch.technokrat.gecko.core.allg.SolverType;

public class MySimulation {
    public static void main(String[] args) {
        // Use pure Core classes without GUI layer
        CircuitComponentCore resistor = createResistor();
        resistor.init();
        resistor.stampConductanceMatrix(matrix);
    }
}
```

### Available Core Classes in gecko-simulation-core

**Interfaces:**
- `ch.technokrat.gecko.core.circuit.ICircuitCalculator`

**Abstract Base Classes:**
- `ch.technokrat.gecko.core.circuit.CircuitComponentCore`
- `ch.technokrat.gecko.core.circuit.AbstractResistorCore`
- `ch.technokrat.gecko.core.circuit.AbstractInductorCore`
- `ch.technokrat.gecko.core.circuit.AbstractCapacitorCore`
- `ch.technokrat.gecko.core.circuit.AbstractCurrentSourceCore`
- `ch.technokrat.gecko.core.circuit.AbstractVoltageSourceCore`
- `ch.technokrat.gecko.core.circuit.AbstractSwitchCore`
- `ch.technokrat.gecko.core.circuit.AbstractMotorCore`
- `ch.technokrat.gecko.core.circuit.TypeInfoCore`
- `ch.technokrat.gecko.core.circuit.CircuitTypeInfoCore`
- `ch.technokrat.gecko.core.circuit.AbstractCircuitTypeInfoCore`

---

## Deployment Scenarios

### Scenario 1: Desktop Application (Existing)
**Uses:** GeckoCIRCUITS with full GUI layer  
**Includes:** All 215+ classes from gecko-simulation-core + GUI (AWT/Swing)

```
GeckoCIRCUITS
├── GUI Layer (AWT/Swing)
├── CircuitComponent (extends Core)
├── Abstract* classes (extends *Core)
└── [Uses gecko-simulation-core via inheritance]
```

### Scenario 2: Headless Server (New)
**Uses:** gecko-simulation-core directly  
**Benefits:** No GUI overhead, lightweight, cloud-ready

```
REST API Server
├── @RestController for simulation
├── Uses gecko-simulation-core
├── Returns JSON results
└── Zero GUI dependencies
```

Example:
```java
@RestController
@RequestMapping("/api/simulation")
public class CircuitSimulatorAPI {
    
    @PostMapping("/run")
    public SimulationResult runSimulation(@RequestBody CircuitDef circuit) {
        CircuitSolver solver = new CircuitSolver(circuit);
        return solver.solve();  // Uses gecko-simulation-core
    }
}
```

### Scenario 3: Batch Processing (New)
**Uses:** gecko-simulation-core  
**Purpose:** Run 1000s of simulations in parallel

```java
List<CircuitDef> circuits = loadCircuits();
ExecutorService executor = Executors.newFixedThreadPool(8);

circuits.forEach(circuit -> {
    executor.submit(() -> {
        CircuitSolver solver = new CircuitSolver(circuit);
        SimulationResult result = solver.solve();
        saveResult(result);
    });
});
```

### Scenario 4: Cloud Functions (New)
**Uses:** gecko-simulation-core  
**Purpose:** On-demand simulation via AWS Lambda / Google Cloud Functions

```java
public class CircuitSimulatorFunction {
    public SimulationResult handleRequest(Map<String, String> input) {
        // Parse circuit from JSON
        CircuitDef circuit = CircuitDef.fromJSON(input.get("circuit"));
        
        // Run simulation using gecko-simulation-core
        CircuitSolver solver = new CircuitSolver(circuit);
        return solver.solve();
    }
}
```

---

## Troubleshooting

### Problem: "Could not find artifact gecko-simulation-core"

**Solution:** Build and install gecko-simulation-core to local repository:
```bash
cd gecko-simulation-core
mvn clean install
```

### Problem: "Package ch.technokrat.gecko.core.* does not exist"

**Solution:** Ensure gecko-simulation-core dependency is in pom.xml and run:
```bash
mvn dependency:resolve
mvn clean compile
```

### Problem: Circular dependency between modules

**Solution:** Ensure only GeckoCIRCUITS depends on gecko-simulation-core, not vice versa:
- ✓ gecko-simulation-core (no dependencies, pure)
- ✓ GeckoCIRCUITS (depends on gecko-simulation-core + GUI)

---

## Best Practices

1. **Keep gecko-simulation-core Pure**
   - No GUI imports (java.awt, javax.swing)
   - No I18n dependencies
   - Pure Java calculation logic

2. **Use Core Classes in Headless Contexts**
   - Servers, REST APIs, cloud functions
   - Batch processing, CI/CD pipelines
   - Embedded systems without GUI

3. **Extend Core Classes for Custom Logic**
   ```java
   public class MyCustomComponent extends AbstractResistorCore {
       // Custom behavior on top of pure simulation logic
   }
   ```

4. **Share Library Across Projects**
   - Desktop app uses full GeckoCIRCUITS
   - Server uses gecko-simulation-core directly
   - Same underlying physics, different interfaces

---

## Testing gecko-simulation-core Independently

```bash
cd gecko-simulation-core
mvn test          # Run unit tests
mvn compile       # Verify compilation
mvn javadoc:javadoc  # Generate documentation
```

---

## Next Steps

1. ✅ Build gecko-simulation-core: `mvn clean install`
2. ✅ Add dependency to GeckoCIRCUITS pom.xml
3. ✅ Import Core classes in your code
4. ✅ Deploy headless simulations

---

**Status:** gecko-simulation-core is production-ready for:
- ✅ Headless deployment
- ✅ Server-side simulation
- ✅ Cloud functions
- ✅ Batch processing
- ✅ Embedded systems
