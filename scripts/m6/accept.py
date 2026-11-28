"""Accept or reject original M6 analyses end to end, one case after another.

Per case: import the unchanged author schematic and reopen it in core, run LTspice in batch on the
unchanged schematic, run the same analysis in GeckoCIRCUITS, compare every trace present in both
outputs with ac_compare.py, and append one JSON row. A failing case is recorded and the batch continues.
Runs from WSL. Every file this command writes, including Windows temp and profile folders, stays
under --output-dir, which must be on a drive with at least 3 GB free (checked before each case).
"""
import argparse
from collections import Counter, defaultdict
import csv
from hashlib import sha256
import io
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys
import time
import zipfile

from ac_compare import FLOORS, compare_case_inputs

REPO = Path(__file__).resolve().parents[2]
EVIDENCE = Path('/mnt/d/codex-evidence/gecko-m6-20261124')
PROFILE = 'ltspice-26.1.1-normal-27c-v1'
MIN_FREE = 3_000_000_000
FREQUENCY_RTOL = 1e-12  # Same grid tolerance as the hand qualifications of ID106 to ID119.


class StageError(Exception):
    def __init__(self, stage, message):
        super().__init__(message)
        self.stage = stage


def digest(path):
    with open(path, 'rb') as stream:
        return sha256(stream.read()).hexdigest()


def windows(path):
    return subprocess.run(['wslpath', '-w', str(path)], check=True, capture_output=True, text=True).stdout.strip()


def require_space(path):
    free = shutil.disk_usage(path).free
    if free < MIN_FREE:
        raise RuntimeError(f'Storage guard: {free} bytes free under {path}, need {MIN_FREE}. Batch stopped.')
    return free


def execute(stage, command, timeout, **options):
    try:
        return subprocess.run(command, capture_output=True, text=True, errors='replace', timeout=timeout, **options)
    except subprocess.TimeoutExpired:
        raise StageError(stage, f'timeout after {timeout} s') from None


# ---------------------------------------------------------------- stage 1: import and reopen

def import_case(ctx, source, model):
    def attempt(*options):
        result = execute('import', ['java', '-Djava.awt.headless=true', f'-Djava.io.tmpdir={ctx["tmp"]}',
                                    '-cp', f'{ctx["harness"]}:{ctx["jar"]}', 'ImportAndParse',
                                    str(source), str(model), '--symbols', str(ctx['stock'] / 'sym'), *options],
                         ctx['timeout'])
        lines = [json.loads(line) for line in (result.stdout + result.stderr).splitlines() if line.startswith('{')]
        issues = [f'{item["code"]}: {item.get("message")}' for item in lines if 'code' in item]
        parsed = any(item.get('ipes_parse') == 'passed' for item in lines)
        if result.returncode == 0 and parsed:
            return None
        detail = issues[0] if issues else (result.stderr.strip().splitlines() or ['no diagnostic'])[-1]
        state = {1: 'import failed', 2: 'import partial'}.get(result.returncode, f'import exit {result.returncode}')
        return f'{state}: {detail}' if parsed or result.returncode else f'reopen failed: {detail}'

    author = ctx['author']
    failure = attempt('--source-root', str(author), '--source-root', str(author / 'Tof'),
                      '--source-root', str(author / 'Tof' / 'Subcircuits'), '--profile', PROFILE, '--behavioural-sources')
    if failure is None:
        return []
    # The opt-in exponential diode exists only on the flat, unselected route (importer Main rejects it otherwise).
    if 'exponential' in failure.casefold() and attempt('--exponential-diodes') is None:
        return ['exponential-diodes']
    raise StageError('import', failure)


# ---------------------------------------------------------------- stage 2: LTspice batch

def ltspice_pids():
    listing = subprocess.run(['tasklist.exe', '/FI', 'IMAGENAME eq LTspice.exe', '/FO', 'CSV', '/NH'],
                             capture_output=True, text=True, errors='replace').stdout
    return {row[1] for row in csv.reader(io.StringIO(listing)) if len(row) > 1 and row[0] == 'LTspice.exe'}


def decode_log(data):
    return data.decode('utf-16-le' if b'\x00' in data[:64] else 'utf-8', errors='replace').lstrip('﻿')


