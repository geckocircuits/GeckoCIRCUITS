"""Compare one signed transient signal on a predeclared physical time interval."""

import argparse
from bisect import bisect_right
import csv
from hashlib import sha256
from heapq import merge
import io
from itertools import groupby
import json
import math


def _trace(rows):
    times, values = [], []
    for time, value in rows:
        if not math.isfinite(time) or not math.isfinite(value):
            raise ValueError('Use finite times and values')
        if times and time <= times[-1]:
            raise ValueError('Use strictly increasing times')
        times.append(time)
        values.append(value)
    if len(times) < 2:
        raise ValueError('Supply at least two samples per trace')
    return times, values


def _interpolate(left, right, fraction):
    # A weighted sum avoids overflow in right - left for opposite signs.
    if left == right:
        return left
    return (1 - fraction) * left + fraction * right


def _sample(trace, point):
    times, values = trace
    index = bisect_right(times, point) - 1
    if times[index] == point:
        return values[index]
    fraction = (point - times[index]) / _finite(times[index + 1] - times[index])
    return _interpolate(values[index], values[index + 1], fraction)


def _finite(number):
    if not math.isfinite(number):
        raise ValueError('Signal or time exceeds the numeric range')
    return number


def _square_integral(left, right, width):
    # Midpoint and slope contributions integrate a linear waveform squared.
    middle = left * 0.5 + right * 0.5
    half_change = right * 0.5 - left * 0.5
    result = _finite((middle * middle + half_change * half_change / 3) * width)
    if result == 0 and (left or right):
        raise ValueError('Signal or time falls below the numeric range')
    return result


def _moments(width, left, right):
    ref0, cand0, err0 = left
    ref1, cand1, err1 = right
    return (
        _finite((err0 * 0.5 + err1 * 0.5) * width),
        _square_integral(ref0, ref1, width),
        _square_integral(cand0, cand1, width),
        _square_integral(err0, err1, width),
    )


class _Integrals:
    """Use range sums so a large startup cannot erase quiet tail energy."""

    def __init__(self, grid, values):
        self.grid = grid
        self.values = values
        self.size = len(grid) - 1
        self.tree = [(0.0,) * 4] * self.size
        self.tree.extend(_moments(b - a, x, y)
                         for a, b, x, y in zip(grid, grid[1:], values, values[1:]))
        for index in range(self.size - 1, 0, -1):
            self.tree[index] = tuple(_finite(a + b) for a, b in
                                     zip(self.tree[2 * index], self.tree[2 * index + 1]))

    def _partial(self, index, begin, end):
        t0, t1 = self.grid[index:index + 2]
        left, right = self.values[index:index + 2]
        at = lambda t: tuple(_interpolate(a, b, (t - t0) / (t1 - t0))
                             for a, b in zip(left, right))
        return _moments(end - begin, at(begin), at(end))

    def between(self, begin, end):
        first = bisect_right(self.grid, begin) - 1
        last = bisect_right(self.grid, end) - 1
        if first == last:
            return self._partial(first, begin, end)
        terms = [self._partial(first, begin, self.grid[first + 1])]
        if end > self.grid[last]:
            terms.append(self._partial(last, self.grid[last], end))
        left, right = self.size + first + 1, self.size + last
        while left < right:
            if left % 2:
                terms.append(self.tree[left])
                left += 1
            if right % 2:
                right -= 1
                terms.append(self.tree[right])
            left //= 2
            right //= 2
        return tuple(_finite(math.fsum(parts)) for parts in zip(*terms))


def _metric(error, scale, fraction, atol, begin, end):
    limit = max(atol, fraction * scale)
    return dict(error=error, limit=limit, reference_scale=scale, relative_fraction=fraction,
                start=begin, stop=end, passed=error <= limit)


def _rank(metric):
    error, limit = metric['error'], metric['limit']
    if not limit:
        return (not metric['passed'], math.inf if error else 0, error)
    return not metric['passed'], error / limit, error


def _window_metrics(integrals, begin, end, atol, full=False):
    duration = end - begin
    mean_error, reference_energy, candidate_energy, error_energy = integrals.between(begin, end)
    # Divide square roots to avoid overflowing the intermediate mean square.
    root_duration = math.sqrt(duration)
    reference_rms = _finite(math.sqrt(reference_energy) / root_duration)
    candidate_rms = _finite(math.sqrt(candidate_energy) / root_duration)
    metrics = {
        'average': _metric(abs(mean_error / duration), reference_rms, .01, atol, begin, end),
        'waveform_rmse': _metric(_finite(math.sqrt(error_energy) / root_duration),
                                 reference_rms, .02 if full else .03, atol, begin, end),
    }
    if not full:
        metrics['rms_value'] = _metric(abs(candidate_rms - reference_rms),
                                       reference_rms, .03, atol, begin, end)
    return metrics


