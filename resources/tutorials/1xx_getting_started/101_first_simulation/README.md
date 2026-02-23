# Tutorial 101: Your First Simulation

## Overview

Welcome to GeckoCIRCUITS! This tutorial will guide you through opening the application, loading a circuit, running your first simulation, and viewing the results.

**Level:** Beginner (1/3)

**Duration:** 15-20 minutes

**Series:** Getting Started

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Launch GeckoCIRCUITS and understand the main interface
- [ ] Open an existing circuit file (.ipes)
- [ ] Run a simulation
- [ ] View and interpret waveforms in the oscilloscope (SCOPE)
- [ ] Save and export results

## Prerequisites

- GeckoCIRCUITS installed (see installation guide in main README)
- Java 21 runtime installed
- No prior circuit simulation experience required

## Materials

| File | Description |
|------|-------------|
| `ex_1.ipes` | Sample circuit for this tutorial |
| `GeckoCIRCUITS_beginners_tutorial.pdf` | Detailed step-by-step guide with screenshots |

## Quick Start Guide

### Step 1: Launch GeckoCIRCUITS

**Option A - Using launcher script (recommended):**
```bash
# Linux
./scripts/run-gecko-linux.sh

# Windows
scripts\run-gecko.bat

# macOS
./scripts/run-gecko-macos.sh
```

**Option B - Direct Java execution:**
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar
```

### Step 2: Explore the Interface

The GeckoCIRCUITS window has several key areas:

```
┌─────────────────────────────────────────────────────────┐
│  Menu Bar: File | Edit | Simulation | View | Help       │
├─────────────────────────────────────────────────────────┤
│  Toolbar: [Open] [Save] [Run] [Stop] [Zoom+/-]          │
├─────────────────┬───────────────────────────────────────┤
│                 │                                       │
│  Component      │         Schematic Editor              │
│  Library        │         (Circuit Canvas)              │
│                 │                                       │
│  - POWER        │                                       │
│  - CONTROL      │                                       │
│  - THERM        │                                       │
│  - SCOPE        │                                       │
│                 │                                       │
├─────────────────┴───────────────────────────────────────┤
│  Status Bar: Simulation progress, messages              │
└─────────────────────────────────────────────────────────┘
```

### Step 3: Open the Sample Circuit

1. Go to **File > Open** (or press Ctrl+O)
2. Navigate to: `resources/tutorials/1xx_getting_started/101_first_simulation/`
3. Select `ex_1.ipes`
4. Click **Open**

You should see a simple circuit appear on the canvas.

### Step 4: Run the Simulation

1. Click the **Run** button in the toolbar (or press F5)
2. Watch the simulation progress in the status bar
3. Wait for "Simulation Complete" message

### Step 5: View Results in the Scope

1. Double-click on the **SCOPE** component in the circuit
2. The oscilloscope window will open showing waveforms
3. Use the toolbar to:
   - **Zoom in/out** on time axis
   - **Auto-scale** to fit all signals
   - **Cursor** to measure values

### Step 6: Save Your Work

- **File > Save** (Ctrl+S) - Save the circuit
- **File > Export** - Export waveforms as data files

## Understanding the Sample Circuit

The `ex_1.ipes` circuit contains:

| Component | Type | Description |
|-----------|------|-------------|
| Voltage Source | V.1 | AC or DC power supply |
| Resistor | R.1 | Load resistance |
| Inductor | L.1 | Reactive element |
| SCOPE | SCOPE.1 | Oscilloscope for viewing waveforms |

## Key Concepts

### Circuit Files (.ipes)

- GeckoCIRCUITS saves circuits in `.ipes` format
- Files are gzip-compressed for efficiency
- All component parameters and connections are stored

### Simulation Types

| Type | Description | Use Case |
|------|-------------|----------|
| Transient | Time-domain simulation | Most common, waveform analysis |
| Steady-state | Periodic steady-state | Efficiency, harmonic analysis |

### The Oscilloscope (SCOPE)

The SCOPE component is essential for viewing results:
- Add signals by connecting to circuit nodes
- Multiple channels (different colors)
- Measurement tools: cursors, FFT, RMS

## Checkpoint

At this point, you should have:
- [ ] Successfully launched GeckoCIRCUITS
- [ ] Opened `ex_1.ipes` circuit
- [ ] Run a simulation
- [ ] Viewed waveforms in the oscilloscope

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Application won't start | Verify Java 21 is installed: `java -version` |
| Circuit file won't open | Ensure file exists and is not corrupted |
| Simulation stuck | Check for circuit errors (short circuits, etc.) |
| No waveforms in scope | Ensure signals are connected to scope inputs |

## Exercises

### Exercise 1: Change a Parameter
1. Double-click on the resistor R.1
2. Change its value from the default to 100 Ω
3. Re-run the simulation
4. **Observe:** How does the waveform change?

### Exercise 2: Add a Measurement
1. Right-click on a wire in the circuit
2. Connect it to an unused SCOPE channel
3. Re-run the simulation
4. **Compare:** View multiple signals on the same plot

### Exercise 3: Explore the Menu
1. Browse the **Simulation** menu options
2. Try changing the simulation time
3. Explore **View** options for zoom and grid

## Summary

In this tutorial, you learned:
1. How to launch GeckoCIRCUITS
2. The layout of the main interface
3. How to open circuit files
4. Running simulations
5. Viewing results in the oscilloscope

## Next Steps

Continue your learning with:
- **Next Tutorial:** [102 - Basic Circuits](../102_basic_circuits/) - Build circuits from scratch
- **Reference:** [GeckoCIRCUITS User Manual](../../docs/)

## Additional Resources

- `GeckoCIRCUITS_beginners_tutorial.pdf` - Detailed guide with screenshots
- Online help: **Help > User Manual**

---
*Tutorial Version: 1.0*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
