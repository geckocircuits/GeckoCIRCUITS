import contextlib
from hashlib import sha256
import io
import json
import math
from unittest import TestCase
from unittest.mock import patch

from transient_compare import compare, main


class TransientComparatorTest(TestCase):
    def check(self, reference, candidate, **overrides):
        policy = dict(start=0, stop=10, ts=1, atol=0)
        policy.update(overrides)
        return compare(reference, candidate, **policy)

    def test_identical_linear_signals_on_adaptive_grids(self):
        result = self.check([(-1, -1), (1, 1), (11, 11)], [(0, 0), (3, 3), (10, 10)])
        self.assertTrue(result['passed'])
        self.assertEqual(result['scale_origin'], 'reference_peak')
        self.assertEqual(result['input_extents']['reference'], [-1, 11])
        self.assertEqual(result['union_knot_count'], 4)
        self.assertEqual([row['evaluated'] for row in result['sliding']], [5, 1])

    def test_piecewise_linear_integrals_are_time_weighted(self):
        result = self.check([(0, 0), (10, 0)], [(0, 0), (1, 1), (2, 0), (10, 0)])
        self.assertFalse(result['passed'])
        self.assertEqual(result['peak']['error'], 1)
        self.assertAlmostEqual(result['full']['average']['error'], .1)
        self.assertAlmostEqual(result['full']['waveform_rmse']['error'], math.sqrt(1 / 15))
        self.assertAlmostEqual(result['sliding'][0]['worst']['rms_value']['error'], math.sqrt(1 / 3))
        self.assertEqual(result['sliding'][0]['worst']['average']['error'], .5)

    def test_dc_bias_can_fail_only_average(self):
        result = self.check([(0, 100), (10, 100)], [(0, 101.5), (10, 101.5)])
        self.assertFalse(result['passed'])
        self.assertTrue(result['peak']['passed'])
        for row in [result['full'], *(item['worst'] for item in result['sliding'])]:
            self.assertFalse(row['average']['passed'])
            self.assertTrue(row['waveform_rmse']['passed'])
        self.assertTrue(result['sliding'][0]['worst']['rms_value']['passed'])

    def test_signed_waveform_and_phase_are_preserved(self):
        ref = [(0, 1), (2, -1), (4, 1), (6, -1), (8, 1), (10, -1)]
        result = self.check(ref, [(t, -v) for t, v in ref], scale=1000)
        self.assertTrue(result['peak']['passed'])
        self.assertTrue(result['full']['average']['passed'])
        self.assertFalse(result['full']['waveform_rmse']['passed'])
        self.assertTrue(result['sliding'][0]['worst']['rms_value']['passed'])

    def test_peak_scale_and_absolute_floor_are_explicit(self):
        ref = [(0, 0), (10, 0)]
        cand = [(0, .01), (10, .01)]
        self.assertFalse(self.check(ref, cand, scale=0)['passed'])
        self.assertTrue(self.check(ref, cand, scale=0, atol=.01)['passed'])

    def test_large_startup_cannot_erase_quiet_tail_error(self):
        result = self.check([(0, 1e100), (1, 1), (10, 1)], [(0, 1e100), (1, 1), (10, 1.04)])
        last = result['sliding'][0]['worst']['waveform_rmse']
        self.assertFalse(last['passed'])
        self.assertEqual(last['stop'], 10)
        self.assertAlmostEqual(last['limit'], .03)
        self.assertEqual(last['reference_scale'], 1)
        self.assertEqual(last['relative_fraction'], .03)
        self.assertGreater(last['error'], .03)

    def test_missing_samples_or_coverage_fail_closed(self):
        ref = [(0, 1), (10, 1)]
        for invalid in ([(1, 1), (10, 1)], [(0, 1), (9.99, 1)], [(0, 1), (0, 1), (10, 1)],
                        [(0, 1), (6, 1), (5, 1), (10, 1)], [(0, 1)], [(0, 1), (10, math.inf)],
                        [(0, 1), (math.nan, 1), (10, 1)]):
            with self.subTest(invalid=invalid), self.assertRaises(ValueError):
                self.check(ref, invalid)
        with self.assertRaisesRegex(ValueError, 'both Ts and 10 Ts'):
            self.check(ref, ref, stop=9.99)

    def test_shifted_knot_and_fixed_tail_starts_are_evaluated(self):
        ref = [(0, 1), (10, 1)]
        result = self.check(ref, [(0, 1), (9.25, 1), (9.75, 2), (10, 1)])
        self.assertEqual(result['sliding'][0]['evaluated'], 4)
        self.assertEqual(result['sliding'][0]['worst']['average']['start'], 9)

    def test_literal_time_shift_does_not_align_traces(self):
        reference = [(0, 0), (2, 1), (3, 0), (10, 0)]
        shifted = [(0, 0), (3, 1), (4, 0), (10, 0)]
        result = self.check(reference, shifted)
        self.assertEqual(result['peak']['error'], 1)
        self.assertFalse(result['full']['waveform_rmse']['passed'])

    def run_cli(self, reference, candidate, policy=None):
        if policy is None:
            policy = b'{"start":0,"stop":10,"ts":1,"signals":[{"name":"V(out)","unit":"V","atol":0},{"name":"I(V1)","unit":"A","atol":0}]}'
        args = ['ref.csv', 'cand.csv', '--manifest', 'manifest.json']
        output = io.StringIO()
        files = [io.BytesIO(data) for data in (policy, reference, candidate)]
        with patch('builtins.open', side_effect=files), contextlib.redirect_stdout(output):
            status = main(args)
        return status, json.loads(output.getvalue())

    def test_cli_matches_declared_signals_by_name_and_unit(self):
        ref = b'time,V(out) [V],I(V1) [A]\n-1,1,-2\n10,1,-2\n'
        for value, status in ((-2, 0), (2, 1)):
            cand = f'time,I(V1) [A],V(out) [V]\n0,{value},1\n10,{value},1\n'.encode()
            actual, output = self.run_cli(ref, cand)
            self.assertEqual(actual, status)
            self.assertEqual(output['policy'], 'transient-windowed-v1')
            self.assertEqual(output['scope'], 'transient_metrics_only')
            self.assertEqual(output['required_interval'], [0, 10])
            self.assertTrue(output['startup_observed'])
            self.assertEqual(output['startup_samples'], {'reference': False, 'candidate': True})
            self.assertEqual(output['signals']['I(V1)']['unit'], 'A')
            self.assertEqual(output['inputs']['candidate']['sha256'], sha256(cand).hexdigest())

    def test_cli_rejects_missing_duplicate_extra_or_wrong_unit_signals(self):
        ref = b'time,V(out) [V],I(V1) [A]\n0,1,-2\n10,1,-2\n'
        for candidate in (b'time,V(out) [A],I(V1) [V]\n0,1,-2\n10,1,-2\n',
                          b'time,V(out) [V],V(out) [V]\n0,1,-2\n10,1,-2\n',
                          b'time,V(out) [V]\n0,1\n10,1\n',
                          b'time,V(out) [V],I(V1) [A],V(in) [V]\n0,1,-2,3\n10,1,-2,3\n',
                          b'time,V(out) [V],I(V1) [A]\n0,1\n10,1,-2\n',
                          b'time,V(out) [V],I(V1) [A]\n1,1,-2\n10,1,-2\n'):
            with self.subTest(candidate=candidate), contextlib.redirect_stderr(io.StringIO()):
                with self.assertRaises(SystemExit) as error:
                    self.run_cli(ref, candidate)
                self.assertEqual(error.exception.code, 2)

    def test_cli_rejects_late_policy_start_and_duplicate_manifest_keys(self):
        ref = b'time,V(out) [V]\n0,1\n10,1\n'
        for policy in (b'{"start":1,"stop":10,"ts":0.5,"signals":[{"name":"V(out)","unit":"V","atol":0}]}',
                       b'{"start":0,"start":0,"stop":10,"ts":1,"signals":[{"name":"V(out)","unit":"V","atol":0}]}'):
            with self.subTest(policy=policy), contextlib.redirect_stderr(io.StringIO()):
                with self.assertRaises(SystemExit) as error:
                    self.run_cli(ref, ref, policy)
                self.assertEqual(error.exception.code, 2)
