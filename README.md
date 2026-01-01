# GeckoCIRCUITS

A fast circuit simulator optimized for power electronics applications. Multi-domain simulation including thermo-electrical simulation, EMI filter design, and thermal design tools.

Originally written by Andreas Müsing, Andrija Stupar and Uwe Drofenik. Published under GPLv3.

## Manual Build & Run (Command Line)

### Prerequisites
- **Java**: JDK 21 (required - project is built for Java 21)
- **Maven**: 3.6+
- **Memory**: 3GB+ heap recommended for simulations

### macOS Setup (Homebrew)

If you're on macOS, use Homebrew for easy installation:

```bash
# Install Homebrew (if not already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install OpenJDK 21
brew install openjdk@21

# Install Maven
brew install maven

# Set JAVA_HOME (macOS uses zsh by default)
# For Apple Silicon (M1/M2/M3):
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@21' >> ~/.zshrc

# For Intel Macs:
echo 'export JAVA_HOME=/usr/local/opt/openjdk@21' >> ~/.zshrc

# Reload your shell configuration
source ~/.zshrc

# Verify installation
java -version
mvn -version
echo $JAVA_HOME
```

**Note:** If you use bash instead of zsh, replace `~/.zshrc` with `~/.bash_profile` in the commands above.

### Setting up JAVA_HOME

The VSCode tasks and Maven require the `JAVA_HOME` environment variable to be set correctly.

**For Windows users:**

Add to your Environment Variables:
1. Press `Win+R`, type `sysdm.cpl`
2. Go to **Advanced** → **Environment Variables**
3. Add or edit `JAVA_HOME` pointing to your JDK 21 installation
4. Ensure `%JAVA_HOME%\bin` is in your `PATH`

Or for Git Bash users, add to `~/.bashrc` or `~/.bash_profile`:
```bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"  # Adjust to your installation path
```
(Note the Unix-style path with forward slashes)

**For Linux users:**

Install OpenJDK 21 and set JAVA_HOME:
```bash
# Ubuntu/Debian:
sudo apt update
sudo apt install openjdk-21-jdk maven

# Fedora/RHEL:
sudo dnf install java-21-openjdk-devel maven

# Add to ~/.bashrc or ~/.zshrc:
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk' >> ~/.bashrc
source ~/.bashrc
```

**Verify your setup:**
```bash
echo $JAVA_HOME
mvn -version
```

After setting JAVA_HOME, restart your terminal or VSCode for changes to take effect.

### Build
```bash
mvn clean package assembly:single -DskipTests
```

Creates `target/gecko-1.0-jar-with-dependencies.jar`

**Note:** The `-DskipTests` flag is required to skip native library tests that fail on non-Netbeans environments. The application will work correctly.

### Run
```bash
# Standard
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar

# With circuit file
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar resources/Education_ETHZ/ex_1.ipes

# HiDPI displays
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -Dsun.java2d.uiScale=2 -jar target/gecko-1.0-jar-with-dependencies.jar
```


## Quick Start with VSCode

### 1. Open Project in VSCode
```bash
# From the project directory
code .

# Or open from anywhere with the project path
code /path/to/GeckoCIRCUITS
```

### 2. Install Recommended Extensions
When prompted, click **"Install All"** to install Java extensions, or install manually:
- Extension Pack for Java
- Maven for Java
- Debugger for Java

### 3. Build and Run
**Press `F5`** - VSCode will automatically build and run GeckoCIRCUITS!

Or use **`Ctrl+Shift+B`** to just build the project.

## VSCode Development Workflow

### Building
- **`Ctrl+Shift+B`** - Default build (skips tests - recommended)
- **Terminal → Run Task → Build GeckoCIRCUITS (With Tests)** - Full build with tests (may fail)
- **Terminal → Run Task → Clean Build Directory** - Clean target folder

### Running
- **`F5`** - Run with debugger
- **`Ctrl+F5`** - Run without debugger

### Available Run Configurations
Access via Run menu or Debug sidebar (`Ctrl+Shift+D`):

1. **Run GeckoCIRCUITS** - Standard run (auto-builds first)
2. **Run GeckoCIRCUITS (HiDPI)** - For 4K/high-DPI displays
3. **Run GeckoCIRCUITS (No Build)** - Quick restart without rebuilding
4. **Run GeckoCIRCUITS with File** - Opens currently active `.ipes` file
5. **Run GeckoCIRCUITS (Buck Converter Example)** - Opens example circuit

