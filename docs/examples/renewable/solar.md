---
title: Grid-Tied Solar Inverter Example
---

# Grid-Tied Solar Inverter Example

Single-phase photovoltaic inverter with Maximum Power Point Tracking (MPPT) and grid synchronization.

## Overview

Grid-tied solar inverters convert DC power from PV arrays to AC power synchronized with the utility grid:
- Maximum Power Point Tracking (MPPT) maximizes PV output
- Phase-Locked Loop (PLL) for grid synchronization
- Current injection in phase with grid voltage
- Anti-islanding protection
- Reactive power support capability
- High efficiency (>97%)

## Specifications

| Parameter | Value |
|-----------|-------|
| PV Input Voltage Range | 200-450V DC |
| PV Rated Power | 5 kW |
| AC Output Voltage | 230V RMS, 50 Hz |
| AC Output Power | 5 kW (continuous) |
| MPPT Range | 200-400V DC |
| Grid Connection | Single-phase (Vac @ 50 Hz) |
| Power Factor | 0.99 (unity or leading) |
| THD (Total Harmonic Distortion) | <3% |
| Efficiency | >97% |
| DC-AC Topology | Full-bridge inverter |
| Switching Frequency | 16 kHz |

## Circuit Files

- `pv_inverter_basic.ipes` - Single-phase full-bridge inverter
- `pv_inverter_mppt.ips` - With Perturb & Observe MPPT control
- `pv_inverter_grid_sync.ipes` - PLL-based grid synchronization
- `pv_inverter_complete.ipes` - Full system with all controls
- `pv_array_model.ipes` - PV cell and array characteristics

## System Architecture

```
┌──────────────────┐
│   PV Array       │ V_pv ∈ [200, 450V]
│ (5 kW nominal)   │
└────────┬─────────┘
         │
         ▼
    ┌─────────────┐
    │ MPPT DC-DC  │  Boost/Buck converter
    │ Converter   │  Tracks V_mpp
    └────┬────────┘
         │
         ▼
    ┌─────────────┐       ┌──────────────┐
    │  400V DC    │──────►│ Full-Bridge  │
    │   Bus       │       │   Inverter   │
    └─────────────┘       └────┬─────────┘
                               │
                               ▼
                          ┌──────────┐
                          │ L-C      │
                          │ Filter   │
                          └─────┬────┘
                                │
                                ▼
                          ┌──────────┐
                          │  Grid    │
                          │ (230V AC)│
                          └──────────┘
```

## Theory

### Photovoltaic Cell Model

The single-diode PV cell model:

$$I = I_{ph} - I_0\left(e^{\frac{q(V + IR_s)}{nk_B T}} - 1\right) - \frac{V + IR_s}{R_{sh}}$$

**Where:**
- Iph: Photocurrent (proportional to irradiance)
- I0: Diode reverse saturation current
- q: Elementary charge (1.6×10⁻¹⁹ C)
- n: Ideality factor (1-2)
- kB: Boltzmann constant (1.38×10⁻²³ J/K)
- T: Junction temperature (K)
- Rs: Series resistance (Ω)
- Rsh: Shunt resistance (Ω)

**Simplified form (ignoring Rs and Rsh):**
$$I = I_{ph} - I_0 e^{qV/(nk_B T)}$$

### Maximum Power Point (MPP)

Power output: P = V × I

At the MPP, the voltage-power curve has zero slope:
$$\frac{dP}{dV} = 0 \implies I + V\frac{dI}{dV} = 0$$

**Key characteristics:**
- MPP voltage ≈ 0.7–0.8 × Voc (open-circuit voltage)
- Varies with temperature and irradiance
- Typical PV module: Vmpp ≈ 30V (at STC: 1000 W/m², 25°C)
- Array: Series/parallel connection to reach desired voltage

### Temperature Dependence

MPP voltage decreases with temperature:
$$V_{mpp}(T) = V_{mpp,ref} - \alpha(T - T_{ref})$$

Where α ≈ 0.5 mV/°C (temperature coefficient, negative).

## MPPT Control Algorithms

### Perturb and Observe (P&O)

Simplest and most common algorithm:

