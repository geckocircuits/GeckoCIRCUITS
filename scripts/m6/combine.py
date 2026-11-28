import collections
from pathlib import Path
import re
import os

from batch import BUDGET, CASE_LIMIT, FLOOR, OUT, digest, read, write

AUDIT = Path(os.environ.get('GECKO_M6_AUDIT_DIR', str(OUT)))
cases = read(AUDIT / 'preview.json')['cases']
recorded_floor = read(AUDIT / 'preview.json').get('storage_floor_bytes', 4_200_000_000)
series = {
    'selected_profile': read(AUDIT / 'results.json'),
    'unselected_with_stock_and_roots': read(AUDIT / 'unselected/results.json'),
    'legacy_non_ac_without_stock_or_hierarchy': read(AUDIT / 'legacy-non-ac/results.json'),
}
indexed = {key: {r['id']: r for r in rows} for key, rows in series.items()}
assert [len(table) for table in indexed.values()] == [467, 467, 345]
assert [len(rows) for rows in series.values()] == [467, 467, 345]
assert all((folder / 'input-provenance-before.json').read_bytes() ==
           (folder / 'input-provenance-after.json').read_bytes() ==
           (AUDIT / 'input-provenance-before.json').read_bytes()
           for folder in (AUDIT, AUDIT / 'unselected', AUDIT / 'legacy-non-ac'))

def issue_family(issue):
    message = issue['message'] or ''
    if issue['code'] == 'CONVERSION_FAILED':
        for start, name in (
                ('Select a root AC analysis', 'Hierarchy requires a root AC analysis'),
                ('Stock-symbol verification requires an AC schematic', 'Stock-symbol verification requires AC'),
                ('The ASC profile requires a complete import', 'Selected profile rejects the first import issue'),
                ('Invalid circuit hierarchy', 'Invalid circuit hierarchy')):
            if message.startswith(start):
                return name
    if issue['code'] == 'INVALID_HIERARCHY_CALL' and ': ' in message:
        return re.sub(r'^.*?[.]asy:\d+: ', '', message)
    return message

def count_groups(groups):
    return [{'code': code, 'message': message, 'case_count': len(paths),
             'examples': sorted(paths)[:5]} for (code, message), paths in
            sorted(groups.items(), key=lambda pair: (-len(pair[1]), pair[0]))]

analyses = {}
classification = []
for key, table in indexed.items():
    resource_anomalies = []
    stats = collections.defaultdict(collections.Counter)
    parsers = collections.Counter()
    groups = collections.defaultdict(set)
    for case in cases:
        row = table.get(case['id'])
        if row is None:
            assert key.startswith('legacy') and any(d.startswith('.ac ') for d in case['analysis_directives'])
            continue
        assert row['archive_path'] == case['archive_path'] and row['source_sha256'] == case['sha256']
        assert min(row['free_bytes_before'].values()) >= recorded_floor
        family = case['analysis_directives'][0].split()[0] if case['analysis_directives'] else 'context'
        stats[family][row['status']] += 1
        parse = row['parse']
        parsers[parse['ipes_parse'] if parse else 'not attempted'] += 1
        if row['status'] in ('output_limit_incomplete', 'storage_guard_incomplete', 'timeout_incomplete') or row['output_bytes'] > CASE_LIMIT or any('OutOfMemoryError' in line for line in row['other_output']):
            resource_anomalies.append({'archive_path': row['archive_path'], 'id': row['id'],
                                       'condition': row['status'] if row['status'].endswith('_incomplete') else 'output cap exceeded' if row['output_bytes'] > CASE_LIMIT else 'JVM heap exhaustion',
                                       'output_bytes': row['output_bytes'], 'status': row['status']})
        if row['output']:
            output = Path(row['output'])
            assert output.is_relative_to(AUDIT) and output.stat().st_size == row['output_bytes']
            assert digest(output) == row['output_sha256']
            assert read(output.parent / 'result.json') == row
        for issue in row['issues']:
            groups[(issue['code'], issue_family(issue))].add(row['archive_path'])
    analyses[key] = {'count_by_status': collections.Counter(r['status'] for r in table.values()),
                     'by_original_analysis': stats, 'ipes_parser': parsers,
                     'issue_families': count_groups(groups), 'resource_anomalies': resource_anomalies}

for case in cases:
    classification.append({
        'id': case['id'], 'archive_path': case['archive_path'], 'sha256': case['sha256'],
        'original_analyses': case['analysis_directives'], 'has_hierarchy_ports': case['has_hierarchy_ports'],
        'catalog_referenced_by': case['referenced_by'], 'catalog_symbol_dependencies': case['symbols'],
        'catalog_library_dependencies': case['libraries'], 'native_context_witness': case['context_witness'],
        'attempts': {key: table[case['id']] for key, table in indexed.items() if case['id'] in table},
    })
write(OUT / 'combined-classification.json', classification)
write(OUT / 'combined-summary.json', analyses)
for key, outcome in analyses.items():
    print(key, outcome['count_by_status'], outcome['by_original_analysis'], outcome['ipes_parser'])
    for item in outcome['issue_families'][:15]:
        print(item['case_count'], item['code'], item['message'])
assert sum(path.stat().st_size for path in OUT.rglob('*') if path.is_file()) < BUDGET
