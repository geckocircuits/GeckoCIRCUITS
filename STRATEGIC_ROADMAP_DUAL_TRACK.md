# GeckoCIRCUITS Strategic Roadmap - Dual Track Approach

**Vision:** Maintain Desktop power-user experience while adding modern web accessibility
**Strategy:** Shared simulation core, multiple interfaces (Desktop GUI, REST API, RMI, WebAssembly)
**Status:** January 2026 - Revised from "Desktop Migration" to "Desktop + Web Synergy"

---

## 🎯 Core Insight

**The GeckoCIRCUITS Desktop application with RMI is a mature, valuable product** that has:
- ✅ Rich GUI with mature scope/waveform visualization
- ✅ Proven RMI interface for MATLAB/Simulink integration
- ✅ Fast native execution without network overhead
- ✅ Established power-user base at ETH Zurich and industry
- ✅ Comprehensive signal processing (RMS, THD, FFT, harmonics)
- ✅ Advanced features (thermal simulation, EMI analysis, subcircuits)

**Rather than replacing it, we should:**
1. **Keep Desktop alive** as the professional/power-user edition
2. **Add Web/REST API** for accessibility, cloud deployment, automation
3. **Extract shared core** that both interfaces use
4. **Maintain feature parity** between Desktop RMI and REST API
5. **Enable hybrid workflows** (Desktop GUI + Python/MATLAB scripting via REST)

---

## 📊 Product Matrix

| Feature | Desktop (RMI) | REST API | WebAssembly Browser | Mobile (Future) |
|---------|---------------|----------|---------------------|-----------------|
| **Target Users** | Power users, researchers | Automation, cloud, CI/CD | Students, quick demos | Field engineers |
| **Deployment** | Local install | Server/cloud | Browser (no install) | iOS/Android app |
| **Performance** | ⭐⭐⭐⭐⭐ Fast | ⭐⭐⭐⭐ Very good | ⭐⭐⭐ Good | ⭐⭐ Limited |
| **GUI Richness** | ⭐⭐⭐⭐⭐ Full Swing | ⭐⭐ API-driven | ⭐⭐⭐⭐ React/WebGL | ⭐⭐⭐ Native |
| **Visualization** | Native scopes | JSON/CSV export | Canvas/WebGL | Chart libraries |
| **MATLAB Integration** | ⭐⭐⭐⭐⭐ Direct RMI | ⭐⭐⭐⭐ HTTP REST | ⭐⭐ Limited | ❌ No |
| **Scripting** | GeckoScript (JS) | Python/curl/any HTTP | JavaScript | Swift/Kotlin |
| **Offline Use** | ✅ Yes | ❌ Requires server | ✅ Yes (after load) | ⚠️ Limited |
| **Parallelization** | Single machine | ✅ Cluster/cloud | ✅ Web Workers | ❌ No |
| **License Model** | GPL Desktop | GPL Server | GPL + MIT client | TBD |

**Key Takeaway:** Each interface serves different use cases. Don't pick one - support all.

---

## 🏗️ Shared Architecture (Revised)

### Current State (Before Refactoring)
```
┌─────────────────────────────────────┐
│   Desktop Application (Monolith)   │
│  ┌──────────┐  ┌────────────────┐  │
│  │   GUI    │  │ GeckoRemote    │  │
│  │ (Swing)  │  │ RMI Interface  │  │
│  └─────┬────┘  └────────┬───────┘  │
│        │                │           │
│  ┌─────▼────────────────▼────────┐ │
│  │   Simulation Engine (Core)    │ │
│  │  (SimulationsKern, NetListLK) │ │
│  └───────────────────────────────┘ │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│      REST API (New, Sprint 1)       │
│  ┌────────────────────────────────┐ │
│  │    Spring Boot Controllers     │ │
│  └────────────┬───────────────────┘ │
│  ┌────────────▼───────────────────┐ │
│  │  LegacySimulationBridge        │ │
│  │  (Reflection wrapper)          │ │
│  └────────────┬───────────────────┘ │
│               │ Calls via reflection│
└───────────────┼─────────────────────┘
                ▼
      Desktop JAR (on classpath)
```

**Problem:** Desktop and REST API duplicate functionality, no shared code.

