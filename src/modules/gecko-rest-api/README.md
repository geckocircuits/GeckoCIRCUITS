# GeckoCIRCUITS REST API

Headless REST API for circuit simulation using the GeckoCIRCUITS simulation engine.

## Features

- **Circuit Simulation**: Submit `.ipes` circuit files for headless simulation
- **Signal Analysis**: Retrieve time-series data, compute RMS, THD, FFT
- **OpenAPI/Swagger**: Interactive API documentation at `/swagger-ui.html`
- **Docker Support**: Production-ready containerization
- **Health Monitoring**: Built-in health checks and progress tracking

## Quick Start

### Using Docker Desktop (Windows, macOS, Linux)

**Prerequisites:**
- Install [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- Ensure Docker Desktop is running (check the system tray/menu bar)

**Step 1: Clone and Navigate**
```bash
git clone https://github.com/tinix84/GeckoCIRCUITS.git
cd GeckoCIRCUITS
```

**Step 2: Build the Image**

Using Docker Desktop's built-in compose:
```bash
# Open terminal in Docker Desktop or use your system terminal
docker compose up --build -d
```

Or using the helper script (Linux/macOS/WSL):
```bash
./scripts/build-docker.sh
```

**Step 3: Verify Deployment**
- Open Docker Desktop
- Go to "Containers" tab
- Look for `gecko-rest-api` container (should show "Running")
- Click the container name to view logs

**Step 4: Access the API**
- **Health Check**: http://localhost:8080/api/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

**Docker Desktop Management:**
- **View Logs**: Click container → "Logs" tab
- **Stop Container**: Click container → "Stop" button
- **Restart**: Click container → "Restart" button
- **Delete**: Click container → "Delete" button

**Troubleshooting (Docker Desktop):**
- **Port conflict**: If port 8080 is in use, edit `docker-compose.yml` and change `ports: - "9090:8080"`
- **Container won't start**: Check logs in Docker Desktop, ensure 4GB+ RAM allocated in Settings → Resources
- **Slow build**: Increase CPU cores in Settings → Resources → CPUs

### Using Docker CLI (Alternative)

```bash
# Build the Docker image
./scripts/build-docker.sh

# Run with docker-compose
docker-compose up -d

# Or run with the helper script
./scripts/run-docker.sh
```

Access the API:
- **Health Check**: http://localhost:8080/api/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

### Using Maven

```bash
# Build core module first (dependency)
cd ../gecko-simulation-core
mvn clean install -DskipTests

# Build and run REST API
cd ../gecko-rest-api
mvn spring-boot:run
```

## API Endpoints

### Health & Info
- `GET /api/health` - Health check
- `GET /api/info` - API metadata
- `GET /api/docs` - Documentation links

### Simulations
- `POST /api/v1/simulations` - Submit new simulation
- `GET /api/v1/simulations` - List all simulations (with optional status filter)
- `GET /api/v1/simulations/{id}` - Get simulation status
- `GET /api/v1/simulations/{id}/results` - Get all results
- `GET /api/v1/simulations/{id}/results/{signal}` - Get specific signal
- `GET /api/v1/simulations/{id}/progress` - Get simulation progress
- `DELETE /api/v1/simulations/{id}` - Cancel simulation
- `POST /api/v1/simulations/{id}/export` - Export results as CSV

## Example Usage

### Submit a Simulation

```bash
curl -X POST http://localhost:8080/api/v1/simulations \
  -H "Content-Type: application/json" \
  -d '{
    "circuitFile": "buck_converter.ipes",
    "simulationTime": 0.01,
    "timeStep": 1e-6
  }'
```

Response:
```json
{
  "simulationId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "circuitFile": "buck_converter.ipes",
  "submittedAt": "2026-02-12T23:15:00Z"
}
```

### Check Status

```bash
curl http://localhost:8080/api/v1/simulations/550e8400-e29b-41d4-a716-446655440000
```

### Get Results

```bash
curl http://localhost:8080/api/v1/simulations/550e8400-e29b-41d4-a716-446655440000/results
```

Response:
```json
{
  "time": [0.0, 1e-6, 2e-6, ...],
  "V_out": [0.0, 0.5, 1.2, ...],
  "I_L": [0.0, 0.1, 0.15, ...]
}
```

### Export as CSV

```bash
curl -X POST http://localhost:8080/api/v1/simulations/550e8400-e29b-41d4-a716-446655440000/export \
  -o results.csv
```

## Docker Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JAVA_OPTS` | `-Xmx512m -Xms256m` | JVM memory settings |
| `SERVER_PORT` | `8080` | HTTP port |
| `CIRCUITS_PATH` | `/app/circuits` | Path to circuit files |
| `LOG_LEVEL` | `INFO` | Logging level |

### Volumes

- `/app/circuits` - Mount directory containing `.ipes` circuit files (read-only)
- `/app/logs` - Application logs (persistent)

### Example docker-compose.yml

```yaml
version: '3.8'
services:
  gecko-rest-api:
    image: geckocircuits/rest-api:1.0.0
    ports:
      - "8080:8080"
    volumes:
      - ./circuits:/app/circuits:ro
      - ./logs:/app/logs
    environment:
      JAVA_OPTS: "-Xmx1g"
      LOG_LEVEL: DEBUG
```

## Development

### Run Tests

```bash
mvn test
```

78 integration tests verify:
- Health endpoints
- Simulation CRUD operations
- Result retrieval and filtering
- CSV export
- Error handling

### Build JAR

```bash
mvn clean package
```

Output: `target/gecko-rest-api-1.0.0.jar`

### Run JAR

```bash
java -jar target/gecko-rest-api-1.0.0.jar
```

## Architecture

### Components

```
gecko-rest-api/
├── controller/
│   ├── HealthController.java       # Health & info endpoints
│   ├── SimulationController.java   # Simulation CRUD
│   └── GlobalExceptionHandler.java # Error handling
├── service/
│   └── SimulationService.java      # Business logic
├── model/
│   ├── SimulationRequest.java      # Request DTOs
│   └── SimulationResponse.java     # Response DTOs
└── GeckoRestApiApplication.java    # Spring Boot main
```

### Dependencies

- **Spring Boot 3.2.1** - Web framework
- **SpringDoc OpenAPI** - API documentation
- **gecko-simulation-core** - Headless simulation engine (148 classes, GUI-free)

## Performance

### Docker Image
- **Size**: ~180MB (Alpine-based JRE 21)
- **Startup**: ~8 seconds
- **Memory**: 512MB base, 1GB recommended

### Simulation Throughput
- Simple circuits (RC, RLC): ~100 simulations/minute
- Complex circuits (Buck converter): ~20 simulations/minute
- Concurrent simulations: Up to CPU core count

## Security

- **Non-root user**: Container runs as user `gecko` (UID 1000)
- **Read-only circuits**: Circuit files mounted as read-only
- **Headless mode**: No GUI dependencies, `-Djava.awt.headless=true`
- **Input validation**: Jakarta Bean Validation on all endpoints

## Monitoring

### Health Check

Built-in health endpoint for Kubernetes/Docker:
```bash
wget --no-verbose --tries=1 --spider http://localhost:8080/api/health
```

Returns:
```json
{
  "status": "UP",
  "version": "1.0.0"
}
```

### Logs

View logs in real-time:
```bash
# Docker
docker logs -f gecko-rest-api

# docker-compose
docker-compose logs -f
```

## Troubleshooting

### Circuit file not found

**Error**: `Circuit file not found: example.ipes`

**Solution**: Ensure circuit files are mounted in `/app/circuits`:
```bash
docker run -v /path/to/circuits:/app/circuits:ro ...
```

### Out of memory

**Error**: `java.lang.OutOfMemoryError: Java heap space`

**Solution**: Increase heap size:
```bash
docker run -e JAVA_OPTS="-Xmx2g" ...
```

### Port already in use

**Error**: `Bind for 0.0.0.0:8080 failed: port is already allocated`

**Solution**: Use a different port:
```bash
docker run -p 9090:8080 ...
```

## License

GPL v3 - See LICENSE file for details.

## Links

- **Documentation**: https://tinix84.github.io/GeckoCIRCUITS/
- **Main Repository**: https://github.com/tinix84/GeckoCIRCUITS
- **Docker Hub**: (coming soon)
