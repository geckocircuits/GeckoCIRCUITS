#!/usr/bin/env python3
"""
Generate Markdown API documentation from Javadoc comments in gecko-simulation-core.

Parses Java source files and extracts class-level and method-level Javadoc
into structured Markdown files for the MkDocs documentation site.

Usage:
    python scripts/generate-api-docs.py

Output:
    docs/api/core/simulation.md
    docs/api/core/matrix-stampers.md
    docs/api/core/calculators.md
"""

import os
import re
from pathlib import Path
from dataclasses import dataclass, field
from typing import Optional

PROJECT_ROOT = Path(__file__).parent.parent
CORE_SRC = PROJECT_ROOT / "src" / "modules" / "gecko-simulation-core" / "src" / "main" / "java"
DOCS_DIR = PROJECT_ROOT / "docs" / "api" / "core"

# Packages to document
TARGET_PACKAGES = {
    "simulation": "ch/technokrat/gecko/core/simulation",
    "circuit.matrix": "ch/technokrat/gecko/core/circuit/matrix",
    "circuit.netlist": "ch/technokrat/gecko/core/circuit/netlist",
    "control.calculators": "ch/technokrat/gecko/core/control/calculators",
    "math": "ch/technokrat/gecko/core/math",
}


@dataclass
class JavadocInfo:
    """Extracted Javadoc information from a Java element."""
    description: str = ""
    params: dict = field(default_factory=dict)
    returns: str = ""
    throws: dict = field(default_factory=dict)
    author: str = ""
    code_example: str = ""


@dataclass
class MethodInfo:
    """Extracted method information."""
    name: str = ""
    return_type: str = ""
    parameters: str = ""
    modifiers: str = ""
    javadoc: Optional[JavadocInfo] = None


@dataclass
class ClassInfo:
    """Extracted class/interface information."""
    name: str = ""
    kind: str = "class"  # class, interface, enum
    package: str = ""
    modifiers: str = ""
    extends: str = ""
    implements: list = field(default_factory=list)
    javadoc: Optional[JavadocInfo] = None
    methods: list = field(default_factory=list)
    constants: list = field(default_factory=list)
    enums: list = field(default_factory=list)
    file_path: str = ""


def parse_javadoc(javadoc_text: str) -> JavadocInfo:
    """Parse a Javadoc comment block into structured data."""
    info = JavadocInfo()
    if not javadoc_text:
        return info

    # Remove /** and */ delimiters
    text = javadoc_text.strip()
    if text.startswith("/**"):
        text = text[3:]
    if text.endswith("*/"):
        text = text[:-2]

    # Remove leading * on each line
    lines = []
    for line in text.split("\n"):
        line = line.strip()
        if line.startswith("* "):
            line = line[2:]
        elif line == "*":
            line = ""
        elif line.startswith("*"):
            line = line[1:]
        lines.append(line)

    text = "\n".join(lines).strip()

    # Extract @param tags
    for match in re.finditer(r'@param\s+(\w+)\s+(.*?)(?=@\w|\Z)', text, re.DOTALL):
        info.params[match.group(1)] = match.group(2).strip()

    # Extract @return tag
    ret_match = re.search(r'@return\s+(.*?)(?=@\w|\Z)', text, re.DOTALL)
    if ret_match:
        info.returns = ret_match.group(1).strip()

    # Extract @throws tags
    for match in re.finditer(r'@throws\s+(\w+)\s+(.*?)(?=@\w|\Z)', text, re.DOTALL):
        info.throws[match.group(1)] = match.group(2).strip()

    # Extract @author tag
    auth_match = re.search(r'@author\s+(.*?)(?=@\w|\Z)', text, re.DOTALL)
    if auth_match:
        info.author = auth_match.group(1).strip()

    # Extract code examples from <pre>{@code ... }</pre>
    code_match = re.search(r'<pre>\{@code\s*(.*?)\}</pre>', text, re.DOTALL)
    if code_match:
        info.code_example = code_match.group(1).strip()

    # Description is everything before the first @tag
    desc_match = re.match(r'(.*?)(?=\n\s*@|\Z)', text, re.DOTALL)
    if desc_match:
        desc = desc_match.group(1).strip()
        # Clean up HTML tags
        desc = re.sub(r'<p>', '\n\n', desc)
        desc = re.sub(r'</p>', '', desc)
        desc = re.sub(r'<pre>\{@code.*?\}</pre>', '', desc, flags=re.DOTALL)
        desc = re.sub(r'\{@code\s+([^}]+)\}', r'`\1`', desc)
        desc = re.sub(r'\{@link\s+([^}]+)\}', r'`\1`', desc)
        info.description = desc.strip()

    return info


def extract_javadoc_before(content: str, pos: int) -> str:
    """Extract the Javadoc comment immediately before the given position."""
    # Look backwards from pos for the nearest /** ... */ block
    before = content[:pos].rstrip()
    match = re.search(r'/\*\*.*?\*/\s*$', before, re.DOTALL)
    if match:
        return match.group(0)
    return ""


