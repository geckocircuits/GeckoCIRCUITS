---
title: Matrix Stampers API
---

# Matrix Stampers API

Package: `gecko.core.circuit.matrix`

MNA (Modified Nodal Analysis) matrix stamping strategies for circuit simulation. Each component type has a stamper that knows how to contribute to the system matrices.

## Overview

The MNA equation is: **A * x = b**

- **A** - admittance/conductance matrix
- **x** - vector of unknowns (node voltages, branch currents)
- **b** - source/excitation vector

Each component type stamps its contribution differently into A and b.

---

## IMatrixStamper

Strategy interface for stamping component contributions into MNA matrices.

### Methods

#### `stampMatrixA`

```java
void stampMatrixA(double[][] a, int nodeX, int nodeY, int nodeZ,
                  double[] parameter, double dt);
```

Stamps the component's contribution into the A matrix (conductance/admittance).

**Example (resistor R between nodes x and y):**

- `a[x][x] += 1/R`
- `a[y][y] += 1/R`
- `a[x][y] -= 1/R`
- `a[y][x] -= 1/R`

**Parameters:**

| Parameter | Description |
|-----------|-------------|
| `a` | The A matrix to stamp into |
| `nodeX` | First node index |
| `nodeY` | Second node index |
| `nodeZ` | Auxiliary node index (for voltage sources, inductors) |
| `parameter` | Component parameters array |
| `dt` | Time step size |

#### `stampVectorB`

```java
void stampVectorB(double[] b, int nodeX, int nodeY, int nodeZ,
                  double[] parameter, double dt, double time,
                  double[] previousValues);
```

Stamps the component's contribution into the b vector (sources).

**Example (current source I from node x to y):**

- `b[x] -= I`
- `b[y] += I`

#### `calculateCurrent`

```java
double calculateCurrent(double nodeVoltageX, double nodeVoltageY,
                        double[] parameter, double dt, double previousCurrent);
```

Calculates the component current after matrix solution.

#### `getAdmittanceWeight`

```java
double getAdmittanceWeight(double parameterValue, double dt);
```

Gets the admittance weight factor (e.g., `1/R` for resistor, `C/dt` for capacitor).

---

## IStatefulStamper

Extension of `IMatrixStamper` for components that maintain internal state across time steps (e.g., inductors, capacitors with history).

---

## StamperRegistry

Registry that maps circuit component types to their matrix stamper implementations. Implements the Strategy pattern for MNA matrix stamping.

```java
StamperRegistry registry = StamperRegistry.createDefault();
IMatrixStamper stamper = registry.getStamper(CircuitTypCore.LK_R);
stamper.stampMatrixA(a, nodeX, nodeY, nodeZ, params, dt);
```

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| `createDefault()` | `StamperRegistry` | Factory with all standard stampers |
| `getStamper(type)` | `IMatrixStamper` | Get stamper for component type |
| `register(type, stamper)` | `void` | Register custom stamper |
| `getSupportedTypes()` | `Set<CircuitTypCore>` | All registered types |
| `hasStamper(type)` | `boolean` | Check if type is registered |

---

## Built-in Stamper Implementations

| Class | Component | A-matrix Pattern |
|-------|-----------|-----------------|
| `ResistorStamper` | Resistor | `1/R` conductance |
| `CapacitorStamper` | Capacitor | `C/dt` companion model |
| `InductorStamper` | Inductor | `dt/L` companion model |
| `VoltageSourceStamper` | Voltage source | Branch equation row |
| `CurrentSourceStamper` | Current source | b-vector only |
| `DiodeStamper` | Diode | Piecewise-linear model |
| `MOSFETStamper` | MOSFET | Switch + on-resistance |
| `IGBTStamper` | IGBT | Switch + saturation voltage |
| `ThyristorStamper` | Thyristor | Latching switch model |
| `IdealSwitchStamper` | Ideal switch | Zero/infinite impedance |

---

## SolverContext

Configuration context for the simulation solver, specifying which integration method to use.

| Solver | Constant | Description |
|--------|----------|-------------|
| Backward Euler | `SOLVER_BE` | First-order implicit, unconditionally stable |
| Trapezoidal | `SOLVER_TRZ` | Second-order, may exhibit numerical oscillation |
| Gear-Shichman | `SOLVER_GS` | Multi-step, good for stiff circuits |

## Related

- [Simulation Engine](simulation.md)
- [Control Calculators](calculators.md)
