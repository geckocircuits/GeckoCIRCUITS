# GeckoCIRCUITS

A fast circuit simulator optimized for power electronics applications. Multi-domain simulation including thermo-electrical simulation, EMI filter design, and thermal design tools.

Originally written by Andreas Müsing, Andrija Stupar and Uwe Drofenik. Published under GPLv3.

## Quick Start with VSCode

### 1. Open Project in VSCode
```bash
code c:\Users\mhr\Documents\GeckoCIRCUITS
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
- **`Ctrl+Shift+B`** - Quick build (skips tests)
- **Terminal → Run Task → Build GeckoCIRCUITS** - Full build with tests
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

## Manual Build & Run (Command Line)

### Prerequisites
- **Java**: JDK 8 or newer (JDK 11+ recommended)
- **Maven**: 3.6+
- **Memory**: 3GB+ heap recommended for simulations

### Build
```bash
mvn clean package assembly:single
```

Creates `target/gecko-1.0-jar-with-dependencies.jar`

### Run
```bash
# Standard
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar

# With circuit file
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar resources/Education_ETHZ/ex_1.ipes

# HiDPI displays
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -Dsun.java2d.uiScale=2 -jar target/gecko-1.0-jar-with-dependencies.jar
```

### Convenience Scripts
- **`run.bat`** - Standard run
- **`run-hidpi.bat`** - HiDPI run
- **`build.bat`** - Full build with tests
- **`build-skip-tests.bat`** - Quick build without tests

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
mvn clean
mvn package assembly:single
```

### VSCode Java Extension Issues
- Press `Ctrl+Shift+P` → **"Java: Clean Java Language Server Workspace"**
- Reload window: `Ctrl+Shift+P` → **"Reload Window"**

### Tests
During build, 11 tests are skipped due to Netbeans-specific environment requirements. This is expected and the application will work correctly.

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
