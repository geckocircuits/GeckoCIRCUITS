import collections
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time
from storage_guard import CASE_RESERVE, DIRTY_RESERVE, FLOOR, directory_bytes, require_free_space

BASE = Path(os.environ.get('GECKO_M6_EVIDENCE_ROOT', r'D:\codex-evidence\gecko-m6-20261124'))
OUT = Path(os.environ.get('GECKO_M6_RUN_DIR', str(BASE / 'archive-import-batch-01')))
HARNESS = Path(os.environ.get('GECKO_M6_HARNESS_DIR', str(BASE / 'archive-import-batch-01')))
MODE = os.environ.get('GECKO_M6_MODE', 'selected_profile')
if MODE not in ('selected_profile', 'unselected', 'legacy-non-ac'):
    raise ValueError(f'Unknown audit mode: {MODE}')
TEMP = Path(os.environ.get('GECKO_M6_TEMP_DIR', str(Path(r'D:\codex-temp\gecko-m6') / OUT.name)))
RUN_ROOT = Path(os.environ.get('GECKO_M6_RUN_ROOT', str(OUT)))
TEMP_ROOT = Path(os.environ.get('GECKO_M6_TEMP_ROOT', str(TEMP)))
AUTHOR = BASE / 'author-source'
STOCK = Path(r'C:\Users\tinix\AppData\Local\LTspice\lib\sym')
JAVA = Path(r'D:\codex-tools\gecko-m6\jdk21\jdk-21.0.13.1+2\bin\java.exe')
BIN = BASE / 'core-gui-lifecycle-diagnosis-01/filtered-original-gecko-prepare-01'
CORE = BIN / 'gecko-simulation-core-1.0-SNAPSHOT.jar'
DESKTOP = BIN / 'gecko-1.0-jar-with-dependencies.jar'
CAT = BASE / 'reference-manifest-20261124-011943.json'
CONTEXT = BASE / 'reference-contexts-20261124-013335.json'
PROFILE = 'ltspice-26.1.1-normal-27c-v1'
EXPECTED = {
    CAT: '65c59b24cba1b59bde95fb623414188b51b02e4fa0648bed4085bfaa53476a08',
    BASE / 'LTspice_SMPS.zip': 'a4df6bdaec111ec392bc43dda9c4d1bef1b082427d07cbe9d4ca951f9c53c7ec',
    CORE: '0892cf4ceba00a001a454e7938facb0f0d717d3318f6680f20ce76d9a5ab9623',
    DESKTOP: '9bd0b1f64a25acb878bc3e4e3f0292610893be707c48bc0c23b7b341e4f0dac4',
}
runtime_path = os.environ.get('GECKO_M6_RUNTIME_MANIFEST')
if runtime_path:
    runtime = json.loads(Path(runtime_path).read_text(encoding='utf-8'))
    JAVA, CORE, DESKTOP = (Path(runtime[name]['path']) for name in ('java', 'core', 'desktop'))
    EXPECTED = {CAT: EXPECTED[CAT], BASE / 'LTspice_SMPS.zip': EXPECTED[BASE / 'LTspice_SMPS.zip'],
                **{Path(runtime[name]['path']): runtime[name]['sha256'] for name in ('java', 'core', 'desktop')}}
BUDGET = 100_000_000
CASE_LIMIT = 2_000_000

def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def read(path):
    return json.loads(path.read_text(encoding='utf-8'))

def write(path, data):
    with path.open('x', encoding='utf-8') as stream:
        json.dump(data, stream, ensure_ascii=False, indent=2)
        stream.write('\n')

def storage(reserve=CASE_RESERVE):
    drives = require_free_space(reserve)
    if not OUT.resolve().is_relative_to(RUN_ROOT.resolve()) or not TEMP.resolve().is_relative_to(TEMP_ROOT.resolve()):
        raise RuntimeError('Mode directories must stay within the aggregate run and temporary roots')
    if TEMP_ROOT.resolve().is_relative_to(BASE.resolve()) or TEMP_ROOT.resolve().is_relative_to(RUN_ROOT.resolve()):
        raise RuntimeError('Test temporary files must be outside the evidence folders')
    total = directory_bytes(RUN_ROOT) + directory_bytes(TEMP_ROOT)
    if total >= BUDGET - CASE_LIMIT:
        raise RuntimeError(f'Storage guard: {drives}, evidence bytes {total}')
    return drives

