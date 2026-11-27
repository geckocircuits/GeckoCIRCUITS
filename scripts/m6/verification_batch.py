"""Verify pinned M6 evidence and run supported numerical metrics without simulators."""

import argparse
from collections import Counter
import csv
from hashlib import sha256
import json
from pathlib import Path
import re
import stat

from ac_compare import POLICY as AC_POLICY, compare_case_inputs as compare_ac
from storage_guard import CASE_RESERVE, DIRTY_RESERVE, require_free_space
from transient_compare import _unique_keys, compare_case_inputs as compare_transient

SCHEMA = 'm6-metric-batch-v1'
POLICIES = ('transient-windowed-v1', 'transient-windowed-v2')
STAGES = ('source', 'runtime', 'native', 'physics')
INPUTS = ('source', 'reference', 'candidate', 'manifest')


def _digest(value):
    if not isinstance(value, str) or not re.fullmatch(r'[0-9a-f]{64}', value):
        raise ValueError('Use a lowercase SHA-256 digest')
    return value


def _json(data):
    return json.loads(data, object_pairs_hook=_unique_keys)


def _keys(record, required, name):
    if not isinstance(record, dict) or set(record) != set(required):
        raise ValueError(f'Use exactly the required fields for {name}')


def _read_pin(base, expected, *, keep):
    _keys(expected, ('path', 'sha256'), 'file pin')
    name = expected['path']
    if not isinstance(name, str) or not name or not name.isprintable():
        raise ValueError('Use a nonempty local path for each pin')
    digest = _digest(expected['sha256'])
    path = (base / name).resolve(strict=True)
    if not stat.S_ISREG(path.stat().st_mode):
        raise ValueError('Pin a regular file')
    with path.open('rb') as stream:
        if keep:
            data = stream.read()
            length = len(data)
            actual = sha256(data).hexdigest()
        else:
            length = 0
            hashed = sha256()
            for block in iter(lambda: stream.read(1024 * 1024), b''):
                hashed.update(block)
                length += len(block)
            actual = hashed.hexdigest()
            data = None
    if actual != digest:
        raise ValueError(f'Input digest differs for {name}')
    return data, dict(path=str(path), sha256=actual, bytes=length)


def _cases(data):
    manifest = _json(data)
    _keys(manifest, ('schema', 'cases'), 'batch manifest')
    if manifest['schema'] != SCHEMA or not isinstance(manifest['cases'], list) or not manifest['cases']:
        raise ValueError('Use the supported batch schema with at least one case')
    ids, originals = set(), set()
    for case in manifest['cases']:
        _keys(case, ('id', 'archive_path', 'source_sha256', 'analysis', 'policy', 'inputs', 'receipts'), 'case')
        if not isinstance(case['policy'], str) or not case['policy']:
            raise ValueError('Declare a comparison policy ID')
        identity = _identity(case)
        case_id = identity['id']
        if not isinstance(case_id, str) or not re.fullmatch(r'[a-z0-9][a-z0-9_-]{0,63}', case_id):
            raise ValueError('Use a stable lowercase case ID')
        archive = identity['archive_path']
        if not isinstance(archive, str) or not archive.isprintable() or '\\' in archive or \
                ':' in archive or any(part in ('', '.', '..') for part in archive.split('/')):
            raise ValueError('Use a normalized relative archive path')
        _digest(identity['source_sha256'])
        analysis = identity['analysis']
        _keys(analysis, ('index', 'directive'), 'analysis identity')
        if type(analysis['index']) is not int or analysis['index'] < 0 or \
                not isinstance(analysis['directive'], str) or not analysis['directive'].isprintable() or \
                not re.match(r'^\.(?:tran|ac)(?:\s|$)', analysis['directive'], re.IGNORECASE):
            raise ValueError('Use an original AC or transient directive and nonnegative index')
        original = archive.casefold(), analysis['index']
        if case_id in ids or original in originals:
            raise ValueError('Declare each case ID and original analysis only once')
        ids.add(case_id)
        originals.add(original)
    return manifest['cases']


def _identity(case):
    return {key: case[key] for key in ('id', 'archive_path', 'source_sha256', 'analysis')}


