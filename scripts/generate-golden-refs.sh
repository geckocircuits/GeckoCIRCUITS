#!/usr/bin/env bash
# Generate golden reference CSVs from v2.02 baseline for regression testing.
#
# Prerequisites:
#   - JDK 21 installed
#   - X display available (Xvfb or desktop)
#   - Repository with v2.02 tag
#
# Usage:
#   # On a machine with working desktop (NOT WSL2, v2.02 GUI doesn't start in WSL2):
#   ./scripts/generate-golden-refs.sh
#
#   # With Xvfb:
#   export DISPLAY=:99
#   Xvfb :99 -screen 0 1024x768x24 &
#   ./scripts/generate-golden-refs.sh
#
# Output:
#   src/test/resources/golden/*.golden.csv

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
WORKTREE_DIR="$PROJECT_ROOT/../gecko-v2.02-golden"
IPES_DIR="$PROJECT_ROOT/src/test/resources/ipes/education"
GOLDEN_DIR="$PROJECT_ROOT/src/test/resources/golden"

echo "=== GeckoCIRCUITS Golden Reference Generator ==="
echo "Project root: $PROJECT_ROOT"
echo "IPES files:   $IPES_DIR"
echo "Output:       $GOLDEN_DIR"

# Verify display is available
if [ -z "${DISPLAY:-}" ]; then
    echo "ERROR: No DISPLAY set. Start Xvfb or use a desktop session."
    echo "  Example: export DISPLAY=:99 && Xvfb :99 -screen 0 1024x768x24 &"
    exit 1
fi

# Create v2.02 worktree if it doesn't exist
if [ ! -d "$WORKTREE_DIR" ]; then
    echo "Creating v2.02 worktree..."
    git -C "$PROJECT_ROOT" worktree add "$WORKTREE_DIR" v2.02
fi

# Build v2.02
echo "Building v2.02..."
cd "$WORKTREE_DIR"
mvn clean package assembly:single -DskipTests --no-transfer-progress

V202_JAR="$WORKTREE_DIR/target/gecko-1.0-jar-with-dependencies.jar"
if [ ! -f "$V202_JAR" ]; then
    echo "ERROR: v2.02 JAR not found at $V202_JAR"
    exit 1
fi

# Build current dev to get test classes (GoldenRefGenerator)
echo "Building current dev test classes..."
cd "$PROJECT_ROOT"
mvn compile test-compile --no-transfer-progress

# Run generator
# Note: The generator class is compiled from current dev, but we need to use it
# against the v2.02 JAR. Since the package structure differs (ch.technokrat.gecko vs gecko),
# we use the current dev generator which works with the current package structure.
#
# For v2.02: Use the standalone GoldenRefGenerator.java in the worktree directory.
echo "Running golden reference generator..."
mkdir -p "$GOLDEN_DIR"

# Copy generator source to worktree and compile against v2.02
cat > "$WORKTREE_DIR/GoldenRefGenerator.java" << 'JAVAEOF'
import ch.technokrat.gecko.GeckoExternal;
import ch.technokrat.gecko.GeckoSim;
import java.io.*;
import java.util.*;

public class GoldenRefGenerator {
    private static final int MAX_WAIT_MS = 120000;
    private static final int POLL_INTERVAL_MS = 200;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) { System.err.println("Usage: GoldenRefGenerator <ipes-dir> <output-dir>"); System.exit(2); }
        File ipesDir = new File(args[0]);
        File outputDir = new File(args[1]);
        outputDir.mkdirs();
        File[] ipesFiles = ipesDir.listFiles((d, name) -> name.endsWith(".ipes"));
        if (ipesFiles == null || ipesFiles.length == 0) { System.err.println("No .ipes files"); System.exit(1); }
        Arrays.sort(ipesFiles);

        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[]{});
        Thread.sleep(5000);

        int ok = 0, fail = 0;
        for (File f : ipesFiles) {
            System.out.println("\n=== " + f.getName() + " ===");
            try { process(f, outputDir); ok++; }
            catch (Exception e) { System.err.println("FAIL: " + e.getMessage()); fail++; }
        }
        System.out.println("\nDone: " + ok + " ok, " + fail + " failed");
        Runtime.getRuntime().halt(fail > 0 ? 1 : 0);
    }

    static void process(File ipesFile, File outputDir) throws Exception {
        GeckoExternal.openFile(ipesFile.getAbsolutePath());
        Thread.sleep(500);
        double tEnd = GeckoExternal.get_Tend();
        double dt = GeckoExternal.get_dt();
        GeckoExternal.runSimulation();
        long t0 = System.currentTimeMillis();
        boolean done = false;
        while (System.currentTimeMillis() - t0 < MAX_WAIT_MS) {
            Thread.sleep(POLL_INTERVAL_MS);
            try { if (GeckoExternal.getSimulationTime() >= tEnd * 0.99) { done = true; break; } } catch (Exception e) {}
        }
        if (!done) throw new RuntimeException("Timeout");
        Thread.sleep(500);
        double startTime = tEnd * 0.8, endTime = tEnd;
        String[] elems = GeckoExternal.getControlElements();
        List<String> scopes = new ArrayList<>();
        if (elems != null) for (String e : elems) if (e != null && e.startsWith("SCOPE")) scopes.add(e);
        Collections.sort(scopes);
        String base = ipesFile.getName().replace(".ipes", "");
        try (PrintWriter pw = new PrintWriter(new FileWriter(new File(outputDir, base + ".golden.csv")))) {
            pw.println("# Golden reference for " + ipesFile.getName());
            pw.println("# Generated from v2.02 (commit b34b9b9d)");
            pw.println("# Steady-state window: " + startTime + " to " + endTime + "s");
            pw.println("# tEnd=" + tEnd + " dt=" + dt);
            pw.println("scope_name,port,avg,rms,min,max,ripple,thd,shape,klirr");
            for (String scope : scopes) {
                for (int port = 0; port < 8; port++) {
                    try {
                        double[] c = GeckoExternal.getSignalCharacteristics(scope, port, startTime, endTime);
                        if (c != null && c.length > 0) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(scope).append(",").append(port);
                            for (double v : c) sb.append(",").append(String.format("%.10g", v));
                            pw.println(sb.toString());
                        } else break;
                    } catch (Exception e) { break; }
                }
            }
        }
        System.out.println("  Written: " + base + ".golden.csv");
    }
}
JAVAEOF

cd "$WORKTREE_DIR"
javac -cp "$V202_JAR" GoldenRefGenerator.java
java -cp "$V202_JAR:." GoldenRefGenerator "$IPES_DIR" "$GOLDEN_DIR"

echo ""
echo "=== Golden references generated ==="
echo "Files in $GOLDEN_DIR:"
ls -la "$GOLDEN_DIR"/*.golden.csv 2>/dev/null || echo "(none)"

# Cleanup worktree
echo ""
echo "To clean up the worktree:"
echo "  git worktree remove $WORKTREE_DIR"
