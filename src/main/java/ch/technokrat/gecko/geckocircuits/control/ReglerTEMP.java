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


import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.i18n.resources.I18nKeys;


public final class ReglerTEMP extends AbstractPotentialMeasurement {            
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerTEMP.class, "TEMP", I18nKeys.TEMPERATURE_MEASUREMENT);
    
    public ReglerTEMP() {
        super(ConnectorType.THERMAL);        
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"Tmeas"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.MEASURED_TEMPERATURE_K};
    }                                  

            

    @Override
    public I18nKeys getCouplingTitle() {
        return I18nKeys.TEMPERATURE_MEASUREMENT;
    }

    @Override
    public I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_THERMAL_COMPONENTS_DETECTED;
    }   

}