---

### Target State (After Refactoring - Phase 2)

```
┌─────────────────────────────────────────────────────────────────┐
│                   SIMULATION CORE (Shared Library)              │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────────────┐  │
│  │ Circuit      │  │ Solver      │  │ Signal Processing    │  │
│  │ Parser       │  │ Engine      │  │ (RMS, THD, FFT)      │  │
│  │ (.ipes, .xml)│  │ (Kern)      │  │                      │  │
│  └──────────────┘  └─────────────┘  └──────────────────────┘  │
│                                                                  │
│  Pure domain logic - NO GUI dependencies                        │
│  Technology: Java 21, GraalVM-ready for native compilation     │
└─────────────────────────────────────────────────────────────────┘
                                   ▲
                ┌──────────────────┼──────────────────┐
                │                  │                  │
┌───────────────▼────┐  ┌──────────▼────────┐  ┌─────▼──────────┐
│  Desktop GUI       │  │   REST API        │  │   WebAssembly  │
│  ┌──────────────┐  │  │  ┌─────────────┐ │  │  ┌──────────┐  │
│  │ Swing/JavaFX │  │  │  │  Spring     │ │  │  │  React   │  │
│  │ Rich Scopes  │  │  │  │  Boot       │ │  │  │  WebGL   │  │
│  └──────────────┘  │  │  │  WebSocket  │ │  │  │  Canvas  │  │
│  ┌──────────────┐  │  │  └─────────────┘ │  │  └──────────┘  │
│  │ RMI Server   │  │  │                   │  │                 │
│  │ (MATLAB)     │  │  │  OpenAPI/Swagger │  │  Browser-based │
│  └──────────────┘  │  │  Python SDK      │  │  No install    │
└────────────────────┘  └───────────────────┘  └────────────────┘
```

**Benefits:**
1. **Single source of truth** for simulation logic
2. **Feature parity** - RMI and REST expose same capabilities
3. **Test once** - Unit tests cover all interfaces
4. **Optimize once** - Performance improvements benefit all
5. **Flexible deployment** - Choose interface per use case

---

## 📅 Revised Roadmap (2026-2027)

### Phase 1: REST API + Signal Analysis (Current - Weeks 1-4) ✅ IN PROGRESS

**Goal:** Add REST API without disrupting Desktop, expose signal processing

**Deliverables:**
- ✅ REST API with async simulation (COMPLETED Sprint 1)
- ✅ Signal analysis endpoints (RMS, THD, FFT) (COMPLETED this session)
- ✅ WebSocket real-time updates (COMPLETED this session)
- ⏳ Interactive simulation (step-by-step control)
- ⏳ Circuit validation endpoint

**Status:** Desktop remains unchanged, REST API uses bridge to legacy code

---

### Phase 2: Core Extraction (Weeks 5-12)

**Goal:** Extract simulation core into shared library used by both Desktop and REST

**Approach:**
1. **Create `gecko-simulation-core` module** (pure Java, no GUI)
   - Move `SimulationsKern`, `NetListLK`, `CircuitSheet`
   - Move signal processing (RMS, THD, FFT libraries)
   - Remove all Swing/GUI dependencies
   - Package: `com.technokrat.gecko.core.*`

2. **Refactor Desktop to use shared core**
   - Desktop GUI becomes thin wrapper around core
   - RMI interface calls core directly (no reflection)
   - Maintain 100% backward compatibility for users
   - GeckoRemoteInterface unchanged

3. **Refactor REST API to use shared core**
   - Replace `LegacySimulationBridge` reflection with direct calls
   - Same API endpoints, cleaner implementation
   - Better error handling, faster performance

4. **Shared test suite**
   - Unit tests for core run in CI/CD
   - Integration tests verify both Desktop RMI and REST API
   - Golden file validation (compare Desktop vs REST results)

**Success Criteria:**
- Desktop JAR still works standalone
- REST API 50% faster (no reflection overhead)
- Both Desktop and REST produce identical results for same .ipes file
- Zero breaking changes for existing Desktop users

---

### Phase 3: Feature Parity & PLECS Compatibility (Weeks 13-18)

**Goal:** Both Desktop and REST expose same advanced features

