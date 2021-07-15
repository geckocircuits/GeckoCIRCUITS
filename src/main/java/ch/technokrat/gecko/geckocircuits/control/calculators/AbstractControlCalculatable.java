/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package ch.technokrat.gecko.geckocircuits.control.calculators;

/**
 * Base-Class for all Control-Calculators.
 *
 * @author andreas
 */
public abstract class AbstractControlCalculatable {
    public static final double SIGNAL_THRESHOLD = 0.5;
    public static double _time = 0;
    
    public static void setTime(final double time) {
        _time = time;
    }
    
    public final double[][] _inputSignal;
    public final double[][] _outputSignal;

    public AbstractControlCalculatable(final int noInputs, final int noOutputs) {        
        _inputSignal = new double[noInputs][]; // careful: the array value of the input
        // signal is set when all components are connected within the netlist.
        _outputSignal = createOutputSignal(noOutputs);
    }

    public abstract void berechneYOUT(final double deltaT);

    public void setInputSignal(final int inputIndex, final AbstractControlCalculatable output, 
            final int outputIndex) throws Exception {
        if (_inputSignal[inputIndex] != null) {            
            throw new Exception("Signal already connected: " + getClass());
        }        
        _inputSignal[inputIndex] = output._outputSignal[outputIndex];                
    }

    /**
     * check if input port has no connection. If this is the case, fill the
     * input port variable with a dummy double[].
     * @param inputIndex
     * @return true if input port has no connection
     */
    public boolean checkInputWithoutConnectionAndFill(final int inputIndex) {
        if(_inputSignal[inputIndex] == null) {
            _inputSignal[inputIndex] = new double[1];
            return true;
        } else {
            return false;
        }    
    }
    
    /**
     * TearDownOnPause will by called if the Simulation is paused or finished.
     * Intended to be overwritten by subclasses to free resources if necessary.
     */
    public void tearDownOnPause() {
    }

    protected double[][] createOutputSignal(final int noOutputs) {
        return new double[noOutputs][1];
    }
}
