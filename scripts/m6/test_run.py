from pathlib import Path
from unittest import TestCase, main
from unittest.mock import patch

from run import BUDGET, completion_storage, validate_destinations


class DestinationAdmissionTest(TestCase):
    def test_output_cannot_mutate_sealed_qualification_audit(self):
        sealed = Path(r'D:\unused-qualification')
        with patch('run.Path.exists', return_value=False):
            with self.assertRaisesRegex(ValueError, 'outside the sealed'):
                validate_destinations(sealed / 'new-run', sealed)

    def test_existing_report_refuses_before_running_imports(self):
        output = Path(r'D:\unused-new-run')
        report = output.with_name(output.name + '-scoreboard')
        with patch.object(Path, 'exists', autospec=True, side_effect=lambda path: path == report):
            with self.assertRaisesRegex(ValueError, 'all be new'):
                validate_destinations(output, Path(r'D:\unused-qualification'))

    def test_existing_temp_refuses_before_running_imports(self):
        output = Path(r'D:\unused-new-run')
        with patch.object(Path, 'exists', autospec=True, side_effect=lambda path: path.name == output.name and path != output):
            with self.assertRaisesRegex(ValueError, 'all be new'):
                validate_destinations(output, Path(r'D:\unused-qualification'))

    def test_final_report_and_temp_are_charged_to_run_budget(self):
        with patch('run.directory_bytes', side_effect=[BUDGET - 2, 1, 2]):
            with self.assertRaisesRegex(RuntimeError, 'Whole-run storage guard'):
                completion_storage('output', 'report', 'temp')


if __name__ == '__main__':
    main()
