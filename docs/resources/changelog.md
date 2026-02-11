---
title: Changelog
---

# Changelog

All notable changes to GeckoCIRCUITS.

## v0.3.0 - "Script It" (2026-02)

### Added
- GeckoSCRIPT tutorial with script editor walkthrough, parameter sweeps, and batch automation
- MATLAB/Octave integration tutorial covering RMI setup, parameter control, and Simulink co-simulation
- Python integration tutorial with subprocess control, file-based modification, and optimization examples
- Java Blocks tutorial for custom control blocks and circuit components
- Python parameter sweep example script (`resources/tutorials/7xx_scripting_automation/706_python_integration/parameter_sweep.py`)
- Installation verification script (`scripts/run-examples.py`)
- Circuit validation step in CI pipeline

### Changed
- REST API documentation marked as "Planned Feature" to reflect actual implementation status
- API index page updated to distinguish implemented vs planned interfaces

## v0.2.0 - "Understand It" (2026-02)

### Added
- Comprehensive documentation site with MkDocs Material theme
- Getting Started guide with installation, quickstart, and first simulation tutorials
- Tutorial library covering DC-DC, AC-DC, DC-AC, thermal, and scripting topics
- FAQ, troubleshooting, and contributing guides
- GitHub issue templates for bugs and feature requests

### Changed
- Consolidated CI workflows into a single `ci.yml`
- Excluded integration tests (ModelResultsTest) that require GUI from CI

### Removed
- Redundant workflow files (maven.yml, build-test.yml, dev-ci.yml)
- Stale remote branches (experimental)

## v0.1.0 - "It Works" (2026-02)

### Added
- GitHub Actions CI pipeline with automated build and test
- MkDocs documentation deployment to GitHub Pages
- Platform-specific launcher scripts (Windows, Linux, macOS, WSL)
- Distribution packaging via Maven profiles
- New user-focused README

### Changed
- Reorganized `resources/` directory structure (tutorials, examples, articles)
- Updated `.gitignore` for modern Java/Python/IDE patterns
- Cleaned up article file naming with date prefixes

### Removed
- Archive directory (old DLLs, database files)
- Redundant resource files

## Pre-release History

GeckoCIRCUITS was originally developed at ETH Zurich's Power Electronic Systems Laboratory (PES). The open-source release builds on years of academic development and use in power electronics education and research.

### Key historical milestones
- **2009-2012** - Newsletter series documenting simulation techniques
- **2010** - MATLAB/Simulink integration via RMI
- **2011** - Thermal simulation capabilities
- **2024** - Migration to Java 21, GraalVM polyglot scripting
- **2025** - GUI-free core extraction (gecko-simulation-core)
- **2026** - Open-source release preparation
