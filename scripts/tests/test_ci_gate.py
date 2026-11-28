"""Check the CI entry point and its failure propagation without running Maven."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class CiGateTest(unittest.TestCase):
    def test_gate_propagates_test_and_coverage_failures(self):
        # Keep this small diagnostic directory recoverable after the test.
        directory = Path(tempfile.mkdtemp(prefix="gecko-ci-gate-"))
        for name, body in {
            "xvfb-run": '#!/bin/bash\nshift\nexec "$@"\n',
            "mvn": '#!/bin/bash\nprintf "%s\\n" "$@" > "$GATE_ARGS"\nexit "$GATE_EXIT"\n',
        }.items():
            executable = directory / name
            executable.write_text(body)
            executable.chmod(0o755)
        for exit_code in (0, 1, 42):
            env = dict(os.environ, PATH=str(directory) + os.pathsep + os.environ["PATH"],
                       GATE_ARGS=str(directory / "arguments"), GATE_EXIT=str(exit_code))
            result = subprocess.run(["bash", "scripts/verify-ci.sh"], cwd=ROOT, env=env,
                                    capture_output=True, text=True)
            self.assertEqual(exit_code, result.returncode, result.stderr)
            arguments = (directory / "arguments").read_text().splitlines()
            self.assertIn("verify", arguments)
            self.assertIn("-Pall-tests", arguments)
            self.assertNotIn("-Dmaven.test.failure.ignore=true", arguments)

    def test_workflow_uses_the_enforcing_entry_point(self):
        workflow = (ROOT / ".github/workflows/ci.yml").read_text()
        self.assertIn("bash scripts/verify-ci.sh", workflow)
        self.assertNotIn("-Dmaven.test.failure.ignore=true", workflow)


if __name__ == "__main__":
    unittest.main()
