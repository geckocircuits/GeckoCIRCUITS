"""Replay an M6 audit without importing circuits or running simulations."""

import argparse
from collections import Counter, defaultdict
import json
import os
from pathlib import Path
import re
import subprocess
import sys

from batch import digest, read, write


def capability(issue):
    code, message = issue['code'], issue.get('message') or ''
    component = issue.get('component') or ''
    if message.startswith('Select a root AC analysis'):
        return 'hierarchy AC-only gate'
    if code == 'UNSUPPORTED_TRANSIENT_OPTION':
        return '.tran options'
    if code == 'UNSUPPORTED_DIRECTIVE':
        return component.lower() or 'other directive'
    if code == 'DIODE_MODEL_APPROXIMATION':
        return 'exponential diode' if 'exponential' in message else 'ideal diode mapping'
    if code in ('MALFORMED_ELEMENT', 'MALFORMED_PARAMETER') and (
            'scalar' in message.lower() or code == 'MALFORMED_PARAMETER'):
        return 'expression parser'
    if code == 'UNSUPPORTED_SYMBOL':
        symbol = message.split(': ', 1)[-1].replace('\\', '/').lower()
        name = symbol.rsplit('/', 1)[-1]
        if name in ('bv', 'bi'):
            return 'behavioural sources'
        if name in ('nmos', 'pmos', 'nmos4', 'pmos4'):
            return 'MOSFET'
        if name in ('npn', 'pnp'):
            return 'BJT'
        if 'digital/' in symbol:
            return 'digital gates'
        if name == 'ind2':
            return 'coupled inductors'
        if name in ('e', 'e2', 'epoly', 'g', 'gpoly', 'h', 'f'):
            return 'linear controlled sources'
        if component.upper().startswith('X'):
            return 'subcircuit blocks'
    if code == 'UNSUPPORTED_ELEMENT':
        letter = component[:1].upper() or message.rsplit(': ', 1)[-1][:1].upper()
        if letter and letter in 'EGHF':
            return 'linear controlled sources'
        if letter == 'K':
            return 'coupled inductors'
        if letter == 'B':
            return 'behavioural sources'
    return f'other: {code}'


def verify_inventory(audit, expected_sha):
    index_path = audit / 'artifact-sha256.json'
    if digest(index_path) != expected_sha:
        raise ValueError('Audit inventory hash differs from the supplied seal')
    index = read(index_path)
    required = {'preview.json', 'prior-qualification.json'}
    for folder in ('', 'unselected/', 'legacy-non-ac/'):
        required.update(folder + name for name in ('results.json', 'input-provenance-before.json',
                                                   'input-provenance-after.json'))
    if (audit / 'run-metadata.json').exists():
        required.add('run-metadata.json')
    if required - index.keys():
        raise ValueError(f'Consumed audit files are missing from the inventory: {sorted(required - index.keys())}')
    for relative, pin in index.items():
        path = (audit / relative).resolve()
        if not path.is_relative_to(audit.resolve()):
            raise ValueError(f'Inventory escapes audit: {relative}')
        if path.stat().st_size != pin['bytes'] or digest(path) != pin['sha256']:
            raise ValueError(f'Audit file differs: {relative}')
    for name in ('results.json', 'unselected/results.json', 'legacy-non-ac/results.json'):
        for row in read(audit / name):
            if row['output']:
                output = Path(row['output']).resolve()
                if not output.is_relative_to(audit.resolve()):
                    raise ValueError('Indexed result output escapes the audit')
                required.add(output.relative_to(audit.resolve()).as_posix())
                required.add((output.parent / 'result.json').relative_to(audit.resolve()).as_posix())
    if required - index.keys():
        raise ValueError(f'Consumed output files are missing from the inventory: {sorted(required - index.keys())}')
    return len(index)