def run_ltspice(ctx, source, folder):
    before = ltspice_pids()
    try:
        execute('ltspice', [str(ctx['ltspice']), '-b', windows(source)], ctx['timeout'],
                cwd=source.parent, env=ctx['windows_env'])
    except StageError:
        # ponytail: kills LTspice processes that appeared during this run; a concurrent foreign launch would die too.
        for pid in ltspice_pids() - before:
            subprocess.run(['taskkill.exe', '/PID', pid, '/F'], capture_output=True)
        raise
    folder.mkdir()
    for suffix in ('.raw', '.op.raw', '.net', '.log'):
        if source.with_suffix(suffix).exists():
            os.replace(source.with_suffix(suffix), folder / (source.stem + suffix))
    log = folder / (source.stem + '.log')
    text = decode_log(log.read_bytes()) if log.exists() else ''
    raw = folder / (source.stem + '.raw')
    if not raw.exists():
        errors = [line for line in text.splitlines() if 'error' in line.casefold() or 'cannot' in line.casefold()]
        raise StageError('ltspice', f'no RAW: {(errors or ["LTspice wrote no log"])[0]}')
    return raw, folder / (source.stem + '.net'), (text.splitlines() or [''])[0]


def read_raw(path):
    from spicelib import RawRead
    data = RawRead(str(path), verbose=False)
    if data.raw_params.get('Plotname') != 'AC Analysis' or 'complex' not in data.raw_params.get('Flags', ''):
        raise StageError('ltspice', f'RAW is not a complex AC plot: {data.raw_params.get("Plotname")}')
    if len(data.get_steps()) != 1:
        raise StageError('ltspice', 'stepped AC RAW is unsupported')
    names = data.get_trace_names()
    frequency = [value.real for value in data.get_trace(names[0]).get_wave().tolist()]
    return frequency, {name: data.get_trace(name).get_wave().tolist() for name in names[1:]}


# ---------------------------------------------------------------- stage 3 and trace mapping

def trace_key(name):
    """Normalise an LTspice or Gecko trace name: case-insensitive, `R§Load` style prefixes dropped."""
    kind, _, inner = name.partition('(')
    return f'{kind.casefold()}({":".join(part.split("§")[-1] for part in inner.rstrip(")").split(":")).casefold()})'


def is_internal(name):
    """LTspice-only subcircuit internals: any name with ':' except a port current Ix(xN:pin) of a root occurrence."""
    key = trace_key(name)
    return ':' in key and not re.fullmatch(r'ix\([^:]+:[^:]+\)', key)


def root_pins(netlist):
    """Root-level device pins of an LTspice netlist: {instance: [node per SPICE order]}."""
    pins, depth = {}, 0
    for line in netlist.splitlines():
        fields = line.split()
        if not fields or fields[0][0] in '*+;':
            continue
        label = fields[0].casefold()
        depth += label == '.subckt'
        depth -= label == '.ends'
        if depth == 0 and not label.startswith('.'):
            pins[label.split('§')[-1]] = [field.casefold() for field in fields[1:]]
    return pins


def gecko_traces(result, netlist):
    """Name Gecko AC observations with LTspice trace keys through the shared root-level device pins."""
    pins = root_pins(netlist)
    wave = lambda pairs: [complex(re, im) for re, im in pairs]
    traces, conflicts = {}, []
    for group in result['groups']:
        nodes = set()
        for member in group['physicalMembers']:
            if member['occurrencePath'] or member['kind'] not in ('PRIMITIVE', 'PARENT_PIN'):
                continue
            row = pins.get(member['instance'].casefold(), [])
            if len(row) >= member['spiceOrder']:
                nodes.add(row[member['spiceOrder'] - 1])
        if len(nodes) > 1:
            conflicts.append(sorted(nodes))
        elif nodes and not group['ground'] and nodes != {'0'}:
            traces[f'v({nodes.pop()})'] = wave(group['voltageV'])
    for current in result['currents']:
        traces.setdefault(trace_key(f'I({current["instance"]})'), wave(current['currentA']))
    for port in (result.get('hierarchy') or {}).get('ports', []):
        if not port['external']['occurrencePath']:
            key = trace_key(f'Ix({port["external"]["instance"]}:{port["internal"]["instance"]})')
            traces[key] = wave(port['currentIntoChildA'])
    if conflicts:
        raise StageError('compare', f'Gecko node maps to several LTspice nodes: {conflicts[0]}')
    return traces


# ---------------------------------------------------------------- stage 4: score

def unit(name):
    return 'V' if name.casefold().startswith('v(') else 'A'


def ac_inputs(frequency, signals, values):
    """Build the ac_compare manifest and one CSV in its signed real/imag layout."""
    manifest = dict(analysis='.ac', frequencies_hz=frequency,
                    signals=[dict(name=name, unit=unit(name)) for name in signals])
    stream = io.StringIO()
    writer = csv.writer(stream, lineterminator='\n')
    writer.writerow(['frequency'] + [f'{name} [{unit(name)}] {part}' for name in signals for part in ('real', 'imag')])
    for index, f in enumerate(frequency):
        writer.writerow([repr(f)] + [repr(getattr(values[name][index], part))
                                     for name in signals for part in ('real', 'imag')])
    return json.dumps(manifest).encode(), stream.getvalue().encode()