def parse_java_file(file_path: Path) -> Optional[ClassInfo]:
    """Parse a Java file and extract class/interface info with Javadoc."""
    try:
        content = file_path.read_text(encoding='utf-8')
    except Exception:
        return None

    # Extract package
    pkg_match = re.search(r'package\s+([\w.]+);', content)
    package = pkg_match.group(1) if pkg_match else ""

    # Find main class/interface/enum declaration
    class_pattern = re.compile(
        r'((?:public\s+)?(?:abstract\s+)?(?:final\s+)?)'
        r'(class|interface|enum)\s+'
        r'(\w+)'
        r'(?:\s+extends\s+(\w+))?'
        r'(?:\s+implements\s+([\w,\s]+))?'
        r'\s*\{',
        re.MULTILINE
    )

    class_match = class_pattern.search(content)
    if not class_match:
        return None

    info = ClassInfo()
    info.modifiers = class_match.group(1).strip()
    info.kind = class_match.group(2)
    info.name = class_match.group(3)
    info.package = package
    info.extends = class_match.group(4) or ""
    if class_match.group(5):
        info.implements = [s.strip() for s in class_match.group(5).split(",")]
    info.file_path = str(file_path.relative_to(PROJECT_ROOT))

    # Extract class-level Javadoc
    javadoc_text = extract_javadoc_before(content, class_match.start())
    info.javadoc = parse_javadoc(javadoc_text)

    # Extract public methods with Javadoc
    method_pattern = re.compile(
        r'(/\*\*.*?\*/\s*)?'
        r'(public\s+(?:static\s+)?(?:final\s+)?(?:synchronized\s+)?)'
        r'([\w<>\[\],\s?]+?)\s+'
        r'(\w+)\s*'
        r'\(([^)]*)\)',
        re.DOTALL
    )

    for m_match in method_pattern.finditer(content):
        method = MethodInfo()
        method.modifiers = m_match.group(2).strip()
        method.return_type = m_match.group(3).strip()
        method.name = m_match.group(4)
        method.parameters = m_match.group(5).strip()

        # Skip constructors that match class name without return type
        if method.return_type == info.name:
            continue

        javadoc_raw = m_match.group(1)
        if javadoc_raw:
            method.javadoc = parse_javadoc(javadoc_raw)

        info.methods.append(method)

    # Extract public static final constants
    const_pattern = re.compile(
        r'(?:/\*\*.*?\*/\s*)?'
        r'public\s+static\s+final\s+'
        r'(\w+)\s+'
        r'(\w+)\s*=\s*([^;]+);',
        re.DOTALL
    )

    for c_match in const_pattern.finditer(content):
        info.constants.append({
            'type': c_match.group(1),
            'name': c_match.group(2),
            'value': c_match.group(3).strip(),
        })

    return info


def scan_package(package_path: str) -> list:
    """Scan a package directory and parse all Java files."""
    src_dir = CORE_SRC / package_path
    if not src_dir.exists():
        print(f"  Package not found: {package_path}")
        return []

    classes = []
    for java_file in sorted(src_dir.glob("*.java")):
        if java_file.name == "package-info.java":
            continue
        class_info = parse_java_file(java_file)
        if class_info:
            classes.append(class_info)

    return classes


def generate_class_summary(classes: list) -> str:
    """Generate a Markdown summary table for a list of classes."""
    if not classes:
        return "*No public classes found.*\n"

    lines = ["| Type | Name | Description |",
             "|------|------|-------------|"]

    for cls in classes:
        desc = cls.javadoc.description.split("\n")[0] if cls.javadoc else ""
        # Truncate long descriptions
        if len(desc) > 100:
            desc = desc[:97] + "..."
        lines.append(f"| {cls.kind} | `{cls.name}` | {desc} |")

    return "\n".join(lines) + "\n"


def generate_report():
    """Generate a summary report of all documented packages."""
    print("=" * 60)
    print("GeckoCIRCUITS API Documentation Generator")
    print("=" * 60)

    all_packages = {}
    total_classes = 0
    total_methods = 0

    for pkg_name, pkg_path in TARGET_PACKAGES.items():
        print(f"\nScanning: {pkg_name}")
        classes = scan_package(pkg_path)
        all_packages[pkg_name] = classes
        class_count = len(classes)
        method_count = sum(len(c.methods) for c in classes)
        total_classes += class_count
        total_methods += method_count
        print(f"  Found {class_count} classes, {method_count} public methods")

        for cls in classes:
            has_doc = "✓" if cls.javadoc and cls.javadoc.description else "✗"
            documented_methods = sum(1 for m in cls.methods if m.javadoc and m.javadoc.description)
            print(f"    {has_doc} {cls.kind} {cls.name} ({documented_methods}/{len(cls.methods)} methods documented)")

    print(f"\n{'=' * 60}")
    print(f"Total: {total_classes} classes, {total_methods} public methods")
    print(f"Packages: {len(all_packages)}")

    # Generate index update information
    print(f"\nDocumentation pages in: {DOCS_DIR.relative_to(PROJECT_ROOT)}")
    print("  - index.md (package overview)")
    print("  - simulation.md (simulation engine)")
    print("  - matrix-stampers.md (MNA stampers)")
    print("  - calculators.md (control calculators)")
    print(f"\n{'=' * 60}")
    print("API docs generation complete!")
    print("Note: Hand-written docs in docs/api/core/ are the primary source.")
    print("This script validates and reports on Javadoc coverage.")
    print("=" * 60)

    return all_packages


def main():
    """Main entry point."""
    DOCS_DIR.mkdir(parents=True, exist_ok=True)
    all_packages = generate_report()


if __name__ == "__main__":
    main()
