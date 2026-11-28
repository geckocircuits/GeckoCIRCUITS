# Converter trace fixtures

The desktop `SimulationsKern` generates these references independently of the headless engine.
Each fixture contains 1,000 electrical steps at a 1 microsecond interval.

## Node and current layout

The desktop netlist uses the following layout for both converter fixtures.
The source-negative node provides the physical voltage reference.
The solver reference at `p[0]` is the switch node, not the source-negative node.

| Column | Physical quantity |
|--------|-------------------|
| `p[0]` | Switch node potential |
| `p[1]` | Output node potential |
| `p[2]` | Source-negative node potential |
| `p[3]` | Source-positive node potential |
| `p[4]` | Source current from the matrix solution |

Current columns follow this order: `U.1`, `S.1`, `L.1`, `D.1`, `C.1`, `R.1`.
The application fixture adapter compares named circuit nodes relative to source negative.
It leaves the source-current unknown unchanged when shifting the voltage reference.
It never discovers a mapping by fitting simulation values.

## Gate export correction

The original exporter read an unused switch parameter for `gate_S.1`.
That column remained zero during ON states.
The corrected desktop logger reports the applied switch state from its recorded resistance.
Both fixtures use an ON resistance of `0.01` ohm.

On October 29, 2026, 405 buck gate cells and 669 boost gate cells were corrected against independent desktop exports.
Every original timestamp, potential, current, and resistance cell was retained exactly.
The corrected fixtures match fresh desktop exports in every CSV cell.

| Fixture | Original SHA-256 | Corrected SHA-256 |
|---------|------------------|-------------------|
| Buck | `22b0528883c31ceab2a9f38319b816f706679fa6c3c3211d46af5b020606a880` | `e9a79396bab66628ba340939a7a820b5e169951c07b8aab5ac0827622c6c72c2` |
| Boost | `2132b8a0c5b06474fcd271701d68a2a29677eeaacfbd187d2fcdcee8fee99b9f` | `50028d87cea5649f5cbd6428f115c013e224992b826aec5d4a66492af82e4785` |

## Verification

Run the application checks:

```bash
mvn -f src/modules/gecko-simulation-core/pom.xml test -Papplication
```

Generate independent desktop exports into a new output directory:

```bash
xvfb-run -a mvn -f pom-reactor.xml -pl . -am test \
  -Dtest=SimulationTraceLoggerTest,TraceGeneratorTest \
  -Dsurefire.failIfNoSpecifiedTests=false -Dgecko.generateTraces=true
```

The generator preserves existing outputs.
Review topology, timestamps, numerical cells, and state columns before changing a reference fixture.
Do not generate expected values with the headless engine under test.
