from unittest import TestCase, main
from unittest.mock import patch
from pathlib import Path

from scoreboard import capability, summarize, verify_inventory


def case(issues=(), status='partial_import', parsed=True):
    attempt = {'status': status, 'parse': {'ipes_parse': 'passed'} if parsed else None, 'issues': list(issues)}
    return {'archive_path': 'example.asc', 'sha256': 'source-hash', 'original_analyses': ['.tran 1u 1m'],
            'attempts': {'selected_profile': attempt, 'legacy_non_ac_without_stock_or_hierarchy': attempt}}


class ScoreboardTest(TestCase):
    def test_partial_parse_does_not_count_as_conversion_or_acceptance(self):
        row = summarize([case()], [])['by_analysis']['.tran']
        self.assertEqual(dict(row), {'declared': 1, 'converts': 0, 'opens_in_core': 1,
                                    'runs_to_completion': 0, 'numerically_accepted': 0})

    def test_capability_counts_analyses_not_issue_instances(self):
        issue = {'code': 'UNSUPPORTED_TRANSIENT_OPTION', 'message': 'uic'}
        report = summarize([case([issue, issue])], [])
        self.assertEqual(report['non_ac_legacy_capability_gaps'], {'.tran options': 1})

    def test_unrelated_hash_cannot_inherit_acceptance(self):
        history = {'archive_path': 'example.asc', 'directive_index': 0, 'sha256': 'wrong',
                   'original_directive': '.tran 1u 1m'}
        with self.assertRaisesRegex(ValueError, 'source differs'):
            summarize([case()], [history])

    def test_behavioural_spike_does_not_automatically_exclude_tier_b(self):
        issue = {'code': 'UNSUPPORTED_SYMBOL', 'component': 'B1', 'message': 'Unsupported LTspice symbol: bv'}
        row = summarize([case([issue])], [])['analyses'][0]
        self.assertEqual(row['tier'], 'B')
        self.assertEqual(row['behavioural_spike_issue'], 162)

    def test_physics_tier_requires_observed_blocker_and_solver_link(self):
        issue = {'code': 'DIODE_MODEL_APPROXIMATION', 'message': 'unsupported exponential behavior'}
        row = summarize([case([issue])], [])['analyses'][0]
        self.assertEqual((row['tier'], row['solver_issues']), ('C', [163]))

    def test_each_declared_analysis_keeps_its_own_identity(self):
        circuit = case(status='complete_import')
        circuit['original_analyses'].append('.ac dec 10 1 100')
        report = summarize([circuit], [])
        self.assertEqual(len(report['analyses']), 2)
        self.assertEqual(report['by_analysis']['.ac']['declared'], 1)

    def test_explicit_averaged_transient_is_tier_a(self):
        circuit = case()
        circuit['archive_path'] = '3-phase PFC averaged models - dq0.asc'
        self.assertEqual(summarize([circuit], [])['analyses'][0]['tier'], 'A')

    def test_unindexed_metadata_cannot_label_replay_as_fresh(self):
        names = {'preview.json', 'prior-qualification.json'}
        for folder in ('', 'unselected/', 'legacy-non-ac/'):
            names.update(folder + name for name in ('results.json', 'input-provenance-before.json',
                                                    'input-provenance-after.json'))
        with patch('scoreboard.digest', return_value='seal'), patch('scoreboard.read', return_value=dict.fromkeys(names)):
            with patch('scoreboard.Path.exists', return_value=True):
                with self.assertRaisesRegex(ValueError, 'run-metadata.json'):
                    verify_inventory(Path(r'D:\unused-sealed-audit'), 'seal')

    def test_resource_failure_keeps_missing_diagnostics_explicit_and_tier_provisional(self):
        report = summarize([case(status='output_limit_incomplete', parsed=False)], [])
        self.assertEqual(report['non_ac_legacy_capability_gaps']['resource-limited or failed diagnostics'], 1)
        self.assertTrue(report['analyses'][0]['tier_provisional'])
        self.assertEqual(report['by_analysis']['.tran']['numerically_accepted'], 0)


if __name__ == '__main__':
    main()
