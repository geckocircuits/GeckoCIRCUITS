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
package gecko.geckocircuits.circuit.losscalculation;

/**
 * This is a class to "package" losses being returned by loss-calculators - so we can keep switching and conduction losses separate for
 * semiconductor components. Wrapping them in an object is more "proper" than returning an array.
 * @author anstupar
 */
public class LossContainer {
    private final double _switchingLosses;
    private final double _conductionLosses;
    
    /**
     * Create a new loss container with loss calculation results.
     * 
     * @param conductionLosses the conduction losses (W)
     * @param switchingLosses the switching losses (W)
     */
    public LossContainer (final double conductionLosses, final double switchingLosses) {
        _conductionLosses = conductionLosses;
        _switchingLosses = switchingLosses;
    }
    
    /**
     * Get the total losses (switching + conduction) stored in this container.
     * 
     * @return the total losses (W)
     */
    public double getTotalLosses() {
        return _conductionLosses + _switchingLosses;
    }
    
    /**
     * Get the conduction losses stored in this container.
     * 
     * @return the conduction losses (W)
     */
    public double getConductionLosses() {
        return _conductionLosses;
    }
    
    /**
     * Get the switching losses stored in this container.
     * 
     * @return the switching losses (W)
     */
    public double getSwitchingLosses() {
        return _switchingLosses;
    }
    
    
}
