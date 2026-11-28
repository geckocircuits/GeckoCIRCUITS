# Generated clients

1. Start the API.
2. Set `GECKO_API_BASE_URL` if the API does not use `http://localhost:8080/gecko`.
3. Run `bash scripts/snapshot-openapi.sh --dry-run`.
4. Run `bash scripts/snapshot-openapi.sh`.
5. Run `bash scripts/generate-sdks.sh --dry-run`.
6. Run `bash scripts/generate-sdks.sh`.

The generator uses the pinned Docker image `openapitools/openapi-generator-cli:v7.10.0`.
Docker Desktop's Windows client is supported from WSL. Native Docker is used when available.
The script writes Python, Java, and TypeScript clients into ignored directories.
The container uses the caller's UID and GID, so generated files remain writable.
Each client includes generated API documentation and examples. Generated sources are not committed.
The script does not purge existing output directories.

Install the Python client in an existing project environment:

```bash
pip install ./clients/python
python clients/smoke_test.py
```

Set `GECKO_API_KEY` or `GECKO_ACCESS_TOKEN` when authentication is enabled.
The smoke test lists circuits and runs the local `buck_simple.ipes` fixture.
Use `--circuit` for a different path that the API process can read.

Build the Java client:

```bash
cd clients/java
mvn test
```

Build the TypeScript client:

```bash
cd clients/typescript
npm install
npm run build
```

Python usage:

```python
from gecko_client import ApiClient, Configuration
from gecko_client.api.circuit_files_api import CircuitFilesApi

config = Configuration(host="http://localhost:8080/gecko")
config.api_key["ApiKey"] = "your-api-key"
with ApiClient(config) as client:
    print(CircuitFilesApi(client).get_all_circuits(page=0, size=20))
```

Java usage:

```java
import gecko.client.ApiClient;
import gecko.client.api.CircuitFilesApi;

ApiClient client = new ApiClient().setBasePath("http://localhost:8080/gecko");
client.setRequestInterceptor(request -> request.header("X-API-Key", "your-api-key"));
System.out.println(new CircuitFilesApi(client).getAllCircuits(0, 20, "id,asc"));
```

TypeScript usage:

```typescript
import { Configuration, CircuitFilesApi } from "geckocircuits-client";

const config = new Configuration({
  basePath: "http://localhost:8080/gecko",
  apiKey: "your-api-key",
});
console.log(await new CircuitFilesApi(config).getAllCircuits({ page: 0, size: 20 }));
```

Package publication requires registry credentials. See [issue #102](https://github.com/tinix84/GeckoCIRCUITS/issues/102).
