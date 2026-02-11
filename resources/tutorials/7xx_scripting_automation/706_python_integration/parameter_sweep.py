#!/usr/bin/env python3
"""
Parameter sweep example for GeckoCIRCUITS using Python subprocess.

This script demonstrates how to automate GeckoCIRCUITS simulations from Python
by running the CircuitValidator to load and verify circuits with different parameters.

For a full simulation with data extraction, GeckoCIRCUITS needs to be running
in remote mode (RMI) or the future REST API. This example shows the subprocess
approach for circuit validation and batch testing.

Usage:
    python3 parameter_sweep.py
    python3 parameter_sweep.py --jar path/to/gecko.jar
    python3 parameter_sweep.py --verbose

Requirements:
    - Java 21+
    - GeckoCIRCUITS JAR (build with: mvn package assembly:single -DskipTests)
    - Display server (or Xvfb for headless)
"""

import subprocess
import sys
import argparse
import csv
import time
from pathlib import Path
from typing import Optional


def find_jar(project_root: Path) -> Optional[Path]:
    """Find the GeckoCIRCUITS JAR file."""
    jar = project_root / "target" / "gecko-1.0-jar-with-dependencies.jar"
    if jar.exists():
        return jar
    return None


def validate_circuit(jar_path: Path, ipes_file: Path, timeout: int = 30) -> dict:
    """
    Load a circuit file using CircuitValidator and return results.

    Returns dict with: success (bool), elements (int), duration (float), error (str)
    """
    cmd = [
        "java", "-Xmx1G",
        "-Dpolyglot.js.nashorn-compat=true",
        "-cp", str(jar_path),
        "ch.technokrat.systemtests.CircuitValidator",
        str(ipes_file)
    ]

    start = time.time()
    try:
        result = subprocess.run(
            cmd, capture_output=True, text=True,
            timeout=timeout, cwd=jar_path.parent.parent
        )
        duration = time.time() - start

        # Parse output for element count
        elements = 0
        for line in result.stdout.splitlines():
            if "Loaded" in line and "circuit elements" in line:
                parts = line.split()
                for i, p in enumerate(parts):
                    if p == "Loaded" and i + 1 < len(parts):
                        try:
                            elements = int(parts[i + 1])
                        except ValueError:
                            pass

        return {
            "success": result.returncode == 0,
            "elements": elements,
            "duration": round(duration, 2),
            "error": result.stderr[:200] if result.returncode != 0 else ""
        }

    except subprocess.TimeoutExpired:
        return {
            "success": False,
            "elements": 0,
            "duration": timeout,
            "error": f"Timeout after {timeout}s"
        }
    except Exception as e:
        return {
            "success": False,
            "elements": 0,
            "duration": time.time() - start,
            "error": str(e)
        }


def sweep_circuits(jar_path: Path, circuits: list, verbose: bool = False) -> list:
    """
    Run validation on a list of circuit files.

    This demonstrates the batch execution pattern. For actual parameter sweeps
    (changing component values), you would need RMI or the REST API.
    """
    results = []

    print(f"Running sweep on {len(circuits)} circuits...")
    print(f"{'Circuit':<50} {'Status':<8} {'Elements':<10} {'Time (s)':<10}")
    print("-" * 80)

    for circuit_path in circuits:
        name = circuit_path.name
        result = validate_circuit(jar_path, circuit_path)
        result["file"] = str(circuit_path)
        result["name"] = name
        results.append(result)

        status = "PASS" if result["success"] else "FAIL"
        print(f"{name:<50} {status:<8} {result['elements']:<10} {result['duration']:<10}")

        if verbose and not result["success"]:
            print(f"  Error: {result['error']}")

    return results


def save_csv(results: list, output_path: Path):
    """Save results to CSV file."""
    with open(output_path, "w", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=["name", "success", "elements", "duration", "error"])
        writer.writeheader()
        writer.writerows(results)
    print(f"\nResults saved to: {output_path}")


def main():
    parser = argparse.ArgumentParser(description="GeckoCIRCUITS parameter sweep example")
    parser.add_argument("--jar", type=str, help="Path to gecko JAR file")
    parser.add_argument("--verbose", "-v", action="store_true", help="Show error details")
    parser.add_argument("--output", "-o", type=str, default="sweep_results.csv",
                        help="Output CSV file (default: sweep_results.csv)")
    args = parser.parse_args()

    # Find project root
    script_dir = Path(__file__).parent
    project_root = script_dir.parents[3]  # Up from 706_python_integration to repo root

    # Find JAR
    if args.jar:
        jar_path = Path(args.jar)
    else:
        jar_path = find_jar(project_root)

    if not jar_path or not jar_path.exists():
        print("Error: JAR not found. Build with: mvn package assembly:single -DskipTests")
        sys.exit(1)

    print(f"Using JAR: {jar_path}")

    # Collect circuits to test - a curated list of key examples
    tutorials_dir = project_root / "resources" / "tutorials"
    circuits = []

    # Key circuits that should always work
    key_circuits = [
        "2xx_dcdc_converters/201_buck_converter/buck_simple.ipes",
        "2xx_dcdc_converters/202_boost_converter/boost_simple.ipes",
        "2xx_dcdc_converters/203_buck_boost/buckBoost_simple.ipes",
        "2xx_dcdc_converters/203_buck_boost/cuk_simple.ipes",
        "2xx_dcdc_converters/203_buck_boost/sepic_simple.ipes",
        "4xx_dcac_inverters/401_single_phase_inverter/singlePhase_PWM_converter.ipes",
        "5xx_thermal_simulation/501_loss_calculation/BuckBoost_thermal.ipes",
        "7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.ipes",
    ]

    for rel_path in key_circuits:
        full_path = tutorials_dir / rel_path
        if full_path.exists():
            circuits.append(full_path)
        else:
            print(f"Warning: Circuit not found: {rel_path}")

    if not circuits:
        print("No circuit files found to test")
        sys.exit(1)

    # Run the sweep
    results = sweep_circuits(jar_path, circuits, verbose=args.verbose)

    # Summary
    passed = sum(1 for r in results if r["success"])
    total = len(results)
    print(f"\nSummary: {passed}/{total} circuits passed")

    # Save results
    output_path = script_dir / args.output
    save_csv(results, output_path)

    sys.exit(0 if passed == total else 1)


if __name__ == "__main__":
    main()
