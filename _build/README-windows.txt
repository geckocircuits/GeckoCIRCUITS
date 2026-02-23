GeckoCIRCUITS - Power Electronics Circuit Simulator
===================================================
Version: ${project.version}
Platform: Windows

REQUIREMENTS
------------
- Java 21 or later (https://adoptium.net/)

QUICK START
-----------
1. Double-click run-gecko.bat
   Or from command prompt:
   run-gecko.bat

2. For HiDPI/4K displays:
   run-gecko.bat --hidpi

3. Open a circuit file:
   run-gecko.bat path\to\circuit.ipes

COMMAND LINE OPTIONS
--------------------
  --hidpi     Enable HiDPI scaling for 4K displays
  -h, --help  Show help message

MANUAL START
------------
java -Xmx3G -Dpolyglot.js.nashorn-compat=true -jar GeckoCIRCUITS.jar

MORE INFORMATION
----------------
- Documentation: https://github.com/geckocircuits/GeckoCIRCUITS
- Examples: Download the examples package separately
