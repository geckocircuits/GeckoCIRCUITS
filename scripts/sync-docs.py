#!/usr/bin/env python3
"""
Sync documentation from resources/ to docs/ for MkDocs site.
Converts README.md files and updates links for the web documentation.
"""

import os
import re
import shutil
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent
RESOURCES_DIR = PROJECT_ROOT / "resources"
DOCS_DIR = PROJECT_ROOT / "docs"

# Mapping from resources structure to docs structure
SYNC_MAP = {
    # Tutorials
    "tutorials/1xx_getting_started": "getting-started",
    "tutorials/2xx_dcdc_converters": "tutorials/dcdc",
    "tutorials/3xx_acdc_rectifiers": "tutorials/acdc",
    "tutorials/4xx_dcac_inverters": "tutorials/dcac",
    "tutorials/5xx_thermal_simulation": "tutorials/thermal",
    "tutorials/7xx_scripting_automation": "tutorials/scripting",
    "tutorials/9xx_magnetics_mechanical": "tutorials/magnetics",
    # Examples
    "examples/basic_topologies": "examples/basic",
    "examples/power_supplies": "examples/power-supplies",
    "examples/motor_drives": "examples/motor-drives",
    "examples/automotive": "examples/automotive",
    "examples/thermal": "examples/thermal",
    "examples/renewable_energy": "examples/renewable",
    "examples/inverters": "examples/inverters",
    "examples/rectifiers": "examples/rectifiers",
}

# File renaming rules
FILE_RENAME = {
    "README.md": "index.md",
}

def convert_links(content: str, source_path: Path) -> str:
    """Convert relative links for web documentation."""
    # Convert .ipes references to download links
    content = re.sub(
        r'\[([^\]]+)\]\(([^)]+\.ipes)\)',
        r'[\1](https://github.com/geckocircuits/geckocircuits/blob/main/resources/\2)',
        content
    )

    # Convert relative tutorial links
    content = re.sub(
        r'\.\./\.\./tutorials/',
        '../tutorials/',
        content
    )

    # Convert relative example links
    content = re.sub(
        r'\.\./\.\./examples/',
        '../examples/',
        content
    )

    return content

def add_frontmatter(content: str, title: str) -> str:
    """Add MkDocs frontmatter if not present."""
    if content.startswith('---'):
        return content

    # Extract title from first heading
    match = re.search(r'^#\s+(.+)$', content, re.MULTILINE)
    if match:
        title = match.group(1)

    frontmatter = f"""---
title: {title}
---

"""
    return frontmatter + content

def sync_file(src: Path, dst: Path):
    """Copy and convert a single file."""
    print(f"  {src.name} -> {dst.name}")

    content = src.read_text(encoding='utf-8')

    # Convert links
    content = convert_links(content, src)

    # Add frontmatter
    title = dst.stem.replace('-', ' ').replace('_', ' ').title()
    content = add_frontmatter(content, title)

    # Ensure parent directory exists
    dst.parent.mkdir(parents=True, exist_ok=True)

    # Write converted file
    dst.write_text(content, encoding='utf-8')

def sync_directory(src_dir: Path, dst_dir: Path):
    """Sync a directory from resources to docs."""
    if not src_dir.exists():
        print(f"  Skipping {src_dir} (not found)")
        return

    print(f"\nSyncing: {src_dir.relative_to(PROJECT_ROOT)}")
    print(f"     to: {dst_dir.relative_to(PROJECT_ROOT)}")

    # Find all README.md files
    for src_file in src_dir.rglob("README.md"):
        # Calculate relative path
        rel_path = src_file.relative_to(src_dir)

        # Determine destination path
        dst_file = dst_dir / rel_path

        # Apply file renaming
        if dst_file.name in FILE_RENAME:
            dst_file = dst_file.parent / FILE_RENAME[dst_file.name]

        sync_file(src_file, dst_file)

def main():
    print("=" * 60)
    print("GeckoCIRCUITS Documentation Sync")
    print("=" * 60)

    # Sync each mapped directory
    for src_rel, dst_rel in SYNC_MAP.items():
        src_dir = RESOURCES_DIR / src_rel
        dst_dir = DOCS_DIR / dst_rel
        sync_directory(src_dir, dst_dir)

    # Copy main README files
    print("\nSyncing main index files...")

    # Tutorials main README
    tutorials_readme = RESOURCES_DIR / "tutorials" / "README.md"
    if tutorials_readme.exists():
        sync_file(tutorials_readme, DOCS_DIR / "tutorials" / "index.md")

    # Examples main README
    examples_readme = RESOURCES_DIR / "examples" / "README.md"
    if examples_readme.exists():
        sync_file(examples_readme, DOCS_DIR / "examples" / "index.md")

    print("\n" + "=" * 60)
    print("Sync complete!")
    print("Run 'mkdocs serve' to preview the documentation.")
    print("=" * 60)

if __name__ == "__main__":
    main()
