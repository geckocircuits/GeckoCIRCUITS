---
title: Core Module API
---

# Core Module API Reference

The `gecko-simulation-core` module provides GUI-free simulation classes for headless operation, REST APIs, and embedded use. All classes in this module are validated to have no GUI imports (`java.awt`, `javax.swing`).

## Package Overview

| Package | Classes | Description |
|---------|---------|-------------|
| [`simulation`](simulation.md) | 5 | Headless simulation engine, zero-crossing detection |
| [`circuit.matrix`](matrix-stampers.md) | 15 | MNA matrix stampers for circuit components |
| `circuit.netlist` | 4 | Netlist building and node indexing |
| `circuit.component` | 10+ | Component definitions |
| [`control.calculators`](calculators.md) | 72 | Control block calculators (PI, PID, math, logic) |
| `datacontainer` | 8 | Signal data storage with caching |
| `math` | 8 | Matrix operations, LU decomposition, FFT |

## Architecture

```
┌──────────────────────────────────────────────────┐
│              HeadlessSimulationEngine             │
│  ┌────────────────┐  ┌────────────────────────┐  │
│  │ SimulationConfig│  │  ZeroCrossingDetector  │  │
│  └────────────────┘  └────────────────────────┘  │
├──────────────────────────────────────────────────┤
│                  Circuit Layer                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │  Matrix   │  │ Netlist  │  │  Components  │   │
│  │ Stampers  │  │ Builder  │  │              │   │
│  └──────────┘  └──────────┘  └──────────────┘   │
├──────────────────────────────────────────────────┤
│               Control & Math Layer               │
│  ┌──────────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Calculators  │  │   Math   │  │   Data   │   │
│  │  (72 classes) │  │ (LU,FFT) │  │Container │   │
│  └──────────────┘  └──────────┘  └──────────┘   │
└──────────────────────────────────────────────────┘
```

## Maven Coordinates

```xml
<dependency>
    <groupId>gecko</groupId>
    <artifactId>gecko-simulation-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Quality Metrics

- JaCoCo coverage enforcement: 60%+ instruction coverage
- GUI-free validation: `CorePackageValidationTest` ensures no AWT/Swing imports
- Build: `mvn -f pom-reactor.xml test -pl src/modules/gecko-simulation-core`

## Related

- [Simulation Engine](simulation.md) - HeadlessSimulationEngine, ZeroCrossingDetector
- [Matrix Stampers](matrix-stampers.md) - IMatrixStamper, StamperRegistry
- [Control Calculators](calculators.md) - PI, PID, math, and logic blocks