```
Loop at time interval Ts:
  1. Measure V(k), I(k), P(k) = V(k) × I(k)
  2. Calculate ΔP = P(k) - P(k-1), ΔV = V(k) - V(k-1)
  3. Update reference:
     if ΔP > 0
       if ΔV > 0
         V_ref = V_ref + ΔV_step  (moved right on curve)
       else
         V_ref = V_ref - ΔV_step  (moved left on curve)
     else
       if ΔV > 0
         V_ref = V_ref - ΔV_step  (moved away from MPP)
       else
         V_ref = V_ref + ΔV_step  (moved away from MPP)
```

**Advantages:** Simple, few computations
**Disadvantages:** Oscillates around MPP (can be reduced with small step size)

### Incremental Conductance (IncCond)

More sophisticated, detects MPP directly:

$$\frac{dI}{dV} = -\frac{I}{V} \quad \text{at MPP}$$

```
Loop at time interval Ts:
  1. Measure ΔI/ΔV (conductance change)
  2. Calculate I/V (instantaneous conductance)
  3. Compare:
     if ΔI/ΔV = -I/V       → At MPP, hold V_ref
     if ΔI/ΔV > -I/V       → Left of MPP, increase V_ref
     if ΔI/ΔV < -I/V       → Right of MPP, decrease V_ref
```

**Advantages:** Fast tracking, no steady-state oscillation
**Disadvantages:** More computation, noise sensitivity

## Grid Synchronization via PLL

The Phase-Locked Loop (PLL) detects the phase angle of the grid voltage:

```
                    ┌───────────────┐
vgrid ──────────────┤  Normalize &  │
                    │  Filter (LP)  │
                    └────────┬──────┘
                             │ sin(θ_grid)
                             ▼
                    ┌───────────────┐
                    │  Multiplier   │ × V_ref
                    │   (demod)     │
                    └────────┬──────┘
                             │ e_pll
                             ▼
                    ┌───────────────┐
                    │  PI Control   │
                    │               │
                    └────────┬──────┘
                             │ ω_pll
                             ▼
                    ┌───────────────┐
                    │  Integrator   │
                    │  θ_pll = ∫ω   │
                    └───────────────┘
                             │
                             └──► θ_grid estimate
```

**PLL Equations:**

Error signal: e_pll = vgrid × sin(θ_pll - θ_grid) ≈ Vgrid·(θ_pll - θ_grid)

Angular frequency: ω_pll = ω_grid + Kp·epll + Ki·∫epll

**Tuning:** Bandwidth ≈ 50-100 Hz (fast PLL locks to grid variations)

## Current Control for Grid Injection

**Reference Current Generation:**

The inverter injects current in phase with grid voltage:
$$i_{ref} = I_{max} \sin(\omega_e t + \theta_pll)$$

Where Imax is set by available PV power:
$$I_{max} = \frac{P_{pv}}{V_{grid,rms}}$$

**Reactive Power (Optional):**

For grid support, inject reactive current:
$$i_{ref} = I_d \sin(\omega_e t + \theta_{pll}) + I_q \cos(\omega_e t + \theta_{pll})$$

Where Id = active current, Iq = reactive current.

**Current Control Loop:**

```
         i_ref
           │
      ┌────┴────┐
      │    -    │
      └────┬────┘
           │ i_error
           ▼
      ┌─────────┐
      │   PI    │
      │ Control │
      └────┬────┘
           │ v_ref
           ▼
    ┌─────────────┐
    │    SPWM     │
    │ (carrier)   │
    └─────┬───────┘
          ▼
    ┌─────────────┐
    │  Inverter   │
    │  Switches   │
    └─────────────┘
```

## Anti-Islanding Protection

Detects when the grid is disconnected (preventing island formation).

### Passive Detection Methods

1. **Frequency Drift:** If grid disconnects, frequency drifts (no stiff voltage source)
2. **Voltage Drift:** RMS voltage deviates from nominal
3. **Rate of Change:** dV/dt or df/dt monitoring

### Active Detection Methods

1. **Frequency Shift (Frequency Bias):** Inject small frequency offset; detect via grid coupling
2. **Reactive Power Injection:** Inject reactive power and observe voltage response
3. **Voltage Shift:** Modulate voltage injection level

### Typical Thresholds (EN 50160 / IEC 61727)

| Parameter | Trip Level | Trip Time |
|-----------|------------|-----------|
| Overvoltage (>150% Vnom) | 264V | 0.16 s |
| Undervoltage (<85% Vnom) | 170V | 2 s |
| Overfrequency (>52 Hz) | 52 Hz | 0.16 s |
| Underfrequency (<48 Hz) | 48 Hz | 0.16 s |

