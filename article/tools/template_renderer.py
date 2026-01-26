#!/usr/bin/env python3
"""
Template Renderer for GeckoCIRCUITS Content Creation

Fills content templates (LinkedIn posts, article sections) with extracted metrics
to provide deterministic, token-efficient content generation.

Usage:
    python template_renderer.py <template_id> <metrics_file>
    python template_renderer.py --list-templates
    python template_renderer.py linkedin_achievement metrics.json
    echo '{"tests_added": 93}' | python template_renderer.py linkedin_achievement -

Examples:
    python template_renderer.py linkedin_achievement metrics.json
    python template_renderer.py article_executive_summary phase1_metrics.json
    python template_renderer.py --list-templates

Token Savings: ~300 tokens per post by using templates instead of regenerating content.
"""

import argparse
import json
import sys
from pathlib import Path
from string import Template
from typing import Dict, Any, List


# Template definitions
TEMPLATES = {
    # LinkedIn Post Templates
    'linkedin_achievement': Template("""
$hook

We $action in $timeframe. Here's what we learned.

$context

The results:
$results

Key takeaways:
$takeaways

$cta

---
$hashtags
""".strip()),

    'linkedin_coverage': Template("""
From $coverage_before to $coverage_after test coverage in $duration.

$context

Our approach:
$approach

The numbers:
→ Tests added: $tests_added
→ Coverage gain: $coverage_gain
→ Sprint: $sprint_number
$additional_metrics

$lesson

$cta

---
$hashtags
""".strip()),

    'linkedin_pivot': Template("""
$hook

$problem

The conventional wisdom: $conventional_approach

Our approach: $our_approach

Results:
$results

Sometimes the best $solution_type is no $solution_type at all.

$cta

---
$hashtags
""".strip()),

    'linkedin_technical': Template("""
$problem_statement

$context

The challenge: $challenge

$solution_intro

```$language
$code_snippet
```

$explanation

$results

$lesson

$cta

---
$hashtags
""".strip()),

    # Article Section Templates
    'article_executive_summary': Template("""
# $title

$hook

$context

$promise

This article walks you through $content_description.

**If you're $target_audience, this case study will show you $value_proposition.**

Let's dive in.
""".strip()),

    'article_problem': Template("""
## The Problem: $problem_title

$situation

$pain_points_intro

$pain_point_list

**The Obvious Solution: $obvious_solution**

$obvious_solution_problems

**The Better Solution: $better_solution**

$better_solution_intro

That's what we set out to prove.
""".strip()),

    'article_results': Template("""
## Results & Metrics

$results_intro

$metrics_table

**Timeline:** $timeline

$before_after_comparison

$impact_summary
""".strip()),

    'article_lessons': Template("""
## Lessons Learned

$lesson_list

$reflection

$advice
""".strip()),

    # Data Formatting Templates
    'metrics_table': Template("""
| Metric | Before | After | Change |
|--------|--------|-------|--------|
$table_rows
""".strip()),

    'before_after': Template("""
**Before:**
$before_list

**After:**
$after_list
""".strip()),

    'bullet_list': Template("""
$items
""".strip()),
}


def load_metrics(metrics_source: str) -> Dict[str, Any]:
    """Load metrics from file or stdin."""
    if metrics_source == '-':
        # Read from stdin
        data = sys.stdin.read()
    else:
        # Read from file
        path = Path(metrics_source)
        if not path.exists():
            raise FileNotFoundError(f"Metrics file not found: {metrics_source}")
        data = path.read_text(encoding='utf-8')

    return json.loads(data)


def format_bullet_list(items: List[str], prefix: str = "→") -> str:
    """Format a list of items as bullets."""
    return '\n'.join(f"{prefix} {item}" for item in items)


def format_numbered_list(items: List[str], start: int = 1) -> str:
    """Format a list of items as numbered list."""
    return '\n'.join(f"{i}. {item}" for i, item in enumerate(items, start))


def format_table_rows(data: List[Dict[str, str]]) -> str:
    """Format data as markdown table rows."""
    rows = []
    for row in data:
        cells = [row.get(col, '') for col in ['metric', 'before', 'after', 'change']]
        rows.append('| ' + ' | '.join(cells) + ' |')
    return '\n'.join(rows)


def enhance_metrics(metrics: Dict[str, Any]) -> Dict[str, Any]:
    """Enhance raw metrics with formatted versions for templates."""
    enhanced = metrics.copy()

    # Format lists
    if 'key_achievements' in metrics and isinstance(metrics['key_achievements'], list):
        enhanced['achievements_bullets'] = format_bullet_list(metrics['key_achievements'])
        enhanced['achievements_numbered'] = format_numbered_list(metrics['key_achievements'])

    if 'challenges' in metrics and isinstance(metrics['challenges'], list):
        enhanced['challenges_bullets'] = format_bullet_list(metrics['challenges'])

    if 'technologies' in metrics and isinstance(metrics['technologies'], list):
        enhanced['technologies_list'] = ', '.join(metrics['technologies'])

    if 'patterns' in metrics and isinstance(metrics['patterns'], list):
        enhanced['patterns_list'] = ', '.join(metrics['patterns'])

    # Format metrics for display
    if 'tests_added' in metrics:
        enhanced['tests_added_formatted'] = f"{metrics['tests_added']:,}"

    if 'lines_added' in metrics and 'lines_deleted' in metrics:
        enhanced['lines_changed'] = f"+{metrics['lines_added']:,} / -{metrics['lines_deleted']:,}"

    # Create before/after lists for common scenarios
    if 'coverage_before' in metrics and 'coverage_after' in metrics:
        enhanced['coverage_comparison'] = (
            f"{metrics['coverage_before']} → {metrics['coverage_after']}"
        )

    return enhanced


