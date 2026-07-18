/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under
 *  the terms of the GNU General Public License as published by the Free Software
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */
package gecko.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import gecko.core.allg.SolverSettingsCore;
import gecko.core.allg.SolverType;
import gecko.core.simulation.HeadlessSimulationEngine;
import gecko.core.simulation.SimulationConfig;
import gecko.core.simulation.SimulationResult;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Headless entry point for GeckoCIRCUITS simulation engine.
 * Enables running simulations from command line without GUI.
 *
 * <p>Usage:</p>
 * <pre>
 * java -jar gecko-headless.jar --circuit input.ipes --output results.csv
 * java -jar gecko-headless.jar --circuit input.ipes --dt 1e-6 --duration 20e-3 --output results.csv
 * </pre>
 *
 * <p>Options:</p>
 * <ul>
 *   <li>--circuit, -c: Path to circuit file (.ipes)</li>
 *   <li>--output, -o: Path to output file (CSV format)</li>
 *   <li>--dt: Simulation time step (seconds), default: 1e-6</li>
 *   <li>--duration, -d: Simulation duration (seconds), default: 20e-3</li>
 *   <li>--solver: Solver type (be, trz, gs), default: be</li>
 *   <li>--quiet, -q: Suppress progress output</li>
 *   <li>--help, -h: Show help message</li>
 * </ul>
 */
public class GeckoHeadless {
    private static final Logger LOGGER = LogManager.getLogger(GeckoHeadless.class);


    public static final String VERSION = "1.0.0";

    private String circuitFile;
    private String outputFile;
    private double dt = 1e-6;
    private double duration = 20e-3;
    private SolverType solverType = SolverType.SOLVER_BE;
    private boolean quiet = false;

    /**
     * Main entry point for headless operation.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        GeckoHeadless app = new GeckoHeadless();
        int exitCode = app.run(args);
        System.exit(exitCode);
    }

    /**
     * Runs the headless simulation with the given arguments.
     *
     * @param args command line arguments
     * @return exit code (0 for success, non-zero for error)
     */
    public int run(String[] args) {
        try {
            if (!parseArguments(args)) {
                return 1;
            }

            if (circuitFile == null) {
                printError("Circuit file is required. Use --circuit <file>");
                return 1;
            }

            return runSimulation();

        } catch (Exception e) {
            printError("Error: " + e.getMessage());
            return 2;
        }
    }

    /**
     * Parses command line arguments.
     *
     * @param args arguments to parse
     * @return true if parsing successful, false if should exit
     */
    private boolean parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--help":
                case "-h":
                    printHelp();
                    return false;

                case "--version":
                case "-v":
                    LOGGER.info("GeckoCIRCUITS Headless v" + VERSION);
                    return false;

                case "--circuit":
                case "-c":
                    if (i + 1 >= args.length) {
                        printError("--circuit requires a file path");
                        return false;
                    }
                    circuitFile = args[++i];
                    break;

                case "--output":
                case "-o":
                    if (i + 1 >= args.length) {
                        printError("--output requires a file path");
                        return false;
                    }
                    outputFile = args[++i];
                    break;

                case "--dt":
                    if (i + 1 >= args.length) {
                        printError("--dt requires a value");
                        return false;
                    }
                    dt = parseDouble(args[++i], "dt");
                    break;

                case "--duration":
                case "-d":
                    if (i + 1 >= args.length) {
                        printError("--duration requires a value");
                        return false;
                    }
                    duration = parseDouble(args[++i], "duration");
                    break;

                case "--solver":
                    if (i + 1 >= args.length) {
                        printError("--solver requires a value (be, trz, gs)");
                        return false;
                    }
                    solverType = parseSolverType(args[++i]);
                    break;

                case "--quiet":
                case "-q":
                    quiet = true;
                    break;

