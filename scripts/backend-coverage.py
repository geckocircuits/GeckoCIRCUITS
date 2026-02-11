#!/usr/bin/env python3
"""
Backend Coverage Analysis Script for GeckoCIRCUITS
Analyzes JaCoCo coverage for backend (non-GUI) packages
"""

import os
import re
import sys
from pathlib import Path

# Backend packages (pure logic, minimal/no GUI)
BACKEND_PACKAGES = [
    "ch.technokrat.gecko.geckocircuits.circuit.matrix",
    "ch.technokrat.gecko.geckocircuits.control.calculators",
    "ch.technokrat.gecko.geckocircuits.math",
    "ch.technokrat.gecko.geckocircuits.datacontainer",
    "ch.technokrat.gecko.geckocircuits.circuit.losscalculation",
    "ch.technokrat.gecko.geckocircuits.api",
    "ch.technokrat.gecko.i18n",
    "ch.technokrat.gecko.i18n.resources",
]

# Mixed packages (have both backend logic and GUI)
MIXED_PACKAGES = [
    "ch.technokrat.gecko.geckocircuits.newscope",
    "ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents",
    "ch.technokrat.gecko.geckocircuits.circuit",
    "ch.technokrat.gecko.geckocircuits.control",
    "ch.technokrat.gecko.geckocircuits.allg",
    "ch.technokrat.modelviewcontrol",
]

# GUI-heavy packages (mostly UI, low priority for backend testing)
GUI_PACKAGES = [
    "ch.technokrat.gecko.geckocircuits.scope",
    "ch.technokrat.gecko.geckocircuits.control.javablock",
    "ch.technokrat.gecko.i18n.translationtoolbox",
    "ch.technokrat.gecko.i18n.bot",
    "ch.technokrat.gecko.geckoscript",
    "ch.technokrat.gecko",
    "ch.technokrat.gecko.geckocircuits.nativec",
]

def parse_jacoco_html(html_path):
    """Parse JaCoCo HTML report to extract coverage data."""
    if not os.path.exists(html_path):
        return None

    with open(html_path, 'r') as f:
        content = f.read()

    coverage_data = {}

    # Pattern: find each package link and its coverage percentages
    # The bar cell contains <img> tags, so we need to match across them
    # Format: <a class="el_package">pkg.name</a></td><td class="bar">...</td><td class="ctr2">YY%</td><td class="bar">...</td><td class="ctr2">ZZ%</td>
    pattern = r'class="el_package">([^<]+)</a></td><td class="bar"[^>]*>.*?</td><td class="ctr2"[^>]*>(\d+)%</td><td class="bar"[^>]*>.*?</td><td class="ctr2"[^>]*>(\d+|n/a)%?</td>'

    for match in re.finditer(pattern, content):
        pkg_name = match.group(1)
        instr_cov = int(match.group(2))
        branch_str = match.group(3)
        branch_cov = int(branch_str) if branch_str != 'n/a' else 0

        coverage_data[pkg_name] = {
            'instructions': instr_cov,
            'branches': branch_cov,
        }

    # Extract total
    total_pattern = r'<td>Total</td><td[^>]*>([^<]+)</td><td class="ctr2"[^>]*>(\d+)%</td><td[^>]*>[^<]*</td><td class="ctr2"[^>]*>(\d+)%</td>'
    total_match = re.search(total_pattern, content)
    if total_match:
        coverage_data['_TOTAL_'] = {
            'missed_info': total_match.group(1),
            'instructions': int(total_match.group(2)),
            'branches': int(total_match.group(3)),
        }

    return coverage_data


def color(text, code):
    """Apply ANSI color to text."""
    colors = {
        'red': '\033[0;31m',
        'green': '\033[0;32m',
        'yellow': '\033[1;33m',
        'blue': '\033[0;34m',
        'cyan': '\033[0;36m',
        'reset': '\033[0m',
        'bold': '\033[1m',
    }
    return f"{colors.get(code, '')}{text}{colors['reset']}"


def coverage_color(pct):
    """Return color based on coverage percentage."""
    if pct >= 80:
        return 'green'
    elif pct >= 50:
        return 'yellow'
    else:
        return 'red'


