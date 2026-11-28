import contextlib
from hashlib import sha256
import io
import json
from pathlib import Path
from unittest import TestCase
from unittest.mock import patch

from transient_compare import compare, main
from verification_batch import SCHEMA, STAGES, _compare, _identity, _read_pin, run_batch


def encode(data):
    return json.dumps(data).encode()


class RecordedIntervalTest(TestCase):
    def test_effective_windows_honor_the_recorded_span(self):
        for stop, widths in [(3, [1, 1]), (7, [1, 5]), (12, [1, 10]), (20, [1, 10])]:
            trace = [(2, 1), (stop, 1)]
            result = compare(trace, trace, start=2, stop=stop, ts=1, atol=0,
                             policy='transient-windowed-v2')
            self.assertTrue(result['passed'])
            self.assertEqual([row['duration'] for row in result['sliding']], widths)
            self.assertEqual([row['requested_duration'] for row in result['sliding']], [1, 10])
            if stop == 3:
                self.assertEqual([row['evaluated'] for row in result['sliding']], [1, 1])
                self.assertEqual([row['shortened'] for row in result['sliding']], [False, True])

    def test_late_recording_still_requires_both_declared_bounds(self):
        trace = [(2, 1), (3, 1)]
        with self.assertRaisesRegex(ValueError, 'complete Ts window'):
            compare(trace, trace, start=2, stop=2.25, ts=1, atol=0,
                    policy='transient-windowed-v2')
        with self.assertRaisesRegex(ValueError, 'full declared coverage'):
            compare(trace, [(2, 1), (2.99, 1)], start=2, stop=3, ts=1, atol=0,
                    policy='transient-windowed-v2')

    def test_cli_reports_coverage_and_distinguishes_considered_startup(self):
        config = encode(dict(start=2, stop=5, ts=1, signals=[dict(name='V(out)', unit='V', atol=0)]))
        for candidate, covered in [(b'2,1\n5,1\n', False), (b'0,1\n5,1\n', True)]:
            header = b'time,V(out) [V]\n'
            files = [io.BytesIO(config), io.BytesIO(header + b'0,1\n5,1\n'), io.BytesIO(header + candidate)]
            output = io.StringIO()
            with patch('builtins.open', side_effect=files), contextlib.redirect_stdout(output):
                status = main(['ref', 'cand', '--manifest', 'config', '--policy', 'transient-windowed-v2'])
            self.assertEqual(status, 0)
            report = json.loads(output.getvalue())
            self.assertEqual(report['required_interval'], [2, 5])
            self.assertEqual(report['coverage_scope'], 'whole_declared_recording')
            self.assertFalse(report['startup_compared'])
            self.assertFalse(report['startup_observed'])
            self.assertEqual(report['startup_available'], {'reference': True, 'candidate': covered})


