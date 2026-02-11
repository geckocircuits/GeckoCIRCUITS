---
title: EV Charger (Level 2 AC)
---

# EV Charger (Level 2 AC)

## Overview

A Level 2 electric vehicle charger converts single-phase or three-phase AC to DC for battery charging. This example demonstrates a basic AC-DC conversion topology with power factor correction.

**Difficulty:** Intermediate

**Status:** Placeholder

## Specifications

| Parameter | Value | Unit |
|-----------|-------|------|
| Input Voltage | 208-240 | VAC |
| Output Voltage | 250-450 | VDC |
| Output Power | 7.2-19.2 | kW |
| Power Factor | >0.99 | - |
| Efficiency | >94% | - |

## Topology: Boost PFC + Isolated DC-DC

```
                    PFC Stage                    Isolated Stage
AC ─[EMI]─[Rectifier]─[Boost PFC]─[DC Link]─[Full Bridge]─[Transformer]─[Rectifier]─ DC Out
     │                    │            │                                      │
   Filter            L, D, S        C_link                                  C_out
```

## Key Design Points

1. **EMI Filter:** Meet CISPR 25 emissions
2. **Boost PFC:** Unity power factor, regulated DC link
3. **Isolation:** Transformer provides galvanic isolation
4. **Output Regulation:** CC/CV charging profile

## Circuit Files

> **Status:** Placeholder
> - `ev_charger_pfc.ipes` - PFC front-end
> - `ev_charger_complete.ipes` - Full charger

---
*Placeholder - Details to be added*
