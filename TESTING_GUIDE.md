# GeckoCIRCUITS Testing Guide
**Version:** 2.0.0-SNAPSHOT
**Last Updated:** January 25, 2026

---

## 🧪 Testing Checklist - Phase 1 Features

### ✅ Pre-requisites
- [ ] Java 21+ installed (`java -version`)
- [ ] Python 3.x installed (for Python examples)
- [ ] curl installed (for API testing)
- [ ] Modern browser (for WebSocket client)
- [ ] Compiled JARs present in `target/` directories

---

## 1. Signal Analysis API Testing

### Step 1.1: Start the REST API Server

```bash
# From project root
cd gecko-rest-api
java -jar target/gecko-rest-api-2.0.0-SNAPSHOT.jar

# OR using Maven (if available)
mvn spring-boot:run

# Server should start on http://localhost:8080
# Look for: "Started GeckoRestApiApplication"
```

### Step 1.2: Test Health Endpoint

```bash
# Basic health check
curl http://localhost:8080/api/simulations/health

# Expected output:
{
  "status": "UP",
  "service": "GeckoCIRCUITS Remote Solver",
  "version": "2.0.0-SNAPSHOT"
}
```

**✅ Pass Criteria:** Returns HTTP 200 with status "UP"

### Step 1.3: Upload Circuit and Start Simulation

```bash
# Upload test circuit
curl -X POST http://localhost:8080/api/simulations/run-ipes \
  -F "file=@examples/api-testing/test-circuits/resistor-divider.ipes" \
  -F "timeStep=0.00001" \
  -F "duration=0.01"

# Expected output:
{
  "simulationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "message": "Simulation queued successfully",
  "fileName": "resistor-divider.ipes"
}
```

**✅ Pass Criteria:**
- Returns HTTP 202 (Accepted)
- Response contains simulationId
- Status is "PENDING"

**Save simulationId for next steps!**

### Step 1.4: Poll Simulation Status

```bash
# Replace {SIM_ID} with actual simulationId from previous step
export SIM_ID="550e8400-e29b-41d4-a716-446655440000"

curl http://localhost:8080/api/simulations/${SIM_ID}/status

# While running:
{
  "simulationId": "...",
  "status": "RUNNING",
  "progress": 45.5,
  "elapsedTimeMs": 1234
}

# When completed:
{
  "simulationId": "...",
  "status": "COMPLETED",
  "progress": 100,
  "executionTimeMs": 2345
}
```

**✅ Pass Criteria:**
- Status transitions: PENDING → RUNNING → COMPLETED
- Progress goes from 0 to 100
- No errors in response

### Step 1.5: Test Signal Analysis - Get Available Signals

```bash
curl http://localhost:8080/api/simulations/${SIM_ID}/analysis/signals

# Expected output:
{
  "simulationId": "...",
  "signals": ["Vout", "Iout", "Vin"],
  "count": 3
}
```

**✅ Pass Criteria:**
- Returns list of signals
- Count matches array length
- HTTP 200 status

### Step 1.6: Test RMS Analysis

```bash
# Get RMS of first signal
curl "http://localhost:8080/api/simulations/${SIM_ID}/analysis/rms?signal=Vout"

# Expected output:
{
  "value": 12.5,
  "unit": "V",
  "analysisType": "RMS",
  "signalName": "Vout",
  "startTime": null,
  "endTime": null
}
```

**✅ Pass Criteria:**
- Returns numeric value
- Analysis type is "RMS"
- Signal name matches request

### Step 1.7: Test RMS with Time Window

```bash
curl "http://localhost:8080/api/simulations/${SIM_ID}/analysis/rms?signal=Vout&startTime=0.002&endTime=0.008"

# Expected output:
{
  "value": 12.48,
  "unit": "V",
  "analysisType": "RMS",
  "signalName": "Vout",
  "startTime": 0.002,
  "endTime": 0.008
}
```

**✅ Pass Criteria:**
- Returns RMS for specified window
- startTime and endTime match request
- Value may differ from full-range RMS

### Step 1.8: Test THD Analysis

