from unittest import TestCase, main

from accept import cause, gecko_traces, score, select, summarize, trace_key

FREQ = [10.0, 100.0, 1000.0]
REF = {'V(out)': [1 + 1j, 0.5 - 0.2j, 0.01j], 'I(R1)': [1e-3 + 0j, -2e-3j, 1e-6 + 1e-6j]}


def scaled(factor, name='V(out)'):
    return {trace_key(key): [value * (factor if key == name else 1) for value in values]
            for key, values in REF.items()}


class LeanProtocolComparatorTest(TestCase):
    """The comparator guarantees of #198, verified once instead of per case."""

    def test_identical_traces_pass(self):
        row, files, failure = score(FREQ, REF, FREQ, scaled(1))
        self.assertIsNone(failure)
        self.assertEqual((row['trace_count'], row['point_count']), (2, 3))
        self.assertEqual(set(files), {'manifest.json', 'reference.csv', 'candidate.csv', 'compare.json'})

    def test_two_percent_error_passes(self):
        row, _, failure = score(FREQ, REF, FREQ, scaled(1.02))
        self.assertIsNone(failure)
        self.assertEqual(row['worst_trace'], 'V(out)')
        self.assertAlmostEqual(row['worst_ratio'], 2 / 3)

    def test_four_percent_error_fails(self):
        row, _, failure = score(FREQ, REF, FREQ, scaled(1.04))
        self.assertIn('1 of 2 traces exceed 3%', failure)
        self.assertEqual(row['failing_traces'], ['V(out)'])
        self.assertAlmostEqual(row['worst_ratio'], 4 / 3)

    def test_sign_reversed_trace_fails(self):
        row, _, failure = score(FREQ, REF, FREQ, scaled(-1, 'I(R1)'))
        self.assertIsNotNone(failure)
        self.assertEqual(row['failing_traces'], ['I(R1)'])

    def test_trace_missing_in_gecko_fails(self):
        candidate = scaled(1)
        del candidate['i(r1)']
        row, _, failure = score(FREQ, REF, FREQ, candidate)
        self.assertEqual(failure, 'Gecko lacks 1 LTspice traces: I(R1)')
        self.assertEqual(row['trace_count'], 1)

    def test_ltspice_internal_nodes_are_listed_not_compared(self):
        reference = dict(REF, **{'V(x1:n001)': [1j] * 3, 'I(X1:R5)': [1j] * 3})
        row, _, failure = score(FREQ, reference, FREQ, scaled(1))
        self.assertIsNone(failure)
        self.assertEqual(row['ltspice_only_internal'], ['I(X1:R5)', 'V(x1:n001)'])
        port = dict(REF, **{'Ix(x1:OUT)': [1j] * 3})
        self.assertIn('Ix(x1:OUT)', score(FREQ, port, FREQ, scaled(1))[2])

    def test_frequency_grid_must_match(self):
        with self.assertRaisesRegex(Exception, 'frequency grid differs'):
            score(FREQ, REF, [10.0, 100.0, 1000.001], scaled(1))


class TraceMappingTest(TestCase):
    NET = '\n'.join(['* title', 'R1 N001 0 1k', 'V§Supply in N001 AC 1', 'X§X2 N001 in out opamp',
                     '.subckt opamp - + OUT', 'R1 OUT N009 10', '.ends opamp', '.end'])

    def member(self, instance, order, path=(), kind='PRIMITIVE'):
        return dict(instance=instance, spiceOrder=order, occurrencePath=list(path), kind=kind)

    def test_groups_take_ltspice_node_names_through_root_device_pins(self):
        result = dict(groups=[
            dict(ground=False, physicalMembers=[self.member('R1', 1), self.member('R1', 1, ['X2']),
                                                self.member('X2', 1, kind='PARENT_PIN')], voltageV=[[1, 2]]),
            dict(ground=False, physicalMembers=[self.member('X2', 3, kind='PARENT_PIN')], voltageV=[[3, 4]]),
            dict(ground=False, physicalMembers=[self.member('R1', 2, ['X2'])], voltageV=[[5, 6]]),
            dict(ground=True, physicalMembers=[self.member('R1', 2)], voltageV=[[0, 0]])],
            currents=[dict(instance='Supply', currentA=[[7, 8]]), dict(instance='X2:R1', currentA=[[9, 0]])],
            hierarchy=dict(ports=[dict(external=dict(instance='X2', occurrencePath=[]), internal=dict(instance='OUT'),
                                       currentIntoChildA=[[1, 1]])]))
        traces = gecko_traces(result, self.NET)
        self.assertEqual(traces, {'v(n001)': [1 + 2j], 'v(out)': [3 + 4j], 'i(supply)': [7 + 8j],
                                  'i(x2:r1)': [9 + 0j], 'ix(x2:out)': [1 + 1j]})
        self.assertEqual(trace_key('I(V§Supply)'), 'i(supply)')

    def test_a_group_spanning_two_ltspice_nodes_is_a_failure(self):
        result = dict(groups=[dict(ground=False, voltageV=[], physicalMembers=[
            self.member('R1', 1), self.member('R1', 2)])], currents=[])
        with self.assertRaisesRegex(Exception, 'several LTspice nodes'):
            gecko_traces(result, self.NET)


class SummaryTest(TestCase):
    def test_failures_group_by_stage_and_normalised_cause_largest_first(self):
        rows = [dict(id='001', verdict='failed', stage='import',
                     message='import failed: CONVERSION_FAILED: /mnt/d/x/author/Tof/Sub circuits/a b.asc [X2]:57 Unknown scalar parameter: rol'),
                dict(id='002', verdict='failed', stage='import',
                     message='import failed: CONVERSION_FAILED: D:\\y\\c.asc [X1]:12 Unknown scalar parameter: rol'),
                dict(id='003', verdict='failed', stage='compare', message='Gecko lacks 2 LTspice traces: V(n001), I(R2)'),
                dict(id='004', verdict='accepted', stage=None, message=None)]
        summary = summarize(rows)
        self.assertEqual(summary['accepted'], ['004'])
        self.assertEqual(summary['failed_by_stage'], {'import': 2, 'compare': 1})
        first, second = summary['failure_groups']
        self.assertEqual((first['stage'], first['count'], first['ids']), ('import', 2, ['001', '002']))
        self.assertEqual(first['cause'], 'import failed: CONVERSION_FAILED: <file> []:# Unknown scalar parameter: rol')
        self.assertEqual(second['cause'], cause('Gecko lacks 5 LTspice traces: I(C1)'))

    def test_select_filters_by_declared_analysis_and_id(self):
        inventory = [dict(id='001', original_analyses=['.tran 1m']),
                     dict(id='002', original_analyses=['.AC dec 10 1 1k']),
                     dict(id='003', original_analyses=['.ac lin 5 1 2'])]
        self.assertEqual([c['id'] for c in select(inventory, 'ac', 'all')], ['002', '003'])
        self.assertEqual([c['directive'] for c in select(inventory, 'ac', '3')], ['.ac lin 5 1 2'])


if __name__ == '__main__':
    main()
