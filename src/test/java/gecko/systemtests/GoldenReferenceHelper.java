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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Utility for reading and writing golden reference CSV files used by regression tests.
 *
 * Golden CSV format:
 * <pre>
 * # Comment lines start with #
 * scope_name,port,avg,rms,min,max,ripple,thd,shape,klirr
 * SCOPE.1,0,19.2000,19.2010,...
 * </pre>
 *
 * Each row represents one scope port's signal characteristics over the steady-state window.
 */
public final class GoldenReferenceHelper {

    private GoldenReferenceHelper() {
        // Utility class
    }

    /**
     * A single golden reference measurement for one scope port.
     */
    public static class GoldenEntry {
        public final String scopeName;
        public final int port;
        public final double[] characteristics; // [avg, rms, min, max, ripple, thd, shape, klirr]

        public GoldenEntry(String scopeName, int port, double[] characteristics) {
            this.scopeName = scopeName;
            this.port = port;
            this.characteristics = characteristics;
        }

        /** Characteristic names matching the CSV column order. */
        public static final String[] CHARACTERISTIC_NAMES = {
            "avg", "rms", "min", "max", "ripple", "thd", "shape", "klirr"
        };
    }

    /**
     * Reads golden reference entries from a CSV file.
     *
     * @param csvPath path to the golden CSV file
     * @return list of golden entries
     * @throws IOException if the file cannot be read
     */
    public static List<GoldenEntry> readGoldenCsv(Path csvPath) throws IOException {
        List<GoldenEntry> entries = new ArrayList<>();
        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            // Header line
            if (line.startsWith("scope_name,")) {
                continue;
            }

            String[] parts = line.split(",");
            if (parts.length < 3) {
                continue;
            }

            String scopeName = parts[0].trim();
            int port = Integer.parseInt(parts[1].trim());
            double[] chars = new double[parts.length - 2];
            for (int i = 2; i < parts.length; i++) {
                chars[i - 2] = Double.parseDouble(parts[i].trim());
            }

            entries.add(new GoldenEntry(scopeName, port, chars));
        }

        return entries;
    }

    /**
     * Extracts the steady-state time window (last 20%) metadata from the golden CSV header.
     *
     * @param csvPath path to the golden CSV file
     * @return double[2] = {tEnd, dt} parsed from header comments, or null if not found
     * @throws IOException if the file cannot be read
     */
    public static double[] readSimulationParams(Path csvPath) throws IOException {
        List<String> lines = Files.readAllLines(csvPath, StandardCharsets.UTF_8);
        for (String line : lines) {
            // Look for: # tEnd=0.01 dt=1e-6
            if (line.startsWith("# tEnd=")) {
                String params = line.substring(2).trim();
                String[] parts = params.split("\\s+");
                double tEnd = 0;
                double dt = 0;
                for (String part : parts) {
                    if (part.startsWith("tEnd=")) {
                        tEnd = Double.parseDouble(part.substring(5));
                    } else if (part.startsWith("dt=")) {
                        dt = Double.parseDouble(part.substring(3));
                    }
                }
                if (tEnd > 0) {
                    return new double[]{tEnd, dt};
                }
            }
        }
        return null;
    }

    /**
     * Checks if two values are within the specified relative tolerance.
     *
     * @param expected the golden reference value
     * @param actual the current simulation value
     * @param relativeTolerance e.g. 0.01 for 1%
     * @param absoluteFloor minimum absolute difference to tolerate (for near-zero values)
     * @return true if within tolerance
     */
    public static boolean withinTolerance(double expected, double actual,
                                          double relativeTolerance, double absoluteFloor) {
        double absDiff = Math.abs(expected - actual);

        // For near-zero expected values, use absolute floor
        if (Math.abs(expected) < absoluteFloor) {
            return absDiff < absoluteFloor;
        }

        double relDiff = absDiff / Math.abs(expected);
        return relDiff <= relativeTolerance;
    }

    /**
     * Formats a tolerance comparison failure message.
     */
    public static String formatMismatch(String scopeName, int port, String charName,
                                        double expected, double actual,
                                        double relativeTolerance) {
        double relDiff = Math.abs(expected) > 1e-15
                ? Math.abs(expected - actual) / Math.abs(expected) * 100.0
                : Double.NaN;
        return String.format("%s port %d %s: expected=%.6g, actual=%.6g, relDiff=%.2f%% (tol=%.1f%%)",
                scopeName, port, charName, expected, actual, relDiff, relativeTolerance * 100);
    }
}