                default:
                    if (arg.startsWith("-")) {
                        printError("Unknown option: " + arg);
                        return false;
                    }
                    // Treat as circuit file if no --circuit provided
                    if (circuitFile == null) {
                        circuitFile = arg;
                    }
            }
        }
        return true;
    }

    /**
     * Runs the simulation with parsed parameters.
     *
     * @return exit code
     */
    private int runSimulation() {
        if (!quiet) {
            LOGGER.info("GeckoCIRCUITS Headless v" + VERSION);
            LOGGER.info("Circuit: " + circuitFile);
            LOGGER.info("Solver: " + solverType);
            LOGGER.info("dt: " + dt + " s");
            LOGGER.info("Duration: " + duration + " s");
            LOGGER.info("");
        }

        // Create simulation configuration
        SimulationConfig config = SimulationConfig.builder()
                .circuitFile(circuitFile)
                .solverType(solverType)
                .stepWidth(dt)
                .simulationDuration(duration)
                .build();

        // Create and configure engine
        HeadlessSimulationEngine engine = new HeadlessSimulationEngine();

        if (!quiet) {
            engine.setProgressListener((currentTime, endTime, currentStep) -> {
                double progress = currentTime / endTime * 100;
                System.out.printf("\rProgress: %.1f%% (t=%.4f s, step=%d)", progress, currentTime, currentStep);
            });
        }

        // Run simulation
        if (!quiet) {
            LOGGER.info("Starting simulation...");
        }

        SimulationResult result = engine.runSimulation(config);

        if (!quiet) {
            LOGGER.info("");
            LOGGER.info("");
        }

        // Check result
        if (!result.isSuccess()) {
            printError("Simulation failed: " + result.getErrorMessage());
            return 3;
        }

        if (!quiet) {
            LOGGER.info("Simulation completed successfully!");
            System.out.printf("  Simulated time: %.4f s%n", result.getSimulatedTime());
            System.out.printf("  Time steps: %d%n", result.getTotalTimeSteps());
            System.out.printf("  Wall clock: %d ms%n", result.getExecutionTimeMs());
        }

        // Export results if output file specified
        if (outputFile != null) {
            try {
                exportToCsv(result, outputFile);
                if (!quiet) {
                    LOGGER.info("  Results exported to: " + outputFile);
                }
            } catch (IOException e) {
                printError("Failed to write output file: " + e.getMessage());
                return 4;
            }
        }

        return 0;
    }

    /**
     * Exports simulation results to a CSV file.
     *
     * @param result the simulation result
     * @param filename the output filename
     * @throws IOException if writing fails
     */
    private void exportToCsv(SimulationResult result, String filename) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header row
            String[] signalNames = result.getSignalNames();
            writer.print("time");
            for (String name : signalNames) {
                writer.print("," + name);
            }
            writer.println();

            // Data rows
            double[] times = result.getTimeArray();
            float[][] signalData = new float[signalNames.length][];
            for (int i = 0; i < signalNames.length; i++) {
                signalData[i] = result.getSignalData(i);
            }

            for (int t = 0; t < times.length; t++) {
                writer.print(times[t]);
                for (int s = 0; s < signalNames.length; s++) {
                    writer.print(",");
                    if (signalData[s] != null && t < signalData[s].length) {
                        writer.print(signalData[s][t]);
                    }
                }
                writer.println();
            }
        }
    }

    /**
     * Parses a solver type string.
     *
     * @param value the string value
     * @return solver type
     */
    private SolverType parseSolverType(String value) {
        switch (value.toLowerCase()) {
            case "be":
            case "backward-euler":
                return SolverType.SOLVER_BE;
            case "trz":
            case "trapezoidal":
                return SolverType.SOLVER_TRZ;
            case "gs":
            case "gear-shichman":
                return SolverType.SOLVER_GS;
            default:
                printError("Unknown solver type: " + value + ". Using backward-euler.");
                return SolverType.SOLVER_BE;
        }
    }

    /**
     * Parses a double value with error handling.
     *
     * @param value the string value
     * @param name the parameter name (for error messages)
     * @return parsed double
     */
    private double parseDouble(String value, String name) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid " + name + " value: " + value);
        }
    }

    /**
     * Prints an error message to stderr.
     *
     * @param message the error message
     */
    private void printError(String message) {
        LOGGER.error("Error: " + message);
    }

    /**
     * Prints the help message.
     */
    private void printHelp() {
        LOGGER.info("GeckoCIRCUITS Headless Simulator v" + VERSION);
        LOGGER.info("");
        LOGGER.info("Usage: java -jar gecko-headless.jar [options] [circuit-file]");
        LOGGER.info("");
        LOGGER.info("Options:");
        LOGGER.info("  --circuit, -c <file>   Path to circuit file (.ipes)");
        LOGGER.info("  --output, -o <file>    Path to output file (CSV format)");
        LOGGER.info("  --dt <value>           Simulation time step in seconds (default: 1e-6)");
        LOGGER.info("  --duration, -d <value> Simulation duration in seconds (default: 20e-3)");
        LOGGER.info("  --solver <type>        Solver type: be (backward-euler), trz (trapezoidal),");
        LOGGER.info("                         gs (gear-shichman). Default: be");
        LOGGER.info("  --quiet, -q            Suppress progress output");
        LOGGER.info("  --help, -h             Show this help message");
        LOGGER.info("  --version, -v          Show version information");
        LOGGER.info("");
        LOGGER.info("Examples:");
        LOGGER.info("  java -jar gecko-headless.jar --circuit buck.ipes --output results.csv");
        LOGGER.info("  java -jar gecko-headless.jar -c buck.ipes -d 50e-3 --dt 0.5e-6 -o out.csv");
        LOGGER.info("");
        LOGGER.info("For more information, visit: https://github.com/geckocircuits");
    }
}