def score(frequency, reference, gecko_frequency, candidate):
    """Compare every reference trace that Gecko also produced.

    Returns (score fields, files to keep, failure message or None). A reference trace that Gecko lacks
    fails the case; LTspice-only subcircuit internals are listed and not compared."""
    if len(gecko_frequency) != len(frequency) or any(
            abs(g - r) > FREQUENCY_RTOL * r for g, r in zip(gecko_frequency, frequency)):
        raise StageError('compare', f'frequency grid differs: {len(gecko_frequency)} Gecko vs {len(frequency)} LTspice points')
    keyed = {trace_key(name): name for name in reference}
    internal = sorted(name for name in reference if is_internal(name))
    missing = sorted(name for name in reference if not is_internal(name) and trace_key(name) not in candidate)
    shared = sorted(name for name in reference if not is_internal(name) and trace_key(name) in candidate)
    row = dict(trace_count=len(shared), point_count=len(frequency), missing_in_gecko=missing,
               ltspice_only_internal=internal, gecko_only=sorted(set(candidate) - set(keyed)),
               worst_trace=None, worst_ratio=None, failing_traces=[])
    files = {}
    if shared:
        manifest, ref_csv = ac_inputs(frequency, shared, reference)
        _, cand_csv = ac_inputs(frequency, shared, {name: candidate[trace_key(name)] for name in shared})
        files = {'manifest.json': manifest, 'reference.csv': ref_csv, 'candidate.csv': cand_csv}
        metrics = compare_case_inputs(lambda name: files[dict(manifest='manifest.json', reference='reference.csv',
                                                              candidate='candidate.csv')[name]], {})
        files['compare.json'] = json.dumps(metrics, indent=2, allow_nan=False).encode()
        worst = max(metrics['signals'], key=lambda name: metrics['signals'][name]['worst']['limit_fraction'])
        row.update(worst_trace=worst, worst_ratio=metrics['signals'][worst]['worst']['limit_fraction'],
                   worst_frequency_hz=metrics['signals'][worst]['worst']['frequency_hz'],
                   failing_traces=sorted(name for name, item in metrics['signals'].items() if not item['passed']))
    failure = None
    if missing:
        failure = f'Gecko lacks {len(missing)} LTspice traces: {", ".join(missing[:5])}'
    elif not shared:
        failure = 'no trace present in both outputs'
    elif row['failing_traces']:
        failure = (f'{len(row["failing_traces"])} of {len(shared)} traces exceed 3%: '
                   f'worst {row["worst_trace"]} at {row["worst_ratio"]:.4g} x limit')
    return row, files, failure


# ---------------------------------------------------------------- one case, one row

def run_case(ctx, case):
    started = time.monotonic()
    folder = ctx['out'] / 'cases' / case['id']
    folder.mkdir(parents=True)
    source = ctx['author'] / case['archive_path']
    row = dict(id=case['id'], archive_path=case['archive_path'], analysis=case['directive'],
               source_sha256=digest(source), commit=ctx['commit'], tool_versions=dict(ctx['versions']),
               raw_sha256={}, trace_count=0, point_count=0, worst_trace=None, worst_ratio=None,
               verdict='failed', stage=None, message=None, device_models=[])
    try:
        if row['source_sha256'] != case['sha256']:
            raise StageError('import', 'source hash differs from the archive inventory')
        model = folder / 'model.ipes'
        row['device_models'] = import_case(ctx, source, model)
        row['raw_sha256']['ipes'] = digest(model)

        raw, net, version = run_ltspice(ctx, source, folder / 'ltspice')
        row['tool_versions']['ltspice'] = version
        row['raw_sha256']['ltspice_raw'] = digest(raw)
        frequency, reference = read_raw(raw)

        result = execute('gecko', ['java', '-Djava.awt.headless=true', f'-Djava.io.tmpdir={ctx["tmp"]}',
                                   '-cp', str(ctx['jar']), 'gecko.core.ac.AcMain', str(model)], ctx['timeout'])
        if result.returncode:
            raise StageError('gecko', (result.stderr.strip().splitlines() or [f'exit {result.returncode}'])[0])
        (folder / 'gecko-ac.json').write_text(result.stdout)
        row['raw_sha256']['gecko_json'] = digest(folder / 'gecko-ac.json')
        gecko = json.loads(result.stdout)
        row['tool_versions']['java'] = gecko['metadata'].get('javaVersion')
        row['gecko_revision'] = gecko['metadata'].get('executionRevision')

        candidate = gecko_traces(gecko, net.read_text(encoding='utf-8', errors='replace'))
        scored, files, failure = score(frequency, reference, gecko['frequencyHz'], candidate)
        row.update(scored)
        for name, data in files.items():
            (folder / name).write_bytes(data)
        if failure:
            raise StageError('compare', failure)
        row['verdict'] = 'accepted'
    except StageError as error:
        row.update(stage=error.stage, message=str(error))
    except Exception as error:  # A defect in one case's data must not stop the batch.
        row.update(stage='harness', message=f'{type(error).__name__}: {error}')
    row['elapsed_seconds'] = round(time.monotonic() - started, 3)
    (folder / 'row.json').write_text(json.dumps(row, indent=2, allow_nan=False))
    return row


