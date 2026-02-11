---
title: API Reference
description: GeckoCIRCUITS programming interfaces and automation
---

# API Reference

GeckoCIRCUITS provides multiple interfaces for automation, integration, and programmatic control.

## Available Interfaces

<div class="grid cards" markdown>

-   :material-remote:{ .lg .middle } **[Remote Interface](remote-interface.md)**

    ---

    Java RMI-based interface for MATLAB integration

    [:octicons-arrow-right-24: Documentation](remote-interface.md)

-   :material-api:{ .lg .middle } **[REST API](rest-api.md)**

    ---

    HTTP-based API for web integration and automation

    [:octicons-arrow-right-24: Documentation](rest-api.md)

-   :material-script:{ .lg .middle } **[GeckoSCRIPT](geckoscript-ref.md)**

    ---

    Built-in scripting language for automation

    [:octicons-arrow-right-24: Reference](geckoscript-ref.md)

</div>

## Interface Comparison

| Feature | Remote (RMI) | REST API | GeckoSCRIPT |
|---------|--------------|----------|-------------|
| Language | Java/MATLAB | Any | Built-in |
| Real-time | Yes | No | Yes |
| Network | Local/Remote | HTTP | Local |
| Complexity | Medium | Low | Low |
| Best for | MATLAB integration | Web apps | Quick automation |

## Quick Start

### GeckoSCRIPT (Built-in)

```javascript
// Parameter sweep example
for (D = 0.2; D <= 0.8; D += 0.1) {
    setParameter("PWM.1", "dutyCycle", D);
    runSimulation();
    Vout = getMeasurement("SCOPE.1", "ch1_avg");
    print("D=" + D + ", Vout=" + Vout);
}
```

### MATLAB (RMI)

```matlab
% Connect to GeckoCIRCUITS
gecko = GeckoRemoteInterface('localhost', 1099);

% Load and run
gecko.loadCircuit('buck.ipes');
gecko.setParameter('PWM.1', 'dutyCycle', 0.5);
gecko.runSimulation();
vout = gecko.getMeasurement('Vout');
```

### Python (REST API)

```python
import requests

# Run simulation
response = requests.post('http://localhost:8080/api/simulate', json={
    'circuit': 'buck.ipes',
    'parameters': {'D': 0.5}
})
results = response.json()
print(f"Vout = {results['Vout']}")
```

### cURL (REST API)

```bash
# Get simulation status
curl http://localhost:8080/api/status

# Run simulation
curl -X POST http://localhost:8080/api/simulate \
  -H "Content-Type: application/json" \
  -d '{"circuit": "buck.ipes", "time": 0.01}'
```

## Core API Methods

### Circuit Control

| Method | Description |
|--------|-------------|
| `loadCircuit(path)` | Load circuit file |
| `saveCircuit(path)` | Save current circuit |
| `runSimulation()` | Start simulation |
| `stopSimulation()` | Stop running simulation |
| `resetSimulation()` | Reset to initial state |

### Parameter Access

| Method | Description |
|--------|-------------|
| `setParameter(component, param, value)` | Set component parameter |
| `getParameter(component, param)` | Get component parameter |
| `setGlobalParameter(name, value)` | Set global variable |

### Data Retrieval

| Method | Description |
|--------|-------------|
| `getMeasurement(scope, channel)` | Get scope measurement |
| `getWaveform(scope, channel)` | Get time-series data |
| `exportData(path, format)` | Export to file |

## Related Tutorials

- [GeckoSCRIPT Basics](../tutorials/scripting/geckoscript.md)
- [MATLAB Integration](../tutorials/scripting/matlab.md)
- [Python Integration](../tutorials/scripting/python.md)
