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
package gecko.systemtests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.GeckoExternal;
import gecko.GeckoSim;

import java.io.File;

/**
 * Command-line utility to validate a single .ipes circuit file.
 *
 * This class loads a circuit file and verifies it can be parsed correctly.
 * By default, it only validates loading. Use --run-simulation to also run
 * a short simulation.
 *
 * Usage:
 *   java -cp gecko-1.0-jar-with-dependencies.jar \
 *        gecko.systemtests.CircuitValidator [--run-simulation] path/to/circuit.ipes
 *
 * Exit codes:
 *   0 - Success (circuit loaded and optionally simulated)
 *   1 - Failure (load or simulation error)
 *   2 - Usage error (no file specified or file not found)
 */
public final class CircuitValidator {
    private static final Logger LOGGER = LogManager.getLogger(CircuitValidator.class);


    private static final int MAX_WAIT_MS = 30000; // 30 seconds max wait for simulation
    private static final int POLL_INTERVAL_MS = 100;

    private CircuitValidator() {
        // Utility class - prevent instantiation
    }

    public static void main(String[] args) {
        boolean runSimulation = false;
        String filePath = null;

        for (String arg : args) {
            if ("--run-simulation".equals(arg)) {
                runSimulation = true;
            } else if (!arg.startsWith("--")) {
                filePath = arg;
            }
        }

        if (filePath == null) {
            LOGGER.error("Usage: CircuitValidator [--run-simulation] <circuit.ipes>");
            System.exit(2);
        }

        File file = new File(filePath);

        if (!file.exists()) {
            LOGGER.error("File not found: " + file.getAbsolutePath());
            System.exit(2);
        }

        if (!file.getName().endsWith(".ipes")) {
            LOGGER.error("Warning: File does not have .ipes extension: " + filePath);
        }

        int exitCode = validate(file, runSimulation);

        // Force exit since GUI threads may still be running
        Runtime.getRuntime().halt(exitCode);
    }

    /**
     * Validate a circuit file by loading it and optionally running a simulation.
     *
     * @param file The .ipes file to validate
     * @param runSimulation If true, also run a short simulation
     * @return 0 for success, 1 for failure
     */
    public static int validate(File file, boolean runSimulation) {
        try {
            // Initialize GeckoCIRCUITS in testing mode
            GeckoSim._isTestingMode = true;
            GeckoSim.main(new String[]{});

            // Wait for initialization
            Thread.sleep(500);

            LOGGER.info("Loading: " + file.getName());
            GeckoExternal.openFile(file.getAbsolutePath());

            // Wait for file to load
            Thread.sleep(500);

            // Verify circuit has elements (validates successful loading)
            String[] elements = GeckoExternal.getCircuitElements();
            if (elements == null || elements.length == 0) {
                LOGGER.error("FAIL: No circuit elements found after loading");
                return 1;
            }

            LOGGER.info("  - Loaded " + elements.length + " circuit elements");

            if (runSimulation) {
                // Initialize with short simulation time (1ms with 1us step)
                GeckoExternal.initSimulation(1e-6, 1e-3);

                LOGGER.info("Running simulation (1ms)...");
                GeckoExternal.runSimulation();

                // Wait for simulation to complete
                int waited = 0;
                while (GeckoSim._win != null && GeckoSim._win.isSimulationRunning() && waited < MAX_WAIT_MS) {
                    Thread.sleep(POLL_INTERVAL_MS);
                    waited += POLL_INTERVAL_MS;
                }

                if (waited >= MAX_WAIT_MS) {
                    LOGGER.error("FAIL: Simulation timeout after " + (MAX_WAIT_MS / 1000) + "s");
                    return 1;
                }

                // Small delay after simulation completes
                Thread.sleep(200);

                // Verify simulation ran
                double simTime = GeckoExternal.getSimulationTime();
                LOGGER.info("  - Simulation time: " + simTime + "s");
            }

            LOGGER.info("PASS: " + file.getName());
            return 0;

        } catch (Exception e) {
            LOGGER.error("FAIL: " + file.getName());
            LOGGER.error("  - Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace(System.err);
            return 1;
        }
    }
}