# ---------------------------------------------------------------- summary

def cause(message):
    """Normalise a failure message so cases with the same cause group together."""
    text = re.sub(r'\S*[\\/][^"\n]*?\.(?:asc|asy|lib|sub|inc|net|ipes|raw|cir)\b', '<file>', message or '', flags=re.I)
    text = re.sub(r'\b[VI]x?\([^)]*\)', '<trace>', text, flags=re.I)
    text = re.sub(r'<trace>(?:, <trace>)+', '<trace>', text)
    text = re.sub(r'\[[^\]]*\]', '[]', text)
    return re.sub(r'(?<![A-Za-z])[-+]?\d+(?:\.\d+)?(?:e[-+]?\d+)?', '#', text, flags=re.I).strip()


def summarize(rows, stopped=None):
    accepted = [row['id'] for row in rows if row['verdict'] == 'accepted']
    groups = defaultdict(list)
    for row in rows:
        if row['verdict'] != 'accepted':
            groups[(row['stage'], cause(row['message']))].append(row['id'])
    ordered = sorted(groups.items(), key=lambda item: (-len(item[1]), item[0]))
    return dict(cases=len(rows), accepted=accepted, stopped=stopped,
                failed_by_stage=dict(Counter(row['stage'] for row in rows if row['verdict'] != 'accepted')),
                failure_groups=[dict(stage=stage, cause=text, count=len(ids), ids=ids)
                                for (stage, text), ids in ordered])


def markdown(summary, rows):
    lines = [f'# M6 batch acceptance', '', f'Cases: {summary["cases"]}. Accepted: {len(summary["accepted"])}.', '']
    if summary['stopped']:
        lines += [f'**Stopped early:** {summary["stopped"]}', '']
    lines += ['Accepted ids: ' + (', '.join(summary['accepted']) or 'none'), '',
              '| Stage | Cause | Cases | Ids |', '|---|---|---:|---|']
    lines += [f'| {g["stage"]} | {g["cause"].replace("|", "/")} | {g["count"]} | {", ".join(g["ids"])} |'
              for g in summary['failure_groups']]
    lines += ['', '| Id | Verdict | Stage | Traces | Points | Worst trace | Worst ratio | Message |',
              '|---|---|---|---:|---:|---|---:|---|']
    for row in rows:
        ratio = '' if row['worst_ratio'] is None else f'{row["worst_ratio"]:.3g}'
        lines.append(f'| {row["id"]} | {row["verdict"]} | {row["stage"] or ""} | {row["trace_count"]} | '
                     f'{row["point_count"]} | {row["worst_trace"] or ""} | {ratio} | '
                     f'{(row["message"] or "").replace("|", "/")[:160]} |')
    return '\n'.join(lines) + '\n'


# ---------------------------------------------------------------- batch

def select(inventory, analysis, ids):
    prefix = '.' + analysis
    wanted = None if ids == 'all' else {int(value) for value in ids.split(',')}
    cases = []
    for item in inventory:
        directives = [d for d in item['original_analyses'] if d.split()[0].casefold() == prefix]
        if directives and (wanted is None or int(item['id']) in wanted):
            cases.append(dict(item, directive=directives[0]))
    return cases


