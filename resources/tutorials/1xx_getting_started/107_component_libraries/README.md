# Tutorial 107: Component Libraries

## Overview

Learn to organize, create, and manage custom component libraries in GeckoCIRCUITS. Build reusable subcircuits and share designs across projects.

**Level:** Intermediate (2/3)

**Duration:** 30-40 minutes

**Series:** Getting Started

**Status:** Placeholder

## Learning Objectives

By the end of this tutorial, you will:
- [ ] Understand component library organization
- [ ] Create custom subcircuits and symbols
- [ ] Manage library paths and updates
- [ ] Share components across projects

## Prerequisites

- Complete [Tutorial 102: Basic Circuits](../102_basic_circuits/)
- Familiarity with GeckoCIRCUITS interface

## Component Library Concepts

### Library Hierarchy

```
GeckoCIRCUITS Libraries
├── Built-in Components
│   ├── POWER (R, L, C, Switches, Diodes)
│   ├── CONTROL (Gain, Sum, PI, Comparator)
│   ├── THERM (Thermal components)
│   └── SCOPE (Measurement)
├── User Libraries
│   ├── My_Converters/
│   ├── My_Controllers/
│   └── Project_Specific/
└── Shared Libraries
    └── Team_Components/
```

### Component Types

| Type | Description | Use Case |
|------|-------------|----------|
| Primitive | Basic element (R, L, C) | Building blocks |
| Subcircuit | Hierarchical block | Reusable designs |
| Masked | Subcircuit with custom dialog | User-friendly |
| Symbol | Custom schematic symbol | Visual clarity |

## Creating Custom Components

### Step 1: Design the Subcircuit

1. Create new schematic
2. Build circuit (e.g., buck converter power stage)
3. Define input/output terminals
4. Add parameters as variables

### Step 2: Define Interface

**Terminals:**
- Input ports (signals coming in)
- Output ports (signals going out)
- Power connections

**Parameters:**
- User-adjustable values
- Default values
- Valid ranges

### Step 3: Create Symbol (Optional)

1. Draw custom symbol shape
2. Place terminal locations
3. Add text labels

### Step 4: Save to Library

1. File > Save as Component
2. Choose library location
3. Add description and keywords

## Example: Buck Power Stage Component

### Internal Circuit

```
    Vin+ ───┬──[S]──┬──[L]──┬─── Vout+
            │       │       │
            │      [D]     [C]
            │       │       │
    Vin- ───┴───────┴───────┴─── Vout-
                │
              Gate (control input)
```

### Parameters

| Parameter | Default | Range | Description |
|-----------|---------|-------|-------------|
| L | 100μH | 1μH-10mH | Inductance |
| C | 100μF | 1μF-10mF | Capacitance |
| Ron_sw | 10mΩ | 1mΩ-1Ω | Switch on-resistance |
| Vf_diode | 0.5V | 0.3-1V | Diode forward voltage |

### Symbol

```
    ┌─────────────┐
    │   BUCK      │
Vin+│    ┌─┐     │Vout+
────┤    │▼│     ├────
    │    └─┘     │
Vin-│            │Vout-
────┤     ▽      ├────
    │   Gate     │
    └─────┬──────┘
          │
         ─┴─
```

## Library Organization

### Recommended Structure

```
~/.geckocircuits/libraries/
├── power_stages/
│   ├── buck.gsub
│   ├── boost.gsub
│   ├── full_bridge.gsub
│   └── README.md
├── controllers/
│   ├── pi_controller.gsub
│   ├── type2_compensator.gsub
│   └── README.md
├── sensors/
│   ├── current_sense.gsub
│   ├── voltage_divider.gsub
│   └── README.md
└── thermal/
    ├── heatsink_model.gsub
    └── README.md
```

### Naming Conventions

| Convention | Example | Use |
|------------|---------|-----|
| lowercase_underscore | buck_converter | File names |
| CamelCase | BuckConverter | Component names |
| PREFIX_name | PWR_buck | Category prefix |
| name_vX.X | buck_v1.2 | Version tracking |

## Using Libraries

### Adding Library Path

1. Edit > Preferences > Libraries
2. Add path to custom library folder
3. Restart GeckoCIRCUITS

### Inserting Components

1. View component browser
2. Navigate to library
3. Drag component to schematic
4. Set parameters in dialog

### Updating Components

When library component changes:
1. Projects using old version unaffected
2. Manual update: right-click > Update from library
3. Or: reload library and replace

## Best Practices

### Documentation

Each component should include:
- Description of function
- Input/output specifications
- Parameter descriptions with units
- Usage examples
- Version history

### Version Control

```
# Component: Buck Power Stage
# Version: 1.2
# Date: 2026-02
# Changes: Added ESR to capacitor model
```

### Testing

Before adding to library:
1. Test standalone operation
2. Verify all parameters work
3. Check edge cases
4. Document known limitations

## Sharing Libraries

### Export

1. Package library folder as ZIP
2. Include README with dependencies
3. List compatible GeckoCIRCUITS versions

### Import

1. Extract to libraries folder
2. Add path in preferences
3. Restart application

### Team Collaboration

- Store libraries in shared network folder
- Use version control (Git) for changes
- Maintain changelog

## Exercises

### Exercise 1: Create Simple Component
1. Build RC low-pass filter
2. Define R and C as parameters
3. Save as component

### Exercise 2: Masked Subcircuit
1. Create buck power stage
2. Add parameter dialog
3. Test parameter variations

### Exercise 3: Library Organization
1. Create library folder structure
2. Move existing subcircuits to library
3. Document each component

### Exercise 4: Share with Team
1. Package library as ZIP
2. Write installation instructions
3. Create usage example

## Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Component not found | Wrong library path | Check preferences |
| Parameters don't update | Cached old version | Clear cache, reload |
| Symbol mismatch | Edited internal circuit | Regenerate symbol |
| Missing dependencies | Uses other components | Include all dependencies |

## Related Tutorials

- [102 - Basic Circuits](../102_basic_circuits/) - Building circuits
- [704 - Java Blocks](../../7xx_scripting_automation/704_java_blocks/) - Custom code components

## Circuit Files

> **Status:** Placeholder
> - `example_buck_stage.gsub` - Buck power stage component
> - `example_pi_controller.gsub` - PI controller component
> - `example_library/` - Sample library structure

---
*Tutorial Version: 1.0 (Placeholder)*
*Last updated: 2026-02*
*Compatible with GeckoCIRCUITS v1.0+*
