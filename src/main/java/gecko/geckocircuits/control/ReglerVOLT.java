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

import gecko.geckocircuits.circuit.*;
import gecko.i18n.resources.I18nKeys;




public final class ReglerVOLT extends AbstractPotentialMeasurement  {        

    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerVOLT.class, "VOLT", I18nKeys.VOLTAGE_MEASUREMENT_V);
    
    public ReglerVOLT() {
        super(ConnectorType.LK);        
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"Vmeas"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.MEASURED_VOLTAGE_V};
    }                            
    

    @Override
    public I18nKeys getCouplingTitle() {
        return I18nKeys.MEASURE_VOLTAGE_AT_COMPONENT;
    }

    @Override
    public I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_CIRCUIT_COMPONENT_DEFINED;
    }
}
