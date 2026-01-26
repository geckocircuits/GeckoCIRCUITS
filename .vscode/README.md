# VSCode Configuration for GeckoCIRCUITS

This directory contains VSCode configuration files to build and run GeckoCIRCUITS directly from the IDE.

## Quick Start

### 1. Install Required Extensions

When you open this project, VSCode will prompt you to install recommended extensions. Click "Install All" or manually install:

- **Extension Pack for Java** (vscjava.vscode-java-pack)
- **Maven for Java** (vscjava.vscode-maven)
- **Debugger for Java** (vscjava.vscode-java-debug)

### 2. Build the Project

**Option A: Using Keyboard Shortcut**
- Press `Ctrl+Shift+B` (Windows/Linux) or `Cmd+Shift+B` (Mac)
- This runs the default build task

**Option B: Using Command Palette**
- Press `Ctrl+Shift+P` (Windows/Linux) or `Cmd+Shift+P` (Mac)
- Type: `Tasks: Run Build Task`
- Select: `Build GeckoCIRCUITS (Skip Tests)` for faster builds

**Option C: Using Terminal Menu**
- Terminal → Run Build Task...
- Choose your preferred build option

### 3. Run the Application

**Option A: Using Run Button**
- Click the "Run and Debug" icon in the sidebar (or press `Ctrl+Shift+D`)
- Select a configuration from the dropdown
- Click the green play button

**Option B: Using Keyboard Shortcut**
- Press `F5` to run with debugging
- Press `Ctrl+F5` to run without debugging

**Option C: Using Command Palette**
- Press `Ctrl+Shift+P`
- Type: `Debug: Select and Start Debugging`
- Choose your configuration

## Available Build Tasks

Access via: `Ctrl+Shift+P` → `Tasks: Run Task`

| Task Name | Description | Shortcut |
|-----------|-------------|----------|
| **Build GeckoCIRCUITS** | Full build with tests | `Ctrl+Shift+B` |
| **Build GeckoCIRCUITS (Skip Tests)** | Fast build without tests | - |
| **Clean Build Directory** | Remove target folder | - |
| **Run Tests** | Run JUnit tests only | - |
| **Maven: Update Dependencies** | Download/update dependencies | - |

## Available Run Configurations

Access via: Run and Debug sidebar (`Ctrl+Shift+D`)

### 1. Run GeckoCIRCUITS (Default)
- **What**: Standard run with auto-build
- **When to use**: Normal development workflow
- **Shortcut**: `F5` (debug) or `Ctrl+F5` (run)
- **Pre-build**: Yes (skip tests)

### 2. Run GeckoCIRCUITS (HiDPI)
- **What**: Run with HiDPI scaling for 4K monitors
- **When to use**: High-resolution displays
- **VM Args**: Includes `-Dsun.java2d.uiScale=2`

### 3. Run GeckoCIRCUITS (No Build)
- **What**: Run without building first
- **When to use**: Quick restart, no code changes
- **Pre-build**: No
- **Fastest startup**

### 4. Run GeckoCIRCUITS (Debug Mode)
- **What**: Run with debugger attached
- **When to use**: Debugging, setting breakpoints
- **Features**: Set breakpoints, inspect variables, step through code

### 5. Run GeckoCIRCUITS with File
- **What**: Opens currently active `.ipes` file
- **When to use**: Testing specific circuit files
- **How**: Open an `.ipes` file, then run this configuration

### 6. Run GeckoCIRCUITS (Buck Converter Example)
- **What**: Opens the buck converter example automatically
- **When to use**: Quick demo or testing
- **File**: `resources/Education_ETHZ/ex_1.ipes`

## Debugging

### Setting Breakpoints
1. Open a Java file (e.g., `GeckoSim.java`)
2. Click in the gutter (left of line numbers) to set a breakpoint (red dot)
3. Run with `F5` (or select "Run GeckoCIRCUITS (Debug Mode)")
4. When code hits the breakpoint, execution pauses

