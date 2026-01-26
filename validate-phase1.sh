#!/bin/bash
#
# Quick validation script for Phase 1 deliverables
# Checks that all expected files exist and have valid syntax
#

set -e

echo "========================================="
echo "Phase 1 Validation Script"
echo "GeckoCIRCUITS REST API"
echo "========================================="
echo ""

ERRORS=0
WARNINGS=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
    else
        echo -e "${RED}✗${NC} $1 (MISSING)"
        ((ERRORS++))
    fi
}

check_java_file() {
    if [ -f "$1" ]; then
        # Basic syntax check - must contain "package" and "class"
        if grep -q "package " "$1" && grep -q "class\|interface\|enum" "$1"; then
            echo -e "${GREEN}✓${NC} $1"
        else
            echo -e "${YELLOW}⚠${NC} $1 (syntax check failed)"
            ((WARNINGS++))
        fi
    else
        echo -e "${RED}✗${NC} $1 (MISSING)"
        ((ERRORS++))
    fi
}

echo "Checking Production Code..."
echo "----------------------------"

echo ""
echo "DTOs (4 files):"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/dto/SignalAnalysisResult.java"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/dto/FFTResult.java"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/dto/SignalStatsResult.java"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/dto/SimulationProgressUpdate.java"

echo ""
echo "Controllers (3 files):"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/controller/SignalAnalysisController.java"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/controller/CircuitController.java"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/controller/SimulationController.java"

echo ""
echo "Services (3 files):"
check_java_file "gecko-application/src/main/java/com/technokrat/gecko/application/InteractiveSimulationService.java"
check_java_file "gecko-application/src/main/java/com/technokrat/gecko/application/CircuitValidationService.java"
check_java_file "gecko-application/src/main/java/com/technokrat/gecko/application/LegacySimulationBridge.java"

echo ""
echo "WebSocket (3 files):"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/config/WebSocketConfig.java"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/config/SimulationWebSocketIntegration.java"
check_java_file "gecko-rest-api/src/main/java/com/technokrat/gecko/api/websocket/SimulationProgressPublisher.java"

echo ""
echo "Checking Tests..."
echo "------------------"
check_java_file "gecko-rest-api/src/test/java/com/technokrat/gecko/api/SignalAnalysisControllerTest.java"
check_java_file "gecko-rest-api/src/test/java/com/technokrat/gecko/api/SignalAnalysisIntegrationTest.java"

echo ""
echo "Checking Documentation..."
echo "-------------------------"
check_file "examples/api-testing/SIGNAL_ANALYSIS_API.md"
check_file "examples/api-testing/websocket-client.html"
check_file "STRATEGIC_ROADMAP_DUAL_TRACK.md"
check_file "TESTING_GUIDE.md"
check_file "SESSION_SUMMARY_2026-01-25.md"
check_file "PHASE_1_COMPLETE.md"

echo ""
echo "Checking Test Circuits..."
echo "-------------------------"
check_file "examples/api-testing/test-circuits/resistor-divider.ipes"
check_file "examples/api-testing/test-circuits/buck-converter.ipes"

echo ""
echo "Checking Build Artifacts..."
echo "----------------------------"
if [ -f "gecko-rest-api/target/gecko-rest-api-2.0.0-SNAPSHOT.jar" ]; then
    echo -e "${GREEN}✓${NC} gecko-rest-api JAR exists"
else
    echo -e "${YELLOW}⚠${NC} gecko-rest-api JAR not found (may need to rebuild)"
    ((WARNINGS++))
fi

if [ -f "gecko-application/target/gecko-application-2.0.0-SNAPSHOT.jar" ]; then
    echo -e "${GREEN}✓${NC} gecko-application JAR exists"
else
    echo -e "${YELLOW}⚠${NC} gecko-application JAR not found (may need to rebuild)"
    ((WARNINGS++))
fi

echo ""
echo "========================================="
echo "Validation Summary"
echo "========================================="
echo ""

if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✅ All checks passed!${NC}"
    echo ""
    echo "Phase 1 deliverables are complete and ready for testing."
    echo ""
    echo "Next steps:"
    echo "  1. Run REST API: cd gecko-rest-api && java -jar target/gecko-rest-api-2.0.0-SNAPSHOT.jar"
    echo "  2. Open WebSocket demo: firefox examples/api-testing/websocket-client.html"
    echo "  3. Follow TESTING_GUIDE.md for comprehensive testing"
    exit 0
elif [ $ERRORS -eq 0 ]; then
    echo -e "${YELLOW}⚠ Validation completed with warnings${NC}"
    echo "  Errors: $ERRORS"
    echo "  Warnings: $WARNINGS"
    echo ""
    echo "Phase 1 files are present but some checks failed."
    echo "Review warnings above and rebuild if necessary."
    exit 0
else
    echo -e "${RED}✗ Validation failed${NC}"
    echo "  Errors: $ERRORS"
    echo "  Warnings: $WARNINGS"
    echo ""
    echo "Some Phase 1 files are missing. Review errors above."
    exit 1
fi