**Desktop Enhancements:**
- Add REST server mode: `java -jar gecko-desktop.jar --rest-server --port 8080`
- Users can run Desktop GUI + REST API simultaneously
- GUI updates when REST API triggers simulation (shared state)

**REST API Enhancements:**
- Hierarchical circuit navigation (Phase 1 plan)
- Scope management API
- State variable access
- Python PLECS compatibility adapter

**Hybrid Workflows Enabled:**
1. **Desktop GUI + Python scripting:**
   - User designs circuit in Desktop GUI
   - Python script via REST API runs parameter sweeps
   - Results appear in Desktop scopes in real-time

2. **MATLAB + Desktop visualization:**
   - MATLAB calls simulation via RMI (existing)
   - Desktop GUI shows live waveforms
   - REST API also available for other tools

3. **CI/CD + Desktop validation:**
   - Jenkins runs simulations via REST API
   - Engineers validate results in Desktop GUI
   - Shared .ipes files, identical results

---

### Phase 4: WebAssembly Browser Edition (Weeks 19-26)

**Goal:** Run GeckoCIRCUITS entirely in browser (no server needed)

**Technology:**
- GraalVM Native Image → WebAssembly
- OR: TeaVM (Java to WebAssembly transpiler)
- React/TypeScript frontend
- WebGL for scope visualization
- Local file system API for .ipes files

**Use Cases:**
- Students: Try GeckoCIRCUITS without installation
- Demos: Share circuit URL, runs in browser
- Documentation: Interactive examples in docs
- Education: Embedded in online courses

**Limitations:**
- Slower than native (but acceptable for small circuits)
- No MATLAB integration (browser sandbox)
- Reduced feature set (focus on core simulation)

**Deployment:**
- Hosted at `https://geckocircuits.ethz.ch/web`
- Can also run offline (Progressive Web App)
- Free for educational use

---

## 🔀 Migration Strategy: Desktop Users

**Key Principle: Desktop users are NOT forced to migrate**

### Option 1: Stay on Desktop (Recommended for power users)
- Continue using Desktop GUI + RMI as before
- Benefits from core improvements (bug fixes, performance)
- Can optionally enable REST server for hybrid workflows
- **No action required**

### Option 2: Hybrid Desktop + REST
- Run Desktop with `--rest-server` flag
- Use Desktop GUI for visualization
- Use Python/REST for automation/parameter sweeps
- Best of both worlds

### Option 3: Pure REST API (For cloud/automation)
- Deploy `gecko-rest-api.jar` on server
- No GUI, headless operation
- Access via curl/Python/any HTTP client
- Ideal for CI/CD, batch processing

### Option 4: Browser Edition (For quick demos)
- Open browser, no installation
- Limited features, educational use
- Not suitable for large simulations

**Migration Timeline:** NONE - all options coexist indefinitely

---

## 📦 Packaging & Distribution

### Desktop Edition (`gecko-desktop.jar`)
- Includes: GUI, RMI server, simulation core
- Size: ~15 MB (with dependencies)
- Platforms: Windows, Linux, macOS
- License: GPL v3
- Distribution: Download from GitHub releases

### REST API Edition (`gecko-rest-api.jar`)
- Includes: Spring Boot, REST endpoints, simulation core
- Size: ~30 MB (with Spring)
- Platforms: Any with Java 21+
- License: GPL v3
- Distribution: Docker image, JAR download

### Core Library (`gecko-simulation-core.jar`)
- Includes: Only simulation engine, no interfaces
- Size: ~5 MB
- Platforms: Any JVM, GraalVM native
- License: GPL v3
- Distribution: Maven Central, GitHub packages

### WebAssembly Edition (Browser)
- Includes: Core + React frontend (compiled to .wasm)
- Size: ~10 MB download, cached by browser
- Platforms: Any modern browser
- License: GPL v3 (core) + MIT (frontend)
- Distribution: Hosted at ethz.ch, CDN

---

## 🎓 Educational Impact

### Before (Desktop Only)
- ❌ Students must install Java
- ❌ University must maintain lab machines
- ❌ Limited to on-campus access
- ❌ No integration with online courses