class EvidenceBatchTest(TestCase):
    def setUp(self):
        self.files = {}

    def add(self, name, data):
        self.files[name] = data
        return dict(path=name, sha256=sha256(data).hexdigest())

    def pinned_bytes(self, base, expected, *, keep):
        data = self.files[expected['path']]
        digest = sha256(data).hexdigest()
        if digest != expected['sha256']:
            raise ValueError('Input digest differs')
        return (data if keep else None), dict(path=expected['path'], sha256=digest, bytes=len(data))

    def case(self, case_id, *, value=1, ac=False):
        config = dict(start=2, stop=4, ts=1, signals=[dict(name='V(out)', unit='V', atol=0)])
        data = dict(source=f'original {case_id}'.encode(), manifest=encode(config),
                    reference=b'time,V(out) [V]\n2,1\n4,1\n',
                    candidate=f'time,V(out) [V]\n2,{value}\n4,{value}\n'.encode())
        if ac:
            data.update(manifest=encode(dict(analysis='.ac', frequencies_hz=[100],
                                             signals=[dict(name='V(out)', unit='V')])),
                        reference=b'frequency,V(out) [V] real,V(out) [V] imag\n100,1,0\n',
                        candidate=f'frequency,V(out) [V] real,V(out) [V] imag\n100,{value},0\n'.encode())
        inputs = {k: self.add(f'{case_id}-{k}', v) for k, v in data.items()}
        case = dict(id=case_id, archive_path=f'{case_id}.asc', source_sha256=inputs['source']['sha256'],
                    analysis=dict(index=0, directive='.ac list 100' if ac else '.tran 0 4 2'),
                    policy='ac-strict-v1' if ac else 'transient-windowed-v2',
                    inputs=inputs, receipts={})
        prior = {}
        for stage in STAGES:
            item = self.add(f'{case_id}-{stage}-artifact', f'{case_id} {stage} evidence'.encode())
            receipt = dict(stage=stage, status='PASS', case=_identity(case), policy=case['policy'],
                           inputs={k: v['sha256'] for k, v in inputs.items()},
                           prior_receipts=prior, artifacts=[item])
            pin = self.add(f'{case_id}-{stage}-receipt', encode(receipt))
            case['receipts'][stage] = pin
            prior[stage] = pin['sha256']
        return case

    def test_batch_keeps_stable_ids_and_continues_failed_cases(self):
        first, invalid, unsupported, failed = [self.case(k, value=v)
                                               for k, v in [('good', 1), ('invalid', 1),
                                                            ('ac', 1), ('fail', 2)]]
        unsupported['analysis']['directive'] = '.ac dec 100 10 100k'
        ac_pass = self.case('ac-pass', ac=True)
        invalid['receipts']['physics'] = first['receipts']['physics']
        self.files[str(Path('cases.json').resolve())] = encode(
            dict(schema=SCHEMA, cases=[invalid, first, unsupported, failed, ac_pass]))
        path = Path('cases.json')
        manifest_hash = sha256(self.files[str(path.resolve())]).hexdigest()
        writes = {}

        def capture(path, report):
            self.assertNotIn(path.name, writes)
            writes[path.name] = encode(report)
            return sha256(writes[path.name]).hexdigest()

        with (patch('verification_batch._read_pin', side_effect=self.pinned_bytes),
              patch('verification_batch.require_free_space'), patch('verification_batch._output_on_d', return_value=True),
              patch.object(Path, 'mkdir'), patch.object(Path, 'open'),
              patch('verification_batch._report', side_effect=capture)):
            audit, digest = run_batch(path, manifest_hash, Path('unused-new-evidence'))
        self.assertEqual(audit['counts'], dict(invalid=1, metric_pass=2, unsupported=1, metric_fail=1))
        self.assertEqual([row['id'] for row in audit['cases']], ['invalid', 'good', 'ac', 'fail', 'ac-pass'])
        self.assertEqual(audit['qualification'], 'independent_review_required')
        self.assertEqual(digest, sha256(writes['batch-audit.json']).hexdigest())
        for row in audit['cases']:
            self.assertEqual(row['sha256'], sha256(writes[row['report']]).hexdigest())

    def test_rejects_same_case_stage_with_wrong_predecessor(self):
        case = self.case('original')
        stage = json.loads(self.files['original-physics-receipt'])
        stage['prior_receipts']['native'] = 'f' * 64
        case['receipts']['physics'] = self.add('wrong-prior', encode(stage))
        with patch('verification_batch._read_pin', side_effect=self.pinned_bytes):
            with self.assertRaisesRegex(ValueError, 'physics receipt differs'):
                _compare(Path('.'), case, dict(case=_identity(case)))

    def test_read_pin_checks_actual_bytes(self):
        path = Path(__file__).resolve()
        pin = dict(path=str(path), sha256=sha256(path.read_bytes()).hexdigest())
        data, observed = _read_pin(path.parent, pin, keep=True)
        self.assertEqual(data, path.read_bytes())
        self.assertEqual(observed['bytes'], len(data))
        with self.assertRaisesRegex(ValueError, 'Input digest differs'):
            _read_pin(path.parent, dict(pin, sha256='0' * 64), keep=False)
