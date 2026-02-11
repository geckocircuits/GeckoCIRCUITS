# GeckoCIRCUITS

A fast circuit simulator optimized for power electronics applications. Multi-domain simulation including thermo-electrical simulation, EMI filter design, and thermal design tools. The application can run standalone, as an applet, or integrate with external tools like MATLAB/Simulink.

## Table of Contents

- [Quick Start](#quick-start) - Get up and running in 5 minutes
- [Getting Help](#getting-help) - Where to find information and support
- [GUI Development](#gui-development) - Using NetBeans GUI Designer
- [Architecture Overview](#architecture-overview) - System structure
- [Project Structure](#project-structure) - Code organization
- [Testing](#testing) - Running and writing tests
- [Examples & Resources](#examples--resources) - Circuit examples and tutorials

---

## Quick Start

### New Developer? Start Here!

**Prerequisites:**
1. **Java JDK 21** - Required for this project
2. **Maven 3.6+** - Build tool (project uses Maven)
3. **NetBeans IDE** - For GUI design with .form files (recommended)
   - Download: https://www.netbeans.org/downloads/
   - Alternative: Use VSCode for code editing

**Setup Steps (15 minutes):**

**Step 1: Set JAVA_HOME (Windows - Optional but Recommended)**
```
1. Press Win+R, type: sysdm.cpl
2. Go to "Advanced" → "Environment Variables"
3. Add new variable:
   - Variable name: JAVA_HOME
   - Variable value: C:\Program Files\Java\jdk-21 (adjust to your path)
4. Click OK
5. Restart VSCode (if open)
```

**Step 2: Install Maven (Windows - Optional)**
```
1. Download Maven: https://maven.apache.org/download.cgi
2. Extract to folder (e.g., C:\Program Files\Apache Maven)
3. Add Maven to PATH: System Properties → Environment Variables → Path → Edit
4. Verify: Open command prompt, run: `mvn -version`
```

**Step 3: Build Project**
```bash
# In this directory (C:\Users\mhr\Documents\GeckoCIRCUITS)
mvn clean package assembly:single -DskipTests
```

**Step 4: Run Application**
```bash
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar target/gecko-1.0-jar-with-dependencies.jar
```

---

## Getting Help

### Documentation
- **[NETBEANS_GUI_DESIGNER.md](NETBEANS_GUI_DESIGNER.md)** - Using NetBeans GUI Designer for Swing dialogs
- **[OPCODE_GUIDE.md](OPCODE_GUIDE.md)** - Complete development guide (build, run, debug, architecture)
- **[REMOTE_CONTROL_MANUAL.md](REMOTE_CONTROL_MANUAL.md)** - Remote control and automation

### Troubleshooting
- **Build fails**: Check Java version (requires JDK 21) and Maven is installed
- **Out of memory**: Increase heap size: change `-Xmx3G` to `-Xmx4G` or higher
- **GUI issues**: Use NetBeans to open .form files (see NETBEANS_GUI_DESIGNER.md)
- **Tests fail**: Ensure you've run `mvn clean package assembly:single` first

---

## GUI Development

### Using NetBeans GUI Designer

**Overview:**
- NetBeans includes **GUI Designer** (also called WindowBuilder)
- Provides **WYSIWYG editor** for Swing .form files
- **No migration needed** - works with existing .form files directly
- **Auto-generates code** - NetBeans writes UI code for you
- **Zero setup time** - Already installed with NetBeans

**Quick Start (Today):**
1. **Open NetBeans**: Run `"C:\Program Files\Apache NetBeans\bin\netbeans64.exe"` (or your NetBeans installation)
2. **Open Project**: File → Open Project → Navigate to: `C:\Users\mhr\Documents\GeckoCIRCUITS` → Select `pom.xml` → Click "Open Project"
3. **Open .form file**: Shift+double-click on `DialogSheetSize.form` (in Projects panel)
4. **Edit GUI**: Drag components from Palette, set properties, save (Ctrl+S)
5. **Test**: Run application to see changes

**Daily Workflow:**
```
Edit Dialog GUI:
1. Shift+double-click DialogSheetSize.form → Opens in [Design] tab
2. Drag components, set properties in Properties window
3. Save (Ctrl+S)
4. Run application (F6 from NetBeans OR `java -jar target/...jar`)

Edit Business Logic:
1. Click [Source] tab in NetBeans
2. Edit Java code
3. Save (Ctrl+S)
```

**For detailed guide:** See [NETBEANS_GUI_DESIGNER.md](NETBEANS_GUI_DESIGNER.md)

---

## Architecture Overview

### Main Entry Point
- **GeckoSim.java** (`ch.technokrat.gecko.GeckoSim`): Main class that initializes the application
  - Handles different operating modes: STANDALONE, REMOTE, MMF (Memory-Mapped File), SIMULINK, EXTERNAL
  - Manages memory settings and JVM restart if more memory is required
  - Initializes main window (`Fenster`)

### Key Package Structure

- **ch.technokrat.gecko**: Core application classes
  - Remote interface classes (`GeckoRemote*`) for external tool integration
  - Memory-mapped file support (`GeckoMemoryMappedFile`, `GeckoCustomMMF`)
  - Control calculation interfaces

- **ch.technokrat.gecko.geckocircuits**: Main simulation components
  - `allg/`: General classes (dialogs, file management, global settings)
  - `circuit/`: Circuit sheet, components, terminals, couplings
  - `control/`: Control blocks and systems
  - `datacontainer/`: Data storage and management
  - `math/`: Mathematical utilities
  - `newscope/` & `scope/`: Oscilloscope/visualization
  - `nativec/`: Native C integration

- **ch.technokrat.gecko.geckoscript**: Scripting support

- **ch.technokrat.gecko.i18n**: Internationalization
  - `I18nKeys.java` - 968+ translation keys
  - `GuiFabric.java` - Factory for localized components
  - `EnglishMapper.java` - English translations

- **ch.technokrat.expressionscripting**: Expression and JavaScript support

### Operating Modes

The simulator supports multiple operating modes defined in `OperatingMode`:
- **STANDALONE**: Normal desktop application
- **REMOTE**: Remote access via RMI (for MATLAB integration)
- **MMF**: Memory-mapped file communication
- **SIMULINK**: Integration with MATLAB Simulink
- **EXTERNAL**: External tool integration

### External Integration

GeckoCIRCUITS can be controlled from external tools (particularly MATLAB/Simulink):
- **Remote interface**: Uses Java RMI for method calls over network
- **Memory-mapped files**: Shared memory communication for high-performance data exchange
- See `GeckoRemoteRegistry`, `GeckoCustomRemote`, `GeckoCustomMMF` classes

---

## Project Structure

### Main Directories

- **`src/main/java/`**: Java source code
- **`src/test/java/`**: Test code
- **`resources/`**: Example circuits and tutorials
- **`target/`**: Build output

### Key Packages

**ch.technokrat.gecko**: Core application classes

**ch.technokrat.gecko.geckocircuits**: Main simulation components
- **`allg/`**: General classes (dialogs, file management, global settings)
- **`circuit/`**: Circuit sheet, components, terminals, couplings
- **`control/`**: Control blocks and systems
- **`datacontainer/`**: Data storage and management
- **`math/`**: Mathematical utilities
- **`newscope/`** & `scope/`: Oscilloscope/visualization
- **`nativec/`**: Native C integration

**ch.technokrat.gecko.geckoscript**: Scripting support

**ch.technokrat.gecko.i18n**: Internationalization

---

## Testing

### Running Tests
```bash
mvn test
```

### Test Status
- All 159 tests now pass successfully (0 failures, 0 skipped).
- Test mode controlled by `GeckoSim._isTestingMode` flag

### Test Categories

**Unit tests**: Calculator and component tests

**Integration tests**: ModelResultsTest - loads and simulates real circuit files

**API tests**: GeckoRemoteTest - verifies GeckoRemote API consistency

---

## Examples & Resources

### Circuit Examples

The `resources/` directory contains extensive examples:

- **DC/DC converters**: Buck, Boost, Buck-Boost, Cuk, SEPIC, Flyback
- **AC/DC rectifiers**: Vienna, Swiss, three-phase VSR, PFC
- **DC/AC inverters**: Single-phase, three-phase
- **AC/AC converters**: Matrix converters

Located in subdirectories like `Education_ETHZ/`, `Topologies/`, `education_www.ipes.ethz.ch/`

---

## Getting Started as New Developer

### Recommended Learning Path

**Week 1: Setup & Orientation**
1. Build and run the application
2. Open a few example circuits (`resources/Education_ETHZ/`)
3. Explore the GUI and dialogs
4. Understand the .form file structure (open in NetBeans GUI Designer)

**Week 2: Simple Modifications**
1. Use NetBeans GUI Designer to modify a dialog's layout
2. Add a simple component (button, label)
3. Test your changes

**Week 3+: Contribute**
1. Fix small bugs
2. Add new features
3. Improve documentation

---

## Original Sourceforge
- **Original Sourceforge**: https://sourceforge.net/projects/geckocircuits/
- **Contributions from**: [Technokrat](https://github.com/technokrat/gecko) (HiDPI support and improvements)

---

## License

- **GPLv3** for open source use
- **Commercial license** available from Gecko-Research GmbH
- See [LICENSE](LICENSE) file or visit http://www.gnu.org/licenses/

---

## Contact & Resources

- **Website**: www.gecko-simulations.com
- **Documentation**: See [OPCODE_GUIDE.md](OPCODE_GUIDE.md) for complete development guide
- **GUI Design**: See [NETBEANS_GUI_DESIGNER.md](NETBEANS_GUI_DESIGNER.md) for NetBeans GUI Designer guide