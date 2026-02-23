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

## Future Roadmap

See the [Roadmap Page](../roadmap.md) for upcoming releases and features.

### Upcoming Milestones

- **v2.18.0 - v2.2x** (Q2-Q3 2026): Incremental REST API features
- **v3.0.0** (Q3 2026): 🚀 Complete REST API Platform - **MARKETING WOW**
- **v3.1.0** (Q4 2026): Web UI Launch (React + TypeScript)
- **v3.2.0** (Q1 2027): Cloud Deployment (Kubernetes)
- **v4.0.0** (Q2 2027): Machine Learning Integration

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


def generate_roadmap_page() -> str:
    """Generate the roadmap page from RELEASE_PLAN.md."""
    release_plan_path = PROJECT_ROOT / "RELEASE_PLAN.md"

    if not release_plan_path.exists():
        return "# Roadmap\n\nRoadmap information coming soon..."

    # Read RELEASE_PLAN.md
    with open(release_plan_path, "r") as f:
        plan_content = f.read()

    # Extract relevant sections for roadmap
    content = """# GeckoCIRCUITS Roadmap

Strategic roadmap for GeckoCIRCUITS development, showing completed milestones and future plans.

---

## Current Status (February 2026)

- **Latest Release:** v2.17.0 - Release Automation
- **Ground Zero:** v2.10.0 - Java 21 Migration
- **Completed Milestones:** 8 releases (v2.10.0 → v2.17.0)

---

## Timeline Visualization

```mermaid
gantt
    title GeckoCIRCUITS Development Timeline
    dateFormat YYYY-MM-DD
    section v2.x Series
    Java 21 Migration (v2.10.0)       :done, 2026-02-14, 1d
    Core Module Foundation (v2.11.0)  :done, 2026-02-14, 1d
    Static Analysis (v2.12.0)         :done, 2026-02-14, 1d
    Terminal Migration (v2.13.0)      :done, 2026-02-14, 1d
    GeckoFile (v2.14.0)               :done, 2026-02-14, 1d
    Loss Calculation (v2.15.0)        :done, 2026-02-14, 1d
    REST API Launch (v2.16.0)         :done, 2026-02-14, 1d
    Release Automation (v2.17.0)      :done, 2026-02-14, 1d

    section Future (v3.0.0)
    Simulation Control Endpoints      :active, 2026-03-01, 45d
    WebSocket Streaming              :       2026-04-15, 30d
    Authentication & Security        :       2026-05-15, 30d
    Advanced Analysis Endpoints      :       2026-06-15, 30d
    Production Infrastructure        :       2026-07-15, 45d
    v3.0.0 MARKETING WOW            :crit,   2026-09-01, 1d

    section Beyond
    Web UI Launch (v3.1.0)          :       2026-10-01, 90d
    Cloud Deployment (v3.2.0)       :       2027-01-01, 90d
    ML Integration (v4.0.0)         :       2027-04-01, 90d
```

---

## Completed Milestones (v2.10.0 → v2.17.0)

"""

    # Add links to each release
    for tag in VERSIONS:
        tag_clean = tag.replace("v", "").replace(".", "")
        info = get_tag_info(tag)
        first_line = info["message"].split("\n")[0] if info["message"] else tag
        title = first_line.replace(tag, "").strip().lstrip(":").strip()

        content += f"### [{tag}](releases/{tag_clean}.md) - {title}\n\n"
        content += f"**Released:** {info['date']}\n\n"

    content += """---

## Future Releases

### v3.0.0 - Complete REST API Platform 🚀 (Q3 2026)

**Target Date:** August-September 2026

**Significance:** MARKETING WOW - First production-ready REST API with breaking changes

#### Major Features

**1. Complete REST API Endpoints (30+ total)**

- Simulation control (create, start, pause, stop, results)
- Real-time data streaming (WebSocket support)
- Advanced analysis (FFT, THD, RMS, power quality, Bode plots)
- Batch operations (parameter sweeps, optimization)
- Circuit management (CRUD operations with pagination)

**2. Security & Authentication**

- JWT-based authentication
- API key management
- Rate limiting (100 req/min per user, configurable)
- Role-based access control (RBAC)
- OAuth2 integration (Google, GitHub, Microsoft)
- Audit logging

**3. Production Infrastructure**

- Horizontal scaling support
- Redis caching layer
- PostgreSQL database for metadata
- Prometheus metrics export
- ELK stack logging integration
- Health check endpoints
- Graceful shutdown

**4. Documentation & Developer Experience**

- Interactive API documentation (Swagger UI)
- Postman collection with examples
- Client SDK generation (Java, Python, JavaScript)
- Migration guide from v2.x API
- Video demonstrations

#### Timeline (6-7 months)

- Sprint 6: Simulation control endpoints (6 weeks)
- Sprint 7: WebSocket streaming (4 weeks)
- Sprint 8: Authentication & security (4 weeks)
- Sprint 9: Advanced analysis endpoints (4 weeks)
- Sprint 10: Production infrastructure (6 weeks)
- Sprint 11: Documentation & testing (3 weeks)

---

### v3.1.0 - Web UI Launch (Q4 2026)

**Significance:** Browser-based circuit editor and simulator

- React 18 + TypeScript web application
- Circuit editor with drag-and-drop components
- Real-time oscilloscope visualization (D3.js)
- Parameter editing and tuning
- Circuit library browser
- Tutorial integration
- Responsive design (desktop, tablet)
- PWA support for offline use

---

### v3.2.0 - Cloud Deployment (Q1 2027)

**Significance:** Multi-tenant SaaS platform

- AWS/Azure/GCP deployment scripts
- Kubernetes orchestration (Helm charts)
- Auto-scaling based on load
- Multi-tenant isolation
- User workspace management
- Circuit sharing and collaboration
- Marketplace for circuit libraries
- Usage analytics and billing integration

---

### v4.0.0 - Machine Learning Integration (Q2 2027)

**Significance:** AI-assisted circuit design and optimization

- Circuit optimization using reinforcement learning
- Automated component selection based on specifications
- Anomaly detection in simulation results
- Predictive maintenance modeling
- Training data generation from simulation runs
- Pre-trained models for common topologies
- Neural network surrogate models for fast approximations

---

## Release Frequency

Predictable release cadence:

- **Major releases (X.0.0):** Annually - Breaking changes, major features, marketing events
- **Minor releases (X.Y.0):** Quarterly - New features, enhancements, backward-compatible
- **Patch releases (X.Y.Z):** As needed - Bug fixes, security updates, hotfixes

---

## Long-Term Vision (2027-2028)

### Educational Platform Expansion

- Interactive tutorials with embedded simulator
- Certification programs (Power Electronics Fundamentals)
- Virtual laboratory for universities
- Competition platform for students (circuit design challenges)
- Integration with LMS (Moodle, Canvas, Blackboard)
- SCORM-compliant content packages

### Industry Partnerships

- Semiconductor vendor integrations (Infineon, Wolfspeed, ON Semi, STMicro)
- Component library partnerships
- Enterprise licensing model (per-seat, per-server, site license)
- Professional support tiers (email, phone, dedicated engineer)
- Training and consulting services

### Research Collaboration

- Academic paper citation tracking
- Research dataset sharing (simulation results, validation data)
- Reproducible research workflows (Docker + circuit files)
- Integration with research tools (Jupyter, MATLAB Online, Mathematica)
- Grant-funded development partnerships

---

## Contributing

Want to contribute to the roadmap? We welcome:

- Feature requests and suggestions
- Bug reports and fixes
- Documentation improvements
- Example circuits and tutorials
- Code contributions

See our [Contributing Guide](https://github.com/tinix84/GeckoCIRCUITS/blob/main/CONTRIBUTING.md) for details.

---

## Feedback

Your feedback shapes the roadmap! Share your thoughts:

- [GitHub Discussions](https://github.com/tinix84/GeckoCIRCUITS/discussions)
- [Feature Requests](https://github.com/tinix84/GeckoCIRCUITS/issues/new?template=feature_request.md)
- [Email](mailto:maintainer@geckocircuits.org) (if public)
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

    # Generate roadmap page
    print("\n🗺️  Generating roadmap page...")
    content = generate_roadmap_page()
    filepath = DOCS_DIR / "roadmap.md"
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
