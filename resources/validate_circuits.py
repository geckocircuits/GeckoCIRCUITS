#!/usr/bin/env python3
"""
Validate all .ipes circuit files in the resources directory.

This script finds all .ipes files under resources/, attempts to load and run
a short simulation for each, and generates a summary report.

REQUIREMENTS:
    - GeckoCIRCUITS requires a display (X11) to run, even in test mode.
    - On headless systems (CI, WSL without GUI), use xvfb-run:
        xvfb-run python3 validate_circuits.py
    - Or install Xvfb and run:
        Xvfb :99 -screen 0 1024x768x24 &
        export DISPLAY=:99
        python3 validate_circuits.py

Usage:
    python3 validate_circuits.py [options]
    xvfb-run python3 validate_circuits.py [options]

Options:
    --timeout SECONDS    Timeout per circuit (default: 30)
    --verbose           Show detailed output
    --json-only         Only output JSON report, no console output
    --use-xvfb          Automatically start Xvfb if no DISPLAY is set
    --help              Show this help message

Example:
    # Basic validation
    python3 validate_circuits.py

    # With xvfb on headless system
    xvfb-run python3 validate_circuits.py

    # Verbose output with longer timeout
    python3 validate_circuits.py --verbose --timeout 60
"""

import subprocess
import json
import sys
import argparse
import shutil
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Any, Optional
import os
import signal


def find_ipes_files(base_dir: Path) -> List[Path]:
    """Find all .ipes files recursively under base_dir."""
    # Filter to only include actual files (not directories)
    return sorted([f for f in base_dir.rglob("*.ipes") if f.is_file()])


def build_jar_if_needed(project_root: Path, quiet: bool = False) -> Path:
    """Build the jar file if it doesn't exist."""
    jar_path = project_root / "target" / "gecko-1.0-jar-with-dependencies.jar"

    if not jar_path.exists():
        if not quiet:
            print("JAR not found, building...")
        result = subprocess.run(
            ["mvn", "clean", "package", "assembly:single", "-DskipTests"],
            cwd=project_root,
            capture_output=True,
            text=True
        )
        if result.returncode != 0:
            print(f"Build failed: {result.stderr}")
            sys.exit(1)
        if not quiet:
            print("Build complete.")

    return jar_path


def check_display() -> bool:
    """Check if a display is available."""
    display = os.environ.get("DISPLAY", "")
    return bool(display)


