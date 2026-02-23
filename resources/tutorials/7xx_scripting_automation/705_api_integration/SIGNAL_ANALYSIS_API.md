# Signal Analysis API - Quick Reference

## Overview

The GeckoCIRCUITS REST API now supports comprehensive signal analysis on completed simulations, exposing the powerful signal processing capabilities of the legacy simulation engine.

**Base URL:** `http://localhost:8080/api/simulations/{simulationId}/analysis`

**Prerequisites:** Simulation must be in `COMPLETED` status before analysis can be performed.

---

## Endpoints

### 1. Get RMS (Root Mean Square)

Calculate the RMS value of a signal over a time window.

```http
GET /api/simulations/{simulationId}/analysis/rms
```

**Query Parameters:**
- `signal` (required): Name of the signal to analyze (e.g., "Vout", "Iout")
- `startTime` (optional): Start time in seconds (default: 0.0)
- `endTime` (optional): End time in seconds (default: simulation end)

**Example Request:**
```bash
curl "http://localhost:8080/api/simulations/abc123/analysis/rms?signal=Vout&startTime=0.01&endTime=0.1"
```

**Example Response:**
```json
{
  "value": 12.5,
  "unit": "V",
  "analysisType": "RMS",
  "signalName": "Vout",
  "startTime": 0.01,
  "endTime": 0.1,
  "dataPoints": 9000
}
```

---

### 2. Get THD (Total Harmonic Distortion)

Calculate the Total Harmonic Distortion as a percentage.

```http
GET /api/simulations/{simulationId}/analysis/thd
```

**Query Parameters:**
- `signal` (required): Signal name
- `startTime` (optional): Start time in seconds
- `endTime` (optional): End time in seconds

**Example Request:**
```bash
curl "http://localhost:8080/api/simulations/abc123/analysis/thd?signal=Vout"
```

**Example Response:**
```json
{
  "value": 2.5,
  "unit": "%",
  "analysisType": "THD",
  "signalName": "Vout",
  "startTime": null,
  "endTime": null
}
```

---

### 3. Get FFT (Fast Fourier Transform)

Perform FFT analysis with harmonic decomposition.

```http
GET /api/simulations/{simulationId}/analysis/fft
```

**Query Parameters:**
- `signal` (required): Signal name
- `harmonics` (optional): Number of harmonics to calculate (default: 50)
- `startTime` (optional): Start time in seconds
- `endTime` (optional): End time in seconds

**Example Request:**
```bash
curl "http://localhost:8080/api/simulations/abc123/analysis/fft?signal=Vout&harmonics=100"
```

**Example Response:**
```json
{
  "signalName": "Vout",
  "frequencies": [0.0, 50.0, 100.0, 150.0, ...],
  "magnitudes": [12.0, 5.5, 2.1, 0.8, ...],
  "phases": [0.0, 1.57, 3.14, 4.71, ...],
  "harmonics": [12.0, 5.5, 2.1, ...],
  "harmonicCount": 100,
  "fundamentalFrequency": 50.0,
  "startTime": null,
  "endTime": null,
  "sampleRate": 100000.0
}
```

**Field Descriptions:**
- `frequencies`: Frequency values in Hz
- `magnitudes`: Absolute magnitude values
- `phases`: Phase values in radians
- `harmonics`: DC component + first N harmonics
- `fundamentalFrequency`: First non-DC frequency component

---

### 4. Get Comprehensive Statistics

Get all signal statistics in one call (min, max, avg, RMS, THD, ripple, etc.).

```http
GET /api/simulations/{simulationId}/analysis/stats
```

**Query Parameters:**
- `signal` (required): Signal name
- `startTime` (optional): Start time in seconds
- `endTime` (optional): End time in seconds

**Example Request:**
```bash
curl "http://localhost:8080/api/simulations/abc123/analysis/stats?signal=Vout"
```

**Example Response:**
```json
{
  "signalName": "Vout",
  "min": 11.5,
  "max": 12.5,
  "average": 12.0,
  "rms": 12.02,
  "thd": 2.5,
  "ripple": 1.0,
  "shapeFactor": 1.002,
  "klirr": 0.025,
  "standardDeviation": 0.3,
  "startTime": null,
  "endTime": null,
  "dataPoints": 10000,
  "unit": "V"
}
```

**Derived Metrics:**
- `ripple`: Peak-to-peak variation (max - min)
- `shapeFactor`: RMS / Average ratio
- `klirr`: German term for distortion factor

---

### 5. List Available Signals

Get a list of all signals available for analysis from the simulation.

```http
GET /api/simulations/{simulationId}/analysis/signals
```

**Example Request:**
```bash
curl "http://localhost:8080/api/simulations/abc123/analysis/signals"
```

**Example Response:**
```json
{
  "simulationId": "abc123",
  "signals": ["Vout", "Iout", "Vin", "IL1"],
  "count": 4
}
```

---

## Complete Workflow Example

### Step 1: Upload and Run Simulation

```bash
# Upload .ipes file
curl -X POST http://localhost:8080/api/simulations/run-ipes \
  -F "file=@buck-converter.ipes" \
  -F "timeStep=0.00001" \
  -F "duration=0.1"

# Response:
{
  "simulationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Simulation queued successfully"
}
```

