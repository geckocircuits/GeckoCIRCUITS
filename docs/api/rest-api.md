---
title: REST API
description: HTTP API for remote simulation control
---

# REST API

The `gecko-rest-api` module provides an HTTP/REST interface for controlling GeckoCIRCUITS programmatically.

!!! note "Module Status"
    The REST API is built on `gecko-simulation-core` and Spring Boot 3.2. It enables headless simulation without GUI dependencies.

## Architecture

```
┌──────────────┐     HTTP      ┌──────────────────┐
│   Python     │◄─────────────►│  gecko-rest-api   │
│   Browser    │  port 8080    │  (Spring Boot)    │
│   curl       │              │                   │
└──────────────┘              └──────────────────┘
```

## Starting the Server

```bash
cd gecko-rest-api
mvn spring-boot:run
```

Server starts on `http://localhost:8080`.

## Endpoints

### Health Check

```
GET /api/health
```

```json
{"status": "ok", "version": "1.0"}
```

### Run Simulation

```
POST /api/simulation/run
Content-Type: application/json
```

```json
{
  "circuitFile": "path/to/circuit.ipes",
  "parameters": {
    "PWM.1.dutyCycle": 0.5,
    "R.1.resistance": 10.0
  },
  "simulationTime": 0.01,
  "timeStep": 1e-7
}
```

Response:

```json
{
  "status": "completed",
  "duration_ms": 1250,
  "measurements": {
    "SCOPE.1.ch1_avg": 12.05,
    "SCOPE.1.ch1_rms": 12.08
  }
}
```

### Get Waveform Data

```
GET /api/simulation/waveform?scope=SCOPE.1&channel=ch1
```

## Python Client Example

```python
import requests
import numpy as np

BASE_URL = "http://localhost:8080/api"

# Run a simulation
response = requests.post(f"{BASE_URL}/simulation/run", json={
    "circuitFile": "buck_simple.ipes",
    "parameters": {"PWM.1.dutyCycle": 0.5},
    "simulationTime": 0.001,
    "timeStep": 5e-8
})

result = response.json()
print(f"Vout: {result['measurements']['SCOPE.1.ch1_avg']:.2f} V")

# Parameter sweep
for D in np.linspace(0.1, 0.9, 9):
    resp = requests.post(f"{BASE_URL}/simulation/run", json={
        "circuitFile": "buck_simple.ipes",
        "parameters": {"PWM.1.dutyCycle": float(D)},
        "simulationTime": 0.001
    })
    vout = resp.json()["measurements"]["SCOPE.1.ch1_avg"]
    print(f"D={D:.1f}, Vout={vout:.2f}V")
```

## WebSocket Interface

For real-time data streaming:

```javascript
const ws = new WebSocket('ws://localhost:8080/ws/simulation');

ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log(`t=${data.time}, Vout=${data.values[0]}`);
};
```

A test client is available at `resources/tutorials/7xx_scripting_automation/705_api_integration/websocket-client.html`.

## See Also

- [Remote Interface (RMI)](remote-interface.md) - Java RMI integration
- [GeckoSCRIPT](geckoscript-ref.md) - Built-in scripting
- [Python Integration](../tutorials/scripting/python.md)
