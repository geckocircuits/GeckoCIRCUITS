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
 * GUI-free base class for current source calculators.
 * Contains pure calculation logic without parent component references.
 *
 * <p>Current sources inject current into the B vector of the MNA system.
 * They do not modify the A matrix (no stamping needed).
 *
 * @author GeckoCIRCUITS Team
 * @since Sprint 5 Phase 2 (GUI-free extraction)
 */
public abstract class AbstractCurrentSourceCalculatorCore extends CircuitComponentCore
    implements AStampable, BStampable, HistoryUpdatable {

    /**
     * Constructor for GUI-free current source calculator.
     * @param solverType the solver type (BE, TRZ, GS)
     */
    public AbstractCurrentSourceCalculatorCore(final SolverType solverType) {
        super(solverType);
    }

    /**
     * Current sources do not stamp the A matrix.
     * This is a no-op for current sources.
     */
    @Override
    public void stampMatrixA(final double[][] matrix, final double deltaT) {
        // no-op - current sources only stamp B vector
    }

    /**
     * Update history after simulation step.
     * Current sources update potentials from the solution vector.
     */
    @Override
    public void updateHistory(final double[] potentials) {
        _potential1 = potentials[matrixIndices[0]];
        _potential2 = potentials[matrixIndices[1]];
        _voltage = _potential1 - _potential2;
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