### Step 2: Poll for Completion

```bash
# Check status
curl http://localhost:8080/api/simulations/550e8400-e29b-41d4-a716-446655440000/status

# Response when completed:
{
  "simulationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "COMPLETED",
  "progress": 100,
  "executionTimeMs": 2345
}
```

### Step 3: Perform Signal Analysis

```bash
# Get available signals
curl http://localhost:8080/api/simulations/550e8400-e29b-41d4-a716-446655440000/analysis/signals

# Get RMS of output voltage
curl "http://localhost:8080/api/simulations/550e8400-e29b-41d4-a716-446655440000/analysis/rms?signal=Vout"

# Get THD
curl "http://localhost:8080/api/simulations/550e8400-e29b-41d4-a716-446655440000/analysis/thd?signal=Vout"

# Get FFT with 50 harmonics
curl "http://localhost:8080/api/simulations/550e8400-e29b-41d4-a716-446655440000/analysis/fft?signal=Vout&harmonics=50"

# Get all statistics at once
curl "http://localhost:8080/api/simulations/550e8400-e29b-41d4-a716-446655440000/analysis/stats?signal=Vout"
```

---

## Python Example

```python
import requests
import time

# Base URL
base_url = "http://localhost:8080/api/simulations"

# 1. Upload and start simulation
with open("buck-converter.ipes", "rb") as f:
    files = {"file": f}
    data = {"timeStep": 0.00001, "duration": 0.1}
    response = requests.post(f"{base_url}/run-ipes", files=files, data=data)
    sim_id = response.json()["simulationId"]

# 2. Wait for completion
while True:
    status = requests.get(f"{base_url}/{sim_id}/status").json()
    if status["status"] == "COMPLETED":
        break
    time.sleep(0.5)

# 3. Get available signals
signals_resp = requests.get(f"{base_url}/{sim_id}/analysis/signals")
signals = signals_resp.json()["signals"]
print(f"Available signals: {signals}")

# 4. Analyze first signal
signal_name = signals[0]

# Get RMS
rms = requests.get(f"{base_url}/{sim_id}/analysis/rms",
                   params={"signal": signal_name}).json()
print(f"RMS of {signal_name}: {rms['value']} {rms['unit']}")

# Get THD
thd = requests.get(f"{base_url}/{sim_id}/analysis/thd",
                   params={"signal": signal_name}).json()
print(f"THD of {signal_name}: {thd['value']}%")

# Get FFT
fft = requests.get(f"{base_url}/{sim_id}/analysis/fft",
                   params={"signal": signal_name, "harmonics": 50}).json()
print(f"Fundamental frequency: {fft['fundamentalFrequency']} Hz")
print(f"First 5 harmonics: {fft['harmonics'][:5]}")

# Get comprehensive stats
stats = requests.get(f"{base_url}/{sim_id}/analysis/stats",
                     params={"signal": signal_name}).json()
print(f"\nSignal Statistics for {signal_name}:")
print(f"  Min: {stats['min']}")
print(f"  Max: {stats['max']}")
print(f"  Avg: {stats['average']}")
print(f"  RMS: {stats['rms']}")
print(f"  THD: {stats['thd']}%")
print(f"  Ripple: {stats['ripple']}")
```

---

## Error Handling

### Simulation Not Found (404)
```json
{
  "error": "Simulation not found: invalid-id"
}
```

### Simulation Not Completed (400)
```json
{
  "error": "Simulation not completed. Current status: RUNNING. Analysis is only available for completed simulations."
}
```

### Bridge Not Available (500)
```json
{
  "error": "Simulation bridge not available"
}
```

### Missing Required Parameter (400)
```json
{
  "timestamp": "2026-01-25T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Required request parameter 'signal' for method parameter type String is not present"
}
```

---

## Performance Notes

- **Analysis Speed:** < 100ms for typical signals (target)
- **FFT Computation:** Scales with data points and harmonic count
- **Cached Results:** Bridge and simulation data remain available after completion
- **Concurrent Analysis:** Multiple analysis requests can run in parallel

---

## Limitations

1. **Analysis Availability:** Only available for `COMPLETED` simulations
2. **Signal Names:** Must match exact scope/signal names from the .ipes file
3. **Time Windows:** Must be within the simulation time range
4. **Legacy Dependency:** Requires GeckoCustom methods to be available in classpath

---

## Testing

### Unit Tests
```bash
mvn test -Dtest=SignalAnalysisControllerTest
```

### Integration Tests
```bash
mvn test -Dtest=SignalAnalysisIntegrationTest
```

---

## Next Steps

Coming in Phase 1:
- ✅ Signal analysis endpoints (COMPLETED)
- ⏳ WebSocket real-time updates
- ⏳ Interactive simulation (step-by-step execution)
- ⏳ Circuit validation endpoint

Coming in Phase 2:
- Hierarchical circuit navigation
- Scope management API
- State variable access

Coming in Phase 3:
- Python PLECS compatibility adapter
- Parameter expressions
- Enhanced documentation