def provenance(catalog):
    pinned = {str(path): digest(path) for path in (*EXPECTED, JAVA, CONTEXT, HARNESS / 'ImportAndParse.class')}
    for path, sha in EXPECTED.items():
        if pinned[str(path)] != sha:
            raise ValueError(f'Pinned input differs: {path}')
    author = {path.relative_to(AUTHOR).as_posix(): digest(path)
              for path in sorted(AUTHOR.rglob('*')) if path.is_file()}
    for relative, info in catalog['source_files'].items():
        if author.get(relative) != info['sha256']:
            raise ValueError(f'Archive source differs: {relative}')
    stock = {str(path): digest(path) for path in sorted(STOCK.rglob('*.asy'))}
    if not stock:
        raise ValueError('Stock ASY set is empty')
    return {'pinned': pinned, 'archive_and_local_author_files': author, 'stock_asy': stock}

def preview():
    storage()
    catalog = read(CAT)
    inputs = provenance(catalog)
    contexts = read(CONTEXT)['no_analysis_schematics']
    cases = []
    for number, (relative, info) in enumerate(catalog['schematics'].items(), 1):
        name = f'{number:03d}'
        case = {
            'id': name, 'source': str(AUTHOR / relative), 'archive_path': relative,
            'output': str(OUT / 'cases' / name / 'model.ipes'),
            **info, 'context_witness': contexts.get(relative),
        }
        cases.append(case)
    counts = collections.Counter(d.split()[0].lower() for c in cases for d in c['analysis_directives'])
    assert len(cases) == 467 and counts == {'.tran': 242, '.ac': 122, '.fra': 12, '.dc': 3}
    assert {c['archive_path'] for c in cases if not c['analysis_directives']} == set(contexts)
    assert len({c['source'].casefold() for c in cases}) == len({c['output'] for c in cases}) == 467
    assert not any(Path(c['output']).exists() for c in cases)
    if MODE == 'legacy-non-ac':
        cases = [case for case in cases if not any(d.split()[0].lower() == '.ac'
                                                 for d in case['analysis_directives'])]
        assert len(cases) == 345
    write(OUT / 'input-provenance-before.json', inputs)
    write(OUT / 'preview.json', {'profile': PROFILE, 'analysis_counts': counts, 'cases': cases,
                               'storage_floor_bytes': FLOOR, 'temp_directory': str(TEMP)})
    print(f'Preview: {len(cases)} unique inputs and new output paths, {len(inputs["archive_and_local_author_files"])} author files and {len(inputs["stock_asy"])} stock symbols', flush=True)

def monitor_case(process, start, output, temp):
    timed_out = False
    limit = False
    storage_error = None
    next_storage_check = 0
    while process.poll() is None:
        timed_out = time.monotonic() - start > 25
        try:
            limit = directory_bytes(output) + directory_bytes(temp) > CASE_LIMIT
            if time.monotonic() >= next_storage_check:
                storage(reserve=DIRTY_RESERVE)
                next_storage_check = time.monotonic() + 1
        except (RuntimeError, OSError) as failure:
            storage_error = str(failure)
        if timed_out or limit or storage_error:
            process.kill()
            break
        time.sleep(.035)
    code = process.wait(timeout=5)
    try:
        limit = limit or directory_bytes(output) + directory_bytes(temp) > CASE_LIMIT
        storage(reserve=DIRTY_RESERVE)
    except (RuntimeError, OSError) as failure:
        storage_error = str(failure)
    return code, timed_out, limit, storage_error


