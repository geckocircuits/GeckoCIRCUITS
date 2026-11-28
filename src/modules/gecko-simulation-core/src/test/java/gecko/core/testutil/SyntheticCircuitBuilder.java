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
package gecko.core.testutil;

import gecko.core.circuit.SourceType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.CircuitNetlist;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds synthetic circuit netlists programmatically for integration testing.
 *
 * <p>Each factory method creates a fully configured {@link CircuitNetlist} with the
 * correct parameter layout for the MatrixSolver (A-matrix stamping via StamperRegistry
 * and b-vector via MatrixSolver.buildVectorB).
 *
 * <p>Parameter index conventions (per component type):
 * <ul>
 *   <li>LK_R: [0]=R</li>
 *   <li>LK_L: [0]=L</li>
 *   <li>LK_C: [0]=C (for A-matrix stamper), [6]=C, [7]=C (linear), [10]=0 (correction)</li>
 *   <li>LK_U: [0]=sourceType (DC=0), [1]=voltage</li>
 *   <li>LK_D: [0]=rD (initial=rOff), [1]=Uf, [2]=rOn, [3]=rOff</li>
 *   <li>LK_S: [0]=R (initial=rOff), [1]=rOn, [2]=rOff, [8]=gate</li>
 *   <li>LK_MOSFET: [0]=R (initial=rOff), [2]=rOn, [3]=rOff, [8]=gate</li>
 * </ul>
 */
public final class SyntheticCircuitBuilder {

    private static final int MIN_PARAMS = 21;  // enough for all component types

    private SyntheticCircuitBuilder() {
    }

    // ========================================================================
    // C1: Resistor Divider — Vin → R1 → node1 → R2 → GND
    // ========================================================================