def start_xvfb() -> Optional[subprocess.Popen]:
    """Start Xvfb on display :99 if not already running."""
    if not shutil.which("Xvfb"):
        print("Error: Xvfb not found. Install with: sudo apt-get install xvfb")
        return None

    # Check if :99 is already in use
    display_num = 99
    while display_num < 200:
        lock_file = Path(f"/tmp/.X{display_num}-lock")
        if not lock_file.exists():
            break
        display_num += 1

    try:
        proc = subprocess.Popen(
            ["Xvfb", f":{display_num}", "-screen", "0", "1024x768x24"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            start_new_session=True
        )
        os.environ["DISPLAY"] = f":{display_num}"
        print(f"Started Xvfb on display :{display_num}")
        return proc
    except Exception as e:
        print(f"Failed to start Xvfb: {e}")
        return None


def run_circuit(
    jar_path: Path,
    ipes_file: Path,
    timeout: int = 30,
    use_xvfb_run: bool = False
) -> Dict[str, Any]:
    """
    Attempt to load and run a circuit file using the CircuitValidator.

    Returns a dict with:
        - status: 'pass', 'fail', 'timeout', or 'error'
        - returncode: process exit code (-1 for timeout/error)
        - stderr: first 500 chars of stderr if any
        - stdout: first 500 chars of stdout if any
        - duration: execution time in seconds
    """
    java_cmd = [
        "java", "-Xmx1G",
        "-Dpolyglot.js.nashorn-compat=true",
        "-cp", str(jar_path),
        "ch.technokrat.systemtests.CircuitValidator",
        str(ipes_file)
    ]

    # If no display and xvfb-run is available, use it
    if use_xvfb_run and shutil.which("xvfb-run"):
        cmd = ["xvfb-run", "--auto-servernum", "--server-args=-screen 0 1024x768x24"] + java_cmd
    else:
        cmd = java_cmd

    start_time = datetime.now()

    try:
        result = subprocess.run(
            cmd,
            timeout=timeout,
            capture_output=True,
            text=True,
            cwd=jar_path.parent.parent  # Run from project root
        )
        duration = (datetime.now() - start_time).total_seconds()

        # Check for specific error patterns
        stderr = result.stderr[:500] if result.stderr else ""
        stdout = result.stdout[:500] if result.stdout else ""

        if result.returncode == 0:
            status = "pass"
        elif "HeadlessException" in stderr or "HeadlessException" in stdout:
            status = "error"
            stderr = "Display required. Run with xvfb-run or set DISPLAY environment variable."
        else:
            status = "fail"

        return {
            "status": status,
            "returncode": result.returncode,
            "stderr": stderr,
            "stdout": stdout,
            "duration": duration
        }
    except subprocess.TimeoutExpired:
        duration = (datetime.now() - start_time).total_seconds()
        return {
            "status": "timeout",
            "returncode": -1,
            "stderr": f"Timeout after {timeout}s",
            "stdout": "",
            "duration": duration
        }
    except Exception as e:
        duration = (datetime.now() - start_time).total_seconds()
        return {
            "status": "error",
            "returncode": -1,
            "stderr": str(e),
            "stdout": "",
            "duration": duration
        }


def run_validation(
    base_dir: Path,
    jar_path: Path,
    timeout: int = 30,
    verbose: bool = False,
    json_only: bool = False,
    use_xvfb_run: bool = False
) -> List[Dict[str, Any]]:
    """Run validation on all .ipes files."""
    files = find_ipes_files(base_dir)

    if not json_only:
        print(f"Found {len(files)} .ipes files to validate\n")

    results = []

    for i, f in enumerate(files, 1):
        result = run_circuit(jar_path, f, timeout, use_xvfb_run)
        result["file"] = str(f.relative_to(base_dir))
        result["absolute_path"] = str(f)
        results.append(result)

        if not json_only:
            status_symbol = {
                "pass": "\033[92m+\033[0m",      # Green +
                "fail": "\033[91m-\033[0m",      # Red -
                "timeout": "\033[93mT\033[0m",   # Yellow T
                "error": "\033[91m!\033[0m"      # Red !
            }.get(result["status"], "?")

            progress = f"[{i}/{len(files)}]"
            print(f"{progress:10} {status_symbol} {result['file']}")

            if verbose and result["status"] != "pass":
                if result["stderr"]:
                    print(f"           Error: {result['stderr'][:200]}")

        # Early exit on display error (all subsequent will fail too)
        if result["status"] == "error" and "Display required" in result.get("stderr", ""):
            if not json_only:
                print("\n" + "=" * 60)
                print("ERROR: Display not available")
                print("=" * 60)
                print("GeckoCIRCUITS requires a display to run.")
                print("\nOptions:")
                print("  1. Use xvfb-run: xvfb-run python3 validate_circuits.py")
                print("  2. Start Xvfb manually:")
                print("     Xvfb :99 -screen 0 1024x768x24 &")
                print("     export DISPLAY=:99")
                print("     python3 validate_circuits.py")
                print("  3. Run with --use-xvfb flag (requires xvfb-run)")
            break

    return results


def generate_summary(results: List[Dict[str, Any]]) -> Dict[str, Any]:
    """Generate a summary of validation results."""
    passed = [r for r in results if r["status"] == "pass"]
    failed = [r for r in results if r["status"] == "fail"]
    timeouts = [r for r in results if r["status"] == "timeout"]
    errors = [r for r in results if r["status"] == "error"]

    total_duration = sum(r.get("duration", 0) for r in results)

    return {
        "total": len(results),
        "passed": len(passed),
        "failed": len(failed),
        "timeouts": len(timeouts),
        "errors": len(errors),
        "pass_rate": f"{len(passed) / len(results) * 100:.1f}%" if results else "N/A",
        "total_duration_seconds": round(total_duration, 2),
        "failed_files": [r["file"] for r in failed],
        "timeout_files": [r["file"] for r in timeouts],
        "error_files": [r["file"] for r in errors]
    }


def main():
    parser = argparse.ArgumentParser(
        description="Validate all .ipes circuit files in the resources directory.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
    # Basic validation (requires display)
    python3 validate_circuits.py

    # With xvfb on headless system
    xvfb-run python3 validate_circuits.py

    # Automatic xvfb usage
    python3 validate_circuits.py --use-xvfb

    # Verbose output with longer timeout
    python3 validate_circuits.py --verbose --timeout 60
"""
    )
    parser.add_argument(
        "--timeout", type=int, default=30,
        help="Timeout per circuit in seconds (default: 30)"
    )
    parser.add_argument(
        "--verbose", "-v", action="store_true",
        help="Show detailed output for failures"
    )
    parser.add_argument(
        "--json-only", action="store_true",
        help="Only output JSON report, no console output"
    )
    parser.add_argument(
        "--output", "-o", type=str, default="validation_report.json",
        help="Output JSON file path (default: validation_report.json)"
    )
    parser.add_argument(
        "--use-xvfb", action="store_true",
        help="Use xvfb-run for each circuit (for headless systems)"
    )
    parser.add_argument(
        "--start-xvfb", action="store_true",
        help="Start a single Xvfb instance before validation"
    )

    args = parser.parse_args()

    # Determine paths
    script_dir = Path(__file__).parent
    project_root = script_dir.parent

    # Handle display requirements
    xvfb_proc = None
    use_xvfb_run = args.use_xvfb

    if args.start_xvfb and not check_display():
        xvfb_proc = start_xvfb()
        if xvfb_proc is None:
            print("Failed to start Xvfb. Try using --use-xvfb instead.")
            sys.exit(1)

    if not check_display() and not use_xvfb_run:
        if not args.json_only:
            print("Warning: No DISPLAY set. Consider using --use-xvfb or --start-xvfb")
        use_xvfb_run = shutil.which("xvfb-run") is not None

    try:
        # Build jar if needed
        if not args.json_only:
            print("Checking for JAR file...")
        jar_path = build_jar_if_needed(project_root, quiet=args.json_only)

        if not jar_path.exists():
            print(f"Error: JAR file not found at {jar_path}")
            sys.exit(1)

        # Run validation
        results = run_validation(
            base_dir=script_dir,
            jar_path=jar_path,
            timeout=args.timeout,
            verbose=args.verbose,
            json_only=args.json_only,
            use_xvfb_run=use_xvfb_run
        )

        # Generate summary
        summary = generate_summary(results)

        # Print summary
        if not args.json_only:
            print("\n" + "=" * 60)
            print("VALIDATION SUMMARY")
            print("=" * 60)
            print(f"Total circuits:  {summary['total']}")
            print(f"Passed:          {summary['passed']} ({summary['pass_rate']})")
            print(f"Failed:          {summary['failed']}")
            print(f"Timeouts:        {summary['timeouts']}")
            print(f"Errors:          {summary['errors']}")
            print(f"Total duration:  {summary['total_duration_seconds']}s")

            if summary["failed_files"]:
                print("\nFailed files:")
                for f in summary["failed_files"]:
                    print(f"  - {f}")

            if summary["timeout_files"]:
                print("\nTimeout files:")
                for f in summary["timeout_files"]:
                    print(f"  - {f}")

            if summary["error_files"]:
                print("\nError files:")
                for f in summary["error_files"]:
                    print(f"  - {f}")

        # Save JSON report
        report = {
            "timestamp": datetime.now().isoformat(),
            "summary": summary,
            "results": results
        }

        output_path = script_dir / args.output
        with open(output_path, "w") as f:
            json.dump(report, f, indent=2)

        if not args.json_only:
            print(f"\nJSON report saved to: {output_path}")

        # Exit with non-zero if any failures
        if summary["failed"] > 0 or summary["errors"] > 0:
            sys.exit(1)

        sys.exit(0)

    finally:
        # Clean up Xvfb if we started it
        if xvfb_proc is not None:
            xvfb_proc.terminate()
            try:
                xvfb_proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                xvfb_proc.kill()


if __name__ == "__main__":
    main()
