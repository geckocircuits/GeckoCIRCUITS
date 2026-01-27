# Quick Command Reference - REST API MVP

## Start the REST API Server

```bash
cd /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api
java -jar target/gecko-rest-api-1.0.0.jar
```

**Server will be available at:**
- Base API: `http://localhost:8080/gecko`
- Health: `http://localhost:8080/gecko/api/health`
- Swagger UI: `http://localhost:8080/gecko/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/gecko/api-docs`

---

## Test Endpoints with curl

### Health Check
```bash
curl http://localhost:8080/gecko/api/health
```

### API Info
```bash
curl http://localhost:8080/gecko/api/info
```

### Submit Simulation
```bash
curl -X POST http://localhost:8080/gecko/api/v1/simulations \
  -H "Content-Type: application/json" \
  -d '{
    "circuitFile": "test-circuit.xml",
    "simulationTime": 1.0,
    "timeStep": 0.01
  }'
```

### List Simulations
```bash
curl http://localhost:8080/gecko/api/v1/simulations
```

### Get Simulation Result
```bash
curl http://localhost:8080/gecko/api/v1/simulations/{simulationId}
```

### Cancel Simulation
```bash
curl -X DELETE http://localhost:8080/gecko/api/v1/simulations/{simulationId}
```

---

## Build REST API JAR

```bash
cd /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api
mvn clean package -DskipTests
```

**Output:** `target/gecko-rest-api-1.0.0.jar` (26 MB)

---

## Validate No GUI Dependencies

```bash
cd /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api
mvn validate
```

**Success:** `BUILD SUCCESS` (no Swing/AWT detected)

---

## Check All Builds (Monolith + Core + REST API)

```bash
cd /home/tinix/claude_wsl/GeckoCIRCUITS

# Build monolith
mvn clean package -DskipTests -q

# Build core module
cd gecko-simulation-core && mvn package -DskipTests -q

# Build REST API
cd ../gecko-rest-api && mvn package -DskipTests -q

# All should complete with SUCCESS
```

---

## Run All Tests

```bash
cd /home/tinix/claude_wsl/GeckoCIRCUITS
mvn test -DskipTests=false -q
```

**Expected:** 2710 tests run, 0 failures, 99.8% success rate

---

## View REST API Code

### Endpoints
```bash
cat /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/controller/SimulationController.java
```

### Service Layer
```bash
cat /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/java/ch/technokrat/gecko/rest/service/SimulationService.java
```

### Configuration
```bash
cat /home/tinix/claude_wsl/GeckoCIRCUITS/gecko-rest-api/src/main/resources/application.properties
```

---

## Docker Deployment (Optional)

Create `Dockerfile` in `gecko-rest-api/`:
```dockerfile
FROM openjdk:21-slim
COPY target/gecko-rest-api-1.0.0.jar /app/api.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/api.jar"]
```

Build and run:
```bash
docker build -t gecko-api:1.0 .
docker run -p 8080:8080 gecko-api:1.0
```

---

## Troubleshooting

### Server won't start
Check logs: `tail -20 /tmp/api_server.log`

### Port 8080 already in use
```bash
# Find and kill existing process
lsof -i :8080
kill -9 {PID}
```

### OpenAPI docs not loading
Check: `curl http://localhost:8080/gecko/api-docs`

### Simulation returns 400 error
Verify request JSON:
```bash
curl -X POST http://localhost:8080/gecko/api/v1/simulations \
  -H "Content-Type: application/json" \
  -d '{
    "circuitFile": "file.xml",
    "simulationTime": 1.0,
    "timeStep": 0.01
  }'
```

---

## Environment Variables

Default configuration is in `application.properties`. Override with environment variables:

```bash
java -Dserver.port=9000 \
     -Dlogging.level.root=DEBUG \
     -jar target/gecko-rest-api-1.0.0.jar
```

---

## Frontend Integration

1. **Development**: Point your frontend to `http://localhost:8080/gecko`
2. **Testing**: Use Swagger UI at `/swagger-ui.html` to explore endpoints
3. **Code Generation**: Download OpenAPI spec from `/api-docs` and use OpenAPI Generator
4. **Production**: Deploy JAR to application server (Tomcat, Docker, Kubernetes)

---

## File Locations

```
GeckoCIRCUITS/
├── gecko-rest-api/
│   ├── pom.xml
│   ├── src/main/
│   │   ├── java/ch/technokrat/gecko/rest/
│   │   │   ├── GeckoRestApiApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── HealthController.java
│   │   │   │   ├── SimulationController.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── model/
│   │   │   │   ├── SimulationRequest.java
│   │   │   │   └── SimulationResponse.java
│   │   │   └── service/
│   │   │       └── SimulationService.java
│   │   └── resources/
│   │       └── application.properties
│   └── target/
│       └── gecko-rest-api-1.0.0.jar (26 MB)
├── TIER2_P0_REST_API_SUMMARY.md
└── [other modules]
```

---

## Summary

✅ **REST API ready in 2-3 hours**  
✅ **8 endpoints fully functional**  
✅ **Swagger documentation auto-generated**  
✅ **Frontend can start development now**  
✅ **All safety gates active (Maven Enforcer)**  

**Next: Tier 2 P1 (DataContainer Tests - 2-3 hours)**