### After (Desktop + Web)
- ✅ Try in browser instantly
- ✅ Desktop for advanced projects
- ✅ Python scripting in Jupyter notebooks
- ✅ Embedded in MOOCs (Coursera, edX)
- ✅ Mobile access for quick checks

---

## 💼 Industry Use Cases

### Power Electronics Companies
**Desktop:** Engineers use GUI for interactive design
**REST API:** Automated overnight parameter optimization
**Hybrid:** Engineer reviews top 10 designs from automated sweep in Desktop GUI

### Automotive Simulation
**Desktop:** Development engineers design power converters
**REST API:** CI/CD pipeline validates designs on every commit
**WebAssembly:** Suppliers demo components to customers in browser

### Research Labs (ETH Zurich)
**Desktop:** PhD students use RMI with MATLAB for research
**REST API:** Server farm runs 1000s of Monte Carlo simulations
**Core Library:** Embedded in custom tools (FPGAs, real-time systems)

---

## 🚀 Competitive Positioning

### vs PLECS
| Aspect | PLECS | GeckoCIRCUITS (Dual Track) |
|--------|-------|----------------------------|
| Desktop GUI | ⭐⭐⭐⭐⭐ Excellent | ⭐⭐⭐⭐ Very good |
| REST API | ❌ None | ✅ Full support |
| Open Source | ❌ Commercial | ✅ GPL v3 |
| MATLAB Integration | ⭐⭐⭐⭐⭐ Native | ⭐⭐⭐⭐ RMI + REST |
| Cloud Deployment | ❌ Not supported | ✅ Docker, Kubernetes |
| Browser Edition | ❌ None | ✅ WebAssembly |
| Education | Paid licenses | ✅ Free, open source |
| Signal Analysis | ⭐⭐⭐⭐ Good | ⭐⭐⭐⭐⭐ Excellent (FFT, THD, CISPR16) |

**Unique Value Proposition:**
"PLECS-compatible workflows + modern REST API + open source + deploy anywhere"

---

## 🔧 Technical Implementation Plan

### Step 1: Extract Core (Week 5-8)
```bash
# Create new module
mkdir gecko-simulation-core
cd gecko-simulation-core

# Move core packages
mv gecko-legacy-desktop/src/main/java/ch/technokrat/gecko/geckocircuits/circuit .
mv gecko-legacy-desktop/src/main/java/ch/technokrat/gecko/geckocircuits/math .
# ... (keep GUI separate)

# Remove GUI dependencies
# Replace Swing components with pure domain objects

# Update pom.xml
<dependencies>
  <!-- Only pure Java dependencies, no Swing -->
  <dependency>
    <groupId>org.jtransforms</groupId>
    <artifactId>jtransforms</artifactId>
  </dependency>
</dependencies>
```

### Step 2: Refactor Desktop (Week 9-10)
```java
// Before: Monolithic
public class GeckoSim {
    public static void main(String[] args) {
        // GUI + Simulation mixed together
    }
}

// After: Layered
public class GeckoDesktop {
    private final SimulationEngine engine; // From gecko-simulation-core
    private final GeckoGUI gui;

    public static void main(String[] args) {
        SimulationEngine engine = new SimulationEngine();

        if (args.contains("--rest-server")) {
            // Start REST API alongside GUI
            RestApiServer.start(engine, port);
        }

        GeckoGUI gui = new GeckoGUI(engine);
        gui.show();
    }
}
```

### Step 3: Update REST API (Week 11-12)
```java
// Before: Reflection bridge
public class LegacySimulationBridge {
    Method runSimMethod = geckoCustomInstance.getClass()
        .getMethod("runSimulation");
    runSimMethod.invoke(geckoCustomInstance); // Slow
}

// After: Direct calls
public class SimulationService {
    private final SimulationEngine engine; // Direct reference

    public SimulationResult runSimulation(Path ipesFile) {
        Circuit circuit = engine.loadCircuit(ipesFile);
        return engine.simulate(circuit); // Fast, type-safe
    }
}
```

---

## 📈 Success Metrics

### Adoption Metrics (6 months after Phase 3)
- Desktop users: Maintained or grown (target: +10%)
- REST API users: 50+ organizations
- WebAssembly page views: 10,000+ monthly
- GitHub stars: 500+ (currently ~100)

