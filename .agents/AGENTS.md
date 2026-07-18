# GeckoCIRCUITS Development Rules

## SpotBugs & Static Analysis
* **Suppressing Warnings in Anonymous Inner Classes:** Place `@SuppressFBWarnings` annotations directly on the inner method (e.g. `actionPerformed`) instead of the field declaring the class.
* **Static Inner Classes:** Keep inner classes `static` (complying with `SIC_INNER_SHOULD_BE_STATIC`) unless they explicitly require non-static access to outer class instance fields/methods.

## JNI Native Libraries
* **Library Unloading:** Call `System.gc()` before creating new class loaders for JNI dynamic modules to unload the previous native library instance.
* **Namespace Compilation:** Keep compiled native JNI binaries (`.dll`, `.so`, `.dylib`) updated to match the Java package name structure (e.g., `Java_gecko_geckocircuits...`).

## GUI Desktop Testing
* **Launcher Execution:** Always start GUI applications using the `PowerShell` tool's `Start-Process` command rather than direct background terminal execution.
* **JDWP Debugger Resume:** When debugging with `suspend=y`, send `F5` to the IDE window to resume execution before attempting to automate the GUI.

## Build, Test & Architecture Constraints
* **Multi-Module Structure:** The project consists of the main module `gecko` (`pom.xml`), the GUI-free simulation core `gecko-simulation-core` (`src/modules/gecko-simulation-core`), and the Spring Boot `gecko-rest-api` (`src/modules/gecko-rest-api`).
* **Running Tests:**
  - Run all module tests: `mvn -f pom-reactor.xml test`
  - Run main module tests only: `mvn test`
  - Run regression tests: `mvn test -Pregression` (always run and verify before major merges).
* **GUI-Free Simulation Core:** `gecko-simulation-core` must remain strictly GUI-free. `CorePackageValidationTest` automatically validates and enforces that no AWT, Swing, or other GUI-dependent packages are imported in this module.
* **UI Forms (.form):** Swing `.form` files must only be edited using the NetBeans GUI Designer. Do not manually edit the generated source blocks managed by NetBeans.
* **Circuit Files (.ipes):** `.ipes` files are gzip-compressed XML circuit definitions.