def _stage(base, stage, expected, identity, policy, bindings, predecessors):
    data, pin = _read_pin(base, expected, keep=True)
    receipt = _json(data)
    _keys(receipt, ('stage', 'status', 'case', 'policy', 'inputs', 'prior_receipts', 'artifacts'), 'stage receipt')
    if receipt['stage'] != stage or receipt['status'] != 'PASS' or receipt['case'] != identity or \
            receipt['policy'] != policy or receipt['inputs'] != bindings or receipt['prior_receipts'] != predecessors:
        raise ValueError(f'{stage} receipt differs from its expected case or evidence bindings')
    artifacts = receipt['artifacts']
    if not isinstance(artifacts, list) or not artifacts:
        raise ValueError(f'Pin the supporting artifacts for the {stage} receipt')
    observed = [_read_pin(base, item, keep=False)[1] for item in artifacts]
    if len({item['path'] for item in observed}) != len(observed):
        raise ValueError(f'Pin each {stage} artifact once')
    return dict(receipt=pin, artifacts=observed)


def _compare(base, case, report):
    identity = report['case']
    is_ac = case['analysis']['directive'].split()[0].casefold() == '.ac'
    policy = case['policy']
    supported = (AC_POLICY,) if is_ac else POLICIES
    if policy not in supported:
        report.update(status='unsupported', reason='Policy is unsupported for this analysis')
        return
    _keys(case['inputs'], INPUTS, 'comparison inputs')
    _keys(case['receipts'], STAGES, 'stage receipts')
    bindings = {}
    for name in INPUTS:
        _keys(case['inputs'][name], ('path', 'sha256'), 'comparison input pin')
        bindings[name] = _digest(case['inputs'][name]['sha256'])
    if bindings['source'] != identity['source_sha256']:
        raise ValueError('The source input digest must match the original source identity')
    pins, verified = {}, {}
    report.update(inputs=pins, verified_stage_bindings=verified)

    def load(name, *, keep=True):
        data, pins[name] = _read_pin(base, case['inputs'][name], keep=keep)
        return data

    load('source', keep=False)
    prior = {}
    for stage in STAGES:
        verified[stage] = _stage(base, stage, case['receipts'][stage], identity, policy, bindings, prior)
        prior[stage] = verified[stage]['receipt']['sha256']
    compare = compare_ac if is_ac else compare_transient
    metrics = compare(load, pins, policy=policy)
    report.update(status='metric_pass' if metrics['passed'] else 'metric_fail', metrics=metrics)


def _report(path, report):
    data = (json.dumps(report, indent=2, allow_nan=False) + '\n').encode()
    with path.open('xb') as stream:
        stream.write(data)
    return sha256(data).hexdigest()


def _output_on_d(output):
    return output.drive.upper() == 'D:'


def run_batch(manifest, manifest_sha256, output):
    """Write exclusive case reports plus one hash-bound audit index."""
    require_free_space(CASE_RESERVE)
    data, manifest_pin = _read_pin(manifest.parent, {'path': str(manifest.resolve()),
                                                  'sha256': manifest_sha256}, keep=True)
    cases = _cases(data)
    output = output.resolve()
    if not _output_on_d(output):
        raise ValueError('Use a new output directory on D:')
    output.mkdir()
    indexed = []
    for case in cases:
        require_free_space(DIRTY_RESERVE)
        report = dict(case=_identity(case), policy=case['policy'],
                      qualification='independent_review_required')
        try:
            _compare(manifest.parent, case, report)
        except (OSError, ValueError, TypeError, KeyError, OverflowError, csv.Error) as error:
            report.update(status='invalid', reason=str(error))
        require_free_space(DIRTY_RESERVE)
        name = 'case-' + case['id'] + '.json'
        digest = _report(output / name, report)
        indexed.append(dict(id=case['id'], status=report['status'], report=name, sha256=digest))
    counts = dict(Counter(entry['status'] for entry in indexed))
    audit = dict(schema=SCHEMA, scope='metrics_and_receipt_bindings_only', manifest=manifest_pin,
                 case_count=len(cases), counts=counts, cases=indexed,
                 qualification='independent_review_required')
    require_free_space(DIRTY_RESERVE)
    digest = _report(output / 'batch-audit.json', audit)
    with (output / 'batch-audit.sha256').open('x', encoding='ascii', newline='\n') as stream:
        stream.write(f'{digest}  batch-audit.json\n')
    return audit, digest


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--manifest-sha256', required=True)
    parser.add_argument('--output-dir', type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        audit, digest = run_batch(args.manifest, args.manifest_sha256, args.output_dir)
    except (OSError, ValueError, RuntimeError) as error:
        parser.error(str(error))
    print(json.dumps(dict(case_count=audit['case_count'], counts=audit['counts'], sha256=digest)))
    return 0 if audit['counts'].get('metric_pass', 0) == audit['case_count'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