```bash
curl "http://localhost:8080/api/simulations/${SIM_ID}/analysis/thd?signal=Vout"

# Expected output:
{
  "value": 2.5,
  "unit": "%",
  "analysisType": "THD",
  "signalName": "Vout"
}
```

**✅ Pass Criteria:**
- Returns THD percentage (0-100%)
- Unit is "%"
- HTTP 200 status

### Step 1.9: Test FFT Analysis

```bash
curl "http://localhost:8080/api/simulations/${SIM_ID}/analysis/fft?signal=Vout&harmonics=50"

# Expected output:
{
  "signalName": "Vout",
  "frequencies": [0.0, 50.0, 100.0, 150.0, ...],
  "magnitudes": [12.0, 5.5, 2.1, 0.8, ...],
  "phases": [0.0, 1.57, 3.14, 4.71, ...],
  "harmonics": [12.0, 5.5, 2.1, ...],
  "harmonicCount": 50,
  "fundamentalFrequency": 50.0
}
```

**✅ Pass Criteria:**
- Returns frequency array
- Magnitudes array same length as frequencies
- Harmonics count matches request
- Fundamental frequency detected

### Step 1.10: Test Comprehensive Stats

```bash
curl "http://localhost:8080/api/simulations/${SIM_ID}/analysis/stats?signal=Vout"

# Expected output:
{
  "signalName": "Vout",
  "min": 11.5,
  "max": 12.5,
  "average": 12.0,
  "rms": 12.02,
  "thd": 2.5,
  "ripple": 1.0,
  "shapeFactor": 1.002
}
```

**✅ Pass Criteria:**
- All statistics present (min, max, avg, rms, thd)
- min ≤ avg ≤ max
- Ripple = max - min
- Shape factor = rms / avg

### Step 1.11: Test Error Cases

```bash
# Test 1: Non-existent simulation
curl http://localhost:8080/api/simulations/invalid-id/analysis/rms?signal=Vout
# Expected: HTTP 400, error message "Simulation not found"

# Test 2: Analysis on running simulation (start new sim first)
# Upload new simulation
NEW_SIM=$(curl -s -X POST http://localhost:8080/api/simulations/run-ipes \
  -F "file=@examples/api-testing/test-circuits/buck-converter.ipes" | \
  grep -o '"simulationId":"[^"]*"' | cut -d'"' -f4)

# Try to analyze immediately (while PENDING/RUNNING)
curl "http://localhost:8080/api/simulations/${NEW_SIM}/analysis/rms?signal=Vout"
# Expected: HTTP 400, error message "not completed"

# Test 3: Missing signal parameter
curl "http://localhost:8080/api/simulations/${SIM_ID}/analysis/rms"
# Expected: HTTP 400, missing parameter error
```

**✅ Pass Criteria:**
- All error cases return appropriate HTTP status (400/404)
- Error messages are clear and actionable

---

## 2. WebSocket Real-Time Updates Testing

### Step 2.1: Open WebSocket Test Client

```bash
# Open in browser (Chrome, Firefox, Edge)
# From project root:
firefox examples/api-testing/websocket-client.html

# Or via file:// URL:
# file:///home/tinix/claude_wsl/GeckoCIRCUITS/examples/api-testing/websocket-client.html
```

### Step 2.2: Configure WebSocket Client

In the browser:
1. **WebSocket URL:** `http://localhost:8080/ws` (should be pre-filled)
2. **Simulation ID:** Enter a new simulation ID (from next step)

### Step 2.3: Start Simulation and Monitor via WebSocket

**Terminal 1:** Start a long simulation
```bash
curl -X POST http://localhost:8080/api/simulations/run-ipes \
  -F "file=@examples/api-testing/test-circuits/buck-converter.ipes" \
  -F "duration=1.0"

# Note the simulationId from response
```

**Browser:**
1. Paste the simulationId into the WebSocket client
2. Click "Connect"
3. Observe real-time progress updates

**✅ Pass Criteria:**
- Connection status changes to "Connected" (green)
- Progress bar updates in real-time (every ~100ms)
- Log shows progress updates: "PROGRESS: RUNNING - 10.0%", "20.0%", etc.
- Final update shows "STATUS_CHANGE: COMPLETED - 100.0%"
- No errors in browser console