### Debug Controls
- **Continue** (`F5`): Resume execution
- **Step Over** (`F10`): Execute current line, move to next
- **Step Into** (`F11`): Enter function calls
- **Step Out** (`Shift+F11`): Exit current function
- **Restart** (`Ctrl+Shift+F5`): Restart debugging
- **Stop** (`Shift+F5`): Stop debugging

### Debug Console
- View variables, call stack, and watch expressions
- Evaluate expressions on-the-fly
- Access via Debug sidebar when debugging is active

## Maven Integration

### Maven Sidebar
- Click "Maven" in the sidebar (or View → Open View → Maven)
- Expand the project to see all Maven goals
- Right-click any goal to execute it

### Common Maven Commands
- **clean**: Remove target directory
- **compile**: Compile source code
- **test**: Run tests
- **package**: Create JAR files
- **install**: Install to local Maven repository

## Keyboard Shortcuts Reference

| Action | Windows/Linux | Mac |
|--------|---------------|-----|
| **Build** | `Ctrl+Shift+B` | `Cmd+Shift+B` |
| **Run (Debug)** | `F5` | `F5` |
| **Run (No Debug)** | `Ctrl+F5` | `Cmd+F5` |
| **Stop** | `Shift+F5` | `Shift+F5` |
| **Command Palette** | `Ctrl+Shift+P` | `Cmd+Shift+P` |
| **Open File** | `Ctrl+P` | `Cmd+P` |
| **Find in Files** | `Ctrl+Shift+F` | `Cmd+Shift+F` |
| **Toggle Terminal** | `` Ctrl+` `` | `` Cmd+` `` |

## Tips & Tricks

### Fast Development Workflow
1. Make code changes
2. Press `Ctrl+F5` - VSCode builds and runs automatically
3. Test your changes
4. Repeat

### Running Specific Circuit Files
1. Open any `.ipes` file from the `resources/` folder
2. Select "Run GeckoCIRCUITS with File" configuration
3. Press `F5`
4. The file opens in GeckoCIRCUITS

### Increasing Memory
Edit `.vscode/launch.json` and change `-Xmx3G` to `-Xmx4G` (or higher) in the `vmArgs` field.

### Terminal Integration
- All builds run in the integrated terminal
- Output is visible in the terminal panel
- Can run custom Maven commands: `` Ctrl+` `` then type `mvn <goal>`

## Troubleshooting

### "Java not found" error
- Install Java Extension Pack (see step 1 above)
- Reload VSCode window: `Ctrl+Shift+P` → `Reload Window`

### "Cannot resolve dependencies"
- Run task: `Maven: Update Dependencies`
- Or in terminal: `mvn dependency:resolve`

### Launch configuration not working
1. Ensure project is built: `Ctrl+Shift+B`
2. Check Java path in `settings.json`
3. Reload window: `Ctrl+Shift+P` → `Reload Window`

### Build fails
- Clean build: Run task `Clean Build Directory`
- Then rebuild: `Ctrl+Shift+B`

## Customization

### Changing Java Home
Edit `.vscode/settings.json`:
```json
"java.configuration.runtimes": [
    {
        "name": "JavaSE-1.8",
        "path": "YOUR_JAVA_PATH_HERE",
        "default": true
    }
]
```

### Adding Custom Run Configurations
Edit `.vscode/launch.json` and add new configuration blocks. Copy an existing one and modify the `name`, `args`, or `vmArgs`.

### Adding Custom Build Tasks
Edit `.vscode/tasks.json` and add new task blocks with custom Maven goals.

## Files in This Directory

- **tasks.json**: Build task definitions
- **launch.json**: Run/debug configurations
- **settings.json**: Java and Maven settings
- **extensions.json**: Recommended extensions
- **README.md**: This file

## Need More Help?

- See `QUICKSTART.md` in project root for general usage
- See `SETUP.md` for installation details
- See `CLAUDE.md` for architecture overview
