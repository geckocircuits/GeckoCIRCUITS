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
import ch.technokrat.gecko.geckocircuits.circuit.AbstractBlockInterface;
import ch.technokrat.gecko.geckocircuits.circuit.ConnectorType;
import ch.technokrat.gecko.geckocircuits.circuit.CurrentMeasurable;
import ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents.AbstractCircuitBlockInterface;
import ch.technokrat.gecko.i18n.resources.I18nKeys;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class ReglerFluxMeter extends AbstractCurrentMeasurement {
    public static final ControlTypeInfo tinfo = new ControlTypeInfo(ReglerFluxMeter.class, "FLUX", I18nKeys.FLUX_MEASUREMENT_WB);

    @Override
    public String[] getOutputNames() {
        return new String[]{"Fluxmeas"};
    }

    @Override
    public I18nKeys[] getOutputDescription() {
        return new I18nKeys[]{I18nKeys.MEASURED_FLUX_WEBER};
    }

    @Override
    String getVariableForDisplay() {
        return "phi";
    }    

    @Override
    public I18nKeys getCouplingTitle() {
        return I18nKeys.SELECT_RELUCTANCE_COMPONENT;
    }

    @Override
    public I18nKeys getMissingComponentsString() {
        return I18nKeys.NO_RELUCTANCE_ELEMENT_IN_CIRCUIT_SHEET;
    }

    @Override
    public void checkComponentCompatibility(final Object testObject, final List<AbstractBlockInterface> insertList) {
        if (testObject instanceof AbstractCircuitBlockInterface && testObject instanceof CurrentMeasurable) {
            final Collection<? extends AbstractBlockInterface> toAdd = 
                    Arrays.asList(((CurrentMeasurable) testObject).getCurrentMeasurementComponents(ConnectorType.RELUCTANCE));
            insertList.addAll(toAdd);
        }
    }
}
