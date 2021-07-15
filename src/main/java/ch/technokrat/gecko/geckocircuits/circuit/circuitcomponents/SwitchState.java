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

//this class allows us to get information about a switching device's state - when it changed into the present state and what is it
public class SwitchState {
    
    public enum State {ON, OFF};
    
    private final State _state;
    private final AbstractCircuitBlockInterface _circuitElement;
    private final double _time;
    
    public SwitchState(AbstractCircuitBlockInterface elem, State switchState, double switchTime) {
        _state = switchState;
        _circuitElement = elem;
        _time = switchTime;
    }
    
    public State getState() {
        return _state;
    }
    
    public AbstractCircuitBlockInterface getElement() {
        return _circuitElement;
    }
    
    public String getElementName() {
        return _circuitElement.getStringID();
    }
    
    public double getTime() {
        return _time;
    }
    
    public String toString() {
        return getElementName() + ": " + _state;
    }       
}