def run_case(case):
    drives = storage()
    start = time.monotonic()
    model = Path(case['output'])
    temp = TEMP / case['id']
    temp.mkdir(parents=True)
    model.parent.mkdir()
    args = [str(JAVA), '-Xms32m', '-Xmx512m', '-Djava.awt.headless=true', '-Dfile.encoding=UTF-8',
            f'-Djava.io.tmpdir={temp}',
            '-cp', os.pathsep.join(map(str, (HARNESS, CORE, DESKTOP))), 'ImportAndParse', case['source'],
            str(model)]
    if MODE != 'legacy-non-ac':
        args += ['--symbols', str(STOCK), '--source-root', str(AUTHOR), '--source-root', str(AUTHOR / 'Tof'),
                 '--source-root', str(AUTHOR / 'Tof' / 'Subcircuits')]
    if MODE == 'selected_profile':
        args += ['--profile', PROFILE]
    stdout_path = model.parent / 'stdout.txt'
    stderr_path = model.parent / 'stderr.txt'
    with stdout_path.open('xb') as stdout, stderr_path.open('xb') as stderr:
        with subprocess.Popen(args, cwd=temp, stdout=stdout, stderr=stderr) as process:
            code, timed_out, limit, storage_error = monitor_case(process, start, model.parent, temp)
    issues = []
    parser = None
    extra = []
    for path in (() if timed_out or limit or storage_error else (stdout_path, stderr_path)):
        for line in path.read_text(encoding='utf-8', errors='replace').splitlines():
            try:
                data = json.loads(line)
                if 'ipes_parse' in data:
                    parser = data
                else:
                    issues.append(data)
            except (ValueError, TypeError):
                extra.append(line)
    states = {0: 'complete_import', 1: 'conversion_failed', 2: 'partial_import'}
    status = 'timeout_incomplete' if timed_out else 'output_limit_incomplete' if limit else states.get(code, 'process_failure')
    if code == 1 and not timed_out and not limit and not storage_error and not any(i.get('code') == 'CONVERSION_FAILED' for i in issues):
        status = 'process_failure'
    if storage_error:
        status = 'storage_guard_incomplete'
    row = {
        'id': case['id'], 'archive_path': case['archive_path'], 'source_sha256': case['sha256'],
        'analysis_directives': case['analysis_directives'], 'status': status, 'return_code': code,
        'elapsed_seconds': round(time.monotonic() - start, 3), 'free_bytes_before': drives,
        'output': str(model) if model.exists() else None, 'output_sha256': digest(model) if model.exists() else None,
        'output_bytes': model.stat().st_size if model.exists() else 0, 'issues': issues,
        'parse': parser, 'other_output': extra,
        'temp_directory': str(temp), 'temp_bytes': directory_bytes(temp), 'storage_error': storage_error,
    }
    write(model.parent / 'result.json', row)
    if storage_error:
        raise RuntimeError(storage_error)
    return row

def execute():
    storage()
    cases = read(OUT / 'preview.json')['cases']
    initial = read(OUT / 'input-provenance-before.json')
    catalog = read(CAT)
    if initial != provenance(catalog):
        raise RuntimeError('Source inputs changed after the preview')
    (OUT / 'cases').mkdir()
    rows = []
    for case in cases:
        rows.append(run_case(case))
        if len(rows) == 10 or len(rows) % 50 == 0:
            print(len(rows), dict(collections.Counter(r['status'] for r in rows)), flush=True)
        if rows[-1]['status'] == 'process_failure' and len(rows) <= 3:
            raise RuntimeError(f'Check packaged runtime: case {case["id"]}')
    after = provenance(catalog)
    write(OUT / 'input-provenance-after.json', after)
    if initial != after:
        raise RuntimeError('Source inputs changed during the batch')
    write(OUT / 'results.json', rows)
    print('Finished', len(rows), dict(collections.Counter(r['status'] for r in rows)), flush=True)

if __name__ == '__main__':
    try:
        {'--preview': preview, '--execute': execute, '--storage-check': storage}[sys.argv[1]]()
    except (RuntimeError, OSError) as failure:
        print(str(failure), file=sys.stderr)
        sys.exit(1)
