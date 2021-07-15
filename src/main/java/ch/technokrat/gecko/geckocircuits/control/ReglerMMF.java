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


public final class ReglerMMF extends AbstractPotentialMeasurement  {        
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerMMF.class,"MMF_MEAS", I18nKeys.MMF_MEASURE_A);
    
    public ReglerMMF() {
        super(ConnectorType.RELUCTANCE);        
    }

    @Override
    public String[] getOutputNames() {
        return new String[]{"MMFmeas"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.MMF_MEASURE_A};
    }                        

    @Override
    protected String getCenteredDrawString() {
        return "MMF";
    }
    

    @Override
    public I18nKeys getCouplingTitle() {
        return I18nKeys.MEASURE_MMF_AT_COMPONENT;
    }

    @Override
    public I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_RELUCTANCE_COMPONENT_DEFINED_IN_CIRCUIT;
    }
    
}
