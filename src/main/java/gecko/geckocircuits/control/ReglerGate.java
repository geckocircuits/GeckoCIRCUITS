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
package gecko.geckocircuits.control;

import gecko.geckocircuits.circuit.AbstractBlockInterface;
import gecko.geckocircuits.circuit.ComponentCoupable;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import gecko.geckocircuits.circuit.circuitcomponents.AbstractSwitch;
import gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import gecko.geckocircuits.control.calculators.GateCalculator;
import gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.util.List;

public final class ReglerGate extends ReglerWithSingleReference implements ComponentCoupable {
    private static final int BLOCK_WIDTH = 3;
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerGate.class, "GATE", I18nKeys.GATE_CONTROL);

    public ReglerGate() {
        super(1, 0);
    }

    @Override
    public String[] getOutputNames() {
        return new String[0];
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[0];
    }

    @Override
    public double getXShift() {
        return 1/2.0;
    }

    @Override
    public int getBlockWidth() {
        return (int) (BLOCK_WIDTH * dpix);
    }

    
    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new GateCalculator(); 
    }    

    @Override
    String getDisplayValueWithoutError() {
        return ">> " + getComponentCoupling()._coupledElements[0].getStringID();
    };
            
        

    @Override
    public I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_SWITCH_EXISTING_IN_CIRCUIT_SHEET;
    }

    @Override
    public I18nKeys getCouplingTitle() {
        return I18nKeys.SELECT_SWITCH_TO_BE_CONTROLLED; 
    }

    @Override
    public void checkComponentCompatibility(final Object testObject, final List<AbstractBlockInterface> insertList) {
        if (testObject instanceof AbstractSwitch) {
            insertList.add((AbstractCircuitBlockInterface) testObject);
        }

    }

    @Override
    protected Window openDialogWindow() {
        return new ReglerGateDialog(this);        
    }            

    
    
    
    
}