def print_section(title, packages, coverage_data):
    """Print a section of coverage data."""
    print(f"\n{color(f'=== {title} ===', 'cyan')}\n")
    print(f"{'Package':<55} {'Instr%':>8} {'Branch%':>8}")
    print("-" * 73)

    total_instr = 0
    count = 0

    for pkg in packages:
        if pkg in coverage_data:
            data = coverage_data[pkg]
            instr = data['instructions']
            branch = data['branches']
            total_instr += instr
            count += 1

            instr_str = color(f"{instr:>7}%", coverage_color(instr))
            branch_str = color(f"{branch:>7}%", coverage_color(branch))
            print(f"{pkg:<55} {instr_str} {branch_str}")
        else:
            print(f"{pkg:<55} {color('     N/A', 'yellow')} {color('     N/A', 'yellow')}")

    if count > 0:
        avg = total_instr / count
        print("-" * 73)
        avg_str = color(f"{avg:>6.1f}%", coverage_color(avg))
        print(f"{'Average':<55} {avg_str}")

    return total_instr, count


def main():
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    jacoco_html = project_root / "target" / "site" / "jacoco" / "index.html"

    print(color("=" * 60, 'blue'))
    print(color("  GeckoCIRCUITS Backend Coverage Analysis", 'blue'))
    print(color("=" * 60, 'blue'))

    if not jacoco_html.exists():
        print(color("\nJaCoCo report not found. Run: mvn test jacoco:report", 'yellow'))
        sys.exit(1)

    coverage_data = parse_jacoco_html(jacoco_html)

    if not coverage_data:
        print(color("\nFailed to parse JaCoCo report.", 'red'))
        sys.exit(1)

    # Print overall stats
    if '_TOTAL_' in coverage_data:
        total = coverage_data['_TOTAL_']
        print(f"\n{color('Overall Project:', 'cyan')} {total['missed_info']}")
        instr_pct = total['instructions']
        branch_pct = total['branches']
        print(f"  Instructions: {color(str(instr_pct) + '%', coverage_color(instr_pct))}")
        print(f"  Branches:     {color(str(branch_pct) + '%', coverage_color(branch_pct))}")

    # Print sections
    backend_instr, backend_count = print_section("Pure Backend Packages", BACKEND_PACKAGES, coverage_data)
    mixed_instr, mixed_count = print_section("Mixed Packages (Backend + GUI)", MIXED_PACKAGES, coverage_data)
    print_section("GUI-Heavy Packages (Low Priority)", GUI_PACKAGES, coverage_data)

    # Calculate backend average
    print(f"\n{color('=' * 60, 'blue')}")
    print(color("  Backend Coverage Summary", 'blue'))
    print(color("=" * 60, 'blue'))

    if backend_count > 0:
        backend_avg = backend_instr / backend_count
        print(f"\n{color('Pure Backend Average:', 'bold')} {color(f'{backend_avg:.1f}%', coverage_color(backend_avg))}")

    if backend_count + mixed_count > 0:
        all_backend_avg = (backend_instr + mixed_instr) / (backend_count + mixed_count)
        print(f"{color('All Backend (incl. mixed):', 'bold')} {color(f'{all_backend_avg:.1f}%', coverage_color(all_backend_avg))}")

    # Target analysis
    print(f"\n{color('=' * 60, 'cyan')}")
    print(color("  Gap Analysis: Target 85% Backend Coverage", 'cyan'))
    print(color("=" * 60, 'cyan'))

    print("\nPackages needing most improvement:")

    all_packages = BACKEND_PACKAGES + MIXED_PACKAGES
    needs_work = []
    for pkg in all_packages:
        if pkg in coverage_data:
            instr = coverage_data[pkg]['instructions']
            if instr < 85:
                needs_work.append((pkg, instr, 85 - instr))

    needs_work.sort(key=lambda x: -x[2])  # Sort by gap descending

    print(f"\n{'Package':<40} {'Current':>10} {'Target':>10} {'Gap':>10}")
    print("-" * 72)
    for pkg, current, gap in needs_work:
        pkg_short = pkg.split('.')[-1]
        current_str = color(f"{current}%", coverage_color(current))
        print(f"{pkg_short:<40} {current_str:>18} {'85%':>10} {color(f'+{gap}%', 'red'):>18}")

    print(f"\n{color('Report:', 'cyan')} {jacoco_html}")


if __name__ == "__main__":
    main()
