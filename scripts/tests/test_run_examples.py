"""Exercise the example runner as a CLI, including build and failure paths."""
import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]
RUNNER = ROOT / "scripts/run-examples.py"
spec = importlib.util.spec_from_file_location("run_examples", RUNNER)
runner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runner)
JAR_NAME = "gecko-1.0-jar-with-dependencies.jar"
REQUIRED_CLASSES = ("gecko/GeckoSim.class", "gecko/systemtests/CircuitValidator.class")


class ExampleRunnerTest(unittest.TestCase):
    def prepare(self, jar=True):
        # Preserve diagnostic fixtures instead of deleting test data.
        root = Path(tempfile.mkdtemp(prefix="gecko-example-runner-"))
        (root / "scripts").mkdir()
        shutil.copyfile(RUNNER, root / "scripts/run-examples.py")
        (root / "target").mkdir()
        for _, relative in runner.EXAMPLE_CIRCUITS:
            circuit = root / "resources" / relative
            circuit.parent.mkdir(parents=True, exist_ok=True)
            circuit.write_text("test circuit")
        if jar:
            with zipfile.ZipFile(root / "target" / JAR_NAME, "w") as archive:
                for name in REQUIRED_CLASSES:
                    archive.writestr(name, b"test class")
        tools = root / "tools"
        tools.mkdir()
        java = tools / "java"
        java.write_text("""#!/usr/bin/env python3
import json, os, pathlib, sys
root = pathlib.Path.cwd()
with (root / "java-calls.jsonl").open("a") as calls:
    calls.write(json.dumps(sys.argv[1:]) + "\\n")
sys.exit(7 if pathlib.Path(sys.argv[-1]).name == os.environ.get("FAIL_EXAMPLE") else 0)
""")
        # Assembly without package omits the project artifact, as the CI failure did.
        maven = tools / "mvn"
        maven.write_text("""#!/usr/bin/env python3
import json, os, pathlib, sys, zipfile
root = pathlib.Path.cwd()
args = sys.argv[1:]
with (root / "maven-calls.jsonl").open("a") as calls:
    calls.write(json.dumps(args) + "\\n")
if os.environ.get("FAIL_BUILD"):
    print("BUILD-DIAGNOSTIC: core could not be built")
    sys.exit(9)
if "pom-reactor.xml" in args and "install" in args:
    (root / "core-installed").touch()
elif "assembly:single" in args:
    if "package" not in args or not (root / "core-installed").exists():
        sys.exit(42)
    with zipfile.ZipFile(root / "target/gecko-1.0-jar-with-dependencies.jar", "w") as jar:
        jar.writestr("gecko/GeckoSim.class", b"test class")
        jar.writestr("gecko/systemtests/CircuitValidator.class", b"test class")
else:
    sys.exit(43)
""")
        java.chmod(0o755)
        maven.chmod(0o755)
        return root

    def run_cli(self, root, **environment):
        env = dict(os.environ, PATH=str(root / "tools") + os.pathsep + os.environ["PATH"],
                   **environment)
        return subprocess.run([sys.executable, str(root / "scripts/run-examples.py")],
                              cwd=root.parent, env=env, capture_output=True, text=True,
                              timeout=15)

    def calls(self, root, name):
        path = root / (name + "-calls.jsonl")
        return [json.loads(line) for line in path.read_text().splitlines()] if path.exists() else []

    def test_all_six_examples_run_from_the_project_directory(self):
        root = self.prepare()
        result = self.run_cli(root)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        calls = self.calls(root, "java")
        self.assertEqual(6, len(calls))
        self.assertEqual({str(root / "resources" / path) for _, path in runner.EXAMPLE_CIRCUITS},
                         {call[-1] for call in calls})
        self.assertTrue(all("gecko.systemtests.CircuitValidator" in call for call in calls))
        self.assertIn("6 passed, 0 failed", result.stdout)

    def test_each_java_failure_causes_a_failing_exit(self):
        for name, relative in runner.EXAMPLE_CIRCUITS:
            with self.subTest(example=name):
                root = self.prepare()
                result = self.run_cli(root, FAIL_EXAMPLE=Path(relative).name)
                self.assertNotEqual(0, result.returncode)
                self.assertEqual(6, len(self.calls(root, "java")))
                self.assertIn("5 passed, 1 failed", result.stdout)

    def test_missing_curated_example_is_a_failure(self):
        root = self.prepare()
        missing = root / "resources" / runner.EXAMPLE_CIRCUITS[0][1]
        missing.rename(missing.with_suffix(".preserved"))
        result = self.run_cli(root)
        self.assertNotEqual(0, result.returncode, result.stdout)
        self.assertIn("5 passed, 1 failed", result.stdout)

    def test_dependency_only_jar_fails_before_java_is_launched(self):
        root = self.prepare(jar=False)
        with zipfile.ZipFile(root / "target" / JAR_NAME, "w") as archive:
            archive.writestr("gecko/core/Simulation.class", b"dependency only")
        result = self.run_cli(root)
        self.assertNotEqual(0, result.returncode, result.stdout)
        self.assertEqual([], self.calls(root, "java"))
        self.assertIn("Invalid application JAR", result.stdout + result.stderr)

    def test_missing_jar_builds_core_then_packages_application(self):
        root = self.prepare(jar=False)
        result = self.run_cli(root)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        commands = self.calls(root, "maven")
        self.assertEqual(2, len(commands))
        self.assertIn("pom-reactor.xml", commands[0])
        self.assertIn("install", commands[0])
        self.assertIn("package", commands[1])
        self.assertIn("assembly:single", commands[1])
        self.assertEqual(6, len(self.calls(root, "java")))

    def test_build_failure_stops_validation_and_preserves_diagnostic(self):
        root = self.prepare(jar=False)
        result = self.run_cli(root, FAIL_BUILD="1")
        self.assertNotEqual(0, result.returncode)
        self.assertEqual([], self.calls(root, "java"))
        self.assertIn("BUILD-DIAGNOSTIC", result.stdout + result.stderr)

    def test_ci_packages_project_and_enforces_example_failures(self):
        workflow = (ROOT / ".github/workflows/ci.yml").read_text()
        self.assertIn("mvn package assembly:single -DskipTests", workflow)
        step = workflow.split("      - name: Validate example circuits\n", 1)[1].split(
            "\n      - name:", 1)[0]
        self.assertNotIn("continue-on-error", step)
        self.assertIn("xvfb-run -a python3 scripts/run-examples.py", step)
        self.assertIn("test_*.py", workflow)


if __name__ == "__main__":
    unittest.main()