def summarize(cases, prior):
    accepted = {(row['archive_path'], row['directive_index']): row for row in prior}
    if len(accepted) != len(prior):
        raise ValueError('Duplicate historical qualification')
    metrics = defaultdict(Counter)
    gaps = defaultdict(set)
    non_ac_gaps = defaultdict(set)
    inventory = []
    known = set()
    for case in cases:
        path = case['archive_path']
        attempts = case['attempts']
        canonical = attempts['selected_profile']
        observed = {capability(issue) for attempt in attempts.values() for issue in attempt['issues']}
        diagnostic_limits = {name: attempt['status'] for name, attempt in attempts.items()
                             if attempt['status'] == 'process_failure' or attempt['status'].endswith('_incomplete')}
        unexpanded = 'subcircuit blocks' in observed or 'hierarchy AC-only gate' in observed
        for number, directive in enumerate(case['original_analyses']):
            key = (path, number)
            known.add(key)
            family = directive.split()[0].lower()
            counters = metrics[family]
            counters['declared'] += 1
            counters['converts'] += canonical['status'] == 'complete_import'
            counters['opens_in_core'] += bool(canonical['parse'] and canonical['parse']['ipes_parse'] == 'passed')
            history = accepted.get(key)
            if history:
                if history['sha256'] != case['sha256'] or history['original_directive'] != directive:
                    raise ValueError(f'Historical qualification source differs: {path}')
                counters['runs_to_completion'] += 1
                counters['numerically_accepted'] += 1
            else:
                counters['runs_to_completion'] += 0
                counters['numerically_accepted'] += 0
            tier = 'A' if family == '.ac' or re.search(r'\b(?:avg|averaged)\b', path.lower()) else 'B'
            solver_issues = []
            if 'exponential diode' in observed:
                solver_issues.append(163)
            if {'MOSFET', 'BJT'} & observed:
                solver_issues.append(164)
            if solver_issues:
                tier = 'C'
            analysis_id = f'{path}#{number}'
            for gap in observed:
                gaps[gap].add(analysis_id)
            legacy = attempts.get('legacy_non_ac_without_stock_or_hierarchy')
            legacy_gaps = {capability(issue) for issue in legacy['issues']} if legacy else set()
            if 'legacy_non_ac_without_stock_or_hierarchy' in diagnostic_limits:
                legacy_gaps.add('resource-limited or failed diagnostics')
            if family != '.ac':
                for gap in legacy_gaps:
                    non_ac_gaps[gap].add(analysis_id)
            inventory.append({'archive_path': path, 'sha256': case['sha256'], 'directive_index': number,
                              'directive': directive, 'tier': tier, 'tier_provisional': unexpanded or bool(diagnostic_limits),
                              'diagnostic_limits': diagnostic_limits,
                              'blockers': sorted(observed), 'solver_issues': solver_issues,
                              'behavioural_spike_issue': 162 if 'behavioural sources' in observed else None,
                              'historical_qualification': history,
                              'legacy_blockers': sorted(legacy_gaps)})
    if set(accepted) - known:
        raise ValueError('Historical qualification is absent from the inventory')
    return {'by_analysis': dict(metrics),
            'capability_gaps': {key: len(value) for key, value in sorted(gaps.items())},
            'non_ac_legacy_capability_gaps': {key: len(value) for key, value in sorted(non_ac_gaps.items())},
            'tier_counts_provisional': dict(Counter(row['tier'] for row in inventory)),
            'analyses': inventory,
            'context_schematics': sum(not case['original_analyses'] for case in cases)}


def markdown(report):
    fresh = report.get('provenance', {}).get('fresh_import_attempts', 0)
    evidence = (f'Fresh sequential import audit: {fresh} attempts. No new simulations.' if fresh else
                'Historical audit replay. No imports or simulations were executed by this command.')
    lines = ['# M6 scoreboard', '', evidence, '',
             '| Analysis | Declared | Converts | Opens in core | Runs completed (historical) | Accepted (historical) |',
             '|---|---:|---:|---:|---:|---:|']
    for family, row in sorted(report['by_analysis'].items()):
        keys = ('declared', 'converts', 'opens_in_core', 'runs_to_completion', 'numerically_accepted')
        lines.append('| ' + family + ' | ' + ' | '.join(str(row.get(key, 0)) for key in keys) + ' |')
    lines += ['', 'Converts and opens use the selected-profile path. Opens means successful core IPES deserialization; '
              'it does not prove readiness or convergence. Run and acceptance counts come from the sealed historical '
              'qualification ledger, including the single authorized Filtered PID exception. They are not new runs.', '',
              '## Capability gaps in non-AC legacy imports', '', '| Capability | Analyses touched |', '|---|---:|']
    for gap, count in sorted(report['non_ac_legacy_capability_gaps'].items(), key=lambda pair: (-pair[1], pair[0])):
        lines.append(f'| {gap} | {count} |')
    lines += ['', 'Counts overlap. Resource-limited or failed diagnostics and unexpanded subcircuits hide further blockers. '
              'A lower diagnostic count caused by a capped or failed attempt is not a capability improvement. Tier assignments are provisional '
              'where hierarchy is unexpanded; observed device-physics blockers link to #163/#164. Behavioural '
              'sources remain subject to #162 and are not automatically moved to Tier C.', '',
              f'Contexts retained: {report["context_schematics"]}. Newly accepted analyses: 0.', '']
    return '\n'.join(lines)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--audit-dir', type=Path, required=True)
    parser.add_argument('--inventory-sha256', required=True)
    parser.add_argument('--output-dir', type=Path, required=True)
    args = parser.parse_args()
    audit, output = args.audit_dir.resolve(), args.output_dir.resolve()
    if output.exists() or output.is_relative_to(audit):
        raise ValueError('Select a new output folder outside the sealed audit')
    verified_files = verify_inventory(audit, args.inventory_sha256)
    output.mkdir(parents=True)
    env = dict(os.environ, GECKO_M6_AUDIT_DIR=str(audit), GECKO_M6_RUN_DIR=str(output))
    subprocess.run([sys.executable, '-B', str(Path(__file__).with_name('combine.py'))], env=env, check=True,
                   stdout=subprocess.DEVNULL)
    report = summarize(read(output / 'combined-classification.json'), read(audit / 'prior-qualification.json'))
    metadata_path = audit / 'run-metadata.json'
    metadata = read(metadata_path) if metadata_path.exists() else {}
    report['provenance'] = {'audit_directory': str(audit), 'inventory_sha256': args.inventory_sha256,
                            'verified_files': verified_files,
                            'fresh_import_attempts': metadata.get('fresh_import_attempts', 0), 'fresh_simulations': 0}
    write(output / 'scoreboard.json', report)
    text = markdown(report)
    with (output / 'scoreboard.md').open('x', encoding='utf-8') as stream:
        stream.write(text)
    print(text)


if __name__ == '__main__':
    main()
