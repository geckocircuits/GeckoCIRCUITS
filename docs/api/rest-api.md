---
title: REST API
description: HTTP API for remote simulation control
---

# REST API

!!! success "Live in v2.16.0+"
    The REST API is fully functional and running in production. This page documents the implemented endpoints and usage examples. Built on Spring Boot 3.2 with `gecko-simulation-core`, enabling headless simulation without GUI dependencies.

The REST API provides an HTTP/REST interface for controlling GeckoCIRCUITS programmatically. It is built on Spring Boot 3.2 and uses the `gecko-simulation-core` module for all simulation logic, enabling cloud deployment, CI/CD automation, and remote computation.

## Quick Start

Deploy and test the REST API with Docker:

```bash
# Start the API server (port 8080)
docker-compose up -d

# Check health
curl http://localhost:8080/actuator/health

# View API documentation
# Open http://localhost:8080/swagger-ui.html in your browser
```

## Architecture

```
┌──────────────┐     HTTP      ┌──────────────────┐
│   Python     │◄─────────────►│  gecko-rest-api   │
│   Browser    │  port 8080    │  (Spring Boot)    │
│   curl       │              │  Uses gecko-core  │
└──────────────┘              └──────────────────┘
```

## API Documentation

Full interactive documentation available at:

**[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)** (when running)

OpenAPI specification:

```
GET /v3/api-docs
```

## Live Endpoints

### Loss Calculation Endpoints

Calculate power loss for semiconductor devices.

**Switching Loss** (Voltage and Energy Scaling)

```bash
POST /api/v1/loss/switching
Content-Type: application/json
```

```json
{
  "voltage": 400,
  "current": 50,
  "frequency": 20000,
  "temperature": 125,
  "referenceVoltage": 600,
  "referenceCurrent": 100,
  "referenceFrequency": 20000,
  "referenceTemperature": 125,
  "e_on_ref": 5.2e-5,
  "e_off_ref": 3.8e-5
}
```

**Conduction Loss** (Resistance Model)

```bash
POST /api/v1/loss/conduction
```

**Detailed Loss** (Temperature-Dependent Interpolation)

```bash
POST /api/v1/loss/detailed
```

### Circuit File Endpoints

**Parse Circuit File**

```bash
POST /api/v1/circuits/parse
Content-Type: multipart/form-data

curl -F "file=@circuit.ipes" http://localhost:8080/api/v1/circuits/parse
```

**Get Circuit Metadata**

```bash
GET /api/v1/circuits/{circuitId}/info
```

**List Components**

```bash
GET /api/v1/circuits/{circuitId}/components
```

**Validate Circuit**

```bash
POST /api/v1/circuits/{circuitId}/validate
```

**List All Circuits**

```bash
GET /api/v1/circuits
```

**Delete Circuit**

```bash
DELETE /api/v1/circuits/{circuitId}
```

### Simulation Endpoints

**Submit Simulation** (Async)

```bash
POST /api/v1/simulations
Content-Type: application/json
```

Request:
```json
{
  "circuitFile": "base64-encoded-.ipes-data",
  "simulationTime": 0.01,
  "timeStep": 1e-7,
  "solverType": "backward-euler",
  "parameters": {
    "PWM.1.dutyCycle": 0.5,
    "R.1.resistance": 10.0
  }
}
```

**Solver Types:**
- `backward-euler` - Implicit 1st order (stable, for stiff circuits)
- `trapezoidal` - Implicit 2nd order (balanced accuracy/stability)
- `gear-shichman` - Variable order (best for variable dynamics)

**Get Simulation Status and Results**

```bash
GET /api/v1/simulations/{simulationId}
```

**List All Simulations**

```bash
GET /api/v1/simulations
```

**Cancel Simulation**

```bash
DELETE /api/v1/simulations/{simulationId}
```

**Pause Simulation**

```bash
POST /api/v1/simulations/{simulationId}/pause
```

**Resume Simulation**

```bash
POST /api/v1/simulations/{simulationId}/resume
```

**Get Detailed Progress**

```bash
GET /api/v1/simulations/{simulationId}/progress
```

Response fields:
- `preCalcProgress` - Initial condition calculation %
- `mainSimProgress` - Main transient simulation %
- `currentStep` - Current time step number
- `totalSteps` - Total steps to complete
- `estimatedRemainingMs` - Estimated time remaining

**Export Results**

```bash
GET /api/v1/simulations/{simulationId}/export
```

## Usage Examples

### Python Client

```python
import requests
import numpy as np

BASE_URL = "http://localhost:8080/api/v1"

# Submit simulation
response = requests.post(f"{BASE_URL}/simulations", json={
    "circuitFile": "base64_encoded_data",
    "parameters": {"PWM.1.dutyCycle": 0.5},
    "simulationTime": 0.001,
    "timeStep": 5e-8,
    "solverType": "trapezoidal"
})

sim_id = response.json()["simulationId"]
print(f"Simulation {sim_id} submitted")

# Poll for completion
import time
while True:
    status = requests.get(f"{BASE_URL}/simulations/{sim_id}").json()
    if status["status"] in ["completed", "failed"]:
        break
    print(f"Progress: {status['progress']['mainSimProgress']}%")
    time.sleep(1)

# Get results
results = status["results"]["measurements"]
print(f"Vout: {results['SCOPE.1.ch1_avg']:.2f} V")
```

### Bash / curl

```bash
# Loss calculation
curl -X POST http://localhost:8080/api/v1/loss/conduction \
  -H "Content-Type: application/json" \
  -d '{"voltage": 400, "current": 50, "temperature": 125}'

# Upload circuit
curl -F "file=@circuit.ipes" http://localhost:8080/api/v1/circuits/parse

# Submit simulation
curl -X POST http://localhost:8080/api/v1/simulations \
  -H "Content-Type: application/json" \
  -d '{"circuitFile": "circuit_base64", "simulationTime": 0.01, "timeStep": 1e-7}'
```

## Request / Response Models

### SimulationRequest

| Field | Type | Description |
|-------|------|-------------|
| `circuitFile` | string | Base64-encoded .ipes file or circuit ID |
| `simulationTime` | number | Total simulation time (seconds) |
| `timeStep` | number | Integration time step (seconds) |
| `solverType` | enum | backward-euler, trapezoidal, gear-shichman |
| `parameters` | map | Parameter overrides |

### ProgressDetails

| Field | Type | Description |
|-------|------|-------------|
| `preCalcProgress` | number | Initial condition solver progress (0-100%) |
| `mainSimProgress` | number | Main transient simulation progress (0-100%) |
| `currentStep` | number | Current simulation step number |
| `totalSteps` | number | Total steps for complete simulation |
| `estimatedRemainingMs` | number | Estimated milliseconds until completion |

## Deployment

### Docker (Recommended)

```bash
# Using docker-compose
docker-compose up -d

# Manual Docker run
docker run -p 8080:8080 gecko-rest-api:latest
```

## See Also

- [Remote Interface (RMI)](remote-interface.md) - Java RMI integration
- [GeckoSCRIPT](geckoscript-ref.md) - Built-in scripting
- [Python Integration](../tutorials/scripting/python.md)
- [Docker Setup](../getting-started/installation.md#docker)
