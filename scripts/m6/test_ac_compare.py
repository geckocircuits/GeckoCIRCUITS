import csv
import io
import json
from unittest import TestCase, main

from ac_compare import compare_case_inputs


def payload(values, frequencies=(1, 10), header=None):
    stream = io.StringIO()
    writer = csv.writer(stream)
    writer.writerow(header or ['frequency', 'I(D1) [A] real', 'I(D1) [A] imag'])
    for freq, value in zip(frequencies, values):
        writer.writerow([freq, value.real, value.imag])
    return stream.getvalue().encode()


class AcComparatorTest(TestCase):
    def run_case(self, ref, cand, config=None):
        spec = config or {'analysis': '.ac', 'frequencies_hz': [1, 10],
                          'signals': [{'name': 'I(D1)', 'unit': 'A'}]}
        values = dict(manifest=json.dumps(spec).encode(), reference=ref, candidate=cand)
        return compare_case_inputs(values.__getitem__, {})

    def test_complex_phase_is_not_replaced_by_equal_magnitude(self):
        reference = payload([1 + 1j, 2 + 3j])
        result = self.run_case(reference, payload([-1 - 1j, -2 - 3j]))
        self.assertFalse(result['passed'])
        self.assertEqual(result['signals']['I(D1)']['failures'], 2)
        self.assertFalse(self.run_case(payload([1 + 0j] * 2), payload([1j] * 2))['passed'])

    def test_current_floor_keeps_the_existing_three_percent_rule(self):
        zero = payload([0j, 0j])
        self.assertTrue(self.run_case(zero, payload([2e-14 + 0j] * 2))['passed'])
        self.assertFalse(self.run_case(zero, payload([4e-14 + 0j] * 2))['passed'])

    def test_each_required_frequency_and_exact_signal_set_is_required(self):
        reference = payload([1j, 2j])
        bad = [payload([1j]), payload([1j, 2j], [1, 11]),
               payload([1j, 2j], header=['frequency', 'I(D1) [V] real', 'I(D1) [V] imag']),
               payload([1j, 2j], header=['frequency', 'I(D1) [A] real', 'I(D1) [A] real'])]
        for candidate in bad:
            with self.subTest(candidate=candidate), self.assertRaises(ValueError):
                self.run_case(reference, candidate)

    def test_rejects_reference_and_candidate_offsets_below_old_relative_tolerance(self):
        exact = payload([1j, 2j], [1.0, 10.0])
        lower = payload([1j, 2j], [0.9999999999995, 10])
        upper = payload([1j, 2j], [1.0000000000005, 10])
        self.assertTrue(self.run_case(exact, exact)['passed'])
        for reference, candidate in ((lower, exact), (exact, upper), (lower, upper)):
            with self.subTest(reference=reference, candidate=candidate), self.assertRaisesRegex(
                    ValueError, 'frequency grid differs'):
                self.run_case(reference, candidate)

    def test_nonfinite_values_and_frequency_reordering_are_rejected(self):
        reference = payload([1j, 2j])
        for candidate in [payload([complex(float('nan'), 0), 2j]), payload([1j, 2j], [10, 1])]:
            with self.assertRaises(ValueError):
                self.run_case(reference, candidate)

    def test_input_loader_and_policy_are_explicit(self):
        values = dict(manifest=json.dumps({'analysis': '.ac', 'frequencies_hz': [1, 10],
                                          'signals': [{'name': 'I(D1)', 'unit': 'A'}]}).encode(),
                      reference=payload([1j, 2j]), candidate=payload([1.02j, 2.04j]))
        consumed = []
        def load(name):
            consumed.append(name)
            return values[name]
        result = compare_case_inputs(load, {'bound': True})
        self.assertTrue(result['passed'])
        self.assertEqual(consumed, ['manifest', 'reference', 'candidate'])
        self.assertEqual(result['scope'], 'ac_metrics_only')
        self.assertEqual(result['comparisons'], 2)
        with self.assertRaises(ValueError):
            compare_case_inputs(load, {}, policy='transient-windowed-v2')

    def test_malformed_manifest_shapes_fail_as_value_errors(self):
        reference = payload([1j, 2j])
        for config in [
            {'analysis': '.ac'},
            {'analysis': '.ac', 'frequencies_hz': {'1': 0}, 'signals': []},
            {'analysis': '.ac', 'frequencies_hz': [1, 10], 'signals': ['wrong']},
            {'analysis': '.ac', 'frequencies_hz': [1, 10], 'signals': [{'unit': 'A'}]},
            {'analysis': '.ac', 'frequencies_hz': [1, 10], 'signals': [{'name': 'x', 'unit': 'unknown'}]},
        ]:
            with self.subTest(config=config), self.assertRaises(ValueError):
                self.run_case(reference, reference, config)


if __name__ == '__main__':
    main()
