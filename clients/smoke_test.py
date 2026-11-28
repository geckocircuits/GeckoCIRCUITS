"""Exercise a generated Python client against a local GeckoCIRCUITS API."""
import argparse
import os
from pathlib import Path
import time

from gecko_client import ApiClient, Configuration
from gecko_client.api.circuit_files_api import CircuitFilesApi
from gecko_client.api.simulations_api import SimulationsApi
from gecko_client.models.simulation_request import SimulationRequest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default=os.getenv("GECKO_API_BASE_URL", "http://localhost:8080/gecko"))
    parser.add_argument("--circuit", default=str(Path(__file__).resolve().parents[1] /
                        "src/modules/gecko-simulation-core/src/test/resources/ipes/buck_simple.ipes"))
    parser.add_argument("--timeout", type=float, default=30)
    args = parser.parse_args()
    configuration = Configuration(host=args.url.rstrip("/"))
    if os.getenv("GECKO_API_KEY"):
        configuration.api_key["ApiKey"] = os.environ["GECKO_API_KEY"]
    if os.getenv("GECKO_ACCESS_TOKEN"):
        configuration.access_token = os.environ["GECKO_ACCESS_TOKEN"]
    with ApiClient(configuration) as client:
        listing = CircuitFilesApi(client).get_all_circuits()
        print(f"Listed {len(listing.circuits)} circuits (total {listing.total}).")
        simulations = SimulationsApi(client)
        job = simulations.submit_simulation(SimulationRequest(
            circuit_file=args.circuit, simulation_time=0.002, time_step=0.00001))
        deadline = time.monotonic() + args.timeout
        while job.status not in {"COMPLETED", "FAILED"} and time.monotonic() < deadline:
            time.sleep(0.1)
            job = simulations.get_simulation(job.simulation_id)
        if job.status != "COMPLETED":
            raise RuntimeError(f"Simulation ended with {job.status}: {job.error_message}")
        if not job.results or not job.results.get("time"):
            raise RuntimeError("The completed simulation has no time samples.")
        print(f"Simulation {job.simulation_id} completed with {len(job.results['time'])} time samples.")


if __name__ == "__main__":
    main()