def compare(reference, candidate, *, start, stop, ts, atol, scale=None, policy='transient-windowed-v1'):
    """Return metric evidence for iterables of (time in seconds, signed value)."""
    if not all(math.isfinite(x) for x in (start, stop, ts, atol)) or not start < stop or ts <= 0 or atol < 0:
        raise ValueError('Specify finite start < stop, Ts > 0, and atol >= 0')
    long_window = _finite(10 * ts)
    span = _finite(stop - start)
    if policy not in ('transient-windowed-v1', 'transient-windowed-v2'):
        raise ValueError('Use a supported transient policy')
    if policy == 'transient-windowed-v1' and span < long_window:
        raise ValueError('Supply coverage for both Ts and 10 Ts windows')
    if policy == 'transient-windowed-v2':
        if start < 0:
            raise ValueError('Use a nonnegative recording start time')
        if span < ts:
            raise ValueError('Supply at least one complete Ts window')
    if scale is not None and (not math.isfinite(scale) or scale < 0):
        raise ValueError('Specify a finite, nonnegative signal scale')
    reference, candidate = _trace(reference), _trace(candidate)
    for name, (times, _) in (('reference', reference), ('candidate', candidate)):
        if times[0] > start or times[-1] < stop:
            raise ValueError(f'Supply full declared coverage in the {name} trace')
    grid = [start]
    grid.extend(point for point, _ in groupby(merge(reference[0], candidate[0])) if start < point < stop)
    grid.append(stop)
    values = []
    for point in grid:
        ref, cand = _sample(reference, point), _sample(candidate, point)
        values.append((ref, cand, _finite(cand - ref)))
    reference_peak = max(abs(ref) for ref, _, _ in values)
    signal_scale = reference_peak if scale is None else scale
    peak_index = max(range(len(grid)), key=lambda i: abs(values[i][2]))
    peak = _metric(abs(values[peak_index][2]), signal_scale, .10, atol, grid[peak_index], grid[peak_index])
    integrals = _Integrals(grid, values)
    full = _window_metrics(integrals, start, stop, atol, full=True)
    sliding = []
    for requested in (ts, long_window):
        width = min(requested, span) if policy == 'transient-windowed-v2' else requested
        tail = start if policy == 'transient-windowed-v2' and width == span else stop - width
        if tail < start:
            raise ValueError('Window bounds exceed numeric precision')
        starts = {start, tail}
        starts.update(point for point in grid if start <= point <= tail)
        starts.update(point - width for point in grid if start <= point - width <= tail)
        worst = {}
        for begin in sorted(starts):
            end = stop if begin == tail else min(stop, begin + width)
            if end <= begin:
                raise ValueError('Window duration falls below time precision')
            for name, metric in _window_metrics(integrals, begin, end, atol).items():
                if name not in worst or _rank(metric) > _rank(worst[name]):
                    worst[name] = metric
        row = dict(duration=width, evaluated=len(starts), worst=worst)
        if policy == 'transient-windowed-v2':
            row.update(requested_duration=requested, shortened=width < requested)
        sliding.append(row)
    passed = all(m['passed'] for m in [peak, *full.values(),
                                     *(m for row in sliding for m in row['worst'].values())])
    return dict(scope='transient_metrics_only', passed=passed, start=start, stop=stop, ts=ts,
                absolute_tolerance=atol, signal_scale=signal_scale,
                scale_origin='reference_peak' if scale is None else 'predeclared',
                input_extents={'reference': [reference[0][0], reference[0][-1]],
                               'candidate': [candidate[0][0], candidate[0][-1]]},
                union_knot_count=len(grid), peak=peak, full=full, sliding=sliding,
                window_protocol='union_knots_and_shifted_knots_plus_head_and_tail')


def _unique_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f'Duplicate manifest key {key}')
        result[key] = value
    return result


def _number(value):
    if type(value) not in (int, float):
        raise ValueError('Use numeric policy values')
    return _finite(float(value))


