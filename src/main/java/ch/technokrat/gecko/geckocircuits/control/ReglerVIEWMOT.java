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
package ch.technokrat.gecko.geckocircuits.control;

import ch.technokrat.gecko.geckocircuits.allg.AbstractComponentTyp;
import ch.technokrat.gecko.geckocircuits.circuit.*;
import ch.technokrat.gecko.geckocircuits.control.calculators.AbstractControlCalculatable;
import ch.technokrat.gecko.geckocircuits.control.calculators.ViewMotorCalculator;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.awt.Window;
import java.util.List;

public final class ReglerVIEWMOT extends ReglerWithSingleReference {    
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerVIEWMOT.class, "VIEWMOT", I18nKeys.MACHINE_INTERNAL);

    public ReglerVIEWMOT() {
        super(0, 1);
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"Mmeas"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.MEASURED_MACHINE_INTERNAL};
    }

    @Override
    public AbstractControlCalculatable getInternalControlCalculatableForSimulationStart() {
        return new ViewMotorCalculator();
    }    
    
    @Override
    protected void importIndividual(final TokenMap tokenMap) {
        super.importIndividual(tokenMap);        
        if (!parameterString[0].isEmpty()) {
            if (parameterString[1].isEmpty()) {
                parameterString[1] = parameterString[0].substring(0, parameterString[0].lastIndexOf("."));  // --> "M-DC.4"                                
            }

            if (parameterString[2].isEmpty() || parameterString[2].equals("0")) {
                parameterString[2] = parameterString[0].substring(parameterString[0].lastIndexOf(".") + 1);  // --> "M-DC.4"                                                
            }
        }        
    }

    @Override
    public String[] getParameterString() {
        // this is ugly, but at the moment the best solution:
        parameterString[0] = parameterString[1] + "." + parameterString[2];
        return parameterString;
    }

    @Override
    public void copyAdditionalParameters(final AbstractBlockInterface originalBlock) {
        super.copyAdditionalParameters(originalBlock);
        if (!originalBlock.getParameterString()[1].isEmpty()) {
            // if a machine is connected, also copy the type of measurement (omega, i, ...).
            this.parameterString[2] = originalBlock.getParameterString()[2];
        }
    }    

    @Override
    String getDisplayValueWithoutError() {
        return parameterString[1] + "." + parameterString[2];
    }        

    @Override
    protected String getCenteredDrawString() {
        return "DRIVE";
    }
    
    

    @Override
    public I18nKeys getCouplingTitle() {
        return I18nKeys.SELECT_MACHINE;
    }

    @Override
    public I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_MOTOR_DEFINED_IN_CIRCUIT_SHEET;
    }

    @Override
    public void checkComponentCompatibility(final Object testObject, final List<AbstractBlockInterface> insertList) {
        // this is still ugly: since we connect to an motor internal thing
        // we don't need to check the compatibility. Refactor later!!!
    }

    @Override
    protected Window openDialogWindow() {
        return new ReglerViewMotDialog(this);
    }
}