    /**
     * Creates a resistive voltage divider.
     *
     * <pre>
     * Topology:
     *   Vin (LK_U, DC) ──[node1]── R1 ──[node2]── R2 ──[GND]
     *   VoltSource: node1(+) to GND(−)
     * </pre>
     *
     * Elements: [0]=Vin, [1]=R1, [2]=R2
     * Nodes: 0=GND, 1=Vin+, 2=midpoint
     *
     * @param vin input voltage [V]
     * @param r1  upper resistor [Ω]
     * @param r2  lower resistor [Ω]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist resistorDivider(double vin, double r1, double r2) {
        int elementCount = 3;     // V_in, R1, R2
        int maxNode = 2;          // nodes: 0=GND, 1=Vin+, 2=midpoint
        int maxVSrc = 1;          // 1 voltage source

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_R};
        int[] nodeX = {1, 1, 2};  // Vin+: node1, R1: node1→node2, R2: node2→GND
        int[] nodeY = {0, 2, 0};
        int[] vSrcNr = {1, -1, -1};

        double[][] params = new double[elementCount][];
        // Voltage source: DC, value = vin
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        // R1
        params[1] = makeParams();
        params[1][0] = r1;

        // R2
        params[2] = makeParams();
        params[2][0] = r2;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "R1", "R2"});
    }

    // ========================================================================
    // C2: RL Step Response — Vin → R → L → GND
    // ========================================================================

    /**
     * Creates an RL series circuit driven by a DC voltage source.
     *
     * <pre>
     * Topology:
     *   Vin (LK_U, DC) ──[node1]── R ──[node2]── L ──[GND]
     * </pre>
     *
     * Elements: [0]=Vin, [1]=R, [2]=L
     * Nodes: 0=GND, 1=Vin+, 2=R-L junction
     *
     * @param vin input voltage [V]
     * @param r   resistance [Ω]
     * @param l   inductance [H]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist rlCircuit(double vin, double r, double l) {
        int elementCount = 3;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_L};
        int[] nodeX = {1, 1, 2};
        int[] nodeY = {0, 2, 0};
        int[] vSrcNr = {1, -1, -1};

        double[][] params = new double[elementCount][];
        // Voltage source
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        // Resistor
        params[1] = makeParams();
        params[1][0] = r;

        // Inductor
        params[2] = makeParams();
        params[2][0] = l;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "R", "L"});
    }

    // ========================================================================
    // C3: RC Step Response — Vin → R → node → C → GND
    // ========================================================================

    /**
     * Creates an RC series circuit driven by a DC voltage source.
     *
     * <pre>
     * Topology:
     *   Vin (LK_U, DC) ──[node1]── R ──[node2]── C ──[GND]
     * </pre>
     *
     * Elements: [0]=Vin, [1]=R, [2]=C
     * Nodes: 0=GND, 1=Vin+, 2=R-C junction (capacitor voltage)
     *
     * @param vin input voltage [V]
     * @param r   resistance [Ω]
     * @param c   capacitance [F]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist rcCircuit(double vin, double r, double c) {
        int elementCount = 3;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_C};
        int[] nodeX = {1, 1, 2};
        int[] nodeY = {0, 2, 0};
        int[] vSrcNr = {1, -1, -1};

        double[][] params = new double[elementCount][];
        // Voltage source
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        // Resistor
        params[1] = makeParams();
        params[1][0] = r;

        // Capacitor: params[0]=C (stamper), params[6]=C, params[7]=C (linear), params[10]=0
        params[2] = makeParams();
        params[2][0] = c;
        params[2][6] = c;
        params[2][7] = c;
        params[2][10] = 0.0;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "R", "C"});
    }

    // ========================================================================
    // C4: Resistor + Diode — Vin → R → D → GND
    // ========================================================================

    /**
     * RC ladder: a DC source at node 1 and, for k = 1..nodes-1, a series resistor between nodes k and k+1 with a
     * shunt capacitor from node k+1 to ground. The netlist has {@code nodes} non-ground nodes (getNodeMax() == nodes),
     * one voltage source, and a matrix order of {@code nodes + 2}.
     */
    public static CircuitNetlist rcLadder(int nodes, double vin, double r, double c) {
        if (nodes < 2) throw new IllegalArgumentException("A ladder needs at least 2 nodes");
        int elementCount = 1 + 2 * (nodes - 1);
        CircuitTypCore[] types = new CircuitTypCore[elementCount];
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        int[] vSrcNr = new int[elementCount];
        double[][] params = new double[elementCount][];
        java.util.Arrays.fill(vSrcNr, -1);
        types[0] = CircuitTypCore.LK_U;
        nodeX[0] = 1;
        nodeY[0] = 0;
        vSrcNr[0] = 1;
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;
        for (int k = 1; k < nodes; k++) {
            int resistor = 2 * k - 1;
            int capacitor = 2 * k;
            types[resistor] = CircuitTypCore.LK_R;
            nodeX[resistor] = k;
            nodeY[resistor] = k + 1;
            params[resistor] = makeParams();
            params[resistor][0] = r;
            types[capacitor] = CircuitTypCore.LK_C;
            nodeX[capacitor] = k + 1;
            nodeY[capacitor] = 0;
            params[capacitor] = makeParams();
            params[capacitor][0] = c;
            params[capacitor][6] = c;
            params[capacitor][7] = c;
        }
        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, nodes, 1, null);
    }

    /**
     * Creates a resistor-diode circuit.
     *
     * <pre>
     * Topology:
     *   Vin (LK_U, DC) ──[node1]── R ──[node2]── D ──[GND]
     *   Diode anode = node2, cathode = GND
     * </pre>
     *
     * Elements: [0]=Vin, [1]=R, [2]=D
     * Nodes: 0=GND, 1=Vin+, 2=R-D junction
     *
     * @param vin  input voltage [V]
     * @param r    series resistance [Ω]
     * @param uf   diode forward voltage [V]
     * @param rOn  diode ON resistance [Ω]
     * @param rOff diode OFF resistance [Ω]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist diodeCircuit(double vin, double r, double uf, double rOn, double rOff) {
        int elementCount = 3;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_D};
        int[] nodeX = {1, 1, 2};
        int[] nodeY = {0, 2, 0};
        int[] vSrcNr = {1, -1, -1};

        double[][] params = new double[elementCount][];
        // Voltage source
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        // Resistor
        params[1] = makeParams();
        params[1][0] = r;

        // Diode: [0]=rD (start OFF), [1]=Uf, [2]=rOn, [3]=rOff
        params[2] = makeParams();
        params[2][0] = rOff;   // start in OFF state
        params[2][1] = uf;
        params[2][2] = rOn;
        params[2][3] = rOff;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "R", "D"});
    }

    // ========================================================================
    // C5: Resistor + Switch — Vin → S → R → GND
    // ========================================================================

    /**
     * Creates a switch + resistor circuit. Gate signal must be set externally
     * via params[8] on element index 1 (the switch).
     *
     * <pre>
     * Topology:
     *   Vin (LK_U, DC) ──[node1]── S ──[node2]── R ──[GND]
     * </pre>
     *
     * Elements: [0]=Vin, [1]=S, [2]=R
     * Nodes: 0=GND, 1=Vin+, 2=S-R junction
     *
     * @param vin  input voltage [V]
     * @param r    load resistance [Ω]
     * @param rOn  switch ON resistance [Ω]
     * @param rOff switch OFF resistance [Ω]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist switchCircuit(double vin, double r, double rOn, double rOff) {
        int elementCount = 3;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_S, CircuitTypCore.LK_R};
        int[] nodeX = {1, 1, 2};
        int[] nodeY = {0, 2, 0};
        int[] vSrcNr = {1, -1, -1};

        double[][] params = new double[elementCount][];
        // Voltage source
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        // Switch (LK_S): [0]=current R (start OFF), [1]=rOn, [2]=rOff, [8]=gate
        params[1] = makeParams();
        params[1][0] = rOff;   // start OFF
        params[1][1] = rOn;
        params[1][2] = rOff;
        params[1][8] = 0.0;    // gate off

        // Resistor
        params[2] = makeParams();
        params[2][0] = r;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "S", "R"});
    }

    // ========================================================================
    // C6: Synthetic Buck Converter
    // ========================================================================

    /**
     * Creates a simplified buck converter topology.
     *
     * <pre>
     * Topology:
     *   Vin ──[node1]── S ──[node2]── L ──[node3]── R_load ──[GND]
     *                      D (cathode=node2, anode=GND)
     *                                       C (node3 to GND)
     *
     * Switch S: controlled by gate signal (params[8] on element 1)
     * Diode D: freewheeling diode
     * </pre>
     *
     * Elements: [0]=Vin, [1]=S, [2]=D, [3]=L, [4]=C, [5]=R_load
     * Nodes: 0=GND, 1=Vin+, 2=switch node, 3=output
     *
     * @param vin   input voltage [V]
     * @param l     inductance [H]
     * @param c     output capacitance [F]
     * @param rLoad load resistance [Ω]
     * @param rSwOn switch ON resistance [Ω]
     * @param rSwOff switch OFF resistance [Ω]
     * @param uf    diode forward voltage [V]
     * @param rDOn  diode ON resistance [Ω]
     * @param rDOff diode OFF resistance [Ω]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist buckConverter(double vin, double l, double c, double rLoad,
                                               double rSwOn, double rSwOff,
                                               double uf, double rDOn, double rDOff) {
        int elementCount = 6;
        int maxNode = 3;          // 0=GND, 1=Vin+, 2=sw node, 3=output
        int maxVSrc = 1;

        CircuitTypCore[] types = {
            CircuitTypCore.LK_U,    // [0] Vin
            CircuitTypCore.LK_S,    // [1] S (high-side switch)
            CircuitTypCore.LK_D,    // [2] D (freewheeling diode)
            CircuitTypCore.LK_L,    // [3] L (inductor)
            CircuitTypCore.LK_C,    // [4] C (output capacitor)
            CircuitTypCore.LK_R     // [5] R_load
        };
        int[] nodeX = {1, 1, 0, 2, 3, 3};  // D: GND(anode)→node2(cathode)
        int[] nodeY = {0, 2, 2, 3, 0, 0};
        int[] vSrcNr = {1, -1, -1, -1, -1, -1};

        double[][] params = new double[elementCount][];

        // [0] Voltage source
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        // [1] Switch (LK_S): start OFF
        params[1] = makeParams();
        params[1][0] = rSwOff;
        params[1][1] = rSwOn;
        params[1][2] = rSwOff;
        params[1][8] = 0.0;

        // [2] Diode (LK_D): start OFF
        params[2] = makeParams();
        params[2][0] = rDOff;
        params[2][1] = uf;
        params[2][2] = rDOn;
        params[2][3] = rDOff;

        // [3] Inductor
        params[3] = makeParams();
        params[3][0] = l;

        // [4] Capacitor
        params[4] = makeParams();
        params[4][0] = c;
        params[4][6] = c;
        params[4][7] = c;
        params[4][10] = 0.0;

        // [5] R_load
        params[5] = makeParams();
        params[5][0] = rLoad;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "S", "D", "L", "C", "R_load"});
    }

    // ========================================================================
    // C7: RLC Series — Vin → R → L → C → GND
    // ========================================================================

    /**
     * Creates a series RLC circuit driven by a DC voltage source.
     *
     * <pre>
     * Topology:
     *   Vin (LK_U, DC) ──[node1]── R ──[node2]── L ──[node3]── C ──[GND]
     * </pre>
     *
     * Elements: [0]=Vin, [1]=R, [2]=L, [3]=C
     * Nodes: 0=GND, 1=Vin+, 2=R-L junction, 3=L-C junction (capacitor voltage)
     *
     * @param vin input voltage [V]
     * @param r   resistance [Ω]
     * @param l   inductance [H]
     * @param c   capacitance [F]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist rlcSeriesCircuit(double vin, double r, double l, double c) {
        int elementCount = 4;
        int maxNode = 3;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                CircuitTypCore.LK_L, CircuitTypCore.LK_C};
        int[] nodeX = {1, 1, 2, 3};
        int[] nodeY = {0, 2, 3, 0};
        int[] vSrcNr = {1, -1, -1, -1};

        double[][] params = new double[elementCount][];
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        params[1] = makeParams();
        params[1][0] = r;

        params[2] = makeParams();
        params[2][0] = l;

        params[3] = makeParams();
        params[3][0] = c;
        params[3][6] = c;
        params[3][7] = c;
        params[3][10] = 0.0;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "R", "L", "C"});
    }

    // ========================================================================
    // C8: RLC Parallel — Vin → Rsrc → node → (R || L || C) → GND
    // ========================================================================

    /**
     * Creates a parallel RLC circuit with a source resistor.
     *
     * <pre>
     * Topology:
     *   Vin (LK_U, DC) ──[node1]── Rsrc ──[node2]── R ──[GND]
     *                                        │── L ──[GND]
     *                                        │── C ──[GND]
     * </pre>
     *
     * Elements: [0]=Vin, [1]=Rsrc, [2]=R, [3]=L, [4]=C
     * Nodes: 0=GND, 1=Vin+, 2=parallel node
     *
     * @param vin  input voltage [V]
     * @param rSrc source resistance [Ω]
     * @param r    parallel resistance [Ω]
     * @param l    parallel inductance [H]
     * @param c    parallel capacitance [F]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist rlcParallelCircuit(double vin, double rSrc,
                                                     double r, double l, double c) {
        int elementCount = 5;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                CircuitTypCore.LK_R, CircuitTypCore.LK_L, CircuitTypCore.LK_C};
        int[] nodeX = {1, 1, 2, 2, 2};
        int[] nodeY = {0, 2, 0, 0, 0};
        int[] vSrcNr = {1, -1, -1, -1, -1};

        double[][] params = new double[elementCount][];
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        params[1] = makeParams();
        params[1][0] = rSrc;

        params[2] = makeParams();
        params[2][0] = r;

        params[3] = makeParams();
        params[3][0] = l;

        params[4] = makeParams();
        params[4][0] = c;
        params[4][6] = c;
        params[4][7] = c;
        params[4][10] = 0.0;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "R_src", "R", "L", "C"});
    }

    // ========================================================================
    // C9: AC Sine Source + RL load — Vsin → R → L → GND
    // ========================================================================

    /**
     * Creates an AC sine source driving an RL load.
     *
     * <pre>
     * Topology:
     *   Vsin (LK_U, SIN) ──[node1]── R ──[node2]── L ──[GND]
     * </pre>
     *
     * Elements: [0]=Vsin, [1]=R, [2]=L
     * Nodes: 0=GND, 1=Vsin+, 2=R-L junction
     *
     * @param amplitude peak amplitude [V]
     * @param freq      frequency [Hz]
     * @param r         resistance [Ω]
     * @param l         inductance [H]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist acSineSource(double amplitude, double freq, double r, double l) {
        int elementCount = 3;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_L};
        int[] nodeX = {1, 1, 2};
        int[] nodeY = {0, 2, 0};
        int[] vSrcNr = {1, -1, -1};

        double[][] params = new double[elementCount][];
        // Sine voltage source: [0]=type, [1]=voltage(unused for sin), [2]=freq,
        // [3]=offset, [4]=phase, [20]=amplitude
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_SIN_NEW;
        params[0][2] = freq;
        params[0][3] = 0.0;   // offset
        params[0][4] = 0.0;   // phase
        params[0][20] = amplitude;

        params[1] = makeParams();
        params[1][0] = r;

        params[2] = makeParams();
        params[2][0] = l;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_sin", "R", "L"});
    }

    /**
     * Creates an AC sine source driving an RC load.
     *
     * <pre>
     * Topology:
     *   Vsin (LK_U, SIN) ──[node1]── R ──[node2]── C ──[GND]
     * </pre>
     *
     * Elements: [0]=Vsin, [1]=R, [2]=C
     * Nodes: 0=GND, 1=Vsin+, 2=R-C junction (output)
     *
     * @param amplitude peak amplitude [V]
     * @param freq      frequency [Hz]
     * @param r         resistance [Ω]
     * @param c         capacitance [F]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist acSineSourceRC(double amplitude, double freq,
                                                double r, double c) {
        int elementCount = 3;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R, CircuitTypCore.LK_C};
        int[] nodeX = {1, 1, 2};
        int[] nodeY = {0, 2, 0};
        int[] vSrcNr = {1, -1, -1};

        double[][] params = new double[elementCount][];
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_SIN_NEW;
        params[0][2] = freq;
        params[0][3] = 0.0;
        params[0][4] = 0.0;
        params[0][20] = amplitude;

        params[1] = makeParams();
        params[1][0] = r;

        params[2] = makeParams();
        params[2][0] = c;
        params[2][6] = c;
        params[2][7] = c;
        params[2][10] = 0.0;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_sin", "R", "C"});
    }

    // ========================================================================
    // C10: Boost Converter — Vin → L → (S→GND, D→Cout||R)
    // ========================================================================

    /**
     * Creates a boost converter topology.
     *
     * <pre>
     * Topology:
     *   Vin ──[node1]── L ──[node2]── S ──[GND]
     *                          │── D (anode=node2) ──[node3]── R_load ──[GND]
     *                                                  │── C ──[GND]
     * </pre>
     *
     * Elements: [0]=Vin, [1]=L, [2]=S, [3]=D, [4]=C, [5]=R_load
     * Nodes: 0=GND, 1=Vin+, 2=switch node, 3=output
     *
     * @param vin    input voltage [V]
     * @param l      inductance [H]
     * @param c      output capacitance [F]
     * @param rLoad  load resistance [Ω]
     * @param rSwOn  switch ON resistance [Ω]
     * @param rSwOff switch OFF resistance [Ω]
     * @param uf     diode forward voltage [V]
     * @param rDOn   diode ON resistance [Ω]
     * @param rDOff  diode OFF resistance [Ω]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist boostConverter(double vin, double l, double c, double rLoad,
                                                double rSwOn, double rSwOff,
                                                double uf, double rDOn, double rDOff) {
        int elementCount = 6;
        int maxNode = 3;
        int maxVSrc = 1;

        CircuitTypCore[] types = {
            CircuitTypCore.LK_U,    // [0] Vin
            CircuitTypCore.LK_L,    // [1] L (inductor)
            CircuitTypCore.LK_S,    // [2] S (low-side switch to GND)
            CircuitTypCore.LK_D,    // [3] D (boost diode, anode=node2, cathode=node3)
            CircuitTypCore.LK_C,    // [4] C (output capacitor)
            CircuitTypCore.LK_R     // [5] R_load
        };
        int[] nodeX = {1, 1, 2, 2, 3, 3};
        int[] nodeY = {0, 2, 0, 3, 0, 0};
        int[] vSrcNr = {1, -1, -1, -1, -1, -1};

        double[][] params = new double[elementCount][];

        // [0] Voltage source
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        // [1] Inductor
        params[1] = makeParams();
        params[1][0] = l;

        // [2] Switch (LK_S): start OFF
        params[2] = makeParams();
        params[2][0] = rSwOff;
        params[2][1] = rSwOn;
        params[2][2] = rSwOff;
        params[2][8] = 0.0;

        // [3] Diode (LK_D): start OFF
        params[3] = makeParams();
        params[3][0] = rDOff;
        params[3][1] = uf;
        params[3][2] = rDOn;
        params[3][3] = rDOff;

        // [4] Capacitor
        params[4] = makeParams();
        params[4][0] = c;
        params[4][6] = c;
        params[4][7] = c;
        params[4][10] = 0.0;

        // [5] R_load
        params[5] = makeParams();
        params[5][0] = rLoad;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "L", "S", "D", "C", "R_load"});
    }

    // ========================================================================
    // C11: Buck-Boost Converter — Vin → S → L → GND, D → Cout||R
    // ========================================================================

    /**
     * Creates a buck-boost converter topology (inverting).
     *
     * <pre>
     * Topology:
     *   Vin ──[node1]── S ──[node2]── L ──[GND]
     *                          │── D (anode=node3, cathode=node2)
     *                                  [node3]── C ──[GND]
     *                                    │── R_load ──[GND]
     *
     * Output voltage is inverted (node3 is negative w.r.t. GND).
     * </pre>
     *
     * Elements: [0]=Vin, [1]=S, [2]=L, [3]=D, [4]=C, [5]=R_load
     * Nodes: 0=GND, 1=Vin+, 2=switch-inductor node, 3=output (negative)
     *
     * @param vin    input voltage [V]
     * @param l      inductance [H]
     * @param c      output capacitance [F]
     * @param rLoad  load resistance [Ω]
     * @param rSwOn  switch ON resistance [Ω]
     * @param rSwOff switch OFF resistance [Ω]
     * @param uf     diode forward voltage [V]
     * @param rDOn   diode ON resistance [Ω]
     * @param rDOff  diode OFF resistance [Ω]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist buckBoostConverter(double vin, double l, double c, double rLoad,
                                                     double rSwOn, double rSwOff,
                                                     double uf, double rDOn, double rDOff) {
        int elementCount = 6;
        int maxNode = 3;
        int maxVSrc = 1;

        CircuitTypCore[] types = {
            CircuitTypCore.LK_U,    // [0] Vin
            CircuitTypCore.LK_S,    // [1] S (high-side switch)
            CircuitTypCore.LK_L,    // [2] L (inductor)
            CircuitTypCore.LK_D,    // [3] D (freewheeling diode, anode=node3, cathode=node2)
            CircuitTypCore.LK_C,    // [4] C (output capacitor)
            CircuitTypCore.LK_R     // [5] R_load
        };
        // S: node1→node2, L: node2→GND, D: node3(anode)→node2(cathode)
        int[] nodeX = {1, 1, 2, 3, 3, 3};
        int[] nodeY = {0, 2, 0, 2, 0, 0};
        int[] vSrcNr = {1, -1, -1, -1, -1, -1};

        double[][] params = new double[elementCount][];

        // [0] Voltage source
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        // [1] Switch (LK_S): start OFF
        params[1] = makeParams();
        params[1][0] = rSwOff;
        params[1][1] = rSwOn;
        params[1][2] = rSwOff;
        params[1][8] = 0.0;

        // [2] Inductor
        params[2] = makeParams();
        params[2][0] = l;

        // [3] Diode (LK_D): start OFF, anode=node3, cathode=node2
        params[3] = makeParams();
        params[3][0] = rDOff;
        params[3][1] = uf;
        params[3][2] = rDOn;
        params[3][3] = rDOff;

        // [4] Capacitor
        params[4] = makeParams();
        params[4][0] = c;
        params[4][6] = c;
        params[4][7] = c;
        params[4][10] = 0.0;

        // [5] R_load
        params[5] = makeParams();
        params[5][0] = rLoad;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "S", "L", "D", "C", "R_load"});
    }

    // ========================================================================
    // C12: Thyristor Circuit — Vin → R → THYR → GND
    // ========================================================================

    /**
     * Creates a resistor-thyristor circuit.
     *
     * <pre>
     * Topology:
     *   Vin (LK_U, DC) ──[node1]── R ──[node2]── THYR ──[GND]
     *   Thyristor anode = node2, cathode = GND
     * </pre>
     *
     * Elements: [0]=Vin, [1]=R, [2]=THYR
     * Nodes: 0=GND, 1=Vin+, 2=R-THYR junction
     *
     * @param vin  input voltage [V]
     * @param r    series resistance [Ω]
     * @param rOn  thyristor ON resistance [Ω]
     * @param rOff thyristor OFF resistance [Ω]
     * @param uf   thyristor forward voltage [V]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist thyristorCircuit(double vin, double r,
                                                   double rOn, double rOff, double uf) {
        int elementCount = 3;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                CircuitTypCore.LK_THYR};
        int[] nodeX = {1, 1, 2};
        int[] nodeY = {0, 2, 0};
        int[] vSrcNr = {1, -1, -1};

        double[][] params = new double[elementCount][];
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        params[1] = makeParams();
        params[1][0] = r;

        // Thyristor: [0]=rD (start OFF), [1]=Uf, [2]=rOn, [3]=rOff, [8]=gate,
        // [9]=recovery time, [11]=last switch-off time
        params[2] = makeParams();
        params[2][0] = rOff;
        params[2][1] = uf;
        params[2][2] = rOn;
        params[2][3] = rOff;
        params[2][8] = 0.0;     // gate off
        params[2][9] = 10e-6;   // recovery time
        params[2][11] = -1.0;   // last switch-off time (never)

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "R", "THYR"});
    }

    // ========================================================================
    // C13: IGBT Circuit — Vin → R → IGBT → GND
    // ========================================================================

    /**
     * Creates a resistor-IGBT circuit.
     *
     * <pre>
     * Topology:
     *   Vin (LK_U, DC) ──[node1]── R ──[node2]── IGBT ──[GND]
     *   IGBT collector = node2, emitter = GND
     * </pre>
     *
     * Elements: [0]=Vin, [1]=R, [2]=IGBT
     * Nodes: 0=GND, 1=Vin+, 2=R-IGBT junction
     *
     * @param vin  input voltage [V]
     * @param r    series resistance [Ω]
     * @param rOn  IGBT ON resistance [Ω]
     * @param rOff IGBT OFF resistance [Ω]
     * @param uf   IGBT forward voltage [V]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist igbtCircuit(double vin, double r,
                                              double rOn, double rOff, double uf) {
        int elementCount = 3;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {CircuitTypCore.LK_U, CircuitTypCore.LK_R,
                CircuitTypCore.LK_IGBT};
        int[] nodeX = {1, 1, 2};
        int[] nodeY = {0, 2, 0};
        int[] vSrcNr = {1, -1, -1};

        double[][] params = new double[elementCount][];
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = vin;

        params[1] = makeParams();
        params[1][0] = r;

        // IGBT: [0]=rD (start OFF), [1]=Uf, [2]=rOn, [3]=rOff, [8]=gate
        params[2] = makeParams();
        params[2][0] = rOff;
        params[2][1] = uf;
        params[2][2] = rOn;
        params[2][3] = rOff;
        params[2][8] = 0.0;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"V_in", "R", "IGBT"});
    }

    // ========================================================================
    // C14: Thermal RC Network — Psrc → Rth → Cth → Tamb
    // ========================================================================

    /**
     * Creates a thermal RC network for temperature rise testing.
     *
     * <pre>
     * Topology (thermal analogy: voltage=temperature, current=heat flow):
     *   TH_FLOW (heat source) ──[node1]── TH_RTH ──[node2]── TH_TEMP (ambient)
     *                              │── TH_CTH ──[GND]
     *
     * The TH_FLOW injects power at node1, TH_RTH connects to TH_TEMP (fixed ambient),
     * and TH_CTH stores thermal energy at node1.
     * </pre>
     *
     * Elements: [0]=TH_TEMP(ambient), [1]=TH_FLOW(power), [2]=TH_RTH, [3]=TH_CTH
     * Nodes: 0=GND(thermal ref), 1=hot node, 2=ambient node
     *
     * @param power   heat flow [W]
     * @param rth     thermal resistance [K/W]
     * @param cth     thermal capacitance [J/K]
     * @param tAmbient ambient temperature [K or °C]
     * @return configured CircuitNetlist
     */
    public static CircuitNetlist thermalRCNetwork(double power, double rth, double cth,
                                                   double tAmbient) {
        int elementCount = 4;
        int maxNode = 2;
        int maxVSrc = 1;

        CircuitTypCore[] types = {
            CircuitTypCore.TH_TEMP,   // [0] ambient temperature source (voltage source)
            CircuitTypCore.TH_FLOW,   // [1] heat flow source (current source)
            CircuitTypCore.TH_RTH,    // [2] thermal resistance
            CircuitTypCore.TH_CTH     // [3] thermal capacitance
        };
        // TH_TEMP: node2(+) to GND(-), TH_FLOW: GND→node1 (heat injection into node1),
        // TH_RTH: node1→node2, TH_CTH: node1 to GND
        // Note: buildVectorB for TH_FLOW uses bContribution = -params[1], so
        // b[nodeX] -= power, b[nodeY] += power. To inject power INTO node1,
        // nodeX=GND(0) and nodeY=node1(1), giving b[1] += power.
        int[] nodeX = {2, 0, 1, 1};
        int[] nodeY = {0, 1, 2, 0};
        int[] vSrcNr = {1, -1, -1, -1};

        double[][] params = new double[elementCount][];

        // [0] Temperature source (ambient): uses voltage source stamper
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = tAmbient;

        // [1] Heat flow source: uses current source stamper
        // buildVectorB reads params[0] as sourceType and params[1] as current value,
        // matching the same convention as LK_I (see MatrixSolver.buildVectorB TH_FLOW case).
        params[1] = makeParams();
        params[1][0] = SourceType.QUELLE_DC_NEW;
        params[1][1] = power;

        // [2] Thermal resistance: uses resistor stamper, params[0]=Rth
        params[2] = makeParams();
        params[2][0] = rth;

        // [3] Thermal capacitance: uses capacitor stamper
        params[3] = makeParams();
        params[3][0] = cth;
        params[3][6] = cth;
        params[3][7] = cth;
        params[3][10] = 0.0;

        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc,
                new String[]{"T_ambient", "P_source", "R_th", "C_th"});
    }

    /**
     * One MMC arm: a DC source, an arm inductor, a series chain of half-bridge submodules and a load resistor.
     * Each submodule has a capacitor between P and N, an upper device X-P and a lower device X-N with N the
     * submodule output, each device an ideal switch with an antiparallel diode. Even submodules are bypassed
     * (lower switch on, upper off) and odd ones are inserted (both switches off), so inserted capacitors charge
     * through their upper diodes and those diodes turn off as the capacitors reach the source share.
     * The netlist has {@code 2 * submodules + 2} nodes and {@code 5 * submodules + 3} elements.
     */
    public static CircuitNetlist mmcArm(int submodules, double vSubmodule, double cSubmodule, double rLoad) {
        int elementCount = 3 + 5 * submodules;
        int maxNode = 2 * submodules + 2;
        CircuitTypCore[] types = new CircuitTypCore[elementCount];
        int[] nodeX = new int[elementCount];
        int[] nodeY = new int[elementCount];
        int[] vSrcNr = new int[elementCount];
        double[][] params = new double[elementCount][];
        java.util.Arrays.fill(vSrcNr, -1);
        types[0] = CircuitTypCore.LK_U;
        nodeX[0] = 1;
        nodeY[0] = 0;
        vSrcNr[0] = 1;
        params[0] = makeParams();
        params[0][0] = SourceType.QUELLE_DC_NEW;
        params[0][1] = submodules * vSubmodule * 0.5;
        types[1] = CircuitTypCore.LK_L;
        nodeX[1] = 1;
        nodeY[1] = 2;
        params[1] = makeParams();
        params[1][0] = 1e-4;
        double rOn = 1e-3;
        double rOff = 1e7;
        int e = 2;
        for (int sm = 1; sm <= submodules; sm++) {
            int x = sm == 1 ? 2 : 2 * sm;
            int p = 2 * sm + 1;
            int n = 2 * sm + 2;
            boolean bypassed = sm % 2 == 0;
            e = mmcDevice(types, nodeX, nodeY, params, e, CircuitTypCore.LK_S, x, p, bypassed ? rOff : rOff, rOn, rOff, false);
            e = mmcDevice(types, nodeX, nodeY, params, e, CircuitTypCore.LK_S, x, n, bypassed ? rOn : rOff, rOn, rOff, false);
            e = mmcDevice(types, nodeX, nodeY, params, e, CircuitTypCore.LK_D, x, p, rOff, rOn, rOff, true);
            e = mmcDevice(types, nodeX, nodeY, params, e, CircuitTypCore.LK_D, n, x, rOff, rOn, rOff, true);
            types[e] = CircuitTypCore.LK_C;
            nodeX[e] = p;
            nodeY[e] = n;
            params[e] = makeParams();
            params[e][0] = cSubmodule;
            params[e][6] = cSubmodule;
            params[e][7] = cSubmodule;
            e++;
        }
        types[e] = CircuitTypCore.LK_R;
        nodeX[e] = maxNode;
        nodeY[e] = 0;
        params[e] = makeParams();
        params[e][0] = rLoad;
        return buildNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, 1, null);
    }

    private static int mmcDevice(CircuitTypCore[] types, int[] nodeX, int[] nodeY, double[][] params, int index,
            CircuitTypCore type, int x, int y, double r0, double rOn, double rOff, boolean diode) {
        types[index] = type;
        nodeX[index] = x;
        nodeY[index] = y;
        params[index] = makeParams();
        params[index][0] = r0;
        if (diode) {
            params[index][1] = 0.7;
            params[index][2] = rOn;
            params[index][3] = rOff;
        } else {
            params[index][1] = rOn;
            params[index][2] = rOff;
        }
        return index + 1;
    }

    // ========================================================================
    // Helper methods
    // ========================================================================

    private static double[] makeParams() {
        return new double[MIN_PARAMS];
    }

    private static CircuitNetlist buildNetlist(
            CircuitTypCore[] types, int[] nodeX, int[] nodeY,
            int[] vSrcNr, double[][] params,
            int maxNode, int maxVSrc,
            String[] componentNames) {

        CircuitNetlist netlist = new CircuitNetlist();
        netlist.initNetlist(types, nodeX, nodeY, vSrcNr, params, maxNode, maxVSrc, types.length);

        if (componentNames != null) {
            Map<String, Integer> nameMap = new LinkedHashMap<>();
            for (int i = 0; i < componentNames.length; i++) {
                nameMap.put(componentNames[i], i);
            }
            netlist.setComponentNameToIndex(nameMap);
        }

        return netlist;
    }
}