### Step 2.4: Test WebSocket Auto-Connect via URL

```bash
# Open with simulation ID in URL
firefox "examples/api-testing/websocket-client.html?simulationId=${SIM_ID}"
```

**✅ Pass Criteria:**
- Client auto-fills simulation ID
- Auto-connects after 500ms
- Subscribes successfully

### Step 2.5: Test WebSocket Throttling

Start a very long simulation (10+ seconds) and observe:
- Updates appear at most 10 times per second
- No flooding of messages
- Progress bar animates smoothly

**✅ Pass Criteria:**
- Max update rate ~10 Hz (confirmed in logs)
- No performance degradation in browser

---

## 3. Integration Testing (Automated)

### Step 3.1: Run Unit Tests

```bash
# If Maven is available:
cd gecko-rest-api
mvn test -Dtest=SignalAnalysisControllerTest

# Expected output:
# Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
```

**✅ Pass Criteria:**
- All 15 tests pass
- No compilation errors
- Coverage >80%

### Step 3.2: Run Integration Tests

```bash
# Make sure REST API is running first!
# Terminal 1: Start API
cd gecko-rest-api
java -jar target/gecko-rest-api-2.0.0-SNAPSHOT.jar

# Terminal 2: Run integration tests
cd gecko-rest-api
mvn test -Dtest=SignalAnalysisIntegrationTest

# Expected output:
# Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
```

**✅ Pass Criteria:**
- All 7 integration tests pass
- Real simulation completes successfully
- Signal analysis results validated

---

## 4. Python Client Testing

### Step 4.1: Test Python Signal Analysis Script

Create `test_signal_analysis.py`:

```python
import requests
import time

base_url = "http://localhost:8080/api/simulations"

# 1. Upload circuit
print("Uploading circuit...")
with open("examples/api-testing/test-circuits/resistor-divider.ipes", "rb") as f:
    response = requests.post(f"{base_url}/run-ipes",
                            files={"file": f},
                            data={"timeStep": 0.00001, "duration": 0.01})
    sim_id = response.json()["simulationId"]
    print(f"Simulation ID: {sim_id}")

# 2. Wait for completion
print("Waiting for simulation...")
while True:
    status = requests.get(f"{base_url}/{sim_id}/status").json()
    if status["status"] == "COMPLETED":
        break
    print(f"  Progress: {status['progress']:.1f}%")
    time.sleep(0.5)

print("Simulation completed!")

# 3. Get available signals
signals_resp = requests.get(f"{base_url}/{sim_id}/analysis/signals")
signals = signals_resp.json()["signals"]
print(f"\nAvailable signals: {signals}")

# 4. Analyze each signal
for signal in signals:
    stats = requests.get(f"{base_url}/{sim_id}/analysis/stats",
                        params={"signal": signal}).json()
    print(f"\n{signal} Statistics:")
    print(f"  Min: {stats['min']:.4f}")
    print(f"  Max: {stats['max']:.4f}")
    print(f"  Avg: {stats['average']:.4f}")
    print(f"  RMS: {stats['rms']:.4f}")
    if stats.get('thd'):
        print(f"  THD: {stats['thd']:.2f}%")

# 5. Get FFT for first signal
print(f"\n{signals[0]} FFT Analysis:")
fft = requests.get(f"{base_url}/{sim_id}/analysis/fft",
                   params={"signal": signals[0], "harmonics": 10}).json()
print(f"  Fundamental frequency: {fft.get('fundamentalFrequency', 'N/A')} Hz")
print(f"  First 5 harmonics: {fft['harmonics'][:5]}")

print("\n✅ All tests passed!")
```

Run the script:
```bash
python3 test_signal_analysis.py
```

**✅ Pass Criteria:**
- Script runs without errors
- All signals analyzed successfully
- FFT data retrieved
- Output shows "✅ All tests passed!"

---

## 5. Performance Testing

### Step 5.1: Test Signal Analysis Speed