### Technical Metrics
- Simulation performance: Desktop = REST API (exact same results)
- API response time: < 100ms (p95) for signal analysis
- Test coverage: > 80% for core module
- Zero critical bugs in Desktop (backward compatibility)

### Community Metrics
- Active contributors: 10+ (currently 2-3)
- Documentation: 100+ example circuits
- Forum activity: 50+ posts/month
- Educational adoption: 5+ universities using browser edition

---

## 🤝 Community & Contribution

### Desktop Development (Swing GUI)
- Mature, stable codebase
- Focus on bug fixes, minor improvements
- Preserve workflows for existing users
- Contribution barrier: Medium (Swing knowledge)

### REST API Development (Spring Boot)
- Active development, new features
- Modern stack, attracts new contributors
- OpenAPI spec for easy integration
- Contribution barrier: Low (standard REST)

### Core Simulation Engine
- Shared between all interfaces
- Performance-critical, needs expert review
- Domain logic, fewer contributors needed
- Contribution barrier: High (circuit simulation knowledge)

### WebAssembly Frontend (React/TypeScript)
- Greenfield project, very active
- Attracts frontend developers
- Good first contribution target
- Contribution barrier: Low (web development)

**Contribution Strategy:** Different skill sets → different modules → broader community

---

## 🎯 Immediate Next Steps (This Sprint)

### Week 1-2: Finish Phase 1 ✅
- [x] Signal analysis endpoints (COMPLETED)
- [x] WebSocket real-time updates (COMPLETED)
- [ ] Interactive simulation mode
- [ ] Circuit validation endpoint
- [ ] Update documentation

### Week 3-4: Phase 1 Polish
- [ ] Integration tests for all Phase 1 features
- [ ] Python example scripts (use REST API + show Desktop visualization)
- [ ] Docker image for REST API
- [ ] Performance benchmarks (Desktop vs REST comparison)

### Week 5: Planning Phase 2
- [ ] Design core module structure
- [ ] Identify GUI dependencies to remove
- [ ] Create migration plan for Desktop users
- [ ] Estimate effort for core extraction

---

## 💡 Key Insights

1. **Desktop is NOT legacy** - it's a mature product with unique advantages
2. **Multiple interfaces = broader reach** - Desktop for power users, REST for automation, WebAssembly for students
3. **Shared core = consistency** - Same simulation results across all interfaces
4. **PLECS compatibility benefits both** - Desktop gains from modern API improvements
5. **Open source advantage** - Can deploy anywhere, modify as needed

---

## ❓ FAQ

**Q: Will Desktop development stop?**
A: No. Desktop gets bug fixes, performance improvements from shared core, and optional REST server mode.

**Q: Do Desktop users need to install anything new?**
A: No. Desktop JAR works standalone as before. REST server is optional flag.

**Q: Can I use Desktop GUI and REST API simultaneously?**
A: Yes! Run Desktop with `--rest-server`, use GUI for visualization, REST for scripting.

**Q: Will REST API results match Desktop exactly?**
A: Yes. Both use identical simulation core. Golden file tests verify this.

**Q: Is WebAssembly as fast as Desktop?**
A: No, ~2-5x slower. But fast enough for small circuits and demos.

**Q: Can I contribute to just one interface?**
A: Yes! Frontend devs can work on WebAssembly, backend devs on REST API, domain experts on core.

**Q: Will MATLAB integration break?**
A: No. GeckoRemoteInterface RMI remains unchanged. Existing MATLAB scripts work as-is.

**Q: What about Python integration with Desktop?**
A: Use REST API! Run Desktop with `--rest-server`, Python calls REST, results show in Desktop scopes.

---

## 📞 Contact & Governance

**Project Lead:** ETH Zurich Power Electronic Systems Laboratory
**Maintainers:** Core team (TBD after Phase 2)
**License:** GPL v3 (all components)
**Repository:** https://github.com/geckocircuits/GeckoCIRCUITS
**Forum:** TBD (Discourse or GitHub Discussions)
**Chat:** TBD (Discord or Slack)

---

**Last Updated:** January 2026
**Next Review:** After Phase 1 completion (February 2026)
**Status:** ✅ Strategic direction approved, implementation in progress
