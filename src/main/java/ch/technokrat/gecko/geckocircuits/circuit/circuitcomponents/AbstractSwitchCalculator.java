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


public abstract class AbstractSwitchCalculator extends CircuitComponent implements AStampable, BStampable {

    
    protected static final double NEARLY_ZERO_R = 1e-9;
    private static final double DEFAULT_TEMP = 25;
    
    protected double _rON = DEFAULT_R_ON;
    protected double _rOFF = DEFAULT_R_OFF;
    // variable resistance
    protected double _rDt = DEFAULT_R_OFF;
    protected double _uForward = DEFAULT_U_FORWARD;

    public static boolean switchAction = false;
    public static boolean switchActionOccurred = false;
    protected BVector _bVector;


    protected boolean _gateValue = false;

    public AbstractSwitchCalculator(final AbstractSwitch parent) {
        super(parent);
    }


    /*
     * maybe unclean coding: Thyristor overrides this method with a different implementation,
     * all other switches use this piece of code.
     */
    public void setGateSignal(final boolean value) {
        
        _gateValue = value;
        switchAction = true;
        
        if (_gateValue) {
            _rDt = _rON;
        } else {
            _rDt = _rOFF;
        }

        _rDt = Math.max(_rDt, NEARLY_ZERO_R);
        if(_bVector != null) {
            _bVector.setUpdateAllFlag();
        }
        
        //System.out.println("Gate signal set to " + _gateValue);
        
    }

    public final boolean isGateSignalOn() {
        return _gateValue;
    }

    public boolean isBasisStampable() {
        return true;
    }

    @Override
    public final void registerBVector(final BVector bvector) {
       _bVector = bvector;
    }

    public final void setROn(final double value) {
        _rON = value;
    }

    public final void setROff(final double value) {
        _rOFF = value;
    }

    public final void setUForward(final double value) {
        _uForward = value;
    }
    
    
    public SwitchState getState(double time) {
        
        SwitchState state;
        SwitchState.State componentState;
        
        if (_rDt > _rON) {
            componentState = SwitchState.State.OFF;
        }
        else {
            componentState = SwitchState.State.ON;
        }
        
        state = new SwitchState(_parent,componentState,time);
        
        return state;
        
    }

}
