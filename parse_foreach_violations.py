#!/usr/bin/env python3
"""Parse ForLoopCanBeForeach violations from PMD XML report."""

import xml.etree.ElementTree as ET
from collections import defaultdict

def parse_violations(xml_file):
    tree = ET.parse(xml_file)
    root = tree.getroot()

    # Define namespace
    ns = {'pmd': 'http://pmd.sourceforge.net/report/2.0.0'}

    violations_by_file = defaultdict(list)

    for file_elem in root.findall('pmd:file', ns):
        file_path = file_elem.get('name')

        for violation in file_elem.findall('pmd:violation', ns):
            if violation.get('rule') == 'ForLoopCanBeForeach':
                violations_by_file[file_path].append({
                    'line': int(violation.get('beginline')),
                    'endline': int(violation.get('endline')),
                    'method': violation.get('method'),
                    'class': violation.get('class')
                })

    return violations_by_file

def main():
    violations = parse_violations('/home/tinix/claude_wsl/GeckoCIRCUITS/target/pmd.xml')

    print(f"Total files with violations: {len(violations)}")
    print(f"Total violations: {sum(len(v) for v in violations.values())}\n")

    # Group by directory for batch processing
    for file_path in sorted(violations.keys()):
        viols = violations[file_path]
        print(f"\n{file_path}")
        print(f"  {len(viols)} violation(s)")
        for v in sorted(viols, key=lambda x: x['line']):
            print(f"    Line {v['line']}-{v['endline']}: {v['class']}.{v['method']}")

if __name__ == '__main__':
    main()
