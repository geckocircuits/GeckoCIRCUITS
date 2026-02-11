# Tutorial 106: State Machines

## Overview

Implement complex control sequences using state machine logic. Learn to model startup sequences, fault handling, and multi-mode operation in power converters.

**Level:** Intermediate (2/3)

**Duration:** 35-45 minutes

**Series:** Getting Started

**Status:** Placeholder - Implementation depends on available components

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Understand finite state machine (FSM) concepts
- [ ] Implement state machines using control blocks
- [ ] Design startup and shutdown sequences
- [ ] Handle fault conditions with state logic

## Prerequisites

- Complete [Tutorial 103: PWM Basics](../103_pwm_basics/)
- Basic understanding of digital logic
- Familiarity with control block diagram modeling

## State Machine Fundamentals

### What is a State Machine?

A finite state machine (FSM) is a system that can be in one of a finite number of states, with transitions triggered by events or conditions.

```
    ┌─────────┐  Enable   ┌─────────┐  Vout OK   ┌─────────┐
    │  IDLE   │──────────►│ STARTUP │───────────►│ RUNNING │
    └─────────┘           └─────────┘            └─────────┘
         ▲                     │                      │
         │                     │ Timeout              │ Fault
         │                     ▼                      ▼
         │               ┌─────────┐            ┌─────────┐
         └───────────────│  FAULT  │◄───────────│SHUTDOWN │
              Reset      └─────────┘            └─────────┘
```

### State Machine Elements

| Element | Description | Example |
|---------|-------------|---------|
| State | Operating mode | IDLE, RUNNING, FAULT |
| Transition | Change between states | Enable signal |
| Condition | Logic for transition | Vout > threshold |
| Action | Output in each state | PWM enable/disable |

## Power Converter States

### Typical Operating States

| State | Description | PWM | Outputs |
|-------|-------------|-----|---------|
| IDLE | Powered off, waiting | OFF | All off |
| SOFT_START | Ramping up | ON (limited D) | Ramping |
| RUNNING | Normal operation | ON | Regulated |
| CURRENT_LIMIT | Overcurrent condition | ON (limited) | Limited |
| FAULT | Protection active | OFF | Fault flag |
| SHUTDOWN | Controlled stop | Ramping down | Decreasing |

### State Encoding

```
STATE[2:0]:
  000 = IDLE
  001 = SOFT_START
  010 = RUNNING
  011 = CURRENT_LIMIT
  100 = FAULT
  101 = SHUTDOWN
```

## Implementation Methods

### Method 1: Logic Blocks

Use comparators, AND/OR gates, flip-flops:

```
                    ┌─────┐
    Enable ─────────┤     │
                    │ AND ├────► State_1
    Condition ──────┤     │
                    └─────┘
```

### Method 2: Java Block

Implement FSM in code within a Java control block:

```java
// State machine in Java block
switch (state) {
    case IDLE:
        if (enable) state = SOFT_START;
        break;
    case SOFT_START:
        if (vout > target) state = RUNNING;
        if (timeout) state = FAULT;
        break;
    case RUNNING:
        if (fault) state = SHUTDOWN;
        break;
    // ... etc
}
```

### Method 3: Lookup Table

Use signal-based state encoding with multiplexers.

## Example: Soft-Start State Machine

### States

1. **IDLE:** Wait for enable
2. **SOFT_START:** Ramp duty cycle from 0 to target
3. **RUNNING:** Normal closed-loop operation
4. **FAULT:** Overcurrent or overvoltage detected

### Transition Logic

```
IDLE → SOFT_START:    Enable = 1
SOFT_START → RUNNING: Vout > 0.9 × Vtarget AND t > Tsoft
SOFT_START → FAULT:   Iout > Ilimit OR t > Timeout
RUNNING → FAULT:      Iout > Ilimit OR Vout > Vmax
FAULT → IDLE:         Reset = 1 AND Enable = 0
```

### Actions per State

| State | Duty Cycle | Fault Flag |
|-------|------------|------------|
| IDLE | 0 | 0 |
| SOFT_START | Ramping (0 to D_target) | 0 |
| RUNNING | Controlled | 0 |
| FAULT | 0 | 1 |

## Building the State Machine

### Step 1: Define States and Transitions

1. List all states (typically 4-8 for power converters)
2. Define transition conditions
3. Define outputs for each state

### Step 2: Create State Register

1. Use integrator or memory block to hold state
2. State changes on transition conditions
3. Initialize to IDLE state

### Step 3: Implement Transition Logic

For each transition:
```
Next_State = Current_State IF no transition
           = New_State IF condition met
```

### Step 4: Output Logic

```
PWM_Enable = 1 IF (State = SOFT_START OR State = RUNNING)
           = 0 OTHERWISE
```

### Step 5: Connect to Power Stage

1. State machine outputs → PWM modulator
2. Power stage signals → State machine inputs (feedback)

## Example Implementation

### Soft-Start Ramp Generator

```
           ┌─────────────────┐
Enable ───►│                 │
           │   State         │───► PWM_Enable
Vout ─────►│   Machine       │───► Fault_Flag
           │                 │
Iout ─────►│                 │───► D_reference
           └─────────────────┘
                   │
                   ▼ (internal)
              ┌─────────┐
              │ Ramp    │
              │Generator│
              └─────────┘
```

### Ramp in SOFT_START State

```
D_ref = min(D_target, t / T_softstart)
```

Where:
- t = time since entering SOFT_START
- T_softstart = soft-start duration (e.g., 10ms)

## Exercises

### Exercise 1: Basic On/Off State Machine
1. Create 2-state machine: IDLE, RUNNING
2. Transition on Enable signal
3. Control PWM on/off

### Exercise 2: Soft-Start Implementation
1. Add SOFT_START state
2. Implement 10ms duty cycle ramp
3. Verify output voltage ramps smoothly

### Exercise 3: Overcurrent Protection
1. Add FAULT state
2. Transition when Iout > 1.2 × Irated
3. Require manual reset to restart

### Exercise 4: Full State Machine
1. Implement all 5 states
2. Add timeout protection
3. Test various fault scenarios

## Timing Considerations

### Debouncing

Avoid false transitions from noise:
```
Transition IF condition = 1 FOR N consecutive cycles
```

### Minimum State Time

Prevent rapid state cycling:
```
Allow transition only IF time_in_state > T_minimum
```

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Stuck in state | Missing transition | Check all paths |
| Oscillating states | No hysteresis | Add debouncing |
| Glitches | Combinational hazards | Register outputs |
| Wrong initial state | No reset | Add initialization |

## Related Tutorials

- [103 - PWM Basics](../103_pwm_basics/) - PWM control
- [704 - Java Blocks](../../7xx_scripting_automation/704_java_blocks/) - Code-based implementation
- [201 - Buck Converter](../../2xx_dcdc_converters/201_buck_converter/) - Test circuit

## References

1. Pressman, A. "Switching Power Supply Design" - Protection circuits
2. IEEE 1547 - Grid interconnection requirements (state machine for inverters)

## Circuit Files

> **Status:** Placeholder
> - `fsm_basic.ipes` - Simple on/off state machine
> - `fsm_softstart.ipes` - Soft-start implementation
> - `fsm_full.ipes` - Complete protection state machine

---
*Tutorial Version: 1.0 (Placeholder)*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
