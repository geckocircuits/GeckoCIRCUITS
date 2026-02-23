GeckoCIRCUITS - Power Electronics Circuit Simulator
===================================================
Version: ${project.version}
Platform: Linux

REQUIREMENTS
------------
- Java 21 or later
  Ubuntu/Debian: sudo apt install openjdk-21-jdk
  Fedora:        sudo dnf install java-21-openjdk
  Arch:          sudo pacman -S jdk21-openjdk

QUICK START
-----------
1. Make script executable (first time only):
   chmod +x run-gecko-linux.sh

2. Run GeckoCIRCUITS:
   ./run-gecko-linux.sh

3. For HiDPI/4K displays:
   ./run-gecko-linux.sh --hidpi

4. Open a circuit file:
   ./run-gecko-linux.sh path/to/circuit.ipes

COMMAND LINE OPTIONS
--------------------
  --hidpi     Enable HiDPI scaling for 4K displays
  --headless  Run with Xvfb (for testing/CI)
  -h, --help  Show help message

MANUAL START
------------
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar GeckoCIRCUITS.jar

MORE INFORMATION
----------------
- Documentation: https://github.com/geckocircuits/GeckoCIRCUITS
- Examples: Download the examples package separately