def prepare(args):
    out = args.output_dir.resolve()
    if str(out).casefold().startswith('/mnt/c'):
        raise RuntimeError('Write outputs to D:, never C:')
    out.mkdir(parents=True)
    require_space(out)
    author, harness, tmp = out / 'author', out / 'harness', out / 'tmp'
    with zipfile.ZipFile(args.archive) as archive:
        archive.extractall(author)
    roaming, local, temp = (out / 'windows' / name for name in ('roaming', 'local', 'temp'))
    for folder in (harness, tmp, roaming, local, temp):
        folder.mkdir(parents=True)
    subprocess.run(['javac', '-d', str(harness), '-cp', str(args.jar), str(Path(__file__).with_name('ImportAndParse.java'))],
                   check=True)
    stock = args.ltspice_stock
    search = lambda *last: ';'.join(windows(p) for p in (author, author / 'Tof' / 'Subcircuits', *last))
    now = int(time.time())
    # LTspice reads %APPDATA%\LTspice.ini. `-ini` makes 26.1.1 open the ini as a document and never exit.
    (roaming / 'LTspice.ini').write_text(
        '[Options]\r\nLastRunVersion=26.1.1\r\nHighestRunVersion=26.1.1\r\nCaptureAnalytics=false\r\n'
        f'LastWebUpdateTime={now}\r\nLastWebUpdateQuery={now}\r\n'
        f'SymbolSearchPath={search(stock / "sym")}\r\nLibrarySearchPath={search(stock / "sub")}\r\n'
        'AutoDeleteRawFiles=0\r\nUseRawTempDir=false\r\n')
    env = dict(os.environ, TEMP=str(temp), TMP=str(temp), APPDATA=str(roaming), LOCALAPPDATA=str(local))
    env['WSLENV'] = ':'.join(filter(None, [os.environ.get('WSLENV'), 'TEMP/p:TMP/p:APPDATA/p:LOCALAPPDATA/p']))
    git = lambda *command: subprocess.run(['git', '-C', str(REPO), *command], capture_output=True, text=True).stdout.strip()
    commit = git('rev-parse', 'HEAD') + ('-dirty' if git('status', '--porcelain', '--untracked-files=no') else '')
    from importlib.metadata import version
    versions = dict(python=platform.python_version(), spicelib=version('spicelib'),
                    gecko_core_jar_sha256=digest(args.jar), ltspice_exe_sha256=digest(args.ltspice))
    return dict(out=out, author=author, harness=harness, tmp=tmp, jar=args.jar.resolve(), stock=stock,
                ltspice=args.ltspice, timeout=args.timeout, windows_env=env, commit=commit, versions=versions)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--analysis', choices=['ac'], required=True)
    parser.add_argument('--ids', required=True, help='"all" or comma-separated archive ids, e.g. 106,113')
    parser.add_argument('--output-dir', type=Path, required=True, help='new folder on D:')
    parser.add_argument('--timeout', type=int, default=300, help='seconds per stage')
    parser.add_argument('--inventory', type=Path,
                        default=EVIDENCE / 'archive-import-batch-01' / 'combined-classification.json')
    parser.add_argument('--archive', type=Path,
                        default=Path.home() / 'claude_wsl/gecko-verification/m6-evidence-20261124/LTspice_SMPS.zip')
    parser.add_argument('--jar', type=Path,
                        default=REPO / 'src/modules/gecko-simulation-core/target/gecko-simulation-core-1.0-SNAPSHOT.jar')
    parser.add_argument('--ltspice', type=Path, default=EVIDENCE / 'adi-ltspice-26_1_1' / 'portable' / 'LTspice.exe')
    parser.add_argument('--ltspice-stock', type=Path, default=EVIDENCE / 'adi-ltspice-26_1_1' / 'stock' / 'lib')
    args = parser.parse_args(argv)
    cases = select(json.loads(args.inventory.read_text(encoding='utf-8')), args.analysis, args.ids)
    if not cases:
        parser.error('no matching case')
    ctx = prepare(args)
    rows, stopped = [], None
    with (ctx['out'] / 'results.jsonl').open('a', encoding='utf-8') as results:
        for number, case in enumerate(cases, 1):
            try:
                require_space(ctx['out'])
            except RuntimeError as error:
                stopped = str(error)
                break
            row = run_case(ctx, case)
            rows.append(row)
            results.write(json.dumps(row, allow_nan=False) + '\n')
            results.flush()
            print(f'[{number}/{len(cases)}] {row["id"]} {row["verdict"]} {row["stage"] or ""} '
                  f'{row["message"] or ""}'[:200], flush=True)
    summary = summarize(rows, stopped)
    (ctx['out'] / 'summary.json').write_text(json.dumps(summary, indent=2))
    (ctx['out'] / 'summary.md').write_text(markdown(summary, rows))
    print(json.dumps({key: summary[key] for key in ('cases', 'accepted', 'failed_by_stage', 'stopped')}))
    return 0 if len(summary['accepted']) == len(cases) else 1


if __name__ == '__main__':
    sys.exit(main())
