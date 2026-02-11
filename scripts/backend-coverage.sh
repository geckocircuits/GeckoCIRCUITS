#!/bin/bash
# Backend Coverage Analysis Script for GeckoCIRCUITS
# Analyzes JaCoCo coverage for backend (non-GUI) packages

set -e

JACOCO_XML="target/site/jacoco/jacoco.xml"
JACOCO_HTML="target/site/jacoco/index.html"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Backend packages (pure logic, no GUI)
BACKEND_PACKAGES=(
    "ch.technokrat.gecko.geckocircuits.circuit.matrix"
    "ch.technokrat.gecko.geckocircuits.control.calculators"
    "ch.technokrat.gecko.geckocircuits.math"
    "ch.technokrat.gecko.geckocircuits.datacontainer"
    "ch.technokrat.gecko.geckocircuits.circuit.losscalculation"
    "ch.technokrat.gecko.geckocircuits.api"
)

# Mixed packages with significant backend logic
MIXED_PACKAGES=(
    "ch.technokrat.gecko.geckocircuits.newscope"
    "ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents"
    "ch.technokrat.gecko.geckocircuits.circuit"
    "ch.technokrat.gecko.geckocircuits.control"
)

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  GeckoCIRCUITS Backend Coverage Analysis${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Check if JaCoCo report exists
if [ ! -f "$JACOCO_XML" ]; then
    echo -e "${YELLOW}JaCoCo XML report not found. Generating...${NC}"
    mvn test jacoco:report -q
fi

# Function to extract coverage from JaCoCo XML
extract_coverage() {
    local package=$1
    local package_path=$(echo "$package" | tr '.' '/')

    # Try to find the package in the HTML report directory structure
    local html_file="target/site/jacoco/${package_path}/index.html"

    if [ -f "$html_file" ]; then
        # Extract instruction coverage percentage
        local cov=$(grep -oP 'class="ctr2"[^>]*>\K[0-9]+%' "$html_file" | head -1)
        if [ -n "$cov" ]; then
            echo "$cov"
            return
        fi
    fi

    # Fallback: parse from XML
    if [ -f "$JACOCO_XML" ]; then
        local missed=$(grep -A1 "name=\"$package\"" "$JACOCO_XML" 2>/dev/null | grep -oP 'missed="\K[0-9]+' | head -1)
        local covered=$(grep -A1 "name=\"$package\"" "$JACOCO_XML" 2>/dev/null | grep -oP 'covered="\K[0-9]+' | head -1)

        if [ -n "$missed" ] && [ -n "$covered" ]; then
            local total=$((missed + covered))
            if [ $total -gt 0 ]; then
                local pct=$((covered * 100 / total))
                echo "${pct}%"
                return
            fi
        fi
    fi

    echo "N/A"
}

# Function to get detailed stats from CSV
get_package_stats() {
    local package=$1
    local package_dir=$(echo "$package" | tr '.' '/')
    local csv_file="target/site/jacoco/${package_dir}/index.source.html"
    local html_file="target/site/jacoco/index.html"

    # Parse from main index.html
    if [ -f "$html_file" ]; then
        local pkg_short=$(echo "$package" | rev | cut -d'.' -f1 | rev)
        # Look for the package row
        grep -oP "${package}[^<]*</a></td><td[^>]*>[^<]*</td><td[^>]*>\K[0-9]+%" "$html_file" 2>/dev/null | head -1
    fi
}

echo -e "${GREEN}=== Pure Backend Packages ===${NC}"
echo ""
printf "%-55s %10s\n" "Package" "Coverage"
printf "%s\n" "-----------------------------------------------------------------------"

total_backend_covered=0
total_backend_total=0

for pkg in "${BACKEND_PACKAGES[@]}"; do
    cov=$(extract_coverage "$pkg")

    # Color based on coverage
    if [[ "$cov" == "N/A" ]]; then
        color=$YELLOW
    elif [[ "${cov%\%}" -ge 80 ]]; then
        color=$GREEN
    elif [[ "${cov%\%}" -ge 50 ]]; then
        color=$YELLOW
    else
        color=$RED
    fi

    printf "%-55s ${color}%10s${NC}\n" "$pkg" "$cov"
done

echo ""
echo -e "${GREEN}=== Mixed Packages (Backend + GUI) ===${NC}"
echo ""
printf "%-55s %10s\n" "Package" "Coverage"
printf "%s\n" "-----------------------------------------------------------------------"

for pkg in "${MIXED_PACKAGES[@]}"; do
    cov=$(extract_coverage "$pkg")

    if [[ "$cov" == "N/A" ]]; then
        color=$YELLOW
    elif [[ "${cov%\%}" -ge 80 ]]; then
        color=$GREEN
    elif [[ "${cov%\%}" -ge 50 ]]; then
        color=$YELLOW
    else
        color=$RED
    fi

    printf "%-55s ${color}%10s${NC}\n" "$pkg" "$cov"
done

echo ""
echo -e "${BLUE}=== Coverage Summary from JaCoCo Report ===${NC}"
echo ""

# Parse the total line from HTML
if [ -f "$JACOCO_HTML" ]; then
    echo "Extracting from JaCoCo HTML report..."
    echo ""

    # Get overall stats
    overall=$(grep -oP 'Total</td><td[^>]*>[^<]*</td><td[^>]*>\K[0-9]+%' "$JACOCO_HTML" | head -1)
    echo -e "Overall Project Coverage: ${YELLOW}${overall:-N/A}${NC}"

    # Calculate backend-specific coverage
    echo ""
    echo -e "${GREEN}Backend Package Details:${NC}"

    # Parse each backend package from the report
    for pkg in "${BACKEND_PACKAGES[@]}"; do
        pkg_name=$(echo "$pkg" | rev | cut -d'.' -f1 | rev)
        # Find the row for this package
        line=$(grep -o "${pkg_name}[^<]*</a></td>.*</tr>" "$JACOCO_HTML" 2>/dev/null | head -1)
        if [ -n "$line" ]; then
            instr_cov=$(echo "$line" | grep -oP 'ctr2"[^>]*>\K[0-9]+%' | head -1)
            branch_cov=$(echo "$line" | grep -oP 'ctr2"[^>]*>\K[0-9]+%' | head -2 | tail -1)
            echo "  $pkg_name: Instructions=$instr_cov, Branches=$branch_cov"
        fi
    done
fi

echo ""
echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}  Target: 85% Backend Coverage${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""
echo "Report location: $JACOCO_HTML"
echo ""

# Quick summary
echo -e "${GREEN}High Coverage (>80%):${NC}"
grep -oP 'el_package">[^<]+</a></td><td[^>]*>[^<]*</td><td class="ctr2"[^>]*>[89][0-9]%' "$JACOCO_HTML" 2>/dev/null | \
    sed 's/.*el_package">//;s/<\/a.*ctr2"[^>]*>/ : /' || echo "  (none found)"

echo ""
echo -e "${RED}Low Coverage (<20%):${NC}"
grep -oP 'el_package">[^<]+</a></td><td[^>]*>[^<]*</td><td class="ctr2"[^>]*>[0-1]?[0-9]%' "$JACOCO_HTML" 2>/dev/null | \
    sed 's/.*el_package">//;s/<\/a.*ctr2"[^>]*>/ : /' | head -10 || echo "  (none found)"
