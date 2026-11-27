"""Score signed complex AC observations without granting circuit qualification."""
import argparse
import csv
from hashlib import sha256
import io
import json
import math

POLICY = 'ac-strict-v1'
FLOORS = {'V': 1e-9, 'A': 1e-12}


def _unique(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate JSON key: ' + key)
        result[key] = value
    return result


def _number(value):
    if isinstance(value, bool):
        raise ValueError('Use numeric values, not booleans')
    number = float(value)
    if not math.isfinite(number):
        raise ValueError('Use finite numeric values')
    return number


def _configuration(data):
    doc = json.loads(data, object_pairs_hook=_unique)
    if not isinstance(doc, dict) or doc.get('analysis') != '.ac':
        raise ValueError('Declare .ac analysis')
    if not isinstance(doc.get('frequencies_hz'), list) or not isinstance(doc.get('signals'), list):
        raise ValueError('Declare frequency and signal arrays')
    grid = [_number(value) for value in doc['frequencies_hz']]
    if not grid or grid[0] <= 0 or any(b <= a for a, b in zip(grid, grid[1:])):
        raise ValueError('Declare a positive, strictly increasing frequency grid')
    signals, names = [], set()
    for item in doc['signals']:
        if not isinstance(item, dict):
            raise ValueError('Declare each signal as an object')
        name, unit = item['name'], item['unit']
        if not isinstance(name, str) or not name or name in names or not isinstance(unit, str) or not unit:
            raise ValueError('Declare unique signal names and nonempty units')
        floor = _number(item.get('floor', FLOORS.get(unit)))
        if floor <= 0:
            raise ValueError('Declare a positive AC reference floor')
        names.add(name)
        signals.append((name, unit, floor))
    if not signals:
        raise ValueError('Declare all required signals')
    return grid, signals


def _trace(data, grid, signals):
    rows = csv.reader(io.StringIO(data.decode('utf-8-sig')))
    header = next(rows, [])
    expected = ['frequency']
    for name, unit, _ in signals:
        expected.extend([f'{name} [{unit}] real', f'{name} [{unit}] imag'])
    if len(header) != len(set(header)) or set(header) != set(expected):
        raise ValueError('CSV signal names, units, or real/imag columns differ')
    columns = [header.index(column) for column in expected]
    values = [[] for _ in signals]
    count, previous = 0, 0.0
    for row in rows:
        if len(row) != len(header) or count >= len(grid):
            raise ValueError('CSV row width or frequency count differs')
        ordered = [_number(row[index]) for index in columns]
        frequency = ordered[0]
        if frequency <= previous or frequency != grid[count]:
            raise ValueError('CSV frequency grid differs from the declared analysis')
        previous = frequency
        for index, signal in enumerate(values):
            signal.append(complex(ordered[1 + 2 * index], ordered[2 + 2 * index]))
        count += 1
    if count != len(grid):
        raise ValueError('Missing declared AC observations')
    return values


def compare_case_inputs(load, pins, *, policy=POLICY):
    """Consume exactly the immutable bytes supplied by a pinned batch loader."""
    if policy != POLICY:
        raise ValueError('Unsupported AC comparison policy')
    try:
        grid, signals = _configuration(load('manifest'))
        reference = _trace(load('reference'), grid, signals)
        candidate = _trace(load('candidate'), grid, signals)
    except (KeyError, TypeError) as error:
        raise ValueError('Malformed AC comparison input') from error
    observed = {}
    for (name, unit, floor), refs, candidates in zip(signals, reference, candidate):
        failures, worst = 0, None
        for frequency, ref, cand in zip(grid, refs, candidates):
            error = abs(cand - ref)
            limit = 0.03 * max(abs(ref), floor)
            if not math.isfinite(error) or not math.isfinite(limit) or limit <= 0:
                raise ValueError('AC calculation exceeds the numeric range')
            failures += error > limit
            fraction = error / limit
            if not math.isfinite(fraction):
                raise ValueError('AC normalized error exceeds the numeric range')
            if worst is None or fraction > worst['limit_fraction']:
                worst = dict(frequency_hz=frequency, absolute_error=error, limit=limit,
                             reference_scale=max(abs(ref), floor), limit_fraction=fraction)
        observed[name] = dict(unit=unit, reference_floor=floor, relative_limit=0.03,
                              observations=len(grid), failures=failures, passed=failures == 0, worst=worst)
    return dict(policy=POLICY, scope='ac_metrics_only',
                passed=all(item['passed'] for item in observed.values()), inputs=pins,
                frequencies=len(grid), comparisons=len(grid) * len(signals), signals=observed)


def compare_case(reference_path, candidate_path, manifest_path):
    paths = dict(reference=reference_path, candidate=candidate_path, manifest=manifest_path)
    pins = {}

    def load(name):
        with open(paths[name], 'rb') as stream:
            data = stream.read()
        pins[name] = dict(path=str(paths[name]), bytes=len(data), sha256=sha256(data).hexdigest())
        return data

    return compare_case_inputs(load, pins)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('reference')
    parser.add_argument('candidate')
    parser.add_argument('--manifest', required=True)
    args = parser.parse_args(argv)
    try:
        result = compare_case(args.reference, args.candidate, args.manifest)
    except (OSError, ValueError, OverflowError, csv.Error) as error:
        parser.error(str(error))
    print(json.dumps(result, indent=2, allow_nan=False))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