```bash
# Test RMS calculation speed
time curl -s "http://localhost:8080/api/simulations/${SIM_ID}/analysis/rms?signal=Vout" > /dev/null

# Expected: < 100ms
```

**✅ Pass Criteria:**
- RMS analysis: < 100ms
- THD analysis: < 100ms
- FFT analysis: < 200ms
- Stats analysis: < 150ms

### Step 5.2: Test WebSocket Latency

In WebSocket client, measure time between simulation start and first update:
- Should be < 50ms for first update
- Subsequent updates < 20ms apart (throttled to 100ms)

**✅ Pass Criteria:**
- WebSocket latency < 50ms
- No missed status changes

### Step 5.3: Test Concurrent Simulations

```bash
# Start 5 simulations simultaneously
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/simulations/run-ipes \
    -F "file=@examples/api-testing/test-circuits/resistor-divider.ipes" \
    -F "duration=0.1" &
done
wait

# Check all completed
curl http://localhost:8080/api/simulations/health
```

**✅ Pass Criteria:**
- All 5 simulations complete successfully
- No resource exhaustion
- Reasonable completion time (< 5 seconds total)

---

## 6. Documentation Testing

### Step 6.1: Verify API Documentation

```bash
# Open Swagger UI (if configured)
firefox http://localhost:8080/swagger-ui.html

# Or check OpenAPI spec
curl http://localhost:8080/v3/api-docs
```

**✅ Pass Criteria:**
- Swagger UI loads
- All endpoints documented
- Request/response schemas present

### Step 6.2: Verify Examples Work

Go through examples in `SIGNAL_ANALYSIS_API.md`:
- [ ] All curl examples execute successfully
- [ ] All expected responses match actual responses
- [ ] No broken links or outdated information

---

## 🐛 Troubleshooting

### Problem: "API is not running"
**Solution:**
```bash
cd gecko-rest-api
java -jar target/gecko-rest-api-2.0.0-SNAPSHOT.jar
# Wait for "Started GeckoRestApiApplication"
```

### Problem: "Simulation not found"
**Solution:** Simulation may have been garbage collected. Use a recent simulationId or start a new simulation.

### Problem: "Bridge not available"
**Solution:** Legacy simulation engine not on classpath. Ensure `gecko-legacy-desktop` module is compiled and available.

### Problem: WebSocket connection fails
**Solution:**
- Check CORS settings in `WebSocketConfig.java`
- Ensure SockJS fallback is working
- Try Chrome DevTools → Network → WS tab to debug

### Problem: FFT returns invalid data
**Solution:** Signal may not have enough data points or may be constant. Try:
- Longer simulation duration
- Non-trivial circuit (buck converter instead of resistor divider)

---

## ✅ Test Results Summary

After completing all tests, fill in:

| Category | Tests | Passed | Failed | Notes |
|----------|-------|--------|--------|-------|
| Health Check | 1 | ☐ | ☐ | |
| Simulation Upload | 1 | ☐ | ☐ | |
| Signal Analysis | 6 | ☐ | ☐ | |
| Error Handling | 3 | ☐ | ☐ | |
| WebSocket | 4 | ☐ | ☐ | |
| Unit Tests | 15 | ☐ | ☐ | |
| Integration Tests | 7 | ☐ | ☐ | |
| Python Client | 1 | ☐ | ☐ | |
| Performance | 3 | ☐ | ☐ | |
| **TOTAL** | **41** | **0** | **0** | |

---

## 📊 Expected Metrics

### Response Times (p95)
- Health check: < 10ms
- Simulation upload: < 100ms
- Status polling: < 20ms
- RMS analysis: < 100ms
- THD analysis: < 100ms
- FFT analysis: < 200ms
- Stats analysis: < 150ms
- WebSocket message: < 50ms

### Success Rates
- Simulation completion: > 95%
- Signal analysis: 100% (for completed simulations)
- WebSocket delivery: > 99%

### Resource Usage
- Memory: < 512MB per simulation
- CPU: < 50% during simulation
- Network: < 10 KB/sec for WebSocket updates

---

**Last Updated:** January 25, 2026
**Test Status:** Ready for execution
**Next Review:** After completing Tasks #8-9
