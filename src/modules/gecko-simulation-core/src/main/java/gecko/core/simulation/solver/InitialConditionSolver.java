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
package gecko.core.simulation.solver;

import gecko.core.allg.SolverType;
import gecko.core.circuit.circuitcomponents.CircuitTypCore;
import gecko.core.circuit.netlist.INetList;

/**
 * Solver for setting initial conditions in circuit simulation.
 *
 * This class extracts the initial condition logic from LKMatrices (lines 1290-1503)
 * to provide a GUI-free, testable initial condition solver for headless operation.
 *
 * Responsibilities:
 * 1. Setting initial node potentials (voltages) from capacitors and sources
 * 2. Setting initial element currents (inductors, current sources)
 * 3. Supporting different initialization modes (dialog vs. continue)
 *
 * Key simplifications for core module:
 * - Uses INetList interface instead of concrete NetListLK
 * - Works with MatrixSolver which provides getter/setter access
 * - Throws exceptions instead of showing GUI dialogs
 * - Foundational version (full logic can be added as netlist integration complete)
 *
 * @author GeckoCIRCUITS Team
 */
public class InitialConditionSolver {

    private final SolverType solverType;

    /** Threshold for treating inductance as zero */
    private static final double FAST_NULL_L = 1e-10;

    /**
     * Constructs an InitialConditionSolver with the specified solver type.
     *
     * @param solverType the numerical integration method
     */
    public InitialConditionSolver(SolverType solverType) {
        this.solverType = solverType;
    }

    /**
     * Sets initial conditions for the simulation.
     *
     * Initializes node potentials (pALT) and component currents (iALT) based on:
     * - Capacitor initial voltages from netlist parameters
     * - Inductor initial currents
     * - Voltage and current source parameters
     *
     * @param matrixSolver the matrix solver containing state arrays to initialize
     * @param netlist the circuit netlist interface
     * @param solverType the solver type for coefficient calculations
     * @throws IllegalStateException if initialization fails
     */
    public void setInitialConditions(
        MatrixSolver matrixSolver,
        INetList netlist,
        SolverType solverType
    ) {
        if (matrixSolver == null) {
            throw new IllegalStateException("MatrixSolver cannot be null");
        }
        if (netlist == null) {
            throw new IllegalStateException("INetList cannot be null");
        }

        // Initialize all potential arrays to zero
        initializePotentials(matrixSolver);

        // Initialize all current arrays to zero
        initializeCurrents(matrixSolver, netlist);

        // Restore saved potentials from netlist parameters (for CONTINUE mode)
        restorePotentialsFromNetlist(matrixSolver, netlist);

        // Restore saved currents from netlist parameters
        restoreCurrentsFromNetlist(matrixSolver, netlist);
    }

    /**
     * Initializes all potential arrays to zero.
     *
     * @param matrixSolver the matrix solver
     */
    private void initializePotentials(MatrixSolver matrixSolver) {
        double[] pALT = matrixSolver.getPALT();
        double[] pALTALT = matrixSolver.getPALTALT();
        double[] pALTALTALT = matrixSolver.getPALTALTALT();

        for (int i = 0; i < pALT.length; i++) {
            pALT[i] = 0;
            pALTALT[i] = 0;
            pALTALTALT[i] = 0;
        }
    }

    /**
     * Initializes all current arrays to zero.
     *
     * @param matrixSolver the matrix solver
     * @param netlist the circuit netlist
     */
    private void initializeCurrents(MatrixSolver matrixSolver, INetList netlist) {
        double[] iALT = matrixSolver.getIALT();
        double[] iALTALT = matrixSolver.getIALTALT();
        double[] iALTALTALT = matrixSolver.getIALTALTALT();

        for (int i = 0; i < iALT.length; i++) {
            iALT[i] = 0;
            iALTALT[i] = 0;
            iALTALTALT[i] = 0;
        }
    }

