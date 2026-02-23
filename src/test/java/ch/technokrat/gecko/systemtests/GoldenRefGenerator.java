/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.technokrat.gecko.systemtests;

import ch.technokrat.gecko.GeckoExternal;
import ch.technokrat.gecko.GeckoSim;

import java.io.*;
import java.util.*;

/**
 * Generates golden reference CSVs for regression testing.
 *
 * <p>Run this tool against a known-good build (e.g., v2.02) to capture
 * steady-state signal characteristics for each .ipes circuit file.
 * The resulting CSVs are used by {@link RegressionTest} to detect regressions.
 *
 * <p>Usage:
 * <pre>
 *   # From project root, with Xvfb running:
 *   export DISPLAY=:99
 *   java -cp target/gecko-1.0-jar-with-dependencies.jar \
 *        ch.technokrat.gecko.systemtests.GoldenRefGenerator \
 *        src/test/resources/ipes/education \
 *        src/test/resources/golden
 * </pre>
 */
public final class GoldenRefGenerator {

    private static final int MAX_WAIT_MS = 120_000; // 2 minutes
    private static final int POLL_INTERVAL_MS = 200;

    private GoldenRefGenerator() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: GoldenRefGenerator <ipes-dir> <output-dir>");
            System.exit(2);
        }

        File ipesDir = new File(args[0]);
        File outputDir = new File(args[1]);
        outputDir.mkdirs();

        File[] ipesFiles = ipesDir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) {
                return name.endsWith(".ipes");
            }
        });
        if (ipesFiles == null || ipesFiles.length == 0) {
            System.err.println("No .ipes files found in " + ipesDir);
            System.exit(1);
        }
        Arrays.sort(ipesFiles);

        System.out.println("Initializing GeckoSim in testing mode...");
        GeckoSim._isTestingMode = true;
        GeckoSim.main(new String[]{});

        // Wait for full initialization
        Thread.sleep(3000);
        System.out.println("GeckoSim initialized.");

        int success = 0;
        int failed = 0;
        List<String> failures = new ArrayList<String>();

        for (File ipesFile : ipesFiles) {
            System.out.println("\n=== Processing: " + ipesFile.getName() + " ===");
            try {
                processCircuit(ipesFile, outputDir);
                success++;
            } catch (Exception e) {
                System.err.println("FAILED: " + ipesFile.getName() + " - " + e.getMessage());
                e.printStackTrace(System.err);
                failures.add(ipesFile.getName() + ": " + e.getMessage());
                failed++;
            }
        }

        System.out.println("\n=== Summary ===");
        System.out.println("Success: " + success + "/" + ipesFiles.length);
        System.out.println("Failed: " + failed + "/" + ipesFiles.length);
        if (!failures.isEmpty()) {
            System.out.println("Failures:");
            for (String f : failures) {
                System.out.println("  - " + f);
            }
        }

        Runtime.getRuntime().halt(failed > 0 ? 1 : 0);
    }

    private static void processCircuit(File ipesFile, File outputDir) throws Exception {
        // Open the circuit file
        GeckoExternal.openFile(ipesFile.getAbsolutePath());
        Thread.sleep(500);

        // Get simulation parameters
        double tEnd = GeckoExternal.get_Tend();
        double dt = GeckoExternal.get_dt();
        System.out.println("  Simulation: tEnd=" + tEnd + "s, dt=" + dt + "s");

        // Run simulation
        GeckoExternal.runSimulation();

        // Wait for simulation to complete
        long startWait = System.currentTimeMillis();
        boolean completed = false;
        while (System.currentTimeMillis() - startWait < MAX_WAIT_MS) {
            Thread.sleep(POLL_INTERVAL_MS);
            try {
                double simTime = GeckoExternal.getSimulationTime();
                if (simTime >= tEnd * 0.99) {
                    completed = true;
                    break;
                }
            } catch (Exception e) {
                // Simulation might still be initializing
            }
        }

        if (!completed) {
            throw new RuntimeException("Simulation timeout after " + (MAX_WAIT_MS / 1000) + "s");
        }

        // Additional wait for results to settle
        Thread.sleep(500);
        System.out.println("  Simulation completed");

        // Steady-state window: last 20% of simulation time
        double startTime = tEnd * 0.8;
        double endTime = tEnd;

        // Discover SCOPE elements
        String[] controlElements = GeckoExternal.getControlElements();
        List<String> scopes = new ArrayList<String>();
        if (controlElements != null) {
            for (String elem : controlElements) {
                if (elem != null && elem.startsWith("SCOPE")) {
                    scopes.add(elem);
                }
            }
        }
        Collections.sort(scopes);
        System.out.println("  Found " + scopes.size() + " SCOPE elements: " + scopes);

        if (scopes.isEmpty()) {
            System.out.println("  WARNING: No SCOPE elements found");
        }

        // Generate golden reference CSV
        String baseName = ipesFile.getName().replace(".ipes", "");
        File csvFile = new File(outputDir, baseName + ".golden.csv");

        PrintWriter pw = new PrintWriter(new FileWriter(csvFile));
        try {
            pw.println("# Golden reference for " + ipesFile.getName());
            pw.println("# Generated from current build");
            pw.println("# Steady-state window: " + startTime + " to " + endTime + "s");
            pw.println("# tEnd=" + tEnd + " dt=" + dt);
            pw.println("scope_name,port,avg,rms,thd,min,max,ripple,klirr,shape,peak_peak");

            for (String scope : scopes) {
                // Try ports 0..7 (most scopes have 1-4 ports)
                for (int port = 0; port < 8; port++) {
                    try {
                        double[] chars = GeckoExternal.getSignalCharacteristics(scope, port, startTime, endTime);
                        if (chars != null && chars.length > 0) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(scope).append(",").append(port);
                            for (double v : chars) {
                                sb.append(",").append(String.format("%.10g", v));
                            }
                            pw.println(sb.toString());
                            System.out.println("  " + scope + " port " + port + ": avg=" +
                                    String.format("%.6g", chars[0]) + " rms=" + String.format("%.6g", chars[1]));
                        } else {
                            break; // No more ports
                        }
                    } catch (Exception e) {
                        // No more ports for this scope
                        if (port == 0) {
                            System.out.println("  WARNING: " + scope + " port 0 failed: " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        } finally {
            pw.close();
        }

        System.out.println("  Written: " + csvFile.getName());
    }
}
