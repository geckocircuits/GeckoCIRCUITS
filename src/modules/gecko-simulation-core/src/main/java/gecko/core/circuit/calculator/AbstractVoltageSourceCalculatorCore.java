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
package gecko.core.circuit.calculator;

import gecko.core.circuit.CircuitComponentCore;
import gecko.core.allg.SolverType;

/**
 * GUI-free base class for voltage source calculators.
 * Contains pure calculation logic without parent component references.
 *
 * <p>This class provides the core voltage source stamping logic for MNA matrices.
 * Matrix indices are injected externally rather than pulled from GUI terminals.
 *
 * @author GeckoCIRCUITS Team
 * @since Sprint 5 Phase 2 (GUI-free extraction)
 */
public abstract class AbstractVoltageSourceCalculatorCore extends CircuitComponentCore
    implements AStampable, DirectCurrentCalculatable, HistoryUpdatable {

    /** Branch equation index for voltage source in augmented MNA matrix */
    protected int _z = -1;

    /**
     * Constructor for GUI-free voltage source calculator.
     * @param solverType the solver type (BE, TRZ, GS)
     */
    public AbstractVoltageSourceCalculatorCore(final SolverType solverType) {
        super(solverType);
    }

    /**
     * Stamp the A matrix with voltage source contributions.
     * Adds branch equation: V+ - V- = Vsource
     */
    @Override
    public void stampMatrixA(final double[][] matrix, final double deltaT) {
        assert _z > 0 : "Z index must be set before stamping";
        // Voltage source branch equation
        matrix[matrixIndices[0]][_z] += (+1.0);
        matrix[matrixIndices[1]][_z] += (-1.0);
        matrix[_z][matrixIndices[0]] += (+1.0);
        matrix[_z][matrixIndices[1]] += (-1.0);
    }

    @Override
    public final int getZValue() {
        return _z;
    }

    @Override
    public final void setZValue(final int value) {
        _z = value;
    }

    /**
     * Update history after simulation step.
     * Voltage source current is stored in the potential vector at index _z.
     */
    @Override
    public final void updateHistory(final double[] potentials) {
        // Voltage source current is an unknown in the augmented MNA system
        _current = potentials[_z];
        _potential1 = potentials[matrixIndices[0]];
        _potential2 = potentials[matrixIndices[1]];
    }

    /**
     * Set matrix indices externally (replaces terminal-based initialization).
     * @param index0 positive terminal matrix index
     * @param index1 negative terminal matrix index
     */
    public void setMatrixIndices(final int index0, final int index1) {
        matrixIndices[0] = index0;
        matrixIndices[1] = index1;
    }
}