    /**
     * Restores potentials from netlist parameters (saved initial conditions).
     *
     * Handles capacitor voltages and other potential sources.
     *
     * @param matrixSolver the matrix solver
     * @param netlist the circuit netlist
     */
    private void restorePotentialsFromNetlist(MatrixSolver matrixSolver, INetList netlist) {
        double[] pALT = matrixSolver.getPALT();

        for (int i = 0; i < netlist.getElementCount(); i++) {
            CircuitTypCore type = netlist.getType(i);
            int nodeX = netlist.getNodeX(i);
            int nodeY = netlist.getNodeY(i);
            double[] params = netlist.getParameter(i);

            if (type == CircuitTypCore.LK_C || type == CircuitTypCore.TH_CTH) {
                // Capacitor: restore initial voltage
                // param[4] = voltage at X node, param[5] = voltage at Y node
                pALT[nodeX] = params[4];
                pALT[nodeY] = params[5];
            } else if (type == CircuitTypCore.REL_MMF || type == CircuitTypCore.TH_TEMP) {
                // MMF/Temperature source: restore potentials
                // param[8] = voltage at X node, param[9] = voltage at Y node
                pALT[nodeX] = params[8];
                pALT[nodeY] = params[9];
            } else if (type == CircuitTypCore.LK_LKOP2) {
                // Coupled inductor: restore source current in potential vector
                // param[2] = source current
                int voltageSourceNumber = netlist.getVoltageSourceNumber(i);
                if (voltageSourceNumber > 0) {
                    pALT[netlist.getNodeMax() + voltageSourceNumber] = params[2];
                }
            }
        }
    }

    /**
     * Restores currents from netlist parameters (saved initial conditions).
     *
     * Handles inductor currents, current sources, and coupled inductors.
     *
     * @param matrixSolver the matrix solver
     * @param netlist the circuit netlist
     */
    private void restoreCurrentsFromNetlist(MatrixSolver matrixSolver, INetList netlist) {
        double[] iALT = matrixSolver.getIALT();
        double[] iALTALT = matrixSolver.getIALTALT();

        for (int i = 0; i < netlist.getElementCount(); i++) {
            CircuitTypCore type = netlist.getType(i);
            double[] params = netlist.getParameter(i);

            if (type == CircuitTypCore.LK_C || type == CircuitTypCore.TH_CTH) {
                // Capacitor: restore initial current
                // param[2] = initial current
                iALT[i] = params[2];
            } else if (type == CircuitTypCore.LK_L || type == CircuitTypCore.NONLIN_REL) {
                // Inductor: restore initial current
                // param[1] = initial current (from dialog), param[2] = saved current
                iALT[i] = params[2];
                iALTALT[i] = iALT[i];
            } else if (type == CircuitTypCore.LK_LKOP2) {
                // Coupled inductor: restore initial current
                iALT[i] = params[2];
                iALTALT[i] = iALT[i];
            } else if (type == CircuitTypCore.LK_I || type == CircuitTypCore.TH_FLOW) {
                // Current source: restore initial current
                // param[6] = saved current value
                iALT[i] = params[6];
            }
        }
    }

    /**
     * Calculates the inductance coefficient for matrix stamping.
     *
     * Extracted from LKMatrices.getAWForInductance (lines 1504-1527).
     * Calculates the coefficient that relates inductance to matrix elements
     * for different numerical integration methods.
     *
     * @param inductance the inductance value
     * @param timeStep the simulation time step (dt)
     * @return the calculated coefficient aW for matrix stamping
     */
    public double getAWForInductance(double inductance, double timeStep) {
        double aW = 0;

        if (inductance < FAST_NULL_L) {
            // Inductance is near zero - use threshold value
            if (solverType == SolverType.SOLVER_BE) {
                aW = timeStep / FAST_NULL_L;
            } else if (solverType == SolverType.SOLVER_TRZ) {
                aW = 0.5 * timeStep / FAST_NULL_L;
            } else if (solverType == SolverType.SOLVER_GS) {
                aW = 2.0 / 3.0 * (timeStep / FAST_NULL_L);
            }
        } else {
            // Normal inductance value
            if (solverType == SolverType.SOLVER_BE) {
                aW = timeStep / inductance;
            } else if (solverType == SolverType.SOLVER_TRZ) {
                aW = 0.5 * timeStep / inductance;
            } else if (solverType == SolverType.SOLVER_GS) {
                aW = 2.0 / 3.0 * (timeStep / inductance);
            }
        }

        return aW;
    }
}
