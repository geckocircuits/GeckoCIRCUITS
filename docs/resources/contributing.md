---
title: Contributing
---

# Contributing to GeckoCIRCUITS

Thank you for your interest in contributing! GeckoCIRCUITS is an open-source power electronics simulator and we welcome contributions of all kinds.

## Ways to Contribute

| Contribution | Description | Difficulty |
|-------------|-------------|------------|
| Bug reports | Report issues you encounter | Easy |
| Circuit examples | Add .ipes example circuits | Easy-Medium |
| Documentation | Improve tutorials, fix typos | Easy-Medium |
| Bug fixes | Fix reported issues | Medium |
| New features | Add simulation capabilities | Medium-Hard |
| Core engine | Improve solvers, matrix operations | Hard |

## Development Setup

### Prerequisites

- **Java 21** (Temurin recommended)
- **Maven 3.8+**
- **Git**

### Build from Source

```bash
git clone https://github.com/tinix84/GeckoCIRCUITS.git
cd GeckoCIRCUITS
mvn clean package assembly:single -DskipTests
```

### Run Tests

```bash
# All unit tests
mvn test

# Single test class
mvn test -Dtest=ClassName

# With coverage report
mvn clean test jacoco:report
# View: target/site/jacoco/index.html
```

### Run the Application

```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar
```

## Code Architecture

GeckoCIRCUITS uses a **multi-module Maven structure**:

```
GeckoCIRCUITS/
├── src/main/java/ch/technokrat/gecko/
│   ├── GeckoSim.java              # Entry point
│   ├── geckocircuits/
│   │   ├── circuit/               # Circuit components, MNA matrix
│   │   ├── control/               # 64 control block calculators
│   │   ├── math/                  # Matrix operations, FFT
│   │   ├── newscope/              # Oscilloscope visualization
│   │   └── datacontainer/         # Signal storage
│   └── i18n/                      # Internationalization
├── gecko-simulation-core/          # GUI-free simulation engine
└── gecko-rest-api/                 # REST API server
```

### Key Architectural Rules

- **`gecko-simulation-core`** must have **zero** Swing/AWT imports. The `CorePackageValidationTest` enforces this.
- Control calculators in `control/calculators/` are pure computation, no GUI.
- The MNA (Modified Nodal Analysis) matrix system is in `circuit/matrix/`.

## Pull Request Process

1. **Fork** the repository
2. **Create a branch** from `main`:
   ```bash
   git checkout -b fix/description-of-change
   ```
3. **Make your changes** with clear, focused commits
4. **Run tests** to ensure nothing breaks:
   ```bash
   mvn test
   ```
5. **Push** and open a Pull Request against `main`

### PR Guidelines

- Keep PRs focused - one logical change per PR
- Include a clear description of what changed and why
- Add tests for new functionality
- Don't break existing tests
- Follow the existing code style

### Branch Naming

| Prefix | Use |
|--------|-----|
| `fix/` | Bug fixes |
| `feature/` | New features |
| `docs/` | Documentation changes |
| `refactor/` | Code refactoring |
| `test/` | Test additions |

## Adding Circuit Examples

Contributing example circuits is one of the easiest ways to help:

1. Create your `.ipes` circuit in GeckoCIRCUITS
2. Test that it simulates correctly
3. Add it to the appropriate directory under `resources/examples/` or `resources/tutorials/`
4. Include a `README.md` with:
   - What the circuit demonstrates
   - Key parameters and expected results
   - Learning objectives (for tutorials)

### Example README Template

```markdown
# Example: [Name]

## Overview
Brief description of what this circuit demonstrates.

## Specifications
| Parameter | Value |
|-----------|-------|
| Topology  | Buck  |
| Vin       | 48V   |
| Vout      | 12V   |

## How to Use
1. Open `circuit_name.ipes` in GeckoCIRCUITS
2. Run the simulation (F5)
3. Open the SCOPE to view waveforms

## Expected Results
- Output voltage: ~12V
- Ripple: < 1%
```

## Reporting Issues

When reporting bugs, please include:

- GeckoCIRCUITS version (or commit hash)
- Java version (`java -version`)
- Operating system
- Steps to reproduce
- Expected vs actual behavior
- Console output or error messages

## Code Style

- Follow existing patterns in the codebase
- Use meaningful variable and method names
- Keep methods focused and reasonably sized
- Add Javadoc for public API methods

## License

By contributing, you agree that your contributions will be licensed under the same [dual license](https://github.com/tinix84/GeckoCIRCUITS/blob/main/LICENSE) as the project.

## Questions?

Open a [GitHub Discussion](https://github.com/tinix84/GeckoCIRCUITS/discussions) or file an issue.
