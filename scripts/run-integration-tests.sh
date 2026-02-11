#!/bin/bash
#
# GeckoCIRCUITS Integration Tests
#
# This script runs all circuit validation tests before a release.
# It validates that all example .ipes files can be loaded correctly.
#
# Usage:
#   ./scripts/run-integration-tests.sh
#
# Options:
#   --headless    Use Xvfb for headless testing (default if no DISPLAY)
#   --with-sim    Also run short simulation for each circuit
#   --verbose     Show detailed output
#   --help        Show this help message
#
# Exit codes:
#   0 - All tests passed
#   1 - Some tests failed
#   2 - Setup/configuration error
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default options
USE_HEADLESS=false
WITH_SIM=false
VERBOSE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --headless)
            USE_HEADLESS=true
            shift
            ;;
        --with-sim)
            WITH_SIM=true
            shift
            ;;
        --verbose|-v)
            VERBOSE=true
            shift
            ;;
        --help|-h)
            echo "GeckoCIRCUITS Integration Tests"
            echo ""
            echo "Usage:"
            echo "  ./scripts/run-integration-tests.sh [options]"
            echo ""
            echo "Options:"
            echo "  --headless    Use Xvfb for headless testing (default if no DISPLAY)"
            echo "  --with-sim    Also run short simulation for each circuit"
            echo "  --verbose     Show detailed output"
            echo "  --help        Show this help message"
            echo ""
            echo "Exit codes:"
            echo "  0 - All tests passed"
            echo "  1 - Some tests failed"
            echo "  2 - Setup/configuration error"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 2
            ;;
    esac
done

echo "============================================"
echo "GeckoCIRCUITS Integration Tests"
echo "============================================"
echo ""
echo "Project root: $PROJECT_ROOT"
echo ""

# Check for required tools
check_requirements() {
    local missing=()

    if ! command -v java &> /dev/null; then
        missing+=("java")
    fi

    if ! command -v python3 &> /dev/null; then
        missing+=("python3")
    fi

    if [ ${#missing[@]} -ne 0 ]; then
        echo "Error: Missing required tools: ${missing[*]}"
        echo "Run ./scripts/setup-wsl.sh to install dependencies"
        exit 2
    fi
}

# Check/setup display
setup_display() {
    if [ -n "$DISPLAY" ] && [ "$USE_HEADLESS" = false ]; then
        echo "Using display: $DISPLAY"
        return 0
    fi

    # Check for WSLg display
    if [ -S "/tmp/.X11-unix/X0" ] && [ "$USE_HEADLESS" = false ]; then
        export DISPLAY=:0
        echo "Using WSLg display: $DISPLAY"
        return 0
    fi

    # Need Xvfb for headless
    if ! command -v xvfb-run &> /dev/null; then
        echo "Error: No display available and xvfb-run not found"
        echo "Install with: sudo apt-get install xvfb"
        echo "Or run with a display: DISPLAY=:0 $0"
        exit 2
    fi

    USE_HEADLESS=true
    echo "Using Xvfb for headless testing"
}

# Build the project if needed
build_if_needed() {
    local jar_path="$PROJECT_ROOT/target/gecko-1.0-jar-with-dependencies.jar"

    if [ ! -f "$jar_path" ]; then
        echo "Building project..."
        cd "$PROJECT_ROOT"
        mvn clean package assembly:single -DskipTests -q
        echo "Build complete."
    else
        echo "JAR file found: $jar_path"
    fi
}

# Run validation using Python script
run_python_validation() {
    echo ""
    echo "Running circuit validation..."
    echo ""

    local python_args=""
    if [ "$VERBOSE" = true ]; then
        python_args="--verbose"
    fi

    local timeout_val=30
    if [ "$WITH_SIM" = true ]; then
        timeout_val=60
        echo "Note: Running with simulation (longer timeout)"
    fi

    cd "$PROJECT_ROOT"

    if [ "$USE_HEADLESS" = true ]; then
        xvfb-run --auto-servernum --server-args="-screen 0 1024x768x24" \
            python3 resources/validate_circuits.py --timeout "$timeout_val" $python_args
    else
        DISPLAY="${DISPLAY:-:0}" python3 resources/validate_circuits.py --timeout "$timeout_val" $python_args
    fi
}

# Run validation using shell loop (alternative method)
run_shell_validation() {
    echo ""
    echo "Running circuit validation (shell method)..."
    echo ""

    cd "$PROJECT_ROOT"

    local jar_path="target/gecko-1.0-jar-with-dependencies.jar"
    local count=0
    local passed=0
    local failed=0
    local failed_files=()

    # Find all .ipes files
    local files=($(find resources -name "*.ipes" -type f | sort))
    local total=${#files[@]}

    echo "Found $total .ipes files to validate"
    echo ""

    for file in "${files[@]}"; do
        count=$((count + 1))
        local name=$(basename "$file")

        local cmd="java -Xmx1G -Dpolyglot.js.nashorn-compat=true -cp $jar_path ch.technokrat.systemtests.CircuitValidator"

        if [ "$WITH_SIM" = true ]; then
            cmd="$cmd --run-simulation"
        fi

        local result
        if [ "$USE_HEADLESS" = true ]; then
            result=$(xvfb-run --auto-servernum --server-args="-screen 0 1024x768x24" \
                timeout 30 $cmd "$file" 2>&1 | grep -E "^(PASS|FAIL)" || echo "FAIL")
        else
            result=$(DISPLAY="${DISPLAY:-:0}" timeout 30 $cmd "$file" 2>&1 | grep -E "^(PASS|FAIL)" || echo "FAIL")
        fi

        if echo "$result" | grep -q "^PASS"; then
            if [ "$VERBOSE" = true ]; then
                echo "[$count/$total] + $name"
            else
                printf "\r[$count/$total] Testing circuits... "
            fi
            passed=$((passed + 1))
        else
            if [ "$VERBOSE" = true ]; then
                echo "[$count/$total] - $name (FAILED)"
            fi
            failed=$((failed + 1))
            failed_files+=("$file")
        fi
    done

    echo ""
    echo ""
    echo "============================================"
    echo "VALIDATION RESULTS"
    echo "============================================"
    echo "Total:  $total"
    echo "Passed: $passed"
    echo "Failed: $failed"

    if [ $failed -gt 0 ]; then
        echo ""
        echo "Failed files:"
        for f in "${failed_files[@]}"; do
            echo "  - $f"
        done
        return 1
    fi

    return 0
}

# Main execution
main() {
    check_requirements
    setup_display
    build_if_needed

    # Use Python validation by default (generates JSON report)
    if [ -f "$PROJECT_ROOT/resources/validate_circuits.py" ]; then
        run_python_validation
        local exit_code=$?
    else
        # Fallback to shell validation
        run_shell_validation
        local exit_code=$?
    fi

    echo ""
    if [ $exit_code -eq 0 ]; then
        echo "All integration tests PASSED"
    else
        echo "Some integration tests FAILED"
    fi

    return $exit_code
}

# Run main
main "$@"
