GeckoCIRCUITS - Power Electronics Circuit Simulator
===================================================
Version: ${project.version}
Platform: macOS

REQUIREMENTS
------------
- Java 21 or later
  Install with Homebrew: brew install openjdk@21
  Or download from: https://adoptium.net/

QUICK START
-----------
1. Make script executable (first time only):
   chmod +x run-gecko-macos.sh

2. Run GeckoCIRCUITS:
   ./run-gecko-macos.sh

3. For HiDPI/Retina displays:
   ./run-gecko-macos.sh --hidpi

4. Open a circuit file:
   ./run-gecko-macos.sh path/to/circuit.ipes

COMMAND LINE OPTIONS
--------------------
  --hidpi     Enable HiDPI scaling for Retina displays
  -h, --help  Show help message

MANUAL START
------------
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar GeckoCIRCUITS.jar

GATEKEEPER NOTE
---------------
If macOS blocks the app, go to System Preferences > Security & Privacy
and click "Open Anyway".

MORE INFORMATION
----------------
- Documentation: https://github.com/geckocircuits/GeckoCIRCUITS
- Examples: Download the examples package separately