def render_template(template_id: str, metrics: Dict[str, Any],
                   extra_vars: Dict[str, str] = None) -> str:
    """Render a template with metrics."""
    if template_id not in TEMPLATES:
        raise ValueError(f"Unknown template: {template_id}")

    template = TEMPLATES[template_id]
    enhanced_metrics = enhance_metrics(metrics)

    # Merge with extra variables
    if extra_vars:
        enhanced_metrics.update(extra_vars)

    # Set defaults for common variables to avoid KeyError
    defaults = {
        'hook': '',
        'context': '',
        'action': 'achieved significant progress',
        'timeframe': 'this sprint',
        'results': 'Significant improvements achieved.',
        'takeaways': 'Lessons learned documented.',
        'cta': 'Follow for more insights.',
        'hashtags': '#SoftwareEngineering #TechDebt',
        'coverage_before': '0%',
        'coverage_after': '0%',
        'coverage_gain': '+0%',
        'duration': 'N/A',
        'tests_added': '0',
        'sprint_number': 'N/A',
        'additional_metrics': '',
        'lesson': '',
        'approach': '',
        'problem': '',
        'conventional_approach': 'Rewrite everything',
        'our_approach': 'Incremental refactoring',
        'solution_type': 'approach',
        'problem_statement': '',
        'challenge': '',
        'solution_intro': '',
        'language': 'java',
        'code_snippet': '// Code example',
        'explanation': '',
        'title': 'Article Title',
        'promise': '',
        'content_description': 'our approach',
        'target_audience': 'an engineer dealing with legacy code',
        'value_proposition': 'a proven path forward',
        'problem_title': '',
        'situation': '',
        'pain_points_intro': '',
        'pain_point_list': '',
        'obvious_solution': 'Rewrite',
        'obvious_solution_problems': '',
        'better_solution': 'Refactor incrementally',
        'better_solution_intro': '',
        'results_intro': '',
        'metrics_table': '',
        'timeline': '',
        'before_after_comparison': '',
        'impact_summary': '',
        'lesson_list': '',
        'reflection': '',
        'advice': '',
        'table_rows': '',
        'before_list': '',
        'after_list': '',
        'items': '',
    }

    # Merge defaults with enhanced metrics
    template_vars = {**defaults, **enhanced_metrics}

    try:
        return template.safe_substitute(**template_vars)
    except KeyError as e:
        raise ValueError(f"Missing required variable for template '{template_id}': {e}")


def list_templates() -> str:
    """List all available templates with descriptions."""
    descriptions = {
        'linkedin_achievement': 'LinkedIn post celebrating an achievement with metrics',
        'linkedin_coverage': 'LinkedIn post about test coverage improvements',
        'linkedin_pivot': 'LinkedIn post about strategic pivot or contrarian approach',
        'linkedin_technical': 'LinkedIn post with technical depth and code snippet',
        'article_executive_summary': 'Substack article executive summary section',
        'article_problem': 'Substack article problem statement section',
        'article_results': 'Substack article results and metrics section',
        'article_lessons': 'Substack article lessons learned section',
        'metrics_table': 'Markdown table for before/after metrics',
        'before_after': 'Before/after comparison list',
        'bullet_list': 'Simple bullet point list formatter',
    }

    lines = ["Available Templates:", ""]
    for template_id in sorted(TEMPLATES.keys()):
        desc = descriptions.get(template_id, 'No description')
        lines.append(f"  {template_id:<30} - {desc}")

    return '\n'.join(lines)


def main():
    parser = argparse.ArgumentParser(
        description='Render content templates with metrics',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )

    parser.add_argument(
        'template_id',
        nargs='?',
        help='Template ID to render'
    )
    parser.add_argument(
        'metrics_file',
        nargs='?',
        help='Path to metrics JSON file (or "-" for stdin)'
    )
    parser.add_argument(
        '--list-templates',
        action='store_true',
        help='List all available templates'
    )
    parser.add_argument(
        '--var',
        action='append',
        metavar='KEY=VALUE',
        help='Additional variables (can be used multiple times)'
    )
    parser.add_argument(
        '--output',
        '-o',
        type=Path,
        help='Output file (default: stdout)'
    )

    args = parser.parse_args()

    # Handle --list-templates
    if args.list_templates:
        print(list_templates())
        return

    # Validate required arguments
    if not args.template_id:
        parser.error("template_id is required (or use --list-templates)")
    if not args.metrics_file:
        parser.error("metrics_file is required")

    try:
        # Load metrics
        metrics = load_metrics(args.metrics_file)

        # Parse extra variables
        extra_vars = {}
        if args.var:
            for var in args.var:
                if '=' not in var:
                    print(f"Warning: Ignoring invalid variable format: {var}",
                          file=sys.stderr)
                    continue
                key, value = var.split('=', 1)
                extra_vars[key.strip()] = value.strip()

        # Render template
        output = render_template(args.template_id, metrics, extra_vars)

        # Write output
        if args.output:
            args.output.write_text(output, encoding='utf-8')
            print(f"Output written to: {args.output}", file=sys.stderr)
        else:
            print(output)

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
