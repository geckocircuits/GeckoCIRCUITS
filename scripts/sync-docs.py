#!/usr/bin/env python3
"""
Sync documentation from resources/ to docs/ for MkDocs site.
Converts README.md files and updates links for the web documentation.
"""

import os
import re
import shutil
from pathlib import Path
from urllib.parse import urlsplit, urlunsplit

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
    "tutorials/6xx_emi_emc": "tutorials/emi",
    "tutorials/8xx_advanced_topics": "tutorials/advanced",
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

LEGACY_DOC_TARGETS = {
    "201_buck_converter/index.md": "tutorials/dcdc/buck-converter.md",
    "202_boost_converter/index.md": "tutorials/dcdc/boost-converter.md",
    "203_buck_boost/index.md": "tutorials/dcdc/buck-boost.md",
    "301_diode_rectifier/index.md": "tutorials/acdc/diode-rectifier.md",
    "302_pfc_basics/index.md": "tutorials/acdc/pfc-basics.md",
    "303_vienna_rectifier/index.md": "tutorials/acdc/vienna-rectifier.md",
    "401_single_phase_inverter/index.md": "tutorials/dcac/single-phase.md",
    "402_three_phase_inverter/index.md": "tutorials/dcac/three-phase.md",
    "403_npc_inverter/index.md": "tutorials/dcac/npc-inverter.md",
    "501_loss_calculation/index.md": "tutorials/thermal/loss-calculation.md",
    "502_junction_temperature/index.md": "tutorials/thermal/junction-temperature.md",
    "503_heatsink_design/index.md": "tutorials/thermal/heatsink-design.md",
    "103_pwm_basics/index.md": "getting-started/pwm-basics.md",
    "702_matlab_integration/index.md": "tutorials/scripting/matlab.md",
    "704_java_blocks/index.md": "tutorials/scripting/java-blocks.md",
    "llc_resonant/index.md": "examples/power-supplies/llc.md",
    "full_bridge/index.md": "examples/power-supplies/dab.md",
    "multilevel_npc/index.md": "tutorials/dcac/npc-inverter.md",
    "three_phase_vsi/index.md": "tutorials/dcac/three-phase.md",
    "motor_drives/index.md": "examples/motor-drives/index.md",
    "examples/inverters/index.md": "examples/inverters/index.md",
    "802_motor_drives_pmsm/index.md": "examples/motor-drives/pmsm-foc.md",
    "801_matrix_converters/index.md": "tutorials/advanced/index.md#801-matrix-converters",
    "2xx_dcdc_converters/index.md": "tutorials/dcdc/index.md",
    "3xx_acdc_rectifiers/index.md": "tutorials/acdc/index.md",
    "9xx_magnetics_mechanical/index.md": "tutorials/magnetics/index.md",
    "examples/basic_topologies/index.md": "examples/basic/index.md",
}


def mapped_doc_path(resource: Path) -> Path | None:
    """Map a resource path to the corresponding generated page."""
    try:
        relative = resource.resolve().relative_to(RESOURCES_DIR.resolve())
    except ValueError:
        return None
    for source, destination in SYNC_MAP.items():
        if relative == Path(source) or Path(source) in relative.parents:
            target = DOCS_DIR / destination / relative.relative_to(source)
            if target.name == "README.md":
                target = target.with_name("index.md")
            elif resource.is_dir():
                target = target / "index.md"
            return target
    return None


