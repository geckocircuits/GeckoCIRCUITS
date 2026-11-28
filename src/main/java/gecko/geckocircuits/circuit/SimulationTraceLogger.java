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
package gecko.geckocircuits.circuit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import gecko.geckocircuits.circuit.circuitcomponents.CircuitTyp;

import java.io.*;
import java.util.zip.GZIPOutputStream;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Per-step CSV trace logger for simulation state capture.
 *
 * <p>Captures the complete simulation state after each time step, including:
 * <ul>
 *   <li>All node potentials (p[])</li>
 *   <li>All component currents (iALT[])</li>
 *   <li>Switch resistances and gate signals</li>
 *   <li>Diode resistances</li>
 * </ul>
 *
 * <p>Output format: gzip-compressed CSV, one row per time step.
 * Used to generate golden reference traces for comparison with the
 * headless HeadlessSimulationEngine (gecko-simulation-core).
 *
 * @author Trace logging for golden reference generation
 */
@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW",
        justification = "No finalizer; constructor fails only when the trace file cannot be opened, and tests subclass it")
public class SimulationTraceLogger implements Closeable {

    private final PrintWriter writer;
    private boolean headerWritten = false;

    /**
     * Creates a trace logger writing to the specified file.
     *
     * @param outputFile output file path (.csv.gz recommended)
     * @throws IOException if file cannot be created
     */
    public SimulationTraceLogger(File outputFile) throws IOException {
        OutputStream os = Files.newOutputStream(outputFile.toPath(), StandardOpenOption.CREATE_NEW);
        if (outputFile.getName().endsWith(".gz")) {
            os = new GZIPOutputStream(os);
        }
        this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8)));
    }

    /**
     * Logs one simulation time step.
     *
     * <p>Called after aktualisiereKnotenpotentiale() and dataTransferLK_Control()
     * in SimulationsKern.simulateOneTimeStep().
     *
     * @param time current simulation time [s]
     * @param lkm  the LKMatrices instance (contains p[], iALT[])
     * @param nl   the NetListLK instance (contains component types, nodes, parameters)
     */
    public synchronized void logStep(double time, LKMatrices lkm, NetListLK nl) {
        if (!headerWritten) {
            writeHeader(lkm, nl);
            headerWritten = true;
        }
        writeDataRow(time, lkm, nl);
    }

    private void writeHeader(LKMatrices lkm, NetListLK nl) {
        StringBuilder sb = new StringBuilder();
        sb.append("time");

        // Node potentials
        for (int i = 0; i < lkm.matrixSize; i++) {
            sb.append(",p[").append(i).append(']');
        }

        // Component currents
        int elementCount = nl.elementANZAHLneu;
        for (int i = 0; i < elementCount; i++) {
            sb.append(",i[").append(i).append(']');
        }

        // Switch/diode state columns
        for (int i = 0; i < elementCount; i++) {
            CircuitTyp type = nl.typ[i];
            String label = getComponentLabel(nl, i);

            if (isSwitch(type)) {
                sb.append(",R_sw_").append(label);
                sb.append(",gate_").append(label);
            } else if (isDiode(type)) {
                sb.append(",R_d_").append(label);
            }
        }

        writer.println(sb);
    }

    private void writeDataRow(double time, LKMatrices lkm, NetListLK nl) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "%.12e", time));

        // Node potentials
        for (int i = 0; i < lkm.matrixSize; i++) {
            sb.append(',').append(String.format(Locale.ROOT, "%.12e", lkm.p[i]));
        }

        // Component currents
        int elementCount = nl.elementANZAHLneu;
        for (int i = 0; i < elementCount; i++) {
            // Ideal-source current is an MNA unknown, not a reactive history value.
            double current = nl.typ[i] == CircuitTyp.LK_U
                    ? lkm.p[nl.knotenMAX + nl.spgQuelleNr[i]] : lkm.iALT[i];
            sb.append(',').append(String.format(Locale.ROOT, "%.12e", current));
        }

        // Switch/diode state
        for (int i = 0; i < elementCount; i++) {
            CircuitTyp type = nl.typ[i];
            double[] params = nl.parameter[i];

            if (isSwitch(type)) {
                sb.append(',').append(String.format(Locale.ROOT, "%.12e", params[0])); // current R
                sb.append(',').append(String.format(Locale.ROOT, "%.12e", getGateSignal(nl, i, type))); // gate
            } else if (isDiode(type)) {
                sb.append(',').append(String.format(Locale.ROOT, "%.12e", params[0])); // current rD
            }
        }

        writer.println(sb);
    }

    private static boolean isSwitch(CircuitTyp type) {
        return type == CircuitTyp.LK_S || type == CircuitTyp.LK_MOSFET;
    }

    private static boolean isDiode(CircuitTyp type) {
        return type == CircuitTyp.LK_D || type == CircuitTyp.LK_THYR || type == CircuitTyp.LK_IGBT;
    }

    private static double getGateSignal(NetListLK nl, int elementIndex, CircuitTyp type) {
        double[] params = nl.parameter[elementIndex];
        // MOSFET uses a different R_ON slot from the ideal switch.
        int onResistanceIndex = type == CircuitTyp.LK_MOSFET ? 2 : 1;
        return Math.abs(params[0] - params[onResistanceIndex]) < 1e-15 ? 1.0 : 0.0;
    }

    private static String getComponentLabel(NetListLK nl, int index) {
        if (nl.elements != null && index < nl.elements.length && nl.elements[index] != null) {
            String id = nl.elements[index].getStringID();
            if (id != null && !id.isEmpty()) {
                return id.replace(',', '_').replace(' ', '_');
            }
        }
        return "elem" + index;
    }

    @Override
    public synchronized void close() throws IOException {
        writer.flush();
        writer.close();
    }
}
