#!/usr/bin/env python3
"""
Quick installation verification: run key example circuits and report results.

Usage:
    python3 scripts/run-examples.py
    xvfb-run python3 scripts/run-examples.py   # headless

This script loads a curated set of example circuits to verify your
GeckoCIRCUITS installation is working correctly.
"""

import subprocess
import sys
import time
from pathlib import Path


# Curated list of circuits that should always load successfully
EXAMPLE_CIRCUITS = [
    ("Buck Converter", "tutorials/2xx_dcdc_converters/201_buck_converter/buck_simple.ipes"),
    ("Boost Converter", "tutorials/2xx_dcdc_converters/202_boost_converter/boost_simple.ipes"),
    ("Buck-Boost", "tutorials/2xx_dcdc_converters/203_buck_boost/buckBoost_simple.ipes"),
    ("Single-Phase Inverter", "tutorials/4xx_dcac_inverters/401_single_phase_inverter/singlePhase_PWM_converter.ipes"),
    ("Thermal Simulation", "tutorials/5xx_thermal_simulation/501_loss_calculation/BuckBoost_thermal.ipes"),
    ("GeckoSCRIPT", "tutorials/7xx_scripting_automation/701_gecko_script_basics/GeckoSCRIPT.ipes"),
]


def main():
    project_root = Path(__file__).parent.parent
    jar_path = project_root / "target" / "gecko-1.0-jar-with-dependencies.jar"
    resources_dir = project_root / "resources"

    if not jar_path.exists():
        print("JAR not found. Building...")
        result = subprocess.run(
            ["mvn", "package", "assembly:single", "-DskipTests", "--no-transfer-progress"],
            cwd=project_root, capture_output=True, text=True
        )
        if result.returncode != 0:
            print(f"Build failed:\n{result.stderr[-500:]}")
            sys.exit(1)
        print("Build complete.\n")

    print("GeckoCIRCUITS Installation Check")
    print("=" * 50)
    print(f"JAR: {jar_path.name}")
    print(f"Resources: {resources_dir}\n")

    passed = 0
    failed = 0
    skipped = 0

    for name, rel_path in EXAMPLE_CIRCUITS:
        circuit = resources_dir / rel_path

        if not circuit.exists():
            print(f"  SKIP  {name} (file not found)")
            skipped += 1
            continue

        start = time.time()
        try:
            result = subprocess.run(
                [
                    "java", "-Xmx1G",
                    "-Dpolyglot.js.nashorn-compat=true",
                    "-cp", str(jar_path),
                    "gecko.systemtests.CircuitValidator",
                    str(circuit)
                ],
                capture_output=True, text=True,
                timeout=30, cwd=project_root
            )
            duration = time.time() - start

            if result.returncode == 0:
                print(f"  PASS  {name} ({duration:.1f}s)")
                passed += 1
            else:
                print(f"  FAIL  {name} ({duration:.1f}s)")
                # Show first line of error
                err = result.stderr.strip().split("\n")[0] if result.stderr else "Unknown error"
                print(f"        {err}")
                failed += 1

        except subprocess.TimeoutExpired:
            print(f"  FAIL  {name} (timeout)")
            failed += 1
        except Exception as e:
            print(f"  FAIL  {name} ({e})")
            failed += 1

    print(f"\n{'=' * 50}")
    print(f"Results: {passed} passed, {failed} failed, {skipped} skipped")

    if failed == 0 and skipped == 0:
        print("Installation verified successfully!")
    elif failed == 0:
        print("All available circuits passed.")
    else:
        print("Some circuits failed. Check your Java version and display settings.")

    sys.exit(0 if failed == 0 else 1)


if __name__ == "__main__":
    main()