## Control Structure

```
         PV Array
            │
            ▼
      ┌──────────┐
      │  MPPT    │ Vpv_ref
      │ Control  │
      │ (P&O)    │
      └─────┬────┘
            │
            ▼
       ┌────────────┐
       │ DC-DC Buck │ I_mppt
       │ Converter  │
       └────┬───────┘
            │ (400V DC)
            ▼
      ┌─────────────┐
      │ Grid Voltage│ sin(ωt + θ)
      │ Sensing PLL │
      └────┬────────┘
           │ θ_grid, ω_grid
      ┌────┴──────────────┐
      │ Current Reference │ i_ref
      │ Generator         │
      └────┬──────────────┘
           │
      ┌────┴─────────┐
      │  - (subtraction)
      └────┬────────┘
           │ i_error
           ▼
      ┌────────────┐
      │ PI Current │
      │ Control    │
      └────┬───────┘
           │ v_ref
           ▼
      ┌────────────────┐
      │ Sinusoidal PWM │
      │ (SPWM) / SVM   │
      └────┬───────────┘
           │
           ▼
      ┌────────────┐
      │ Full-Bridge│
      │ Inverter   │
      └────┬───────┘
           │
           ▼
      ┌─────────────┐
      │  L-C Filter │
      └────┬────────┘
           │
           ▼
        ┌─────┐
        │Grid │
        └─────┘
```

## Design Steps

### Step 1: PV Array Configuration

For 200-450V DC input:
- Typical module: 40V at MPP
- Series: 5-10 modules (200-400V nominal)
- Parallel: 2-3 strings (increases current)

### Step 2: MPPT DC-DC Converter

Design a boost/buck converter:
- Input: 200-450V DC (PV array)
- Output: 400V DC (regulated)
- Power: 5 kW

Equations:
$$D = 1 - \frac{V_{pv}}{V_{out}}$$
$$L = \frac{V_{pv}(1-D)}{f_{sw} \Delta I_L}, \quad \Delta I_L = 0.2 \times I_{pv}$$
$$C = \frac{I_{pv} D}{f_{sw} V_r}$$

### Step 3: Full-Bridge Inverter

Select switches for 400V DC bus:
- IGBT or SiC MOSFET rated for 600V minimum
- Switching frequency: 16 kHz (balance switching loss vs. filter size)
- Deadtime: 200-500 ns

Gate driver PWM frequency: fsw = 16 kHz
Modulation index: Ma = 0.9 (for linear region)

### Step 4: Output L-C Filter

Reduce high-frequency switching ripple:
$$L = \frac{V_{dc}}{8 f_{sw} I_{max}}, \quad C = \frac{I_{max}}{8 f_{sw} V_r}$$

Example: L = 5 mH (reduces di/dt), C = 5 μF (10% voltage ripple)

### Step 5: PLL Tuning

For 50 Hz grid, typical PLL bandwidth 50-100 Hz:
$$K_p = 2 \times BW, \quad K_i = BW^2$$

For BW = 100 Hz: Kp = 200, Ki = 10000

### Step 6: MPPT Tuning

Sampling interval: 100 ms (slow update, stable)
Step size: ΔVref = 5V (balance speed vs. oscillation)

## Exercises

1. **PV Curve Tracing:** Measure I-V and P-V curves at different irradiance levels
2. **MPPT Algorithm:** Implement P&O or IncCond, verify tracking efficiency >99%
3. **PLL Synchronization:** Measure phase error; tune for fast lock (<100 ms)
4. **Current Injection:** Verify sine wave injection in phase with grid (PF > 0.98)
5. **THD Measurement:** Measure output voltage THD; design filter for <3%
6. **Anti-Islanding:** Simulate grid disconnection; verify detection within 2 seconds
7. **Efficiency Mapping:** Plot efficiency vs. load and irradiance
8. **Reactive Power:** Inject Q = ±2 kVAr for grid support if applicable

## Related Resources

- [Wind Converter](wind.md)
- [PFC Converters](../power-supplies/pfc.md)
- [Grid-Tied Inverter Design](../../tutorials/dcac/grid-tied-inverters.md)
- [Three-Phase Systems](../../tutorials/dcac/three-phase.md)
- [Renewable Energy Integration](../../tutorials/renewable/index.md)