### Debugging
1. Set breakpoints by clicking in the gutter (left of line numbers)
2. Press `F5` to run with debugger
3. Use debug controls: **F5** (Continue), **F10** (Step Over), **F11** (Step Into), **Shift+F5** (Stop)

See [.vscode/README.md](.vscode/README.md) for complete VSCode guide with keyboard shortcuts and tips.


## Example Circuits

Find example circuits in the `resources/` directory:

| Circuit Type | File Path |
|--------------|-----------|
| Buck Converter | `resources/Education_ETHZ/ex_1.ipes` |
| Buck-Boost | `resources/Topologies/BuckBoost_const_dutyCycle.ipes` |
| Three-Phase VSR | `resources/Topologies/three-phase_VSR_simpleControl_250kW.ipes` |

See [TOC.md](TOC.md) for a complete catalog of examples.

## Project Structure

- **`src/main/java/`** - Java source code
  - `ch.technokrat.gecko` - Core application classes
  - `ch.technokrat.gecko.geckocircuits` - Circuit simulation components
  - `ch.technokrat.gecko.geckoscript` - Scripting support
- **`resources/`** - Example circuits and tutorials
- **`pom.xml`** - Maven build configuration
- **`.vscode/`** - VSCode tasks and launch configurations
- **`CLAUDE.md`** - Detailed architecture documentation

Main entry point: `ch.technokrat.gecko.GeckoSim`

## Remote Control & Automation

GeckoCIRCUITS provides comprehensive remote control capabilities for:
- Running simulations programmatically
- Setting and modifying parameters
- Retrieving simulation results
- Performing signal analysis (RMS, THD, FFT, etc.)
- Integrating with MATLAB, Python, or other external tools

**📖 See [REMOTE_CONTROL_MANUAL.md](REMOTE_CONTROL_MANUAL.md) for complete documentation on:**
- Remote Mode (RMI) and Memory-Mapped File Mode (MMF)
- API reference with all available methods
- Code examples in Java, MATLAB, Octave, and Python
- Best practices and troubleshooting

**Quick Example:**
```bash
# Start GeckoCIRCUITS with remote access on port 43035
java -Xmx3G -Dpolyglot.js.nashorn-compat=true \
  -jar target/gecko-1.0-jar-with-dependencies.jar -p 43035

# Connect from MATLAB/Octave
gesim = javaObject('gecko.GeckoRemote');
gesim.connectToGecko(43035);
gesim.runSimulation();
results = gesim.getSignalData('Scope1.Out1', 0, gesim.getSimulationTime(), 1);
gesim.disconnectFromGecko();
```

## Operating Modes

GeckoCIRCUITS supports multiple operating modes:
- **STANDALONE** - Normal desktop application (default)
- **REMOTE** - Remote access via RMI (MATLAB integration)
- **MMF** - Memory-mapped file communication
- **SIMULINK** - MATLAB Simulink integration
- **EXTERNAL** - External tool integration

## Troubleshooting

### Out of Memory Errors
Increase heap size by editing `run.bat` or [.vscode/launch.json](.vscode/launch.json):
- Change `-Xmx3G` to `-Xmx4G` or higher

### Build Fails
```bash
# Try building with tests skipped
mvn clean package assembly:single -DskipTests
```

### VSCode Java Extension Issues
- Press `Ctrl+Shift+P` → **"Java: Clean Java Language Server Workspace"**
- Reload window: `Ctrl+Shift+P` → **"Reload Window"**

### Tests
All tests now pass successfully (160 tests, 0 failures, 0 skipped).
- ModelResultsTest integration tests verify real circuit files can be loaded and simulated
- NativeCTest works on all platforms (Windows, Linux, macOS) with provided native libraries
- No tests need to be skipped for normal builds

## License

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License version 3** (GPLv3) as published by the Free Software Foundation.

For commercial usage/redistribution, please contact Gecko-Research GmbH to obtain a commercial license.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

See [LICENSE](LICENSE) file or visit http://www.gnu.org/licenses/

## Additional Resources

- **Architecture & Development**: [CLAUDE.md](CLAUDE.md)
- **FAQ**: [FAQ.md](FAQ.md)
- **Example Catalog**: [TOC.md](TOC.md)
- **Authors**: [AUTHORS.txt](AUTHORS.txt)
- **Website**: www.gecko-simulations.com
- **Original Sourceforge**: https://sourceforge.net/projects/geckocircuits/
- **Contributions from**: [Technokrat](https://github.com/technokrat/gecko) (HiDPI support and improvements)
