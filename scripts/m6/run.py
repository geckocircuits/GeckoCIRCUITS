"""Run the three original import audit modes sequentially and print one scoreboard."""

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys

from batch import BUDGET, CORE, DESKTOP, EXPECTED, JAVA, digest, read, write
from storage_guard import CASE_RESERVE, directory_bytes, require_free_space


def invoke(arguments, env=None, log=None):
    require_free_space(CASE_RESERVE)
    subprocess.run(arguments, env=env, check=True, stdout=log, stderr=log)


def validate_destinations(output, qualification):
    output, qualification = output.resolve(), qualification.resolve()
    report = output.with_name(output.name + '-scoreboard')
    temp_root = (Path(r'D:\codex-temp\gecko-m6') / output.name).resolve()
    if output.exists() or report.exists() or temp_root.exists():
        raise ValueError('Run, report and temporary destinations must all be new folders')
    if output.is_relative_to(qualification):
        raise ValueError('Run output must stay outside the sealed qualification audit')
    if output.drive.upper() != 'D:' or temp_root.drive.upper() != 'D:':
        raise ValueError('Run outputs and temporary files must be on D: to avoid occupying C:')
    return output, report, temp_root


def completion_storage(output, report, temp_root):
    total = sum(directory_bytes(path) for path in (output, report, temp_root))
    if total > BUDGET:
        raise RuntimeError(f'Whole-run storage guard: evidence, report and temporary files use {total} bytes; limit {BUDGET}')
    return total


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-dir', required=True, type=Path)
    parser.add_argument('--qualification-audit', required=True, type=Path)
    parser.add_argument('--qualification-inventory-sha256', required=True)
    parser.add_argument('--runtime-manifest', type=Path)
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args()
    from scoreboard import verify_inventory
    require_free_space(CASE_RESERVE)
    verify_inventory(args.qualification_audit, args.qualification_inventory_sha256)
    output, report, temp_root = validate_destinations(args.output_dir, args.qualification_audit)
    scripts = Path(__file__).resolve().parent
    harness = output / 'harness'
    runtime = read(args.runtime_manifest) if args.runtime_manifest else {
        name: {'path': str(path), 'sha256': digest(path)}
        for name, path in (('java', JAVA), ('core', CORE), ('desktop', DESKTOP))}
    if not args.runtime_manifest:
        for path, sha in EXPECTED.items():
            if digest(path) != sha:
                raise ValueError(f'Pinned baseline differs: {path}')
    for item in runtime.values():
        if isinstance(item, dict) and 'path' in item and digest(Path(item['path'])) != item['sha256']:
            raise ValueError('Runtime manifest hash mismatch')
    if args.dry_run:
        print(json.dumps({'execution': 'one JVM at a time', 'output': str(output),
                          'modes': {'selected_profile': 467, 'unselected': 467, 'legacy-non-ac': 345},
                          'runtime': runtime, 'writes_to_C': False,
                          'simulations': 0, 'dry_run': True}, indent=2))
        return
    output.mkdir(parents=True)
    harness.mkdir()
    runtime_file = output / 'runtime.json'
    write(runtime_file, runtime)
    javac = Path(runtime['java']['path']).with_name('javac.exe')
    compiler_temp = temp_root / 'compiler'
    compiler_temp.mkdir(parents=True)
    classpath = os.pathsep.join(runtime[name]['path'] for name in ('core', 'desktop'))
    with (output / 'harness-compile.log').open('x', encoding='utf-8') as log:
        invoke([str(javac), f'-J-Djava.io.tmpdir={compiler_temp}', '-encoding', 'UTF-8', '-cp', classpath, '-d', str(harness),
                str(scripts / 'ImportAndParse.java')], log=log)
    for mode in ('selected_profile', 'unselected', 'legacy-non-ac'):
        folder = output if mode == 'selected_profile' else output / mode
        folder.mkdir(exist_ok=mode == 'selected_profile')
        env = dict(os.environ, GECKO_M6_RUN_DIR=str(folder), GECKO_M6_MODE=mode,
                   GECKO_M6_RUN_ROOT=str(output), GECKO_M6_TEMP_ROOT=str(temp_root),
                   GECKO_M6_HARNESS_DIR=str(harness), GECKO_M6_RUNTIME_MANIFEST=str(runtime_file),
                   GECKO_M6_TEMP_DIR=str(temp_root / mode))
        for operation in ('--preview', '--execute'):
            with (output / f'{mode}-{operation[2:]}.log').open('x', encoding='utf-8') as log:
                print(f'{mode} {operation}', flush=True)
                invoke([sys.executable, '-B', str(scripts / 'batch.py'), operation], env=env, log=log)
    prior = read(args.qualification_audit / 'prior-qualification.json')
    write(output / 'prior-qualification.json', prior)
    env = dict(os.environ, GECKO_M6_AUDIT_DIR=str(output), GECKO_M6_RUN_DIR=str(output))
    with (output / 'combine.log').open('x', encoding='utf-8') as log:
        invoke([sys.executable, '-B', str(scripts / 'combine.py')], env=env, log=log)
    write(output / 'run-metadata.json', {
        'fresh_import_attempts': sum(len(read(folder / 'results.json'))
                                     for folder in (output, output / 'unselected', output / 'legacy-non-ac')),
        'fresh_simulations': 0, 'execution': 'sequential',
        'qualification_source': str(args.qualification_audit),
        'qualification_inventory_sha256': args.qualification_inventory_sha256})
    index = {path.relative_to(output).as_posix(): {'bytes': path.stat().st_size, 'sha256': digest(path)}
             for path in sorted(output.rglob('*')) if path.is_file()}
    write(output / 'artifact-sha256.json', index)
    inventory_sha = digest(output / 'artifact-sha256.json')
    invoke([sys.executable, '-B', str(scripts / 'scoreboard.py'), '--audit-dir', str(output),
            '--inventory-sha256', inventory_sha, '--output-dir', str(report)])
    print(json.dumps({'whole_run_bytes': completion_storage(output, report, temp_root),
                      'whole_run_budget_bytes': BUDGET}), flush=True)


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, ValueError, subprocess.CalledProcessError) as failure:
        print(str(failure), file=sys.stderr)
        sys.exit(1)