def convert_links(content: str, source_path: Path, destination_path: Path | None = None) -> str:
    """Resolve resource links relative to the generated page, preserving examples."""
    destination_path = destination_path or mapped_doc_path(source_path)
    if destination_path is None:
        return content

    def replace(match):
        label, link = match.groups()
        parts = urlsplit(link)
        if parts.scheme or parts.netloc or not parts.path or parts.path.startswith('/'):
            return match.group(0)
        resource = (source_path.parent / parts.path).resolve()
        if resource.suffix == '.ipes' and resource.is_file():
            try:
                relative = resource.relative_to(RESOURCES_DIR.resolve()).as_posix()
            except ValueError:
                return match.group(0)
            return f'[{label}](https://github.com/tinix84/GeckoCIRCUITS/blob/main/resources/{relative})'
        target = None
        fragment = parts.fragment
        page_path = parts.path + 'index.md' if parts.path.endswith('/') else parts.path
        for legacy, replacement in sorted(LEGACY_DOC_TARGETS.items(), key=lambda item: -len(item[0])):
            if page_path == legacy or page_path.endswith('/' + legacy):
                mapped = urlsplit(replacement)
                target = DOCS_DIR / mapped.path
                fragment = mapped.fragment or fragment
                if legacy == 'full_bridge/index.md' and label == 'Full Bridge':
                    label = 'Dual Active Bridge'
                break
        if target is None:
            target = mapped_doc_path(resource)
        if target is None or not target.is_file():
            # Some source READMEs omit a parent level before a resource root.
            for source in SYNC_MAP:
                offset = page_path.find(source + '/')
                if offset >= 0 and (offset == 0 or page_path[offset - 1] == '/'):
                    target = mapped_doc_path(RESOURCES_DIR / page_path[offset:])
                    break
        if target is None or not target.is_file():
            candidate = (destination_path.parent / parts.path).resolve()
            target = candidate / 'index.md' if candidate.is_dir() else candidate
        if not target.is_file():
            return match.group(0)
        relative = Path(os.path.relpath(target, destination_path.parent)).as_posix()
        return f'[{label}]({urlunsplit(("", "", relative, parts.query, fragment))})'

    sections = re.split(r'(^```[^\n]*\n.*?^```[^\n]*(?:\n|$))', content, flags=re.MULTILINE | re.DOTALL)
    return ''.join(section if index % 2 else re.sub(r'\[([^\]]+)\]\(([^)]+)\)', replace, section)
                   for index, section in enumerate(sections))

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
    content = convert_links(content, src, dst)

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

ARTICLES_DIR = RESOURCES_DIR / "articles"
ARTICLES_SKIP = {"temp.md", "Readme.md"}


def sync_articles():
    """Sync individual article .md files from resources/articles/ to docs/articles/."""
    if not ARTICLES_DIR.exists():
        print(f"  Skipping articles (not found)")
        return

    dst_dir = DOCS_DIR / "articles"
    dst_dir.mkdir(parents=True, exist_ok=True)

    print(f"\nSyncing articles:")
    print(f"  from: {ARTICLES_DIR.relative_to(PROJECT_ROOT)}")
    print(f"    to: {dst_dir.relative_to(PROJECT_ROOT)}")

    for src_file in sorted(ARTICLES_DIR.glob("*.md")):
        if src_file.name in ARTICLES_SKIP:
            continue
        # Sanitize filename for web: lowercase, replace spaces with hyphens
        safe_name = src_file.stem.lower().replace(" ", "-").replace("_", "-")
        # Remove special characters but keep hyphens and alphanumerics
        safe_name = re.sub(r'[^a-z0-9\-]', '', safe_name)
        # Collapse multiple hyphens
        safe_name = re.sub(r'-+', '-', safe_name).strip('-')
        dst_file = dst_dir / (safe_name + ".md")
        sync_file(src_file, dst_file)

    # Copy article assets (images, ipes files)
    for asset_dir in ("img", "ipes_files"):
        src_assets = ARTICLES_DIR / asset_dir
        if src_assets.exists():
            dst_assets = dst_dir / asset_dir
            shutil.copytree(src_assets, dst_assets, dirs_exist_ok=True)
            file_count = sum(1 for _ in dst_assets.iterdir())
            print(f"  Copied {file_count} files from {asset_dir}/")


def main():
    print("=" * 60)
    print("GeckoCIRCUITS Documentation Sync")
    print("=" * 60)

    # Sync each mapped directory
    for src_rel, dst_rel in SYNC_MAP.items():
        src_dir = RESOURCES_DIR / src_rel
        dst_dir = DOCS_DIR / dst_rel
        sync_directory(src_dir, dst_dir)

    # NOTE: tutorials/index.md and examples/index.md are hand-maintained in docs/
    # because the resources/ READMEs use old-format paths (1xx_getting_started/,
    # basic_topologies/) that don't match the docs site structure (dcdc/, basic/).
    # Do NOT sync them automatically.
    print("\nSkipping tutorials/index.md and examples/index.md (hand-maintained)")

    # Sync articles
    sync_articles()

    print("\n" + "=" * 60)
    print("Sync complete!")
    print("Run 'mkdocs serve' to preview the documentation.")
    print("=" * 60)

if __name__ == "__main__":
    main()
