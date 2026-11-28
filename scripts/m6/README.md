# M6 batch acceptance (lean protocol, #198)

Run from WSL in the repository root, after building the core jar once:

```bash
mvn -o -f pom-reactor.xml -pl src/modules/gecko-simulation-core -am -DskipTests package
python3 -B scripts/m6/accept.py --analysis ac --ids 106,113 --output-dir /mnt/d/claude-m6/my-new-run
python3 -B scripts/m6/accept.py --analysis ac --ids all --output-dir /mnt/d/claude-m6/my-new-run
```

Each case runs four stages, each with its own timeout (`--timeout`, default 300 s):
`import` (importer `Main` with the 26.1.1 profile, then reopen in core),
`ltspice` (`LTspice.exe -b` on the unchanged schematic),
`gecko` (`gecko.core.ac.AcMain` on the saved `.ipes`) and
`compare` (`ac_compare.py` on every trace present in both outputs).
A reference trace that Gecko lacks fails the case. LTspice-only subcircuit internals (`V(x1:n001)`, `I(X1:R5)`) are listed and not compared; root port currents `Ix(x1:pin)` are compared.
`worst_ratio` is the worst error divided by its allowed error, so a case passes when it is at most 1.

The output folder holds `results.jsonl` (one row per case, written as the batch runs), `summary.json` and `summary.md` (failures grouped by stage and normalised message, largest group first) and `cases/<id>/` with every intermediate file.
The command writes nothing outside the output folder. Windows temp, `%APPDATA%` and `%LOCALAPPDATA%` for LTspice point into it, and the batch stops when its drive has under 3 GB free.

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

### Recorded-window policy v2

Add `--policy transient-windowed-v2` to select the recorded-window policy.
Supply the original declared recording start and stop in the comparison manifest.
The start can be greater than zero. Both traces must still cover both declared bounds.
The report marks whether the comparison includes startup and whether each input covers time zero.
A late recording covers the whole declared recording only.

The `Ts` window must fit. The second window has duration `min(10 Ts, stop - start)`.
The report shows both requested and actual durations.
The metric limits remain the same. Without `--policy`, the CLI continues to use v1.

## Run a batch of pinned comparisons

Use a batch manifest to compare original analyses with pinned input files and evidence.
Paths in pins are relative to the batch manifest unless they are absolute.
Each pin has `path` and a lowercase `sha256`.
Record the batch manifest SHA-256 before execution.

The manifest has `schema` set to `m6-metric-batch-v1` and a `cases` array.
Each case has these fields:

| Field | Value |
|---|---|
| `id` | A unique lowercase ID with letters, digits, hyphens, or underscores |
| `archive_path` | The original relative schematic path with forward slashes |
| `source_sha256` | The original schematic digest |
| `analysis` | An object with the zero-based directive `index` and original `directive` |
| `policy` | The explicit comparison policy ID |
| `inputs` | Pins named `source`, `reference`, `candidate`, and `manifest` |
| `receipts` | Pins named `source`, `runtime`, `native`, and `physics` |

Each stage receipt is a JSON object. It gives the `stage`, `status` of `PASS`, and `policy`.
Copy the case ID, archive path, source digest, and analysis into the receipt's `case` object.
Set its `inputs` object to the four named input digests.
Set `prior_receipts` to the digests of all earlier stages, starting with an empty object for `source`.
Set `artifacts` to the supporting file pins.
These receipt envelopes bind the supporting evidence to the case. Independent review must still validate the evidence.

```powershell
& $auditPython -B scripts/m6/verification_batch.py `
  --manifest 'D:\codex-evidence\case-inputs\batch.json' `
  --manifest-sha256 $batchManifestSha256 `
  --output-dir 'D:\codex-evidence\case-results-new'
```

The command writes one exclusive report per case and an audit index with all report digests.
A checksum file identifies the audit index. The command preserves partial reports on a global failure.
A case with invalid inputs or failed metrics does not stop the subsequent cases.
Exit code 1 indicates at least one case without a metric pass. Exit code 2 indicates a batch input or output error.
No simulator starts from the manifest. Metric results and bound receipts require independent review before qualification.

### AC comparisons in a batch

Use `ac-strict-v1` for an AC case.
The metric manifest has `analysis` set to `.ac`, the declared `frequencies_hz`, and the required `signals`.
Each signal has a `name`, a `unit`, and an optional positive reference `floor`.
The default floors are 1 nV for V and 1 pA for A.

Use one `frequency` column and two signed columns per signal.
For example, use `V(out) [V] real` and `V(out) [V] imag`.
Both exports must contain the exact declared frequency grid and signal set.
Use enough digits to round-trip each frequency to the declared numeric value.
The limit at each frequency is 3% of the larger of the reference magnitude and its floor.
The comparator uses the magnitude of the signed complex difference.

A case with an unsupported policy receives an explicit `unsupported` result.
