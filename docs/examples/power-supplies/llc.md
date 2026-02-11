---
title: LLC Resonant Converter Example
---

# LLC Resonant Converter Example

High-efficiency resonant DC-DC converter with Zero Voltage Switching (ZVS) and Zero Current Switching (ZCS) operation.

## Overview

The LLC resonant converter offers:
- Zero Voltage Switching (ZVS) for all switches across wide range
- Zero Current Switching (ZCS) for secondary-side diodes
- High efficiency (>96%) with minimal active losses
- Wide input voltage range with frequency modulation
- Soft-switching reduces EMI and switching losses
- Ideal for high-power density applications

## Specifications

| Parameter | Value |
|-----------|-------|
| Input Voltage | 360-400V DC |
| Output Voltage | 48V DC |
| Output Power | 1 kW |
| Resonant Frequency (fr) | 250 kHz |
| Switching Frequency Range | 200-300 kHz |
| Transformer Turns Ratio | 9:1 (step-down) |
| Efficiency Target | >95% |
| Output Ripple Voltage | <1% |

## Circuit Files

- `llc_basic_operation.ipes` - Basic LLC resonant tank operation
- `llc_frequency_sweep.ipes` - Gain vs frequency analysis
- `llc_load_variation.ipes` - Efficiency across load range
- `llc_zvs_verification.ipes` - Switch voltage and current waveforms

## LLC Tank Topology

```
Primary Side:              Resonant Tank:        Secondary Side:

   Vin ──┐                 Lr       Cr              ┌─ Vout
        │                ───⊏⊐───┤├───┬────        │
      ┌─┴─┐              │        │    │       ┌────┴────┐
      │S1 │──────────────┤    T    │    ├──Cr──┤ D1  D2  ├─┐
      │S4 │──────┐       │   ::   │    │       │      │  │ C_out
      └───┘      │       │        │    │       │  D3  D4 │ ───
      ┌───┐      │       └────────┴────┴───────┤         │
      │S2 │──────┼─────────────────────────────┤ │ Tr   ├─┘
      │S3 │      │                             │  ::    │
      └─┬─┘      │       Lm (Magnetizing)      │         │
        │        │            │                └─────────┘
        └────────┤            │
               GND           GND
```

**Tank Components:**
- Lr: Series resonant inductance (μH range)
- Cr: Series resonant capacitance (nF range)
- Lm: Magnetizing inductance (parallel with primary winding)
- T: High-frequency transformer (250 kHz rated)

## Theory

### Resonant Frequencies

The LLC tank exhibits two natural resonant frequencies:

**Series Resonance:**
$$f_r = \frac{1}{2\pi\sqrt{L_r C_r}}$$

All current flows through Lr and Cr; Lm is "invisible" at series resonance.

**Parallel Resonance:**
$$f_p = \frac{1}{2\pi\sqrt{(L_r || L_m) C_r}} = \frac{1}{2\pi\sqrt{\frac{L_r L_m}{L_r + L_m} C_r}}$$

At parallel resonance, current divides between Lr and Lm.

### Quality Factor and Impedance Ratio

**Quality Factor:**
$$Q = \frac{Z_0}{R_{ac}} = \frac{\sqrt{L_r/C_r}}{R_{ac}}$$

Where the AC load resistance (referred to primary):
$$R_{ac} = \left(\frac{8}{\pi^2}\right) n^2 R_{load}$$

And n is the transformer turns ratio (primary to secondary).

**Inductance Ratio:**
$$L_n = \frac{L_m}{L_r}$$

Typical design range: Ln = 3 to 7

### Voltage Gain via Fundamental Harmonic Analysis (FHA)

The voltage gain is frequency-dependent:

$$M(f_n) = \frac{L_n f_n^2}{\sqrt{(1 + L_n - L_n f_n^2)^2 + \frac{Q^2}{f_n^2}(f_n - 1/f_n)^2 L_n^2}}$$

Where fn = fsw/fr is the normalized frequency.

**Key characteristics:**
- At fr: M = 1/(1+Ln) (minimum gain, highest Q)
- At fp: Maximum gain plateau
- Above fp: M increases toward M∞ = 1 (ideal transformer)

### Operating Regions

| Operating Region | fsw vs fr | Characteristics | ZVS | ZCS |
|------------------|-----------|-----------------|-----|-----|
| **Light Load** | fsw < fr | High impedance, current lags voltage | Yes | Yes |
| **At Resonance** | fsw = fr | Minimum impedance, lowest losses | Yes | Yes |
| **Heavy Load** | fsw > fr | Lower impedance, current leads voltage | Yes | Yes |

### Energy Storage Analysis

**Peak current in resonant tank:**
$$I_{peak} = \frac{V_{in}}{Z_0}$$

**ZVS Condition:**

For soft switching, the energy stored in Lr must be sufficient to charge/discharge the switch capacitances:

