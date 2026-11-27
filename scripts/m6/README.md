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

## Transient waveform comparison

Use one CSV for the reference and one for the candidate. The first column is `time` in seconds.
Use one `name [unit]` column for each required signed signal.
Both files must contain exactly the declared signal columns. The column order can differ.

Prepare the comparison manifest before you look at the errors. Declare startup at time zero and the full stop time.
Use seconds for the stop time and switching period `ts`. Use the signal unit for `atol` and optional `scale`.

```json
{
  "start": 0,
  "stop": 0.002,
  "ts": 0.0001,
  "signals": [
    {"name": "V(out)", "unit": "V", "atol": 0.00001},
    {"name": "I(V1)", "unit": "A", "atol": 0.000001, "scale": 2}
  ]
}
```

For this manifest, use the CSV header `time,V(out) [V],I(V1) [A]`.

```powershell
& $auditPython -B scripts/m6/transient_compare.py reference.csv candidate.csv --manifest comparison.json
```

Both traces must cover both bounds. Bracketing samples permit interpolation at a bound.
The comparator integrates linear segments on the union of both time grids.
It uses the original signed values on a shared time axis without a phase shift.
The default signal scale is the maximum absolute reference value over the declared interval.

Each limit is the larger of the absolute tolerance and the applicable percentage below.

| Check | Error | Relative limit |
|---|---|---|
| Peak | Maximum absolute instantaneous difference | 10% of the signal scale |
| Sliding average | Absolute difference of averages | 1% of reference window RMS |
| Sliding RMS | Absolute difference of RMS values | 3% of reference window RMS |
| Sliding waveform RMSE | Root mean square of the signed pointwise difference | 3% of reference window RMS |
| Full average | Absolute difference of averages | 1% of full reference RMS |
| Full waveform RMSE | Root mean square of the signed pointwise difference | 2% of full reference RMS |

The comparator evaluates both `Ts` and `10 Ts` windows.
For each duration, it checks starts at union knots and at knots minus the duration within the declared interval.
It also checks the first and last possible windows.
This finite protocol does not search for extrema between those starts.
An interval shorter than `10 Ts` is invalid.
A zero limit requires zero error.

The JSON report records the input hashes, required interval, startup coverage, and worst result for each check.
Exit codes are 0 for a metric pass, 1 for a metric failure, and 2 for invalid input.
A metric pass supplies evidence for review. The command has no qualification ledger access.
The AC numerical policy remains the existing 3% comparison.
