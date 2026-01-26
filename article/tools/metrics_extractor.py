#!/usr/bin/env python3
"""
Metrics Extractor for GeckoCIRCUITS Content Creation

Extracts quantitative metrics from markdown documentation files and git commits
to provide consistent, structured data for content generation.

Usage:
    python metrics_extractor.py <file_path> [--format json|yaml|pretty]
    python metrics_extractor.py --git-log <commit_range>
    python metrics_extractor.py --sprint <sprint_number>

Examples:
    python metrics_extractor.py PHASE_1_COMPLETE.md
    python metrics_extractor.py --git-log HEAD~5..HEAD
    python metrics_extractor.py --sprint 15

Token Savings: ~500 tokens per article by providing structured data instead of
re-reading full documentation files.
"""

import argparse
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Dict, List, Optional, Any
import yaml


@dataclass
class Metrics:
    """Structured metrics extracted from documentation or git history."""

    # Core metrics
    tests_added: int = 0
    coverage_before: str = ""
    coverage_after: str = ""
    coverage_gain: str = ""

    # Code metrics
    files_created: int = 0
    files_modified: int = 0
    lines_added: int = 0
    lines_deleted: int = 0

    # Sprint/timeline metrics
    sprint_number: Optional[int] = None
    sprints_completed: int = 0
    duration: str = ""

    # Feature metrics
    endpoints_created: int = 0
    interfaces_extracted: int = 0
    components_added: int = 0

    # Technologies
    technologies: List[str] = None
    patterns: List[str] = None

    # Context
    source_file: str = ""
    title: str = ""
    summary: str = ""
    key_achievements: List[str] = None
    challenges: List[str] = None

    def __post_init__(self):
        if self.technologies is None:
            self.technologies = []
        if self.patterns is None:
            self.patterns = []
        if self.key_achievements is None:
            self.key_achievements = []
        if self.challenges is None:
            self.challenges = []

    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary, filtering out None/empty values."""
        data = asdict(self)
        return {k: v for k, v in data.items() if v not in [None, "", 0, []]}


class MetricsExtractor:
    """Extract metrics from various sources."""

    def __init__(self, repo_root: Optional[Path] = None):
        self.repo_root = repo_root or Path.cwd()

    def extract_from_file(self, file_path: Path) -> Metrics:
        """Extract metrics from a markdown documentation file."""
        if not file_path.exists():
            raise FileNotFoundError(f"File not found: {file_path}")

        content = file_path.read_text(encoding='utf-8')
        metrics = Metrics(source_file=str(file_path))

        # Extract title (first H1)
        title_match = re.search(r'^#\s+(.+)$', content, re.MULTILINE)
        if title_match:
            metrics.title = title_match.group(1).strip()

        # Extract test coverage metrics
        coverage_patterns = [
            r'coverage.*?(\d+(?:\.\d+)?%)\s*(?:→|to|->)\s*(\d+(?:\.\d+)?%)',
            r'(\d+(?:\.\d+)?%)\s*→\s*(\d+(?:\.\d+)?%)\s*coverage',
            r'coverage.*?from\s*(\d+(?:\.\d+)?%)\s*to\s*(\d+(?:\.\d+)?%)',
        ]
        for pattern in coverage_patterns:
            match = re.search(pattern, content, re.IGNORECASE)
            if match:
                metrics.coverage_before = match.group(1)
                metrics.coverage_after = match.group(2)
                before_val = float(match.group(1).rstrip('%'))
                after_val = float(match.group(2).rstrip('%'))
                metrics.coverage_gain = f"+{after_val - before_val:.1f}pp"
                break

        # Extract test counts
        test_patterns = [
            r'(\d+)\s+tests?\s+(?:added|created|written)',
            r'added\s+(\d+)\s+tests?',
            r'(\d+)\s+new\s+tests?',
        ]
        for pattern in test_patterns:
            match = re.search(pattern, content, re.IGNORECASE)
            if match:
                metrics.tests_added = int(match.group(1))
                break

        # Extract sprint number
        sprint_match = re.search(r'[Ss]print\s+(\d+)', content)
        if sprint_match:
            metrics.sprint_number = int(sprint_match.group(1))

        # Extract endpoint count
        endpoint_patterns = [
            r'(\d+)\s+(?:REST\s+)?endpoints?',
            r'endpoints?.*?(\d+)',
        ]
        for pattern in endpoint_patterns:
            match = re.search(pattern, content, re.IGNORECASE)
            if match:
                metrics.endpoints_created = int(match.group(1))
                break

        # Extract interface count
        interface_match = re.search(r'(\d+)\s+interfaces?', content, re.IGNORECASE)
        if interface_match:
            metrics.interfaces_extracted = int(interface_match.group(1))

        # Extract timeline/duration
        duration_patterns = [
            r'(\d+\s+weeks?)',
            r'(\d+\s+months?)',
            r'(\d+\s+sprints?)',
        ]
        for pattern in duration_patterns:
            match = re.search(pattern, content, re.IGNORECASE)
            if match:
                metrics.duration = match.group(1)
                break

        # Extract technologies
        tech_keywords = [
            'Spring Boot', 'Maven', 'REST API', 'WebSocket', 'STOMP',
            'Java', 'JUnit', 'JaCoCo', 'Swagger', 'OpenAPI',
            'React', 'Python', 'JavaFX', 'WebAssembly'
        ]
        for tech in tech_keywords:
            if tech in content:
                metrics.technologies.append(tech)

        # Extract patterns
        pattern_keywords = [
            'Facade', 'Interface', 'God class', 'Matrix stamping',
            'Dual-track', 'Test-driven', 'Incremental refactoring'
        ]
        for pattern in pattern_keywords:
            if pattern.lower() in content.lower():
                metrics.patterns.append(pattern)

        # Extract key achievements (look for bullet points)
        achievements = re.findall(r'^[•\-\*]\s+(.+)$', content, re.MULTILINE)
        metrics.key_achievements = achievements[:5]  # Top 5

        # Extract summary (first paragraph after title)
        summary_match = re.search(r'^#.+?\n\n(.+?)(?:\n\n|\n#)', content, re.DOTALL)
        if summary_match:
            summary = summary_match.group(1).strip()
            metrics.summary = summary[:300]  # First 300 chars

        return metrics

    def extract_from_git_log(self, commit_range: str = "HEAD~10..HEAD") -> Metrics:
        """Extract metrics from git log."""
        try:
            # Get commit messages
            result = subprocess.run(
                ['git', 'log', '--oneline', commit_range],
                cwd=self.repo_root,
                capture_output=True,
                text=True,
                check=True
            )
            commits = result.stdout.strip().split('\n')

            # Get file stats
            result = subprocess.run(
                ['git', 'diff', '--shortstat', commit_range.split('..')[0],
                 commit_range.split('..')[1] if '..' in commit_range else 'HEAD'],
                cwd=self.repo_root,
                capture_output=True,
                text=True,
                check=True
            )

            metrics = Metrics(source_file=f"git log {commit_range}")
            metrics.sprints_completed = len([c for c in commits if 'sprint' in c.lower()])

            # Parse diff stats
            stats = result.stdout.strip()
            files_match = re.search(r'(\d+)\s+files?\s+changed', stats)
            insertions_match = re.search(r'(\d+)\s+insertions?', stats)
            deletions_match = re.search(r'(\d+)\s+deletions?', stats)

            if files_match:
                metrics.files_modified = int(files_match.group(1))
            if insertions_match:
                metrics.lines_added = int(insertions_match.group(1))
            if deletions_match:
                metrics.lines_deleted = int(deletions_match.group(1))

            # Extract key achievements from commit messages
            metrics.key_achievements = [
                c.split(' ', 1)[1] for c in commits[:5] if ' ' in c
            ]

            return metrics

        except subprocess.CalledProcessError as e:
            print(f"Error running git command: {e}", file=sys.stderr)
            return Metrics(source_file=f"git log {commit_range} (error)")

    def extract_from_sprint(self, sprint_number: int) -> Metrics:
        """Extract metrics for a specific sprint by finding related files."""
        # Look for sprint-related files
        search_patterns = [
            f"SPRINT{sprint_number}_*.md",
            f"SPRINT_{sprint_number}_*.md",
            f"sprint{sprint_number}*.md",
            f"*sprint{sprint_number}*.md",
        ]

        found_files = []
        for pattern in search_patterns:
            found_files.extend(self.repo_root.glob(pattern))
            found_files.extend(self.repo_root.glob(pattern.upper()))

        if found_files:
            # Use the first matching file
            return self.extract_from_file(found_files[0])

        # If no dedicated file, search in session summaries or main docs
        all_md_files = list(self.repo_root.glob("*.md"))
        for md_file in all_md_files:
            content = md_file.read_text(encoding='utf-8')
            if f"sprint {sprint_number}" in content.lower() or f"sprint{sprint_number}" in content.lower():
                metrics = self.extract_from_file(md_file)
                metrics.sprint_number = sprint_number
                return metrics

        # If still not found, create empty metrics
        return Metrics(
            sprint_number=sprint_number,
            source_file=f"Sprint {sprint_number} (no dedicated file found)"
        )

    def extract_multiple_files(self, file_paths: List[Path]) -> Dict[str, Metrics]:
        """Extract metrics from multiple files and return as dictionary."""
        return {
            str(path): self.extract_from_file(path)
            for path in file_paths
        }


def format_output(metrics: Metrics, format_type: str = 'json') -> str:
    """Format metrics for output."""
    data = metrics.to_dict()

    if format_type == 'json':
        return json.dumps(data, indent=2)

    elif format_type == 'yaml':
        return yaml.dump(data, default_flow_style=False, sort_keys=False)

    elif format_type == 'pretty':
        lines = [f"# Metrics: {metrics.title or metrics.source_file}", ""]

        if metrics.summary:
            lines.append(f"**Summary:** {metrics.summary}\n")

        if metrics.tests_added:
            lines.append(f"**Tests Added:** {metrics.tests_added}")

        if metrics.coverage_before and metrics.coverage_after:
            lines.append(f"**Coverage:** {metrics.coverage_before} → {metrics.coverage_after} ({metrics.coverage_gain})")

        if metrics.endpoints_created:
            lines.append(f"**Endpoints Created:** {metrics.endpoints_created}")

        if metrics.interfaces_extracted:
            lines.append(f"**Interfaces Extracted:** {metrics.interfaces_extracted}")

        if metrics.files_created:
            lines.append(f"**Files Created:** {metrics.files_created}")

        if metrics.lines_added or metrics.lines_deleted:
            lines.append(f"**Lines:** +{metrics.lines_added} / -{metrics.lines_deleted}")

        if metrics.duration:
            lines.append(f"**Duration:** {metrics.duration}")

        if metrics.technologies:
            lines.append(f"\n**Technologies:** {', '.join(metrics.technologies)}")

        if metrics.patterns:
            lines.append(f"**Patterns:** {', '.join(metrics.patterns)}")

        if metrics.key_achievements:
            lines.append("\n**Key Achievements:**")
            for achievement in metrics.key_achievements[:3]:
                lines.append(f"  • {achievement}")

        return '\n'.join(lines)

    else:
        raise ValueError(f"Unknown format: {format_type}")


def main():
    parser = argparse.ArgumentParser(
        description='Extract metrics from GeckoCIRCUITS documentation',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )

    parser.add_argument(
        'file_path',
        nargs='?',
        help='Path to markdown file to extract metrics from'
    )
    parser.add_argument(
        '--git-log',
        metavar='RANGE',
        help='Extract metrics from git log (e.g., HEAD~5..HEAD)'
    )
    parser.add_argument(
        '--sprint',
        type=int,
        metavar='N',
        help='Extract metrics for sprint N'
    )
    parser.add_argument(
        '--format',
        choices=['json', 'yaml', 'pretty'],
        default='json',
        help='Output format (default: json)'
    )
    parser.add_argument(
        '--repo-root',
        type=Path,
        help='Repository root directory (default: current directory)'
    )

    args = parser.parse_args()

    # Validate arguments
    if not any([args.file_path, args.git_log, args.sprint]):
        parser.error("Must specify either file_path, --git-log, or --sprint")

    extractor = MetricsExtractor(repo_root=args.repo_root)

    try:
        if args.git_log:
            metrics = extractor.extract_from_git_log(args.git_log)
        elif args.sprint:
            metrics = extractor.extract_from_sprint(args.sprint)
        else:
            file_path = Path(args.file_path)
            metrics = extractor.extract_from_file(file_path)

        output = format_output(metrics, args.format)
        print(output)

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
