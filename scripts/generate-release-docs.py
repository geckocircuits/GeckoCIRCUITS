#!/usr/bin/env python3
"""
Generate release documentation for MkDocs site.
Combines Python data extraction with Haiku LLM content generation.

Usage:
    python scripts/generate-release-docs.py
    python scripts/generate-release-docs.py --dry-run  # Preview without writing
"""

import os
import re
import subprocess
from pathlib import Path
from typing import Dict, List, Optional
import argparse

PROJECT_ROOT = Path(__file__).parent.parent
DOCS_DIR = PROJECT_ROOT / "docs"
RELEASES_DIR = DOCS_DIR / "releases"
INSTALL_DIR = DOCS_DIR / "install"

# Version range to document
VERSIONS = [
    "v2.10.0",
    "v2.11.0",
    "v2.12.0",
    "v2.13.0",
    "v2.14.0",
    "v2.15.0",
    "v2.16.0",
    "v2.17.0",
]

CURRENT_VERSION = "v2.17.0"
FUTURE_VERSION = "v3.0.0"


def run_command(cmd: List[str]) -> str:
    """Run a shell command and return output."""
    try:
        result = subprocess.run(
            cmd,
            cwd=PROJECT_ROOT,
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error running command {' '.join(cmd)}: {e}")
        return ""


def get_tag_info(tag: str) -> Dict[str, str]:
    """Extract information from a git tag."""
    # Get tag message (full annotation)
    tag_message = run_command(["git", "tag", "-l", "-n999", tag])

    # Get commit hash
    commit_hash = run_command(["git", "rev-list", "-n", "1", tag])

    # Get commit date
    commit_date = run_command(["git", "log", "-1", "--format=%ai", tag])

    # Get commit count since tag
    commit_count = run_command(["git", "rev-list", "--count", f"{tag}..HEAD"])

    return {
        "tag": tag,
        "message": tag_message,
        "commit": commit_hash[:8] if commit_hash else "",
        "date": commit_date.split()[0] if commit_date else "",
        "commits_ahead": commit_count
    }


def get_commits_between_tags(tag1: str, tag2: str) -> List[str]:
    """Get commit messages between two tags."""
    commits = run_command([
        "git", "log", "--oneline", "--no-decorate",
        f"{tag1}..{tag2}"
    ])
    return commits.split("\n") if commits else []


def extract_features_from_tag_message(message: str) -> Dict[str, List[str]]:
    """Extract structured information from tag message."""
    features = []
    improvements = []
    metrics = []

    current_section = None
    for line in message.split("\n"):
        line = line.strip()
        if not line:
            continue

        # Detect sections
        if "Major Features:" in line or "Major Changes:" in line:
            current_section = "features"
        elif "Key Metrics:" in line:
            current_section = "metrics"
        elif "Significance:" in line:
            current_section = None
        elif line.startswith("-") and current_section == "features":
            features.append(line[1:].strip())
        elif line.startswith("-") and current_section == "metrics":
            metrics.append(line[1:].strip())

    return {
        "features": features,
        "improvements": improvements,
        "metrics": metrics
    }


def generate_release_page(tag: str, info: Dict[str, str], prev_tag: Optional[str] = None) -> str:
    """Generate markdown content for a release page."""
    tag_clean = tag.replace("v", "")

    # Extract structured data from tag message
    data = extract_features_from_tag_message(info["message"])

    # Get commits if we have a previous tag
    commits_list = ""
    if prev_tag:
        commits = get_commits_between_tags(prev_tag, tag)
        if commits:
            commits_list = "\n".join([f"- {commit}" for commit in commits[:20]])
            if len(commits) > 20:
                commits_list += f"\n- ... and {len(commits) - 20} more commits"

    # Build the markdown page
    content = f"""# {tag} Release Notes

**Release Date:** {info['date']}
**Commit:** [{info['commit']}](https://github.com/tinix84/GeckoCIRCUITS/commit/{info['commit']})

---

## Overview

{tag} represents a major milestone in the GeckoCIRCUITS modernization journey.

---

## What's New

### Features

"""

    # Add features from tag message
    if data["features"]:
        for feature in data["features"]:
            content += f"- {feature}\n"
    else:
        content += "- Major refactoring and improvements\n"

    content += "\n"

    # Add metrics section if available
    if data["metrics"]:
        content += "### Key Metrics\n\n"
        for metric in data["metrics"]:
            content += f"- {metric}\n"
        content += "\n"

    # Add downloads section
    content += f"""---

## Downloads

Distribution packages for all platforms:

=== "Windows"

    ```bash
    # Download
    wget https://github.com/tinix84/GeckoCIRCUITS/releases/download/{tag}/GeckoCIRCUITS-{tag_clean}-windows.zip

    # Extract
    unzip GeckoCIRCUITS-{tag_clean}-windows.zip

    # Run
    run-gecko.bat
    ```

=== "Linux"

    ```bash
    # Download
    wget https://github.com/tinix84/GeckoCIRCUITS/releases/download/{tag}/GeckoCIRCUITS-{tag_clean}-linux.zip

    # Extract
    unzip GeckoCIRCUITS-{tag_clean}-linux.zip

    # Make executable
    chmod +x run-gecko-linux.sh

    # Run
    ./run-gecko-linux.sh
    ```

=== "macOS"

    ```bash
    # Download
    wget https://github.com/tinix84/GeckoCIRCUITS/releases/download/{tag}/GeckoCIRCUITS-{tag_clean}-macos.zip

    # Extract
    unzip GeckoCIRCUITS-{tag_clean}-macos.zip

    # Make executable
    chmod +x run-gecko-macos.sh

    # Run
    ./run-gecko-macos.sh
    ```

=== "WSL"

    ```bash
    # Download
    wget https://github.com/tinix84/GeckoCIRCUITS/releases/download/{tag}/GeckoCIRCUITS-{tag_clean}-wsl.zip

    # Extract
    unzip GeckoCIRCUITS-{tag_clean}-wsl.zip

    # Make executable
    chmod +x run-gecko-wsl.sh

    # Setup (first time)
    ./run-gecko-wsl.sh --setup

    # Run
    ./run-gecko-wsl.sh
    ```

---

## System Requirements

- **Java:** OpenJDK 21 or later
- **Memory:** 4 GB RAM (8 GB recommended)
- **Storage:** 200 MB for application
- **Display:** 1280x720 resolution (HiDPI supported)
- **WSL Only:** X server (VcXsrv, Xming, or WSLg)

---

## Installation & Getting Started

See the [Installation Guide](../install/index.md) for detailed platform-specific instructions.

After installation, check out:

- [Quick Start Guide](../getting-started/quickstart.md)
- [First Simulation Tutorial](../getting-started/first-simulation.md)
- [Tutorials Index](../tutorials/index.md)

---

## Full Changelog

"""

    if prev_tag:
        content += f"[View all changes]({info['commit']}...{prev_tag})\n\n"
        if commits_list:
            content += "### Commits in this release\n\n"
            content += commits_list + "\n\n"

    content += f"""---

## Links

- [GitHub Release](https://github.com/tinix84/GeckoCIRCUITS/releases/tag/{tag})
- [All Releases](https://github.com/tinix84/GeckoCIRCUITS/releases)
- [Source Code](https://github.com/tinix84/GeckoCIRCUITS/tree/{tag})
- [Documentation](https://tinix84.github.io/GeckoCIRCUITS/)

---

## Support

- **Issues:** [GitHub Issues](https://github.com/tinix84/GeckoCIRCUITS/issues)
- **Discussions:** [GitHub Discussions](https://github.com/tinix84/GeckoCIRCUITS/discussions)
- **Documentation:** [tinix84.github.io/GeckoCIRCUITS](https://tinix84.github.io/GeckoCIRCUITS/)
"""

    return content


def generate_releases_index() -> str:
    """Generate the releases index page."""
    content = """# GeckoCIRCUITS Releases

Complete history of GeckoCIRCUITS releases with download links, release notes, and changelogs.

---

## Current Release

The latest stable release is **{current}** (released {date}).

[Download {current}](https://github.com/tinix84/GeckoCIRCUITS/releases/tag/{current}){{ .md-button .md-button--primary }}
[View Release Notes](./{current_clean}.md){{ .md-button }}

---

## Version History

### v2.x Series - Modern Development

""".format(
        current=CURRENT_VERSION,
        current_clean=CURRENT_VERSION.replace("v", "").replace(".", ""),
        date="February 2026"
    )

    # Add all releases in reverse order
    for tag in reversed(VERSIONS):
        tag_clean = tag.replace("v", "").replace(".", "")
        info = get_tag_info(tag)

        # Extract first line of tag message as title
        first_line = info["message"].split("\n")[0] if info["message"] else tag
        title = first_line.replace(tag, "").strip().lstrip(":").strip()

        content += f"""#### [{tag}](./{tag_clean}.md) - {title}

**Released:** {info['date']}

[Release Notes](./{tag_clean}.md){{ .md-button }}
[Download](https://github.com/tinix84/GeckoCIRCUITS/releases/tag/{tag}){{ .md-button }}

---

"""

    content += """### Upstream Lineage

| Version | Source | Description |
|---------|--------|-------------|
| v2.03-spotbugs-clean | This Fork | All 1,096 SpotBugs violations fixed to zero |
| v2.04-repo-reorg | This Fork | Repository reorganization with JDK 21 workflow fix |
| v2.02 | geckocircuits/GeckoCIRCUITS | Last upstream release (not in this fork) |

---

## What's Next

Planned work is tracked in the [GitHub milestones](https://github.com/tinix84/GeckoCIRCUITS/milestones).

---

## Release Channels

GeckoCIRCUITS follows semantic versioning with predictable release cadence:

- **Major releases (X.0.0):** Annually - Breaking changes, major features, marketing events
- **Minor releases (X.Y.0):** Quarterly - New features, enhancements, backward-compatible
- **Patch releases (X.Y.Z):** As needed - Bug fixes, security updates, hotfixes

---

## Installation

Each release includes platform-specific packages:

- **Windows** - `GeckoCIRCUITS-*-windows.zip` with run-gecko.bat launcher
- **Linux** - `GeckoCIRCUITS-*-linux.zip` with run-gecko-linux.sh launcher
- **macOS** - `GeckoCIRCUITS-*-macos.zip` with run-gecko-macos.sh launcher
- **WSL** - `GeckoCIRCUITS-*-wsl.zip` with run-gecko-wsl.sh launcher (X11 support)
- **Examples** - `GeckoCIRCUITS-*-examples.zip` with circuit files and tutorials

See the [Installation Guide](../install/index.md) for detailed instructions.

---

## Support

- **Documentation:** [tinix84.github.io/GeckoCIRCUITS](https://tinix84.github.io/GeckoCIRCUITS/)
- **GitHub Issues:** [Report bugs or request features](https://github.com/tinix84/GeckoCIRCUITS/issues)
- **GitHub Discussions:** [Ask questions and share ideas](https://github.com/tinix84/GeckoCIRCUITS/discussions)
"""

    return content


def main():
    """Main function to generate all documentation."""
    parser = argparse.ArgumentParser(description="Generate release documentation")
    parser.add_argument("--dry-run", action="store_true", help="Preview without writing files")
    args = parser.parse_args()

    print("=" * 60)
    print("GeckoCIRCUITS Documentation Generator")
    print("=" * 60)
    print()

    # Create directories
    if not args.dry_run:
        RELEASES_DIR.mkdir(parents=True, exist_ok=True)
        print(f"✓ Created directory: {RELEASES_DIR}")

    # Generate release pages
    print("\n📄 Generating release pages...")
    prev_tag = None
    for tag in VERSIONS:
        info = get_tag_info(tag)
        if not info["commit"]:
            print(f"  ⚠ Skipping {tag} (tag not found)")
            continue

        content = generate_release_page(tag, info, prev_tag)
        tag_clean = tag.replace("v", "").replace(".", "")
        filepath = RELEASES_DIR / f"{tag_clean}.md"

        if args.dry_run:
            print(f"  [DRY RUN] Would write: {filepath}")
        else:
            with open(filepath, "w") as f:
                f.write(content)
            print(f"  ✓ Generated: {filepath.name}")

        prev_tag = tag

    # Generate releases index
    print("\n📑 Generating releases index...")
    content = generate_releases_index()
    filepath = RELEASES_DIR / "index.md"
    if args.dry_run:
        print(f"  [DRY RUN] Would write: {filepath}")
    else:
        with open(filepath, "w") as f:
            f.write(content)
        print(f"  ✓ Generated: {filepath.name}")

    print("\n" + "=" * 60)
    print("✅ Documentation generation complete!")
    print("=" * 60)

    if not args.dry_run:
        print("\nNext steps:")
        print("1. Update mkdocs.yml navigation to include new pages")
        print("2. Run: mkdocs serve (to preview locally)")
        print("3. Run: mkdocs build --strict (to check for errors)")
        print("4. Run: mkdocs gh-deploy --force (to deploy)")
    else:
        print("\n(Dry run - no files were written)")


if __name__ == "__main__":
    main()
