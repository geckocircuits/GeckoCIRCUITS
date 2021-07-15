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
package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import ch.technokrat.gecko.geckocircuits.allg.Fenster;
import ch.technokrat.gecko.geckocircuits.allg.SolverType;
import ch.technokrat.gecko.geckocircuits.circuit.AbstractTerminal;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractTwoPortLKreisBlock;

/**
 *
 * @author andy
 */
public abstract class CircuitComponent<T extends AbstractTwoPortLKreisBlock> {

    protected int _portNumber = -1;
    protected int[] matrixIndices = new int[]{-1, -1};
    protected int _componentNumber;
    protected double _current;
    protected double _voltage;
    protected double _potential1;
    protected double _potential2;
    protected double _potOld1;
    protected double _potOld2;
    protected final AbstractTerminal _term1;
    protected AbstractTerminal _term2;
    protected double _oldCurrent;
    protected double _oldOldCurrent;
    protected boolean _needsOldPotCurrent = false;
    
    //for stepping back in history
    protected double prev_time = -1;
    protected boolean stepped_back = false;
    //double array to hold history - previous two steps
    protected double[][] var_history;
    protected int steps_saved = 2;
    protected int steps_reversed = 0;
    public static final double DEFAULT_U_FORWARD = 0.6;
    public static final double DEFAULT_R_ON = 10e-3;
    public static final double DEFAULT_R_OFF = 10e6;
    /*
     * used for diode-behavior, to find a correct on/off state (Diode,
     * Thyristor, IGBT).
     */
    protected static double disturbanceValue;
    protected final T _parent;
    final SolverType _solverType;

    public CircuitComponent(final T parent) {
        _parent = parent;
        matrixIndices = new int[2];
        _term1 = parent.XIN.get(0);
        _term2 = parent.YOUT.get(0);        
        var_history = new double[steps_saved][9];        
        _solverType = Fenster._solverSettings.SOLVER_TYPE.getValue();
    }
    
    public double getOldCurrent() {
        return _oldCurrent;
    }        

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

    public final double getVoltage() {
        return _voltage;
    }

    public double getPotential(int terminalNUmber) {
        switch (terminalNUmber) {
            case 0:
                return _potential1;
            case 1:
                return _potential2;
            default:
                assert false;
        }
        return 0;
    }

    public void assignTerminalIndices() {
        matrixIndices[0] = _term1.getIndex();
        if (_term2 != null) {
            matrixIndices[1] = _term2.getIndex();
        } else {
            matrixIndices[1] = 0;
        }
        //System.out.println("element: " + _element._elementName.getValue() + " matrixIndices[0]=" + matrixIndices[0] + " matrixIndices[1]=" + matrixIndices[1]);

    }

    public void setComponentNumber(int componentNumber) {
        _componentNumber = componentNumber;
    }

    public int getComponentNumber() {
        return _componentNumber;
    }

    public final void calculateVoltage(double[] p) {
        _voltage = p[matrixIndices[0]] - p[matrixIndices[1]];
    }

    public int getTerminalIndex(int termNumber) {
        return matrixIndices[termNumber];
    }        

    public final double getCurrent() {
        return _current;
    }

    //to when going forward a timestep: copy the data of the previous timestep into the data from 2 previous timesteps ago
    private void historyForward() {
        for (int j = var_history.length - 1; j > 0; j--) {
            for (int i = 0; i < var_history[0].length; i++) {
                var_history[j][i] = var_history[j - 1][i];
            }
        }
    }

    public void saveHistory() {
        if (!stepped_back) {
            //write existing values into history
            //do so only if we have not stepped back; otherwise they are already there
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

    public void historyMaintainance(final double time) {
        prev_time = time;

        if (stepped_back) {
            stepped_back = false;
        }
        if (steps_reversed != 0) {
            steps_reversed--;
        }
    }

    public void stepBack() {
        if ((!stepped_back && (steps_reversed == 0)) || (stepped_back && (steps_reversed < steps_saved))) {
            if (stepped_back) {
                historyBackward();
            }
            prev_time = var_history[0][0];
            //System.out.println("before: _potential 1 = " + _potential1 + " _potential 2 = " + _potential2);
            _potential1 = var_history[0][1];
            _potential2 = var_history[0][2];
            //System.out.println("after: _potential 1 = " + _potential1 + " _potential 2 = " + _potential2);
            _current = var_history[0][3];
            _voltage = var_history[0][4];
            if (_needsOldPotCurrent) {
                _potOld1 = var_history[0][5];
                _potOld2 = var_history[0][6];
                //System.out.println("before: _oldCurrent = " + _oldCurrent);
                _oldCurrent = var_history[0][7];
                //System.out.println("after: _oldCurrent = " + _oldCurrent);
                _oldOldCurrent = var_history[0][8];
            }


            stepped_back = true;
            steps_reversed++;
        }
    }

    protected void historyBackward() {
        for (int j = var_history.length - 1; j > 0; j--) {
            System.arraycopy(var_history[j], 0, var_history[j - 1], 0, var_history[0].length);
        }
    }

    @Override
    public String toString() {
        return _parent.getStringID() + " calculator "; //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
}
