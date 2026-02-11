---
title: User Interface
description: Guide to the GeckoCIRCUITS editor and workspace
---

# User Interface Guide

Overview of the GeckoCIRCUITS workspace, toolbar, and editor.

## Main Window Layout

The application window has four main areas:

```
┌──────────────────────────────────────────────┐
│  Menu Bar                                     │
├──────────────────────────────────────────────┤
│  Toolbar (Run, Stop, Settings, Zoom)          │
├──────────────────────────────────────────────┤
│                                               │
│                                               │
│            Schematic Editor                    │
│          (Circuit Drawing Area)                │
│                                               │
│                                               │
├──────────────────────────────────────────────┤
│  Status Bar                                   │
└──────────────────────────────────────────────┘
```

## Menu Bar

### File Menu

| Action | Shortcut | Description |
|--------|----------|-------------|
| New | ++ctrl+n++ | Create empty circuit |
| Open | ++ctrl+o++ | Open .ipes file |
| Save | ++ctrl+s++ | Save current circuit |
| Save As | ++ctrl+shift+s++ | Save with new name |
| Export SVG | | Export schematic as SVG image |

### Edit Menu

| Action | Shortcut | Description |
|--------|----------|-------------|
| Undo | ++ctrl+z++ | Undo last action |
| Redo | ++ctrl+y++ | Redo undone action |
| Copy | ++ctrl+c++ | Copy selected components |
| Paste | ++ctrl+v++ | Paste copied components |
| Delete | ++delete++ | Delete selection |
| Select All | ++ctrl+a++ | Select all components |

### Simulation Menu

| Action | Shortcut | Description |
|--------|----------|-------------|
| Run | ++f5++ | Start simulation |
| Stop | | Stop running simulation |
| Settings | | Open simulation parameters |

## Toolbar

The toolbar provides quick access to common actions:

| Button | Action |
|--------|--------|
| **Run** (play icon) | Start simulation |
| **Stop** (stop icon) | Stop simulation |
| **Settings** (gear icon) | Simulation parameters |
| **Zoom In/Out** | Adjust view scale |
| **Fit** | Fit circuit to window |

## Schematic Editor

The main drawing area where you build and edit circuits.

### Navigation

| Action | Control |
|--------|---------|
| Pan | Middle-click + drag, or scroll |
| Zoom | Scroll wheel |
| Fit to window | Double-click empty area |

### Selecting Components

| Action | How |
|--------|-----|
| Select one | Left-click on component |
| Select multiple | Left-click + drag (selection box) |
| Add to selection | ++shift++ + click |
| Deselect all | Click empty area |

### Moving Components

1. Select component(s)
2. Left-click and drag to new position
3. Components snap to grid

### Editing Parameters

- **Double-click** a component to open its parameter dialog
- Change values and click OK
- Parameters take effect on next simulation run

## Component Palette

Components are organized by domain:

### Electrical Components

| Category | Components |
|----------|-----------|
| **Sources** | DC voltage, DC current, AC voltage, AC current |
| **Passive** | Resistor, Inductor, Capacitor |
| **Semiconductors** | Diode, MOSFET, IGBT, Thyristor |
| **Transformers** | Ideal transformer, coupled inductor |
| **Measurement** | Voltage probe, Current probe |

### Control Components

| Category | Components |
|----------|-----------|
| **Sources** | Constant, Sine, PWM, Signal generator |
| **Math** | Gain, Sum, Multiply, Divide |
| **Controllers** | PI, PID, Integrator, Limiter |
| **Logic** | Comparator, AND, OR, Flip-flop |
| **Display** | SCOPE (oscilloscope) |

### Thermal Components

| Category | Components |
|----------|-----------|
| **Thermal** | Thermal resistance (Rth), Thermal capacitance (Cth) |
| **Sources** | Temperature source, Heat source |

## Wiring

### Connecting Components

1. Click an output terminal (connection point)
2. Route the wire by clicking intermediate points
3. Click the input terminal of the destination component

### Wire Rules

- Wires connect at **terminals** (small squares on components)
- A **junction dot** appears where three or more wires meet
- Crossing wires without a junction are **not connected**

## Scope (Oscilloscope)

The SCOPE is the primary visualization tool.

### Adding Scope Channels

1. Place a SCOPE block on the schematic
2. Connect signals to scope input terminals
3. Double-click scope to open the waveform viewer

### Scope Features

- Multiple channels with independent scaling
- Time-domain and frequency-domain views
- Cursors for precise measurement
- Data export to CSV

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| ++f5++ | Run simulation |
| ++ctrl+s++ | Save |
| ++ctrl+z++ | Undo |
| ++ctrl+y++ | Redo |
| ++ctrl+c++ / ++ctrl+v++ | Copy / Paste |
| ++delete++ | Delete selection |
| ++ctrl+a++ | Select all |
| ++escape++ | Cancel current action |

## Next Steps

- [First Simulation](first-simulation.md) - Run your first circuit
- [Building Circuits](building-circuits.md) - Create circuits from scratch
- [Quick Start](quickstart.md) - 5-minute getting started guide
