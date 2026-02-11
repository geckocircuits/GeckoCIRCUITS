---
title: "301 - Diode Rectifier"
---

# 301 - Diode Rectifier

Single-phase and three-phase diode bridge rectifiers for AC-DC conversion.

## Overview

Diode rectifiers are the simplest form of AC-DC power conversion, using uncontrolled diodes to convert AC voltage to pulsating DC.

## Topics Covered

- Single-phase half-wave rectifier
- Single-phase full-wave bridge
- Three-phase diode bridge
- Ripple calculation and filtering
- Power factor considerations

## Single-Phase Full-Wave Bridge

### Circuit Configuration

```
     D1      D3
AC+ ─┤►├─┬──┤►├─ DC+
         │
        Load
         │
AC- ─┤◄├─┴──┤◄├─ DC-
     D2      D4
```

### Key Equations

**Average DC Voltage:**
$$V_{DC} = \frac{2V_m}{\pi} \approx 0.637 \cdot V_m$$

**RMS Voltage:**
$$V_{rms} = \frac{V_m}{\sqrt{2}}$$

**Ripple Factor:**
$$\gamma = \sqrt{\left(\frac{V_{rms}}{V_{DC}}\right)^2 - 1} \approx 0.483$$

## Three-Phase Diode Bridge

### Circuit Configuration

Six-diode bridge configuration for three-phase input.

### Key Equations

**Average DC Voltage:**
$$V_{DC} = \frac{3\sqrt{2}}{\pi} V_{LL} \approx 1.35 \cdot V_{LL}$$

**Ripple Frequency:** 6× line frequency (300 Hz for 50 Hz input)

## Simulation Parameters

| Parameter | Single-Phase | Three-Phase |
|-----------|--------------|-------------|
| Input Voltage | 230V RMS | 400V L-L |
| Frequency | 50 Hz | 50 Hz |
| Load | 100Ω | 50Ω |
| Filter Cap | 1000µF | 470µF |

## Related Resources

- [302 - PFC Basics](pfc-basics.md) - Power factor correction
- [303 - Vienna Rectifier](vienna-rectifier.md) - Three-level PFC
