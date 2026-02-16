#!/usr/bin/env python3
"""
Fix broken internal links in MkDocs documentation.
Handles missing /index.md suffixes and incorrect directory names.

Usage:
    python scripts/fix-broken-links.py
    python scripts/fix-broken-links.py --dry-run  # Preview changes
"""

import argparse
import re
from pathlib import Path
from typing import List, Tuple

PROJECT_ROOT = Path(__file__).parent.parent
DOCS_DIR = PROJECT_ROOT / "docs"

# Fix patterns: (search_pattern, replacement_pattern, description)
FIX_PATTERNS = [
    # Pattern 1: Add /index.md to directory links (66 occurrences)
    (
        r'\]\((\.\./[^)]+)/\)',
        r'](\1/index.md)',
        "Add /index.md to directory links"
    ),

    # Pattern 2: Fix basic_topologies -> basic (8 occurrences)
    (
        r'\]\((\.\./?)basic_topologies/',
        r'](\1basic/',
        "Fix basic_topologies -> basic"
    ),

    # Pattern 3: Fix motor_drives -> motor-drives (3 occurrences)
    (
        r'\]\((\.\./?)motor_drives/',
        r'](\1motor-drives/',
        "Fix motor_drives -> motor-drives"
    ),

    # Pattern 4: Fix power_supplies -> power-supplies (2 occurrences)
    (
        r'\]\((\.\./?)power_supplies/',
        r'](\1power-supplies/',
        "Fix power_supplies -> power-supplies"
    ),
]


def find_markdown_files(directory: Path) -> List[Path]:
    """Find all markdown files in directory."""
    return list(directory.rglob("*.md"))


def apply_fixes(content: str, filepath: Path, dry_run: bool = False) -> Tuple[str, int]:
    """Apply all fix patterns to content."""
    fixed_content = content
    total_fixes = 0

    for pattern, replacement, description in FIX_PATTERNS:
        matches = re.findall(pattern, fixed_content)
        if matches:
            fixed_content = re.sub(pattern, replacement, fixed_content)
            count = len(matches)
            total_fixes += count
            if not dry_run and count > 0:
                print(f"  ✓ {description}: {count} fixes in {filepath.name}")

    return fixed_content, total_fixes


def main():
    """Main function to fix all broken links."""
    parser = argparse.ArgumentParser(description="Fix broken documentation links")
    parser.add_argument("--dry-run", action="store_true", help="Preview changes without writing")
    args = parser.parse_args()

    print("=" * 60)
    print("GeckoCIRCUITS Broken Link Fixer")
    print("=" * 60)
    print()

    if args.dry_run:
        print("⚠️  DRY RUN MODE - No files will be modified")
        print()

    # Find all markdown files
    md_files = find_markdown_files(DOCS_DIR)
    print(f"📁 Found {len(md_files)} markdown files")
    print()

    # Process each file
    total_files_fixed = 0
    total_links_fixed = 0

    for filepath in md_files:
        # Skip releases directory (already clean)
        if "releases" in filepath.parts:
            continue

        try:
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()

            fixed_content, fixes_count = apply_fixes(content, filepath, args.dry_run)

            if fixes_count > 0:
                total_files_fixed += 1
                total_links_fixed += fixes_count

                if not args.dry_run:
                    with open(filepath, "w", encoding="utf-8") as f:
                        f.write(fixed_content)
                else:
                    print(f"  [DRY RUN] Would fix {fixes_count} links in {filepath.name}")

        except Exception as e:
            print(f"  ⚠️  Error processing {filepath}: {e}")

    print()
    print("=" * 60)
    print("✅ Summary")
    print("=" * 60)
    print(f"📝 Files processed: {len(md_files)}")
    print(f"🔧 Files fixed: {total_files_fixed}")
    print(f"🔗 Links fixed: {total_links_fixed}")
    print()

    if args.dry_run:
        print("(Dry run - no files were modified)")
        print("Run without --dry-run to apply fixes")
    else:
        print("✓ All broken links have been fixed!")
        print()
        print("Next steps:")
        print("1. Run: mkdocs build --strict")
        print("2. Run: mkdocs serve (preview locally)")
        print("3. Run: mkdocs gh-deploy --force (deploy)")


if __name__ == "__main__":
    main()
