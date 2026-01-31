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
package ch.technokrat.gecko.geckocircuits.circuit;

import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Core simulation logic for circuit components.
 * NO GUI IMPORTS - extractable to gecko-simulation-core.
 *
 * <p>This abstract class contains all the pure simulation logic extracted from
 * CircuitComponent. It has NO dependencies on java.awt, javax.swing, or any
 * UI-related classes.
 *
 * <p>The GUI-specific CircuitComponent class extends this to add rendering
 * and property dialog functionality for the desktop application.
 *
 * @author GeckoCIRCUITS Team
 * @since 2.0 (refactored for GUI-free extraction)
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Matrix indices exposed for efficient circuit matrix stamping")
public abstract class CircuitComponentCore implements ICircuitCalculator {

    protected int _portNumber = -1;
    protected int[] matrixIndices = new int[]{-1, -1};
    protected int _componentNumber;
    protected double _current;
    protected double _voltage;
    protected double _potential1;
    protected double _potential2;
    protected double _potOld1;
    protected double _potOld2;
    protected double _oldCurrent;
    protected double _oldOldCurrent;
    protected boolean _needsOldPotCurrent = false;
    
    // For stepping back in history
    protected double prev_time = -1;
    protected boolean stepped_back = false;
    // Double array to hold history - previous two steps
    protected double[][] var_history;
    protected int steps_saved = 2;
    protected int steps_reversed = 0;
    
    // Constants for semiconductor defaults
    public static final double DEFAULT_U_FORWARD = 0.6;
    public static final double DEFAULT_R_ON = 10e-3;
    public static final double DEFAULT_R_OFF = 10e6;
    
    /**
     * Used for diode-behavior, to find a correct on/off state (Diode,
     * Thyristor, IGBT).
     */
    public static final double disturbanceValue = 0.0;
    
    protected final SolverType _solverType;

    /**
     * Constructor for CircuitComponentCore.
     * @param solverType the integration method to use
     */
    protected CircuitComponentCore(final SolverType solverType) {
        this._solverType = solverType;
        this.matrixIndices = new int[2];
        this.var_history = new double[steps_saved][9];
    }
    
    @Override
    public double getOldCurrent() {
        return _oldCurrent;
    }        

    @Override
    public void init() {
        _current = 0;
        _oldCurrent = 0;
        _oldOldCurrent = 0;
        _voltage = 0;
        _potential1 = 0;
        _potential2 = 0;
        _potOld1 = 0;
        _potOld2 = 0;
    }

    @Override
    public final double getVoltage() {
        return _voltage;
    }

    @Override
    public double getPotential(int terminalNumber) {
        switch (terminalNumber) {
            case 0:
                return _potential1;
            case 1:
                return _potential2;
            default:
                assert false;
        }
        return 0;
    }

    @Override
    public void setComponentNumber(int componentNumber) {
        _componentNumber = componentNumber;
    }

    @Override
    public int getComponentNumber() {
        return _componentNumber;
    }

    @Override
    public final void calculateVoltage(double[] p) {
        _voltage = p[matrixIndices[0]] - p[matrixIndices[1]];
    }

    @Override
    public int getTerminalIndex(int termNumber) {
        return matrixIndices[termNumber];
    }        

    @Override
    public final double getCurrent() {
        return _current;
    }
    
    @Override
    public SolverType getSolverType() {
        return _solverType;
    }

    /**
     * Move history forward when going forward a timestep.
     * Copies data of the previous timestep into the data from 2 previous timesteps ago.
     */
    private void historyForward() {
        for (int j = var_history.length - 1; j > 0; j--) {
            for (int i = 0; i < var_history[0].length; i++) {
                var_history[j][i] = var_history[j - 1][i];
            }
        }
    }

    @Override
    public void saveHistory() {
        if (!stepped_back) {
            // Write existing values into history
            // Do so only if we have not stepped back; otherwise they are already there
            historyForward();
            var_history[0][0] = prev_time;
            var_history[0][1] = _potential1;
            var_history[0][2] = _potential2;
            var_history[0][3] = _current;
            var_history[0][4] = _voltage;
            if (_needsOldPotCurrent) {
                var_history[0][5] = _potOld1;
                var_history[0][6] = _potOld2;
                var_history[0][7] = _oldCurrent;
                var_history[0][8] = _oldOldCurrent;
            }
        }
    }

    @Override
    public void historyMaintainance(final double time) {
        prev_time = time;

        if (stepped_back) {
            stepped_back = false;
        }
        if (steps_reversed != 0) {
            steps_reversed--;
        }
    }

    @Override
    public void stepBack() {
        if ((!stepped_back && (steps_reversed == 0)) || (stepped_back && (steps_reversed < steps_saved))) {
            if (stepped_back) {
                historyBackward();
            }
            prev_time = var_history[0][0];
            _potential1 = var_history[0][1];
            _potential2 = var_history[0][2];
            _current = var_history[0][3];
            _voltage = var_history[0][4];
            if (_needsOldPotCurrent) {
                _potOld1 = var_history[0][5];
                _potOld2 = var_history[0][6];
                _oldCurrent = var_history[0][7];
                _oldOldCurrent = var_history[0][8];
            }

            stepped_back = true;
            steps_reversed++;
        }
    }

    /**
     * Move history backward for step-back operation.
     */
    protected void historyBackward() {
        for (int j = var_history.length - 1; j > 0; j--) {
            System.arraycopy(var_history[j], 0, var_history[j - 1], 0, var_history[0].length);
        }
    }
    
    /**
     * Get the matrix indices for this component.
     * @return array of matrix indices [index0, index1]
     */
    public int[] getMatrixIndices() {
        return matrixIndices;
    }
    
    /**
     * Set the matrix indices for this component.
     * @param index0 first terminal matrix index
     * @param index1 second terminal matrix index
     */
    public void setMatrixIndices(int index0, int index1) {
        matrixIndices[0] = index0;
        matrixIndices[1] = index1;
    }
}
