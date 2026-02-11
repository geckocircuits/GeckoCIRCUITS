GeckoCIRCUITS - Power Electronics Circuit Simulator
===================================================
Version: ${project.version}
Platform: Windows Subsystem for Linux (WSL)

REQUIREMENTS
------------
- WSL2 with Ubuntu 22.04 or 24.04
- WSLg (Windows 11) or X server on Windows
- Java 21 or later

FIRST TIME SETUP
----------------
Run the included setup script to install all dependencies:
  chmod +x setup-wsl.sh
  ./setup-wsl.sh

This installs Java 21, X11 libraries, and Xvfb.

QUICK START
-----------
1. Make script executable (first time only):
   chmod +x run-gecko-wsl.sh

2. Run GeckoCIRCUITS:
   ./run-gecko-wsl.sh

3. For HiDPI/4K displays:
   ./run-gecko-wsl.sh --hidpi

4. Open a circuit file:
   ./run-gecko-wsl.sh path/to/circuit.ipes

COMMAND LINE OPTIONS
--------------------
  --hidpi     Enable HiDPI scaling for 4K displays
  --headless  Run with Xvfb (for testing/CI)
  -h, --help  Show help message

DISPLAY CONFIGURATION
---------------------
- WSLg (Windows 11): Automatic, uses DISPLAY=:0
- X server on Windows: Set DISPLAY=<Windows-IP>:0

MANUAL START
------------
DISPLAY=:0 java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar GeckoCIRCUITS.jar

MORE INFORMATION
----------------
- Documentation: https://github.com/geckocircuits/GeckoCIRCUITS
- Examples: Download the examples package separately
