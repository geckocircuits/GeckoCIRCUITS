# M6 archive scoreboard

Run these commands from the repository root in PowerShell. Use a new run name each time. Outputs stay on D:, and the runner does not delete files.

```powershell
$auditPython = 'C:\Users\tinix\miniconda3\envs\2brain_env\python.exe'
& $auditPython -B scripts/m6/run.py `
  --output-dir 'D:\codex-evidence\gecko-m6-runs\my-new-run' `
  --qualification-audit 'D:\codex-evidence\gecko-m6-20261124\archive-import-batch-01' `
  --qualification-inventory-sha256 dbaeddf165f340987215bde1ccf88b79c44d185c33453e81b7f8286a1621f8ad `
  --dry-run
```

Inspect the preview, then repeat the command without `--dry-run`. The driver runs the selected-profile, unselected and legacy diagnostic modes with one JVM at a time. These are import and IPES deserialization checks, not simulations.

The default runtime is the frozen audited baseline. **To test a changed checkout, pass `--runtime-manifest` for that checkout's matching build artifacts.** The manifest contains `java`, `core` and `desktop` objects, each with a `path` and `sha256`. The Java executable must have `javac.exe` beside it. The runner verifies the hashes before use.

Each case needs the 4.2 GB free-space floor plus the 512 MB admission reserve on both drives. Monitoring retains a 32 MB dirty reserve. Evidence and temporary files share the run budget, including earlier modes and the final sibling report. Temporary directories are under `D:\codex-temp\gecko-m6\`.

The qualification audit must be an immutable, hash-sealed snapshot. The driver verifies its inventory and carries its historical qualification ledger forward. A new ledger entry requires the separate numerical verification and review specified by issue #48. Parsing or completing an import never creates a numerical acceptance.

To replay an existing sealed import audit without launching Java:

```powershell
& $auditPython -B scripts/m6/scoreboard.py `
  --audit-dir 'D:\codex-evidence\gecko-m6-20261124\archive-import-batch-01' `
  --inventory-sha256 dbaeddf165f340987215bde1ccf88b79c44d185c33453e81b7f8286a1621f8ad `
  --output-dir 'D:\codex-evidence\gecko-m6-runs\my-new-replay'
```

Use the inventory hash printed or recorded for the audit being replayed. The example hash above belongs only to the pinned baseline audit.

The Markdown and JSON reports distinguish full conversion, core IPES deserialization, historical completed runs and historical numerical acceptance. Deserialization does not prove simulation readiness. Counts can differ between stages because historical qualifications can use a different import route from the current selected-profile check. Blocker counts overlap; unexpanded subcircuits and resource-limited diagnostics can hide further blockers. A falling diagnostic count caused by a failed or capped attempt is not a capability improvement. Provisional tier assignments do not waive numerical verification.

Run the Python checks without creating temporary fixtures:

```powershell
& $auditPython -B -m unittest discover -s scripts/m6 -p 'test_*.py' -v
```