$$E_r = \frac{1}{2}L_r I_{peak}^2 \geq E_{sw} = \frac{1}{2}C_{sw}(2V_{sw})^2$$

This ensures voltage reaches zero before current reverses.

## Design Procedure

### Step 1: Select Resonant Frequency

Choose fr based on:
- Core loss (smaller cores require higher frequency)
- Component availability
- EMI considerations
- Typical range: 100-500 kHz

For 1 kW converter, fr = 250 kHz is common.

### Step 2: Design Resonant Tank

**Design Equations:**

For desired output voltage Vo:
$$n = \frac{V_{in}}{V_o \cdot M_{max}}$$

Where Mmax is the desired maximum gain (typically 1.2-1.5).

$$L_r = \frac{Z_0}{2\pi f_r}$$

$$C_r = \frac{1}{2\pi f_r Z_0}$$

Characteristic impedance Z0 is chosen based on:
$$Z_0 = \sqrt{\frac{L_r}{C_r}} = 10 - 20 \, \Omega$$

Lower Z0 → higher current ripple but higher gain range.

### Step 3: Select Magnetizing Inductance

Lm is determined by:
$$L_m = L_n \cdot L_r$$

Higher Ln → steeper gain curve but narrower soft-switching range.

### Step 4: Component Selection

**Resonant Inductor:**
- Gapped ferrite core or distributed air gap
- Rated for peak current: Ipeak = Vin/(2πfrZ0)
- Tolerance: ±10% (affects gain curve)

**Resonant Capacitor:**
- Film or ceramic with low ESR
- Voltage rating ≥ 1.5× peak voltage across Cr
- Example: 48 nF at 450V (2 × 24 nF in series)

**Transformer:**
- Ferrite core (EE, EI, or RM series)
- Primary inductance ≈ 3–5 × Lr (becomes Lm)
- Leakage inductance < 5% of primary inductance
- Frequency rating: fr+safety margin

**Power Switches:**
- MOSFETs or IGBTs rated for Vin
- CIss, Qoss important for ZVS margin
- Example: 600V SiC MOSFET for 400V input

**Output Rectifier:**
- Schottky or SiC diodes (low forward voltage)
- Current rating ≥ Iout_max × 1.5
- SiC better for high-frequency, lower loss

### Step 5: Output Filter

**LC Filter:**
$$L_{out} = \frac{V_o(1-D)}{f_{sw} \Delta I_L}$$

$$C_{out} = \frac{I_{out} D}{f_{sw} V_r}$$

Where ripple current ≔ 20% of output current, voltage ripple ≔ 1%.

## Control Strategy

### Frequency Modulation

The simplest control method varies switching frequency to regulate output voltage:

```
Vout sensing
     │
     ▼
  ┌────────┐
  │ PI Reg │ ──► fsw command
  └────────┘
     ▲
     │
  Vout (measured)
```

**PI Controller:**
$$f_{sw} = f_r + K_p(V_{out}^* - V_{out}) + K_i \int (V_{out}^* - V_{out}) dt$$

**Range:** fsw ∈ [fr, 2×fr] typically

**Advantages:**
- Simple to implement
- Natural soft-switching over wide range
- Single control variable

### Duty Cycle Modulation (Alternative)

Primary switches can also use modulated duty cycle while fsw = fr:
- More complex (requires multiple control loops)
- Maintains resonant frequency advantage
- Useful for bidirectional converters

## Efficiency Considerations

**Loss Components:**

1. **Core Loss (Pcore):** Proportional to Bpeak × f1.6
2. **Copper Loss (Pcu):** I²R in windings and tank
3. **Switching Loss:** Minimized by ZVS
4. **Diode Loss:** Forward drop × Iavg in secondary
5. **Capacitor ESR Loss:** ESR × I²

**Typical Loss Budget (1 kW converter):**
- Core: 30-40 W
- Windings: 20-30 W
- Switches: 10-15 W (ZVS advantage)
- Diodes: 15-25 W
- **Total: 50-70 W (95-98% efficiency)**

## Exercises

1. **Frequency Sweep:** Plot gain vs frequency; identify series and parallel resonance
2. **ZVS Verification:** Capture switch voltage and current; verify zero crossing at turn-on
3. **Load Variation:** Measure efficiency at 25%, 50%, 75%, 100% load; plot curves
4. **Component Sensitivity:** Vary Lr, Cr, Lm ±10%; observe gain curve shift
5. **Soft-Switching Range:** Determine minimum and maximum fsw for ZVS at light and full load
6. **Thermal Management:** Estimate junction temperature of switches and diodes at rated power
7. **Output Ripple Analysis:** Measure and reduce ripple via Cout and Lout tuning

## Related Resources

- [DAB Converter](dab.md)
- [PFC Converters](pfc.md)
- [EV Charger](../automotive/ev-charger.md)
- [502 - Junction Temperature Analysis](../../tutorials/thermal/junction-temperature.md)
- [Resonant Converter Design Guide](../../tutorials/power-supplies/resonant-design.md)