def _manifest(data, version):
    policy = json.loads(data, object_pairs_hook=_unique_keys)
    if not isinstance(policy, dict) or set(policy) != {'start', 'stop', 'ts', 'signals'}:
        raise ValueError('Specify start, stop, ts and signals in the manifest')
    start, stop, ts = (_number(policy[key]) for key in ('start', 'stop', 'ts'))
    if version == 'transient-windowed-v1' and start != 0:
        raise ValueError('Declare startup at time zero for a complete transient comparison')
    signals = policy['signals']
    if not isinstance(signals, list) or not signals:
        raise ValueError('Declare at least one required signal')
    names, headers = set(), set()
    for signal in signals:
        required = {'name', 'unit', 'atol'}
        if not isinstance(signal, dict) or not required <= signal.keys() <= required | {'scale'}:
            raise ValueError('Specify name, unit, atol and optional scale for each signal')
        name, unit = signal['name'], signal['unit']
        if not all(isinstance(x, str) and x and x == x.strip() and x.isprintable() for x in (name, unit)):
            raise ValueError('Use nonempty, single-line signal names and units')
        header = f'{name} [{unit}]'
        if name in names or header in headers:
            raise ValueError(f'Duplicate signal {name}')
        names.add(name)
        headers.add(header)
        signal['column'] = header
        signal['atol'] = _number(signal['atol'])
        if 'scale' in signal:
            signal['scale'] = _number(signal['scale'])
    return start, stop, ts, signals


def _case_trace(data, signals):
    rows = csv.reader(io.StringIO(data.decode('utf-8'), newline=''), strict=True)
    header = next(rows, [])
    required = {'time', *(signal['column'] for signal in signals)}
    if len(header) != len(required) or set(header) != required or header[0] != 'time':
        raise ValueError('Use time and exactly the declared signal columns with their units')
    columns = [header.index(signal['column']) for signal in signals]
    times = []
    values = [[] for _ in signals]
    for row in rows:
        if len(row) != len(header):
            raise ValueError('Supply one value for each declared column')
        times.append(float(row[0]))
        for series, index in zip(values, columns):
            series.append(float(row[index]))
    return times, values


def compare_case(reference_path, candidate_path, manifest_path, *, policy='transient-windowed-v1'):
    """Compare every required signal and bind the report to the input bytes."""
    paths = {'reference': reference_path, 'candidate': candidate_path, 'manifest': manifest_path}
    pins = {}

    def load(name):
        path = paths[name]
        with open(path, 'rb') as stream:
            data = stream.read()
        pins[name] = dict(path=str(path), bytes=len(data), sha256=sha256(data).hexdigest())
        return data

    return compare_case_inputs(load, pins, policy=policy)


def compare_case_inputs(load, pins, *, policy):
    """Compare the same input bytes that the caller records in pins."""
    start, stop, ts, signals = _manifest(load('manifest'), policy)
    ref_times, ref_values = _case_trace(load('reference'), signals)
    cand_times, cand_values = _case_trace(load('candidate'), signals)
    observed = {}
    for signal, ref, cand in zip(signals, ref_values, cand_values):
        metrics = compare(zip(ref_times, ref), zip(cand_times, cand), start=start, stop=stop, ts=ts,
                          atol=signal['atol'], scale=signal.get('scale'), policy=policy)
        observed[signal['name']] = dict(unit=signal['unit'], **metrics)
    if policy == 'transient-windowed-v2':
        coverage = {'reference': ref_times[0] <= 0 <= ref_times[-1],
                    'candidate': cand_times[0] <= 0 <= cand_times[-1]}
        return dict(policy=policy, scope='transient_metrics_only',
                    passed=all(metrics['passed'] for metrics in observed.values()),
                    required_interval=[start, stop], coverage_scope='whole_declared_recording',
                    startup_compared=start == 0, startup_available=coverage,
                    startup_observed=start == 0 and all(coverage.values()),
                    startup_samples={'reference': 0 in ref_times, 'candidate': 0 in cand_times},
                    inputs=pins, signals=observed)
    return dict(policy=policy, scope='transient_metrics_only',
                passed=all(metrics['passed'] for metrics in observed.values()),
                required_interval=[start, stop], startup_observed=True,
                startup_samples={'reference': start in ref_times, 'candidate': start in cand_times},
                inputs=pins, signals=observed)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('reference')
    parser.add_argument('candidate')
    parser.add_argument('--manifest', required=True)
    parser.add_argument('--policy', choices=('transient-windowed-v1', 'transient-windowed-v2'),
                        default='transient-windowed-v1')
    args = parser.parse_args(argv)
    try:
        result = compare_case(args.reference, args.candidate, args.manifest, policy=args.policy)
    except (OSError, ValueError, OverflowError, csv.Error) as error:
        parser.error(str(error))
    print(json.dumps(result, indent=2, allow_nan=False))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
